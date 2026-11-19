package com.kidsguard.child;
import android.content.Context;
import android.provider.Settings;
public class DeviceIdentifier {
    public static String getId(Context ctx) {
        String id = Settings.Secure.getString(ctx.getContentResolver(), Settings.Secure.ANDROID_ID);
        return (id == null || id.equals("9774d56d682e549c")) ? java.util.UUID.randomUUID().toString() : id;
    }
}