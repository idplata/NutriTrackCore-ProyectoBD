@echo off
REM ============================================================
REM  Instalador para USUARIOS SIN PYTHON.
REM  Descarga el servicio de vision ya empaquetado desde GitHub Releases.
REM  Uso: doble clic, o desde la terminal: ml\instalar.bat
REM ============================================================
setlocal
cd /d "%~dp0"

REM Cambiar esta URL cada vez que se publique una version nueva
set "URL=https://github.com/idplata/NutriTrackCore-ProyectoBD/releases/download/vision-v1.0/vision_service.zip"

if exist "vision_service\vision_service.exe" (
    echo El servicio de vision ya esta instalado en ml\vision_service\.
    echo Para actualizarlo, borra esa carpeta y vuelve a ejecutar este instalador.
    pause
    exit /b 0
)

echo Descargando el servicio de vision ^(puede pesar 1 GB o mas^)...
powershell -NoProfile -Command "$ProgressPreference='SilentlyContinue'; Invoke-WebRequest -Uri '%URL%' -OutFile 'vision_service.zip'" || goto :error

echo Descomprimiendo...
powershell -NoProfile -Command "Expand-Archive -Path 'vision_service.zip' -DestinationPath '.' -Force" || goto :error
del "vision_service.zip"

echo.
echo Listo. El servicio quedo en ml\vision_service\vision_service.exe
pause
exit /b 0

:error
echo.
echo No se pudo descargar o descomprimir el servicio. Revisa tu conexion y la URL.
pause
exit /b 1
