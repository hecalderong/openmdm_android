# OpenMDM-Android

Sistema MDM (Mobile Device Management) para administración remota de dispositivos Android con backend NestJS, dashboard web Vue 3 y app Android nativa en Java.

## Documentación

La documentación técnica está organizada en `docs/`:

- Índice general: [docs/README.md](docs/README.md)
- Puesta en marcha: [docs/getting-started.md](docs/getting-started.md)
- Arquitectura: [docs/architecture.md](docs/architecture.md)
- Backend: [docs/backend.md](docs/backend.md)
- Frontend: [docs/frontend.md](docs/frontend.md)
- Android: [docs/android.md](docs/android.md)

## Resumen Ejecutivo

- Gestión centralizada de dispositivos Android en tiempo real.
- Instalación individual y masiva de APKs.
- Control remoto (bloqueo/desbloqueo y ejecución de comandos).
- Monitoreo de batería, RAM, almacenamiento, conectividad y ubicación.
- Arquitectura dockerizada para despliegue rápido y reproducible.

## Estado Actual del Proyecto

El sistema está funcional para operación base de MDM y pruebas de campo:

- Backend API + WebSocket operativos.
- Dashboard operativo para monitoreo y acciones principales.
- App Android conectando y reportando estado periódicamente.
- Flujo de carga e instalación de APK funcionando.

Pendientes de UI (detectados en documentación técnica del dashboard):

- Visualización detallada de apps instaladas por dispositivo.
- Estado de instalación por dispositivo más explícito en la interfaz.
- Vista avanzada de logs del dispositivo y mapa real.
- Funciones avanzadas (kiosk/políticas/factory reset) en UI.

## Arquitectura

```text
Android Devices (MDM App)
        │  (WebSocket + HTTP)
        ▼
Backend NestJS (REST API + Gateway WS)
        │
   ┌────┴───────────────┐
   ▼                    ▼
PostgreSQL            Redis
 (datos)            (cache/sesiones)
        │
        ▼
Frontend Dashboard (Vue 3)
```

### Componentes

- `backend/`: API REST, autenticación JWT, módulos de negocio (`auth`, `users`, `devices`, `apps`, `monitoring`).
- `frontend/`: Dashboard administrativo (Vue 3 + TypeScript + Pinia + Socket.IO).
- `android-app/`: Cliente MDM Android (Java, Device Admin, foreground service).
- `docker/`: Orquestación de backend + BD + cache.

## Stack Tecnológico

### Backend
- Node.js 18+
- NestJS 10
- TypeORM 0.3
- PostgreSQL 15
- Redis 7
- Socket.IO 4

### Frontend
- Vue 3
- TypeScript
- Vite
- Pinia
- Axios
- Socket.IO Client

### Android
- Java 8
- Android API 24+
- Device Admin API
- WorkManager / servicios en primer plano

## Instalación Rápida

### Requisitos

- Docker 20.10+
- Docker Compose 2+
- Android Studio + SDK Android (si vas a compilar la app)

### Levantar plataforma

```bash
cd docker
cp .env.example .env
cd ..
./start-mdm.sh
```

Servicios principales:

- API: `http://localhost:3000`
- Dashboard: `http://localhost:80`
- WebSocket: `ws://localhost:3000`

### Crear usuario administrador

```bash
curl -X POST http://localhost:3000/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email":"admin@openmdm.com",
    "password":"admin123",
    "name":"Administrador"
  }'
```

### Compilar e instalar app Android

```bash
cd android-app
./gradlew assembleDebug
adb install app/build/outputs/apk/debug/app-debug.apk
```

## Operación Básica

### 1) Login y token

```bash
curl -X POST http://localhost:3000/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email":"admin@openmdm.com",
    "password":"admin123"
  }'
```

### 2) Ver dispositivos

```bash
curl -H "Authorization: Bearer TU_TOKEN" \
  http://localhost:3000/devices
```

### 3) Subir APK

```bash
curl -X POST http://localhost:3000/apps/upload \
  -H "Authorization: Bearer TU_TOKEN" \
  -F "apk=@/ruta/app.apk" \
  -F "name=Mi App" \
  -F "packageName=com.example.app" \
  -F "version=1.0.0" \
  -F "versionCode=1"
```

### 4) Instalar APK en un dispositivo

```bash
curl -X POST \
  -H "Authorization: Bearer TU_TOKEN" \
  http://localhost:3000/apps/APP_ID/install/DEVICE_ID
```

### 5) Instalación masiva

```bash
curl -X POST http://localhost:3000/apps/APP_ID/install-multiple \
  -H "Authorization: Bearer TU_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"deviceIds":["device1","device2","device3"]}'
```

## Testing

### Ejecutar pruebas sin instalaciones locales

```bash
./test-mdm.sh
```

Esto corre en contenedores:

- Backend: unit + integration + e2e
- Frontend: unit (Vitest)

### Android

```bash
cd android-app
./gradlew testDebugUnitTest
```

Test instrumentado (requiere emulador/dispositivo):

```bash
cd android-app
./gradlew connectedDebugAndroidTest
```

CI automática en GitHub Actions: `.github/workflows/ci.yml`.

## API (Resumen)

### Auth
- `POST /auth/register`
- `POST /auth/login`

### Devices
- `GET /devices`
- `GET /devices/:id`
- `POST /devices/register`
- `PUT /devices/:id`
- `PUT /devices/:id/lock`
- `PUT /devices/:id/unlock`
- `GET /devices/:id/logs`
- `GET /devices/stats`
- `DELETE /devices/:id`

### Apps
- `GET /apps`
- `GET /apps/:id`
- `POST /apps/upload`
- `PUT /apps/:id`
- `DELETE /apps/:id`
- `POST /apps/:appId/install/:deviceId`
- `POST /apps/:appId/install-multiple`
- `DELETE /apps/:appId/uninstall/:deviceId`
- `GET /apps/device/:deviceId`
- `GET /apps/device/:deviceId/pending`
- `PUT /apps/:appId/status/:deviceId`

### Users
- `GET /users`
- `GET /users/:id`
- `PUT /users/:id`
- `DELETE /users/:id`

## WebSocket (Resumen)

### Cliente → Servidor
- `device:register`
- `device:heartbeat`
- `device:location`

### Servidor → Cliente
- `device:online`
- `device:offline`
- `device:update`
- `command`
- `stats`

## Seguridad y Producción

Checklist mínimo:

- Cambiar `JWT_SECRET`.
- Cambiar credenciales de PostgreSQL.
- Publicar por HTTPS/WSS (no HTTP/WS en producción).
- Restringir puertos con firewall.
- Configurar backups automáticos de PostgreSQL.
- Monitorear logs y rotación.

## Troubleshooting Rápido

### Ver logs

```bash
docker-compose -f docker/docker-compose.yml logs -f
```

### Reiniciar backend

```bash
docker-compose -f docker/docker-compose.yml restart backend
```

### Backup de BD

```bash
docker exec openmdm_android_db pg_dump -U mdm_user openmdm_android > backup.sql
```

### Detener todo

```bash
./stop-mdm.sh
```

## Estructura del Repositorio

```text
openmdm-android/
├── docs/
├── backend/
├── frontend/
├── android-app/
├── docker/
├── uploads/
├── start-mdm.sh
├── stop-mdm.sh
└── README.md
```

## Convención de documentación

- `README.md` queda como resumen ejecutivo y guía rápida.
- `docs/` concentra detalle técnico por componente para mantenimiento ordenado.

## Licencia

Propietario - OpenMDM.
