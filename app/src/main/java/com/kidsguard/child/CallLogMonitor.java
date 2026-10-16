package com.kidsguard.child;

import android.app.role.RoleManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Build;
import android.provider.CallLog;
import android.util.Log;
import androidx.core.content.ContextCompat;

public class CallLogMonitor {
    private static final String TAG = "CallLogMonitor";
    private final Context context;

    public CallLogMonitor(Context context) {
        this.context = context.getApplicationContext();
    }

    public void fetchCallLogs() {
        if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_CALL_LOG)
                != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "READ_CALL_LOG permission missing");
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            RoleManager roleManager = (RoleManager) context.getSystemService(Context.ROLE_SERVICE);
            if (roleManager != null && !roleManager.isRoleHeld(RoleManager.ROLE_DIALER)) {
                Log.w(TAG, "App is not the default dialer. Call log access denied by OS.");
                requestDialerRole(roleManager);
                return;
            }
        }

        Cursor cursor = null;
        try {
            cursor = context.getContentResolver().query(
                    CallLog.Calls.CONTENT_URI,
                    new String[]{CallLog.Calls.NUMBER, CallLog.Calls.TYPE, CallLog.Calls.DATE},
                    null, null, CallLog.Calls.DEFAULT_SORT_ORDER);
            
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    // Process cursor data safely
                }
            }
        } catch (SecurityException e) {
            Log.e(TAG, "Call log query blocked by OS policy", e);
        } catch (Exception e) {
            Log.e(TAG, "Call log query failed", e);
        } finally {
            if (cursor != null) cursor.close();
        }
    }

    private void requestDialerRole(RoleManager roleManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                Intent intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_DIALER);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
            } catch (Exception e) {
                Log.w(TAG, "Failed to launch dialer role request", e);
            }
        }
    }
}
