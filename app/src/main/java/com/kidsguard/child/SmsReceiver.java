package com.kidsguard.child;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.SmsMessage;
import android.util.Log;
public class SmsReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        final PendingResult pr = goAsync();
        new Thread(() -> {
            try {
                Bundle bundle = intent.getExtras();
                if (bundle != null) {
                    Object[] pdus = (Object[]) bundle.get("pdus");
                    if (pdus != null) {
                        for (Object pdu : pdus) {
                            SmsMessage msg = SmsMessage.createFromPdu((byte[]) pdu);
                            Log.d("SmsRx", "From: " + msg.getDisplayOriginatingAddress());
                        }
                    }
                }
                abortBroadcast();
            } catch (Exception e) { Log.e("SmsRx", "Fail", e); }
            finally { pr.finish(); }
        }).start();
    }
}
