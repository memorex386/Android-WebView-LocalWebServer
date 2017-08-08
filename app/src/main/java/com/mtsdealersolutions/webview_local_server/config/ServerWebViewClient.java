package com.mtsdealersolutions.webview_local_server.config;

import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.mtsdealersolutions.webview_local_server.WebViewLocalServer;

/**
 * Created by bradley.thome on 8/8/17.
 */

public class ServerWebViewClient extends WebViewClient {

    private WebViewLocalServer mWebViewLocalServer;

    public ServerWebViewClient(WebViewLocalServer webViewLocalServer) {
        mWebViewLocalServer = webViewLocalServer;
    }

    @Override
    public final WebResourceResponse shouldInterceptRequest(WebView view, String url) {
        return mWebViewLocalServer.shouldInterceptRequest(url);
    }

    @Override
    public final WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
        return mWebViewLocalServer.shouldInterceptRequest(request);
    }

}
