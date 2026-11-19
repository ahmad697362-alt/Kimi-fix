package com.kidsguard.child;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
public class SmsReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        final PendingResult pr = goAsync();
        new Thread(() -> {
            try {
                Bundle b = intent.getExtras();
                if (b != null) { /* parse sms */ }
            } catch (Exception e) { Log.e("SmsRx", "Fail", e); }
            finally { pr.finish(); }
        }).start();
    }
}
