package com.kidsguard.child;
import android.accessibilityservice.AccessibilityService;
import android.view.accessibility.AccessibilityEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
public class BlockAccessibility extends AccessibilityService {
    public static final List<String> blockedPkgs = Collections.synchronizedList(new ArrayList<>());
    @Override
    public void onAccessibilityEvent(AccessibilityEvent e) {
        if (e.getPackageName() != null && blockedPkgs.contains(e.getPackageName().toString())) {
            performGlobalAction(GLOBAL_ACTION_BACK);
        }
    }
    @Override public void onInterrupt() {}
}
