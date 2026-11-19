package com.kidsguard.child;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
public class ActionReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(final Context ctx, final Intent intent) {
        final PendingResult pr = goAsync();
        new Thread(() -> {
            try { handleAction(ctx, intent); }
            catch (Exception e) { Log.e("Action", "Fail", e); }
            finally { pr.finish(); }
        }).start();
    }
    private void handleAction(Context ctx, Intent intent) {
        String action = intent.getStringExtra("action");
        Log.d("Action", "Received: " + action);
    }
}
