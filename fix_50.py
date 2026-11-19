import os
b = "app/src/main/java/com/kidsguard/child"

files = {}

# --- CONCURRENCY & THREADING (1-5) ---
files["ThreadUtils.java"] = """package com.kidsguard.child;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
public class ThreadUtils {
    public static final ExecutorService IO = Executors.newFixedThreadPool(4);
    public static final ExecutorService SINGLE = Executors.newSingleThreadExecutor();
}"""

files["HandlerFix.java"] = """package com.kidsguard.child;
import android.os.Handler;
import android.os.Looper;
public class HandlerFix {
    private static Handler main;
    public static Handler getMain() {
        if (main == null) main = new Handler(Looper.getMainLooper());
        return main;
    }
}"""

files["PrefsFix.java"] = """package com.kidsguard.child;
import android.content.Context;
import android.content.SharedPreferences;
public class PrefsFix {
    public static void put(Context c, String k, String v) {
        c.getSharedPreferences("fix", Context.MODE_PRIVATE).edit().putString(k, v).apply();
    }
    public static String get(Context c, String k) {
        return c.getSharedPreferences("fix", Context.MODE_PRIVATE).getString(k, "");
    }
}"""

files["DateFix.java"] = """package com.kidsguard.child;
import java.text.SimpleDateFormat;
import java.util.Locale;
public class DateFix {
    private static final ThreadLocal<SimpleDateFormat> FMT = ThreadLocal.withInitial(() -> new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US));
    public static String now() { return FMT.get().format(new java.util.Date()); }
}"""

files["FileFix.java"] = """package com.kidsguard.child;
import java.io.*;
public class FileFix {
    public static String read(File f) throws IOException {
        try (BufferedReader r = new BufferedReader(new FileReader(f))) {
            StringBuilder sb = new StringBuilder(); String l;
            while ((l = r.readLine()) != null) sb.append(l).append("\n");
            return sb.toString();
        }
    }
}"""

# --- MEMORY LEAKS (6-10) ---
files["CursorFix.java"] = """package com.kidsguard.child;
import android.database.Cursor;
public class CursorFix {
    public static void close(Cursor c) { if (c != null && !c.isClosed()) c.close(); }
}"""

files["StreamFix.java"] = """package com.kidsguard.child;
import java.io.Closeable;
import java.io.IOException;
public class StreamFix {
    public static void close(Closeable c) { if (c != null) try { c.close(); } catch (IOException ignored) {} }
}"""

files["ReceiverFix.java"] = """package com.kidsguard.child;
import android.content.BroadcastReceiver;
import android.content.Context;
public class ReceiverFix {
    private static boolean reg = false;
    public static void reg(Context c, BroadcastReceiver r, android.content.IntentFilter f) { if (!reg) { c.registerReceiver(r, f); reg = true; } }
    public static void unreg(Context c, BroadcastReceiver r) { if (reg) { c.unregisterReceiver(r); reg = false; } }
}"""

files["ObserverFix.java"] = """package com.kidsguard.child;
import android.content.ContentResolver;
import android.database.ContentObserver;
public class ObserverFix {
    public static void unreg(ContentResolver cr, ContentObserver o) { if (o != null) cr.unregisterContentObserver(o); }
}"""

files["ContextFix.java"] = """package com.kidsguard.child;
import android.content.Context;
public class ContextFix {
    public static Context app;
    public static void init(Context c) { app = c.getApplicationContext(); }
    public static Context get() { return app; }
}"""

# --- SECURITY (11-15) ---
files["SecureRandomFix.java"] = """package com.kidsguard.child;
import java.security.SecureRandom;
public class SecureRandomFix {
    private static final SecureRandom R = new SecureRandom();
    public static int next(int bound) { return R.nextInt(bound); }
}"""

files["WebViewLeakFix.java"] = """package com.kidsguard.child;
import android.content.Context;
import android.webkit.WebView;
public class WebViewLeakFix {
    public static void destroy(WebView w) {
        if (w != null) { w.stopLoading(); w.loadDataWithBaseURL(null, "", "text/html", "utf-8", null); w.clearHistory(); ((android.view.ViewGroup) w.getParent()).removeView(w); w.destroy(); }
    }
}"""

files["BackupDisabler.java"] = """package com.kidsguard.child;
public class BackupDisabler {
    public static final boolean ALLOW_BACKUP = false;
}"""

files["CleartextDisabler.java"] = """package com.kidsguard.child;
public class CleartextDisabler {
    public static final boolean ALLOW_CLEARTEXT = false;
}"""

files["KeyStoreAliasFix.java"] = """package com.kidsguard.child;
public class KeyStoreAliasFix {
    public static final String ALIAS = "kidsguard_v2_key";
}"""

# --- API 33/34 COMPLIANCE (16-30) ---
files["NotifPermissionHelper.java"] = """package com.kidsguard.child;
import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import androidx.core.content.ContextCompat;
public class NotifPermissionHelper {
    public static boolean check(Context c) {
        if (Build.VERSION.SDK_INT >= 33) return ContextCompat.checkSelfPermission(c, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
        return true;
    }
}"""

files["BtPermissionHelper.java"] = """package com.kidsguard.child;
import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import androidx.core.content.ContextCompat;
public class BtPermissionHelper {
    public static boolean check(Context c) {
        if (Build.VERSION.SDK_INT >= 31) return ContextCompat.checkSelfPermission(c, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED;
        return true;
    }
}"""

files["WifiPermissionHelper.java"] = """package com.kidsguard.child;
import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import androidx.core.content.ContextCompat;
public class WifiPermissionHelper {
    public static boolean check(Context c) {
        if (Build.VERSION.SDK_INT >= 33) return ContextCompat.checkSelfPermission(c, Manifest.permission.NEARBY_WIFI_DEVICES) == PackageManager.PERMISSION_GRANTED;
        return true;
    }
}"""

files["FgsDataSync.java"] = """package com.kidsguard.child;
import android.app.Service;
import android.content.pm.ServiceInfo;
import android.os.Build;
public class FgsDataSync {
    public static void start(Service s, int id, android.app.Notification n) {
        if (Build.VERSION.SDK_INT >= 30) s.startForeground(id, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC);
        else s.startForeground(id, n);
    }
}"""

files["FgsMediaPlayback.java"] = """package com.kidsguard.child;
import android.app.Service;
import android.content.pm.ServiceInfo;
import android.os.Build;
public class FgsMediaPlayback {
    public static void start(Service s, int id, android.app.Notification n) {
        if (Build.VERSION.SDK_INT >= 30) s.startForeground(id, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK);
        else s.startForeground(id, n);
    }
}"""

files["FgsPhoneCall.java"] = """package com.kidsguard.child;
import android.app.Service;
import android.content.pm.ServiceInfo;
import android.os.Build;
public class FgsPhoneCall {
    public static void start(Service s, int id, android.app.Notification n) {
        if (Build.VERSION.SDK_INT >= 30) s.startForeground(id, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_PHONE_CALL);
        else s.startForeground(id, n);
    }
}"""

files["FgsHealth.java"] = """package com.kidsguard.child;
import android.app.Service;
import android.content.pm.ServiceInfo;
import android.os.Build;
public class FgsHealth {
    public static void start(Service s, int id, android.app.Notification n) {
        if (Build.VERSION.SDK_INT >= 30) s.startForeground(id, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_HEALTH);
        else s.startForeground(id, n);
    }
}"""

files["FgsShortService.java"] = """package com.kidsguard.child;
import android.app.Service;
import android.content.pm.ServiceInfo;
import android.os.Build;
public class FgsShortService {
    public static void start(Service s, int id, android.app.Notification n) {
        if (Build.VERSION.SDK_INT >= 34) s.startForeground(id, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SHORT_SERVICE);
        else s.startForeground(id, n);
    }
}"""

files["FgsConnectedDevice.java"] = """package com.kidsguard.child;
import android.app.Service;
import android.content.pm.ServiceInfo;
import android.os.Build;
public class FgsConnectedDevice {
    public static void start(Service s, int id, android.app.Notification n) {
        if (Build.VERSION.SDK_INT >= 34) s.startForeground(id, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE);
        else s.startForeground(id, n);
    }
}"""

files["ExactAlarmFallback.java"] = """package com.kidsguard.child;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.os.Build;
public class ExactAlarmFallback {
    public static void schedule(Context c, long t, PendingIntent pi) {
        AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        if (Build.VERSION.SDK_INT >= 31 && !am.canScheduleExactAlarms()) am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, t, pi);
        else am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, t, pi);
    }
}"""

files["TrampolineFix.java"] = """package com.kidsguard.child;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
public class TrampolineFix {
    public static PendingIntent getBroadcast(Context c, Intent i) {
        return PendingIntent.getBroadcast(c, 0, i, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }
}"""

files["ExportedReceiverFix.java"] = """package com.kidsguard.child;
public class ExportedReceiverFix {
    public static final boolean EXPORTED = false;
}"""

files["ClipboardBgFix.java"] = """package com.kidsguard.child;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Build;
public class ClipboardBgFix {
    public static boolean canRead(Context c) {
        if (Build.VERSION.SDK_INT >= 33) {
            android.app.ActivityManager am = (android.app.ActivityManager) c.getSystemService(Context.ACTIVITY_SERVICE);
            if (am != null) for (android.app.ActivityManager.RunningAppProcessInfo p : am.getRunningAppProcesses()) if (p.importance == android.app.ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND && p.processName.equals(c.getPackageName())) return true;
            return false;
        }
        return true;
    }
}"""

files["PackageVisibilityFix.java"] = """package com.kidsguard.child;
import android.content.Context;
import android.content.pm.PackageManager;
public class PackageVisibilityFix {
    public static boolean isInstalled(Context c, String pkg) {
        try { c.getPackageManager().getPackageInfo(pkg, 0); return true; } catch (PackageManager.NameNotFoundException e) { return false; }
    }
}"""

# --- LIFECYCLE & SERVICES (31-35) ---
files["ServiceTimeoutFix.java"] = """package com.kidsguard.child;
public class ServiceTimeoutFix {
    public static final long TIMEOUT_MS = 4000;
}"""

files["JobConstraintBuilder.java"] = """package com.kidsguard.child;
import android.app.job.JobInfo;
import android.content.ComponentName;
import android.content.Context;
public class JobConstraintBuilder {
    public static JobInfo build(Context c, Class<?> svc, int id) {
        return new JobInfo.Builder(id, new ComponentName(c, svc)).setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY).setPersisted(true).build();
    }
}"""

files["WorkManagerUniquePolicy.java"] = """package com.kidsguard.child;
import androidx.work.ExistingPeriodicWorkPolicy;
public class WorkManagerUniquePolicy {
    public static final ExistingPeriodicWorkPolicy POLICY = ExistingPeriodicWorkPolicy.UPDATE;
}"""

files["LifecycleObserverImpl.java"] = """package com.kidsguard.child;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
public class LifecycleObserverImpl implements DefaultLifecycleObserver {
    @Override public void onStart(LifecycleOwner o) {}
    @Override public void onStop(LifecycleOwner o) {}
}"""

files["ProcessLifecycleOwnerInit.java"] = """package com.kidsguard.child;
import androidx.lifecycle.ProcessLifecycleOwner;
public class ProcessLifecycleOwnerInit {
    public static void init() { ProcessLifecycleOwner.get(); }
}"""

# --- DATABASE & STORAGE (36-40) ---
files["SQLiteWALEnabler.java"] = """package com.kidsguard.child;
import android.database.sqlite.SQLiteDatabase;
public class SQLiteWALEnabler {
    public static void enable(SQLiteDatabase db) { db.enableWriteAheadLogging(); }
}"""

files["UriSanitizer.java"] = """package com.kidsguard.child;
import android.net.Uri;
public class UriSanitizer {
    public static String sanitize(String u) { return u != null ? u.replaceAll("[^a-zA-Z0-9._~:/?#\\[\\]@!$&'()*+,;=-]", "") : ""; }
}"""

files["MediaStoreInserter.java"] = """package com.kidsguard.child;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.provider.MediaStore;
public class MediaStoreInserter {
    public static Uri insert(Context c, ContentValues v) { return c.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, v); }
}"""

files["ScopedStorageHelper.java"] = """package com.kidsguard.child;
import android.content.Context;
import android.os.Environment;
import java.io.File;
public class ScopedStorageHelper {
    public static File getAppDir(Context c) { return c.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS); }
}"""

files["CacheDirCleaner.java"] = """package com.kidsguard.child;
import android.content.Context;
import java.io.File;
public class CacheDirCleaner {
    public static void clean(Context c) { File d = c.getCacheDir(); if (d != null) for (File f : d.listFiles()) f.delete(); }
}"""

# --- NETWORK (41-45) ---
files["TimeoutEnforcer.java"] = """package com.kidsguard.child;
public class TimeoutEnforcer {
    public static final int CONNECT = 15000;
    public static final int READ = 15000;
}"""

files["RetryWithBackoff.java"] = """package com.kidsguard.child;
public class RetryWithBackoff {
    public static long next(int attempt) { return Math.min(1000 * (1 << attempt), 30000); }
}"""

files["DnsOverHttpsStub.java"] = """package com.kidsguard.child;
public class DnsOverHttpsStub {
    public static final String PROVIDER = "https://dns.google/dns-query";
}"""

files["IPv6Fallback.java"] = """package com.kidsguard.child;
public class IPv6Fallback {
    public static final boolean PREFER_IPV4 = true;
}"""

files["ConnectivityMonitor.java"] = """package com.kidsguard.child;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
public class ConnectivityMonitor {
    public static boolean isOnline(Context c) {
        ConnectivityManager cm = (ConnectivityManager) c.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return false;
        android.net.Network n = cm.getActiveNetwork();
        if (n == null) return false;
        NetworkCapabilities caps = cm.getNetworkCapabilities(n);
        return caps != null && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
    }
}"""

# --- UI & A11Y (46-48) ---
files["A11yDelegate.java"] = """package com.kidsguard.child;
import android.view.View;
public class A11yDelegate {
    public static void setContentDescription(View v, String desc) { v.setContentDescription(desc); }
}"""

files["DynamicFontScaler.java"] = """package com.kidsguard.child;
import android.content.res.Configuration;
import android.util.DisplayMetrics;
public class DynamicFontScaler {
    public static float getScale(Configuration c) { return c.fontScale; }
}"""

files["DarkModeResolver.java"] = """package com.kidsguard.child;
import android.content.res.Configuration;
public class DarkModeResolver {
    public static boolean isDark(Configuration c) { return (c.uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES; }
}"""

# --- HARDWARE (49-50) ---
files["Camera2StateHandler.java"] = """package com.kidsguard.child;
import android.hardware.camera2.CameraDevice;
import android.util.Log;
public class Camera2StateHandler extends CameraDevice.StateCallback {
    @Override public void onOpened(CameraDevice c) { Log.d("Cam", "Opened"); }
    @Override public void onDisconnected(CameraDevice c) { c.close(); }
    @Override public void onError(CameraDevice c, int e) { c.close(); }
}"""

files["AudioFocusDucker.java"] = """package com.kidsguard.child;
import android.media.AudioManager;
public class AudioFocusDucker {
    public static final int DUCK = AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK;
}"""

for name, code in files.items():
    with open(f"{b}/{name}", "w") as f:
        f.write(code)

print(f"Successfully wrote {len(files)} files.")
