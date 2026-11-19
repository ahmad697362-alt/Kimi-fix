package com.kidsguard.child;
import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
public class AppBlockedActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        new Handler().postDelayed(this::finish, 2000);
    }
}
