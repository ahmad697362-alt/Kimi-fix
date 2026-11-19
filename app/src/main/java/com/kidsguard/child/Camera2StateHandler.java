package com.kidsguard.child;
import android.hardware.camera2.CameraDevice;
import android.util.Log;
public class Camera2StateHandler extends CameraDevice.StateCallback {
    @Override public void onOpened(CameraDevice c) { Log.d("Cam", "Opened"); }
    @Override public void onDisconnected(CameraDevice c) { c.close(); }
    @Override public void onError(CameraDevice c, int e) { c.close(); }
}