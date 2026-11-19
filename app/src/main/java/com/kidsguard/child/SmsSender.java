package com.kidsguard.child;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.telephony.SmsManager;
import android.util.Log;
public class SmsSender {
    public static void sendSms(Context context, String number, String message) {
        if (androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.SEND_SMS) != android.content.pm.PackageManager.PERMISSION_GRANTED) return;
        try {
            SmsManager sms = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ? context.getSystemService(SmsManager.class) : SmsManager.getDefault();
            int flags = PendingIntent.FLAG_UPDATE_CURRENT;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) flags |= PendingIntent.FLAG_MUTABLE;
            PendingIntent pi = PendingIntent.getBroadcast(context, 0, new Intent("SMS_SENT"), flags);
            if (message.length() > 160) sms.sendMultipartTextMessage(number, null, sms.divideMessage(message), null, null);
            else sms.sendTextMessage(number, null, message, pi, null);
        } catch (Exception e) { Log.e("SmsSender", "Send failed", e); }
    }
}
