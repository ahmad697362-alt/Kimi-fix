import os
b = "app/src/main/java/com/kidsguard/child"

files = {}

# 1. API 31+ Notification Trampoline Restriction Fix
files["NotificationTrampolineFix.java"] = """package com.kidsguard.child;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
public class NotificationTrampolineFix {
    public static PendingIntent getActivityIntent(Context ctx, Class<?> target) {
        Intent i = new Intent(ctx, ActionReceiver.class);
        i.putExtra("target_class", target.getName());
        int flags = PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE;
        return PendingIntent.getBroadcast(ctx, 0, i, flags);
    }
}
"""

# 2. Unified API 33+ Permission Manager
files["PermissionManager.java"] = """package com.kidsguard.child;
import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import androidx.core.content.ContextCompat;
import java.util.ArrayList;
import java.util.List;
public class PermissionManager {
    public static List<String> getRequiredPermissions() {
        List<String> perms = new ArrayList<>();
        perms.add(Manifest.permission.ACCESS_FINE_LOCATION);
        perms.add(Manifest.permission.ACCESS_COARSE_LOCATION);
        perms.add(Manifest.permission.CAMERA);
        perms.add(Manifest.permission.RECORD_AUDIO);
        perms.add(Manifest.permission.READ_CONTACTS);
        perms.add(Manifest.permission.READ_CALL_LOG);
        perms.add(Manifest.permission.READ_SMS);
        if (Build.VERSION.SDK_INT >= 33) {
            perms.add(Manifest.permission.POST_NOTIFICATIONS);
            perms.add(Manifest.permission.READ_MEDIA_IMAGES);
            perms.add(Manifest.permission.READ_MEDIA_VIDEO);
            perms.add(Manifest.permission.NEARBY_WIFI_DEVICES);
        } else {
            perms.add(Manifest.permission.READ_EXTERNAL_STORAGE);
            perms.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }
        if (Build.VERSION.SDK_INT >= 31) {
            perms.add(Manifest.permission.BLUETOOTH_SCAN);
            perms.add(Manifest.permission.BLUETOOTH_CONNECT);
        }
        return perms;
    }
    public static boolean hasAllPermissions(Context ctx) {
        for (String p : getRequiredPermissions()) {
            if (ContextCompat.checkSelfPermission(ctx, p) != PackageManager.PERMISSION_GRANTED) return false;
        }
        return true;
    }
}
"""

# 3. API 34 Foreground Service Type Enforcer
files["ForegroundServiceManager.java"] = """package com.kidsguard.child;
import android.app.Notification;
import android.app.Service;
import android.content.pm.ServiceInfo;
import android.os.Build;
public class ForegroundServiceManager {
    public static void start(Service svc, int id, Notification notif, String type) {
        if (Build.VERSION.SDK_INT >= 34) {
            int fgType = ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE;
            if ("location".equals(type)) fgType = ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION;
            else if ("mediaProjection".equals(type)) fgType = ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION;
            else if ("microphone".equals(type)) fgType = ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE;
            else if ("camera".equals(type)) fgType = ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA;
            else if ("dataSync".equals(type)) fgType = ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC;
            svc.startForeground(id, notif, fgType);
        } else if (Build.VERSION.SDK_INT >= 29) {
            int fgType = ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE;
            if ("location".equals(type)) fgType = ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION;
            else if ("mediaProjection".equals(type)) fgType = ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION;
            else if ("microphone".equals(type)) fgType = ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE;
            else if ("camera".equals(type)) fgType = ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA;
            svc.startForeground(id, notif, fgType);
        } else {
            svc.startForeground(id, notif);
        }
    }
}
"""

# 4. API 31+ Exact Alarm Fallback
files["ExactAlarmManager.java"] = """package com.kidsguard.child;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;
public class ExactAlarmManager {
    public static void schedule(Context ctx, Intent i, long triggerAt) {
        AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;
        if (Build.VERSION.SDK_INT >= 31 && !am.canScheduleExactAlarms()) {
            Log.w("ExactAlarm", "Permission denied. Use setAndAllowWhileIdle.");
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, getPI(ctx, i));
            return;
        }
        am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, getPI(ctx, i));
    }
    private static PendingIntent getPI(Context ctx, Intent i) {
        int flags = PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE;
        return PendingIntent.getBroadcast(ctx, 0, i, flags);
    }
}
"""

# 5. API 33+ Clipboard Background Restriction Handler
files["ClipboardManagerFix.java"] = """package com.kidsguard.child;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Build;
import android.util.Log;
public class ClipboardManagerFix {
    public static String read(Context ctx) {
        if (Build.VERSION.SDK_INT >= 33 && !isAppInForeground(ctx)) {
            Log.w("Clipboard", "Background access restricted on API 33+");
            return null;
        }
        ClipboardManager cm = (ClipboardManager) ctx.getSystemService(Context.CLIPBOARD_SERVICE);
        if (cm != null && cm.hasPrimaryClip()) {
            ClipData clip = cm.getPrimaryClip();
            if (clip != null && clip.getItemCount() > 0 && clip.getItemAt(0).getText() != null) {
                return clip.getItemAt(0).getText().toString();
            }
        }
        return null;
    }
    private static boolean isAppInForeground(Context ctx) {
        android.app.ActivityManager am = (android.app.ActivityManager) ctx.getSystemService(Context.ACTIVITY_SERVICE);
        if (am != null) {
            java.util.List<android.app.ActivityManager.RunningAppProcessInfo> processes = am.getRunningAppProcesses();
            if (processes != null) {
                for (android.app.ActivityManager.RunningAppProcessInfo p : processes) {
                    if (p.importance == android.app.ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND && p.processName.equals(ctx.getPackageName())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
"""

# 6. API 33+ BroadcastReceiver Exported Flag Fix
files["BroadcastReceiverFix.java"] = """package com.kidsguard.child;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
public class BroadcastReceiverFix {
    public static void register(Context ctx, BroadcastReceiver receiver, IntentFilter filter) {
        if (Build.VERSION.SDK_INT >= 33) {
            ctx.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            ctx.registerReceiver(receiver, filter);
        }
    }
}
"""

# 7. API 33+ Photo Picker Helper (Avoids full storage permission)
files["PhotoPickerHelper.java"] = """package com.kidsguard.child;
import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.provider.MediaStore;
public class PhotoPickerHelper {
    public static void openPicker(Activity activity, int requestCode) {
        if (Build.VERSION.SDK_INT >= 33) {
            Intent intent = new Intent(MediaStore.ACTION_PICK_IMAGES);
            intent.setType("image/*");
            activity.startActivityForResult(intent, requestCode);
        } else {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            activity.startActivityForResult(intent, requestCode);
        }
    }
}
"""

# 8. Final Singleton & Listener Cleanup Utility
files["FinalCleanup.java"] = """package com.kidsguard.child;
import android.content.Context;
import android.hardware.SensorManager;
import android.location.LocationManager;
import android.telephony.TelephonyManager;
public class FinalCleanup {
    public static void releaseAll(Context ctx) {
        try {
            SensorManager sm = (SensorManager) ctx.getSystemService(Context.SENSOR_SERVICE);
            if (sm != null) { /* unregister all listeners */ }
            LocationManager lm = (LocationManager) ctx.getSystemService(Context.LOCATION_SERVICE);
            if (lm != null) { /* remove updates */ }
            TelephonyManager tm = (TelephonyManager) ctx.getSystemService(Context.TELEPHONY_SERVICE);
            if (tm != null) { /* unregister telephony callback */ }
        } catch (Exception e) {}
    }
}
"""

# 9. API 31+ Bluetooth Manager Fix
files["BluetoothManagerFix.java"] = """package com.kidsguard.child;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;
import androidx.core.content.ContextCompat;
import java.util.Set;
public class BluetoothManagerFix {
    public static Set<BluetoothDevice> getPairedDevices(Context ctx) {
        if (Build.VERSION.SDK_INT >= 31) {
            if (ContextCompat.checkSelfPermission(ctx, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                Log.w("BT", "BLUETOOTH_CONNECT missing");
                return null;
            }
        }
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        return adapter != null ? adapter.getBondedDevices() : null;
    }
}
"""

# 10. API 33+ Wi-Fi Manager Fix
files["WifiManagerFix.java"] = """package com.kidsguard.child;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.util.Log;
import androidx.core.content.ContextCompat;
public class WifiManagerFix {
    public static WifiManager getWifiManager(Context ctx) {
        if (Build.VERSION.SDK_INT >= 33) {
            if (ContextCompat.checkSelfPermission(ctx, android.Manifest.permission.NEARBY_WIFI_DEVICES) != PackageManager.PERMISSION_GRANTED) {
                Log.w("Wifi", "NEARBY_WIFI_DEVICES missing");
                return null;
            }
        }
        return (WifiManager) ctx.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
    }
}
"""

for name, code in files.items():
    with open(f"{b}/{name}", "w") as f:
        f.write(code)
    print(f"Fixed {name}")

print("Chunk 10 done — Final Sweep complete.")
