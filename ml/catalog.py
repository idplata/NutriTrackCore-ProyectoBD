"""Catálogo de alimentos para el servicio de visión de NutriTrack Core.

Lee los alimentos activos de la tabla Alimento usando la misma configuración
que la aplicación Java (db.properties). Si la base de datos no responde, usa
la última copia guardada en catalog.csv. A cada alimento le agrega sus
descripciones en inglés (catalog_aliases.csv), que son las que entiende CLIP.

Uso desde la terminal (dentro de la carpeta ml):
    .venv\\Scripts\\python.exe catalog.py
muestra de dónde se leyó el catálogo y qué alimentos no tienen alias.
"""
import csv
import os
import re
from dataclasses import dataclass, field
from pathlib import Path

ML_DIR = Path(__file__).resolve().parent
CATALOG_CSV = ML_DIR / "catalog.csv"
ALIASES_CSV = ML_DIR / "catalog_aliases.csv"

# Densidad (g/cm³) cuando el alimento no la tiene en la BD, según su grupo
DENSIDAD_POR_GRUPO = {
    "FRUTAS_Y_VERDURAS": 0.60,
    "CARBOHIDRATOS": 0.75,
    "ARROZ": 0.80,
    "PROTEINAS_Y_LACTEOS": 1.00,
    "ULTRAPROCESADOS": 0.95,
    "GRASA_SALUDABLES": 0.90,
    "DULCES": 0.80,
}
DENSIDAD_DEFECTO = 0.85


@dataclass
class Food:
    """Un alimento del catálogo."""
    food_id: int
    nombre: str
    densidad: float
    grupo: str
    aliases: list = field(default_factory=list)


class CatalogError(Exception):
    """No se pudo leer el catálogo ni de la BD ni del CSV."""


def _rutas_db_properties():
    """Lugares donde se busca db.properties, en orden."""
    env = os.environ.get("NUTRITRACK_DB_PROPERTIES")
    if env:
        yield Path(env)
    raiz = ML_DIR.parent
    yield raiz / "db.properties"
    yield raiz / "src" / "main" / "resources" / "db.properties"
    yield ML_DIR / "db.properties"


def leer_properties(ruta: Path) -> dict:
    """Lee un archivo .properties sencillo (clave=valor, # para comentarios)."""
    datos = {}
    for linea in ruta.read_text(encoding="utf-8", errors="replace").splitlines():
        linea = linea.strip()
        if not linea or linea[0] in "#!":
            continue
        clave, sep, valor = linea.partition("=")
        if not sep:
            clave, sep, valor = linea.partition(":")
        if sep:
            datos[clave.strip()] = valor.strip()
    return datos


def parametros_conexion(props: dict) -> dict:
    """Convierte db.url (formato JDBC) en los parámetros de psycopg."""
    url = props.get("db.url", "")
    m = re.match(r"jdbc:postgresql://([^:/?]+)(?::(\d+))?/([^?]+)", url)
    if not m:
        raise CatalogError(f"db.url no tiene el formato jdbc:postgresql://host:puerto/base: {url!r}")
    host, puerto, base = m.groups()
    return {
        "host": host,
        "port": int(puerto or 5432),
        "dbname": base,
        "user": props.get("db.usuario", ""),
        "password": props.get("db.contrasena", ""),
        "connect_timeout": 5,
    }


def leer_desde_bd():
    """Devuelve la lista de alimentos activos leídos de PostgreSQL."""
    ruta = next((r for r in _rutas_db_properties() if r.is_file()), None)
    if ruta is None:
        raise CatalogError("No se encontró db.properties")
    params = parametros_conexion(leer_properties(ruta))
    import psycopg  # se importa aquí para que el CSV funcione aunque falte la librería
    with psycopg.connect(**params) as conn, conn.cursor() as cur:
        cur.execute("SELECT id_alimento, nombre, densidad_g_cm3, grupo FROM Alimento "
                    "WHERE activo ORDER BY id_alimento")
        filas = cur.fetchall()
    return [(int(i), n, None if d is None else float(d), g) for i, n, d, g in filas], str(ruta)


def guardar_csv(filas):
    """Guarda la copia de respaldo del catálogo."""
    with open(CATALOG_CSV, "w", newline="", encoding="utf-8") as f:
        w = csv.writer(f)
        w.writerow(["id_alimento", "nombre", "densidad_g_cm3", "grupo"])
        for i, n, d, g in filas:
            w.writerow([i, n, "" if d is None else d, g])


def leer_desde_csv():
    """Lee la copia de respaldo del catálogo."""
    if not CATALOG_CSV.is_file():
        raise CatalogError("No hay catalog.csv: arranca una vez con la base de datos encendida")
    filas = []
    with open(CATALOG_CSV, newline="", encoding="utf-8") as f:
        for r in csv.DictReader(f):
            d = r["densidad_g_cm3"].strip()
            filas.append((int(r["id_alimento"]), r["nombre"], float(d) if d else None, r["grupo"]))
    return filas


def leer_alias():
    """nombre en español -> lista de descripciones en inglés."""
    alias = {}
    if ALIASES_CSV.is_file():
        with open(ALIASES_CSV, newline="", encoding="utf-8") as f:
            for r in csv.DictReader(f):
                alias[r["nombre"].strip().lower()] = [a.strip() for a in r["alias_en"].split("|") if a.strip()]
    return alias


def cargar_catalogo(log=print):
    """Carga el catálogo completo. Devuelve (lista de Food, origen)."""
    try:
        filas, origen = leer_desde_bd()
        guardar_csv(filas)
        origen = f"base de datos ({origen})"
    except Exception as e:  # BD apagada, credenciales malas, falta psycopg...
        log(f"[catalogo] No se pudo leer la BD ({e}); se usa catalog.csv")
        filas = leer_desde_csv()
        origen = "catalog.csv"
    if not filas:
        raise CatalogError("La tabla Alimento no tiene alimentos activos")
    alias = leer_alias()
    alimentos = []
    for i, n, d, g in filas:
        densidad = d if d and d > 0 else DENSIDAD_POR_GRUPO.get(g, DENSIDAD_DEFECTO)
        nombres = alias.get(n.strip().lower()) or [n]   # sin alias se usa el nombre en español
        alimentos.append(Food(i, n, densidad, g, nombres))
    return alimentos, origen


if __name__ == "__main__":
    comida, desde = cargar_catalogo()
    sin_alias = [f.nombre for f in comida if f.aliases == [f.nombre]]
    print(f"{len(comida)} alimentos leídos desde {desde}")
    if sin_alias:
        print(f"{len(sin_alias)} sin alias en inglés (agrégalos a catalog_aliases.csv):")
        for n in sin_alias:
            print("  -", n)