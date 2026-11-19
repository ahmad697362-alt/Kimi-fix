package com.kidsguard.child;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
public class NotificationBuilder {
    public PendingIntent getActionIntent(Context ctx, String action) {
        Intent i = new Intent(ctx, ActionReceiver.class);
        i.putExtra("action", action);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) flags |= PendingIntent.FLAG_IMMUTABLE;
        return PendingIntent.getBroadcast(ctx, action.hashCode(), i, flags);
    }
}
