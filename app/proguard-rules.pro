# Keep Android Components
-keep class * extends android.app.Application { *; }
-keep class * extends android.app.Activity { *; }
-keep class * extends android.app.Service { *; }
-keep class * extends android.content.BroadcastReceiver { *; }
-keep class * extends android.content.ContentProvider { *; }
-keep class * extends android.app.admin.DeviceAdminReceiver { *; }
-keep class * extends android.accessibilityservice.AccessibilityService { *; }
-keep class * extends android.service.notification.NotificationListenerService { *; }

# Keep App Packages
-keep class com.kidsguard.child.** { *; }
-keep class com.kidsguard.parent.** { *; }

# Keep JSON
-keep class org.json.** { *; }

# Keep Native Methods
-keepclasseswithmembernames class * {
    native <methods>;
}

# Remove Log in Release
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
}
