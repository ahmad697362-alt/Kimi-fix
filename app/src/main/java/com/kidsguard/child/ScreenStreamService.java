package com.kidsguard.child;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.hardware.display.VirtualDisplay;
import android.media.projection.MediaProjection;
import android.os.Build;
import android.os.IBinder;
import androidx.core.app.NotificationCompat;
public class ScreenStreamService extends Service {
    private MediaProjection proj;
    private VirtualDisplay display;
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel ch = new NotificationChannel("screen", "Screen", NotificationManager.IMPORTANCE_LOW);
            ((NotificationManager) getSystemService(NOTIFICATION_SERVICE)).createNotificationChannel(ch);
        }
        Notification n = new NotificationCompat.Builder(this, "screen").build();
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(1002, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION);
        } else if (Build.VERSION.SDK_INT >= 29) {
            startForeground(1002, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION);
        } else { startForeground(1002, n); }
        return START_STICKY;
    }
    public void setProjection(MediaProjection p) {
        this.proj = p;
        p.registerCallback(new MediaProjection.Callback() {
            @Override public void onStop() { stopSelf(); }
        }, null);
    }
    @Override public void onDestroy() {
        if (display != null) display.release();
        if (proj != null) proj.stop();
        super.onDestroy();
    }
    @Override public IBinder onBind(Intent intent) { return null; }
}
