import os
b = "app/src/main/java/com/kidsguard/child"

files = {}

# 1. LocalDatabase: Enable WAL to prevent "database is locked" crashes
files["LocalDatabase.java"] = """package com.kidsguard.child;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
public class LocalDatabase extends SQLiteOpenHelper {
    private static volatile LocalDatabase instance;
    public static synchronized LocalDatabase getInstance(Context ctx) {
        if (instance == null) instance = new LocalDatabase(ctx.getApplicationContext());
        return instance;
    }
    private LocalDatabase(Context c) { super(c, "kidsguard.db", null, 2); }
    @Override
    public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);
        db.enableWriteAheadLogging();
    }
    @Override public void onCreate(SQLiteDatabase db) { db.execSQL("CREATE TABLE IF NOT EXISTS logs (id INTEGER PRIMARY KEY, data TEXT)"); }
    @Override public void onUpgrade(SQLiteDatabase db, int o, int n) { db.execSQL("DROP TABLE IF EXISTS logs"); onCreate(db); }
}
"""

# 2. SmsReceiver: Abort broadcast to prevent duplicate saving
files["SmsReceiver.java"] = """package com.kidsguard.child;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.SmsMessage;
import android.util.Log;
public class SmsReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        final PendingResult pr = goAsync();
        new Thread(() -> {
            try {
                Bundle bundle = intent.getExtras();
                if (bundle != null) {
                    Object[] pdus = (Object[]) bundle.get("pdus");
                    if (pdus != null) {
                        for (Object pdu : pdus) {
                            SmsMessage msg = SmsMessage.createFromPdu((byte[]) pdu);
                            Log.d("SmsRx", "From: " + msg.getDisplayOriginatingAddress());
                        }
                    }
                }
                abortBroadcast();
            } catch (Exception e) { Log.e("SmsRx", "Fail", e); }
            finally { pr.finish(); }
        }).start();
    }
}
"""

# 3. NetworkMonitor: Prevent Context leak on unregister
files["NetworkMonitor.java"] = """package com.kidsguard.child;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.util.Log;
public class NetworkMonitor {
    private ConnectivityManager.NetworkCallback callback;
    public void start(Context ctx) {
        ConnectivityManager cm = (ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return;
        callback = new ConnectivityManager.NetworkCallback() {
            @Override public void onAvailable(Network network) { Log.d("Net", "Connected"); }
            @Override public void onLost(Network network) { Log.d("Net", "Lost"); }
        };
        NetworkRequest request = new NetworkRequest.Builder().addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET).build();
        cm.registerNetworkCallback(request, callback);
    }
    public void stop(Context ctx) {
        if (callback != null) {
            ConnectivityManager cm = (ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm != null) cm.unregisterNetworkCallback(callback);
            callback = null;
        }
    }
}
"""

# 4. ClipboardMonitor: Null-safe ClipData.Item handling
files["ClipboardMonitor.java"] = """package com.kidsguard.child;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.util.Log;
public class ClipboardMonitor {
    public void check(Context ctx) {
        ClipboardManager cm = (ClipboardManager) ctx.getSystemService(Context.CLIPBOARD_SERVICE);
        try {
            if (cm != null && cm.hasPrimaryClip()) {
                ClipData clip = cm.getPrimaryClip();
                if (clip != null && clip.getItemCount() > 0) {
                    ClipData.Item item = clip.getItemAt(0);
                    if (item != null) {
                        CharSequence text = item.getText();
                        if (text != null) Log.d("Clip", text.toString());
                    }
                }
            }
        } catch (SecurityException e) { Log.w("Clip", "Blocked"); }
    }
}
"""

# 5. MovementDetector: Proper lifecycle management
files["MovementDetector.java"] = """package com.kidsguard.child;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
public class MovementDetector implements SensorEventListener {
    private SensorManager sm;
    private Sensor sensor;
    public void start(Context ctx) {
        sm = (SensorManager) ctx.getSystemService(Context.SENSOR_SERVICE);
        sensor = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        if (sensor != null) sm.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL);
    }
    public void stop() { if (sm != null) sm.unregisterListener(this); }
    @Override public void onSensorChanged(SensorEvent e) {}
    @Override public void onAccuracyChanged(Sensor s, int a) {}
}
"""

# 6. AppListFetcher: Filter updated system apps correctly
files["AppListFetcher.java"] = """package com.kidsguard.child;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import java.util.ArrayList;
import java.util.List;
public class AppListFetcher {
    public List<PackageInfo> getUserApps(Context ctx) {
        List<PackageInfo> apps = new ArrayList<>();
        PackageManager pm = ctx.getPackageManager();
        List<PackageInfo> packages = pm.getInstalledPackages(0);
        for (PackageInfo p : packages) {
            if ((p.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) == 0 ||
                (p.applicationInfo.flags & ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0) {
                apps.add(p);
            }
        }
        return apps;
    }
}
"""

# 7. FloatingTimerWidget: Prevent WindowManager leak
files["FloatingTimerWidget.java"] = """package com.kidsguard.child;
import android.content.Context;
import android.graphics.PixelFormat;
import android.os.Build;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
public class FloatingTimerWidget {
    private View view;
    private WindowManager wm;
    public void show(Context ctx) {
        if (Build.VERSION.SDK_INT >= 26 && !android.provider.Settings.canDrawOverlays(ctx)) return;
        wm = (WindowManager) ctx.getSystemService(Context.WINDOW_SERVICE);
        int type = Build.VERSION.SDK_INT >= 26 ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY : WindowManager.LayoutParams.TYPE_PHONE;
        WindowManager.LayoutParams p = new WindowManager.LayoutParams(100, 100, type, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, PixelFormat.TRANSLUCENT);
        view = new TextView(ctx);
        view.setText("Timer");
        wm.addView(view, p);
    }
    public void hide() {
        if (view != null && wm != null) {
            wm.removeView(view);
            view = null;
        }
    }
}
"""

# 8. CallLogMonitor: Runtime permission check inside method
files["CallLogMonitor.java"] = """package com.kidsguard.child;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.provider.CallLog;
import android.util.Log;
import androidx.core.content.ContextCompat;
public class CallLogMonitor {
    public void fetch(Context ctx) {
        if (ContextCompat.checkSelfPermission(ctx, android.Manifest.permission.READ_CALL_LOG) != PackageManager.PERMISSION_GRANTED) {
            Log.w("CallLog", "Permission missing"); return;
        }
        Cursor c = null;
        try {
            c = ctx.getContentResolver().query(CallLog.Calls.CONTENT_URI, null, null, null, CallLog.Calls.DATE + " DESC LIMIT 50");
            if (c != null) while (c.moveToNext()) { /* process */ }
        } catch (SecurityException e) { Log.w("CallLog", "Denied"); }
        finally { if (c != null) c.close(); }
    }
}
"""

# 9. UpdateManager: Zip Bomb protection
files["UpdateManager.java"] = """package com.kidsguard.child;
import java.io.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
public class UpdateManager {
    private static final long MAX_ENTRY_SIZE = 100 * 1024 * 1024; // 100MB
    public void extract(File zip, File dest) throws IOException {
        String destPath = dest.getCanonicalPath();
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zip))) {
            ZipEntry e;
            while ((e = zis.getNextEntry()) != null) {
                if (e.getSize() > MAX_ENTRY_SIZE) throw new IOException("Zip Bomb detected");
                File out = new File(dest, e.getName());
                if (!out.getCanonicalPath().startsWith(destPath + File.separator)) throw new IOException("Path traversal blocked");
                if (e.isDirectory()) out.mkdirs();
                else {
                    out.getParentFile().mkdirs();
                    try (FileOutputStream fos = new FileOutputStream(out)) {
                        byte[] buf = new byte[4096]; int len;
                        while ((len = zis.read(buf)) > 0) fos.write(buf, 0, len);
                    }
                }
            }
        }
    }
}
"""

# 10. HeartbeatScheduler: Inexact fallback for Doze mode
files["HeartbeatScheduler.java"] = """package com.kidsguard.child;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
public class HeartbeatScheduler {
    public void schedule(Context ctx, Intent i) {
        AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= 23 ? PendingIntent.FLAG_IMMUTABLE : 0);
        PendingIntent pi = PendingIntent.getBroadcast(ctx, 0, i, flags);
        long t = System.currentTimeMillis() + 60000;
        if (Build.VERSION.SDK_INT >= 23) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, t, pi);
        } else {
            am.set(AlarmManager.RTC_WAKEUP, t, pi);
        }
    }
}
"""

for name, code in files.items():
    with open(f"{b}/{name}", "w") as f:
        f.write(code)
    print(f"Fixed {name}")

print("Chunk 8 done — 10 data & resource bugs.")
