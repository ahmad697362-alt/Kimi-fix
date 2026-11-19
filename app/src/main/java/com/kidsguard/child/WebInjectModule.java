package com.kidsguard.child;
import android.os.Build;
import android.webkit.WebView;
import android.webkit.WebViewClient;
public class WebInjectModule {
    public void initWebView(WebView webView) {
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setAllowFileAccess(false);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            webView.getSettings().setAllowUniversalAccessFromFileURLs(false);
            webView.getSettings().setAllowFileAccessFromFileURLs(false);
        }
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                if (url != null && url.contains("kidsguard-internal")) {
                    view.addJavascriptInterface(new Object(), "AndroidBridge");
                }
            }
        });
    }
}
