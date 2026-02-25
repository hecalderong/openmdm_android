# Frontend Dashboard (Vue 3)

## Ubicación

- Código: `frontend/src`
- Entrypoint: `frontend/src/main.ts`
- Router: `frontend/src/router/index.ts`
- Layout principal: `frontend/src/layouts/DashboardLayout.vue`

## Stack

- Vue 3 + TypeScript
- Vite
- Pinia (estado global)
- Axios (REST)
- Socket.IO client (tiempo real)

## Vistas principales

- `views/LoginView.vue`: autenticación.
- `views/DashboardView.vue`: resumen general y métricas.
- `views/DevicesView.vue`: listado y estado de dispositivos.
- `views/DeviceDetailView.vue`: detalle operativo de dispositivo.
- `views/AppsView.vue`: catálogo/subida/instalación de APKs.
- `views/UsersView.vue`: gestión de usuarios.
- `views/SettingsView.vue`: configuración general.

## Stores

- `stores/auth.ts`
- `stores/devices.ts`
- `stores/apps.ts`
- `stores/users.ts`
- `stores/settings.ts`

## Integraciones

- API client: `services/api.ts`
- WebSocket client: `services/websocket.ts`

## Estado funcional documentado

Funcionalidades base operativas:

- Monitoreo de dispositivos en tiempo real.
- Bloqueo/desbloqueo remoto.
- Subida de APKs.
- Instalación en uno o varios dispositivos.

Pendientes de UI reportados:

- Vista detallada de apps instaladas por dispositivo.
- Estado de instalación más visible por dispositivo.
- Logs avanzados en UI y mapa integrado real.
- Funciones avanzadas (kiosk/políticas/factory reset) en frontend.
