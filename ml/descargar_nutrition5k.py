"""
Descarga la parte de Nutrition5k que necesita NutriTrack Core:
  1. Metadatos de platos e ingredientes (pocos MB).
  2. Una muestra de fotos cenitales (desde arriba) de platos.
  3. Dos CSV limpios: dishes.csv y dish_ingredients.csv.

Uso (dentro de la carpeta ml):
    .venv\\Scripts\\python.exe descargar_nutrition5k.py --platos 1000

Se puede cortar y volver a correr: lo ya descargado no se baja otra vez.
"""
import argparse
import csv
import random
import sys
import time 
import urllib.error
import urllib.request
from pathlib import Path


# Carpeta ml (los valores por defecto no dependen de dónde se corra el script)
ML_DIR = Path(__file__).resolve().parent
MAX_PLATOS = 1000
REINTENTOS = 3

# Raíz pública del bucket de Nutrition5k
BASE = "https://storage.googleapis.com/nutrition5k_dataset/nutrition5k_dataset"

# Archivos de metadatos que se necesitan
METADATOS = [
    "metadata/dish_metadata_cafe1.csv",
    "metadata/dish_metadata_cafe2.csv",
    "metadata/ingredients_metadata.csv",
]

# Foto cenital RGB de cada plato
RUTA_FOTO = "imagery/realsense_overhead/{dish_id}/rgb.png"


def descargar(url: str, destino: Path) -> bool:
    """Descarga una URL a un archivo. Devuelve False si no existe (404)."""
    if destino.exists() and destino.stat().st_size > 0:
        return True  # ya descargado: permite reanudar
    destino.parent.mkdir(parents=True, exist_ok=True)
    temporal = destino.with_suffix(destino.suffix + ".part")
    for intento in range(1, REINTENTOS + 1):
        try:
            with urllib.request.urlopen(url, timeout=60) as resp, open(temporal, "wb") as f:
                f.write(resp.read())
            temporal.replace(destino)   # solo queda el archivo si llegó completo
            return True
        except urllib.error.HTTPError as e:
            if e.code == 404:
                return False
            error = e
        except (urllib.error.URLError, TimeoutError, ConnectionError) as e:
            error = e
        print(f"    reintento {intento}/{REINTENTOS} ({error})")
        time.sleep(2 * intento)
    temporal.unlink(missing_ok=True)
    raise RuntimeError(f"No se pudo descargar {url}: {error}")


def leer_platos(ruta_csv: Path):
    """
    Lee un dish_metadata_cafeX.csv. Cada fila tiene longitud variable:
    dish_id, total_calories, total_mass, total_fat, total_carb, total_protein,
    y luego bloques de 7 campos por ingrediente:
    ingr_id, ingr_name, grams, calories, fat, carb, protein.
    """
    platos, ingredientes = [], []
    with open(ruta_csv, newline="", encoding="utf-8") as f:
        for fila in csv.reader(f):
            if len(fila) < 6:
                continue
            dish_id = fila[0]
            platos.append({
                "dish_id": dish_id,
                "total_calories": fila[1],
                "total_mass_g": fila[2],
                "total_fat": fila[3],
                "total_carb": fila[4],
                "total_protein": fila[5],
            })
            resto = fila[6:]
            for i in range(0, len(resto) - 6, 7):
                ingredientes.append({
                    "dish_id": dish_id,
                    "ingr_id": resto[i],
                    "ingr_name": resto[i + 1],
                    "grams": resto[i + 2],
                    "calories": resto[i + 3],
                    "fat": resto[i + 4],
                    "carb": resto[i + 5],
                    "protein": resto[i + 6],
                })
    return platos, ingredientes


def escribir_csv(ruta: Path, filas: list):
    """Escribe una lista de diccionarios como CSV con encabezado."""
    if not filas:
        return
    with open(ruta, "w", newline="", encoding="utf-8") as f:
        w = csv.DictWriter(f, fieldnames=list(filas[0].keys()))
        w.writeheader()
        w.writerows(filas)


def main():
    p = argparse.ArgumentParser()
    p.add_argument("--destino", default=str(ML_DIR / "data" / "nutrition5k"))
    p.add_argument("--platos", type=int, default=MAX_PLATOS, help=f"cuántas fotos bajar (máximo {MAX_PLATOS})")
    p.add_argument("--semilla", type=int, default=42)
    args = p.parse_args()
    args.platos = max(1, min(args.platos, MAX_PLATOS))
    destino = Path(args.destino)

    print(f"Destino: {destino.resolve()}")

    # Paso 1: metadatos
    for rel in METADATOS:
        ok = descargar(f"{BASE}/{rel}", destino / rel)
        print(f"  {'OK ' if ok else 'NO '} {rel}")
        if not ok:
            sys.exit(f"No se encontró {rel}. Revisa la ruta en el README del repositorio.")

    # Paso 2: CSV limpios
    platos, ingredientes = [], []
    for cafe in ("dish_metadata_cafe1.csv", "dish_metadata_cafe2.csv"):
        pl, ing = leer_platos(destino / "metadata" / cafe)
        platos += pl
        ingredientes += ing
    escribir_csv(destino / "dishes.csv", platos)
    escribir_csv(destino / "dish_ingredients.csv", ingredientes)
    print(f"Platos: {len(platos)} | Líneas de ingredientes: {len(ingredientes)}")

    # Paso 3: muestra de fotos cenitales (no todos los platos tienen foto cenital)
    random.seed(args.semilla)
    candidatos = [pl["dish_id"] for pl in platos if float(pl["total_mass_g"] or 0) > 0]
    random.shuffle(candidatos)
    bajadas, intentos = 0, 0
    con_foto = []
    for dish_id in candidatos:
        if bajadas >= args.platos:
            break
        intentos += 1
        rel = RUTA_FOTO.format(dish_id=dish_id)
        try:
            ok = descargar(f"{BASE}/{rel}", destino / "overhead" / f"{dish_id}.png")
        except RuntimeError as e:
            print(f"  se salta {dish_id}: {e}")
            continue
        if ok:
            bajadas += 1
            con_foto.append({"dish_id": dish_id})
            if bajadas % 25 == 0:
                print(f"  fotos: {bajadas}/{args.platos}")
    escribir_csv(destino / "dishes_with_photo.csv", con_foto)
    print(f"Fotos descargadas: {bajadas} (intentos: {intentos})")


if __name__ == "__main__":
    main()
