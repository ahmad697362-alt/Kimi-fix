import os
b = "app/src/main/java/com/kidsguard/child"

code = """package com.kidsguard.child;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.util.Log;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;
public class AppListFetcher {
    public static JSONArray getUserAppsJson(Context ctx) {
        JSONArray arr = new JSONArray();
        PackageManager pm = ctx.getPackageManager();
        List<PackageInfo> packages = pm.getInstalledPackages(0);
        for (PackageInfo p : packages) {
            if ((p.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) == 0) {
                try {
                    JSONObject obj = new JSONObject();
                    obj.put("name", pm.getApplicationLabel(p.applicationInfo).toString());
                    obj.put("pkg", p.packageName);
                    arr.put(obj);
                } catch (Exception e) { Log.e("AppList", "Error processing " + p.packageName, e); }
            }
        }
        return arr;
    }
}
"""

with open(f"{b}/AppListFetcher.java", "w") as f:
    f.write(code)
print("AppListFetcher fixed for large lists.")
