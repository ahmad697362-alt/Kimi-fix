import os, re

# 1. Fix AndroidManifest.xml - Add DeviceAdmin meta-data
manifest_path = "app/src/main/AndroidManifest.xml"
if os.path.exists(manifest_path):
    with open(manifest_path, "r") as f:
        content = f.read()
    
    # Check if DeviceAdmin meta-data already exists
    if "android.app.device_admin" not in content:
        # Find AdminReceiver and add meta-data inside it
        # Pattern: <receiver android:name=".AdminReceiver" ...>
        pattern = r'(<receiver[^>]*android:name="\.AdminReceiver"[^>]*>)'
        replacement = r'\1\n        <meta-data android:name="android.app.device_admin" android:resource="@xml/device_admin" />'
        
        new_content = re.sub(pattern, replacement, content, flags=re.DOTALL)
        
        if new_content != content:
            with open(manifest_path, "w") as f:
                f.write(new_content)
            print("1. DeviceAdmin meta-data added to Manifest")
        else:
            print("1. Could not find AdminReceiver to patch - manual check needed")
    else:
        print("1. DeviceAdmin meta-data already present")
else:
    print("1. Manifest not found")

# 2. Create network_security_config.xml
xml_dir = "app/src/main/res/xml"
os.makedirs(xml_dir, exist_ok=True)

netsec_path = f"{xml_dir}/network_security_config.xml"
if not os.path.exists(netsec_path):
    netsec_content = """<?xml version="1.0" encoding="utf-8"?>
<network-security-config>
    <base-config cleartextTrafficPermitted="false">
        <trust-anchors>
            <certificates src="system" />
        </trust-anchors>
    </base-config>
    
    <!-- Allow cleartext only for local development if needed -->
    <domain-config cleartextTrafficPermitted="true">
        <domain includeSubdomains="false">10.0.2.2</domain>
        <domain includeSubdomains="false">localhost</domain>
    </domain-config>
</network-security-config>
"""
    with open(netsec_path, "w") as f:
        f.write(netsec_content)
    print("2. network_security_config.xml created")
else:
    print("2. network_security_config.xml already exists")

# 3. Add networkSecurityConfig reference to manifest if missing
if os.path.exists(manifest_path):
    with open(manifest_path, "r") as f:
        content = f.read()
    
    if 'networkSecurityConfig="@xml/network_security_config"' not in content:
        content = content.replace(
            '<application',
            '<application\n    android:networkSecurityConfig="@xml/network_security_config"',
            1
        )
        with open(manifest_path, "w") as f:
            f.write(content)
        print("3. networkSecurityConfig attribute added to <application>")
    else:
        print("3. networkSecurityConfig already referenced")

print("\nAll fixes applied.")
