package com.kidsguard.child;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.IBinder;
import androidx.core.app.NotificationCompat;
public class RecordingService extends Service {
    private MediaRecorder recorder;
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel ch = new NotificationChannel("rec", "Recording", NotificationManager.IMPORTANCE_LOW);
            ((NotificationManager) getSystemService(NOTIFICATION_SERVICE)).createNotificationChannel(ch);
        }
        Notification n = new NotificationCompat.Builder(this, "rec").build();
        if (Build.VERSION.SDK_INT >= 29) {
            startForeground(2001, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE);
        } else {
            startForeground(2001, n);
        }
        return START_STICKY;
    }
    @Override
    public void onDestroy() {
        if (recorder != null) {
            try { recorder.stop(); } catch (Exception e) {}
            recorder.release();
        }
        super.onDestroy();
    }
    @Override public IBinder onBind(Intent intent) { return null; }
}
