package com.kidsguard.child;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import androidx.core.app.NotificationCompat;
public class AppBlocker {
    public void block(Context ctx, String pkg) {
        Intent i = new Intent(ctx, AppBlockedActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE;
        PendingIntent pi = PendingIntent.getActivity(ctx, 0, i, flags);
        NotificationManager nm = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= 26) nm.createNotificationChannel(new NotificationChannel("block", "Block", NotificationManager.IMPORTANCE_HIGH));
        nm.notify(pkg.hashCode(), new NotificationCompat.Builder(ctx, "block").setFullScreenIntent(pi, true).build());
    }
}