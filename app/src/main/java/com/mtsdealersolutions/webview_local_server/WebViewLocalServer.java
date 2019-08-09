/*
Copyright 2015 Google Inc. All rights reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
 */
package com.mtsdealersolutions.webview_local_server;

import android.annotation.TargetApi;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;
import android.webkit.MimeTypeMap;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;

import com.mtsdealersolutions.webview_local_server.android.UriMatcher;
import com.mtsdealersolutions.webview_local_server.chromium.AndroidProtocolHandler;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Helper class meant to be used with the android.webkit.WebView class to enable hosting assets,
 * resources and other data on 'virtual' http(s):// URL.
 * Hosting assets and resources on http(s):// URLs is desirable as it is compatible with the
 * Same-Origin policy.
 * <p>
 * This class is intended to be used from within the
 * {@link android.webkit.WebViewClient#shouldInterceptRequest(android.webkit.WebView, String)} and
 * {@link android.webkit.WebViewClient#shouldInterceptRequest(android.webkit.WebView,
 * WebResourceRequest)}
 * methods.
 * <pre>
 *     WebViewLocalServer localServer = new WebViewLocalServer(this);
 *     // For security WebViewLocalServer uses a unique subdomain by default.
 *     AssetHostingDetails ahd = localServer.hostAssets("/www");
 *     webView.setWebViewClient(new WebViewClient() {
 *         Override
 *         public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
 *             return localServer.shouldInterceptRequest(request);
 *         }
 *     });
 *     // If your application's assets are in the "main/assets" folder this will read the file
 *     // from "main/assets/www/index.html" and load it as if it were hosted on:
 *     // https://{uuid}.androidplatform.net/assets/index.html
 *     webview.loadUrl(ahd.getHttpsPrefix().buildUpon().appendPath("index.html").build().toString());
 *
 * </pre>
 */
public class WebViewLocalServer {
    private static String TAG = "WebViewAssetServer";
    /**
     * The androidplatform.net domain currently belongs to Google and has been reserved for the
     * purpose of Android applications intercepting navigations/requests directed there.
     */
    public final static String DEFAULT_DOMAIN = "androidplatform.net";

    private final UriMatcher uriMatcher;
    private final AndroidProtocolHandler protocolHandler;

    /**
     * A handler that produces responses for paths on the virtual asset server.
     * <p>
     * Methods of this handler will be invoked on a background thread and care must be taken to
     * correctly synchronize access to any shared state.
     * <p>
     * On Android KitKat and above these methods may be called on more than one thread. This thread
     * may be different than the thread on which the shouldInterceptRequest method was invoke.
     * This means that on Android KitKat and above it is possible to block in this method without
     * blocking other resources from loading. The number of threads used to parallelize loading
     * is an internal implementation detail of the WebView and may change between updates which
     * means that the amount of time spend blocking in this method should be kept to an absolute
     * minimum.
     */
    public abstract static class PathHandler {
        private String mimeType;
        private String encoding;
        private String charset;
        private int statusCode;
        private String reasonPhrase;
        private Map<String, String> responseHeaders;

        public PathHandler() {
            this(null, null, null, 200, "OK", null);
        }

        public PathHandler(String mimeType, String encoding, String charset, int statusCode,
                           String reasonPhrase, Map<String, String> responseHeaders) {
            this.mimeType = mimeType;
            this.encoding = encoding;
            this.charset = charset;
            this.statusCode = statusCode;
            this.reasonPhrase = reasonPhrase;
            this.responseHeaders = responseHeaders;
        }

        @TargetApi(21)
        public InputStream handle(WebResourceRequest request) {
            return handle(request.getUrl());
        }

        abstract public InputStream handle(Uri url);

        public String getMimeType() {
            return mimeType;
        }

        public String getEncoding() {
            return encoding;
        }

        public String getCharset() {
            return charset;
        }

        public int getStatusCode() {
            return statusCode;
        }

        public String getReasonPhrase() {
            return reasonPhrase;
        }

        public Map<String, String> getResponseHeaders() {
            return responseHeaders;
        }
    }

    /**
     * Information about the URLs used to host the assets in the WebView.
     */
    public static class Server {
        private Uri.Builder mBuilder;

        /*package*/ Server(Uri.Builder builder) {
            mBuilder = builder;
        }

        /**
         * Gets the http: scheme prefix at which assets are hosted.
         *
         * @param appendToPath add file path to Server Uri
         * @param protocol     the protocol to use for this Uri
         * @return the http: scheme prefix at which assets are hosted. Can return null.
         */
        public Uri getServerUri(UrlProtocol protocol, String appendToPath) {
            Uri.Builder builder = mBuilder.scheme(protocol.getProtocol());
            if (!TextUtils.isEmpty(appendToPath)) mBuilder.appendPath(appendToPath);
            return mBuilder.build();
        }


        /**
         * Gets the http: scheme prefix at which assets are hosted.
         *
         * @param protocol the protocol to use for this Uri
         * @return the http: scheme prefix at which assets are hosted. Can return null.
         */
        public Uri getServerUri(UrlProtocol protocol) {
            return getServerUri(protocol, null);
        }

    }

    /*package*/ WebViewLocalServer(AndroidProtocolHandler protocolHandler) {
        uriMatcher = new UriMatcher(null);
        this.protocolHandler = protocolHandler;
    }

    /**
     * Creates a new instance of the WebView local server.
     *
     * @param context context used to resolve resources/assets/
     */
    public WebViewLocalServer(Context context) {
        // We only need the context to resolve assets and resources so the ApplicationContext is
        // sufficient while holding on to an Activity context could cause leaks.
        this(new AndroidProtocolHandler(context.getApplicationContext()));
    }

    private static Uri parseAndVerifyUrl(String url) {
        if (url == null) {
            return null;
        }
        Uri uri = Uri.parse(url);
        if (uri == null) {
            Log.e(TAG, "Malformed URL: " + url);
            return null;
        }
        String path = uri.getPath();
        if (path == null || path.length() == 0) {
            Log.e(TAG, "URL does not have a path: " + url);
            return null;
        }
        return uri;
    }

    //get mime type by url
    public String getMimeType(String url) {
        String type = null;
        String extension = MimeTypeMap.getFileExtensionFromUrl(url);
        if (extension != null) {
            if (extension.equals("js")) {
                return "text/javascript";
            } else if (extension.equals("woff")) {
                return "application/font-woff";
            } else if (extension.equals("woff2")) {
                return "application/font-woff2";
            } else if (extension.equals("ttf")) {
                return "application/x-font-ttf";
            } else if (extension.equals("eot")) {
                return "application/vnd.ms-fontobject";
            } else if (extension.equals("svg")) {
                return "image/svg+xml";
            }
            type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
        }
        return type;
    }




    /**
     * Attempt to retrieve the WebResourceResponse associated with the given <code>request</code>.
     * This method should be invoked from within
     * {@link android.webkit.WebViewClient#shouldInterceptRequest(android.webkit.WebView,
     * WebResourceRequest)}.
     *
     * @param request the request to process.
     * @return a response if the request URL had a matching handler, null if no handler was found.
     */
    @TargetApi(Build.VERSION_CODES.N)
    public WebResourceResponse shouldInterceptRequest(WebResourceRequest request) {
        PathHandler handler;
        synchronized (uriMatcher) {
            handler = (PathHandler) uriMatcher.match(request.getUrl());
        }
        if (handler == null) {
            return null;
        }

        return new WebResourceResponse(handler.getMimeType() == null ? getMimeType(request.getUrl().toString()) : handler.getMimeType(), handler.getEncoding() == null ? "UTF-8" : handler.getEncoding(),
                handler.getStatusCode() <= 0 ? 200 : handler.getStatusCode(), handler.getReasonPhrase() == null ? "" : handler.getReasonPhrase(), handler.getResponseHeaders() == null ? new HashMap<String, String>() : handler.getResponseHeaders(),
                new LollipopLazyInputStream(handler, request));
    }

    /**
     * Attempt to retrieve the WebResourceResponse associated with the given <code>url</code>.
     * This method should be invoked from within
     * {@link android.webkit.WebViewClient#shouldInterceptRequest(android.webkit.WebView, String)}.
     *
     * @param url the url to process.
     * @return a response if the request URL had a matching handler, null if no handler was found.
     */
    public WebResourceResponse shouldInterceptRequest(String url) {
        PathHandler handler = null;
        Uri uri = parseAndVerifyUrl(url);
        if (uri != null) {
            synchronized (uriMatcher) {
                handler = (PathHandler) uriMatcher.match(uri);
            }
        }
        if (handler == null)
            return null;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            return new WebResourceResponse(handler.getMimeType() == null ? getMimeType(url) : handler.getMimeType(), handler.getEncoding() == null ? "UTF-8" : handler.getEncoding(),
                    new LegacyLazyInputStream(handler, uri));
        } else {
            InputStream is = handler.handle(uri);
            return new WebResourceResponse(handler.getMimeType() == null ? getMimeType(url) : handler.getMimeType(), handler.getEncoding() == null ? "UTF-8" : handler.getEncoding(),
                    is);
        }
    }

    /**
     * Registers a handler for the given <code>uri</code>. The <code>handler</code> will be invoked
     * every time the <code>shouldInterceptRequest</code> method of the instance is called with
     * a matching <code>uri</code>.
     *
     * @param uri     the uri to use the handler for. The scheme and authority (domain) will be matched
     *                exactly. The path may contain a '*' element which will match a single element of
     *                a path (so a handler registered for /a/* will be invoked for /a/b and /a/c.html
     *                but not for /a/b/b) or the '**' element which will match any number of path
     *                elements.
     * @param handler the handler to use for the uri.
     */
    void register(Uri uri, PathHandler handler) {
        synchronized (uriMatcher) {
            uriMatcher.addURI(uri.getScheme(), uri.getAuthority(), uri.getPath(), handler);
        }
    }

    /**
     * Hosts the application's assets on an http(s):// URL. Assets from the local path
     * <code>assetPath/...</code> will be available under
     * <code>http(s)://{domain}/{virtualAssetPath}/...</code>.
     *
     * @param builder Import the Builder
     * @return prefixes under which the assets are hosted.
     */
    public Server createHost(final Builder builder) {

        if (builder == null)
            throw new IllegalArgumentException("assets Builder cannot be null");

        String domain = builder.getDomain();
        if (!TextUtils.isEmpty(builder.getSubDomain()))
            domain = builder.getSubDomain() + "." + domain;

        Uri.Builder uriBuilder = new Uri.Builder();
        uriBuilder.scheme(UrlProtocol.HTTPS.getProtocol());
        String encodedAuthority = domain;
        try{
           encodedAuthority = URLEncoder.encode(domain, "UTF-8");
        }catch (Exception e){

        }
        if (builder.mPort > -1) encodedAuthority += ":" + String.valueOf(builder.mPort);
        uriBuilder.encodedAuthority(encodedAuthority);
        uriBuilder.path(builder.getUrlVirtualPath());

        if (builder instanceof AssetsBuilder && ((AssetsBuilder) builder).getPathInAndroidLocation().indexOf('*') != -1) {
            throw new IllegalArgumentException("assetPath cannot contain the '*' character.");
        }
        if (builder.getUrlVirtualPath().indexOf('*') != -1) {
            throw new IllegalArgumentException(
                    "virtualAssetPath cannot contain the '*' character.");
        }

        PathHandler handler = new PathHandler() {
            @Override
            public InputStream handle(Uri url) {
                InputStream stream;

                try {
                    if (builder instanceof AssetsBuilder) {

                        String path = url.getPath();
                        if (path.startsWith("/")) path = path.replaceFirst("/", "");

                        if (path.startsWith(builder.getUrlVirtualPath()))
                            path = path.replaceFirst(builder.getUrlVirtualPath(), "");

                        path = ((AssetsBuilder) builder).getPathInAndroidLocation() + (path.startsWith("/") ? "" : "/") + path;

                        stream = protocolHandler.openAsset(path);
                    } else {
                        stream = protocolHandler.openResource(url);
                    }

                } catch (IOException e) {
                    Log.e(TAG, "Unable to open asset URL: " + url);
                    return null;
                }


                return stream;
            }
        };

        for (Map.Entry<UrlProtocol, Boolean> mapEntry : builder.getIsAllowed().entrySet()) {
            if (mapEntry.getValue())
                register(Uri.withAppendedPath(uriBuilder.scheme(mapEntry.getKey().getProtocol()).build(), "**"), handler);
        }

        return new Server(uriBuilder);
    }

    /**
     * The KitKat WebView reads the InputStream on a separate threadpool. We can use that to
     * parallelize loading.
     */
    private static abstract class LazyInputStream extends InputStream {
        protected final PathHandler handler;
        private InputStream is = null;

        public LazyInputStream(PathHandler handler) {
            this.handler = handler;
        }

        private InputStream getInputStream() {
            if (is == null) {
                is = handle();
            }
            return is;
        }

        protected abstract InputStream handle();

        @Override
        public int available() throws IOException {
            InputStream is = getInputStream();
            return (is != null) ? is.available() : 0;
        }

        @Override
        public int read() throws IOException {
            InputStream is = getInputStream();
            return (is != null) ? is.read() : -1;
        }

        @Override
        public int read(byte b[]) throws IOException {
            InputStream is = getInputStream();
            return (is != null) ? is.read(b) : -1;
        }

        @Override
        public int read(byte b[], int off, int len) throws IOException {
            InputStream is = getInputStream();
            return (is != null) ? is.read(b, off, len) : -1;
        }

        @Override
        public long skip(long n) throws IOException {
            InputStream is = getInputStream();
            return (is != null) ? is.skip(n) : 0;
        }
    }

    // For earlier than L.
    private static class LegacyLazyInputStream extends LazyInputStream {
        private Uri uri;
        private InputStream is;

        public LegacyLazyInputStream(PathHandler handler, Uri uri) {
            super(handler);
            this.uri = uri;
        }

        @Override
        protected InputStream handle() {
            return handler.handle(uri);
        }
    }

    // For L and above.
    private static class LollipopLazyInputStream extends LazyInputStream {
        private WebResourceRequest request;
        private InputStream is;

        public LollipopLazyInputStream(PathHandler handler, WebResourceRequest request) {
            super(handler);
            this.request = request;
        }

        @Override
        protected InputStream handle() {
            return handler.handle(request);
        }
    }

    /**
     * The Available URL Protocols that can be used for hosting
     */
    public enum UrlProtocol {
        HTTP("http"), HTTPS("https"), FILE("file");

        private String mProtocol;

        /**
         * The Available URL Protocols that can be used for hosting
         */
        UrlProtocol(String protocol) {
            mProtocol = protocol;
        }

        /**
         * The URL Protocol that can be used for hosting
         *
         * @return protocol string
         */
        public String getProtocol() {
            return mProtocol;
        }
    }

    /**
     * Hosts the application's assets on an http(s):// URL. Assets from the local path
     * <code>assetPath/...</code> will be available under
     * <code>http(s)://{domain}/{virtualAssetPath}/...</code>.
     * <p>
     * prefixes under which the assets are hosted.
     */
    public static class AssetsBuilder extends ResBuilder {

        public static final String DEFAULT_ASSETS_PATH = "www";

        private String mPathInAndroidLocation = DEFAULT_ASSETS_PATH;

        /**
         * Hosts the application's assets on an http(s):// URL. Assets from the local path
         * <code>assetPath/...</code> will be available under
         * <code>http(s)://{domain}/{virtualAssetPath}/...</code>.
         * <p>
         * prefixes under which the assets are hosted.
         */
        public AssetsBuilder() {
            super();
        }

        /**
         * Hosts the application's assets on an http(s):// URL. Assets from the local path
         * <code>assetPath/...</code> will be available under
         * <code>http(s)://{domain}/{virtualAssetPath}/...</code>.
         *
         * @param pathInAndroidLocation the path within the asset folder which the assets are located (example, "www" for "ASSETS_FOLDER/www/index.html")
         * @return prefixes under which the assets are hosted.
         */
        public AssetsBuilder setPathInAndroidLocation(String pathInAndroidLocation) {
            if (!TextUtils.isEmpty(pathInAndroidLocation))
                while (pathInAndroidLocation.startsWith("/")) {
                    pathInAndroidLocation = pathInAndroidLocation.substring(1, pathInAndroidLocation.length());
                }
            if (!TextUtils.isEmpty(pathInAndroidLocation))
                while (pathInAndroidLocation.endsWith("/")) {
                    pathInAndroidLocation = pathInAndroidLocation.substring(0, pathInAndroidLocation.length() - 1);
                }
            mPathInAndroidLocation = pathInAndroidLocation;
            return this;
        }

        public String getPathInAndroidLocation() {
            return mPathInAndroidLocation == null ? "" : mPathInAndroidLocation;
        }

        @Override
        public AssetsBuilder setDomain(String domain) {
            return (AssetsBuilder) super.setDomain(domain);
        }

        @Override
        public AssetsBuilder setUrlVirtualPath(String urlVirtualPath) {
            return (AssetsBuilder) super.setUrlVirtualPath(urlVirtualPath);
        }

        @Override
        public String getDomain() {
            return super.getDomain();
        }

        @Override
        public AssetsBuilder clearDomain() {
            return (AssetsBuilder) super.clearDomain();
        }

        @Override
        public String getUrlVirtualPath() {
            return super.getUrlVirtualPath();
        }

        @Override
        public String getSubDomain() {
            return super.getSubDomain();
        }

        @Override
        public AssetsBuilder setProtocol(UrlProtocol urlProtocol, boolean isAllowed) {
            return (AssetsBuilder) super.setProtocol(urlProtocol, isAllowed);
        }

        @Override
        public AssetsBuilder setSubDomain(String subDomain) {
            return (AssetsBuilder) super.setSubDomain(subDomain);
        }

        @Override
        public AssetsBuilder setRandomSubDomain() {
            return (AssetsBuilder) super.setRandomSubDomain();
        }

        @Override
        public AssetsBuilder clearSubDomain() {
            return (AssetsBuilder) super.clearSubDomain();
        }

    }


    /**
     * Hosts the application's assets on an http(s):// URL. Assets from the local path
     * <code>assetPath/...</code> will be available under
     * <code>http(s)://{domain}/{virtualAssetPath}/...</code>.
     * <p>
     * return prefixes under which the assets are hosted.
     */
    public static class ResBuilder extends Builder {
        public ResBuilder() {
            super();
        }

        @Override
        public ResBuilder setDomain(String domain) {
            return (ResBuilder) super.setDomain(domain);
        }

        @Override
        public ResBuilder setUrlVirtualPath(String urlVirtualPath) {
            return (ResBuilder) super.setUrlVirtualPath(urlVirtualPath);
        }

        @Override
        public String getDomain() {
            return super.getDomain();
        }

        @Override
        public String getUrlVirtualPath() {
            return super.getUrlVirtualPath();
        }

        @Override
        public String getSubDomain() {
            return super.getSubDomain();
        }

        @Override
        public ResBuilder clearDomain() {
            return (ResBuilder) super.clearDomain();
        }

        @Override
        public ResBuilder setProtocol(UrlProtocol urlProtocol, boolean isAllowed) {
            return (ResBuilder) super.setProtocol(urlProtocol, isAllowed);
        }

        @Override
        public ResBuilder setSubDomain(String subDomain) {
            return (ResBuilder) super.setSubDomain(subDomain);
        }

        @Override
        public ResBuilder setRandomSubDomain() {
            return (ResBuilder) super.setRandomSubDomain();
        }

        @Override
        public ResBuilder clearSubDomain() {
            return (ResBuilder) super.clearSubDomain();
        }
    }


    /**
     * Hosts the application's assets on an http(s):// URL. Assets from the local path
     * <code>assetPath/...</code> will be available under
     * <code>http(s)://{domain}/{virtualAssetPath}/...</code>.
     * <p>
     * return prefixes under which the assets are hosted.
     */
    public static class Builder {
        private String mDomain = DEFAULT_DOMAIN;
        private String mSubDomain;
        private String mUrlVirtualPath = "";
        private Map<UrlProtocol, Boolean> mIsAllowed;
        private int mPort = -1;

        /**
         * Hosts the application's assets on an http(s):// URL. Assets from the local path
         * <code>assetPath/...</code> will be available under
         * <code>http(s)://{domain}/{virtualAssetPath}/...</code>.
         * <p>
         * return prefixes under which the assets are hosted.
         */
        private Builder() {
            mIsAllowed = new HashMap<>();
            for (UrlProtocol urlProtocol : UrlProtocol.values()) {
                mIsAllowed.put(urlProtocol, true);
            }
            setRandomSubDomain();
        }

        /**
         * Hosts the application's assets on an http(s):// URL. Assets from the local path
         * <code>assetPath/...</code> will be available under
         * <code>http(s)://{domain}/{virtualAssetPath}/...</code>.
         *
         * @param domain custom domain on which the assets should be hosted (for example "example.com").
         * @return prefixes under which the assets are hosted.
         */
        public Builder setDomain(String domain) {
            if (!TextUtils.isEmpty(domain)) while (domain.startsWith("/")) {
                domain = domain.substring(1, domain.length());
            }
            if (!TextUtils.isEmpty(domain)) while (domain.endsWith("/")) {
                domain = domain.substring(0, domain.length() - 1);
            }
            mDomain = domain;
            return this;
        }

        /**
         * Hosts the application's assets on an http(s):// URL. Assets from the local path
         * <code>assetPath/...</code> will be available under
         * <code>http(s)://{domain}/{virtualAssetPath}/...</code>.
         *
         * @param urlVirtualPath the path on the local server under which the assets should be hosted.
         * @return prefixes under which the assets are hosted.
         */
        public Builder setUrlVirtualPath(String urlVirtualPath) {
            if (!TextUtils.isEmpty(urlVirtualPath)) while (urlVirtualPath.startsWith("/")) {
                urlVirtualPath = urlVirtualPath.substring(1, urlVirtualPath.length());
            }
            if (!TextUtils.isEmpty(urlVirtualPath)) while (urlVirtualPath.endsWith("/")) {
                urlVirtualPath = urlVirtualPath.substring(0, urlVirtualPath.length() - 1);
            }
            mUrlVirtualPath = urlVirtualPath;
            return this;
        }

        /**
         * Which port the host should connect to, default is to use the HTTP default port
         * <code>http(s)://{domain}<b>:{port}</b>/...</code>.
         *
         * @param port the port that the host will link to, -1 means the HTTP default port (default is -1)
         * @return prefixes under which the assets are hosted.
         */
        public Builder setPort(int port) {
            mPort = port;
            return this;
        }

        /**
         * @return the port that the host will link to, -1 means the HTTP default port (default is -1)
         */
        public int getPort() {
            return mPort;
        }

        public String getDomain() {
            return TextUtils.isEmpty(mDomain) ? DEFAULT_DOMAIN : mDomain.equals("**NONE") ? "" : mDomain;
        }

        public String getUrlVirtualPath() {
            if (TextUtils.isEmpty(mUrlVirtualPath)) return "";
            return TextUtils.isEmpty(mUrlVirtualPath) ? "" : mUrlVirtualPath;
        }

        public String getSubDomain() {
            return mSubDomain;
        }

        /**
         * clear the domain.  This is useful particularity for FILE protocol, (example file:///android_asset/www)
         *
         * @return this builder
         */
        public Builder clearDomain() {
            mDomain = "**NONE";
            return this;
        }

        /**
         * Set the Protocols to be included or excluded from the local host, (example to include/exclude HTTP, HTTPS)
         * <p>
         * By default all are included
         *
         * @param urlProtocol the protocol in question
         * @param isAllowed   whether it is allowed or not
         * @return prefixes under which the assets are hosted.
         */
        public Builder setProtocol(UrlProtocol urlProtocol, boolean isAllowed) {
            mIsAllowed.put(urlProtocol, isAllowed);
            return this;
        }

        /**
         * Hosts the application's assets on an http(s):// URL. Assets from the local path
         * <code>assetPath/...</code> will be available under
         * <code>http(s)://{domain}/{virtualAssetPath}/...</code>.
         *
         * @param subDomain custom subDomain on which the assets should be hosted (for example "{CUSTOM}.example.com").
         * @return prefixes under which the assets are hosted.
         */
        public Builder setSubDomain(String subDomain) {
            mSubDomain = subDomain;
            return this;
        }

        /**
         * create a random subDomain on which the assets should be hosted, great for increased security (for example "{RANDOM-UUID-SUBDOMAIN}.example.com").
         *
         * @return this builder
         */
        public Builder setRandomSubDomain() {
            mSubDomain = UUID.randomUUID().toString();
            return this;
        }

        /**
         * clear the subdomain, by default the subdomain is "{RANDOM-UUID-SUBDOMAIN}.example.com"
         *
         * @return this builder
         */
        public Builder clearSubDomain() {
            mSubDomain = null;
            return this;
        }

        public Map<UrlProtocol, Boolean> getIsAllowed() {
            return mIsAllowed;
        }
    }
}

