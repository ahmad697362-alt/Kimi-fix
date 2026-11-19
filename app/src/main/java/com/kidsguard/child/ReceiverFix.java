package com.kidsguard.child;
import android.content.BroadcastReceiver;
import android.content.Context;
public class ReceiverFix {
    private static boolean reg = false;
    public static void reg(Context c, BroadcastReceiver r, android.content.IntentFilter f) { if (!reg) { c.registerReceiver(r, f); reg = true; } }
    public static void unreg(Context c, BroadcastReceiver r) { if (reg) { c.unregisterReceiver(r); reg = false; } }
}