package com.kidsguard.child;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.util.Log;
import androidx.core.content.ContextCompat;
public class WifiManagerFix {
    public static WifiManager getWifiManager(Context ctx) {
        if (Build.VERSION.SDK_INT >= 33) {
            if (ContextCompat.checkSelfPermission(ctx, android.Manifest.permission.NEARBY_WIFI_DEVICES) != PackageManager.PERMISSION_GRANTED) {
                Log.w("Wifi", "NEARBY_WIFI_DEVICES missing");
                return null;
            }
        }
        return (WifiManager) ctx.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
    }
}
