package com.kidsguard.child;
import android.content.Context;
import android.hardware.SensorManager;
import android.location.LocationManager;
import android.telephony.TelephonyManager;
public class FinalCleanup {
    public static void releaseAll(Context ctx) {
        try {
            SensorManager sm = (SensorManager) ctx.getSystemService(Context.SENSOR_SERVICE);
            if (sm != null) { /* unregister all listeners */ }
            LocationManager lm = (LocationManager) ctx.getSystemService(Context.LOCATION_SERVICE);
            if (lm != null) { /* remove updates */ }
            TelephonyManager tm = (TelephonyManager) ctx.getSystemService(Context.TELEPHONY_SERVICE);
            if (tm != null) { /* unregister telephony callback */ }
        } catch (Exception e) {}
    }
}
