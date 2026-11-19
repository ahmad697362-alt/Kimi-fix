package com.kidsguard.child;
import android.content.Context;
import android.content.pm.PackageManager;
public class PackageVisibilityFix {
    public static boolean isInstalled(Context c, String pkg) {
        try { c.getPackageManager().getPackageInfo(pkg, 0); return true; } catch (PackageManager.NameNotFoundException e) { return false; }
    }
}