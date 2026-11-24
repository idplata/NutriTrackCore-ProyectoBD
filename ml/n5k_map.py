"""Relaciona los ingredientes de Nutrition5k con los alimentos del catálogo.

Genera n5k_map.csv (ingr, nombre, metodo, similitud), que usa build_index.py
para saber cuántos gramos de cada alimento del catálogo traía cada plato.

Orden de búsqueda para cada ingrediente:
  1. n5k_map_manual.csv (relaciones escritas a mano; mandan sobre todo lo demás).
  2. Igual a un alias en inglés de catalog_aliases.csv (sin distinguir mayúsculas
     ni la "s" final).
  3. Con --clip: el alimento cuyo texto CLIP se parece más, si la similitud
     pasa el umbral. Revisa esas filas en el CSV antes de construir el índice.
Los ingredientes que no se relacionan (sal, salsas, aceites de cocción...)
se ignoran: el peso del plato completo sí los incluye.

Uso (dentro de la carpeta ml, después de descargar_nutrition5k.py):
    .venv\\Scripts\\python.exe n5k_map.py --clip
"""
import argparse
import csv
from pathlib import Path

from catalog import cargar_catalogo

ML_DIR = Path(__file__).resolve().parent
DATOS = ML_DIR / "data" / "nutrition5k"
MANUAL = ML_DIR / "n5k_map_manual.csv"
SALIDA = ML_DIR / "n5k_map.csv"


def normalizar(texto: str) -> str:
    t = " ".join(texto.lower().replace("_", " ").split())
    return t[:-1] if t.endswith("s") and not t.endswith("ss") else t


def ingredientes_nutrition5k():
    """Nombres únicos de ingredientes que aparecen en los platos descargados."""
    nombres = set()
    archivo = DATOS / "dish_ingredients.csv"
    if not archivo.is_file():
        raise SystemExit(f"No existe {archivo}: corre primero descargar_nutrition5k.py")
    with open(archivo, newline="", encoding="utf-8") as f:
        for r in csv.DictReader(f):
            if r["ingr_name"].strip():
                nombres.add(r["ingr_name"].strip())
    return sorted(nombres)


def main():
    p = argparse.ArgumentParser()
    p.add_argument("--clip", action="store_true", help="usar CLIP para los que no se encuentren por nombre")
    p.add_argument("--umbral", type=float, default=0.88, help="similitud mínima de CLIP (0 a 1)")
    args = p.parse_args()

    alimentos, origen = cargar_catalogo()
    por_nombre = {f.nombre.lower(): f for f in alimentos}
    por_alias = {}
    for f in alimentos:
        for a in f.aliases:
            por_alias.setdefault(normalizar(a), f)

    manual = {}
    if MANUAL.is_file():
        with open(MANUAL, newline="", encoding="utf-8") as fh:
            for r in csv.DictReader(fh):
                f = por_nombre.get(r["nombre"].strip().lower())
                if f is None:
                    print(f"  aviso: '{r['nombre']}' de n5k_map_manual.csv no está en el catálogo")
                else:
                    manual[r["ingr"].strip().lower()] = f

    ingredientes = ingredientes_nutrition5k()
    filas, pendientes = [], []
    for ingr in ingredientes:
        if ingr.lower() in manual:
            filas.append((ingr, manual[ingr.lower()].nombre, "manual", ""))
        elif normalizar(ingr) in por_alias:
            filas.append((ingr, por_alias[normalizar(ingr)].nombre, "alias", ""))
        else:
            pendientes.append(ingr)

    if args.clip and pendientes:
        import numpy as np
        from classifier import ClipClassifier
        clip = ClipClassifier(alimentos)
        textos = clip.textos[:clip.n_alimentos]
        vectores = clip.encode_texts([f"a photo of {i}, a type of food." for i in pendientes])
        sim = vectores @ textos.T
        for ingr, fila in zip(pendientes, sim):
            k = int(np.argmax(fila))
            if fila[k] >= args.umbral:
                filas.append((ingr, alimentos[k].nombre, "clip", f"{fila[k]:.3f}"))

    with open(SALIDA, "w", newline="", encoding="utf-8") as fh:
        w = csv.writer(fh)
        w.writerow(["ingr", "nombre", "metodo", "similitud"])
        w.writerows(sorted(filas))
    print(f"Catálogo: {len(alimentos)} alimentos ({origen})")
    print(f"Ingredientes de Nutrition5k: {len(ingredientes)} | relacionados: {len(filas)}")
    print(f"Guardado en {SALIDA}")


if __name__ == "__main__":
    main()