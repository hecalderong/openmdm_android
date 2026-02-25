import { Controller, Get, Res, Header } from '@nestjs/common';
import { Response } from 'express';
import * as QRCode from 'qrcode';
import * as fs from 'fs';
import * as path from 'path';

@Controller('provisioning')
export class ProvisioningController {
  private readonly apkPath = path.join(process.cwd(), 'uploads', 'mdm-app.apk');
  
  /**
   * Endpoint para descargar el APK del MDM
   * Este APK será descargado automáticamente durante la provisión QR
   */
  @Get('mdm-app.apk')
  @Header('Content-Type', 'application/vnd.android.package-archive')
  @Header('Content-Disposition', 'attachment; filename="openmdm-android.apk"')
  async downloadMDMApk(@Res() res: Response) {
    try {
      if (!fs.existsSync(this.apkPath)) {
        return res.status(404).json({ 
          message: 'APK not found. Please upload the MDM APK to uploads/mdm-app.apk' 
        });
      }
      
      const apkStream = fs.createReadStream(this.apkPath);
      apkStream.pipe(res);
      
    } catch (error) {
      return res.status(500).json({ message: 'Error serving APK', error: error.message });
    }
  }
  
  /**
   * Genera código QR para provisión automática de Device Owner
   * 
   * Query params:
   * - wifiSsid: SSID del WiFi (opcional)
   * - wifiPassword: Password del WiFi (opcional)
   * - serverUrl: URL del servidor backend (opcional, usa el actual por defecto)
   */
  @Get('qr-code')
  @Header('Content-Type', 'image/png')
  async generateProvisioningQR(
    @Res() res: Response,
    @Res() query: any,
  ) {
    try {
      const serverUrl = query.serverUrl || `http://${process.env.SERVER_IP || 'localhost'}:3000`;
      const apkUrl = `${serverUrl}/api/provisioning/mdm-app.apk`;
      const wifiSsid = query.wifiSsid || '';
      const wifiPassword = query.wifiPassword || '';
      
      // Configuración de provisión para Android Enterprise
      const provisioningConfig: any = {
        'android.app.extra.PROVISIONING_DEVICE_ADMIN_COMPONENT_NAME': 
          'com.androidmdm.app/.admin.MDMDeviceAdminReceiver',
        'android.app.extra.PROVISIONING_DEVICE_ADMIN_PACKAGE_DOWNLOAD_LOCATION': apkUrl,
        'android.app.extra.PROVISIONING_SKIP_ENCRYPTION': false,
        'android.app.extra.PROVISIONING_LEAVE_ALL_SYSTEM_APPS_ENABLED': true,
        'android.app.extra.PROVISIONING_LOCALE': 'es_ES',
        'android.app.extra.PROVISIONING_TIME_ZONE': 'America/Bogota',
      };
      
      // Agregar WiFi si se proporciona
      if (wifiSsid) {
        provisioningConfig['android.app.extra.PROVISIONING_WIFI_SSID'] = wifiSsid;
        
        if (wifiPassword) {
          provisioningConfig['android.app.extra.PROVISIONING_WIFI_PASSWORD'] = wifiPassword;
          provisioningConfig['android.app.extra.PROVISIONING_WIFI_SECURITY_TYPE'] = 'WPA';
        }
      }
      
      // Convertir a JSON
      const jsonConfig = JSON.stringify(provisioningConfig);
      
      // Generar QR code
      const qrCodeBuffer = await QRCode.toBuffer(jsonConfig, {
        errorCorrectionLevel: 'M',
        type: 'png',
        width: 512,
        margin: 2,
      });
      
      res.send(qrCodeBuffer);
      
    } catch (error) {
      return res.status(500).json({ 
        message: 'Error generating QR code', 
        error: error.message 
      });
    }
  }
  
  /**
   * Obtiene las instrucciones de provisión en texto
   */
  @Get('instructions')
  async getProvisioningInstructions(@Res() res: Response) {
    const serverUrl = `http://${process.env.SERVER_IP || 'localhost'}:3000`;
    const qrUrl = `${serverUrl}/api/provisioning/qr-code`;
    
    const instructions = `
╔════════════════════════════════════════════════════════════════╗
║        PROVISIÓN AUTOMÁTICA - OPENMDM-ANDROID                  ║
║              Device Owner via QR Code                          ║
╚════════════════════════════════════════════════════════════════╝

📋 REQUISITOS PREVIOS:
   ✓ Dispositivo Android 7.0 o superior
   ✓ Dispositivo en estado de fábrica (factory reset)
   ✓ Sin cuentas de Google o usuario configuradas
   ✓ Acceso a internet (WiFi o datos móviles)

📱 PROCESO DE PROVISIÓN (2-5 minutos):

1️⃣  PREPARACIÓN:
   • Asegúrate de que el APK esté disponible en el servidor
   • Genera el código QR desde el dashboard o accede a:
     ${qrUrl}
   
2️⃣  FACTORY RESET (si el dispositivo ya fue usado):
   • Configuración > Sistema > Restablecer
   • Seleccionar "Borrar todos los datos"
   • Confirmar y esperar el reinicio
   
3️⃣  INICIO DE PROVISIÓN:
   • Encender el dispositivo
   • En la pantalla de bienvenida, tocar 6 VECES en cualquier lugar
   • Aparecerá automáticamente el escáner de QR
   
4️⃣  ESCANEAR QR:
   • Apuntar la cámara al código QR generado
   • El dispositivo lo detectará automáticamente
   
5️⃣  PROCESO AUTOMÁTICO (sin intervención):
   ✓ Conexión automática a WiFi (si se configuró en el QR)
   ✓ Descarga del APK MDM desde el servidor
   ✓ Instalación automática como Device Owner
   ✓ Configuración de permisos de sistema
   ✓ Inicio del servicio MDM
   ✓ Conexión con el backend
   
6️⃣  VERIFICACIÓN:
   • El dispositivo mostrará: "Administrado por tu organización"
   • En el dashboard aparecerá el nuevo dispositivo como "Online"
   • La app MDM estará corriendo en segundo plano

⚠️  IMPORTANTE:
   • NO interrumpir el proceso una vez iniciado
   • El dispositivo DEBE tener acceso a internet
   • Este método SOLO funciona con dispositivos de fábrica o después de factory reset
   • Si falla, verificar que el servidor backend esté accesible

🔧 CONFIGURACIÓN DEL QR:

   Básico (solo MDM):
   ${qrUrl}
   
   Con WiFi:
   ${qrUrl}?wifiSsid=TU_WIFI&wifiPassword=TU_PASSWORD
   
   Con servidor personalizado:
   ${qrUrl}?serverUrl=http://TU_SERVIDOR:3000

📊 CAPACIDADES POST-PROVISIÓN:
   ✅ Instalación silenciosa de apps (sin interacción del usuario)
   ✅ Desinstalación remota de apps
   ✅ Lanzamiento remoto de aplicaciones
   ✅ Bloqueo/desbloqueo de pantalla
   ✅ Modo kiosk (bloqueo a una sola app)
   ✅ Políticas de seguridad y contraseñas
   ✅ Borrado remoto de datos
   ✅ Monitoreo en tiempo real
   ✅ Control total del dispositivo

💡 TROUBLESHOOTING:

   Problema: "El escáner QR no aparece"
   Solución: Asegúrate de tocar EXACTAMENTE 6 veces en la pantalla de bienvenida
   
   Problema: "Error al descargar el APK"
   Solución: Verifica que el servidor backend esté accesible desde el dispositivo
   
   Problema: "Falló la configuración de Device Owner"
   Solución: El dispositivo tiene cuentas configuradas. Hacer factory reset completo.
   
   Problema: "QR code inválido"
   Solución: Generar nuevo QR desde el dashboard con configuración actualizada

📞 SOPORTE:
   Dashboard: ${serverUrl}
   API: ${serverUrl}/api
   
═══════════════════════════════════════════════════════════════════
`;
    
    return res.type('text/plain').send(instructions);
  }
}
