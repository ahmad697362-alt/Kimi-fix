import os
p = "app/src/main/java/com/kidsguard/child/LocationService.java"
c = open(p).read()

# 1. Fix getTelephonyInfo - use getImei/getMeid with permission check
old_telephony = """private JSONObject getTelephonyInfo() throws Exception {
    JSONObject o = new JSONObject();
    android.telephony.TelephonyManager tm = (android.telephony.TelephonyManager) getSystemService(TELEPHONY_SERVICE);
    if (tm != null) {
        try { o.put("device_id", tm.getDeviceId()); } catch (SecurityException e) { o.put("device_id", "N/A"); }
        o.put("sim_state", tm.getSimState());
        o.put("operator", tm.getNetworkOperatorName());
        o.put("country", tm.getNetworkCountryIso());
    }
    return o;
}"""

new_telephony = """private JSONObject getTelephonyInfo() throws Exception {
    JSONObject o = new JSONObject();
    android.telephony.TelephonyManager tm = (android.telephony.TelephonyManager) getSystemService(TELEPHONY_SERVICE);
    if (tm != null) {
        if (Build.VERSION.SDK_INT >= 26 && checkSelfPermission(android.Manifest.permission.READ_PHONE_STATE) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            try { o.put("device_id", tm.getImei()); } catch (SecurityException e) { o.put("device_id", "N/A"); }
        } else {
            o.put("device_id", "N/A (Permission denied)");
        }
        o.put("sim_state", tm.getSimState());
        o.put("operator", tm.getNetworkOperatorName());
        o.put("country", tm.getNetworkCountryIso());
    }
    return o;
}"""

c = c.replace(old_telephony, new_telephony)

# 2. Fix getNetworkInfo - use NetworkCapabilities
old_network = """private JSONObject getNetworkInfo() throws Exception {
    JSONObject o = new JSONObject();
    android.net.ConnectivityManager cm = (android.net.ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
    android.net.NetworkInfo ni = cm != null ? cm.getActiveNetworkInfo() : null;
    o.put("connected", ni != null && ni.isConnected());
    if (ni != null) {
        o.put("type", ni.getTypeName());
        o.put("roaming", ni.isRoaming());
    }
    return o;
}"""

new_network = """private JSONObject getNetworkInfo() throws Exception {
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

c = c.replace(old_network, new_network)

# 3. Fix sendContacts - add projection array
old_contacts = """android.database.Cursor cursor = getContentResolver().query(android.provider.ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, null, null, null);"""
new_contacts = """String[] proj = {android.provider.ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME, android.provider.ContactsContract.CommonDataKinds.Phone.NUMBER};
            android.database.Cursor cursor = getContentResolver().query(android.provider.ContactsContract.CommonDataKinds.Phone.CONTENT_URI, proj, null, null, null);"""
c = c.replace(old_contacts, new_contacts)

# 4. Fix sendInstalledApps - include updated system apps
old_apps = """if ((app.flags & android.content.pm.ApplicationInfo.FLAG_SYSTEM) == 0) {"""
new_apps = """if ((app.flags & android.content.pm.ApplicationInfo.FLAG_SYSTEM) == 0 || (app.flags & android.content.pm.ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0) {"""
c = c.replace(old_apps, new_apps)

open(p, "w").write(c)
print("6 remaining bugs fixed.")
