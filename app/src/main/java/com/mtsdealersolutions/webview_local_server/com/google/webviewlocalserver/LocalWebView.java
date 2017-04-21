package com.mtsdealersolutions.webview_local_server.com.google.webviewlocalserver;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Bitmap;
import android.net.http.SslError;
import android.os.Build;
import android.os.Message;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.webkit.ClientCertRequest;
import android.webkit.HttpAuthHandler;
import android.webkit.SslErrorHandler;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.mtsdealersolutions.webview_local_server.com.google.webviewlocalserver.WebViewLocalServer;

/**
 * Created by bradley.thome on 4/20/17.
 */

public class LocalWebView extends WebView {

    private WebViewLocalServer mWebViewLocalServer;

    private WebViewClient mWebViewClient;

    public LocalWebView(Context context) {
        this(context, null);
    }

    public LocalWebView(Context context, AttributeSet attrs) {
        this(context, attrs, -1);
    }

    public LocalWebView(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, -1);
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
        // Set true for enable JavaScript feature or Set False to Disable JavaScript.
        getSettings().setJavaScriptEnabled(true);
        getSettings().setJavaScriptCanOpenWindowsAutomatically(true);
        getSettings().setDomStorageEnabled(true);
        getSettings().setAppCacheEnabled(true);
        getSettings().setUseWideViewPort(true);
        getSettings().setLoadWithOverviewMode(true);
        getSettings().setBuiltInZoomControls(false);
        getSettings().setSupportZoom(false);

        super.setWebViewClient(new CustomWebViewClient());

        mWebViewLocalServer = new WebViewLocalServer(getContext());
    }

    public WebViewLocalServer getWebViewLocalServer() {
        return mWebViewLocalServer;
    }

    public void setWebViewLocalServer(WebViewLocalServer webViewLocalServer) {
        mWebViewLocalServer = webViewLocalServer;
    }

    @Override
    public void setWebViewClient(WebViewClient webViewClient) {
        mWebViewClient = webViewClient;
    }

    private class CustomWebViewClient extends WebViewClient {

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            if (mWebViewClient != null) {
                return mWebViewClient.shouldOverrideUrlLoading(view, url);
            }
            return super.shouldOverrideUrlLoading(view, url);
        }

        @TargetApi(24)
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
            if (mWebViewClient != null) {
                return mWebViewClient.shouldOverrideUrlLoading(view, request);
            }
            return super.shouldOverrideUrlLoading(view, request);
        }

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            if (mWebViewClient != null) {
                mWebViewClient.onPageStarted(view, url, favicon);
                return;
            }
            super.onPageStarted(view, url, favicon);
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            if (mWebViewClient != null) {
                mWebViewClient.onPageFinished(view, url);
                return;
            }
            super.onPageFinished(view, url);
        }

        @Override
        public void onLoadResource(WebView view, String url) {
            if (mWebViewClient != null) {
                mWebViewClient.onLoadResource(view, url);

                return;
            }
            super.onLoadResource(view, url);
        }

        @TargetApi(23)
        @Override
        public void onPageCommitVisible(WebView view, String url) {
            if (mWebViewClient != null) {
                mWebViewClient.onPageCommitVisible(view, url);

                return;
            }
            super.onPageCommitVisible(view, url);
        }

        @Override
        public WebResourceResponse shouldInterceptRequest(WebView view, String url) {
            return mWebViewLocalServer.shouldInterceptRequest(url);
        }

        @Override
        public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
            return mWebViewLocalServer.shouldInterceptRequest(request);
        }

        @Override
        public void onTooManyRedirects(WebView view, Message cancelMsg, Message continueMsg) {
            if (mWebViewClient != null) {
                mWebViewClient.onTooManyRedirects(view, cancelMsg, continueMsg);
                return;
            }
            super.onTooManyRedirects(view, cancelMsg, continueMsg);
        }

        @Override
        public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
            if (mWebViewClient != null) {
                mWebViewClient.onReceivedError(view, errorCode, description, failingUrl);

                return;
            }
            super.onReceivedError(view, errorCode, description, failingUrl);
        }

        @TargetApi(23)
        @Override
        public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
            if (mWebViewClient != null) {
                mWebViewClient.onReceivedError(view, request, error);

                return;
            }
            super.onReceivedError(view, request, error);
        }

        @TargetApi(23)
        @Override
        public void onReceivedHttpError(WebView view, WebResourceRequest request, WebResourceResponse errorResponse) {
            if (mWebViewClient != null) {
                mWebViewClient.onReceivedHttpError(view, request, errorResponse);

                return;
            }
            super.onReceivedHttpError(view, request, errorResponse);
        }

        @Override
        public void onFormResubmission(WebView view, Message dontResend, Message resend) {
            if (mWebViewClient != null) {
                mWebViewClient.onFormResubmission(view, dontResend, resend);

                return;
            }
            super.onFormResubmission(view, dontResend, resend);
        }

        @Override
        public void doUpdateVisitedHistory(WebView view, String url, boolean isReload) {
            if (mWebViewClient != null) {
                mWebViewClient.doUpdateVisitedHistory(view, url, isReload);

                return;
            }
            super.doUpdateVisitedHistory(view, url, isReload);
        }

        @Override
        public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
            if (mWebViewClient != null) {
                mWebViewClient.onReceivedSslError(view, handler, error);

                return;
            }
            super.onReceivedSslError(view, handler, error);
        }

        @TargetApi(21)
        @Override
        public void onReceivedClientCertRequest(WebView view, ClientCertRequest request) {
            if (mWebViewClient != null) {
                mWebViewClient.onReceivedClientCertRequest(view, request);

                return;
            }
            super.onReceivedClientCertRequest(view, request);
        }

        @Override
        public void onReceivedHttpAuthRequest(WebView view, HttpAuthHandler handler, String host, String realm) {
            if (mWebViewClient != null) {
                mWebViewClient.onReceivedHttpAuthRequest(view, handler, host, realm);

                return;
            }
            super.onReceivedHttpAuthRequest(view, handler, host, realm);
        }

        @Override
        public boolean shouldOverrideKeyEvent(WebView view, KeyEvent event) {
            if (mWebViewClient != null) {
                return mWebViewClient.shouldOverrideKeyEvent(view, event);

            }
            return super.shouldOverrideKeyEvent(view, event);
        }

        @Override
        public void onUnhandledKeyEvent(WebView view, KeyEvent event) {
            if (mWebViewClient != null) {
                mWebViewClient.onUnhandledKeyEvent(view, event);

                return;
            }
            super.onUnhandledKeyEvent(view, event);
        }

        @Override
        public void onScaleChanged(WebView view, float oldScale, float newScale) {
            if (mWebViewClient != null) {
                mWebViewClient.onScaleChanged(view, oldScale, newScale);

                return;
            }
            super.onScaleChanged(view, oldScale, newScale);
        }

        @TargetApi(12)
        @Override
        public void onReceivedLoginRequest(WebView view, String realm, String account, String args) {
            if (mWebViewClient != null) {
                mWebViewClient.onReceivedLoginRequest(view, realm, account, args);

                return;
            }
            super.onReceivedLoginRequest(view, realm, account, args);
        }

        @Override
        public int hashCode() {
            if (mWebViewClient != null) {
                return mWebViewClient.hashCode();

            }
            return super.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (mWebViewClient != null) {
                return mWebViewClient.equals(obj);

            }
            return super.equals(obj);
        }

        @Override
        public String toString() {
            if (mWebViewClient != null) {
                return mWebViewClient.toString();

            }
            return super.toString();
        }

    }


}
