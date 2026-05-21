#!/bin/bash

# ==========================================================================
# ACUÉRDATE — SCRIPT DE INICIO Y CONFIGURACIÓN MULTIPLATAFORMA (LINUX & TELÉFONOS)
# ==========================================================================

# Limpiar pantalla
clear

# Colores estéticos para la terminal
VIOLET='\033[1-38;5;135m'
CYAN='\033[0;36m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RESET='\033[0m'
BOLD='\033[1m'

echo -e "${VIOLET}${BOLD}=================================================================="
echo -e "                   ACUÉRDATE — INICIO DE SERVIDOR                 "
echo -e "==================================================================${RESET}"
echo -e "Cargando configuración de red y preparando el entorno..."
echo ""

# Intentar detectar la IP de red local en Linux
LOCAL_IP=$(hostname -I | awk '{print $1}')

if [ -z "$LOCAL_IP" ]; then
  # Fallback si hostname -I no da nada
  LOCAL_IP=$(ip route get 1.1.1.1 2>/dev/null | grep -oP 'src \K\S+' || echo "127.0.0.1")
fi

PORT=8001


echo -e "${GREEN}${BOLD}¡Todo listo! Servidor Web activado con éxito.${RESET}"
echo -e "------------------------------------------------------------------"
echo -e "${BOLD}1. EN ESTA COMPUTADORA (Linux):${RESET}"
echo -e "   Abre tu navegador e ingresa a:"
echo -e "   👉 ${CYAN}${BOLD}http://localhost:${PORT}${RESET}"
echo -e ""
echo -e "${BOLD}2. EN TU TELÉFONO MÓVIL (iPhone o Android):${RESET}"
echo -e "   Asegúrate de estar conectado al ${BOLD}mismo Wi-Fi${RESET} que esta PC."
echo -e "   Abre el navegador de tu celular e ingresa a:"
echo -e "   👉 ${VIOLET}${BOLD}http://${LOCAL_IP}:${PORT}${RESET}"
echo -e "------------------------------------------------------------------"
echo -e ""
echo -e "${YELLOW}${BOLD}📱 CÓMO INSTALAR COMO APLICACIÓN (PWA):${RESET}"
echo -e "   • ${BOLD}En iPhone (Safari):${RESET} Toca el botón de ${BOLD}Compartir${RESET} (caja con flecha arriba) "
echo -e "     y selecciona ${BOLD}'Agregar a la pantalla de inicio'${RESET}."
echo -e "   • ${BOLD}En Android (Chrome):${RESET} Toca los ${BOLD}tres puntos${RESET} de la esquina superior derecha"
echo -e "     y selecciona ${BOLD}'Agregar a la pantalla de inicio'${RESET} o ${BOLD}'Instalar aplicación'${RESET}."
echo -e ""
echo -e "Esto ocultará la barra del navegador, ejecutará la app en pantalla completa"
echo -e "con soporte offline completo y se sentirá 100% como una aplicación nativa."
echo -e "------------------------------------------------------------------"
echo -e "Presiona ${BOLD}Ctrl + C${RESET} para detener el servidor."
echo -e ""

# Ejecutar el servidor HTTP integrado de Python 3
python3 -m http.server $PORT
