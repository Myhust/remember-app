# ==========================================================================
# ACUÉRDATE — SCRIPT DE INICIO PARA WINDOWS (PowerShell)
# ==========================================================================

Clear-Host

# Colores estéticos
$Violet = "[1-38;5;135m"
$Cyan = "[0;36m"
$Green = "[0;32m"
$Yellow = "[1;33m"
$Reset = "[0m"
$Bold = "[1m"

Write-Host "==================================================================" -ForegroundColor Magenta
Write-Host "                   ACUÉRDATE — INICIO EN WINDOWS                  " -ForegroundColor Magenta
Write-Host "==================================================================" -ForegroundColor Magenta
Write-Host "Cargando configuración de red y preparando el entorno..."
Write-Host ""

# Intentar detectar la IP local activa en Windows
$LocalIP = "127.0.0.1"
try {
    # Filtrar adaptadores activos de IPv4 que no sean loopback o virtuales de docker/etc
    $IPAddresses = Get-NetIPAddress -AddressFamily IPv4 | 
                   Where-Object { $_.IPAddress -notlike "127.*" -and $_.IPAddress -notlike "169.254.*" -and $_.InterfaceAlias -notlike "*Loopback*" -and $_.InterfaceAlias -notlike "*vEthernet*" }
    
    if ($IPAddresses) {
        # Tomar la primera IP de la lista
        $LocalIP = $IPAddresses[0].IPAddress
    }
} catch {
    # Fallback si falla el comando moderno
    $ipconfig = ipconfig
    foreach ($line in $ipconfig) {
        if ($line -match "IPv4.*:\s*([0-9\.]+)") {
            $LocalIP = $Matches[1]
            break
        }
    }
}

$Port = 8001

Write-Host "¡Todo listo! Servidor Web activado con éxito en Windows." -ForegroundColor Green
Write-Host "------------------------------------------------------------------"
Write-Host "1. EN ESTA COMPUTADORA (Windows):" -ForegroundColor White -FontWeight Bold
Write-Host "   Abre tu navegador e ingresa a:"
Write-Host "   👉 http://localhost:$Port" -ForegroundColor Cyan
Write-Host ""
Write-Host "2. EN TU TELÉFONO MÓVIL (iPhone o Android):" -ForegroundColor White -FontWeight Bold
Write-Host "   Asegúrate de estar conectado al mismo Wi-Fi que esta PC."
Write-Host "   Abre el navegador de tu celular e ingresa a:"
Write-Host "   👉 http://$LocalIP:$Port" -ForegroundColor Magenta
Write-Host "------------------------------------------------------------------"
Write-Host ""
Write-Host "💻 CÓMO INSTALAR EN WINDOWS COMO APLICACIÓN DE ESCRITORIO (PWA):" -ForegroundColor Yellow -FontWeight Bold
Write-Host "   • Abre http://localhost:$Port en Google Chrome o Microsoft Edge."
Write-Host "   • En la barra de direcciones (al final, junto a la estrella de favoritos),"
Write-Host "     aparecerá un icono de una computadora con una flecha: 'Instalar Acuérdate'."
Write-Host "     Haz clic en él para agregarlo como App independiente en Windows."
Write-Host "   • Esto creará un acceso directo en tu escritorio de Windows y funcionará"
Write-Host "     en una ventana propia sin la barra del navegador, 100% como app nativa."
Write-Host "------------------------------------------------------------------"
Write-Host "Presiona Ctrl + C en esta ventana para detener el servidor."
Write-Host ""

# Buscar ejecutable de Python
$PythonCmd = "python"
$HasPython = Get-Command "python" -ErrorAction SilentlyContinue
if (-not $HasPython) {
    $HasPython3 = Get-Command "python3" -ErrorAction SilentlyContinue
    if ($HasPython3) {
        $PythonCmd = "python3"
    } else {
        Write-Host "⚠️ ADVERTENCIA: No se detectó Python instalado en Windows." -ForegroundColor Red
        Write-Host "Para correr el servidor local en Windows, necesitas tener Python instalado." -ForegroundColor Yellow
        Write-Host "Puedes descargarlo gratis desde la Microsoft Store o python.org." -ForegroundColor Yellow
        Write-Host "De lo contrario, puedes abrir directamente el archivo 'index.html' haciendo doble clic,"
        Write-Host "aunque las funciones de voz PWA requieren obligatoriamente correr sobre un servidor local (http://)." -ForegroundColor Red
        Write-Host ""
        Pause
        exit
    }
}

# Ejecutar el servidor
& $PythonCmd -m http.server $Port
