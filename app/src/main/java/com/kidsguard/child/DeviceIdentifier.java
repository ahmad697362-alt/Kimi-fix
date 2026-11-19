package com.kidsguard.child;
import android.content.Context;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.util.Log;
public class DeviceIdentifier {
    public static String getId(Context ctx) {
        String id = Settings.Secure.getString(ctx.getContentResolver(), Settings.Secure.ANDROID_ID);
        if (id == null || id.equals("9774d56d682e549c")) return java.util.UUID.randomUUID().toString();
        return id;
    }
    public static String getTelephonyId(Context ctx) {
        try {
            TelephonyManager tm = (TelephonyManager) ctx.getSystemService(Context.TELEPHONY_SERVICE);
            if (tm != null && android.Manifest.permission.READ_PHONE_STATE.equals("granted")) {
                return tm.getDeviceId();
            }
        } catch (SecurityException e) { Log.w("Device", "Telephony ID blocked"); }
        return getId(ctx);
    }
}
