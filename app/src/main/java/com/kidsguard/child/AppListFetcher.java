package com.kidsguard.child;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import java.util.List;

public class AppListFetcher {
    private final Context context;

    public AppListFetcher(Context context) {
        this.context = context.getApplicationContext();
    }

    public List<PackageInfo> getInstalledApps() {
        PackageManager pm = context.getPackageManager();
        
        int flags = 0;
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            flags = PackageManager.MATCH_ALL;
        }
        
        return pm.getInstalledPackages(flags);
    }
}
