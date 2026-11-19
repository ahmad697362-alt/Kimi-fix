package com.kidsguard.child;
import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import androidx.core.content.ContextCompat;
public class BtPermissionHelper {
    public static boolean check(Context c) {
        if (Build.VERSION.SDK_INT >= 31) return ContextCompat.checkSelfPermission(c, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED;
        return true;
    }
}