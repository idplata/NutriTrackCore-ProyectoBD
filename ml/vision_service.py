"""Servicio de visión real de NutriTrack Core (solo en el PC de la presentación).

Mismos contratos que stub_service.py:
    GET  /health              -> {"status": "ok", "model": "real", ...}
    POST /analyze             -> {"items": [{label, food_id, grams, confidence, alternatives}]}
    POST /estimate?food_id=N  -> {"grams": g, "confidence": c}

Flujo de /analyze: SAM propone regiones, CLIP dice qué alimento es cada una
(o si es plato/mesa) y portion.py calcula los gramos. Las calorías las
calcula la aplicación Java con la tabla Alimento.

Arranque manual (dentro de la carpeta ml):
    .venv\\Scripts\\python.exe -m uvicorn vision_service:app --host 127.0.0.1 --port 8000
Los modelos se cargan antes de que /health responda, así que la aplicación
espera hasta que el servicio esté listo de verdad.
"""
import hashlib
import io
import threading
import time
from collections import OrderedDict
from contextlib import asynccontextmanager

import numpy as np
from fastapi import FastAPI, HTTPException, Query, Request
from PIL import Image, ImageOps

from catalog import cargar_catalogo
from classifier import ClipClassifier, recorte_con_mascara
from embedder import DinoEmbedder
from portion import PortionModel
from segmenter import SamSegmenter

MAX_BYTES = 10 * 1024 * 1024
MAX_PORCIONES = 8
PROB_FONDO_MAX = 0.55      # si el recorte es más "plato/mesa" que esto, se descarta
SOLAPE_MAX = 0.5           # fracción de una máscara que puede caer dentro de otra ya aceptada
ALTERNATIVAS = 4
CACHE_FOTOS = 4

estado = {}
candado = threading.Lock()   # los modelos se usan de a una foto a la vez


def log(msg):
    print(time.strftime("%H:%M:%S"), msg, flush=True)


@asynccontextmanager
async def lifespan(app):
    t0 = time.time()
    alimentos, origen = cargar_catalogo(log)
    log(f"[inicio] {len(alimentos)} alimentos desde {origen}")
    log("[inicio] Cargando SAM...")
    estado["sam"] = SamSegmenter()
    log("[inicio] Cargando CLIP...")
    estado["clip"] = ClipClassifier(alimentos, log)
    log("[inicio] Cargando DINOv2...")
    estado["porciones"] = PortionModel(alimentos, DinoEmbedder(), log)
    estado["alimentos"] = alimentos
    estado["cache"] = OrderedDict()
    log(f"[inicio] Listo en {time.time() - t0:.0f} s")
    yield
    estado.clear()


app = FastAPI(title="NutriTrack - servicio de visión", lifespan=lifespan)


async def leer_imagen(request: Request):
    """Lee la foto del cuerpo (JPEG o PNG) y devuelve (bytes, imagen reducida)."""
    datos = await request.body()
    if len(datos) > MAX_BYTES:
        raise HTTPException(status_code=413, detail="Imagen demasiado grande")
    if not (datos[:3] == b"\xff\xd8\xff" or datos[:4] == b"\x89PNG"):
        raise HTTPException(status_code=400, detail="La imagen no se puede leer")
    try:
        imagen = ImageOps.exif_transpose(Image.open(io.BytesIO(datos))).convert("RGB")
    except Exception:
        raise HTTPException(status_code=400, detail="La imagen no se puede leer")
    return datos, SamSegmenter.reducir(imagen)


def analizar(datos: bytes, imagen: Image.Image):
    """Corre el flujo completo y guarda el resultado en caché por hash de la foto."""
    clave = hashlib.sha256(datos).hexdigest()
    cache = estado["cache"]
    if clave in cache:
        cache.move_to_end(clave)
        return cache[clave]

    t0 = time.time()
    mascaras = estado["sam"].segment(imagen)
    recortes = [recorte_con_mascara(imagen, m) for m in mascaras]
    clasificados = estado["clip"].classify(recortes)

    # Candidatas de comida ordenadas por confianza
    candidatas = []
    for m, (prob, fondo) in zip(mascaras, clasificados):
        if fondo > PROB_FONDO_MAX:
            continue
        prob = prob / prob.sum()
        candidatas.append((float(prob.max()), m, prob))
    candidatas.sort(key=lambda c: c[0], reverse=True)

    # Se quitan las que se montan sobre otra ya aceptada; mismo alimento = misma porción
    aceptadas, ocupado = [], np.zeros(imagen.size[::-1], dtype=bool)
    for conf, m, prob in candidatas:
        area = int(m.sum())
        if area == 0 or (m & ocupado).sum() / area > SOLAPE_MAX:
            continue
        libre = m & ~ocupado
        ocupado |= m
        top = int(prob.argmax())
        repetida = next((a for a in aceptadas if a["top"] == top), None)
        if repetida:
            repetida["area"] += int(libre.sum())
            repetida["prob"] = np.maximum(repetida["prob"], prob)
            continue
        aceptadas.append({"top": top, "area": int(libre.sum()), "conf": conf, "prob": prob})
        if len(aceptadas) >= MAX_PORCIONES:
            break

    gramos, factor = estado["porciones"].estimate(imagen, [(a["top"], a["area"]) for a in aceptadas])
    for a, g in zip(aceptadas, gramos):
        a["grams"] = g
        a["confidence"] = round(min(0.95, a["conf"] * (0.6 + 0.4 * factor)), 3)
    resultado = {"porciones": aceptadas, "factor": factor}
    log(f"[analyze] {len(mascaras)} regiones, {len(aceptadas)} porciones, {time.time() - t0:.1f} s")

    cache[clave] = resultado
    while len(cache) > CACHE_FOTOS:
        cache.popitem(last=False)
    return resultado


@app.get("/health")
def health():
    if "clip" not in estado:
        return {"status": "loading", "model": "real"}
    return {"status": "ok", "model": "real", "foods": len(estado["alimentos"])}


@app.post("/analyze")
async def analyze(request: Request):
    datos, imagen = await leer_imagen(request)
    with candado:
        resultado = analizar(datos, imagen)
    alimentos = estado["alimentos"]
    items = []
    for a in resultado["porciones"]:
        orden = [int(i) for i in np.argsort(-a["prob"])[:ALTERNATIVAS + 1]]
        food = alimentos[a["top"]]
        items.append({
            "label": food.aliases[0],
            "food_id": food.food_id,
            "grams": round(a["grams"], 1),
            "confidence": a["confidence"],
            "alternatives": [alimentos[i].food_id for i in orden if i != a["top"]][:ALTERNATIVAS],
        })
    return {"items": items}


@app.post("/estimate")
async def estimate(request: Request, food_id: int = Query(..., gt=0)):
    datos, imagen = await leer_imagen(request)
    alimentos = estado["alimentos"]
    idx = next((i for i, f in enumerate(alimentos) if f.food_id == food_id), None)
    if idx is None:
        raise HTTPException(status_code=404, detail="El alimento no está en el catálogo del servicio")
    with candado:
        resultado = analizar(datos, imagen)
    porciones = resultado["porciones"]
    if not porciones:
        gramos, factor = estado["porciones"].estimate(imagen, [(idx, imagen.width * imagen.height // 4)])
        return {"grams": round(gramos[0], 1), "confidence": round(0.3 * factor, 3)}
    # La porción donde ese alimento es más probable; se ajusta por la densidad del nuevo alimento
    mejor = max(porciones, key=lambda a: a["prob"][idx])
    anterior = alimentos[mejor["top"]]
    gramos = mejor["grams"] * alimentos[idx].densidad / anterior.densidad
    confianza = min(0.9, 0.3 + float(mejor["prob"][idx])) * (0.6 + 0.4 * resultado["factor"])
    return {"grams": round(gramos, 1), "confidence": round(confianza, 3)}