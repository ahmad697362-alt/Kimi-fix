import os

java_code = """package com.kidsguard.child;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.provider.Settings;
import android.util.Log;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.RandomAccessFile;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class LocationService extends Service {
    public static final String PREFS = "kidsguard_prefs";
    public static final String KEY_ID = "device_id";
    private static final String TAG = "LocationService";
    private static final long COMMAND_INTERVAL_MS = 60_000L;
    private static final long LOCATION_INTERVAL_MS = 30_000L;
    private static final int NOTIF_ID = 1001;

    private static volatile boolean alive;
    private final ExecutorService io = Executors.newFixedThreadPool(3);
    private final Handler handler = new Handler(Looper.getMainLooper());
    private LocationManager lm;
    private LocationListener locationListener;
    private MediaPlayer alarmPlayer;
    private PowerManager.WakeLock wakeLock;
    private String deviceId;
    private volatile boolean running;

    public static boolean isRunning() { return alive; }

    public static String getSafeDeviceId(Context ctx) {
        SharedPreferences p = ctx.getSharedPreferences(PREFS, 0);
        String cached = p.getString(KEY_ID, null);
        if (cached != null && cached.length() == 6) return cached;
        String raw = Settings.Secure.getString(ctx.getContentResolver(), Settings.Secure.ANDROID_ID);
        if (raw == null || raw.isEmpty()) raw = java.util.UUID.randomUUID().toString();
        String code = String.format("%06d", Math.abs(raw.hashCode() % 1000000));
        p.edit().putString(KEY_ID, code).apply();
        return code;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        running = true; alive = true;
        deviceId = getSafeDeviceId(this);
        startForegroundNotification();
        startLocationTracking();
        sendDeviceInfo();
        handler.postDelayed(commandPoller, COMMAND_INTERVAL_MS);
        handler.postDelayed(locationUploader, LOCATION_INTERVAL_MS);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) { return START_STICKY; }

    @Override
    public void onDestroy() {
        running = false; alive = false;
        handler.removeCallbacksAndMessages(null);
        if (lm != null && locationListener != null) {
            try { lm.removeUpdates(locationListener); } catch (Exception e) { Log.w(TAG, "loc fail", e); }
        }
        releaseAlarm(); releaseWakeLock();
        io.shutdownNow();
        try { io.awaitTermination(2, TimeUnit.SECONDS); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }

    private void startForegroundNotification() {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel ch = new NotificationChannel("kidsguard_service", "Service", NotificationManager.IMPORTANCE_LOW);
            getSystemService(NotificationManager.class).createNotificationChannel(ch);
        }
        Intent open = new Intent(this, MainActivity.class);
        int flags = Build.VERSION.SDK_INT >= 23 ? PendingIntent.FLAG_IMMUTABLE : 0;
        PendingIntent pi = PendingIntent.getActivity(this, 0, open, flags);
        Notification n = new Notification.Builder(this, "kidsguard_service")
                .setSmallIcon(android.R.drawable.ic_menu_mylocation)
                .setContentTitle("Kidsguard").setContentText("Running")
                .setOngoing(true).setContentIntent(pi).build();
        startForeground(NOTIF_ID, n);
    }

    private void startLocationTracking() {
        try {
            lm = (LocationManager) getSystemService(LOCATION_SERVICE);
            if (lm == null) return;
            locationListener = new LocationListener() {
                @Override public void onLocationChanged(Location l) { uploadLocationAsync(l); }
                @Override public void onStatusChanged(String p, int s, Bundle b) {}
                @Override public void onProviderEnabled(String p) {}
                @Override public void onProviderDisabled(String p) {}
            };
            if (Build.VERSION.SDK_INT >= 23 && checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) != android.content.pm.PackageManager.PERMISSION_GRANTED) return;
            for (String provider : new String[]{ LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER }) {
                if (lm.getAllProviders().contains(provider) && lm.isProviderEnabled(provider)) {
                    lm.requestLocationUpdates(provider, 10_000L, 5f, locationListener);
                }
            }
        } catch (Exception e) { Log.e(TAG, "loc track fail", e); }
    }

    private void uploadLocationAsync(final Location loc) {
        io.execute(() -> {
            try {
                JSONObject payload = new JSONObject();
                payload.put("lat", loc.getLatitude()); payload.put("lng", loc.getLongitude());
                payload.put("accuracy", loc.getAccuracy());
                payload.put("address", resolveAddress(loc.getLatitude(), loc.getLongitude()));
                payload.put("time", new SimpleDateFormat("HH:mm:ss dd/MM/yyyy", Locale.getDefault()).format(new Date(loc.getTime())));
                FirebaseHelper.put("devices/" + deviceId + "/location.json", payload.toString());
            } catch (Exception e) { Log.e(TAG, "loc upload fail", e); }
        });
    }

    private String resolveAddress(double lat, double lng) {
        try {
            android.location.Geocoder geocoder = new android.location.Geocoder(this, Locale.getDefault());
            List<android.location.Address> hits = geocoder.getFromLocation(lat, lng, 1);
            if (hits == null || hits.isEmpty()) return "";
            android.location.Address a = hits.get(0);
            StringBuilder sb = new StringBuilder();
            int max = a.getMaxAddressLineIndex();
            for (int i = 0; i <= max; i++) { sb.append(a.getAddressLine(i)); if (i < max) sb.append(", "); }
            return sb.toString();
        } catch (Exception e) { return ""; }
    }

    private void uploadLastKnown() {
        if (lm == null) return;
        try {
            if (Build.VERSION.SDK_INT >= 23 && checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) != android.content.pm.PackageManager.PERMISSION_GRANTED) return;
            Location best = null;
            for (String p : new String[]{ LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER }) {
                if (!lm.getAllProviders().contains(p)) continue;
                Location l = lm.getLastKnownLocation(p);
                if (l != null && (best == null || l.getTime() > best.getTime())) best = l;
            }
            if (best != null) uploadLocationAsync(best);
        } catch (Exception e) { Log.w(TAG, "last known fail", e); }
    }

    private final Runnable commandPoller = new Runnable() {
        @Override public void run() { if (!running) return; checkCommands(); handler.postDelayed(this, COMMAND_INTERVAL_MS); }
    };

    private void checkCommands() {
        io.execute(() -> {
            try {
                String path = "devices/" + deviceId + "/commands.json";
                String json = FirebaseHelper.get(path);
                if (json == null || "null".equals(json) || json.isEmpty()) return;
                JSONObject cmd = new JSONObject(json);
                String name = cmd.optString("cmd"); String arg = cmd.optString("arg");
                FirebaseHelper.delete(path); handleCommand(name, arg);
            } catch (Exception e) { Log.e(TAG, "cmd poll fail", e); }
        });
    }

    private void handleCommand(String name, String arg) {
        try {
            switch (name) {
                case "LOCATE": uploadLastKnown(); break;
                case "ALARM": startAlarm(R.raw.alarm); break;
                case "STOP": stopAlarm(); break;
                case "GET_CONTACTS": sendContacts(); break;
                case "GET_SMS": sendSms(); break;
                case "GET_CALL_LOGS": sendCallLogs(); break;
                case "GET_APPS": sendInstalledApps(); break;
                case "GET_SCREEN_TIME": sendScreenTime(); break;
                default: Log.w(TAG, "unknown cmd: " + name);
            }
        } catch (Exception e) { Log.e(TAG, "cmd exec fail: " + name, e); }
    }

    private void startAlarm(int resId) {
        stopAlarm();
        try {
            alarmPlayer = MediaPlayer.create(this, resId);
            if (alarmPlayer == null) return;
            alarmPlayer.setLooping(true); alarmPlayer.setVolume(1.0f, 1.0f); alarmPlayer.start();
        } catch (Exception e) { Log.e(TAG, "alarm fail", e); releaseAlarm(); }
    }

    private void stopAlarm() {
        try { if (alarmPlayer != null && alarmPlayer.isPlaying()) alarmPlayer.stop(); } catch (Exception e) {}
        finally { releaseAlarm(); }
    }

    private void releaseAlarm() { if (alarmPlayer != null) { alarmPlayer.release(); alarmPlayer = null; } }

    private void acquireWakeLock() {
        if (wakeLock != null && wakeLock.isHeld()) return;
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        if (pm == null) return;
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Kidsguard::WakeLock");
        wakeLock.acquire(10 * 60 * 1000L);
    }

    private void releaseWakeLock() { if (wakeLock != null && wakeLock.isHeld()) wakeLock.release(); wakeLock = null; }

    private void sendDeviceInfo() {
        io.execute(() -> {
            try {
                JSONObject info = new JSONObject();
                info.put("device_id", deviceId); info.put("model", Build.MODEL); info.put("brand", Build.BRAND); info.put("sdk", Build.VERSION.SDK_INT);
                info.put("time", new SimpleDateFormat("HH:mm dd/MM", Locale.getDefault()).format(new Date()));
                FirebaseHelper.put("devices/" + deviceId + "/info.json", info.toString());
            } catch (Exception e) { Log.e(TAG, "info fail", e); }
        });
    }

    private final Runnable locationUploader = new Runnable() {
        @Override public void run() { if (!running) return; uploadLastKnown(); handler.postDelayed(this, LOCATION_INTERVAL_MS); }
    };

    private void sendContacts() {
        io.execute(() -> {
            try {
                JSONArray arr = new JSONArray();
                android.database.Cursor cursor = getContentResolver().query(android.provider.ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, null, null, null);
                if (cursor != null) {
                    while (cursor.moveToNext()) {
                        int n = cursor.getColumnIndex(android.provider.ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME);
                        int p = cursor.getColumnIndex(android.provider.ContactsContract.CommonDataKinds.Phone.NUMBER);
                        JSONObject obj = new JSONObject();
                        obj.put("name", n >= 0 ? cursor.getString(n) : ""); obj.put("phone", p >= 0 ? cursor.getString(p) : "");
                        arr.put(obj);
                    }
                    cursor.close();
                }
                FirebaseHelper.put("devices/" + deviceId + "/contacts.json", arr.toString());
            } catch (Exception e) { Log.e(TAG, "contacts fail", e); }
        });
    }

    private void sendSms() {
        io.execute(() -> {
            try {
                JSONArray arr = new JSONArray();
                android.database.Cursor cursor = getContentResolver().query(android.net.Uri.parse("content://sms/inbox"), null, null, null, "date DESC LIMIT 50");
                if (cursor != null) {
                    while (cursor.moveToNext()) {
                        int b = cursor.getColumnIndex("body"); int a = cursor.getColumnIndex("address"); int d = cursor.getColumnIndex("date");
                        JSONObject obj = new JSONObject();
                        obj.put("from", a >= 0 ? cursor.getString(a) : ""); obj.put("body", b >= 0 ? cursor.getString(b) : ""); obj.put("date", d >= 0 ? cursor.getString(d) : "");
                        arr.put(obj);
                    }
                    cursor.close();
                }
                FirebaseHelper.put("devices/" + deviceId + "/sms.json", arr.toString());
            } catch (Exception e) { Log.e(TAG, "sms fail", e); }
        });
    }

    private void sendCallLogs() {
        io.execute(() -> {
            android.database.Cursor cursor = null;
            try {
                JSONArray arr = new JSONArray();
                cursor = getContentResolver().query(android.provider.CallLog.Calls.CONTENT_URI, null, null, null, android.provider.CallLog.Calls.DATE + " DESC LIMIT 50");
                if (cursor != null) {
                    while (cursor.moveToNext()) {
                        int n = cursor.getColumnIndex(android.provider.CallLog.Calls.NUMBER); int t = cursor.getColumnIndex(android.provider.CallLog.Calls.TYPE);
                        int d = cursor.getColumnIndex(android.provider.CallLog.Calls.DATE); int du = cursor.getColumnIndex(android.provider.CallLog.Calls.DURATION);
                        JSONObject obj = new JSONObject();
                        obj.put("number", n >= 0 ? cursor.getString(n) : ""); obj.put("type", t >= 0 ? cursor.getString(t) : "");
                        obj.put("date", d >= 0 ? cursor.getString(d) : ""); obj.put("duration", du >= 0 ? cursor.getString(du) : "");
                        arr.put(obj);
                    }
                }
                FirebaseHelper.put("devices/" + deviceId + "/call_logs.json", arr.toString());
            } catch (SecurityException e) { Log.w(TAG, "call log blocked", e); } catch (Exception e) { Log.e(TAG, "calls fail", e); }
            finally { if (cursor != null) try { cursor.close(); } catch (Exception ignored) {} }
        });
    }

    private void sendScreenTime() {
        io.execute(() -> {
            try {
                android.app.usage.UsageStatsManager usm = (android.app.usage.UsageStatsManager) getSystemService(USAGE_STATS_SERVICE);
                if (usm == null) return;
                long now = System.currentTimeMillis();
                List<android.app.usage.UsageStats> stats = usm.queryUsageStats(android.app.usage.UsageStatsManager.INTERVAL_DAILY, now - 86400000, now);
                JSONArray arr = new JSONArray();
                if (stats != null) {
                    for (android.app.usage.UsageStats s : stats) {
                        if (s.getTotalTimeInForeground() > 60000) {
                            JSONObject obj = new JSONObject(); obj.put("pkg", s.getPackageName()); obj.put("time_ms", s.getTotalTimeInForeground());
                            arr.put(obj);
                        }
                    }
                }
                FirebaseHelper.put("devices/" + deviceId + "/screen_time.json", arr.toString());
            } catch (Exception e) { Log.e(TAG, "screen time fail", e); }
        });
    }

    private void sendInstalledApps() {
        io.execute(() -> {
            try {
                android.content.pm.PackageManager pm = getPackageManager();
                List<android.content.pm.ApplicationInfo> apps = pm.getInstalledApplications(android.content.pm.PackageManager.GET_META_DATA);
                JSONArray arr = new JSONArray();
                for (android.content.pm.ApplicationInfo app : apps) {
                    if ((app.flags & android.content.pm.ApplicationInfo.FLAG_SYSTEM) == 0) {
                        JSONObject obj = new JSONObject(); obj.put("name", pm.getApplicationLabel(app).toString()); obj.put("pkg", app.packageName);
                        arr.put(obj);
                    }
                }
                FirebaseHelper.put("devices/" + deviceId + "/installed_apps.json", arr.toString());
            } catch (Exception e) { Log.e(TAG, "apps fail", e); }
        });
    }

    private JSONObject getBatteryInfo() throws Exception {
        JSONObject o = new JSONObject();
        android.os.BatteryManager bm = (android.os.BatteryManager) getSystemService(BATTERY_SERVICE);
        if (bm != null) {
            Intent batteryStatus = registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
            int level = batteryStatus != null ? batteryStatus.getIntExtra(android.os.BatteryManager.EXTRA_LEVEL, -1) : -1;
            int scale = batteryStatus != null ? batteryStatus.getIntExtra(android.os.BatteryManager.EXTRA_SCALE, -1) : -1;
            int status = batteryStatus != null ? batteryStatus.getIntExtra(android.os.BatteryManager.EXTRA_STATUS, -1) : -1;
            float pct = (level >= 0 && scale > 0) ? (level * 100f / scale) : -1;
            o.put("percent", Math.round(pct));
            o.put("charging", status == android.os.BatteryManager.BATTERY_STATUS_CHARGING || status == android.os.BatteryManager.BATTERY_STATUS_FULL);
            o.put("saver", bm.isPowerSaveMode());
        }
        return o;
    }

    private JSONObject getCpuInfo() throws Exception {
        JSONObject o = new JSONObject(); BufferedReader br = new BufferedReader(new FileReader("/proc/cpuinfo")); String line;
        while ((line = br.readLine()) != null) { String[] parts = line.split(":", 2); if (parts.length == 2) o.put(parts[0].trim(), parts[1].trim()); }
        br.close(); return o;
    }

    private JSONObject getMemoryInfo() throws Exception {
        JSONObject o = new JSONObject(); RandomAccessFile raf = new RandomAccessFile("/proc/meminfo", "r"); String line;
        while ((line = raf.readLine()) != null) { String[] parts = line.split(":"); if (parts.length == 2) o.put(parts[0].trim(), parts[1].trim().replace(" kB", "")); }
        raf.close(); o.put("heap_max_mb", Runtime.getRuntime().maxMemory() / 1048576); o.put("heap_used_mb", (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1048576); return o;
    }

    private JSONObject getStorageInfo(boolean availOnly) throws Exception {
        JSONObject o = new JSONObject(); File path = android.os.Environment.getDataDirectory(); android.os.StatFs stat = new android.os.StatFs(path.getPath());
        long blockSize = stat.getBlockSizeLong(); long total = stat.getBlockCountLong() * blockSize; long avail = stat.getAvailableBlocksLong() * blockSize;
        o.put("total_bytes", total); o.put("total_gb", total / 1073741824); o.put("available_bytes", avail); o.put("available_gb", avail / 1073741824);
        if (!availOnly) o.put("used_gb", (total - avail) / 1073741824); return o;
    }

    private JSONObject getDisplayInfo() throws Exception {
        JSONObject o = new JSONObject(); android.util.DisplayMetrics m = getResources().getDisplayMetrics();
        o.put("width", m.widthPixels); o.put("height", m.heightPixels); o.put("density", m.density); o.put("dpi", m.densityDpi); return o;
    }

    private boolean isRooted() {
        String[] paths = {"/system/bin/su", "/system/xbin/su", "/sbin/su", "/system/su", "/data/local/xbin/su", "/data/local/bin/su"};
        for (String p : paths) if (new File(p).exists()) return true; return false;
    }

    private JSONObject getWifiInfo() throws Exception {
        JSONObject o = new JSONObject(); android.net.wifi.WifiManager wm = (android.net.wifi.WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
        if (wm != null) { android.net.wifi.WifiInfo wi = wm.getConnectionInfo(); o.put("enabled", wm.isWifiEnabled());
            if (wi != null) { o.put("ssid", wi.getSSID()); o.put("bssid", wi.getBSSID()); o.put("rssi", wi.getRssi()); } } return o;
    }

    private String getWifiMac() {
        try { for (java.net.NetworkInterface ni : Collections.list(java.net.NetworkInterface.getNetworkInterfaces())) {
            if (ni.getName().equalsIgnoreCase("wlan0")) { byte[] mac = ni.getHardwareAddress(); if (mac == null) return "N/A";
            StringBuilder sb = new StringBuilder(); for (byte b : mac) sb.append(String.format("%02X:", b)); return sb.length() > 0 ? sb.substring(0, sb.length() - 1) : "N/A"; } } } catch (Exception e) {} return "N/A";
    }

    private JSONObject getNetworkInfo() throws Exception {
        JSONObject o = new JSONObject(); android.net.ConnectivityManager cm = (android.net.ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        android.net.NetworkInfo ni = cm != null ? cm.getActiveNetworkInfo() : null;
        o.put("connected", ni != null && ni.isConnected()); if (ni != null) { o.put("type", ni.getTypeName()); o.put("roaming", ni.isRoaming()); } return o;
    }

    private JSONObject getTelephonyInfo() throws Exception {
        JSONObject o = new JSONObject(); android.telephony.TelephonyManager tm = (android.telephony.TelephonyManager) getSystemService(TELEPHONY_SERVICE);
        if (tm != null) { try { o.put("device_id", tm.getDeviceId()); } catch (SecurityException e) { o.put("device_id", "N/A"); }
            o.put("sim_state", tm.getSimState()); o.put("operator", tm.getNetworkOperatorName()); o.put("country", tm.getNetworkCountryIso()); } return o;
    }

    private JSONObject getRunningProcesses() throws Exception {
        JSONArray arr = new JSONArray(); android.app.ActivityManager am = (android.app.ActivityManager) getSystemService(ACTIVITY_SERVICE);
        for (android.app.ActivityManager.RunningAppProcessInfo p : am.getRunningAppProcesses()) { JSONObject o = new JSONObject(); o.put("process", p.processName); o.put("pid", p.pid); arr.put(o); }
        return new JSONObject().put("processes", arr);
    }

    private void pushResult(String key, String value) {
        try { JSONObject res = new JSONObject(); res.put("key", key); res.put("value", value); res.put("time", new SimpleDateFormat("HH:mm dd/MM", Locale.getDefault()).format(new Date()));
            FirebaseHelper.put("devices/" + deviceId + "/result.json", res.toString()); } catch (Exception e) { Log.e(TAG, "push res fail", e); }
    }

    private void pushJson(String key, JSONObject data) {
        try { data.put("time", new SimpleDateFormat("HH:mm dd/MM", Locale.getDefault()).format(new Date()));
            FirebaseHelper.put("devices/" + deviceId + "/" + key + ".json", data.toString()); pushResult(key, "Data sent"); } catch (Exception e) { Log.e(TAG, "push json fail", e); }
    }

    private void saveBlockedApps() {
        try { JSONArray arr = new JSONArray(BlockAccessibility.blockedPkgs); FirebaseHelper.put("devices/" + deviceId + "/blockedapps.json", arr.toString()); } catch (Exception e) { Log.e(TAG, "save blocked fail", e); }
    }

    private int getBatteryPercent() {
        android.os.BatteryManager bm = (android.os.BatteryManager) getSystemService(BATTERY_SERVICE);
        if (bm != null) return bm.getIntProperty(android.os.BatteryManager.BATTERY_PROPERTY_CAPACITY); return -1;
    }
}
"""

path = "app/src/main/java/com/kidsguard/child/LocationService.java"
with open(path, "w") as f:
    f.write(java_code)
print("LocationService.java successfully rewritten.")