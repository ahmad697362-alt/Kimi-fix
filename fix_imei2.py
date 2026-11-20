p = "app/src/main/java/com/kidsguard/child/LocationService.java"
lines = open(p).readlines()

new_lines = []
for i, line in enumerate(lines):
    if "getDeviceId()" in line:
        # Replace with getImei() + permission check
        indent = line[:len(line) - len(line.lstrip())]
        new_lines.append(indent + 'if (Build.VERSION.SDK_INT >= 26 && checkSelfPermission(android.Manifest.permission.READ_PHONE_STATE) == android.content.pm.PackageManager.PERMISSION_GRANTED) {\n')
        new_lines.append(indent + '    try { o.put("device_id", tm.getImei()); } catch (SecurityException e) { o.put("device_id", "N/A"); }\n')
        new_lines.append(indent + '} else {\n')
        new_lines.append(indent + '    o.put("device_id", "N/A (Permission denied)");\n')
        new_lines.append(indent + '}\n')
        print(f"Line {i+1} replaced")
    else:
        new_lines.append(line)

open(p, "w").writelines(new_lines)
print("getDeviceId() removed, getImei() added")
