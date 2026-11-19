package com.kidsguard.child;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.location.LocationManager;
import android.os.Build;
import android.util.Log;
public class GeofenceManager {
    private final Context context;
    public GeofenceManager(Context context) { this.context = context.getApplicationContext(); }
    public void setGeofence(String id, double lat, double lng) {
        Intent intent = new Intent(context, GeofenceReceiver.class);
        intent.putExtra("geofence_id", id);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) flags |= PendingIntent.FLAG_IMMUTABLE;
        PendingIntent pi = PendingIntent.getBroadcast(context, id.hashCode(), intent, flags);
        LocationManager lm = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        if (lm != null) {
            try { lm.addProximityAlert(lat, lng, 100, -1, pi); Log.d("Geo", "Set: " + id); }
            catch (SecurityException e) { Log.e("Geo", "Permission missing", e); }
        }
    }
}
