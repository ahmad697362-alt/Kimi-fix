package com.kidsguard.child;
import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import androidx.core.content.ContextCompat;
public class NotifPermissionHelper {
    public static boolean check(Context c) {
        if (Build.VERSION.SDK_INT >= 33) return ContextCompat.checkSelfPermission(c, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
        return true;
    }
}