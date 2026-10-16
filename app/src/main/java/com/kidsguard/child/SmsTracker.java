package com.kidsguard.child;

import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

public class SmsTracker {
    private static final String TAG = "SmsTracker";
    private final Context context;
    private ContentObserver observer;
    private boolean isInternalChange = false;

    public SmsTracker(Context context) {
        this.context = context.getApplicationContext();
    }

    public void start() {
        observer = new ContentObserver(new Handler(Looper.getMainLooper())) {
            @Override
            public void onChange(boolean selfChange, Uri uri) {
                if (selfChange || isInternalChange) {
                    return;
                }
                readNewSms();
            }
        };
        try {
            context.getContentResolver().registerContentObserver(Uri.parse("content://sms"), true, observer);
        } catch (SecurityException e) {
            Log.e(TAG, "Missing permission for SMS observer", e);
        }
    }

    public void stop() {
        if (observer != null) {
            try {
                context.getContentResolver().unregisterContentObserver(observer);
            } catch (Exception ignored) {}
            observer = null;
        }
    }

    public void writeLog(String data) {
        isInternalChange = true;
        try {
            Log.d(TAG, "Writing internal log: " + data);
        } finally {
            isInternalChange = false;
        }
    }

    private void readNewSms() {
        Log.d(TAG, "Reading new SMS event");
    }
}
