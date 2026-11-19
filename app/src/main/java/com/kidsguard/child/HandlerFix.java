package com.kidsguard.child;
import android.os.Handler;
import android.os.Looper;
public class HandlerFix {
    private static Handler main;
    public static Handler getMain() {
        if (main == null) main = new Handler(Looper.getMainLooper());
        return main;
    }
}