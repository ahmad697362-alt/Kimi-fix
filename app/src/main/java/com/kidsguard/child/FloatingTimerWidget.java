package com.kidsguard.child;
import android.content.Context;
import android.graphics.PixelFormat;
import android.os.Build;
import android.view.WindowManager;
import android.widget.TextView;
public class FloatingTimerWidget {
    public void show(Context ctx) {
        if (Build.VERSION.SDK_INT >= 26 && !android.provider.Settings.canDrawOverlays(ctx)) return;
        WindowManager wm = (WindowManager) ctx.getSystemService(Context.WINDOW_SERVICE);
        int type = Build.VERSION.SDK_INT >= 26 ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY : WindowManager.LayoutParams.TYPE_PHONE;
        WindowManager.LayoutParams p = new WindowManager.LayoutParams(100, 100, type, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, PixelFormat.TRANSLUCENT);
        wm.addView(new TextView(ctx), p);
    }
}