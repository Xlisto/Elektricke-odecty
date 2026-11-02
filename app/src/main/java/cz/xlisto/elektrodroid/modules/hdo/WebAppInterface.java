package cz.xlisto.elektrodroid.modules.hdo;

import android.webkit.JavascriptInterface;


/**
 * Rozhraní pro komunikaci mezi JavaScriptovým kódem ve WebView a aplikací.
 * Slouží k zachycení a předání URL adresy s HDO daty.
 * <p>
 * Xlisto 26.06.2023 3:21
 */
public class WebAppInterface {

    private OnSaveUrlListener onSaveUrlListener;


    /**
     * Vytvoří novou instanci rozhraní pro WebView.
     */
    WebAppInterface() {
    }


    /**
     * Metoda volaná z JavaScriptového kódu ve WebView pro předání zachycené URL adresy.
     *
     * @param url Zachycená URL adresa
     */
    @JavascriptInterface   // Musí být přidáno pro API 17 nebo vyšší
    public void showToast(String url) {
        if (onSaveUrlListener != null) {
            onSaveUrlListener.onSaveUrl(url);
        }
    }


    /**
     * Nastaví posluchače pro zpracování a uložení zachycené URL adresy.
     *
     * @param onSaveUrlListener Posluchač události zachycení URL adresy
     */
    public void setOnSaveUrlListener(OnSaveUrlListener onSaveUrlListener) {
        this.onSaveUrlListener = onSaveUrlListener;
    }


    /**
     * Rozhraní posluchače pro předání zachycené URL adresy z WebView.
     */
    public interface OnSaveUrlListener {

        /**
         * Voláno při zachycení URL adresy z JavaScriptu.
         *
         * @param url Zachycená URL adresa
         */
        void onSaveUrl(String url);

    }

}
