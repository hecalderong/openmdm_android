package com.androidmdm.app.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInstaller;
import android.util.Log;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

public class InstallReceiver extends BroadcastReceiver {
    private static final String TAG = "InstallReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        int status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, -1);
        String message = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE);
        String packageName = intent.getStringExtra(PackageInstaller.EXTRA_PACKAGE_NAME);
        String action = intent.getStringExtra("action"); // "install" o "uninstall"

        // Si no viene packageName en EXTRA_PACKAGE_NAME, intentar obtenerlo del intent
        if (packageName == null) {
            packageName = intent.getStringExtra("packageName");
        }

        switch (status) {
            case PackageInstaller.STATUS_PENDING_USER_ACTION:
                Log.d(TAG, "Action pending user confirmation for: " + packageName);
                Intent confirmIntent = intent.getParcelableExtra(Intent.EXTRA_INTENT);
                if (confirmIntent != null) {
                    confirmIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    try {
                        context.startActivity(confirmIntent);
                    } catch (Exception e) {
                        Log.e(TAG, "Failed to start confirmation activity", e);
                    }
                }
                break;

            case PackageInstaller.STATUS_SUCCESS:
                if ("uninstall".equals(action)) {
                    Log.d(TAG, "✅ Uninstallation successful for: " + packageName);
                    notifyUninstallResult(context, packageName, true, null);
                } else {
                    Log.d(TAG, "✅ Installation successful for: " + packageName);
                    notifyInstallResult(context, packageName, true, null);
                }
                break;

            case PackageInstaller.STATUS_FAILURE:
            case PackageInstaller.STATUS_FAILURE_ABORTED:
            case PackageInstaller.STATUS_FAILURE_BLOCKED:
            case PackageInstaller.STATUS_FAILURE_CONFLICT:
            case PackageInstaller.STATUS_FAILURE_INCOMPATIBLE:
            case PackageInstaller.STATUS_FAILURE_INVALID:
            case PackageInstaller.STATUS_FAILURE_STORAGE:
                if ("uninstall".equals(action)) {
                    Log.e(TAG, "❌ Uninstallation failed for: " + packageName + " - " + message);
                    notifyUninstallResult(context, packageName, false, message);
                } else {
                    Log.e(TAG, "❌ Installation failed for: " + packageName + " - " + message);
                    notifyInstallResult(context, packageName, false, message);
                }
                break;
        }
    }
    
    
    private void notifyInstallResult(Context context, String packageName, boolean success, String errorMessage) {
        Intent intent = new Intent("com.androidmdm.app.INSTALL_RESULT");
        intent.putExtra("packageName", packageName);
        intent.putExtra("success", success);
        if (errorMessage != null) {
            intent.putExtra("errorMessage", errorMessage);
        }
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
        Log.d(TAG, "📡 Install result broadcast sent");
    }
    
    private void notifyUninstallResult(Context context, String packageName, boolean success, String errorMessage) {
        Intent intent = new Intent("com.androidmdm.app.UNINSTALL_RESULT");
        intent.putExtra("packageName", packageName);
        intent.putExtra("success", success);
        if (errorMessage != null) {
            intent.putExtra("errorMessage", errorMessage);
        }
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
        Log.d(TAG, "📡 Uninstall result broadcast sent");
    }
}

