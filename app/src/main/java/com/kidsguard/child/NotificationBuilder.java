package com.kidsguard.child;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

public class NotificationBuilder {

    public PendingIntent createActionIntent(Context context, String action) {
        Intent intent = new Intent(context, ActionReceiver.class);
        intent.putExtra("action", action);
        
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        
        int requestCode = action != null ? action.hashCode() : 0;
        return PendingIntent.getBroadcast(context, requestCode, intent, flags);
    }
}
