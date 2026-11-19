package com.kidsguard.child;
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
