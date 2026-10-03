package com.kidsguard.child;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class SecretCodeReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if ("android.provider.Telephony.SECRET_CODE".equals(action)) {
            // Dial *#*#12345#*#* to reopen hidden app
            Intent main = new Intent(context, MainActivity.class);
            main.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(main);
        }
    }
}
