p = "app/src/main/java/com/kidsguard/child/LocationService.java"
c = open(p).read()

new_method = '''private JSONObject getTelephonyInfo() throws Exception {
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
            o.put("device_id", "N/A (API < 26)");
        }
        o.put("sim_state", tm.getSimState());
        o.put("operator", tm.getNetworkOperatorName());
        o.put("country", tm.getNetworkCountryIso());
    }
    return o;
}'''

start = c.find("private JSONObject getTelephonyInfo")
if start != -1:
    i = c.find("{", start)
    depth = 0
    j = i
    while j < len(c):
        if c[j] == "{": depth += 1
        elif c[j] == "}":
            depth -= 1
            if depth == 0: break
        j += 1
    c = c[:start] + new_method + c[j+1:]
    print("Method found and replaced")
else:
    idx = c.rfind("}")
    c = c[:idx] + new_method + "\n" + c[idx:]
    print("Method missing, inserted at end")

open(p, "w").write(c)
print("Done")
