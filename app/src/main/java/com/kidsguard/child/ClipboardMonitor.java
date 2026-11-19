package com.kidsguard.child;
import android.content.ClipboardManager;
import android.content.Context;
import android.util.Log;
public class ClipboardMonitor {
    public void check(Context ctx) {
        ClipboardManager cm = (ClipboardManager) ctx.getSystemService(Context.CLIPBOARD_SERVICE);
        try {
            if (cm != null && cm.hasPrimaryClip()) {
                CharSequence text = cm.getPrimaryClip().getItemAt(0).getText();
                if (text != null) Log.d("Clip", text.toString());
            }
        } catch (SecurityException e) { Log.w("Clip", "Blocked by OS"); }
    }
}