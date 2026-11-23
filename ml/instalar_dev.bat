@echo off
REM ============================================================
REM  Instalador del servicio de vision (Python 3.14, solo CPU).
REM  Crea el entorno virtual, instala las librerias y descarga SAM.
REM  Uso:  ml\instalar_dev.bat        -> servicio real (unos 2 GB)
REM        ml\instalar_dev.bat stub   -> solo el stub (pocos MB)
REM ============================================================
setlocal
cd /d "%~dp0"

set "PY="
where py >nul 2>nul && py -3.14 --version >nul 2>nul && set "PY=py -3.14"
if not defined PY (
    where python >nul 2>nul && python --version 2>nul | findstr /b /c:"Python 3.14" >nul && set "PY=python"
)
if not defined PY (
    echo No se encontro Python 3.14. Instalalo desde python.org marcando "Add python.exe to PATH".
    goto :error
)

if exist ".venv" if not exist ".venv\Scripts\python.exe" (
    echo El entorno virtual esta incompleto. Se borra y se crea de nuevo...
    rmdir /s /q ".venv" || goto :error
)
if not exist ".venv" (
    echo Creando entorno virtual con %PY%...
    %PY% -m venv .venv || goto :error
)

set "VPY=.venv\Scripts\python.exe"
"%VPY%" -m pip install --upgrade pip || goto :error

where git >nul 2>nul
if errorlevel 1 if /i not "%1"=="stub" (
    echo Falta Git: se necesita para instalar segment-anything. Instala Git y vuelve a correr esto.
    goto :error
)

if /i "%1"=="stub" (
    echo Instalando solo las librerias del stub...
    "%VPY%" -m pip install -r requirements-stub.txt || goto :error
) else (
    echo Instalando las librerias del servicio real ^(unos 2 GB, puede tardar^)...
    "%VPY%" -m pip install -r requirements.txt || goto :error
    if not exist "weights" mkdir weights
    if not exist "weights\sam_vit_b_01ec64.pth" (
        echo Descargando el modelo SAM ^(375 MB^)...
        powershell -NoProfile -Command "$ProgressPreference='SilentlyContinue'; Invoke-WebRequest -Uri https://dl.fbaipublicfiles.com/segment_anything/sam_vit_b_01ec64.pth -OutFile weights\sam_vit_b_01ec64.pth.part" || goto :error
        move /y "weights\sam_vit_b_01ec64.pth.part" "weights\sam_vit_b_01ec64.pth" >nul || goto :error
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