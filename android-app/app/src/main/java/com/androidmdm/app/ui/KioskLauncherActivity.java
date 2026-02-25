package com.androidmdm.app.ui;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

/**
 * KioskLauncherActivity - Activity transitoria que inicia Lock Task Mode
 * Se lanza antes de la app objetivo para activar el kiosk correctamente
 */
public class KioskLauncherActivity extends Activity {
    private static final String TAG = "KioskLauncher";
    public static final String EXTRA_PACKAGE_NAME = "packageName";
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        String targetPackage = getIntent().getStringExtra(EXTRA_PACKAGE_NAME);
        
        if (targetPackage == null) {
            Log.e(TAG, "No target package specified");
            finish();
            return;
        }
        
        Log.d(TAG, "🔒 Starting kiosk mode for: " + targetPackage);
        
        try {
            // Verificar que la app existe
            PackageManager pm = getPackageManager();
            Intent launchIntent = pm.getLaunchIntentForPackage(targetPackage);
            
            if (launchIntent == null) {
                Log.e(TAG, "❌ App not found: " + targetPackage);
                finish();
                return;
            }
            
            // Iniciar Lock Task Mode ANTES de lanzar la app
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                startLockTask();
                Log.d(TAG, "✅ Lock Task Mode started");
            }
            
            // Lanzar la app objetivo
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(launchIntent);
            
            Log.d(TAG, "✅ Target app launched: " + targetPackage);
            
            // Finalizar esta activity transitoria
            finish();
            
        } catch (Exception e) {
            Log.e(TAG, "❌ Error starting kiosk mode", e);
            finish();
        }
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "KioskLauncherActivity destroyed");
    }
}
