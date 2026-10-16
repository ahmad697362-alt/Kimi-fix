package com.kidsguard.child;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.admin.DeviceAdminReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import androidx.core.app.NotificationCompat;
import org.json.JSONObject;

public class AdminReceiver extends DeviceAdminReceiver {
    private static final String TAG = "AdminReceiver";

    @Override
    public void onEnabled(Context context, Intent intent) {
        super.onEnabled(context, intent);
        String devId = LocationService.getSafeDeviceId(context);
        JSONObject status = new JSONObject();
        try {
            status.put("status", "ENABLED");
            status.put("time", System.currentTimeMillis());
        } catch (Exception e) {}
        FirebaseHelper.put("devices/" + devId + "/admin_status.json", status.toString());
    }

    @Override
    public void onDisabled(Context context, Intent intent) {
        super.onDisabled(context, intent);
        String devId = LocationService.getSafeDeviceId(context);

        try {
            JSONObject status = new JSONObject();
            status.put("status", "DISABLED");
            status.put("time", System.currentTimeMillis());
            FirebaseHelper.put("devices/" + devId + "/admin_status.json", status.toString());

            JSONObject alert = new JSONObject();
            alert.put("title", "ADMIN DISABLED");
            alert.put("body", "Child disabled device admin!");
            alert.put("time", System.currentTimeMillis());
            FirebaseHelper.put("devices/" + devId + "/alerts.json", alert.toString());
        } catch (Exception e) {}

        Intent auth = new Intent(context, UninstallAuthActivity.class);
        auth.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= 23 ? PendingIntent.FLAG_IMMUTABLE : 0);
        PendingIntent pi = PendingIntent.getActivity(context, 0, auth, flags);

        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= 26) {
            nm.createNotificationChannel(new NotificationChannel(
                    "admin_alert", "Alerts", NotificationManager.IMPORTANCE_HIGH));
        }
        Notification n = new NotificationCompat.Builder(context, "admin_alert")
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentTitle("Device admin removed")
                .setContentText("Tap to enter unlock password")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pi)
                .setAutoCancel(true)
                .build();
        nm.notify(2001, n);
    }

    @Override
    public CharSequence onDisableRequested(Context context, Intent intent) {
        return "Disabling device admin will remove parental protection. Are you sure?";
    }
}
