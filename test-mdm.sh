#!/bin/bash

set -e

echo "🧪 Ejecutando pruebas en contenedores (stack de test dedicado, sin Node/npm local)..."

ROOT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$ROOT_DIR/docker"

if [ ! -f .env ]; then
  echo "📝 Archivo docker/.env no existe, creando desde .env.example"
  cp .env.example .env
fi

COMPOSE_FILE="docker-compose.test.yml"

echo "🚀 Levantando dependencias..."
docker compose -f "$COMPOSE_FILE" up -d postgres redis

echo "🧪 Backend tests (unit + integration + e2e)"
docker compose -f "$COMPOSE_FILE" run --rm backend-tests

echo "🧪 Frontend unit tests"
docker compose -f "$COMPOSE_FILE" run --rm frontend-tests

echo "✅ Pruebas de contenedor finalizadas correctamente"

echo "🧹 Limpiando servicios de test..."
docker compose -f "$COMPOSE_FILE" down

echo "🎉 Entorno de pruebas limpiado"
