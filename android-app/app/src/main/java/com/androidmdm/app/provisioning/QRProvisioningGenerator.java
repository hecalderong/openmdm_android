package com.androidmdm.app.provisioning;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.util.Log;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import org.json.JSONObject;
import java.io.File;
import java.io.FileOutputStream;

/**
 * Generador de código QR para provisión automática de Device Owner
 * 
 * IMPORTANTE: Este QR debe escanearse durante el setup inicial del dispositivo
 * (cuando se enciende por primera vez o después de factory reset)
 * 
 * Proceso:
 * 1. Factory reset del dispositivo
 * 2. Encender dispositivo
 * 3. En pantalla de bienvenida, tocar 6 veces en cualquier lugar
 * 4. Escanear el QR generado por este método
 * 5. El dispositivo descargará e instalará automáticamente el MDM como Device Owner
 */
public class QRProvisioningGenerator {
    
    private static final String TAG = "QRProvisioning";
    
    /**
     * Genera el JSON de provisión para Device Owner
     * 
     * @param downloadUrl URL donde está hosteado el APK del MDM
     * @param wifiSsid SSID del WiFi (opcional)
     * @param wifiPassword Password del WiFi (opcional)
     * @return JSON string con la configuración
     */
    public static String generateProvisioningJson(String downloadUrl, String wifiSsid, String wifiPassword) {
        try {
            JSONObject config = new JSONObject();
            
            // Información básica del Device Owner
            config.put("android.app.extra.PROVISIONING_DEVICE_ADMIN_COMPONENT_NAME", 
                      "com.androidmdm.app/.admin.MDMDeviceAdminReceiver");
            
            config.put("android.app.extra.PROVISIONING_DEVICE_ADMIN_PACKAGE_DOWNLOAD_LOCATION", 
                      downloadUrl);
            
            // Checksum del APK (SHA-256)
            // TODO: Calcular automáticamente el checksum del APK
            // config.put("android.app.extra.PROVISIONING_DEVICE_ADMIN_PACKAGE_CHECKSUM", "");
            
            // Skip encryption (opcional, para dispositivos que no requieren encriptación)
            config.put("android.app.extra.PROVISIONING_SKIP_ENCRYPTION", false);
            
            // Dejar el usuario en el setup wizard después de provisión
            config.put("android.app.extra.PROVISIONING_LEAVE_ALL_SYSTEM_APPS_ENABLED", true);
            
            // Configuración WiFi (si se proporciona)
            if (wifiSsid != null && !wifiSsid.isEmpty()) {
                config.put("android.app.extra.PROVISIONING_WIFI_SSID", wifiSsid);
                
                if (wifiPassword != null && !wifiPassword.isEmpty()) {
                    config.put("android.app.extra.PROVISIONING_WIFI_PASSWORD", wifiPassword);
                    config.put("android.app.extra.PROVISIONING_WIFI_SECURITY_TYPE", "WPA");
                }
            }
            
            // Locale
            config.put("android.app.extra.PROVISIONING_LOCALE", "es_ES");
            
            // Zona horaria
            config.put("android.app.extra.PROVISIONING_TIME_ZONE", "America/Bogota");
            
            return config.toString();
            
        } catch (Exception e) {
            Log.e(TAG, "Error generando JSON de provisión", e);
            return null;
        }
    }
    
    /**
     * Genera imagen QR con la configuración de provisión
     * 
     * @param context Contexto de la aplicación
     * @param downloadUrl URL del APK
     * @param wifiSsid WiFi SSID (puede ser null)
     * @param wifiPassword WiFi password (puede ser null)
     * @param size Tamaño del QR en pixels
     * @return Bitmap con el código QR
     */
    public static Bitmap generateQRCode(Context context, String downloadUrl, 
                                       String wifiSsid, String wifiPassword, int size) {
        try {
            String json = generateProvisioningJson(downloadUrl, wifiSsid, wifiPassword);
            
            if (json == null) {
                return null;
            }
            
            QRCodeWriter writer = new QRCodeWriter();
            BitMatrix bitMatrix = writer.encode(json, BarcodeFormat.QR_CODE, size, size);
            
            Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565);
            
            for (int x = 0; x < size; x++) {
                for (int y = 0; y < size; y++) {
                    bitmap.setPixel(x, y, bitMatrix.get(x, y) ? Color.BLACK : Color.WHITE);
                }
            }
            
            // Guardar QR en almacenamiento para referencia
            saveQRCode(context, bitmap);
            
            return bitmap;
            
        } catch (WriterException e) {
            Log.e(TAG, "Error generando código QR", e);
            return null;
        }
    }
    
    private static void saveQRCode(Context context, Bitmap bitmap) {
        try {
            File qrFile = new File(context.getExternalFilesDir(null), "mdm_provisioning_qr.png");
            FileOutputStream out = new FileOutputStream(qrFile);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
            out.flush();
            out.close();
            
            Log.d(TAG, "Código QR guardado en: " + qrFile.getAbsolutePath());
            
        } catch (Exception e) {
            Log.e(TAG, "Error guardando QR", e);
        }
    }
    
    /**
     * Genera instrucciones para el técnico de campo
     * 
     * @param downloadUrl URL del APK
     * @return String con las instrucciones
     */
    public static String getProvisioningInstructions(String downloadUrl) {
        return "INSTRUCCIONES DE PROVISIÓN - OpenMDM-Android\n" +
               "==========================================\n\n" +
               
               "1. PREPARACIÓN:\n" +
               "   - Asegúrate de que el APK está disponible en:\n" +
               "     " + downloadUrl + "\n" +
               "   - Ten el código QR impreso o en otra pantalla\n\n" +
               
               "2. FACTORY RESET:\n" +
               "   - Si el dispositivo ya fue usado, hacer factory reset\n" +
               "   - Configuración > Sistema > Restablecer > Borrar todos los datos\n\n" +
               
               "3. PROVISIÓN:\n" +
               "   - Encender el dispositivo\n" +
               "   - En la pantalla de bienvenida, tocar 6 veces\n" +
               "   - Aparecerá el escáner QR\n" +
               "   - Escanear el código QR generado\n\n" +
               
               "4. PROCESO AUTOMÁTICO:\n" +
               "   - El dispositivo se conectará al WiFi (si se configuró)\n" +
               "   - Descargará el APK del MDM automáticamente\n" +
               "   - Se instalará como Device Owner\n" +
               "   - Se configurarán todos los permisos\n\n" +
               
               "5. VERIFICACIÓN:\n" +
               "   - El dispositivo debe mostrar 'Administrado por tu organización'\n" +
               "   - Abrir el dashboard y verificar que aparece el dispositivo\n\n" +
               
               "IMPORTANTE:\n" +
               "- El proceso completo toma 2-5 minutos\n" +
               "- NO interrumpir durante la descarga/instalación\n" +
               "- El dispositivo debe tener acceso a internet\n" +
               "- Este método solo funciona en dispositivos de fábrica o después de factory reset\n";
    }
}
