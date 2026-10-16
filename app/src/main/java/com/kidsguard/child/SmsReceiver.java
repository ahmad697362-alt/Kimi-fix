package com.kidsguard.child;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.SmsMessage;
import android.util.Log;

public class SmsReceiver extends BroadcastReceiver {
    private static final String TAG = "SmsReceiver";

    @Override
    public void onReceive(final Context context, final Intent intent) {
        if (intent == null) return;
        final PendingResult pendingResult = goAsync();
        
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Bundle bundle = intent.getExtras();
                    if (bundle != null) {
                        Object[] pdus = (Object[]) bundle.get("pdus");
                        String format = bundle.getString("format");
                        if (pdus != null) {
                            for (Object pdu : pdus) {
                                SmsMessage msg;
                                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                                    msg = SmsMessage.createFromPdu((byte[]) pdu, format);
                                } else {
                                    msg = SmsMessage.createFromPdu((byte[]) pdu);
                                }
                                if (msg != null) {
                                    String sender = msg.getDisplayOriginatingAddress();
                                    String body = msg.getMessageBody();
                                    Log.d(TAG, "Received SMS from " + sender + ": " + body);
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, "SMS processing failed", e);
                } finally {
                    pendingResult.finish();
                }
            }
        }).start();
    }
}
