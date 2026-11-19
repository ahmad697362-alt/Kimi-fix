package com.kidsguard.child;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
public class MovementDetector implements SensorEventListener {
    private SensorManager sm;
    private Sensor sensor;
    public void start(Context ctx) {
        sm = (SensorManager) ctx.getSystemService(Context.SENSOR_SERVICE);
        sensor = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        if (sensor != null) sm.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL);
    }
    public void stop() { if (sm != null) sm.unregisterListener(this); }
    @Override public void onSensorChanged(SensorEvent e) {}
    @Override public void onAccuracyChanged(Sensor s, int a) {}
}
