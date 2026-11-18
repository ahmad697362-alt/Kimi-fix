package com.kidsguard.child;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.admin.DeviceAdminReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;
import androidx.core.app.NotificationCompat;
import org.json.JSONObject;

public class AdminReceiver extends DeviceAdminReceiver {
    private static final String TAG = "AdminReceiver";
    private static final String CHANNEL_ID = "admin_alerts";

    @Override
    public void onEnabled(Context context, Intent intent) {
        super.onEnabled(context, intent);
        Log.d(TAG, "Device Admin enabled");
        sendStatusUpdate(context, "ENABLED");
    }

    @Override
    public void onDisabled(Context context, Intent intent) {
        super.onDisabled(context, intent);
        Log.w(TAG, "Device Admin DISABLED - Critical Alert");
        sendStatusUpdate(context, "DISABLED");
        showCriticalNotification(context);
    }

    @Override
    public void onPasswordFailed(Context context, Intent intent) {
        super.onPasswordFailed(context, intent);
        sendStatusUpdate(context, "PASSWORD_FAILED");
    }

    @Override
    public void onPasswordSucceeded(Context context, Intent intent) {
        super.onPasswordSucceeded(context, intent);
        sendStatusUpdate(context, "PASSWORD_SUCCESS");
    }

    private void sendStatusUpdate(Context context, String status) {
        try {
            String deviceId = LocationService.getSafeDeviceId(context);
            JSONObject payload = new JSONObject();
            payload.put("status", status);
            payload.put("time", System.currentTimeMillis());
            FirebaseHelper.put("devices/" + deviceId + "/admin_status.json", payload.toString());
        } catch (Exception e) {
            Log.e(TAG, "Failed to send admin status", e);
        }
    }

    private void showCriticalNotification(Context context) {
        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Admin Alerts", NotificationManager.IMPORTANCE_HIGH);
            nm.createNotificationChannel(channel);
        }

        Intent launchIntent = context.getPackageManager().getLaunchIntentForPackage(context.getPackageName());
        if (launchIntent == null) launchIntent = new Intent(context, MainActivity.class);
        
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) flags |= PendingIntent.FLAG_IMMUTABLE;
        
        PendingIntent pi = PendingIntent.getActivity(context, 0, launchIntent, flags);

        Notification notification = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentTitle("Security Alert")
                .setContentText("Device Admin was removed. Tap to restore.")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pi)
                .setAutoCancel(true)
                .build();

        nm.notify(9999, notification);
    }
}
