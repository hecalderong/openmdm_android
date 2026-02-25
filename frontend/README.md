# OpenMDM-Android - Frontend Dashboard

Dashboard web administrativo construido con Vue 3 + TypeScript para el sistema de gestión de dispositivos móviles (MDM).

## 🚀 Características

- **Dashboard en tiempo real** con estadísticas de dispositivos
- **Gestión de dispositivos** - Visualización, bloqueo/desbloqueo, monitoreo
- **Gestión de aplicaciones** - Subida de APKs e instalación individual o masiva
- **Gestión de usuarios** - Control de acceso y permisos
- **WebSocket** - Actualizaciones en tiempo real del estado de dispositivos
- **UI Responsiva** - Compatible con escritorio, tablet y móvil
- **Autenticación JWT** - Seguridad integrada

## 🛠️ Stack Tecnológico

- **Framework**: Vue 3.3.8 (Composition API)
- **Lenguaje**: TypeScript 5.3
- **Build Tool**: Vite 5
- **Estado Global**: Pinia 2.1.7
- **Rutas**: Vue Router 4.2.5
- **HTTP Client**: Axios 1.6.2
- **WebSocket**: Socket.IO Client 4.6.0
- **Servidor Web**: Nginx (en producción)

## 📋 Prerrequisitos

- Node.js 18+ 
- npm o yarn
- Backend del MDM corriendo (puerto 3000)

## 🔧 Instalación

### Desarrollo Local

1. **Instalar dependencias**
   ```bash
   npm install
   ```

2. **Copiar variables de entorno**
   ```bash
   cp .env.example .env
   ```

3. **Configurar variables (opcional)**
   ```env
   VITE_API_BASE_URL=http://localhost:3000/api
   VITE_WS_URL=http://localhost:3000
   ```

4. **Ejecutar en modo desarrollo**
   ```bash
   npm run dev
   ```

5. **Abrir en el navegador**
   ```
   http://localhost:5173
   ```

### Build para Producción

```bash
# Construir aplicación
npm run build

# Previsualizar build
npm run preview
```

### Con Docker

```bash
# Construir imagen
docker build -t openmdm-android-frontend .

# Ejecutar contenedor
docker run -p 80:80 openmdm-android-frontend
```

## 📁 Estructura del Proyecto

```
frontend/
├── public/              # Archivos estáticos
├── src/
│   ├── assets/          # Estilos y recursos
│   │   └── main.css     # Estilos globales
│   ├── layouts/         # Layouts de páginas
│   │   └── DashboardLayout.vue
│   ├── router/          # Configuración de rutas
│   │   └── index.ts
│   ├── services/        # Servicios de API y WebSocket
│   │   ├── api.ts       # Cliente HTTP (Axios)
│   │   └── websocket.ts # Cliente WebSocket (Socket.IO)
│   ├── stores/          # Estado global (Pinia)
│   │   ├── auth.ts      # Autenticación
│   │   ├── devices.ts   # Dispositivos
│   │   └── apps.ts      # Aplicaciones
│   ├── views/           # Vistas/Páginas
│   │   ├── LoginView.vue
│   │   ├── DashboardView.vue
│   │   ├── DevicesView.vue
│   │   ├── DeviceDetailView.vue
│   │   ├── AppsView.vue
│   │   ├── UsersView.vue
│   │   └── SettingsView.vue
│   ├── App.vue          # Componente raíz
│   └── main.ts          # Punto de entrada
├── Dockerfile           # Configuración Docker
├── nginx.conf           # Configuración Nginx
├── package.json
├── tsconfig.json
├── vite.config.ts
└── README.md
```

## 🎨 Vistas Principales

### 1. **Login** (`/login`)
- Autenticación de usuarios
- Validación de credenciales
- Redirección automática si ya está autenticado

### 2. **Dashboard** (`/dashboard`)
- Estadísticas de dispositivos en tiempo real
- Gráficos de estado
- Dispositivos recientes
- Resumen de aplicaciones

### 3. **Dispositivos** (`/devices`)
- Lista completa de dispositivos
- Filtrado por estado
- Búsqueda por nombre o ID
- Acciones: Ver detalles, Bloquear/Desbloquear

### 4. **Detalle de Dispositivo** (`/devices/:id`)
- Información completa del dispositivo
- Indicadores de batería, almacenamiento y RAM
- Ubicación en mapa
- Registro de actividad

### 5. **Aplicaciones** (`/apps`)
- Lista de APKs disponibles
- Subir nuevas aplicaciones
- Instalación individual o masiva
- Eliminación de aplicaciones

### 6. **Usuarios** (`/users`)
- Gestión de usuarios del sistema
- Roles y permisos
- Estado de usuarios

### 7. **Configuración** (`/settings`)
- Configuración general del sistema
- Notificaciones
- Seguridad
- Preferencias de dispositivos

## 🔌 Integración con Backend

### API HTTP
El frontend se comunica con el backend a través de Axios:

```typescript
// Configuración automática en src/services/api.ts
baseURL: '/api'
headers: { Authorization: `Bearer ${token}` }
```

### Endpoints principales:
- `POST /auth/login` - Autenticación
- `POST /auth/register` - Registro
- `GET /devices` - Listar dispositivos
- `POST /devices/:id/lock` - Bloquear dispositivo
- `GET /apps` - Listar aplicaciones
- `POST /apps` - Subir APK
- `POST /apps/:id/install-multiple` - Instalación masiva

### WebSocket (Socket.IO)
Eventos en tiempo real:

**Emitidos por el frontend:**
- `device:register` - Registro de dispositivo
- `device:heartbeat` - Latido del corazón
- `device:location` - Actualización de ubicación

**Escuchados por el frontend:**
- `device:online` - Dispositivo conectado
- `device:offline` - Dispositivo desconectado
- `device:update` - Actualización de estado
- `device:stats` - Estadísticas actualizadas

## 🔐 Autenticación

El sistema usa JWT (JSON Web Tokens):

1. El usuario inicia sesión en `/login`
2. El backend devuelve un token JWT
3. El token se almacena en `localStorage`
4. Todas las peticiones incluyen el token en el header `Authorization`
5. El router protege rutas que requieren autenticación

## 🎯 Estado Global (Pinia)

### Auth Store
```typescript
const authStore = useAuthStore()
authStore.login({ email, password })
authStore.logout()
authStore.isAuthenticated
authStore.user
```

### Devices Store
```typescript
const devicesStore = useDevicesStore()
await devicesStore.fetchDevices()
await devicesStore.lockDevice(id)
await devicesStore.unlockDevice(id)
devicesStore.devices
devicesStore.stats
```

### Apps Store
```typescript
const appsStore = useAppsStore()
await appsStore.fetchApps()
await appsStore.uploadApp(file, onProgress)
await appsStore.installAppMultiple(appId, deviceIds)
appsStore.apps
```

## 🚢 Despliegue

### Con Docker Compose (Recomendado)

Desde el directorio raíz del proyecto:

```bash
cd docker
docker-compose up -d
```

Esto levantará:
- Frontend en `http://localhost:80`
- Backend en `http://localhost:3000`
- PostgreSQL en `http://localhost:5432`
- Redis en `http://localhost:6379`

### Nginx Manual

Si despliegas sin Docker:

1. **Build de producción**
   ```bash
   npm run build
   ```

2. **Copiar archivos a Nginx**
   ```bash
   cp -r dist/* /usr/share/nginx/html/
   ```

3. **Configurar proxy inverso**
   Usar el archivo `nginx.conf` incluido para proxy a la API

## 🧪 Scripts Disponibles

```bash
# Desarrollo
npm run dev          # Servidor de desarrollo

# Build
npm run build        # Construir para producción
npm run preview      # Previsualizar build

# Linting
npm run lint         # Verificar código
npm run format       # Formatear código

# TypeScript
npm run type-check   # Verificar tipos
```

## 🌐 Navegadores Soportados

- Chrome/Edge (últimas 2 versiones)
- Firefox (últimas 2 versiones)
- Safari (últimas 2 versiones)

## 📝 Credenciales por Defecto

```
Email: admin@openmdm.com
Password: Admin123!
```

*Cambiar estas credenciales en producción*

## 🐛 Troubleshooting

### Error de conexión con el backend
```bash
# Verificar que el backend esté corriendo
curl http://localhost:3000/api/health

# Verificar configuración CORS en backend
```

### WebSocket no conecta
```bash
# Verificar URL en src/services/websocket.ts
# Verificar que el puerto 3000 esté accesible
# Revisar configuración de nginx para /socket.io
```

### Build falla
```bash
# Limpiar node_modules y reinstalar
rm -rf node_modules package-lock.json
npm install

# Limpiar caché de Vite
rm -rf .vite
```

## 📞 Soporte

Para reportar problemas o solicitar características, ver el README principal del proyecto.

## 📄 Licencia

Este proyecto es parte del sistema OpenMDM-Android.
