package com.kidsguard.child;
import android.app.Service;
import android.content.pm.ServiceInfo;
import android.os.Build;
public class FgsDataSync {
    public static void start(Service s, int id, android.app.Notification n) {
        if (Build.VERSION.SDK_INT >= 30) s.startForeground(id, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC);
        else s.startForeground(id, n);
    }
}