package com.androidmdm.app.service;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInstaller;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import androidx.core.app.NotificationCompat;
import androidx.core.content.FileProvider;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import android.content.BroadcastReceiver;
import android.content.IntentFilter;
import com.androidmdm.app.BuildConfig;
import com.androidmdm.app.R;
import com.androidmdm.app.admin.MDMDeviceAdminReceiver;
import com.androidmdm.app.network.WebSocketManager;
import com.androidmdm.app.receiver.InstallReceiver;
import com.androidmdm.app.receiver.PowerScheduleReceiver;
import com.androidmdm.app.util.DeviceInfoUtil;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.Calendar;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class MDMService extends Service {
    
    private static final String CHANNEL_ID = "MDMServiceChannel";
    private static final int NOTIFICATION_ID = 1;
    private static final int HEARTBEAT_INTERVAL = 60000; // 1 minute
    
    private WebSocketManager webSocketManager;
    private Handler heartbeatHandler;
    private Runnable heartbeatRunnable;
    private DevicePolicyManager devicePolicyManager;
    private ComponentName adminComponent;
    private BroadcastReceiver installResultReceiver;
    private BroadcastReceiver uninstallResultReceiver;
    
    @Override
    public void onCreate() {
        super.onCreate();
        
        devicePolicyManager = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
        adminComponent = new ComponentName(this, MDMDeviceAdminReceiver.class);
        
        createNotificationChannel();
        startForeground(NOTIFICATION_ID, createNotification());
        
        // Initialize WebSocket
        webSocketManager = new WebSocketManager(this, BuildConfig.WS_URL);
        webSocketManager.connect();
        
        // Register install result receiver
        registerInstallResultReceiver();
        
        // Register uninstall result receiver
        registerUninstallResultReceiver();
        
        // Start heartbeat
        startHeartbeat();
        
        // Check if kiosk mode should be activated (persisted from previous session)
        checkAndActivateKioskMode();
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Manejar acciones específicas
        if (intent != null && intent.getAction() != null) {
            String action = intent.getAction();
            
            if ("LAUNCH_APP".equals(action)) {
                String packageName = intent.getStringExtra("packageName");
                if (packageName != null) {
                    launchApp(packageName);
                }
            } else if ("INSTALL_APP".equals(action)) {
                String appId = intent.getStringExtra("appId");
                String appName = intent.getStringExtra("appName");
                String packageName = intent.getStringExtra("packageName");
                String downloadUrl = intent.getStringExtra("downloadUrl");
                if (downloadUrl != null) {
                    installApp(appId, appName, packageName, downloadUrl);
                }
            } else if ("UNINSTALL_APP".equals(action)) {
                String packageName = intent.getStringExtra("packageName");
                if (packageName != null) {
                    uninstallApp(packageName);
                }
            } else if ("ENABLE_KIOSK".equals(action)) {
                String packageName = intent.getStringExtra("packageName");
                if (packageName != null) {
                    enableKioskMode(packageName);
                }
            } else if ("DISABLE_KIOSK".equals(action)) {
                disableKioskMode();
            } else if ("SET_POWER_SCHEDULE".equals(action)) {
                String scheduleJson = intent.getStringExtra("scheduleJson");
                if (scheduleJson != null) {
                    try {
                        JSONObject json = new JSONObject(scheduleJson);
                        setPowerSchedule(json);
                    } catch (Exception e) {
                        Log.e("MDMService", "Error parsing schedule JSON", e);
                    }
                }
            } else if ("CLEAR_POWER_SCHEDULE".equals(action)) {
                clearPowerSchedule();
            }
        }
        
        return START_STICKY;
    }
    
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
    
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "MDM Service",
                NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Servicio de gestión de dispositivos");
            
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }
    }
    
    private Notification createNotification() {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("OpenMDM-Android")
            .setContentText("Servicio activo - Dispositivo monitoreado")
            .setSmallIcon(R.drawable.ic_notification)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build();
    }
    
    private void startHeartbeat() {
        heartbeatHandler = new Handler(Looper.getMainLooper());
        heartbeatRunnable = new Runnable() {
            @Override
            public void run() {
                sendHeartbeat();
                heartbeatHandler.postDelayed(this, HEARTBEAT_INTERVAL);
            }
        };
        heartbeatHandler.post(heartbeatRunnable);
    }
    
    private void sendHeartbeat() {
        try {
            Log.d("MDMService", "📡 Sending heartbeat...");
            JSONObject heartbeat = new JSONObject();
            heartbeat.put("deviceId", DeviceInfoUtil.getDeviceId(this));
            heartbeat.put("batteryLevel", DeviceInfoUtil.getBatteryLevel(this));
            heartbeat.put("storageUsed", DeviceInfoUtil.getStorageUsed());
            heartbeat.put("storageTotal", DeviceInfoUtil.getStorageTotal());
            heartbeat.put("ramUsed", DeviceInfoUtil.getRamUsed(this));
            heartbeat.put("ramTotal", DeviceInfoUtil.getRamTotal(this));
            heartbeat.put("ipAddress", DeviceInfoUtil.getIPAddress(this));
            
            webSocketManager.sendHeartbeat(heartbeat);
            Log.d("MDMService", "✅ Heartbeat sent");
        } catch (Exception e) {
            Log.e("MDMService", "❌ Error sending heartbeat: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public void lockDevice() {
        if (devicePolicyManager.isAdminActive(adminComponent)) {
            devicePolicyManager.lockNow();
        }
    }
    
    public void unlockDevice() {
        // Note: Unlock requires user interaction in modern Android
        // This would typically show a notification
    }
    
    public void launchApp(String packageName) {
        try {
            Intent launchIntent = getPackageManager().getLaunchIntentForPackage(packageName);
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(launchIntent);
                android.util.Log.d("MDMService", "Launched app: " + packageName);
            } else {
                android.util.Log.e("MDMService", "App not found: " + packageName);
            }
        } catch (Exception e) {
            android.util.Log.e("MDMService", "Error launching app: " + packageName, e);
        }
    }
    
    public void installApp(final String appId, final String appName, final String packageName, final String downloadUrl) {
        new AsyncTask<Void, Void, File>() {
            @Override
            protected File doInBackground(Void... voids) {
                try {
                    android.util.Log.d("MDMService", "Downloading APK from: " + downloadUrl);
                    
                    URL url = new URL(downloadUrl);
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestMethod("GET");
                    connection.setConnectTimeout(15000);
                    connection.setReadTimeout(15000);
                    connection.connect();
                    
                    if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                        android.util.Log.e("MDMService", "Download failed with code: " + connection.getResponseCode());
                        return null;
                    }
                    
                    // Crear archivo temporal
                    File apkFile = new File(getExternalCacheDir(), appName + ".apk");
                    
                    InputStream input = connection.getInputStream();
                    OutputStream output = new FileOutputStream(apkFile);
                    
                    byte[] buffer = new byte[4096];
                    int bytesRead;
                    long totalBytes = 0;
                    
                    while ((bytesRead = input.read(buffer)) != -1) {
                        output.write(buffer, 0, bytesRead);
                        totalBytes += bytesRead;
                    }
                    
                    output.flush();
                    output.close();
                    input.close();
                    connection.disconnect();
                    
                    android.util.Log.d("MDMService", "APK downloaded successfully: " + totalBytes + " bytes");
                    return apkFile;
                    
                } catch (Exception e) {
                    android.util.Log.e("MDMService", "Error downloading APK", e);
                    return null;
                }
            }
            
            @Override
            protected void onPostExecute(File apkFile) {
                if (apkFile != null && apkFile.exists()) {
                    installApkFile(apkFile, appName);
                } else {
                    android.util.Log.e("MDMService", "APK download failed");
                    showNotification("Instalación fallida", "No se pudo descargar " + appName);
                }
            }
        }.execute();
    }
    
    private void installApkFile(File apkFile, String appName) {
        try {
            android.util.Log.d("MDMService", "Starting installation for: " + appName);
            
            // Verificar si somos Device Owner
            DevicePolicyManager dpm = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
            ComponentName adminComponent = new ComponentName(this, com.androidmdm.app.admin.MDMDeviceAdminReceiver.class);
            
            // Detectar si es auto-actualización del MDM
            boolean isSelfUpdate = false;
            try {
                PackageManager pm = getPackageManager();
                android.content.pm.PackageInfo info = pm.getPackageArchiveInfo(apkFile.getAbsolutePath(), 0);
                if (info != null && info.packageName != null) {
                    isSelfUpdate = info.packageName.equals(getPackageName());
                    if (isSelfUpdate) {
                        android.util.Log.d("MDMService", "🔄 SELF-UPDATE DETECTED - Installing own package: " + info.packageName);
                    }
                }
            } catch (Exception e) {
                android.util.Log.w("MDMService", "Could not detect package name from APK", e);
            }
            
            if (dpm.isDeviceOwnerApp(getPackageName())) {
                // INSTALACIÓN SILENCIOSA con Device Owner
                android.util.Log.d("MDMService", "Device Owner detected - Silent installation");
                installApkSilently(apkFile, appName, isSelfUpdate);
            } else {
                // INSTALACIÓN MANUAL con diálogo (fallback)
                android.util.Log.w("MDMService", "NOT Device Owner - Manual installation required");
                installApkManually(apkFile, appName);
            }
            
        } catch (Exception e) {
            android.util.Log.e("MDMService", "Error during installation", e);
            showNotification("Error de instalación", "No se pudo instalar: " + appName);
        }
    }
    
    private void installApkSilently(File apkFile, String appName, boolean isSelfUpdate) {
        try {
            PackageInstaller packageInstaller = getPackageManager().getPackageInstaller();
            PackageInstaller.SessionParams params = new PackageInstaller.SessionParams(
                PackageInstaller.SessionParams.MODE_FULL_INSTALL
            );
            
            // CRITICAL: Para auto-actualización MDM, usar flag especial
            if (isSelfUpdate && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                // Android 12+ (API 31+): Requiere flag para self-update
                params.setRequireUserAction(PackageInstaller.SessionParams.USER_ACTION_NOT_REQUIRED);
                android.util.Log.d("MDMService", "🔄 Self-update mode enabled (API 31+)");
            }
            
            int sessionId = packageInstaller.createSession(params);
            PackageInstaller.Session session = packageInstaller.openSession(sessionId);
            
            OutputStream out = session.openWrite(appName, 0, apkFile.length());
            InputStream in = new FileInputStream(apkFile);
            
            byte[] buffer = new byte[65536];
            int c;
            while ((c = in.read(buffer)) != -1) {
                out.write(buffer, 0, c);
            }
            
            session.fsync(out);
            in.close();
            out.close();
            
            Intent intent = new Intent(this, InstallReceiver.class);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                this,
                sessionId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE
            );
            
            session.commit(pendingIntent.getIntentSender());
            session.close();
            
            if (isSelfUpdate) {
                android.util.Log.d("MDMService", "✅ MDM self-update initiated silently for: " + appName);
                showNotification("Actualizando MDM", "Instalando nueva versión automáticamente...");
            } else {
                android.util.Log.d("MDMService", "✅ Silent installation initiated for: " + appName);
                showNotification("Instalando app silenciosamente", appName);
            }
            
        } catch (Exception e) {
            android.util.Log.e("MDMService", "❌ Error in silent installation", e);
            showNotification("Error de instalación silenciosa", "No se pudo instalar: " + appName);
        }
    }
    
    private void installApkManually(File apkFile, String appName) {
        try {
            // Verificar si tenemos permiso para solicitar instalaciones
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (!getPackageManager().canRequestPackageInstalls()) {
                    android.util.Log.e("MDMService", "No permission to install apps. User must grant REQUEST_INSTALL_PACKAGES");
                    showNotification("Permiso requerido", "Habilita 'Instalar apps desconocidas' en Configuración para " + appName);
                    return;
                }
            }
            
            // Usar FileProvider para obtener URI del APK
            Uri apkUri = FileProvider.getUriForFile(
                this,
                getPackageName() + ".fileprovider",
                apkFile
            );
            
            // Crear intent de instalación
            Intent installIntent = new Intent(Intent.ACTION_VIEW);
            installIntent.setDataAndType(apkUri, "application/vnd.android.package-archive");
            installIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            installIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            
            // Iniciar instalación
            startActivity(installIntent);
            
            android.util.Log.d("MDMService", "Manual installation intent launched for: " + appName);
            showNotification("Instalación manual", "Confirma la instalación de: " + appName);
            
        } catch (Exception e) {
            android.util.Log.e("MDMService", "Error during manual installation", e);
            showNotification("Error de instalación", "No se pudo iniciar instalación de: " + appName);
        }
    }
    
    public void uninstallApp(String packageName) {
        try {
            android.util.Log.d("MDMService", "🗑️ Uninstall requested for: " + packageName);
            
            // Verificar si somos Device Owner
            if (devicePolicyManager != null && devicePolicyManager.isDeviceOwnerApp(getPackageName())) {
                // DESINSTALACIÓN SILENCIOSA con Device Owner (sin confirmación del usuario)
                android.util.Log.d("MDMService", "✅ Device Owner detected - Silent uninstallation via DPM");
                
                // Crear callback para resultado de desinstalación
                Intent intent = new Intent(this, InstallReceiver.class);
                intent.putExtra("packageName", packageName);
                intent.putExtra("action", "uninstall");
                
                PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    this,
                    packageName.hashCode(),
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE
                );
                
                // CRITICAL: Device Owner debe usar DevicePolicyManager.setUninstallBlocked(false)
                // seguido de PackageInstaller.uninstall() O usar el método directo (API 28+)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    // Android 9+ (API 28+): Usar método Device Owner directo
                    devicePolicyManager.setUninstallBlocked(adminComponent, packageName, false);
                    PackageInstaller packageInstaller = getPackageManager().getPackageInstaller();
                    packageInstaller.uninstall(packageName, pendingIntent.getIntentSender());
                    android.util.Log.d("MDMService", "✅ Silent uninstallation via DPM (API 28+) for: " + packageName);
                } else {
                    // Android 8 y anteriores: PackageInstaller directo con permisos Device Owner
                    PackageInstaller packageInstaller = getPackageManager().getPackageInstaller();
                    packageInstaller.uninstall(packageName, pendingIntent.getIntentSender());
                    android.util.Log.d("MDMService", "✅ Silent uninstallation via PackageInstaller for: " + packageName);
                }
                
                showNotification("Desinstalando app", "Desinstalación automática de " + packageName);
                
            } else {
                // DESINSTALACIÓN MANUAL con diálogo (fallback si no somos Device Owner)
                android.util.Log.w("MDMService", "⚠️ NOT Device Owner - Manual uninstallation required");
                
                Intent intent = new Intent(Intent.ACTION_DELETE);
                intent.setData(Uri.parse("package:" + packageName));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                
                android.util.Log.d("MDMService", "Manual uninstall intent launched for: " + packageName);
                showNotification("Desinstalación manual", "Confirma la desinstalación de " + packageName);
            }
            
        } catch (Exception e) {
            android.util.Log.e("MDMService", "❌ Error uninstalling app: " + packageName, e);
            showNotification("Error de desinstalación", "No se pudo desinstalar: " + packageName);
            
            // Notificar error al backend
            if (webSocketManager != null) {
                webSocketManager.sendUninstallResult(packageName, false, e.getMessage());
            }
        }
    }
    
    private void showNotification(String title, String message) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(R.drawable.ic_notification)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true);
        
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify((int) System.currentTimeMillis(), builder.build());
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        
        if (heartbeatHandler != null && heartbeatRunnable != null) {
            heartbeatHandler.removeCallbacks(heartbeatRunnable);
        }
        
        if (installResultReceiver != null) {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(installResultReceiver);
        }
        
        if (uninstallResultReceiver != null) {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(uninstallResultReceiver);
        }
        
        if (webSocketManager != null) {
            webSocketManager.disconnect();
        }
    }
    
    private void registerInstallResultReceiver() {
        installResultReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String packageName = intent.getStringExtra("packageName");
                boolean success = intent.getBooleanExtra("success", false);
                String errorMessage = intent.getStringExtra("errorMessage");
                
                Log.d("MDMService", "📬 Install result received: " + packageName + " - success: " + success);
                
                // Send result via WebSocket
                if (webSocketManager != null) {
                    webSocketManager.sendInstallResult(packageName, success, errorMessage);
                }
            }
        };
        
        IntentFilter filter = new IntentFilter("com.androidmdm.app.INSTALL_RESULT");
        LocalBroadcastManager.getInstance(this).registerReceiver(installResultReceiver, filter);
        Log.d("MDMService", "✅ Install result receiver registered");
    }
    
    private void registerUninstallResultReceiver() {
        uninstallResultReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String packageName = intent.getStringExtra("packageName");
                boolean success = intent.getBooleanExtra("success", false);
                String errorMessage = intent.getStringExtra("errorMessage");
                
                Log.d("MDMService", "📬 Uninstall result received: " + packageName + " - success: " + success);
                
                // Send result via WebSocket
                if (webSocketManager != null) {
                    webSocketManager.sendUninstallResult(packageName, success, errorMessage);
                }
            }
        };
        
        IntentFilter filter = new IntentFilter("com.androidmdm.app.UNINSTALL_RESULT");
        LocalBroadcastManager.getInstance(this).registerReceiver(uninstallResultReceiver, filter);
        Log.d("MDMService", "✅ Uninstall result receiver registered");
    }
    
    private void checkAndActivateKioskMode() {
        SharedPreferences prefs = getSharedPreferences("MDMKiosk", MODE_PRIVATE);
        boolean kioskEnabled = prefs.getBoolean("kiosk_enabled", false);
        String kioskPackage = prefs.getString("kiosk_package", null);
        
        if (kioskEnabled && kioskPackage != null) {
            Log.d("MDMService", "🔒 Restoring kiosk mode on boot: " + kioskPackage);
            enableKioskMode(kioskPackage);
        }
    }
    
    public void enableKioskMode(String packageName) {
        try {
            Log.d("MDMService", "🔒 Enabling kiosk mode for: " + packageName);
            
            if (devicePolicyManager != null && devicePolicyManager.isDeviceOwnerApp(getPackageName())) {
                // Save kiosk state
                SharedPreferences prefs = getSharedPreferences("MDMKiosk", MODE_PRIVATE);
                prefs.edit()
                    .putBoolean("kiosk_enabled", true)
                    .putString("kiosk_package", packageName)
                    .apply();
                
                // Set lock task packages (whitelist MDM + target app)
                devicePolicyManager.setLockTaskPackages(adminComponent, new String[]{
                    getPackageName(),  // MDM app itself
                    packageName        // Target kiosk app
                });
                
                // Launch using KioskLauncherActivity that will start Lock Task Mode
                Intent kioskIntent = new Intent(this, com.androidmdm.app.ui.KioskLauncherActivity.class);
                kioskIntent.putExtra(com.androidmdm.app.ui.KioskLauncherActivity.EXTRA_PACKAGE_NAME, packageName);
                kioskIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(kioskIntent);
                
                Log.d("MDMService", "✅ Kiosk mode activation initiated for: " + packageName);
                showNotification("Modo Kiosk Activado", "App bloqueada: " + packageName);
                
                Log.d("MDMService", "✅ Kiosk mode activation initiated for: " + packageName);
                showNotification("Modo Kiosk Activado", "App bloqueada: " + packageName);
            } else {
                Log.w("MDMService", "⚠️ NOT Device Owner - Kiosk mode requires Device Owner");
                showNotification("Kiosk no disponible", "Requiere permisos Device Owner");
            }
        } catch (Exception e) {
            Log.e("MDMService", "❌ Error enabling kiosk mode", e);
            showNotification("Error Kiosk", "No se pudo activar: " + e.getMessage());
        }
    }
    
    public void disableKioskMode() {
        try {
            Log.d("MDMService", "🔓 Disabling kiosk mode");
            
            if (devicePolicyManager != null && devicePolicyManager.isDeviceOwnerApp(getPackageName())) {
                // Clear kiosk state
                SharedPreferences prefs = getSharedPreferences("MDMKiosk", MODE_PRIVATE);
                prefs.edit()
                    .putBoolean("kiosk_enabled", false)
                    .remove("kiosk_package")
                    .apply();
                
                // Clear lock task packages
                devicePolicyManager.setLockTaskPackages(adminComponent, new String[]{});
                
                // Lanzar activity que detendrá el Lock Task Mode
                Intent stopKioskIntent = new Intent(this, com.androidmdm.app.ui.MainActivity.class);
                stopKioskIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                stopKioskIntent.putExtra("STOP_LOCK_TASK", true);
                startActivity(stopKioskIntent);
                
                Log.d("MDMService", "✅ Kiosk mode disabled");
                showNotification("Modo Kiosk Desactivado", "Dispositivo liberado");
            } else {
                Log.w("MDMService", "⚠️ NOT Device Owner");
            }
        } catch (Exception e) {
            Log.e("MDMService", "❌ Error disabling kiosk mode", e);
            showNotification("Error Kiosk", "No se pudo desactivar: " + e.getMessage());
        }
    }
    
    /**
     * Configurar horarios de encendido/apagado programados
     * @param scheduleJson JSONObject con estructura: { enabled, powerOnTime, powerOffTime, daysOfWeek }
     */
    public void setPowerSchedule(JSONObject scheduleJson) {
        try {
            Log.d("MDMService", "⏰ Setting power schedule: " + scheduleJson.toString());
            
            boolean enabled = scheduleJson.getBoolean("enabled");
            String powerOnTime = scheduleJson.getString("powerOnTime");   // "08:00"
            String powerOffTime = scheduleJson.getString("powerOffTime"); // "22:00"
            JSONArray daysArray = scheduleJson.getJSONArray("daysOfWeek"); // [0,1,2,3,4]
            
            // Guardar en SharedPreferences
            SharedPreferences prefs = getSharedPreferences("MDMSchedule", MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putBoolean("schedule_enabled", enabled);
            editor.putString("power_on_time", powerOnTime);
            editor.putString("power_off_time", powerOffTime);
            
            // Convertir JSONArray a String para guardar
            StringBuilder daysStr = new StringBuilder();
            for (int i = 0; i < daysArray.length(); i++) {
                if (i > 0) daysStr.append(",");
                daysStr.append(daysArray.getInt(i));
            }
            editor.putString("days_of_week", daysStr.toString());
            editor.apply();
            
            if (enabled) {
                // Configurar alarmas
                AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
                
                if (alarmManager != null) {
                    // === ALARMA POWER OFF ===
                    Intent powerOffIntent = new Intent(this, PowerScheduleReceiver.class);
                    powerOffIntent.setAction(PowerScheduleReceiver.ACTION_POWER_OFF);
                    
                    PendingIntent powerOffPendingIntent = PendingIntent.getBroadcast(
                        this,
                        1001, // Request code único
                        powerOffIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                    );
                    
                    // Parsear hora de apagado (formato "HH:mm")
                    String[] offParts = powerOffTime.split(":");
                    int offHour = Integer.parseInt(offParts[0]);
                    int offMinute = Integer.parseInt(offParts[1]);
                    
                    Calendar offCalendar = Calendar.getInstance();
                    offCalendar.set(Calendar.HOUR_OF_DAY, offHour);
                    offCalendar.set(Calendar.MINUTE, offMinute);
                    offCalendar.set(Calendar.SECOND, 0);
                    
                    // Si la hora ya pasó hoy, programar para mañana
                    if (offCalendar.getTimeInMillis() < System.currentTimeMillis()) {
                        offCalendar.add(Calendar.DAY_OF_YEAR, 1);
                    }
                    
                    // Configurar alarma repetitiva diaria
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        alarmManager.setRepeating(
                            AlarmManager.RTC_WAKEUP,
                            offCalendar.getTimeInMillis(),
                            AlarmManager.INTERVAL_DAY,
                            powerOffPendingIntent
                        );
                    } else {
                        alarmManager.setRepeating(
                            AlarmManager.RTC_WAKEUP,
                            offCalendar.getTimeInMillis(),
                            AlarmManager.INTERVAL_DAY,
                            powerOffPendingIntent
                        );
                    }
                    
                    Log.d("MDMService", "✅ Power OFF alarm set for: " + powerOffTime);
                    
                    // === ALARMA POWER ON ===
                    Intent powerOnIntent = new Intent(this, PowerScheduleReceiver.class);
                    powerOnIntent.setAction(PowerScheduleReceiver.ACTION_POWER_ON);
                    
                    PendingIntent powerOnPendingIntent = PendingIntent.getBroadcast(
                        this,
                        1002, // Request code único
                        powerOnIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                    );
                    
                    // Parsear hora de encendido
                    String[] onParts = powerOnTime.split(":");
                    int onHour = Integer.parseInt(onParts[0]);
                    int onMinute = Integer.parseInt(onParts[1]);
                    
                    Calendar onCalendar = Calendar.getInstance();
                    onCalendar.set(Calendar.HOUR_OF_DAY, onHour);
                    onCalendar.set(Calendar.MINUTE, onMinute);
                    onCalendar.set(Calendar.SECOND, 0);
                    
                    if (onCalendar.getTimeInMillis() < System.currentTimeMillis()) {
                        onCalendar.add(Calendar.DAY_OF_YEAR, 1);
                    }
                    
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        alarmManager.setRepeating(
                            AlarmManager.RTC_WAKEUP,
                            onCalendar.getTimeInMillis(),
                            AlarmManager.INTERVAL_DAY,
                            powerOnPendingIntent
                        );
                    } else {
                        alarmManager.setRepeating(
                            AlarmManager.RTC_WAKEUP,
                            onCalendar.getTimeInMillis(),
                            AlarmManager.INTERVAL_DAY,
                            powerOnPendingIntent
                        );
                    }
                    
                    Log.d("MDMService", "✅ Power ON alarm set for: " + powerOnTime);
                    
                    showNotification("Horarios Configurados", 
                        "ON: " + powerOnTime + " | OFF: " + powerOffTime);
                }
            } else {
                // Limpiar alarmas
                clearPowerSchedule();
            }
            
        } catch (Exception e) {
            Log.e("MDMService", "❌ Error setting power schedule", e);
            showNotification("Error Horarios", "No se pudo configurar: " + e.getMessage());
        }
    }
    
    /**
     * Limpiar horarios programados
     */
    public void clearPowerSchedule() {
        try {
            Log.d("MDMService", "🗑️ Clearing power schedule");
            
            // Limpiar SharedPreferences
            SharedPreferences prefs = getSharedPreferences("MDMSchedule", MODE_PRIVATE);
            prefs.edit().clear().apply();
            
            // Cancelar alarmas
            AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
            if (alarmManager != null) {
                // Cancelar alarma OFF
                Intent powerOffIntent = new Intent(this, PowerScheduleReceiver.class);
                powerOffIntent.setAction(PowerScheduleReceiver.ACTION_POWER_OFF);
                PendingIntent powerOffPendingIntent = PendingIntent.getBroadcast(
                    this, 1001, powerOffIntent, 
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                );
                alarmManager.cancel(powerOffPendingIntent);
                
                // Cancelar alarma ON
                Intent powerOnIntent = new Intent(this, PowerScheduleReceiver.class);
                powerOnIntent.setAction(PowerScheduleReceiver.ACTION_POWER_ON);
                PendingIntent powerOnPendingIntent = PendingIntent.getBroadcast(
                    this, 1002, powerOnIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                );
                alarmManager.cancel(powerOnPendingIntent);
                
                Log.d("MDMService", "✅ Power schedule alarms cancelled");
            }
            
            showNotification("Horarios Eliminados", "Programación desactivada");
            
        } catch (Exception e) {
            Log.e("MDMService", "❌ Error clearing power schedule", e);
        }
    }
}

