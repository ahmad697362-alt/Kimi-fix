package com.kidsguard.child;

import android.app.admin.DeviceAdminReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

public class AdminReceiver extends DeviceAdminReceiver {

    @Override
    public void onEnabled(Context context, Intent intent) {
        super.onEnabled(context, intent);
        Toast.makeText(context, "Device Admin Enabled", Toast.LENGTH_SHORT).show();
        String devId = LocationService.getSafeDeviceId(context);
        FirebaseHelper.put("devices/" + devId + "/admin_status.json",
                "{\"status\":\"ENABLED\",\"time\":\"" + System.currentTimeMillis() + "\"}");
    }

    @Override
    public void onDisabled(Context context, Intent intent) {
        super.onDisabled(context, intent);
        Toast.makeText(context, "Device Admin Disabled!", Toast.LENGTH_LONG).show();
        String devId = LocationService.getSafeDeviceId(context);
        FirebaseHelper.put("devices/" + devId + "/admin_status.json",
                "{\"status\":\"DISABLED\",\"time\":\"" + System.currentTimeMillis() + "\"}");
        FirebaseHelper.put("devices/" + devId + "/alerts.json",
                "{\"title\":\"ADMIN DISABLED\",\"body\":\"Child disabled device admin!\",\"time\":\"" + System.currentTimeMillis() + "\"}");

        // Admin deactivate hone pe auth screen dikhao — taake parent ko pata chale
        Intent auth = new Intent(context, UninstallAuthActivity.class);
        auth.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        context.startActivity(auth);
    }

    @Override
    public CharSequence onDisableRequested(Context context, Intent intent) {
        return "Disabling device admin will remove parental protection. Are you sure?";
    }
}
