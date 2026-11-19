package com.kidsguard.child;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Build;
public class ClipboardBgFix {
    public static boolean canRead(Context c) {
        if (Build.VERSION.SDK_INT >= 33) {
            android.app.ActivityManager am = (android.app.ActivityManager) c.getSystemService(Context.ACTIVITY_SERVICE);
            if (am != null) for (android.app.ActivityManager.RunningAppProcessInfo p : am.getRunningAppProcesses()) if (p.importance == android.app.ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND && p.processName.equals(c.getPackageName())) return true;
            return false;
        }
        return true;
    }
}