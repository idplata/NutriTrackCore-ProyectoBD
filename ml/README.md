# Servicio de visión de NutriTrack Core (`ml/`)

Programa en Python que reconoce los alimentos de una foto y estima sus gramos. NutriTrack (Java) le envía la foto por HTTP en `http://127.0.0.1:8000` y recibe los alimentos, los gramos y la confianza.

Hay tres formas de usar esta carpeta:

| Quién | Qué necesita | Sección |
|---|---|---|
| **Usuario sin Python** (solo quiere usar la cámara) | Clonar el repo y ejecutar `instalar.bat` | [A](#a-usar-la-cámara-sin-python) |
| **Desarrollador** (va a modificar el servicio) | Python 3.11 | [B](#b-instalación-para-desarrolladores) |
| **Responsable de publicar** (genera el `.exe`) | Todo lo de B + PyInstaller | [C](#c-publicar-el-servicio-para-usuarios-sin-python) |

---

## A. Usar la cámara sin Python

**Requisitos:** Windows 10 u 11, Java, PostgreSQL configurado para NutriTrack, internet la primera vez y unos 3 GB libres.

1. **Clonar el repositorio:**
   ```
   git clone https://github.com/idplata/NutriTrackCore-ProyectoBD.git
   cd NutriTrackCore-ProyectoBD
   ```
2. **Instalar el servicio de visión:** doble clic en `ml\instalar.bat`. Descarga el servicio ya empaquetado (1 GB o más) y lo deja en `ml\vision_service\`.
3. **Arrancar el servicio:** doble clic en `ml\vision_service\vision_service.exe`. Se abre una ventana negra: déjala abierta. La primera vez tarda entre 10 y 30 segundos.
   > Si Windows Defender lo bloquea: **Más información → Ejecutar de todas formas**.
4. **Comprobar:** abre `http://localhost:8000/health` en el navegador. Debe mostrar `{"status":"ok"...}`.
5. **Abrir NutriTrack** y usar **Consumo → Registrar con foto**.
6. **Primera vez con el celular:**
   - Cuando Windows pregunte por el firewall, marca **Redes privadas** y acepta.
   - El celular debe estar en la **misma red Wi-Fi** que el PC. En la universidad, usa el **punto de acceso del celular**.
   - La red Wi-Fi del PC debe estar como **Privada** (Configuración → Red e Internet → Wi-Fi → tu red).

> Cuando NutriTrack incluya `VisionServiceLauncher`, el paso 3 deja de ser necesario: la app arrancará el servicio sola.

---

## B. Instalación para desarrolladores

### B.1 Instalar Python 3.11

1. Descarga el instalador de 64 bits: https://www.python.org/ftp/python/3.11.9/python-3.11.9-amd64.exe
   (página oficial: https://www.python.org/downloads/release/python-3119/)
2. Ábrelo y, **en la primera pantalla**, marca **Add python.exe to PATH**. Luego **Install Now**.
   pip viene incluido; no se instala aparte.
3. Abre una terminal **nueva** (cmd o PowerShell) y comprueba:
   ```
   python --version
   pip --version
   ```
   Debe decir `Python 3.11.9`. Si `python` no se reconoce, usa `py` en su lugar (por ejemplo `py --version`).

### B.2 Instalación rápida (recomendada)

Desde la raíz del repositorio:
```
ml\instalar_dev.bat stub
```
instala solo lo necesario para el **stub** (unos 50 MB). Para el servicio real, con PyTorch, CLIP, DINOv2 y SAM (unos 2 GB):
```
ml\instalar_dev.bat
```
El instalador crea `ml\.venv`, instala las librerías y descarga el peso de SAM a `ml\weights\`. Si funcionó, salta a [B.4](#b4-arrancar-y-probar).

### B.3 Instalación manual (paso a paso)

**1. Entrar a la carpeta y crear el entorno virtual** (solo la primera vez):
```
cd ml
python -m venv .venv
```

**2. Activar el entorno** (cada vez que abras una terminal nueva):
- cmd: `.venv\Scripts\activate`
- PowerShell: `.venv\Scripts\Activate.ps1`

Al activarlo aparece `(.venv)` al inicio de la línea.
> Si PowerShell dice que la ejecución de scripts está deshabilitada:
> `Set-ExecutionPolicy -Scope CurrentUser RemoteSigned` y vuelve a intentar.

**3. Actualizar pip:**
```
python -m pip install --upgrade pip
```

**4. Instalar las librerías.** Solo el stub:
```
pip install -r requirements-stub.txt
```
O el servicio real completo (PyTorch CPU, open_clip, transformers y segment-anything):
```
pip install -r requirements.txt
```

**5. Descargar el peso de SAM** (solo para el servicio real; 375 MB):
```
mkdir weights
powershell -Command "Invoke-WebRequest -Uri https://dl.fbaipublicfiles.com/segment_anything/sam_vit_b_01ec64.pth -OutFile weights\sam_vit_b_01ec64.pth"
```
Los pesos de CLIP y DINOv2 se descargan solos la primera vez que arranca el servicio real.

**6. Comprobar las librerías del servicio real:**
```
python -c "import torch, open_clip, transformers, segment_anything; print('OK', torch.__version__)"
```

**7. (Solo si vas a reconstruir el índice) Descargar la muestra de Nutrition5k.** El repo ya trae el índice en `ml\index\`, así que normalmente **no hace falta**.
```
python descargar_nutrition5k.py --destino data\nutrition5k --platos 300
python build_index.py
```
Repositorio del dataset: https://github.com/google-research-datasets/Nutrition5k

### B.4 Arrancar y probar

Con el entorno activo, dentro de `ml\`:
```
uvicorn stub_service:app --host 127.0.0.1 --port 8000
```
(o `vision_service:app` para el servicio real). Comprueba:
- Navegador: `http://localhost:8000/health`
- Con una foto: `curl -X POST --data-binary @plato.jpg -H "Content-Type: image/jpeg" http://localhost:8000/analyze`

### B.5 Librerías Java (en el `pom.xml` de la raíz)

Dentro de `<dependencies>` (o la versión más reciente de Maven Central):
```xml
<!-- Leer el JSON del servicio de visión -->
<dependency>
    <groupId>com.google.code.gson</groupId>
    <artifactId>gson</artifactId>
    <version>2.11.0</version>
</dependency>
<!-- QR del enlace para el celular (opcional) -->
<dependency>
    <groupId>com.google.zxing</groupId>
    <artifactId>core</artifactId>
    <version>3.5.3</version>
</dependency>
<dependency>
    <groupId>com.google.zxing</groupId>
    <artifactId>javase</artifactId>
    <version>3.5.3</version>
</dependency>
```
- https://central.sonatype.com/artifact/com.google.code.gson/gson
- https://central.sonatype.com/artifact/com.google.zxing/core

---

## C. Publicar el servicio para usuarios sin Python

Se hace una vez por versión, en Windows, con el servicio ya funcionando en B.4.

1. **Que el servicio arranque solo.** Al final de `vision_service.py`:
   ```python
   if __name__ == "__main__":
       import uvicorn
       uvicorn.run(app, host="127.0.0.1", port=8000)
   ```
2. **Instalar PyInstaller** (con el entorno activo):
   ```
   pip install pyinstaller
   ```
3. **Generar el paquete:**
   ```
   pyinstaller vision_service.py --onedir --name vision_service --noconfirm ^
       --collect-all open_clip --collect-all segment_anything ^
       --collect-all transformers --collect-all uvicorn
   ```
   Resultado: `ml\dist\vision_service\vision_service.exe`.
4. **Copiar junto al `.exe` los archivos editables** (así se pueden cambiar sin regenerar):
   ```
   xcopy /E /I index dist\vision_service\index
   xcopy /E /I weights dist\vision_service\weights
   copy catalog_aliases.csv dist\vision_service\
   copy ingredient_mapping.csv dist\vision_service\
   ```
5. **Probar** `dist\vision_service\vision_service.exe` y abrir `http://localhost:8000/health`.
   Si aparece `ModuleNotFoundError: X`, agrega `--hidden-import X` en el paso 3.
6. **Probar en un PC sin Python.** Es la prueba que más importa.
7. **Comprimir:**
   ```
   powershell Compress-Archive -Path dist\vision_service -DestinationPath vision_service.zip
   ```
8. **Publicar en GitHub:** Releases → **Draft a new release** → tag `vision-v1.0` → adjuntar `vision_service.zip` → **Publish release**.
9. Si cambió la versión, actualiza la URL en `ml\instalar.bat`.

**Tamaño esperado:** 1 a 1,5 GB comprimido con el servicio real (sobre todo por PyTorch y los pesos). El stub empaquetado pesa 30 a 50 MB.

---

## Qué se sube al repositorio

| Sí se sube | No se sube (`.gitignore`) |
|---|---|
| `*.py`, `requirements*.txt`, `instalar*.bat`, `README.md` | `ml/.venv/` |
| `catalog_aliases.csv`, `ingredient_mapping.csv` | `ml/data/` (Nutrition5k) |
| `ml/index/` (índice ya calculado, pocos MB) | `ml/weights/` (SAM) |
| | `ml/dist/`, `ml/build/`, `ml/*.spec`, `ml/vision_service/` |

---

## Problemas frecuentes

| Problema | Solución |
|---|---|
| `python` no se reconoce | Cierra y abre la terminal; si sigue, usa `py`. O reinstala marcando **Add to PATH** |
| PowerShell no deja activar el entorno | `Set-ExecutionPolicy -Scope CurrentUser RemoteSigned` |
| `pip install` de torch tarda mucho | Es normal: descarga unos 800 MB |
| El puerto 8000 está ocupado | Cierra el otro servicio o cambia el puerto (y `vision.url` en `camera.properties`) |
| El celular no abre el enlace | Misma red, red **Privada**, firewall con Java permitido; en la universidad, punto de acceso del celular |
| Windows Defender bloquea el `.exe` | Más información → Ejecutar de todas formas |

> Nutrition5k: Thames et al., CVPR 2021, licencia CC BY 4.0. Citarlo en el informe del proyecto.
