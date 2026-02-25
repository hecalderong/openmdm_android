#!/bin/bash

# Script para detener el sistema MDM

echo "🛑 Deteniendo OpenMDM-Android..."

cd "$(dirname "$0")/docker" || exit

docker-compose down

echo "✅ Sistema MDM detenido"
