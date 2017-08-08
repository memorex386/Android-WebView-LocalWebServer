package com.mtsdealersolutions.webview_local_server.ui;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Bitmap;
import android.net.http.SslError;
import android.os.Build;
import android.os.Message;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.View;
import android.webkit.ClientCertRequest;
import android.webkit.HttpAuthHandler;
import android.webkit.SslErrorHandler;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.mtsdealersolutions.webview_local_server.WebViewLocalServer;
import com.mtsdealersolutions.webview_local_server.config.ServerWebViewClient;
import com.mtsdealersolutions.webview_local_server.utils.ConfigWebViewUtil;

import java.util.Map;

/**
 * Created by bradley.thome on 4/20/17.
 */

public class LocalWebView extends WebView {

    private WebViewLocalServer mWebViewLocalServer;

    public LocalWebView(Context context) {
        super(context);
        init(context, null, -1, -1, false);
    }

    public LocalWebView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs, -1, -1, false);
    }

    public LocalWebView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs, defStyleAttr, -1, false);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public LocalWebView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context, attrs, defStyleAttr, defStyleRes, false);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public LocalWebView(Context context, AttributeSet attrs, int defStyleAttr, boolean privateBrowsing) {
        super(context, attrs, defStyleAttr, privateBrowsing);
        init(context, attrs, defStyleAttr, -1, privateBrowsing);
    }

    private void init(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes, boolean privateBrowsing) {

       ConfigWebViewUtil.setDefaultAppSettings(this);

        mWebViewLocalServer = new WebViewLocalServer(getContext());

    }

    public WebViewLocalServer getWebViewLocalServer() {
        return mWebViewLocalServer;
    }

    public void setWebViewLocalServer(WebViewLocalServer webViewLocalServer) {
        mWebViewLocalServer = webViewLocalServer;
    }

    @Override
    public void loadUrl(String url) {
        if (!TextUtils.isEmpty(url) && url.startsWith("file://")) {
            if (Build.VERSION.SDK_INT > 15) {
                getSettings().setAllowFileAccessFromFileURLs(true);
                getSettings().setAllowUniversalAccessFromFileURLs(true);
            }
        }
        super.loadUrl(url);
    }

    @Override
    public void loadUrl(String url, Map<String, String> additionalHttpHeaders) {
        if (!TextUtils.isEmpty(url) && url.startsWith("file://")) {
            if (Build.VERSION.SDK_INT > 15) {
                getSettings().setAllowFileAccessFromFileURLs(true);
                getSettings().setAllowUniversalAccessFromFileURLs(true);
            }
        }
        super.loadUrl(url, additionalHttpHeaders);
    }

    @Override
    public void setWebViewClient(WebViewClient webViewClient) {
        if (webViewClient instanceof ServerWebViewClient){
            super.setWebViewClient(webViewClient);
        } else {
            throw new RuntimeException("WebViewClient instance must extend ServerWebViewClient");
        }
    }



}
