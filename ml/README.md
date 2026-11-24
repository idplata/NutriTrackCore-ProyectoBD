# Servicio de visión de NutriTrack Core (`ml/`)

Programa en Python que reconoce los alimentos de una foto y estima sus gramos. NutriTrack (Java) le manda la foto a `http://127.0.0.1:8000` y recibe para cada porción el alimento, los gramos y la confianza. Las calorías las calcula Java con la tabla `Alimento`.

| Archivo | Para qué sirve |
|---|---|
| `stub_service.py` | Respuestas fijas para desarrollar la app sin modelos (cualquier PC) |
| `vision_service.py` | Servicio real: SAM + CLIP + DINOv2 (solo el PC de la presentación) |
| `catalog.py` | Lee los alimentos de la BD (`db.properties`); si falla, usa `catalog.csv` |
| `catalog_aliases.csv` | Nombre en español de la BD → descripciones en inglés para CLIP |
| `segmenter.py` / `classifier.py` / `portion.py` | Segmentar, clasificar y calcular gramos |
| `descargar_nutrition5k.py`, `n5k_map.py`, `build_index.py` | Preparar el índice de platos de Nutrition5k |

El modo se elige en `camera.properties` con `vision.module` (`stub_service` o `vision_service`).

---

## A. Stub (PCs de desarrollo)

Requisito: Python 3.14.
```
ml\instalar_dev.bat stub
```
La app lo arranca sola si `vision.module=stub_service`. A mano, dentro de `ml\`:
```
.venv\Scripts\python.exe -m uvicorn stub_service:app --host 127.0.0.1 --port 8000
```

---

## B. Servicio real (PC de la presentación)

Windows 11, 16 GB de RAM, sin GPU NVIDIA: todo corre en la CPU. Hacen falta unos 6 GB libres e internet solo durante la preparación.

### B.1 Requisitos
1. **Python 3.14** de 64 bits (python.org), marcando **Add python.exe to PATH**.
2. **Git** (lo usa pip para instalar `segment-anything`).
3. **PostgreSQL** con la base de NutriTrack cargada (001 a 007) y el `db.properties` de la app en la raíz del proyecto o en `src\main\resources\`.

### B.2 Instalar (una vez, unos 20 a 40 minutos)
Desde la raíz del repositorio:
```
ml\instalar_dev.bat
```
Crea `ml\.venv`, instala PyTorch para CPU, open_clip, transformers, psycopg y segment-anything, y descarga SAM a `ml\weights\`.

### B.3 Preparar el índice de Nutrition5k (una vez)
Dentro de `ml\`:
```
cd ml
.venv\Scripts\python.exe descargar_nutrition5k.py --platos 1000
.venv\Scripts\python.exe n5k_map.py --clip
.venv\Scripts\python.exe build_index.py
```
1. El primero baja los metadatos y hasta 1000 fotos cenitales (unos 600 MB). Se puede cortar y volver a correr.
2. El segundo crea `n5k_map.csv`. Revisa las filas con `metodo=clip`; si alguna está mal, bórrala o agrégala bien en `n5k_map_manual.csv` y vuelve a correrlo.
3. El tercero crea `ml\index\` (unos minutos en CPU).

### B.4 Primer arranque manual (descarga CLIP y DINOv2)
```
.venv\Scripts\python.exe -m uvicorn vision_service:app --host 127.0.0.1 --port 8000
```
La primera vez descarga CLIP (unos 600 MB) y DINOv2 (90 MB) y calcula los textos del catálogo; puede tardar varios minutos. Las siguientes veces arranca en menos de un minuto. Cuando aparezca `Listo en ... s`, abre `http://localhost:8000/health`: debe decir `"status":"ok","model":"real"`. Ciérralo con `Ctrl+C`.

### B.5 Usarlo desde la app
En `camera.properties`: `vision.module=vision_service`. NutriTrack lo arranca solo al abrir la cámara (el registro queda en `ml\vision_service.log`). Cada foto tarda entre 10 y 40 segundos en CPU.

### B.6 Si cambian los alimentos
- Alimento nuevo en la BD: agrégale su fila en `catalog_aliases.csv` (sin alias, CLIP usa el nombre en español y acierta menos). `.venv\Scripts\python.exe catalog.py` muestra los que faltan.
- El servicio vuelve a leer la BD cada vez que arranca y guarda la copia en `catalog.csv`.

---

## Qué se sube al repositorio

| Sí se sube | No se sube (`.gitignore`) |
|---|---|
| `*.py`, `requirements*.txt`, `instalar*.bat`, `README.md` | `ml/.venv/`, `ml/weights/`, `ml/data/` |
| `catalog_aliases.csv`, `n5k_map_manual.csv` | `ml/index/`, `ml/catalog.csv`, `ml/n5k_map.csv` (se generan en cada PC) |

---

## Problemas frecuentes

| Problema | Solución |
|---|---|
| `No se encontró Python 3.14` | Instálalo y abre una terminal nueva; comprueba con `py -3.14 --version` |
| PowerShell no deja activar el entorno | No hace falta activarlo: usa `.venv\Scripts\python.exe ...` |
| `Falta ...sam_vit_b_01ec64.pth` | Vuelve a correr `ml\instalar_dev.bat` |
| `[catalogo] No se pudo leer la BD` | Revisa que PostgreSQL esté encendido y `db.properties`; mientras tanto usa `catalog.csv` |
| `Sin índice de Nutrition5k` en el log | Falta B.3; funciona, pero los gramos salen con menos confianza |
| La app dice que el servicio no responde | Mira `ml\vision_service.log`; el primer arranque debe hacerse a mano (B.4) |
| El puerto 8000 está ocupado | Cierra el otro servicio o cambia `vision.url` en `camera.properties` |

> Nutrition5k: Thames et al., CVPR 2021, licencia CC BY 4.0. Tabla de Composición de Alimentos Colombianos, ICBF 2018. Citarlos en el informe del proyecto.