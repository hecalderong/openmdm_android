# OpenMDM-Android - Android App

Aplicación Android nativa en Java para el sistema MDM de OpenMDM.

## Características

- ✅ Device Admin API para control del dispositivo
- ✅ Conexión WebSocket en tiempo real
- ✅ Monitoreo de batería, almacenamiento y RAM
- ✅ Instalación/desinstalación remota de APKs
- ✅ Bloqueo remoto del dispositivo
- ✅ Servicio en segundo plano persistente
- ✅ Inicio automático al arrancar el dispositivo
- ✅ Reporte de ubicación GPS

## Requisitos

- Android 7.0 (API 24) o superior
- Permisos de Device Administrator
- Conexión a Internet

## Compilación

### Debug Build
```bash
./gradlew assembleDebug
```

APK: `app/build/outputs/apk/debug/app-debug.apk`

### Release Build
```bash
./gradlew assembleRelease
```

APK: `app/build/outputs/apk/release/app-release.apk`

## Configuración

Editar `app/build.gradle` para configurar las URLs del servidor:

```gradle
buildConfigField "String", "API_BASE_URL", "\"http://your-server.com:3000\""
buildConfigField "String", "WS_URL", "\"ws://your-server.com:3000\""
```

## Instalación

1. Instalar el APK en el dispositivo
2. Abrir la aplicación
3. Presionar "Habilitar Administración"
4. Aceptar los permisos de Device Admin
5. Presionar "Iniciar Servicio MDM"

## Permisos

La aplicación requiere los siguientes permisos:

- `INTERNET` - Comunicación con el servidor
- `ACCESS_NETWORK_STATE` - Estado de la red
- `ACCESS_WIFI_STATE` - Información WiFi
- `ACCESS_FINE_LOCATION` - Ubicación GPS
- `REQUEST_INSTALL_PACKAGES` - Instalación de APKs
- `QUERY_ALL_PACKAGES` - Listar aplicaciones
- `BATTERY_STATS` - Estado de batería
- `WAKE_LOCK` - Mantener servicio activo
- `FOREGROUND_SERVICE` - Servicio en primer plano
- `RECEIVE_BOOT_COMPLETED` - Inicio automático

## Arquitectura

```
com.androidmdm.app/
├── MDMApplication.java          # Application class
├── admin/
│   └── MDMDeviceAdminReceiver.java  # Device Admin receiver
├── network/
│   └── WebSocketManager.java    # WebSocket communication
├── receiver/
│   └── BootReceiver.java        # Boot receiver
├── service/
│   └── MDMService.java          # Main MDM service
├── ui/
│   └── MainActivity.java        # Main activity
└── util/
    └── DeviceInfoUtil.java      # Device information utilities
```

## Funcionalidades del Device Admin

- Bloqueo remoto del dispositivo
- Restablecimiento de contraseña
- Borrado remoto de datos
- Desactivación de cámara
- Políticas de contraseña
- Almacenamiento cifrado

## Testing Local

Para testing en emulador o dispositivo USB, usar la IP de la máquina host:

```gradle
buildConfigField "String", "API_BASE_URL", "\"http://10.0.2.2:3000\""
buildConfigField "String", "WS_URL", "\"ws://10.0.2.2:3000\""
```

Nota: `10.0.2.2` es el localhost del host desde el emulador Android.

## Troubleshooting

### App no se conecta al servidor
- Verificar que el servidor está ejecutándose
- Verificar URLs en `build.gradle`
- Verificar permisos de Internet
- Verificar firewall del servidor

### Permisos de Device Admin no se activan
- Verificar que el dispositivo permite apps de fuentes desconocidas
- Algunas ROMs personalizadas tienen restricciones adicionales
- Dispositivos Samsung/Xiaomi pueden requerir pasos adicionales

### Servicio se detiene
- Verificar que la app no está optimizada para ahorro de batería
- Agregar a lista blanca de administración de energía
- Verificar que tiene permisos de inicio automático

## Licencia

Propietario - OpenMDM © 2025
