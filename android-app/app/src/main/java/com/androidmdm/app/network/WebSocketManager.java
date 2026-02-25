package com.androidmdm.app.network;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import com.androidmdm.app.util.DeviceInfoUtil;
import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;
import org.json.JSONException;
import org.json.JSONObject;
import java.net.URISyntaxException;

public class WebSocketManager {
    
    private static final String TAG = "WebSocketManager";
    
    private Socket socket;
    private Context context;
    private String serverUrl;
    
    public WebSocketManager(Context context, String serverUrl) {
        this.context = context;
        this.serverUrl = serverUrl;
    }
    
    public void connect() {
        try {
            IO.Options opts = new IO.Options();
            opts.forceNew = true;
            opts.reconnection = true;
            opts.reconnectionAttempts = Integer.MAX_VALUE;
            opts.reconnectionDelay = 1000;
            opts.reconnectionDelayMax = 5000;
            opts.timeout = 20000;
            opts.transports = new String[] {"websocket", "polling"};
            
            socket = IO.socket(serverUrl, opts);
            
            socket.on(Socket.EVENT_CONNECT, onConnect);
            socket.on(Socket.EVENT_DISCONNECT, onDisconnect);
            socket.on(Socket.EVENT_CONNECT_ERROR, onConnectError);
            socket.on("command", onCommand);
            
            socket.connect();
            Log.d(TAG, "Attempting to connect to: " + serverUrl);
            
        } catch (URISyntaxException e) {
            Log.e(TAG, "Error connecting to WebSocket", e);
        }
    }
    
    private Emitter.Listener onConnect = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            Log.d(TAG, "Connected to server");
            registerDevice();
        }
    };
    
    private Emitter.Listener onDisconnect = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            Log.d(TAG, "Disconnected from server");
        }
    };
    
    private Emitter.Listener onConnectError = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            Log.e(TAG, "Connection error: " + args[0]);
        }
    };
    
    private Emitter.Listener onCommand = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            try {
                JSONObject command = (JSONObject) args[0];
                String type = command.getString("type");
                
                Log.d(TAG, "Received command: " + type);
                
                switch (type) {
                    case "lock":
                        handleLockCommand();
                        break;
                    case "unlock":
                        handleUnlockCommand();
                        break;
                    case "launch_app":
                        handleLaunchApp(command.getJSONObject("payload"));
                        break;
                    case "install_app":
                        handleInstallApp(command.getJSONObject("payload"));
                        break;
                    case "uninstall_app":
                        handleUninstallApp(command.getJSONObject("payload"));
                        break;
                    case "enable_kiosk":
                        handleEnableKiosk(command.getJSONObject("payload"));
                        break;
                    case "disable_kiosk":
                        handleDisableKiosk();
                        break;
                    case "set_power_schedule":
                        handleSetPowerSchedule(command.getJSONObject("payload"));
                        break;
                    case "clear_power_schedule":
                        handleClearPowerSchedule();
                        break;
                    default:
                        Log.w(TAG, "Unknown command type: " + type);
                }
            } catch (JSONException e) {
                Log.e(TAG, "Error parsing command", e);
            }
            if (args[0] instanceof Exception) {
                // Log the full stack trace for better debugging
                Log.e(TAG, "Connection error: ", (Exception) args[0]);
            } else {
                Log.e(TAG, "Connection error: " + args[0].toString());
            }
        }
    };
    
    private void registerDevice() {
        try {
            JSONObject data = new JSONObject();
            data.put("deviceId", DeviceInfoUtil.getDeviceId(context));
            data.put("name", DeviceInfoUtil.getDeviceName());
            data.put("manufacturer", DeviceInfoUtil.getManufacturer());
            data.put("model", DeviceInfoUtil.getModel());
            data.put("androidVersion", DeviceInfoUtil.getAndroidVersion());
            data.put("serialNumber", DeviceInfoUtil.getSerialNumber());
            
            socket.emit("device:register", data);
            Log.d(TAG, "Device registered");
        } catch (JSONException e) {
            Log.e(TAG, "Error registering device", e);
        }
    }
    
    public void sendHeartbeat(JSONObject heartbeat) {
        Log.d("WebSocketManager", "💓 sendHeartbeat called, socket connected: " + (socket != null && socket.connected()));
        if (socket != null && socket.connected()) {
            Log.d("WebSocketManager", "📤 Emitting heartbeat: " + heartbeat.toString());
            socket.emit("device:heartbeat", heartbeat);
            Log.d("WebSocketManager", "✅ Heartbeat emitted");
        } else {
            Log.w("WebSocketManager", "❌ Cannot send heartbeat - socket not connected");
        }
    }
    
    public void sendLocation(double latitude, double longitude) {
        try {
            JSONObject data = new JSONObject();
            data.put("deviceId", DeviceInfoUtil.getDeviceId(context));
            data.put("latitude", latitude);
            data.put("longitude", longitude);
            
            socket.emit("device:location", data);
        } catch (JSONException e) {
            Log.e(TAG, "Error sending location", e);
        }
    }
    
    public void sendInstallResult(String packageName, boolean success, String errorMessage) {
        if (socket != null && socket.connected()) {
            try {
                JSONObject data = new JSONObject();
                data.put("deviceId", DeviceInfoUtil.getDeviceId(context));
                data.put("packageName", packageName);
                data.put("success", success);
                if (errorMessage != null) {
                    data.put("errorMessage", errorMessage);
                }
                
                socket.emit("device:install-result", data);
                Log.d(TAG, "✅ Install result sent: " + packageName + " - success: " + success);
            } catch (JSONException e) {
                Log.e(TAG, "Error sending install result", e);
            }
        } else {
            Log.w(TAG, "❌ Cannot send install result - socket not connected");
        }
    }
    
    public void sendUninstallResult(String packageName, boolean success, String errorMessage) {
        if (socket != null && socket.connected()) {
            try {
                JSONObject data = new JSONObject();
                data.put("deviceId", DeviceInfoUtil.getDeviceId(context));
                data.put("packageName", packageName);
                data.put("success", success);
                if (errorMessage != null) {
                    data.put("errorMessage", errorMessage);
                }
                
                socket.emit("device:uninstall-result", data);
                Log.d(TAG, "✅ Uninstall result sent: " + packageName + " - success: " + success);
            } catch (JSONException e) {
                Log.e(TAG, "Error sending uninstall result", e);
            }
        } else {
            Log.w(TAG, "❌ Cannot send uninstall result - socket not connected");
        }
    }
    
    private void handleLockCommand() {
        // Implement device lock
        Log.d(TAG, "Lock command received");
    }
    
    private void handleUnlockCommand() {
        // Implement device unlock
        Log.d(TAG, "Unlock command received");
    }
    
    private void handleLaunchApp(JSONObject payload) {
        try {
            String packageName = payload.getString("packageName");
            Log.d(TAG, "Launch app command received: " + packageName);
            
            // Llamar al servicio MDM para lanzar la app
            Intent intent = new Intent(context, com.androidmdm.app.service.MDMService.class);
            intent.setAction("LAUNCH_APP");
            intent.putExtra("packageName", packageName);
            context.startService(intent);
            
        } catch (JSONException e) {
            Log.e(TAG, "Error handling launch app", e);
        }
    }
    
    private void handleInstallApp(JSONObject payload) {
        try {
            // appId puede ser null cuando es actualización del MDM
            String appId = payload.optString("appId", null);
            String appName = payload.getString("name");
            String packageName = payload.getString("packageName");
            
            // Intentar obtener downloadUrl o apkUrl (para compatibilidad con actualización MDM)
            String downloadUrl = payload.optString("downloadUrl", null);
            if (downloadUrl == null) {
                downloadUrl = payload.optString("apkUrl", null);
            }
            
            Log.d(TAG, "Install app command received: " + appName + " from " + downloadUrl);
            
            // Llamar al servicio MDM para instalar la app
            Intent intent = new Intent(context, com.androidmdm.app.service.MDMService.class);
            intent.setAction("INSTALL_APP");
            if (appId != null) {
                intent.putExtra("appId", appId);
            }
            intent.putExtra("appName", appName);
            intent.putExtra("packageName", packageName);
            intent.putExtra("downloadUrl", downloadUrl);
            context.startService(intent);
            
        } catch (JSONException e) {
            Log.e(TAG, "Error handling install app", e);
        }
    }
    
    private void handleUninstallApp(JSONObject payload) {
        try {
            String packageName = payload.getString("packageName");
            Log.d(TAG, "Uninstall app command received: " + packageName);
            
            // Llamar al servicio MDM para desinstalar la app
            Intent intent = new Intent(context, com.androidmdm.app.service.MDMService.class);
            intent.setAction("UNINSTALL_APP");
            intent.putExtra("packageName", packageName);
            context.startService(intent);
            
        } catch (JSONException e) {
            Log.e(TAG, "Error handling uninstall app", e);
        }
    }
    
    private void handleEnableKiosk(JSONObject payload) {
        try {
            String packageName = payload.getString("packageName");
            Log.d(TAG, "🔒 Enable kiosk mode command received: " + packageName);
            
            // Llamar al servicio MDM para activar modo kiosk
            Intent intent = new Intent(context, com.androidmdm.app.service.MDMService.class);
            intent.setAction("ENABLE_KIOSK");
            intent.putExtra("packageName", packageName);
            context.startService(intent);
            
        } catch (JSONException e) {
            Log.e(TAG, "Error handling enable kiosk", e);
        }
    }
    
    private void handleDisableKiosk() {
        Log.d(TAG, "🔓 Disable kiosk mode command received");
        
        // Llamar al servicio MDM para desactivar modo kiosk
        Intent intent = new Intent(context, com.androidmdm.app.service.MDMService.class);
        intent.setAction("DISABLE_KIOSK");
        context.startService(intent);
    }
    
    private void handleSetPowerSchedule(JSONObject payload) {
        try {
            Log.d(TAG, "⏰ Set power schedule command received: " + payload.toString());
            
            // Enviar al servicio MDM para configurar horarios
            Intent intent = new Intent(context, com.androidmdm.app.service.MDMService.class);
            intent.setAction("SET_POWER_SCHEDULE");
            intent.putExtra("scheduleJson", payload.toString());
            context.startService(intent);
            
        } catch (Exception e) {
            Log.e(TAG, "Error handling set power schedule", e);
        }
    }
    
    private void handleClearPowerSchedule() {
        Log.d(TAG, "🗑️ Clear power schedule command received");
        
        // Enviar al servicio MDM para limpiar horarios
        Intent intent = new Intent(context, com.androidmdm.app.service.MDMService.class);
        intent.setAction("CLEAR_POWER_SCHEDULE");
        context.startService(intent);
    }
    
    public void disconnect() {
        if (socket != null) {
            socket.disconnect();
            socket.off();
        }
    }
}
