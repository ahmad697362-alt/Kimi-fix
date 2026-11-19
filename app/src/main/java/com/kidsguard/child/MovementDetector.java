package com.kidsguard.child;
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
