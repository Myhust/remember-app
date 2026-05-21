@echo off
:: ==========================================================================
# ACUÉRDATE — LANZADOR DOBLE CLIC PARA WINDOWS
# ==========================================================================
TITLE Acuerdate - Lanzador de Servidor Local

:: Ejecutar PowerShell evadiendo restricciones de políticas locales
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0start.ps1"

if %errorlevel% neq 0 (
    echo.
    echo Ocurrio un error al intentar iniciar el servidor a traves de PowerShell.
    echo Asegurate de tener instalado Python en tu equipo Windows.
    echo.
    pause
)
