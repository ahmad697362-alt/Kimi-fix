import os
f1 = """package com.kidsguard.child;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.telephony.SmsManager;
import android.util.Log;
public class SmsSender {
    public static void sendSms(Context context, String number, String message) {
        if (androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.SEND_SMS) != android.content.pm.PackageManager.PERMISSION_GRANTED) return;
        try {
            SmsManager sms = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ? context.getSystemService(SmsManager.class) : SmsManager.getDefault();
            int flags = PendingIntent.FLAG_UPDATE_CURRENT;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) flags |= PendingIntent.FLAG_MUTABLE;
            PendingIntent pi = PendingIntent.getBroadcast(context, 0, new Intent("SMS_SENT"), flags);
            if (message.length() > 160) sms.sendMultipartTextMessage(number, null, sms.divideMessage(message), null, null);
            else sms.sendTextMessage(number, null, message, pi, null);
        } catch (Exception e) { Log.e("SmsSender", "Send failed", e); }
    }
}
"""
f2 = """package com.kidsguard.child;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;
import java.util.ArrayList;
import java.util.List;
public class AppListFetcher {
    private final Context context;
    public AppListFetcher(Context context) { this.context = context.getApplicationContext(); }
    public List<PackageInfo> getInstalledApps() {
        PackageManager pm = context.getPackageManager();
        List<PackageInfo> all = new ArrayList<>();
        try {
            int flags = Build.VERSION.SDK_INT < Build.VERSION_CODES.R ? PackageManager.MATCH_ALL : 0;
            List<PackageInfo> base = pm.getInstalledPackages(flags);
            for (PackageInfo info : base) {
                try { all.add(pm.getPackageInfo(info.packageName, PackageManager.GET_ACTIVITIES | PackageManager.GET_SERVICES)); }
                catch (PackageManager.NameNotFoundException e) { all.add(info); }
            }
        } catch (Exception e) { Log.e("AppList", "Fetch failed", e); all = pm.getInstalledPackages(0); }
        return all;
    }
}
"""
f3 = """package com.kidsguard.child;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.location.LocationManager;
import android.os.Build;
import android.util.Log;
public class GeofenceManager {
    private final Context context;
    public GeofenceManager(Context context) { this.context = context.getApplicationContext(); }
    public void setGeofence(String id, double lat, double lng) {
        Intent intent = new Intent(context, GeofenceReceiver.class);
        intent.putExtra("geofence_id", id);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) flags |= PendingIntent.FLAG_IMMUTABLE;
        PendingIntent pi = PendingIntent.getBroadcast(context, id.hashCode(), intent, flags);
        LocationManager lm = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        if (lm != null) {
            try { lm.addProximityAlert(lat, lng, 100, -1, pi); Log.d("Geo", "Set: " + id); }
            catch (SecurityException e) { Log.e("Geo", "Permission missing", e); }
        }
    }
}
"""
base = "app/src/main/java/com/kidsguard/child"
with open(f"{base}/SmsSender.java", "w") as f: f.write(f1)
with open(f"{base}/AppListFetcher.java", "w") as f: f.write(f2)
with open(f"{base}/GeofenceManager.java", "w") as f: f.write(f3)
print("Chunk 3 done.")
