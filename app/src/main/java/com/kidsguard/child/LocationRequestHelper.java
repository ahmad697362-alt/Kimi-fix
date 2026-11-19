package com.kidsguard.child;
import android.location.LocationRequest;
import android.os.Build;
public class LocationRequestHelper {
    public static LocationRequest createHighAccuracy() {
        LocationRequest req = new LocationRequest();
        req.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        req.setInterval(10000);
        req.setFastestInterval(5000);
        return req;
    }
}
