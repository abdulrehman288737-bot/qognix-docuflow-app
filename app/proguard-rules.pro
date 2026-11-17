# Qognix DocuFlow - release builds currently ship with minifyEnabled=false.
# Keep WebView JavaScript bridge methods if minification is ever turned on:
-keepclassmembers class com.qognix.docuflow.MainActivity$BlobDownloader {
    @android.webkit.JavascriptInterface <methods>;
}
