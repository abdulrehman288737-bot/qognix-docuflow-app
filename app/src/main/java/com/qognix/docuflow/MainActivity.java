package com.qognix.docuflow;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.DownloadManager;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Base64;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.DownloadListener;
import android.webkit.JavascriptInterface;
import android.webkit.URLUtil;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

/**
 * Qognix DocuFlow - WebView shell for https://qognixdocuflow.netlify.app/
 *
 * Important behaviours:
 *  - PDF upload works: onShowFileChooser opens the Android file picker.
 *  - Downloads work: normal http(s) downloads go through DownloadManager,
 *    and the website's "download summary/quiz as .txt" buttons (which use
 *    browser "blob:" downloads) are captured by a small JavaScript bridge
 *    and saved into the phone's Downloads folder.
 *  - Same-site pages stay inside the app; other links (mailto, tel,
 *    external websites, pop-up windows such as the site's ad popunder)
 *    open in the phone's browser / matching app.
 *  - No internet -> friendly offline page with a Retry button.
 *  - ADS (user decision, 2026-11-17): ONLY banner ads, and only the
 *    website's own Adsterra banners (its 5 slots), which load inside the
 *    WebView as part of the site. There is NO AdMob / video / full-screen
 *    ad code in this app at all. The website's Popunder is blocked inside
 *    the app (see shouldInterceptRequest) and pop-up windows are disabled,
 *    so the app stays banner-only. The website itself is unchanged - web
 *    visitors still get the Popunder exactly as before.
 */
public class MainActivity extends Activity {

    public static final String SITE_URL = "https://qognixdocuflow.netlify.app/";
    private static final String SITE_HOST = "qognixdocuflow.netlify.app";
    private static final int FILE_CHOOSER_REQUEST = 1001;

    private WebView webView;
    private ProgressBar progressBar;
    private SwipeRefreshLayout refreshLayout;
    private LinearLayout errorLayout;
    private ValueCallback<Uri[]> filePathCallback;

    // ------------------------------------------------------------------
    // JavaScript bridge for "blob:" downloads (the site's .txt downloads)
    // ------------------------------------------------------------------
    public class BlobDownloader {
        @JavascriptInterface
        public void download(String base64Data, String fileName) {
            runOnUiThread(() -> saveDownloadedFile(base64Data, fileName));
        }
    }

    private void saveDownloadedFile(String base64Data, String fileName) {
        try {
            String name = (fileName == null || fileName.trim().isEmpty())
                    ? "qognix-download.txt" : fileName.trim();
            byte[] bytes = Base64.decode(base64Data, Base64.DEFAULT);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContentValues values = new ContentValues();
                values.put(MediaStore.Downloads.DISPLAY_NAME, name);
                values.put(MediaStore.Downloads.MIME_TYPE, "text/plain");
                values.put(MediaStore.Downloads.RELATIVE_PATH,
                        Environment.DIRECTORY_DOWNLOADS + "/QognixDocuFlow");
                ContentResolver resolver = getContentResolver();
                Uri uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
                if (uri == null) throw new Exception("Could not create file");
                try (OutputStream out = resolver.openOutputStream(uri)) {
                    out.write(bytes);
                }
            } else {
                File dir = new File(Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_DOWNLOADS), "QognixDocuFlow");
                if (!dir.exists()) dir.mkdirs();
                File file = new File(dir, name);
                try (FileOutputStream out = new FileOutputStream(file)) {
                    out.write(bytes);
                }
            }
            Toast.makeText(this, "Saved to Downloads: " + name, Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, "Download failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    // ------------------------------------------------------------------

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        webView = findViewById(R.id.webView);
        progressBar = findViewById(R.id.progressBar);
        refreshLayout = findViewById(R.id.swipeRefresh);
        errorLayout = findViewById(R.id.errorLayout);
        Button retryButton = findViewById(R.id.retryButton);
        retryButton.setOnClickListener(v -> {
            errorLayout.setVisibility(View.GONE);
            webView.setVisibility(View.VISIBLE);
            if (isOnline()) {
                webView.reload();
            } else {
                showOffline();
            }
        });

        refreshLayout.setColorSchemeColors(0xFF7657FF, 0xFFA78BFA);
        refreshLayout.setOnRefreshListener(() -> webView.reload());

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        settings.setLoadsImagesAutomatically(true);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);
        // Banner-only app: pop-up windows are disabled, so the website's
        // Popunder cannot open a window inside the app.
        settings.setSupportMultipleWindows(false);
        settings.setJavaScriptCanOpenWindowsAutomatically(false);
        settings.setBuiltInZoomControls(false);
        settings.setSupportZoom(false);

        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.setAcceptCookie(true);
        cookieManager.setAcceptThirdPartyCookies(webView, true);

        webView.addJavascriptInterface(new BlobDownloader(), "QognixBlobDownloader");

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                progressBar.setProgress(newProgress);
                progressBar.setVisibility(newProgress >= 100 ? View.GONE : View.VISIBLE);
                if (newProgress >= 100) refreshLayout.setRefreshing(false);
            }

            // PDF upload (and any <input type="file">) -> Android file picker
            @Override
            public boolean onShowFileChooser(WebView view,
                                             ValueCallback<Uri[]> filePathCallback,
                                             FileChooserParams fileChooserParams) {
                if (MainActivity.this.filePathCallback != null) {
                    MainActivity.this.filePathCallback.onReceiveValue(null);
                }
                MainActivity.this.filePathCallback = filePathCallback;
                try {
                    Intent intent = fileChooserParams.createIntent();
                    startActivityForResult(intent, FILE_CHOOSER_REQUEST);
                } catch (Exception e) {
                    Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                    intent.addCategory(Intent.CATEGORY_OPENABLE);
                    intent.setType("*/*");
                    try {
                        startActivityForResult(Intent.createChooser(intent, "Choose a PDF file"),
                                FILE_CHOOSER_REQUEST);
                    } catch (Exception e2) {
                        MainActivity.this.filePathCallback = null;
                        return false;
                    }
                }
                return true;
            }

            // No onCreateWindow override on purpose: with multiple windows
            // disabled above, window.open() (used by the site's Popunder)
            // does nothing inside the app. The app stays banner-only.
        });

        webView.setWebViewClient(new WebViewClient() {

            // Block ONLY the website's Popunder script host inside the app,
            // so the app shows banner ads only (user decision 2026-11-17).
            // bicea.org (the banner ad scripts) is NOT blocked, and no site
            // feature (PDF upload, downloads, Gemini, translation) is touched.
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view,
                                                              WebResourceRequest request) {
                String host = request.getUrl().getHost();
                if (host != null && (host.equals("afders.org") || host.endsWith(".afders.org"))) {
                    return new WebResourceResponse("text/plain", "utf-8",
                            new java.io.ByteArrayInputStream(new byte[0]));
                }
                return super.shouldInterceptRequest(view, request);
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String url = request.getUrl().toString();
                String host = request.getUrl().getHost();
                if (host != null && host.equals(SITE_HOST)) {
                    return false; // same site -> stay inside the app
                }
                if (url.startsWith("http://") || url.startsWith("https://")) {
                    openExternal(url); // other websites -> phone browser
                    return true;
                }
                // mailto:, tel:, sms:, intent: etc.
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
                } catch (Exception ignored) { }
                return true;
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                refreshLayout.setRefreshing(false);
                injectBlobDownloadSupport(view);
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request,
                                        WebResourceError error) {
                super.onReceivedError(view, request, error);
                if (request.isForMainFrame()) {
                    showOffline();
                }
            }
        });

        // Normal (non-blob) downloads -> Android DownloadManager
        webView.setDownloadListener((url, userAgent, contentDisposition, mimeType, contentLength) -> {
            try {
                DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
                request.setMimeType(mimeType);
                String cookies = CookieManager.getInstance().getCookie(url);
                request.addRequestHeader("cookie", cookies);
                request.addRequestHeader("User-Agent", userAgent);
                request.setDescription("Downloading file…");
                request.setTitle(URLUtil.guessFileName(url, contentDisposition, mimeType));
                request.setNotificationVisibility(
                        DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS,
                        URLUtil.guessFileName(url, contentDisposition, mimeType));
                DownloadManager dm = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
                if (dm != null) dm.enqueue(request);
                Toast.makeText(this, "Downloading…", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                openExternal(url);
            }
        });

        if (isOnline()) {
            webView.loadUrl(SITE_URL);
        } else {
            showOffline();
        }
    }

    /** Teach the page to send its blob: downloads (summary/quiz .txt) to the app. */
    private void injectBlobDownloadSupport(WebView view) {
        String js = "javascript:(function(){"
                + "if(window.__qognixBlobHook) return; window.__qognixBlobHook=true;"
                + "document.addEventListener('click', function(e){"
                + "  var a = e.target.closest ? e.target.closest('a') : null;"
                + "  if(!a || !a.href || a.href.indexOf('blob:')!==0) return;"
                + "  e.preventDefault();"
                + "  var name = a.getAttribute('download') || 'qognix-download.txt';"
                + "  fetch(a.href).then(function(r){return r.blob();}).then(function(b){"
                + "    var fr = new FileReader();"
                + "    fr.onloadend = function(){"
                + "      var res = fr.result + '';"
                + "      var b64 = res.substring(res.indexOf(',')+1);"
                + "      window.QognixBlobDownloader.download(b64, name);"
                + "    };"
                + "    fr.readAsDataURL(b);"
                + "  }).catch(function(err){});"
                + "}, true);"
                + "})()";
        view.evaluateJavascript(js, null);
    }

    private void openExternal(String url) {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (Exception ignored) { }
    }

    private boolean isOnline() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return true;
        NetworkInfo info = cm.getActiveNetworkInfo();
        return info != null && info.isConnected();
    }

    private void showOffline() {
        refreshLayout.setRefreshing(false);
        webView.setVisibility(View.GONE);
        errorLayout.setVisibility(View.VISIBLE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == FILE_CHOOSER_REQUEST) {
            if (filePathCallback == null) return;
            Uri[] results = null;
            if (resultCode == Activity.RESULT_OK && data != null) {
                String dataString = data.getDataString();
                if (dataString != null) {
                    results = new Uri[]{Uri.parse(dataString)};
                }
            }
            filePathCallback.onReceiveValue(results);
            filePathCallback = null;
        }
    }

    @Override
    public void onBackPressed() {
        // Back walks through the site's history first; only exits at the start page.
        if (webView != null && webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onPause() {
        if (webView != null) webView.onPause();
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (webView != null) webView.onResume();
    }

    @Override
    protected void onDestroy() {
        if (webView != null) {
            webView.destroy();
            webView = null;
        }
        super.onDestroy();
    }
}
