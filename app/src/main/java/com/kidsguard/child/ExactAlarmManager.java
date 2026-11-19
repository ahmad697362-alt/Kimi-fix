package com.kidsguard.child;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;
public class ExactAlarmManager {
    public static void schedule(Context ctx, Intent i, long triggerAt) {
        AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;
        if (Build.VERSION.SDK_INT >= 31 && !am.canScheduleExactAlarms()) {
            Log.w("ExactAlarm", "Permission denied. Use setAndAllowWhileIdle.");
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, getPI(ctx, i));
            return;
        }
        am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, getPI(ctx, i));
    }
    private static PendingIntent getPI(Context ctx, Intent i) {
        int flags = PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE;
        return PendingIntent.getBroadcast(ctx, 0, i, flags);
    }
}
