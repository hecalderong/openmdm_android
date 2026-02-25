package com.androidmdm.app.util;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Environment;
import android.os.StatFs;
import android.provider.Settings;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Collections;
import java.util.List;

public class DeviceInfoUtil {
    
    public static String getDeviceId(Context context) {
        return Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
    }
    
    public static String getDeviceName() {
        return Build.MODEL;
    }
    
    public static String getManufacturer() {
        return Build.MANUFACTURER;
    }
    
    public static String getModel() {
        return Build.MODEL;
    }
    
    public static String getAndroidVersion() {
        return Build.VERSION.RELEASE;
    }
    
    public static String getSerialNumber() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                return Build.getSerial();
            } else {
                return Build.SERIAL;
            }
        } catch (SecurityException e) {
            // No tenemos permiso para acceder al serial
            // Usar Android ID como alternativa
            return "UNKNOWN";
        }
    }
    
    public static float getBatteryLevel(Context context) {
        IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = context.registerReceiver(null, ifilter);
        
        if (batteryStatus != null) {
            int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
            return level * 100 / (float) scale;
        }
        
        return 0;
    }
    
    public static long getStorageUsed() {
        StatFs stat = new StatFs(Environment.getDataDirectory().getPath());
        long blockSize = stat.getBlockSizeLong();
        long totalBlocks = stat.getBlockCountLong();
        long availableBlocks = stat.getAvailableBlocksLong();
        
        return (totalBlocks - availableBlocks) * blockSize;
    }
    
    public static long getStorageTotal() {
        StatFs stat = new StatFs(Environment.getDataDirectory().getPath());
        return stat.getBlockCountLong() * stat.getBlockSizeLong();
    }
    
    public static long getRamUsed(Context context) {
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
        activityManager.getMemoryInfo(memoryInfo);
        
        return memoryInfo.totalMem - memoryInfo.availMem;
    }
    
    public static long getRamTotal(Context context) {
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
        activityManager.getMemoryInfo(memoryInfo);
        
        return memoryInfo.totalMem;
    }
    
    public static String getIPAddress(Context context) {
        try {
            // Try WiFi first
            WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            if (wifiManager != null) {
                WifiInfo wifiInfo = wifiManager.getConnectionInfo();
                int ipAddress = wifiInfo.getIpAddress();
                
                if (ipAddress != 0) {
                    return String.format("%d.%d.%d.%d",
                            (ipAddress & 0xff),
                            (ipAddress >> 8 & 0xff),
                            (ipAddress >> 16 & 0xff),
                            (ipAddress >> 24 & 0xff));
                }
            }
            
            // Try other network interfaces
            List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface intf : interfaces) {
                List<InetAddress> addrs = Collections.list(intf.getInetAddresses());
                for (InetAddress addr : addrs) {
                    if (!addr.isLoopbackAddress()) {
                        String sAddr = addr.getHostAddress();
                        boolean isIPv4 = sAddr.indexOf(':') < 0;
                        if (isIPv4) {
                            return sAddr;
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return "Unknown";
    }
    
    public static String getDeviceInfo(Context context) {
        StringBuilder info = new StringBuilder();
        info.append("Device ID: ").append(getDeviceId(context)).append("\n");
        info.append("Manufacturer: ").append(getManufacturer()).append("\n");
        info.append("Model: ").append(getModel()).append("\n");
        info.append("Android Version: ").append(getAndroidVersion()).append("\n");
        info.append("Battery: ").append((int) getBatteryLevel(context)).append("%\n");
        info.append("IP Address: ").append(getIPAddress(context));
        
        return info.toString();
    }
}
