package com.kidsguard.child;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;

public class BlockWarningActivity extends Activity {

    @Override
    protected void onCreate(Bundle s) {
        super.onCreate(s);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setGravity(android.view.Gravity.CENTER);
        layout.setBackgroundColor(0xFF0D0D1A);
        layout.setPadding(60, 60, 60, 60);

        TextView tv = new TextView(this);
        tv.setText("This app is blocked by your parent.");
        tv.setTextSize(20);
        tv.setTextColor(0xFFFFFFFF);
        tv.setGravity(android.view.Gravity.CENTER);
        layout.addView(tv);

        TextView tv2 = new TextView(this);
        tv2.setText("Contact your parent to unblock this app.");
        tv2.setTextSize(14);
        tv2.setTextColor(0xFF888888);
        tv2.setGravity(android.view.Gravity.CENTER);
        tv2.setPadding(0, 20, 0, 0);
        layout.addView(tv2);

        setContentView(layout);
    }

    @Override
    public void onBackPressed() {
        performGlobalActionBack();
    }

    private void performGlobalActionBack() {
        // Home bhejo aur finish karo
        Intent home = new Intent(Intent.ACTION_MAIN);
        home.addCategory(Intent.CATEGORY_HOME);
        home.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(home);
        finish();
    }
}
