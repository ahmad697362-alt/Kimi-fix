import os, re

print("=" * 50)
print("PROJECT VERIFICATION REPORT")
print("=" * 50)

# 1. build.gradle check
bg = "app/build.gradle"
if os.path.exists(bg):
    with open(bg) as f: c = f.read()
    checks = {
        "work-runtime": "androidx.work:work-runtime" in c,
        "security-crypto": "androidx.security:security-crypto" in c,
        "lifecycle-process": "androidx.lifecycle:lifecycle-process" in c,
        "appcompat": "androidx.appcompat:appcompat" in c,
        "core-ktx": "androidx.core:core" in c,
    }
    print("\n[1] build.gradle:")
    for k, v in checks.items():
        print(f"  {'PASS' if v else 'FAIL'}: {k}")
else:
    print("\n[1] build.gradle: FILE NOT FOUND")

# 2. proguard-rules.pro check
pg = "app/proguard-rules.pro"
if os.path.exists(pg):
    with open(pg) as f: c = f.read()
    print("\n[2] proguard-rules.pro:")
    print(f"  {'PASS' if 'AccessibilityService' in c else 'FAIL'}: Accessibility keep rule")
    print(f"  {'PASS' if 'BroadcastReceiver' in c else 'FAIL'}: Broadcast keep rule")
    print(f"  {'PASS' if 'kidsguard.child' in c else 'FAIL'}: Package keep rule")
    print(f"  {'PASS' if 'native <methods>' in c else 'FAIL'}: Native methods rule")
else:
    print("\n[2] proguard-rules.pro: FILE NOT FOUND")

# 3. AndroidManifest.xml check
mf = "app/src/main/AndroidManifest.xml"
if os.path.exists(mf):
    with open(mf) as f: c = f.read()
    perms = ["POST_NOTIFICATIONS", "BLUETOOTH_CONNECT", "NEARBY_WIFI_DEVICES",
             "SCHEDULE_EXACT_ALARM", "FOREGROUND_SERVICE_MEDIA_PROJECTION",
             "FOREGROUND_SERVICE_MICROPHONE", "FOREGROUND_SERVICE_CAMERA",
             "READ_MEDIA_IMAGES", "QUERY_ALL_PACKAGES"]
    print("\n[3] AndroidManifest.xml:")
    for p in perms:
        print(f"  {'PASS' if p in c else 'FAIL'}: {p}")
    print(f"  {'PASS' if 'FileProvider' in c else 'FAIL'}: FileProvider declared")
    print(f"  {'PASS' if 'device_admin.xml' in c else 'FAIL'}: DeviceAdmin meta-data")
else:
    print("\n[3] AndroidManifest.xml: FILE NOT FOUND")

# 4. res/xml files
print("\n[4] res/xml files:")
for x in ["file_paths.xml", "device_admin.xml", "network_security_config.xml"]:
    p = f"app/src/main/res/xml/{x}"
    print(f"  {'PASS' if os.path.exists(p) else 'FAIL'}: {x}")

# 5. Java files count
print("\n[5] Java files in child package:")
b = "app/src/main/java/com/kidsguard/child"
if os.path.exists(b):
    files = [f for f in os.listdir(b) if f.endswith(".java")]
    print(f"  Total: {len(files)} files")
    critical = ["LocationService.java", "FirebaseHelper.java", "MainActivity.java",
                "AdminReceiver.java", "CryptoManager.java", "DataSaverBypass.java"]
    for c in critical:
        print(f"  {'PASS' if c in files else 'FAIL'}: {c}")
else:
    print("  FAIL: Directory not found")

print("\n" + "=" * 50)
