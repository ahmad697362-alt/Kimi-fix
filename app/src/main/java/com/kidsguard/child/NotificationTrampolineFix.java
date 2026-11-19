package com.kidsguard.child;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
public class NotificationTrampolineFix {
    public static PendingIntent getActivityIntent(Context ctx, Class<?> target) {
        Intent i = new Intent(ctx, ActionReceiver.class);
        i.putExtra("target_class", target.getName());
        int flags = PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE;
        return PendingIntent.getBroadcast(ctx, 0, i, flags);
    }
}
