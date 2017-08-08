package com.mtsdealersolutions.webview_local_server.utils;

import android.content.Context;
import android.os.Build;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;

/**
 * Created by bradley.thome on 8/8/17.
 */

public class ConfigWebViewUtil {

    public static void setDefaultAppSettings(WebView webView){

        if (webView == null) return;

        webView.setFocusable(true);
        webView.setFocusableInTouchMode(true);

        webView.setInitialScale(0);
        webView.setVerticalScrollBarEnabled(false);

        // Set true for enable JavaScript feature or Set False to Disable JavaScript.
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setLayoutAlgorithm(WebSettings.LayoutAlgorithm.NORMAL);
        webView.getSettings().setJavaScriptCanOpenWindowsAutomatically(true);

        //Make WebView sizing feel more like an app
        webView.getSettings().setUseWideViewPort(true);
        webView.getSettings().setLoadWithOverviewMode(true);
        webView.getSettings().setSupportZoom(false);

        // Jellybean rightfully tried to lock this down. Too bad they didn't give us a whitelist
        // while we do this
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            webView.getSettings().setAllowUniversalAccessFromFileURLs(true);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            webView.getSettings().setMediaPlaybackRequiresUserGesture(false);
        }
        // Enable database
        // We keep this disabled because we use or shim to get around DOM_EXCEPTION_ERROR_16
        String databasePath = webView.getContext().getApplicationContext().getDir("database", Context.MODE_PRIVATE).getPath();
        webView.getSettings().setDatabaseEnabled(true);

        //Deprecated but still used API 23 and below
        webView.getSettings().setDatabasePath(databasePath);
        webView.getSettings().setGeolocationDatabasePath(databasePath);

        // Enable DOM storage
        webView.getSettings().setDomStorageEnabled(true);

        // Enable built-in geolocation
        webView. getSettings().setGeolocationEnabled(true);

        // Enable AppCache
        webView. getSettings().setAppCacheMaxSize(5 * 1048576);
        webView. getSettings().setAppCachePath(databasePath);
        webView. getSettings().setAppCacheEnabled(true);

        //Deprecated in API 18, but changing from NORMAL to HIGH for older APIs
        webView.getSettings().setRenderPriority(WebSettings.RenderPriority.HIGH);

        //These should be set by default already, but just in case...
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            // chromium, enable hardware acceleration
            webView. setLayerType(View.LAYER_TYPE_HARDWARE, null);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            // older android version, disable hardware acceleration
            webView. setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        }

    }

}
