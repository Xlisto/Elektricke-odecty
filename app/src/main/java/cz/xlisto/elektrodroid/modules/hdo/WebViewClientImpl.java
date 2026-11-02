package cz.xlisto.elektrodroid.modules.hdo;


import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;

/**
 * Vlastní klient pro WebView umožňující sledovat dokončení načtení stránky.
 * <p>
 * Xlisto 24.06.2023 10:34
 */
public class WebViewClientImpl extends WebViewClient {

    private OnPageFinishedListener onPageFinishedListener;


    @Override
    public void onPageFinished(WebView view, String url) {
        super.onPageFinished(view, url);
        if (onPageFinishedListener != null) {
            onPageFinishedListener.onPageFinished();
        }
    }


    @Override
    public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
        return false;
    }


    @SuppressWarnings("deprecation")
    @Override
    public boolean shouldOverrideUrlLoading(WebView webView, String url) {
        return false;
    }


    /**
     * Nastaví posluchače události dokončení načtení stránky ve WebView.
     *
     * @param onPageFinishedListener Posluchač události
     */
    public void setOnPageFinishedListener(OnPageFinishedListener onPageFinishedListener) {
        this.onPageFinishedListener = onPageFinishedListener;
    }


    /**
     * Rozhraní posluchače pro událost dokončení načtení stránky.
     */
    public interface OnPageFinishedListener {

        /**
         * Voláno při dokončení načtení stránky ve WebView.
         */
        void onPageFinished();

    }

}
