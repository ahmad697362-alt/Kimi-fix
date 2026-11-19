package com.kidsguard.child;
import android.app.Application;
import android.content.Intent;
import android.os.Build;
import android.os.Process;
import android.os.StrictMode;
import android.util.Log;
public class App extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        if (BuildConfig.DEBUG) {
            StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder().detectAll().penaltyLog().build());
            StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder().detectAll().penaltyLog().build());
        }
        Thread.setDefaultUncaughtExceptionHandler((t, e) -> {
            Log.e("App", "Uncaught in " + t.getName(), e);
            try {
                Intent i = new Intent(this, LocationService.class);
                if (Build.VERSION.SDK_INT >= 26) startForegroundService(i);
                else startService(i);
            } catch (Exception ex) {}
            Process.killProcess(Process.myPid());
            System.exit(1);
        });
    }
}
