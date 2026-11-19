# Keep Accessibility Services
-keep class * extends android.accessibilityservice.AccessibilityService { *; }

# Keep Broadcast Receivers
-keep class * extends android.content.BroadcastReceiver { *; }

# Keep Services
-keep class * extends android.app.Service { *; }

# Keep JSON models and Child/Parent packages
-keep class com.kidsguard.child.** { *; }
-keep class com.kidsguard.parent.** { *; }

# Keep Native Methods
-keepclasseswithmembernames class * {
    native <methods>;
}
