package com.kidsguard.child;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.telephony.SmsManager;
import android.util.Log;
import androidx.core.content.ContextCompat;

public class SmsSender {
    private static final String TAG = "SmsSender";

    public static void sendSms(Context context, String number, String message) {
        if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.SEND_SMS) 
                != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "SEND_SMS permission not granted at runtime");
            return;
        }

        try {
            SmsManager smsManager;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                smsManager = context.getSystemService(SmsManager.class);
            } else {
                smsManager = SmsManager.getDefault();
            }

            int flags = PendingIntent.FLAG_UPDATE_CURRENT;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                flags |= PendingIntent.FLAG_IMMUTABLE;
            }

            Intent sentIntent = new Intent("SMS_SENT_ACTION");
            PendingIntent sentPI = PendingIntent.getBroadcast(context, 0, sentIntent, flags);

            Intent deliveredIntent = new Intent("SMS_DELIVERED_ACTION");
            PendingIntent deliveredPI = PendingIntent.getBroadcast(context, 0, deliveredIntent, flags);

            if (message != null && message.length() > 160) {
                smsManager.sendMultipartTextMessage(number, null, 
                        smsManager.divideMessage(message), 
                        null, null);
            } else {
                smsManager.sendTextMessage(number, null, message, sentPI, deliveredPI);
            }
        } catch (Exception e) {
            Log.e(TAG, "SMS send failed", e);
        }
    }
}
