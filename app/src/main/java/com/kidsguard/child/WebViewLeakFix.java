package com.kidsguard.child;
import android.content.Context;
import android.webkit.WebView;
public class WebViewLeakFix {
    public static void destroy(WebView w) {
        if (w != null) { w.stopLoading(); w.loadDataWithBaseURL(null, "", "text/html", "utf-8", null); w.clearHistory(); ((android.view.ViewGroup) w.getParent()).removeView(w); w.destroy(); }
    }
}