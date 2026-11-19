import os

manifest_path = "app/src/main/AndroidManifest.xml"

if not os.path.exists(manifest_path):
    print("Manifest not found!")
    exit()

with open(manifest_path, "r") as f:
    content = f.read()

permissions_to_add = [
    '<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />',
    '<uses-permission android:name="android.permission.BLUETOOTH_CONNECT" />',
    '<uses-permission android:name="android.permission.BLUETOOTH_SCAN" />',
    '<uses-permission android:name="android.permission.NEARBY_WIFI_DEVICES" android:usesPermissionFlags="neverForLocation" />',
    '<uses-permission android:name="android.permission.READ_MEDIA_IMAGES" />',
    '<uses-permission android:name="android.permission.READ_MEDIA_VIDEO" />',
    '<uses-permission android:name="android.permission.SCHEDULE_EXACT_ALARM" />',
    '<uses-permission android:name="android.permission.USE_EXACT_ALARM" />',
    '<uses-permission android:name="android.permission.FOREGROUND_SERVICE_MEDIA_PROJECTION" />',
    '<uses-permission android:name="android.permission.FOREGROUND_SERVICE_MICROPHONE" />',
    '<uses-permission android:name="android.permission.FOREGROUND_SERVICE_CAMERA" />',
    '<uses-permission android:name="android.permission.FOREGROUND_SERVICE_DATA_SYNC" />'
]

added_count = 0
for perm in permissions_to_add:
    if perm not in content:
        # Insert before </manifest> or after the last permission
        if "</manifest>" in content:
            content = content.replace("</manifest>", f"    {perm}\n</manifest>")
            added_count += 1

if added_count > 0:
    with open(manifest_path, "w") as f:
        f.write(content)
    print(f"Manifest updated: Added {added_count} new permissions.")
else:
    print("Manifest already up to date.")
