package com.kidsguard.child;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

public class MovementDetector implements SensorEventListener {
    private static final String TAG = "MovementDetector";
    private final Context context;
    private SensorManager sm;
    private Sensor accelerometer;

    public MovementDetector(Context context) {
        this.context = context.getApplicationContext();
    }

    public void startTracking() {
        sm = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        if (sm == null) return;
        
        accelerometer = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        if (accelerometer == null) return;

        sm.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
    }

    public void stopTracking() {
        if (sm != null) {
            sm.unregisterListener(this);
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event == null || event.values == null || event.values.length < 3) return;
        float x = event.values[0];
        float y = event.values[1];
        float z = event.values[2];
        double acceleration = Math.sqrt(x * x + y * y + z * z);
        if (Math.abs(acceleration - 9.8) > 2.0) {
            Log.d(TAG, "Movement detected: " + acceleration);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}
}
