# Arquitectura del sistema

## Visión general

```text
Android Devices (MDM App)
        │  (WebSocket + HTTP)
        ▼
Backend NestJS (REST API + Gateway WS)
        │
   ┌────┴───────────────┐
   ▼                    ▼
PostgreSQL            Redis
        │
        ▼
Frontend Dashboard (Vue 3)
```

## Componentes y responsabilidades

- **Backend (`backend/`)**
  - API REST para autenticación, usuarios, dispositivos y aplicaciones.
  - Gateway WebSocket para eventos y comandos en tiempo real.
  - Persistencia con TypeORM + PostgreSQL.

- **Frontend (`frontend/`)**
  - UI administrativa para monitoreo, operación y gestión.
  - Integración HTTP (Axios) + WebSocket (Socket.IO client).

- **Android App (`android-app/`)**
  - Servicio MDM persistente en foreground.
  - Reporte periódico de estado (heartbeat).
  - Recepción y ejecución de comandos remotos.

## Flujo principal (instalación de app)

1. Operador solicita instalación desde frontend.
2. Backend registra la operación y envía comando por WebSocket.
3. Dispositivo Android descarga/instala APK.
4. Dispositivo reporta estado de instalación al backend.
5. Dashboard refleja estado actualizado.

## Entidades principales

- `users`: usuarios y roles del sistema.
- `devices`: inventario de dispositivos y telemetría.
- `device_logs`: eventos y trazas de actividad.
- `apps`: catálogo de APKs disponibles.
- `device_apps`: estado de instalación por dispositivo.

## Referencias de implementación

- Backend root module: `backend/src/app.module.ts`
- Módulo de dispositivos: `backend/src/modules/devices`
- Módulo de apps: `backend/src/modules/apps`
- WebSocket gateway: `backend/src/modules/monitoring`
- App layout dashboard: `frontend/src/layouts/DashboardLayout.vue`
- Servicio Android: `android-app/app/src/main/java/com/openmdm/mdm/service/MDMService.java`
