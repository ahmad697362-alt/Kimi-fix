package com.kidsguard.child;

import android.content.ClipboardManager;
import android.content.Context;
import android.util.Log;

public class ClipboardTracker {
    private static final String TAG = "ClipboardTracker";
    private final Context context;
    private ClipboardManager cm;
    private ClipboardManager.OnPrimaryClipChangedListener listener;

    public ClipboardTracker(Context context) {
        this.context = context.getApplicationContext();
    }

    public void start() {
        stop();

        cm = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        if (cm == null) return;

        listener = () -> {
            Log.d(TAG, "Primary clip changed");
        };
        cm.addPrimaryClipChangedListener(listener);
    }

    public void stop() {
        if (cm != null && listener != null) {
            cm.removePrimaryClipChangedListener(listener);
            listener = null;
        }
    }
}
