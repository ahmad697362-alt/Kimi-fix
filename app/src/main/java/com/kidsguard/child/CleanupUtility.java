package com.kidsguard.child;
import android.app.NotificationManager;
import android.content.Context;
public class CleanupUtility {
    private final Context ctx;
    public CleanupUtility(Context c) { this.ctx = c.getApplicationContext(); }
    public void clearOld() {
        NotificationManager nm = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm == null) return;
        int[] ids = {1002, 2001, 3001};
        for (int id : ids) {
            if (id != 1001) nm.cancel(id);
        }
    }
}
