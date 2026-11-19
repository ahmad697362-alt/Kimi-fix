import os
f1 = """package com.kidsguard.child;
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
"""
f2 = """package com.kidsguard.child;
import android.util.Log;
public class NetworkSecurityBypass {
    public void initSecureConnection() {
        Log.d("NetSec", "Using default system SSL. No bypass.");
    }
}
"""
f3 = """package com.kidsguard.child;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
public class DeepLinkActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent incoming = getIntent();
        if (incoming != null && incoming.hasExtra("forward_to")) {
            Intent forward = incoming.getParcelableExtra("forward_to");
            if (forward != null && isIntentSafe(forward)) {
                forward.setFlags(0);
                startActivity(forward);
            } else { Log.w("DeepLink", "Rejected unsafe intent"); }
        }
    }
    private boolean isIntentSafe(Intent intent) {
        return intent.getPackage() != null && intent.getPackage().equals(getPackageName());
    }
}
"""
base = "app/src/main/java/com/kidsguard/child"
with open(f"{base}/App.java", "w") as f: f.write(f1)
with open(f"{base}/NetworkSecurityBypass.java", "w") as f: f.write(f2)
with open(f"{base}/DeepLinkActivity.java", "w") as f: f.write(f3)
print("Chunk 2 done.")
