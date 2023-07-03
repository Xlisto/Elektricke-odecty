package cz.xlisto.odecty.modules.hdo;

import android.content.Context;
import android.webkit.JavascriptInterface;
import android.widget.Toast;

/**
 * Xlisto 26.06.2023 3:21
 */
public class WebAppInterface {
    private static final String TAG = "WebAppInterface";
    private OnSaveUrlListener onSaveUrlListener;


    /** Instantiate the interface and set the context */
    WebAppInterface() {
    }


    @JavascriptInterface   // must be added for API 17 or higher
    public void showToast(String url) {
        onSaveUrlListener.onSaveUrl(url);
    }


    public void setOnSaveUrlListener(OnSaveUrlListener onSaveUrlListener) {
        this.onSaveUrlListener = onSaveUrlListener;
    }


    interface OnSaveUrlListener {
        void onSaveUrl(String url);
    }
}
