package com.kidsguard.child;
import android.view.accessibility.AccessibilityNodeInfo;
public class UiExtractor {
    public String getText(AccessibilityNodeInfo n) {
        if (n == null || !n.refresh()) return "";
        CharSequence t = n.getText();
        return t != null ? t.toString() : "";
    }
}