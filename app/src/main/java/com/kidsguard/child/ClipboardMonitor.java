package com.kidsguard.child;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.util.Log;
public class ClipboardMonitor {
    public void check(Context ctx) {
        ClipboardManager cm = (ClipboardManager) ctx.getSystemService(Context.CLIPBOARD_SERVICE);
        try {
            if (cm != null && cm.hasPrimaryClip()) {
                ClipData clip = cm.getPrimaryClip();
                if (clip != null && clip.getItemCount() > 0) {
                    ClipData.Item item = clip.getItemAt(0);
                    if (item != null) {
                        CharSequence text = item.getText();
                        if (text != null) Log.d("Clip", text.toString());
                    }
                }
            }
        } catch (SecurityException e) { Log.w("Clip", "Blocked"); }
    }
}
