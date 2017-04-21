# Android WebView Local Web Server

Run the HTTP, HTTPS, and FILE protocols on your Android device to be viewed in a WebView

## Gradle
```groovy
compile 'com.mtsdealersolutions:webview_local_server:1.0.2'
```

## Implementation

#### Optional WebView...

	<com.mtsdealersolutions.webview_local_server.com.google.webviewlocalserver.LocalWebView
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:id="@+id/local_web_view"/>

#
        
    LocalWebView localwebview = (LocalWebView)findViewById(R.id.local_web_view);
    mWebViewLocalServer = webview.getWebViewLocalServer();
    
#### ...Or Just Implement WebViewLocalServer

    mWebViewLocalServer = new WebViewLocalServer(context);
    
#
    
    class MyWebViewClient extends WebViewClient {
            // For KitKat and earlier.
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, String url) {
                return mWebViewLocalServer.shouldInterceptRequest(url);
            }
            // For Lollipop and above.
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                return mWebViewLocalServer.shouldInterceptRequest(request);
            }
        }
        
#

     webView.setWebViewClient(new MyWebViewClient);
     
## Usage

#### 1. Tell the server where to host the resources.
        
        //User AssetsBuilder for assets or ResBuilder for res
     WebViewLocalServer.AssetsBuilder assetsBuilder = new WebViewLocalServer.AssetsBuilder()
               
                // The default domain is androidplatform.net, which is an unused domain
                .setDomain("androidTest.com")
                
                // www is the default path, ie 'ASSETS_FOLDER/www'"
                .setPathInAndroidLocation("www")
                
                // generate a random subdomain, this is the default.  You can also clear the subdomain by setSubDomain("")"      
                .setRandomSubDomain()
                
                // HTTP, HTTPS, and FILE are enabled by default
                .setProtocol(WebViewLocalServer.UrlProtocol.HTTP, false)
                
                // no Url virtual path is set by default
                .setUrlVirtualPath("testpath");
                
This will create a server that pulls assets from "ASSETS_FOLDER/www" for the server "https://{RANDOM_UUID}.androidTest.com/testpath"

#### 2. Now implement it

    //Get the server instance
     WebViewLocalServer.Server server = webViewLocalServer.createHostFromAssets(assetsBuilder);
     
     //Get the uri for the server path, in this example this now is "https://{RANDOM_UUID}.androidTest.com/testpath/index.html"
     Uri uri = server.getServerUri(WebViewLocalServer.UrlProtocol.HTTPS, "index.html");

    //Convert to string and load into webview
     webview.loadUrl(uri.toString());


#### 3. Consider using the following settings in order to maximize security:

        webView.getSettings().setAllowFileAccessFromFileURLs(false);
        webView.getSettings().setAllowUniversalAccessFromFileURLs(false);

        // Keeping these off is less critical but still a good idea, especially
        // if your app is not using file:// or content:// URLs.
        webView.getSettings().setAllowFileAccess(false);
        webView.getSettings().setAllowContentAccess(false);
        
## LocalWebView

LocalWebView automatically implements the WebViewLocalServer, so use ```localwebview.getWebViewLocalServer();``` method to retrieve it.

LocalWebView also sets these webview settings by default...

    getSettings().setJavaScriptEnabled(true);
    getSettings().setJavaScriptCanOpenWindowsAutomatically(true);
    getSettings().setDomStorageEnabled(true);
    getSettings().setAppCacheEnabled(true);
    getSettings().setUseWideViewPort(true);
    getSettings().setLoadWithOverviewMode(true);
    getSettings().setBuiltInZoomControls(false);
    getSettings().setSupportZoom(false);
    
... you can still modify these settings from ```localwebview.getSettings()```

## Picking a domain.

One potential problem of hosting local resources on a http(s):// URL is that
doing so may conflict with a real website. This means that local resources
should only be hosted on domains that the user has control of or which have
been dedicated for this purpose.  
The `androidplatform.net` domain has been specifically reserved for this
purpose and you are free to use it.

By default the `WebViewLocalServer` will attempt to host assets/resources on
a random subdomain of `androidplatform.net` (something like
`123e4567-e89b-12d3-a456-426655440000.androidplatform.net`). This random
subdomain is chosen once per `WebViewLocalServer`.

Should using a random subdomain be inconvenient for some reason it is possible
to use a fixed domain (like `androidplatform.net` or a domain you own).

      
#Based off of [Google's WebView-Local-Server](https://github.com/google/webview-local-server) 
