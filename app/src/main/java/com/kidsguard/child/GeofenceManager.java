package com.kidsguard.child;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.location.LocationManager;
import android.os.Build;
import android.util.Log;
import androidx.core.content.ContextCompat;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingClient;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;

public class GeofenceManager {
    private static final String TAG = "GeofenceManager";
    private final Context context;
    private final GeofencingClient geofencingClient;

    public GeofenceManager(Context context) {
        this.context = context.getApplicationContext();
        this.geofencingClient = LocationServices.getGeofencingClient(this.context);
    }

    public void setGeofence(double lat, double lng, float radius) {
        Intent intent = new Intent(context, GeofenceReceiver.class);

        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }

        PendingIntent pi = PendingIntent.getBroadcast(context, 0, intent, flags);

        LocationManager lm = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        if (lm != null) {
            try {
                lm.addProximityAlert(lat, lng, radius, -1, pi);
            } catch (SecurityException e) {
                Log.w(TAG, "Location permission missing for proximity alert", e);
            }
        }
    }

    public void addGeofence(String id, double lat, double lng) {
        try {
            Geofence geofence = new Geofence.Builder()
                    .setRequestId(id)
                    .setCircularRegion(lat, lng, 100)
                    .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER | Geofence.GEOFENCE_TRANSITION_EXIT)
                    .setExpirationDuration(Geofence.NEVER_EXPIRE)
                    .build();

            GeofencingRequest request = new GeofencingRequest.Builder()
                    .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
                    .addGeofence(geofence)
                    .build();

            Intent intent = new Intent(context, GeofenceReceiver.class);
            int flags = PendingIntent.FLAG_UPDATE_CURRENT;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                flags |= PendingIntent.FLAG_MUTABLE;
            }
            PendingIntent pi = PendingIntent.getBroadcast(context, id.hashCode(), intent, flags);

            if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION)
                    == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                geofencingClient.addGeofences(request, pi)
                        .addOnSuccessListener(aVoid -> Log.d(TAG, "Geofence registered: " + id))
                        .addOnFailureListener(e -> Log.e(TAG, "Failed to register geofence: " + id, e));
            }
        } catch (SecurityException e) {
            Log.e(TAG, "SecurityException registering geofence", e);
        } catch (Exception e) {
            Log.e(TAG, "Exception registering geofence", e);
        }
    }

    public static class GeofenceReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "Geofence transition event received");
        }
    }
}
