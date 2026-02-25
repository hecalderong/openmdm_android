#!/bin/bash

# Script para iniciar el sistema MDM con Docker

echo "🚀 Iniciando OpenMDM-Android..."

# Verificar que Docker está instalado
if ! command -v docker &> /dev/null; then
    echo "❌ Docker no está instalado. Por favor instala Docker primero."
    exit 1
fi

if ! command -v docker-compose &> /dev/null; then
    echo "❌ Docker Compose no está instalado. Por favor instala Docker Compose primero."
    exit 1
fi

# Navegar al directorio docker
cd "$(dirname "$0")/docker" || exit

# Copiar archivo de entorno si no existe
if [ ! -f .env ]; then
    echo "📝 Creando archivo .env desde .env.example..."
    cp .env.example .env
    echo "⚠️  Por favor edita el archivo docker/.env y configura las variables necesarias"
    read -p "Presiona Enter cuando hayas configurado el archivo .env..."
fi

# Construir e iniciar los contenedores
echo "🔨 Construyendo contenedores..."
docker-compose build

echo "▶️  Iniciando servicios..."
docker-compose up -d

# Esperar a que los servicios estén listos
echo "⏳ Esperando a que los servicios estén listos..."
sleep 10

# Verificar estado de los servicios
echo ""
echo "📊 Estado de los servicios:"
docker-compose ps

echo ""
echo "✅ Sistema MDM iniciado correctamente!"
echo ""
echo "🌐 Backend API: http://localhost:3000"
echo "🔌 WebSocket: ws://localhost:3000"
echo "🗄️  PostgreSQL: localhost:5432"
echo "🔴 Redis: localhost:6379"
echo ""
echo "📝 Para ver los logs: docker-compose -f docker/docker-compose.yml logs -f"
echo "🛑 Para detener: docker-compose -f docker/docker-compose.yml down"
