package com.kidsguard.child;

import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.view.Gravity;
import android.view.WindowManager;
import android.widget.TextView;

public class FloatingTimerWidget {
    private final Context context;
    private final WindowManager windowManager;
    private TextView view;

    public FloatingTimerWidget(Context context) {
        this.context = context.getApplicationContext();
        this.windowManager = (WindowManager) this.context.getSystemService(Context.WINDOW_SERVICE);
    }

    public void showWidget() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(context)) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + context.getPackageName()));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                try {
                    context.startActivity(intent);
                } catch (Exception ignored) {}
                return;
            }
        }

        int overlayType;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            overlayType = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            overlayType = WindowManager.LayoutParams.TYPE_PHONE;
        }

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                overlayType,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                PixelFormat.TRANSLUCENT);

        params.gravity = Gravity.TOP | Gravity.START;
        params.x = 0;
        params.y = 100;

        view = new TextView(context);
        view.setText("Time Remaining: 00:00");
        view.setTextColor(0xFFFFFFFF);
        view.setBackgroundColor(0xAA000000);
        view.setPadding(20, 10, 20, 10);
        
        try {
            if (windowManager != null) {
                windowManager.addView(view, params);
            }
        } catch (Exception ignored) {}
    }

    public void hideWidget() {
        if (view != null && view.isShown() && windowManager != null) {
            try {
                windowManager.removeView(view);
            } catch (Exception ignored) {}
            view = null;
        }
    }
}
