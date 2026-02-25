package com.androidmdm.app;

import android.app.Application;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;
import com.androidmdm.app.admin.MDMDeviceAdminReceiver;
import com.androidmdm.app.service.MDMService;
import java.io.DataOutputStream;

public class MDMApplication extends Application {
    
    private static final String TAG = "MDMApplication";
    private static final String PREFS_NAME = "MDMSetup";
    private static final String KEY_DEVICE_OWNER_CONFIGURED = "device_owner_configured";
    
    @Override
    public void onCreate() {
        super.onCreate();
        
        // Auto-configurar Device Owner en el primer inicio
        autoConfigureDeviceOwner();
        
        // Start MDM Service
        Intent serviceIntent = new Intent(this, MDMService.class);
        startForegroundService(serviceIntent);
    }
    
    private void autoConfigureDeviceOwner() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean alreadyConfigured = prefs.getBoolean(KEY_DEVICE_OWNER_CONFIGURED, false);
        
        // Solo intentar configurar una vez
        if (alreadyConfigured) {
            Log.d(TAG, "Device Owner ya fue configurado previamente");
            return;
        }
        
        DevicePolicyManager dpm = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
        ComponentName adminComponent = new ComponentName(this, MDMDeviceAdminReceiver.class);
        
        // Verificar si ya es Device Owner
        if (dpm.isDeviceOwnerApp(getPackageName())) {
            Log.d(TAG, "La app ya es Device Owner");
            prefs.edit().putBoolean(KEY_DEVICE_OWNER_CONFIGURED, true).apply();
            return;
        }
        
        // Intentar auto-configurarse como Device Owner
        Log.d(TAG, "Intentando auto-configurar como Device Owner...");
        
        new Thread(() -> {
            try {
                // Ejecutar comando para configurar Device Owner
                Process process = Runtime.getRuntime().exec("su");
                DataOutputStream os = new DataOutputStream(process.getOutputStream());
                
                String command = "dpm set-device-owner " + getPackageName() + "/.admin.MDMDeviceAdminReceiver\n";
                os.writeBytes(command);
                os.writeBytes("exit\n");
                os.flush();
                
                int exitCode = process.waitFor();
                
                if (exitCode == 0) {
                    Log.d(TAG, "✓ Device Owner configurado exitosamente");
                    prefs.edit().putBoolean(KEY_DEVICE_OWNER_CONFIGURED, true).apply();
                    
                    // Otorgar permisos adicionales
                    grantAdditionalPermissions();
                } else {
                    Log.e(TAG, "No se pudo configurar Device Owner (código: " + exitCode + ")");
                    Log.e(TAG, "El dispositivo debe estar rooteado O usar el método NFC/QR para provisión");
                }
                
            } catch (Exception e) {
                Log.e(TAG, "Error al configurar Device Owner", e);
                // Intentar método alternativo sin root
                tryAlternativeProvisioningMethod();
            }
        }).start();
    }
    
    private void grantAdditionalPermissions() {
        new Thread(() -> {
            try {
                Process process = Runtime.getRuntime().exec("su");
                DataOutputStream os = new DataOutputStream(process.getOutputStream());
                
                // Otorgar permisos de sistema
                os.writeBytes("appops set " + getPackageName() + " REQUEST_INSTALL_PACKAGES allow\n");
                os.writeBytes("pm grant " + getPackageName() + " android.permission.ACCESS_FINE_LOCATION\n");
                os.writeBytes("pm grant " + getPackageName() + " android.permission.ACCESS_COARSE_LOCATION\n");
                os.writeBytes("pm grant " + getPackageName() + " android.permission.READ_PHONE_STATE\n");
                os.writeBytes("exit\n");
                os.flush();
                
                process.waitFor();
                Log.d(TAG, "✓ Permisos adicionales otorgados");
                
            } catch (Exception e) {
                Log.e(TAG, "Error al otorgar permisos", e);
            }
        }).start();
    }
    
    private void tryAlternativeProvisioningMethod() {
        Log.d(TAG, "Intentando método alternativo de provisión...");
        
        // Para dispositivos sin root, se debe usar uno de estos métodos:
        // 1. NFC (Android Beam durante setup)
        // 2. QR Code (Android 7+)
        // 3. DPC Identifier (Android 7+)
        // 4. Zero-touch enrollment (Android 8+)
        
        // El dispositivo debe estar en estado de factory reset y durante el setup inicial
        // se puede escanear un QR code con la configuración del MDM
        
        Log.d(TAG, "Para dispositivos OEM sin root, usar provisión NFC/QR durante el setup inicial");
        Log.d(TAG, "Generando configuración QR para próxima instalación...");
        
        // TODO: Generar QR code con la configuración del Device Owner
        // Formato: {"android.app.extra.PROVISIONING_DEVICE_ADMIN_COMPONENT_NAME":"com.androidmdm.app/.admin.MDMDeviceAdminReceiver",...}
    }
}
