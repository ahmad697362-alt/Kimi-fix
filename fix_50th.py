import os
b = "app/src/main/java/com/kidsguard/child"

code = """package com.kidsguard.child;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkRequest;
import android.util.Log;
public class DataSaverBypass {
    public static boolean isDataSaverOn(Context ctx) {
        ConnectivityManager cm = (ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm != null) {
            return cm.getRestrictBackgroundStatus() == ConnectivityManager.RESTRICT_BACKGROUND_STATUS_ENABLED;
        }
        return false;
    }
    public static void requestUnrestrictedAccess(Context ctx) {
        // Requires android.permission.ACCESS_NETWORK_STATE
        Log.d("DataSaver", "App is running in Data Saver mode. Critical syncs may be delayed.");
    }
}
"""

with open(f"{b}/DataSaverBypass.java", "w") as f:
    f.write(code)
print("50th file written: DataSaverBypass.java")
