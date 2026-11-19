package com.kidsguard.child;
import android.app.Application;
import android.content.Intent;
import android.os.Build;
import android.os.Process;
import android.util.Log;
public class App extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        Thread.setDefaultUncaughtExceptionHandler((t, e) -> {
            Log.e("App", "Uncaught in " + t.getName(), e);
            try {
                Intent i = new Intent(this, LocationService.class);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(i);
                else startService(i);
            } catch (Exception ex) {}
            Process.killProcess(Process.myPid());
            System.exit(1);
        });
    }
}
