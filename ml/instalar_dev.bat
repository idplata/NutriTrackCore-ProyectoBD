@echo off
REM ============================================================
REM  Instalador para DESARROLLADORES (requiere Python 3.11).
REM  Crea el entorno virtual, instala las librerias y descarga SAM.
REM  Uso: doble clic, o desde la terminal: ml\instalar_dev.bat
REM ============================================================
setlocal
cd /d "%~dp0"

where py >nul 2>nul
if errorlevel 1 (
    echo No se encontro Python. Instala Python 3.11 marcando "Add Python to PATH".
    pause
    exit /b 1
)

if not exist ".venv" (
    echo Creando entorno virtual...
    py -3.11 -m venv .venv || python -m venv .venv
)

call ".venv\Scripts\activate.bat"
python -m pip install --upgrade pip

if /i "%1"=="stub" (
    echo Instalando solo las librerias del stub...
    pip install -r requirements-stub.txt || goto :error
) else (
    echo Instalando todas las librerias del servicio real ^(unos 2 GB^)...
    pip install -r requirements.txt || goto :error
    if not exist "weights" mkdir weights
    if not exist "weights\sam_vit_b_01ec64.pth" (
        echo Descargando el modelo SAM ^(375 MB^)...
        powershell -NoProfile -Command "Invoke-WebRequest -Uri https://dl.fbaipublicfiles.com/segment_anything/sam_vit_b_01ec64.pth -OutFile weights\sam_vit_b_01ec64.pth" || goto :error
    )
)

echo.
echo Instalacion terminada.
pause
exit /b 0

:error
echo.
echo Hubo un error durante la instalacion. Revisa los mensajes de arriba.
pause
exit /b 1
