# Puesta en marcha

## Requisitos

- Docker 20.10+
- Docker Compose 2+
- Android Studio + SDK API 24+ (si se compila la app)

## Levantar el sistema

```bash
cd docker
cp .env.example .env
cd ..
./start-mdm.sh
```

Servicios esperados:

- API REST: `http://localhost:3000`
- Dashboard: `http://localhost:80`
- WebSocket: `ws://localhost:3000`

## Crear usuario administrador

```bash
curl -X POST http://localhost:3000/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email":"admin@openmdm.com",
    "password":"admin123",
    "name":"Administrador"
  }'
```

## Compilar e instalar Android app

```bash
cd android-app
./gradlew assembleDebug
adb install app/build/outputs/apk/debug/app-debug.apk
```

## Operaciones frecuentes

### Login

```bash
curl -X POST http://localhost:3000/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email":"admin@openmdm.com",
    "password":"admin123"
  }'
```

### Ver dispositivos

```bash
curl -H "Authorization: Bearer TU_TOKEN" http://localhost:3000/devices
```

### Subir APK

```bash
curl -X POST http://localhost:3000/apps/upload \
  -H "Authorization: Bearer TU_TOKEN" \
  -F "apk=@/ruta/app.apk" \
  -F "name=Mi App" \
  -F "packageName=com.example.app" \
  -F "version=1.0.0" \
  -F "versionCode=1"
```

## Mantenimiento

```bash
# Logs
docker-compose -f docker/docker-compose.yml logs -f

# Reiniciar backend
docker-compose -f docker/docker-compose.yml restart backend

# Backup PostgreSQL
docker exec openmdm_android_db pg_dump -U mdm_user openmdm_android > backup.sql

# Detener plataforma
./stop-mdm.sh
```

## Seguridad mínima para producción

- Cambiar `JWT_SECRET`.
- Cambiar credenciales de PostgreSQL.
- Usar HTTPS/WSS.
- Limitar puertos con firewall.
- Configurar backup automático y monitoreo de logs.

## Testing recomendado antes de publicar

### Backend + Frontend (sin instalar Node local)

```bash
./test-mdm.sh
```

### Android

```bash
cd android-app
./gradlew testDebugUnitTest
```

Instrumentación (requiere emulador/dispositivo):

```bash
cd android-app
./gradlew connectedDebugAndroidTest
```
