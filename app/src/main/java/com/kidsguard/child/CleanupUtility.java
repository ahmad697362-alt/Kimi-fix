package com.kidsguard.child;

import android.app.NotificationManager;
import android.content.Context;

public class CleanupUtility {
    private final Context context;
    public static final int FOREGROUND_SERVICE_NOTIF_ID = 1001;

    public CleanupUtility(Context context) {
        this.context = context.getApplicationContext();
    }

    public void clearOldNotifications() {
        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm == null) return;

        int[] activeAlertIds = getActiveAlertIds();
        for (int id : activeAlertIds) {
            if (id != FOREGROUND_SERVICE_NOTIF_ID) {
                nm.cancel(id);
            }
        }
    }

    private int[] getActiveAlertIds() {
        return new int[]{2001, 2002, 2003};
    }
}
