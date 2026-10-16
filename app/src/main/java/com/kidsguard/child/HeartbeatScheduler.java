package com.kidsguard.child;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.os.Build;
import android.util.Log;

public class HeartbeatScheduler {
    private static final String TAG = "HeartbeatScheduler";
    private final Context context;

    public HeartbeatScheduler(Context context) {
        this.context = context.getApplicationContext();
    }

    public void scheduleHeartbeat(PendingIntent pi) {
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (am == null || pi == null) return;

        long triggerAt = System.currentTimeMillis() + 60000;

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi);
            } else {
                am.setExact(AlarmManager.RTC_WAKEUP, triggerAt, pi);
            }
        } catch (SecurityException e) {
            Log.w(TAG, "Exact alarm permission missing, fallback to setAndAllowWhileIdle", e);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi);
            } else {
                am.set(AlarmManager.RTC_WAKEUP, triggerAt, pi);
            }
        }
    }
}
