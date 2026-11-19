package com.kidsguard.child;
import android.content.Context;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
public class PhoneStateMonitor {
    private TelephonyManager tm;
    private PhoneStateListener l;
    public void start(Context ctx) {
        tm = (TelephonyManager) ctx.getSystemService(Context.TELEPHONY_SERVICE);
        if (tm == null) return;
        l = new PhoneStateListener() { @Override public void onCallStateChanged(int s, String n) {} };
        tm.listen(l, PhoneStateListener.LISTEN_CALL_STATE);
    }
    public void stop() {
        if (tm != null && l != null) { tm.listen(l, PhoneStateListener.LISTEN_NONE); l = null; }
    }
}
