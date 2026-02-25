package com.androidmdm.app.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import com.androidmdm.app.service.MDMService;

/**
 * BroadcastReceiver que se activa cuando el dispositivo arranca
 * para iniciar automáticamente el servicio MDM y el modo kiosk si está configurado
 */
public class BootReceiver extends BroadcastReceiver {
    private static final String TAG = "BootReceiver";
    
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            Log.d(TAG, "📱 Device boot detected");
            
            // Start MDM Service first
            Intent serviceIntent = new Intent(context, MDMService.class);
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent);
            } else {
                context.startService(serviceIntent);
            }
            
            Log.d(TAG, "✅ MDM Service started on boot");
            
            // Check if kiosk mode should be activated
            SharedPreferences prefs = context.getSharedPreferences("MDMKiosk", Context.MODE_PRIVATE);
            boolean kioskEnabled = prefs.getBoolean("kiosk_enabled", false);
            String kioskPackage = prefs.getString("kiosk_package", null);
            
            if (kioskEnabled && kioskPackage != null) {
                Log.d(TAG, "🔒 Auto-launching kiosk app on boot: " + kioskPackage);
                
                // Launch kiosk app after delay (MDM service needs time to start)
                Handler handler = new Handler(Looper.getMainLooper());
                handler.postDelayed(() -> {
                    try {
                        Intent launchIntent = context.getPackageManager().getLaunchIntentForPackage(kioskPackage);
                        if (launchIntent != null) {
                            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            context.startActivity(launchIntent);
                            Log.d(TAG, "✅ Kiosk app launched on boot: " + kioskPackage);
                        } else {
                            Log.e(TAG, "❌ Kiosk app not found: " + kioskPackage);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "❌ Error launching kiosk app on boot", e);
                    }
                }, 8000); // 8 seconds delay to ensure system is fully ready
            }
        }
    }
}
