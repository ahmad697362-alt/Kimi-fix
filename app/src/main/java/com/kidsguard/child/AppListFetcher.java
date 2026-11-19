package com.kidsguard.child;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;
import java.util.ArrayList;
import java.util.List;
public class AppListFetcher {
    private final Context context;
    public AppListFetcher(Context context) { this.context = context.getApplicationContext(); }
    public List<PackageInfo> getInstalledApps() {
        PackageManager pm = context.getPackageManager();
        List<PackageInfo> all = new ArrayList<>();
        try {
            int flags = Build.VERSION.SDK_INT < Build.VERSION_CODES.R ? PackageManager.MATCH_ALL : 0;
            List<PackageInfo> base = pm.getInstalledPackages(flags);
            for (PackageInfo info : base) {
                try { all.add(pm.getPackageInfo(info.packageName, PackageManager.GET_ACTIVITIES | PackageManager.GET_SERVICES)); }
                catch (PackageManager.NameNotFoundException e) { all.add(info); }
            }
        } catch (Exception e) { Log.e("AppList", "Fetch failed", e); all = pm.getInstalledPackages(0); }
        return all;
    }
}
