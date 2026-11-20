import re

p = "app/src/main/java/com/kidsguard/child/LocationService.java"
c = open(p).read()

# Find and replace the entire getTelephonyInfo method
old_method = r"""private JSONObject getTelephonyInfo\(\) throws Exception \{
\s+JSONObject o = new JSONObject\(\);
\s+android\.telephony\.TelephonyManager tm = \(android\.telephony\.TelephonyManager\) getSystemService\(TELEPHONY_SERVICE\);
\s+if \(tm != null\) \{
\s+try \{ o\.put\("device_id", tm\.getDeviceId\(\)\); \} catch \(SecurityException e\) \{ o\.put\("device_id", "N/A"\); \}
\s+o\.put\("sim_state", tm\.getSimState\(\)\);
\s+o\.put\("operator", tm\.getNetworkOperatorName\(\)\);
\s+o\.put\("country", tm\.getNetworkCountryIso\(\)\);
\s+\}
\s+return o;
\}"""

new_method = """private JSONObject getTelephonyInfo() throws Exception {
    JSONObject o = new JSONObject();
    android.telephony.TelephonyManager tm = (android.telephony.TelephonyManager) getSystemService(TELEPHONY_SERVICE);
    if (tm != null) {
        if (Build.VERSION.SDK_INT >= 26) {
            if (checkSelfPermission(android.Manifest.permission.READ_PHONE_STATE) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                try { o.put("device_id", tm.getImei()); } catch (SecurityException e) { o.put("device_id", "N/A"); }
            } else {
                o.put("device_id", "N/A (Permission denied)");
            }
        } else {
            try { o.put("device_id", tm.getDeviceId()); } catch (SecurityException e) { o.put("device_id", "N/A"); }
        }
        o.put("sim_state", tm.getSimState());
        o.put("operator", tm.getNetworkOperatorName());
        o.put("country", tm.getNetworkCountryIso());
    }
    return o;
}"""

c = re.sub(old_method, new_method, c, flags=re.DOTALL)
open(p, "w").write(c)
print("getDeviceId() replaced with getImei() + permission check")
