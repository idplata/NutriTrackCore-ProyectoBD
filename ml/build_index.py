"""Construye el índice de platos de Nutrition5k que usa portion.py.

Para cada plato con foto cenital calcula su huella DINOv2 y guarda:
  index/n5k_index.npz   vectores (n, 384), masas (g) y dish_id de cada plato
  index/n5k_foods.json  {dish_id: {nombre del catálogo: gramos}} según n5k_map.csv

Se corre una sola vez en el PC de la presentación (en CPU tarda unos
minutos con 1000 platos):
    .venv\\Scripts\\python.exe build_index.py
"""
import argparse
import csv
import json
import time
from pathlib import Path

import numpy as np
from PIL import Image

from embedder import DinoEmbedder

ML_DIR = Path(__file__).resolve().parent
DATOS = ML_DIR / "data" / "nutrition5k"
MAPA = ML_DIR / "n5k_map.csv"
SALIDA = ML_DIR / "index"


def leer_csv(ruta: Path):
    with open(ruta, newline="", encoding="utf-8") as f:
        return list(csv.DictReader(f))


def main():
    p = argparse.ArgumentParser()
    p.add_argument("--lote", type=int, default=16)
    args = p.parse_args()

    for requerido in (DATOS / "dishes.csv", DATOS / "dish_ingredients.csv", DATOS / "dishes_with_photo.csv"):
        if not requerido.is_file():
            raise SystemExit(f"No existe {requerido}: corre primero descargar_nutrition5k.py")

    masas = {r["dish_id"]: float(r["total_mass_g"] or 0) for r in leer_csv(DATOS / "dishes.csv")}
    mapa = {}
    if MAPA.is_file():
        mapa = {r["ingr"].lower(): r["nombre"] for r in leer_csv(MAPA)}
    else:
        print("Aviso: no existe n5k_map.csv; el índice no tendrá gramos por alimento (corre n5k_map.py)")

    contenido = {}
    for r in leer_csv(DATOS / "dish_ingredients.csv"):
        nombre = mapa.get(r["ingr_name"].strip().lower())
        if nombre:
            platos = contenido.setdefault(r["dish_id"], {})
            platos[nombre] = platos.get(nombre, 0.0) + float(r["grams"] or 0)

    platos = []
    for r in leer_csv(DATOS / "dishes_with_photo.csv"):
        d = r["dish_id"]
        foto = DATOS / "overhead" / f"{d}.png"
        if masas.get(d, 0) > 0 and foto.is_file():
            platos.append(d)
    if not platos:
        raise SystemExit("No hay platos con foto y peso mayor que 0")

    print(f"Calculando la huella de {len(platos)} platos...")
    embedder = DinoEmbedder()
    vectores, t0 = [], time.time()
    for i in range(0, len(platos), args.lote):
        lote = platos[i:i + args.lote]
        imagenes = [Image.open(DATOS / "overhead" / f"{d}.png").convert("RGB") for d in lote]
        vectores.append(embedder.embed(imagenes))
        hechos = i + len(lote)
        if hechos % 160 < args.lote or hechos == len(platos):
            print(f"  {hechos}/{len(platos)} ({time.time() - t0:.0f} s)")

    SALIDA.mkdir(exist_ok=True)
    np.savez(SALIDA / "n5k_index.npz",
             vectores=np.vstack(vectores),
             masas=np.array([masas[d] for d in platos], dtype=np.float32),
             platos=np.array(platos))
    (SALIDA / "n5k_foods.json").write_text(
        json.dumps({d: contenido.get(d, {}) for d in platos}, ensure_ascii=False, indent=0), encoding="utf-8")
    con_alimentos = sum(1 for d in platos if contenido.get(d))
    print(f"Índice guardado en {SALIDA} ({len(platos)} platos, {con_alimentos} con alimentos del catálogo)")


if __name__ == "__main__":
    main()