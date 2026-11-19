package com.kidsguard.child;
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
