# Backend (NestJS)

## Ubicación

- Código: `backend/src`
- Entrypoint: `backend/src/main.ts`
- Módulo raíz: `backend/src/app.module.ts`

## Módulos funcionales

- `modules/auth`: autenticación JWT.
- `modules/users`: administración de usuarios.
- `modules/devices`: inventario, estado y acciones remotas.
- `modules/apps`: gestión de APKs e instalaciones.
- `modules/monitoring`: comunicación WebSocket en tiempo real.

## API REST (resumen)

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

## WebSocket

### Cliente → servidor
- `device:register`
- `device:heartbeat`
- `device:location`

### Servidor → cliente
- `device:online`
- `device:offline`
- `device:update`
- `command`
- `stats`

## Persistencia

- PostgreSQL + TypeORM.
- Subida de APKs en `uploads/apks`.

## Seguridad

- JWT para autenticación.
- Roles de usuario (`admin`, `operator`, `viewer`).
- Validación de payloads y CORS configurable.
