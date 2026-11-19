package com.kidsguard.child;
import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
public class ScreenActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent i = new Intent(this, ScreenStreamService.class);
        if (Build.VERSION.SDK_INT >= 26) startForegroundService(i);
        else startService(i);
        finish();
    }
}
