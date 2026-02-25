package com.androidmdm.app.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.os.PowerManager;
import android.util.Log;
import com.androidmdm.app.admin.MDMDeviceAdminReceiver;

/**
 * PowerScheduleReceiver - Maneja las alarmas programadas para encendido/apagado
 * Ejecuta acciones cuando AlarmManager dispara las alarmas configuradas
 */
public class PowerScheduleReceiver extends BroadcastReceiver {
    private static final String TAG = "PowerScheduleReceiver";
    
    public static final String ACTION_POWER_OFF = "com.androidmdm.app.POWER_OFF";
    public static final String ACTION_POWER_ON = "com.androidmdm.app.POWER_ON";
    
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        
        if (action == null) {
            Log.w(TAG, "Received null action");
            return;
        }
        
        Log.i(TAG, "PowerScheduleReceiver triggered: " + action);
        
        switch (action) {
            case ACTION_POWER_OFF:
                handlePowerOff(context);
                break;
                
            case ACTION_POWER_ON:
                handlePowerOn(context);
                break;
                
            default:
                Log.w(TAG, "Unknown action: " + action);
        }
    }
    
    /**
     * Apagar o bloquear el dispositivo
     * Bloquea pantalla + guarda estado para evitar que se despierte
     */
    private void handlePowerOff(Context context) {
        try {
            DevicePolicyManager dpm = (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
            ComponentName adminComponent = new ComponentName(context, MDMDeviceAdminReceiver.class);
            
            if (dpm != null && dpm.isAdminActive(adminComponent)) {
                Log.i(TAG, "Executing power OFF - Locking device and screen");
                
                // Guardar estado de "apagado programado"
                SharedPreferences prefs = context.getSharedPreferences("MDMSchedule", Context.MODE_PRIVATE);
                prefs.edit().putBoolean("device_scheduled_off", true).apply();
                
                // Bloquear pantalla
                dpm.lockNow();
                
                // Apagar pantalla después de bloquear
                new android.os.Handler().postDelayed(() -> {
                    PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
                    if (pm != null) {
                        // Crear un WakeLock especial para forzar sleep
                        PowerManager.WakeLock wakeLock = pm.newWakeLock(
                            PowerManager.PARTIAL_WAKE_LOCK,
                            "MDM:PowerOff"
                        );
                        
                        // Forzar que la pantalla se apague
                        if (wakeLock.isHeld()) {
                            wakeLock.release();
                        }
                        
                        Log.i(TAG, "✅ Device locked and screen turned off");
                    }
                }, 500);
                
            } else {
                Log.e(TAG, "Device admin not active - cannot lock device");
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error in power OFF", e);
        }
    }
    
    /**
     * Encender o despertar el dispositivo
     * Despierta la pantalla y limpia el estado de apagado programado
     */
    private void handlePowerOn(Context context) {
        try {
            Log.i(TAG, "Executing power ON - Waking device");
            
            // Limpiar estado de "apagado programado"
            SharedPreferences prefs = context.getSharedPreferences("MDMSchedule", Context.MODE_PRIVATE);
            prefs.edit().putBoolean("device_scheduled_off", false).apply();
            
            PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            if (pm != null) {
                // Despertar la pantalla con flags más agresivos
                PowerManager.WakeLock wakeLock = pm.newWakeLock(
                    PowerManager.FULL_WAKE_LOCK | 
                    PowerManager.ACQUIRE_CAUSES_WAKEUP | 
                    PowerManager.ON_AFTER_RELEASE,
                    "MDM:PowerScheduleOn"
                );
                wakeLock.acquire(10000); // 10 segundos
                
                Log.i(TAG, "✅ Device wake lock acquired - Screen should be ON");
                
                // Si hay modo kiosk activado, relanzar la app
                SharedPreferences kioskPrefs = context.getSharedPreferences("MDMKiosk", Context.MODE_PRIVATE);
                boolean kioskEnabled = kioskPrefs.getBoolean("kiosk_enabled", false);
                String kioskPackage = kioskPrefs.getString("kiosk_package", null);
                
                if (kioskEnabled && kioskPackage != null) {
                    Log.i(TAG, "Relaunching kiosk app: " + kioskPackage);
                    
                    new android.os.Handler().postDelayed(() -> {
                        Intent launchIntent = context.getPackageManager().getLaunchIntentForPackage(kioskPackage);
                        if (launchIntent != null) {
                            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                            context.startActivity(launchIntent);
                            Log.i(TAG, "✅ Kiosk app relaunched");
                        }
                    }, 2000);
                }
                
                // Liberar después de 10 segundos
                new android.os.Handler().postDelayed(() -> {
                    if (wakeLock.isHeld()) {
                        wakeLock.release();
                        Log.i(TAG, "Wake lock released - Device should stay awake");
                    }
                }, 10000);
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error in power ON", e);
        }
    }
}
