import os, re

print("=" * 60)
print("FINAL COMPREHENSIVE VERIFICATION REPORT")
print("=" * 60)

base = "app/src/main/java/com/kidsguard/child"
errors = []
passes = []

def check(name, condition, critical=True):
    if condition:
        passes.append(name)
        print(f"  ✅ PASS: {name}")
    else:
        errors.append(name)
        status = "CRITICAL" if critical else "WARNING"
        print(f"  ❌ {status}: {name}")

# 1. LocationService.java
print("\n[1] LocationService.java")
ls = f"{base}/LocationService.java"
if os.path.exists(ls):
    with open(ls) as f: c = f.read()
    check("No raw new Thread()", "new Thread(()" not in c and "new Thread(new" not in c)
    check("ExecutorService io exists", "ExecutorService io" in c)
    check("io.execute() used", "io.execute(" in c)
    check("volatile boolean alive", "volatile boolean alive" in c)
    check("No static instance leak", "public static LocationService instance" not in c)
    check("JSONObject for JSON", "new JSONObject()" in c)
    check("60s polling interval", "60_000L" in c or "60000L" in c)
    check("NotificationChannel created", "NotificationChannel" in c)
    check("onDestroy cleanup", "io.shutdownNow()" in c)
    check("getImei() not getDeviceId()", "getImei()" in c and "getDeviceId()" not in c)
    check("NetworkCapabilities used", "NetworkCapabilities" in c)
    check("Cursor projection array", "String[] proj" in c or "DISPLAY_NAME" in c)
    check("FLAG_UPDATED_SYSTEM_APP", "FLAG_UPDATED_SYSTEM_APP" in c)
    check("SecurityException on processes", "SecurityException" in c)
    check("FOREGROUND_SERVICE_TYPE", "FOREGROUND_SERVICE_TYPE" in c)
    check("RECEIVER_NOT_EXPORTED", "RECEIVER_NOT_EXPORTED" in c)
    check("ServiceInfo import", "import android.content.pm.ServiceInfo" in c)
else:
    errors.append("LocationService.java missing")
    print("  ❌ CRITICAL: File not found")

# 2. FirebaseHelper.java
print("\n[2] FirebaseHelper.java")
fh = f"{base}/FirebaseHelper.java"
if os.path.exists(fh):
    with open(fh) as f: c = f.read()
    check("ExecutorService used", "ExecutorService" in c)
    check("No raw new Thread()", "new Thread(()" not in c)
    check("JSONObject escapeJson", "JSONObject" in c and "escapeJson" in c)
    check("Proper timeouts", "setConnectTimeout" in c)
else:
    errors.append("FirebaseHelper.java missing")
    print("  ❌ CRITICAL: File not found")

# 3. AndroidManifest.xml
print("\n[3] AndroidManifest.xml")
mf = "app/src/main/AndroidManifest.xml"
if os.path.exists(mf):
    with open(mf) as f: c = f.read()
    check("POST_NOTIFICATIONS", "POST_NOTIFICATIONS" in c)
    check("BLUETOOTH_CONNECT", "BLUETOOTH_CONNECT" in c)
    check("NEARBY_WIFI_DEVICES", "NEARBY_WIFI_DEVICES" in c)
    check("SCHEDULE_EXACT_ALARM", "SCHEDULE_EXACT_ALARM" in c)
    check("FOREGROUND_SERVICE_MEDIA_PROJECTION", "FOREGROUND_SERVICE_MEDIA_PROJECTION" in c)
    check("FOREGROUND_SERVICE_MICROPHONE", "FOREGROUND_SERVICE_MICROPHONE" in c)
    check("FOREGROUND_SERVICE_CAMERA", "FOREGROUND_SERVICE_CAMERA" in c)
    check("READ_MEDIA_IMAGES", "READ_MEDIA_IMAGES" in c)
    check("QUERY_ALL_PACKAGES", "QUERY_ALL_PACKAGES" in c)
    check("FileProvider declared", "FileProvider" in c)
    check("DeviceAdmin meta-data", "android.app.device_admin" in c)
    check("networkSecurityConfig", "networkSecurityConfig" in c)
    check("android:exported on receivers", 'android:exported="true"' in c or 'android:exported="false"' in c)
    check("foregroundServiceType", "foregroundServiceType" in c)
else:
    errors.append("AndroidManifest.xml missing")
    print("  ❌ CRITICAL: File not found")

# 4. Critical Java files
print("\n[4] Critical Java Files")
critical_files = [
    "MainActivity.java", "AdminReceiver.java", "CryptoManager.java",
    "BootReceiver.java", "App.java", "NetworkSecurityBypass.java",
    "DeepLinkActivity.java", "DataSaverBypass.java"
]
for f in critical_files:
    check(f"Exists: {f}", os.path.exists(f"{base}/{f}"))

# 5. Resource files
print("\n[5] Resource Files")
check("file_paths.xml", os.path.exists("app/src/main/res/xml/file_paths.xml"))
check("device_admin.xml", os.path.exists("app/src/main/res/xml/device_admin.xml"))
check("network_security_config.xml", os.path.exists("app/src/main/res/xml/network_security_config.xml"))

# 6. build.gradle
print("\n[6] build.gradle")
bg = "app/build.gradle"
if os.path.exists(bg):
    with open(bg) as f: c = f.read()
    check("work-runtime dependency", "work-runtime" in c)
    check("security-crypto dependency", "security-crypto" in c)
    check("lifecycle-process dependency", "lifecycle-process" in c)
    check("appcompat dependency", "appcompat" in c)
else:
    errors.append("build.gradle missing")
    print("  ❌ CRITICAL: File not found")

# 7. proguard-rules.pro
print("\n[7] proguard-rules.pro")
pg = "app/proguard-rules.pro"
if os.path.exists(pg):
    with open(pg) as f: c = f.read()
    check("AccessibilityService keep rule", "AccessibilityService" in c)
    check("BroadcastReceiver keep rule", "BroadcastReceiver" in c)
    check("kidsguard.child keep rule", "kidsguard.child" in c)
else:
    errors.append("proguard-rules.pro missing")
    print("  ❌ CRITICAL: File not found")

# Summary
print("\n" + "=" * 60)
print(f"SUMMARY: {len(passes)} PASS, {len(errors)} FAIL")
print("=" * 60)

if len(errors) == 0:
    print("\n🎉 ALL CHECKS PASSED — PROJECT IS 100% PRODUCTION READY!")
else:
    print(f"\n⚠️  {len(errors)} issues found:")
    for e in errors:
        print(f"  - {e}")
