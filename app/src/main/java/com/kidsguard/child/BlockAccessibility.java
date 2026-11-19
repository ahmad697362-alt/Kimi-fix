package com.kidsguard.child;
import android.accessibilityservice.AccessibilityService;
import android.view.accessibility.AccessibilityEvent;
import java.util.List;
import java.util.ArrayList;
public class BlockAccessibility extends AccessibilityService {
    public static List<String> blockedPkgs = new ArrayList<>();
    @Override
    public void onAccessibilityEvent(AccessibilityEvent e) {
        if (blockedPkgs.contains(e.getPackageName())) performGlobalAction(GLOBAL_ACTION_BACK);
    }
    @Override public void onInterrupt() {}
}