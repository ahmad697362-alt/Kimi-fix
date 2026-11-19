package com.kidsguard.child;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.location.LocationManager;
import android.util.Log;
import org.json.JSONObject;
public class GeofenceReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context ctx, Intent intent) {
        if (intent == null) return;
        String id = intent.getStringExtra("geofence_id");
        boolean entering = intent.getBooleanExtra(LocationManager.KEY_PROXIMITY_ENTERING, false);
        Log.d("Geofence", (entering ? "Enter" : "Exit") + " id=" + id);
        try {
            JSONObject o = new JSONObject();
            o.put("geofence_id", id != null ? id : "unknown");
            o.put("entering", entering);
            o.put("time", System.currentTimeMillis());
            FirebaseHelper.put("devices/" + LocationService.getSafeDeviceId(ctx) + "/geofence.json", o.toString());
        } catch (Exception e) { Log.e("Geofence", "Fail", e); }
    }
}
