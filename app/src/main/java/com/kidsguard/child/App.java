package com.kidsguard.child;

import android.app.Application;
import android.content.Intent;
import android.os.Build;
import android.os.Process;
import android.util.Log;

public class App extends Application {
    private static final String TAG = "App";

    @Override
    public void onCreate() {
        super.onCreate();
        
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            Log.e(TAG, "Uncaught exception in thread " + thread.getName(), throwable);
            
            try {
                Intent intent = new Intent(this, LocationService.class);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(intent);
                } else {
                    startService(intent);
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to restart service on crash", e);
            }

            Process.killProcess(Process.myPid());
            System.exit(1);
        });
    }
}
