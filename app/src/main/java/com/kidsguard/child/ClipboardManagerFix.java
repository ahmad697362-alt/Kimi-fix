package com.kidsguard.child;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Build;
import android.util.Log;
public class ClipboardManagerFix {
    public static String read(Context ctx) {
        if (Build.VERSION.SDK_INT >= 33 && !isAppInForeground(ctx)) {
            Log.w("Clipboard", "Background access restricted on API 33+");
            return null;
        }
        ClipboardManager cm = (ClipboardManager) ctx.getSystemService(Context.CLIPBOARD_SERVICE);
        if (cm != null && cm.hasPrimaryClip()) {
            ClipData clip = cm.getPrimaryClip();
            if (clip != null && clip.getItemCount() > 0 && clip.getItemAt(0).getText() != null) {
                return clip.getItemAt(0).getText().toString();
            }
        }
        return null;
    }
    private static boolean isAppInForeground(Context ctx) {
        android.app.ActivityManager am = (android.app.ActivityManager) ctx.getSystemService(Context.ACTIVITY_SERVICE);
        if (am != null) {
            java.util.List<android.app.ActivityManager.RunningAppProcessInfo> processes = am.getRunningAppProcesses();
            if (processes != null) {
                for (android.app.ActivityManager.RunningAppProcessInfo p : processes) {
                    if (p.importance == android.app.ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND && p.processName.equals(ctx.getPackageName())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
