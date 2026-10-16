package com.kidsguard.child;

import android.hardware.display.VirtualDisplay;
import android.media.projection.MediaProjection;
import android.util.Log;

public class ScreenRecorder {
    private static final String TAG = "ScreenRecorder";
    private MediaProjection projection;
    private VirtualDisplay virtualDisplay;

    public void startRecording(MediaProjection proj) {
        this.projection = proj;
        if (this.projection == null) return;

        projection.registerCallback(new MediaProjection.Callback() {
            @Override
            public void onStop() {
                Log.d(TAG, "MediaProjection stopped by user");
                releaseResources();
            }
        }, null);

        virtualDisplay = projection.createVirtualDisplay(
                "ScreenRecorder", 1080, 1920, 300, 
                android.hardware.display.DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR, 
                null, null, null);
    }

    public void releaseResources() {
        if (virtualDisplay != null) {
            virtualDisplay.release();
            virtualDisplay = null;
        }
        if (projection != null) {
            projection.stop();
            projection = null;
        }
    }
}
