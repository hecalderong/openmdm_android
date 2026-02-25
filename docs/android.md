# Android App (Java)

## Ubicación

- Código principal: `android-app/app/src/main`
- Paquete base: `com.openmdm.mdm` (según estructura actual)

## Componentes clave

- `MDMApplication.java`: inicialización global.
- `admin/MDMDeviceAdminReceiver.java`: integración Device Admin.
- `network/WebSocketManager.java`: canal en tiempo real.
- `service/MDMService.java`: servicio foreground persistente.
- `receiver/BootReceiver.java`: autoarranque al iniciar el dispositivo.
- `ui/MainActivity.java`: activación y estado local del agente.
- `util/DeviceInfoUtil.java`: métricas de batería/RAM/storage/red.

## Capacidades actuales

- Registro y conexión con backend.
- Heartbeat periódico con métricas del dispositivo.
- Recepción de comandos remotos.
- Soporte de instalación/desinstalación remota de APKs.
- Bloqueo remoto vía Device Admin.
- Inicio automático post-reinicio.

## Compilación

```bash
cd android-app
./gradlew assembleDebug
./gradlew assembleRelease
```

## Instalación

```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

## Configuración de endpoints

Revisar `android-app/app/build.gradle` para `API_BASE_URL` y `WS_URL` según entorno.

Para emulador Android suele usarse:

- `http://10.0.2.2:3000`
- `ws://10.0.2.2:3000`

## Permisos relevantes

- `INTERNET`
- `ACCESS_FINE_LOCATION`
- `REQUEST_INSTALL_PACKAGES`
- `FOREGROUND_SERVICE`
- `RECEIVE_BOOT_COMPLETED`
- Permisos Device Admin

## Troubleshooting rápido

- Verificar URL backend y red entre dispositivo y servidor.
- Excluir la app de optimización de batería.
- Confirmar activación de Device Admin.
- Validar permisos especiales según fabricante (Samsung/Xiaomi, etc.).
