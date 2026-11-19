package com.kidsguard.child;
import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
public class SmsMonitor {
    private ContentObserver obs;
    private Context ctx;
    public SmsMonitor(Context c) { this.ctx = c.getApplicationContext(); }
    public void start() {
        obs = new ContentObserver(new Handler(Looper.getMainLooper())) {
            @Override public void onChange(boolean selfChange) { if (!selfChange) process(); }
        };
        ctx.getContentResolver().registerContentObserver(Uri.parse("content://sms"), true, obs);
    }
    public void stop() { if (obs != null) { ctx.getContentResolver().unregisterContentObserver(obs); obs = null; } }
    private void process() {}
}