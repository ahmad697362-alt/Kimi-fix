package com.kidsguard.child;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
public class ScreenStateReceiver extends BroadcastReceiver {
    public void register(Context ctx) {
        IntentFilter f = new IntentFilter();
        f.addAction(Intent.ACTION_SCREEN_ON);
        f.addAction(Intent.ACTION_SCREEN_OFF);
        ctx.registerReceiver(this, f);
    }
    public void unregister(Context ctx) { ctx.unregisterReceiver(this); }
    @Override public void onReceive(Context c, Intent i) {}
}
