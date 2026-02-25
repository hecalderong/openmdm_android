package com.androidmdm.app.ui;

import android.Manifest;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.androidmdm.app.R;
import com.androidmdm.app.admin.MDMDeviceAdminReceiver;
import com.androidmdm.app.service.MDMService;
import com.androidmdm.app.util.DeviceInfoUtil;

public class MainActivity extends AppCompatActivity {
    
    private static final int REQUEST_CODE_ENABLE_ADMIN = 1;
    private static final int REQUEST_CODE_OVERLAY_PERMISSION = 2;
    private static final int REQUEST_CODE_USAGE_STATS = 3;
    private static final int REQUEST_CODE_WRITE_SETTINGS = 4;
    private static final int REQUEST_RUNTIME_PERMISSIONS = 5;
    
    private static final String PREFS_NAME = "MDMPrefs";
    private static final String KEY_FIRST_RUN = "first_run";
    
    private DevicePolicyManager devicePolicyManager;
    private ComponentName adminComponent;
    private SharedPreferences prefs;
    
    private TextView statusText;
    private TextView deviceInfoText;
    private Button enableAdminButton;
    private Button startServiceButton;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        devicePolicyManager = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
        adminComponent = new ComponentName(this, MDMDeviceAdminReceiver.class);
        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        
        // Check if we need to stop Lock Task Mode
        if (getIntent().getBooleanExtra("STOP_LOCK_TASK", false)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                try {
                    stopLockTask();
                    Toast.makeText(this, "Modo Kiosk desactivado", Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    // Lock task not active
                }
            }
        }
        
        initViews();
        updateUI();
        
        // Solicitar permisos automáticamente en el primer inicio
        if (isFirstRun()) {
            requestAllPermissionsAutomatically();
        }
    }
    
    private void initViews() {
        statusText = findViewById(R.id.statusText);
        deviceInfoText = findViewById(R.id.deviceInfoText);
        enableAdminButton = findViewById(R.id.enableAdminButton);
        startServiceButton = findViewById(R.id.startServiceButton);
        
        enableAdminButton.setOnClickListener(v -> enableDeviceAdmin());
        startServiceButton.setOnClickListener(v -> startMDMService());
        
        // Display device information
        String deviceInfo = DeviceInfoUtil.getDeviceInfo(this);
        deviceInfoText.setText(deviceInfo);
    }
    
    private void updateUI() {
        boolean isAdminActive = devicePolicyManager.isAdminActive(adminComponent);
        
        if (isAdminActive) {
            statusText.setText("Estado: Administración Activa ✓");
            enableAdminButton.setEnabled(false);
            startServiceButton.setEnabled(true);
        } else {
            statusText.setText("Estado: Administración Inactiva");
            enableAdminButton.setEnabled(true);
            startServiceButton.setEnabled(false);
        }
    }
    
    private void enableDeviceAdmin() {
        Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
        intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponent);
        intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                "Esta aplicación necesita permisos de administrador para gestionar el dispositivo remotamente.");
        startActivityForResult(intent, REQUEST_CODE_ENABLE_ADMIN);
    }
    
    private void startMDMService() {
        Intent serviceIntent = new Intent(this, MDMService.class);
        startForegroundService(serviceIntent);
        Toast.makeText(this, "Servicio MDM iniciado", Toast.LENGTH_SHORT).show();
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        switch (requestCode) {
            case REQUEST_CODE_ENABLE_ADMIN:
                if (resultCode == RESULT_OK) {
                    Toast.makeText(this, "Administración de dispositivo habilitada", Toast.LENGTH_SHORT).show();
                    finishPermissionSetup();
                } else {
                    Toast.makeText(this, "Administración de dispositivo rechazada - El MDM no funcionará correctamente", Toast.LENGTH_LONG).show();
                }
                updateUI();
                break;
                
            case REQUEST_CODE_OVERLAY_PERMISSION:
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Settings.canDrawOverlays(this)) {
                    Toast.makeText(this, "Permiso de superposición otorgado", Toast.LENGTH_SHORT).show();
                }
                requestUsageStatsPermission();
                break;
                
            case REQUEST_CODE_USAGE_STATS:
                if (hasUsageStatsPermission()) {
                    Toast.makeText(this, "Permiso de uso de apps otorgado", Toast.LENGTH_SHORT).show();
                }
                requestWriteSettingsPermission();
                break;
                
            case REQUEST_CODE_WRITE_SETTINGS:
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Settings.System.canWrite(this)) {
                    Toast.makeText(this, "Permiso de configuración otorgado", Toast.LENGTH_SHORT).show();
                }
                requestDeviceAdminPermission();
                break;
        }
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        updateUI();
    }
    
    private boolean isFirstRun() {
        return prefs.getBoolean(KEY_FIRST_RUN, true);
    }
    
    private void markFirstRunComplete() {
        prefs.edit().putBoolean(KEY_FIRST_RUN, false).apply();
    }
    
    private void requestAllPermissionsAutomatically() {
        new AlertDialog.Builder(this)
            .setTitle("Configuración MDM")
            .setMessage("Esta aplicación necesita permisos de administración para gestionar el dispositivo remotamente. Se solicitarán varios permisos automáticamente.")
            .setPositiveButton("Continuar", (dialog, which) -> {
                // Paso 1: Solicitar permisos runtime normales
                requestRuntimePermissions();
            })
            .setCancelable(false)
            .show();
    }
    
    private void requestRuntimePermissions() {
        String[] permissions = {
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.READ_PHONE_STATE
        };
        
        ActivityCompat.requestPermissions(this, permissions, REQUEST_RUNTIME_PERMISSIONS);
    }
    
    private void requestOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                new AlertDialog.Builder(this)
                    .setTitle("Permiso de superposición")
                    .setMessage("El MDM necesita permiso para mostrar ventanas sobre otras apps.")
                    .setPositiveButton("Otorgar", (dialog, which) -> {
                        Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:" + getPackageName()));
                        startActivityForResult(intent, REQUEST_CODE_OVERLAY_PERMISSION);
                    })
                    .setCancelable(false)
                    .show();
            } else {
                requestUsageStatsPermission();
            }
        } else {
            requestUsageStatsPermission();
        }
    }
    
    private void requestUsageStatsPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (!hasUsageStatsPermission()) {
                new AlertDialog.Builder(this)
                    .setTitle("Permiso de uso de apps")
                    .setMessage("El MDM necesita acceso a las estadísticas de uso para monitorear las aplicaciones.")
                    .setPositiveButton("Otorgar", (dialog, which) -> {
                        Intent intent = new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS);
                        startActivityForResult(intent, REQUEST_CODE_USAGE_STATS);
                    })
                    .setCancelable(false)
                    .show();
            } else {
                requestWriteSettingsPermission();
            }
        } else {
            requestWriteSettingsPermission();
        }
    }
    
    private boolean hasUsageStatsPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            try {
                PackageManager packageManager = getPackageManager();
                android.content.pm.ApplicationInfo applicationInfo = packageManager.getApplicationInfo(getPackageName(), 0);
                android.app.AppOpsManager appOpsManager = (android.app.AppOpsManager) getSystemService(Context.APP_OPS_SERVICE);
                int mode = appOpsManager.checkOpNoThrow(android.app.AppOpsManager.OPSTR_GET_USAGE_STATS,
                    applicationInfo.uid, applicationInfo.packageName);
                return mode == android.app.AppOpsManager.MODE_ALLOWED;
            } catch (Exception e) {
                return false;
            }
        }
        return true;
    }
    
    private void requestWriteSettingsPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.System.canWrite(this)) {
                new AlertDialog.Builder(this)
                    .setTitle("Permiso de configuración")
                    .setMessage("El MDM necesita permiso para modificar la configuración del sistema.")
                    .setPositiveButton("Otorgar", (dialog, which) -> {
                        Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS,
                            Uri.parse("package:" + getPackageName()));
                        startActivityForResult(intent, REQUEST_CODE_WRITE_SETTINGS);
                    })
                    .setCancelable(false)
                    .show();
            } else {
                requestDeviceAdminPermission();
            }
        } else {
            requestDeviceAdminPermission();
        }
    }
    
    private void requestDeviceAdminPermission() {
        boolean isAdminActive = devicePolicyManager.isAdminActive(adminComponent);
        
        if (!isAdminActive) {
            new AlertDialog.Builder(this)
                .setTitle("Permisos de Administración")
                .setMessage("El MDM necesita permisos de administrador del dispositivo para control completo (instalar apps, bloquear pantalla, borrar datos, etc.).")
                .setPositiveButton("Otorgar", (dialog, which) -> enableDeviceAdmin())
                .setCancelable(false)
                .show();
        } else {
            finishPermissionSetup();
        }
    }
    
    private void finishPermissionSetup() {
        markFirstRunComplete();
        
        new AlertDialog.Builder(this)
            .setTitle("Configuración Completa")
            .setMessage("Todos los permisos han sido otorgados. El servicio MDM se iniciará automáticamente.")
            .setPositiveButton("OK", (dialog, which) -> {
                startMDMService();
                updateUI();
            })
            .show();
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        if (requestCode == REQUEST_RUNTIME_PERMISSIONS) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }
            
            if (allGranted) {
                requestOverlayPermission();
            } else {
                Toast.makeText(this, "Algunos permisos fueron rechazados", Toast.LENGTH_LONG).show();
                requestOverlayPermission(); // Continuar de todas formas
            }
        }
    }
}
