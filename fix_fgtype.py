import os
p = "app/src/main/java/com/kidsguard/child/LocationService.java"
c = open(p).read()

# 1. Add ServiceInfo import
if "import android.content.pm.ServiceInfo;" not in c:
    c = c.replace("import android.content.SharedPreferences;",
                  "import android.content.SharedPreferences;\nimport android.content.pm.ServiceInfo;")

# 2. Fix startForeground with type (Android 14 crash fix)
c = c.replace(
    "        startForeground(NOTIF_ID, n);\n    }",
    """        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(NOTIF_ID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION | ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC);
        } else if (Build.VERSION.SDK_INT >= 29) {
            startForeground(NOTIF_ID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION);
        } else {
            startForeground(NOTIF_ID, n);
        }
    }"""
)

# 3. Fix registerReceiver flag (API 34)
c = c.replace(
    "Intent batteryStatus = registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));",
    """Intent batteryStatus;
            if (Build.VERSION.SDK_INT >= 33) {
                batteryStatus = registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED), RECEIVER_NOT_EXPORTED);
            } else {
                batteryStatus = registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
            }"""
)

open(p, "w").write(c)
print("startForeground type + registerReceiver flag fixed.")
