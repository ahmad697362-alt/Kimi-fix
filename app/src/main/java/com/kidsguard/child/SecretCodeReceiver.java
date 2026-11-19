package com.kidsguard.child;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.util.Log;
public class SecretCodeReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context ctx, Intent intent) {
        try {
            ComponentName cn = new ComponentName(ctx, "com.kidsguard.child.MainActivityLauncher");
            ctx.getPackageManager().setComponentEnabledSetting(cn,
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP);
            Log.d("SecretCode", "App icon restored");
        } catch (Exception e) {
            Log.e("SecretCode", "Restore failed", e);
        }
    }
}
