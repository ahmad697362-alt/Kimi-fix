package com.kidsguard.child;

import android.content.Context;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;

public class PhoneStateMonitor {
    private static final String TAG = "PhoneStateMonitor";
    private final Context context;
    private TelephonyManager tm;
    private PhoneStateListener listener;

    public PhoneStateMonitor(Context context) {
        this.context = context.getApplicationContext();
    }

    public void start() {
        tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        if (tm == null) return;

        listener = new PhoneStateListener() {
            @Override
            public void onCallStateChanged(int state, String phoneNumber) {
                Log.d(TAG, "Call state changed: " + state);
            }
        };
        tm.listen(listener, PhoneStateListener.LISTEN_CALL_STATE);
    }

    public void stop() {
        if (tm != null && listener != null) {
            tm.listen(listener, PhoneStateListener.LISTEN_NONE);
            listener = null;
        }
    }
}
