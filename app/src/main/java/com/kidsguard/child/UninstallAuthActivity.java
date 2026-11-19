package com.kidsguard.child;
import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.os.Bundle;
import android.util.Log;
public class UninstallAuthActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        DevicePolicyManager dpm = (DevicePolicyManager) getSystemService(DEVICE_POLICY_SERVICE);
        ComponentName cn = new ComponentName(this, AdminReceiver.class);
        if (dpm != null && dpm.isAdminActive(cn)) {
            Log.d("UninstallAuth", "Admin active — blocking uninstall");
        }
        finish();
    }
}
