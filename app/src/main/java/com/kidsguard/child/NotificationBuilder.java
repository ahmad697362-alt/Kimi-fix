package com.kidsguard.child;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
public class NotificationBuilder {
    public void show(Context ctx, String title, String text) {
        Intent i = new Intent(ctx, ActionReceiver.class);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE;
        PendingIntent pi = PendingIntent.getBroadcast(ctx, 0, i, flags);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(ctx, "default_channel")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title).setContentText(text)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pi).setAutoCancel(true);
        NotificationManagerCompat.from(ctx).notify(1001, builder.build());
    }
}
