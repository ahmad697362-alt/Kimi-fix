import os
p = "app/src/main/java/com/kidsguard/child/LocationService.java"
c = open(p).read()

# 1. Fix getTelephonyInfo - getDeviceId() → getImei() with permission check
c = c.replace(
    """try { o.put("device_id", tm.getDeviceId()); } catch (SecurityException e) { o.put("device_id", "N/A"); }""",
    """if (Build.VERSION.SDK_INT >= 26 && checkSelfPermission(android.Manifest.permission.READ_PHONE_STATE) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            try { o.put("device_id", tm.getImei()); } catch (SecurityException e) { o.put("device_id", "N/A"); }
        } else { o.put("device_id", "N/A (Permission denied)"); }"""
)

# 2. Fix getNetworkInfo - deprecated API → NetworkCapabilities
old_net = """private JSONObject getNetworkInfo() throws Exception {
    JSONObject o = new JSONObject(); android.net.ConnectivityManager cm = (android.net.ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
    android.net.NetworkInfo ni = cm != null ? cm.getActiveNetworkInfo() : null;
    o.put("connected", ni != null && ni.isConnected()); if (ni != null) { o.put("type", ni.getTypeName()); o.put("roaming", ni.isRoaming()); } return o;
}"""

new_net = """private JSONObject getNetworkInfo() throws Exception {
    JSONObject o = new JSONObject();
    android.net.ConnectivityManager cm = (android.net.ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
    if (cm != null) {
        if (Build.VERSION.SDK_INT >= 23) {
            android.net.Network n = cm.getActiveNetwork();
            if (n != null) {
                android.net.NetworkCapabilities caps = cm.getNetworkCapabilities(n);
                o.put("connected", caps != null && caps.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET));
                o.put("wifi", caps != null && caps.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI));
                o.put("cellular", caps != null && caps.hasTransport(android.net.NetworkCapabilities.TRANSPORT_CELLULAR));
            }
        } else {
            android.net.NetworkInfo ni = cm.getActiveNetworkInfo();
            o.put("connected", ni != null && ni.isConnected());
            if (ni != null) o.put("type", ni.getTypeName());
        }
    }
    return o;
}"""
c = c.replace(old_net, new_net)

# 3. Fix sendContacts - add projection array
c = c.replace(
    "android.database.Cursor cursor = getContentResolver().query(android.provider.ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, null, null, null);",
    "String[] proj = {android.provider.ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME, android.provider.ContactsContract.CommonDataKinds.Phone.NUMBER};\n            android.database.Cursor cursor = getContentResolver().query(android.provider.ContactsContract.CommonDataKinds.Phone.CONTENT_URI, proj, null, null, null);"
)

# 4. Fix sendInstalledApps - include updated system apps
c = c.replace(
    "if ((app.flags & android.content.pm.ApplicationInfo.FLAG_SYSTEM) == 0) {",
    "if ((app.flags & android.content.pm.ApplicationInfo.FLAG_SYSTEM) == 0 || (app.flags & android.content.pm.ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0) {"
)

# 5. Fix getRunningProcesses - wrap in try-catch (restricted API)
c = c.replace(
    """private JSONObject getRunningProcesses() throws Exception {
    JSONArray arr = new JSONArray(); android.app.ActivityManager am = (android.app.ActivityManager) getSystemService(ACTIVITY_SERVICE);
    for (android.app.ActivityManager.RunningAppProcessInfo p : am.getRunningAppProcesses()) { JSONObject o = new JSONObject(); o.put("process", p.processName); o.put("pid", p.pid); arr.put(o); }
    return new JSONObject().put("processes", arr);
}""",
    """private JSONObject getRunningProcesses() throws Exception {
    JSONArray arr = new JSONArray();
    try {
        android.app.ActivityManager am = (android.app.ActivityManager) getSystemService(ACTIVITY_SERVICE);
        if (am != null) {
            for (android.app.ActivityManager.RunningAppProcessInfo p : am.getRunningAppProcesses()) {
                JSONObject o = new JSONObject();
                o.put("process", p.processName);
                o.put("pid", p.pid);
                arr.put(o);
            }
        }
    } catch (SecurityException e) {
        arr.put(new JSONObject().put("process", "Restricted by OS").put("pid", 0));
    }
    return new JSONObject().put("processes", arr);
}"""
)

open(p, "w").write(c)
print("5 remaining bugs fixed.")
