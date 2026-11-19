package com.kidsguard.child;
import android.app.Notification;
import android.app.Service;
import android.content.pm.ServiceInfo;
import android.os.Build;
public class ForegroundServiceManager {
    public static void start(Service svc, int id, Notification notif, String type) {
        if (Build.VERSION.SDK_INT >= 34) {
            int fgType = ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE;
            if ("location".equals(type)) fgType = ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION;
            else if ("mediaProjection".equals(type)) fgType = ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION;
            else if ("microphone".equals(type)) fgType = ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE;
            else if ("camera".equals(type)) fgType = ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA;
            else if ("dataSync".equals(type)) fgType = ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC;
            svc.startForeground(id, notif, fgType);
        } else if (Build.VERSION.SDK_INT >= 29) {
            int fgType = ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE;
            if ("location".equals(type)) fgType = ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION;
            else if ("mediaProjection".equals(type)) fgType = ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION;
            else if ("microphone".equals(type)) fgType = ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE;
            else if ("camera".equals(type)) fgType = ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA;
            svc.startForeground(id, notif, fgType);
        } else {
            svc.startForeground(id, notif);
        }
    }
}
