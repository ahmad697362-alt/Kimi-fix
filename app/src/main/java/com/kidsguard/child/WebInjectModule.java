package com.kidsguard.child;

import android.os.Build;
import android.webkit.WebView;
import android.util.Log;

public class WebInjectModule {
    private static final String TAG = "WebInject";

    public void injectScript(WebView webView, String script) {
        if (webView == null) return;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            if (webView.isAttachedToWindow() && webView.getHandler() != null) {
                try {
                    webView.evaluateJavascript(script, value -> {
                        Log.d(TAG, "JS evaluation completed: " + value);
                    });
                } catch (Exception e) {
                    Log.e(TAG, "JS injection failed (WebView likely destroyed)", e);
                }
            } else {
                Log.w(TAG, "WebView not attached, skipping injection");
            }
        }
    }
}
