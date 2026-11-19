package com.kidsguard.child;
import android.content.Context;
import android.os.Build;
import android.telephony.TelephonyCallback;
import android.telephony.TelephonyManager;
import android.util.Log;
public class PhoneStateMonitor {
    private TelephonyManager tm;
    private Object callback;
    public void start(Context ctx) {
        tm = (TelephonyManager) ctx.getSystemService(Context.TELEPHONY_SERVICE);
        if (tm == null) return;
        if (Build.VERSION.SDK_INT >= 31) {
            callback = new TelephonyCallback() implements TelephonyCallback.CallStateListener {
                @Override public void onCallStateChanged(int state) { Log.d("Phone", "State: " + state); }
            };
            tm.registerTelephonyCallback(ctx.getMainExecutor(), (TelephonyCallback) callback);
        } else {
            // Fallback for older APIs handled elsewhere or via reflection if strictly needed
        }
    }
    public void stop() {
        if (tm != null && callback != null && Build.VERSION.SDK_INT >= 31) {
            tm.unregisterTelephonyCallback((TelephonyCallback) callback);
            callback = null;
        }
    }
}
