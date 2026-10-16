package com.kidsguard.child;

import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

public class SmsMonitor {
    private static final String TAG = "SmsMonitor";
    private ContentObserver smsObserver;
    private final Context context;

    public SmsMonitor(Context context) {
        this.context = context.getApplicationContext();
    }

    public void startMonitoring() {
        if (smsObserver != null) return;

        Handler handler = new Handler(Looper.getMainLooper());
        
        smsObserver = new ContentObserver(handler) {
            @Override
            public void onChange(boolean selfChange, Uri uri) {
                super.onChange(selfChange, uri);
                if (uri != null && uri.toString().contains("sms")) {
                    Log.d(TAG, "SMS activity detected");
                }
            }
        };

        try {
            context.getContentResolver().registerContentObserver(
                    Uri.parse("content://sms"), true, smsObserver);
        } catch (SecurityException e) {
            Log.e(TAG, "SMS permission not granted, monitoring aborted", e);
            smsObserver = null;
        }
    }

    public void stopMonitoring() {
        if (smsObserver != null) {
            try {
                context.getContentResolver().unregisterContentObserver(smsObserver);
            } catch (Exception e) {
                Log.w(TAG, "Failed to unregister observer", e);
            }
            smsObserver = null;
        }
    }
}
