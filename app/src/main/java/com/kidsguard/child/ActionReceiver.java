package com.kidsguard.child;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class ActionReceiver extends BroadcastReceiver {
    private static final String TAG = "ActionReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) return;
        String action = intent.getStringExtra("action");
        Log.d(TAG, "Received notification action: " + action);
    }
}
