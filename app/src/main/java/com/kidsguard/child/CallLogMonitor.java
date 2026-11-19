package com.kidsguard.child;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.provider.CallLog;
import android.util.Log;
import androidx.core.content.ContextCompat;
public class CallLogMonitor {
    public void fetch(Context ctx) {
        if (ContextCompat.checkSelfPermission(ctx, android.Manifest.permission.READ_CALL_LOG) != PackageManager.PERMISSION_GRANTED) {
            Log.w("CallLog", "Permission missing"); return;
        }
        Cursor c = null;
        try {
            c = ctx.getContentResolver().query(CallLog.Calls.CONTENT_URI, null, null, null, CallLog.Calls.DATE + " DESC LIMIT 50");
            if (c != null) while (c.moveToNext()) { /* process */ }
        } catch (SecurityException e) { Log.w("CallLog", "Denied"); }
        finally { if (c != null) c.close(); }
    }
}
