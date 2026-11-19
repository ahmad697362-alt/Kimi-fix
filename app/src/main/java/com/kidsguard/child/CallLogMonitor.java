package com.kidsguard.child;
import android.content.Context;
import android.database.Cursor;
import android.provider.CallLog;
import android.util.Log;
public class CallLogMonitor {
    public void fetch(Context ctx) {
        Cursor c = null;
        try {
            c = ctx.getContentResolver().query(CallLog.Calls.CONTENT_URI, null, null, null, CallLog.Calls.DATE + " DESC LIMIT 50");
            if (c != null) while (c.moveToNext()) { /* process */ }
        } catch (SecurityException e) { Log.w("CallLog", "Permission denied"); }
        finally { if (c != null) c.close(); }
    }
}