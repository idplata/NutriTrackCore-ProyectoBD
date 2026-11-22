"""Servicio de visión de prueba (stub) para NutriTrack Core.

Responde los mismos contratos que el servicio real (/health, /analyze, /estimate)
con datos fijos, para desarrollar la app sin descargar modelos.

Arranque:
    uvicorn stub_service:app --host 127.0.0.1 --port 8000

Los food_id deben existir en la tabla Alimento. Se pueden cambiar sin tocar el
código con la variable de entorno STUB_FOOD_IDS (por ejemplo: STUB_FOOD_IDS=1,2,3).
"""
import os

from fastapi import FastAPI, HTTPException, Query, Request

MAX_BYTES = 10 * 1024 * 1024
FOOD_IDS = [int(x) for x in os.environ.get("STUB_FOOD_IDS", "1,2,3").split(",") if x.strip()]

app = FastAPI(title="NutriTrack - servicio de visión (stub)")


async def read_image(request: Request) -> bytes:
    """Lee la imagen del cuerpo y aplica las mismas reglas que el servicio real."""
    data = await request.body()
    if len(data) > MAX_BYTES:
        raise HTTPException(status_code=413, detail="Imagen demasiado grande")
    is_jpeg = data[:3] == b"\xff\xd8\xff"
    is_png = data[:4] == b"\x89PNG"
    if not (is_jpeg or is_png):
        raise HTTPException(status_code=400, detail="La imagen no se puede leer")
    return data


@app.get("/health")
def health():
    return {"status": "ok", "model": "stub"}


@app.post("/analyze")
async def analyze(request: Request):
    await read_image(request)
    first = FOOD_IDS[0] if FOOD_IDS else None
    second = FOOD_IDS[1] if len(FOOD_IDS) > 1 else None
    items = [
        {"label": "white rice", "food_id": first, "grams": 150.0, "confidence": 0.82,
         "alternatives": FOOD_IDS[1:4]},
        {"label": "chicken breast", "food_id": second, "grams": 120.0, "confidence": 0.35,
         "alternatives": FOOD_IDS[2:5]},
        {"label": "unknown", "food_id": None, "grams": 80.0, "confidence": 0.20,
         "alternatives": FOOD_IDS[:3]},
    ]
    return {"items": items}


@app.post("/estimate")
async def estimate(request: Request, food_id: int = Query(..., gt=0)):
    await read_image(request)
    return {"grams": 150.0, "confidence": 0.75}