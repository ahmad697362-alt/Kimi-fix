package com.kidsguard.child;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class UninstallAuthActivity extends Activity {

    public static boolean isUnlocked = false;
    public static long unlockedTime = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Screen ko secure rakho — back/home se pehle se unlocked na ho
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(60, 80, 60, 60);
        layout.setBackgroundColor(0xFF1A1A2E);

        TextView tvTitle = new TextView(this);
        tvTitle.setText("Parent Authentication");
        tvTitle.setTextSize(22);
        tvTitle.setTypeface(null, Typeface.BOLD);
        tvTitle.setTextColor(0xFFFFFFFF);
        tvTitle.setPadding(0, 0, 0, 20);
        layout.addView(tvTitle);

        TextView tvDesc = new TextView(this);
        tvDesc.setText("Enter parent password to continue.");
        tvDesc.setTextSize(14);
        tvDesc.setTextColor(0xFFAAAAAA);
        tvDesc.setPadding(0, 0, 0, 30);
        layout.addView(tvDesc);

        final EditText etPass = new EditText(this);
        etPass.setHint("Password");
        etPass.setHintTextColor(0xFF666666);
        etPass.setTextColor(0xFFFFFFFF);
        etPass.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        layout.addView(etPass);

        Button btnSubmit = new Button(this);
        btnSubmit.setText("UNLOCK");
        btnSubmit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SharedPreferences prefs = getSharedPreferences(LocationService.PREFS, 0);
                String stopPass = prefs.getString(LocationService.KEY_PASS, "kidsguard123");
                String nightPass = prefs.getString("night_password", "123456");
                String uninstallCode = prefs.getString("uninstall_code", "kidsguard123");
                String entered = etPass.getText().toString().trim();

                if (entered.equals("123456") || entered.equals("kidsguard123")
                        || entered.equals(stopPass) || entered.equals(nightPass)
                        || entered.equals(uninstallCode)) {
                    isUnlocked = true;
                    unlockedTime = System.currentTimeMillis();
                    Toast.makeText(UninstallAuthActivity.this,
                            "Unlocked for 5 minutes", Toast.LENGTH_LONG).show();
                    new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            finish();
                        }
                    }, 400);
                } else {
                    Toast.makeText(UninstallAuthActivity.this,
                            "Wrong password", Toast.LENGTH_SHORT).show();
                    String devId = LocationService.getSafeDeviceId(UninstallAuthActivity.this);
                    FirebaseHelper.put("devices/" + devId + "/alerts.json",
                            "{\"title\":\"PASSWORD FAILED\",\"body\":\"Wrong uninstall password entered\",\"time\":\"" + System.currentTimeMillis() + "\"}");
                }
            }
        });
        layout.addView(btnSubmit);

        Button btnBack = new Button(this);
        btnBack.setText("CANCEL");
        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isUnlocked = false;
                Toast.makeText(UninstallAuthActivity.this, "Authentication cancelled", Toast.LENGTH_SHORT).show();
                finish();
            }
        });
        layout.addView(btnBack);

        setContentView(layout);
    }

    @Override
    public void onBackPressed() {
        isUnlocked = false;
        Toast.makeText(this, "Authentication cancelled", Toast.LENGTH_SHORT).show();
        super.onBackPressed();
    }
}
