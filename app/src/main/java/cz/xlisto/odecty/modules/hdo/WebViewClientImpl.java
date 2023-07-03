package cz.xlisto.odecty.modules.hdo;


import android.webkit.WebView;
import android.webkit.WebViewClient;

/**
 * Xlisto 24.06.2023 10:34
 */
public class WebViewClientImpl extends WebViewClient {
    private static final String TAG = "WebViewClientImpl";
    private OnPageFinishedListener onPageFinishedListener;


    public void onPageFinished(WebView view, String url) {
        onPageFinishedListener.onPageFinished();
    }


    @Override
    public boolean shouldOverrideUrlLoading(android.webkit.WebView webView, String url) {
        return false;
    }


    public void setOnPageFinishedListener(OnPageFinishedListener onPageFinishedListener) {
        this.onPageFinishedListener = onPageFinishedListener;
    }


    interface OnPageFinishedListener {
        void onPageFinished();
    }
}
