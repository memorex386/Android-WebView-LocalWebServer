[ ![Download](https://api.bintray.com/packages/memorex386/Android_WebView_LocalWebServer/webview_local_server/images/download.svg) ](https://bintray.com/memorex386/Android_WebView_LocalWebServer/webview_local_server/_latestVersion)

# Android WebView Local Web Server

## Overview

Create a local web server to run local html files into a webview and view them like a website.

 You create a virtual web server that runs on your android device, and when you navigate to the server url in the webview it will load it from local assets.  Run HTTP, HTTPS, and FILE protocols on your Android device to be viewed in a WebView. 

All other websites will load directly from the web.

It is recommended that you store all website data under '__{ASSETS_FOLDER}__/www/__{HTML_FILE}__', although you can store it under the res folder as well.  You can load the files anywhere in the assets folder still and just change the ```  .setPathInAndroidLocation("myfiles/secondaryfolder")``` to reference the specific folder (or ```  .setPathInAndroidLocation("")``` if you load the files directly into the assets folder.

### Why?
The purpose of this library is to enable hosting local content (such as assets
or resources) under an http(s):// URL.  
The traditional way to access local resources is to use `file:///android_asset`
or `file://android_res/` URLs but using the `file:` scheme poses problems with 
[the Same-Origin policy](http://en.wikipedia.org/wiki/Same-origin_policy) and
makes it problematic to reference local content from content loaded over
a secure (`https:`) connection.

## Gradle
    repositories
    {
    ...
    maven {
       url  'https://dl.bintray.com/memorex386/Android_WebView_LocalWebServer'
    }
    ...
    }


    dependencies
    {
    ...
    compile 'com.mtsdealersolutions:webview_local_server:1.0.15'
    ...
    }

## Quick Implementation

Quick implementation will get you up and running!
        
    LocalWebView localWebView = (LocalWebView)findViewById(R.id.local_web_view);
    
    WebViewLocalServer webViewLocalServer = localWebView.getWebViewLocalServer();
    
    WebViewLocalServer.AssetsBuilder assetsBuilder = new WebViewLocalServer.AssetsBuilder();
    
    WebViewLocalServer.Server server = webViewLocalServer.createHost(assetsBuilder);
    
    Uri uri = server.getServerUri(WebViewLocalServer.UrlProtocol.HTTPS, "index.html");

    localWebView.loadUrl(uri.toString());  
        
####

This will get '__{ASSETS_FOLDER}__/www/index.html' and host it it as 'https://{RANDOM_UUID}.androidPlatform.net/index.html'.

## Recommended Implementation

#### Optional WebView...

	<com.mtsdealersolutions.webview_local_server.LocalWebView
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:id="@+id/local_web_view"/>

####
        
    mLocalWebView = (LocalWebView)findViewById(R.id.local_web_view);
    mWebViewLocalServer = mLocalWebView.getWebViewLocalServer();
    
#### ...Or Just Implement WebViewLocalServer

    mWebViewLocalServer = new WebViewLocalServer(context);
    
####
    
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
        
####

     mLocalWebView.setWebViewClient(new MyWebViewClient);
     
## Usage

#### 1. Tell the server where to host the resources.
        
        //User AssetsBuilder for assets or ResBuilder for res
     WebViewLocalServer.AssetsBuilder assetsBuilder = new WebViewLocalServer.AssetsBuilder()
               
                // The default domain is androidplatform.net, which is an unused domain
                .setDomain("androidTest.com")
                
                // www is the default path, ie 'ASSETS_FOLDER/www'"
                .setPathInAndroidLocation("www")
                
                // generate a random subdomain, this is the default.  You can also clear the subdomain by setSubDomain("") or clearSubDomain()"
                // NOTE : Remember that a random subdomain means that cookies and history will be lost on activity/app restart
                .setRandomSubDomain()
                
                // HTTP, HTTPS, and FILE are enabled by default
                .setProtocol(WebViewLocalServer.UrlProtocol.HTTP, false)
                
                
                // no Url virtual path is set by default
                // 
                // NOTE : Remember, if you define a virtual path AND your website uses <base href>,
                // then you need to adjust <base href> to match whatever is here (example <base href="/testpath/">
                .setUrlVirtualPath("testpath");
                
This will create a server that pulls assets from '__assetsFolder__/www' for the server "https://{RANDOM_UUID}.androidTest.com/testpath"

#### 2. Now implement it

    //Get the server instance
     WebViewLocalServer.Server server = mWebViewLocalServer.createHost(assetsBuilder);
     
     // Get the uri for the server path, in this example this now is 
     // "https://{RANDOM_UUID}.androidTest.com/testpath/index.html"
     Uri uri = server.getServerUri(WebViewLocalServer.UrlProtocol.HTTPS, "index.html");

    //Convert to string and load into webview
     mLocalWebView.loadUrl(uri.toString());


#### 3. Consider using the following settings in order to maximize security:

        // Set these to false for HTTP and HTTPS is recommended, but true 
        // is required for FILE protocol
        mLocalWebView.getSettings().setAllowFileAccessFromFileURLs(false);
        mLocalWebView.getSettings().setAllowUniversalAccessFromFileURLs(false);

        // Keeping these off is less critical but still a good idea, especially
        // if your app is not using file:// or content:// URLs.
        mLocalWebView.getSettings().setAllowFileAccess(false);
        mLocalWebView.getSettings().setAllowContentAccess(false);
        
## LocalWebView

LocalWebView automatically implements the WebViewLocalServer, so use ```mLocalWebView.getWebViewLocalServer();``` method to retrieve it.

LocalWebView also sets these webview settings by default...

    // Without setting these the webview is not touchable
    setFocusable(true);
    setFocusableInTouchMode(true);
    
    // Make WebView sizing feel more like an app
    setInitialScale(0);
    setVerticalScrollBarEnabled(false);
    getSettings().setUseWideViewPort(true);
    getSettings().setLoadWithOverviewMode(true);
    getSettings().setSupportZoom(false);
    
    // Set true for enable JavaScript feature or Set False to Disable JavaScript.
    getSettings().setJavaScriptEnabled(true);
    getSettings().setLayoutAlgorithm(WebSettings.LayoutAlgorithm.NORMAL);
    getSettings().setJavaScriptCanOpenWindowsAutomatically(true);

    // Jellybean rightfully tried to lock this down. Too bad they didn't give us a whitelist
    // while we do this
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
         getSettings().setAllowUniversalAccessFromFileURLs(true);
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
        getSettings().setMediaPlaybackRequiresUserGesture(false);
    }
    
    // Enable database
    // We keep this disabled because we use or shim to get around DOM_EXCEPTION_ERROR_16
    String databasePath = getContext().getApplicationContext().getDir("database", Context.MODE_PRIVATE).getPath();
    getSettings().setDatabaseEnabled(true);
        
    // Deprecated but still used API 23 and below
    getSettings().setDatabasePath(databasePath);
    getSettings().setGeolocationDatabasePath(databasePath);

    // Enable DOM storage
    getSettings().setDomStorageEnabled(true);

    // Enable built-in geolocation
    getSettings().setGeolocationEnabled(true);

    // Enable AppCache
    getSettings().setAppCacheMaxSize(5 * 1048576);
    getSettings().setAppCachePath(databasePath);
    getSettings().setAppCacheEnabled(true);


    // Deprecated in API 18, but changing from NORMAL to HIGH for older APIs
    getSettings().setRenderPriority(WebSettings.RenderPriority.HIGH);

    // These should be set by default already, but just in case...
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
         // chromium, enable hardware acceleration
         setLayerType(View.LAYER_TYPE_HARDWARE, null);
    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
         // older android version, disable hardware acceleration
         setLayerType(View.LAYER_TYPE_SOFTWARE, null);
    }
        
    
####

... and if you load the FILE protocol url then these are forced to true...

    getSettings().setAllowFileAccessFromFileURLs(true);
    getSettings().setAllowUniversalAccessFromFileURLs(true);

    
... you can still modify these settings from ```mLocalwebview.getSettings()```

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

Remember that a random domain means that cookies and history will not be saved for the webview on activity/app restart.

Should using a random subdomain be inconvenient for some reason it is possible
to use a fixed domain (like `androidplatform.net` or a domain you own).

      
# Based off of [Google's WebView-Local-Server](https://github.com/google/webview-local-server) 
