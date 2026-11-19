package com.kidsguard.child;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
public class StreamService extends Service {
    @Override public IBinder onBind(Intent i) { return null; }
}