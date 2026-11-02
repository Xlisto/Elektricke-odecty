package cz.xlisto.elektrodroid.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.util.Log;

import cz.xlisto.elektrodroid.shp.ShPSettings;


/**
 * Pomocná třída pro práci s internetovým připojením.
 */
public class NetworkUtil {

    private static final String TAG = "NetworkUtil";


    /**
     * Zjistí, zda je aktivní internetové připojení.
     *
     * @param context Kontext aplikace
     * @return true, pokud je aktivní internetové připojení, jinak false
     */
    public static boolean isInternetAvailable(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager == null) {
            return false;
        }

        Network activeNetwork = connectivityManager.getActiveNetwork();
        if (activeNetwork == null) {
            return false;
        }

        NetworkCapabilities networkCapabilities = connectivityManager.getNetworkCapabilities(activeNetwork);
        if (networkCapabilities == null) {
            return false;
        }

        if (!networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                || !networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)) {
            return false;
        }

        if (networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
            Log.w(TAG, "Connected via WiFi");
            return true;
        } else if (networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
            Log.w(TAG, "Connected via Mobile Data");
            return true;
        } else if (networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) {
            Log.w(TAG, "Connected via Ethernet");
            return true;
        }

        return false;
    }


    /**
     * Zjistí, zda je aktivní připojení k WiFi.
     *
     * @param context Kontext aplikace
     * @return true, pokud je aktivní připojení k WiFi, jinak false
     */
    public static boolean isWifiConnected(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager == null) {
            return false;
        }

        Network activeNetwork = connectivityManager.getActiveNetwork();
        if (activeNetwork == null) {
            return false;
        }

        NetworkCapabilities networkCapabilities = connectivityManager.getNetworkCapabilities(activeNetwork);
        return networkCapabilities != null && networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI);
    }


    /**
     * Vrátí, zda uživatel v nastavení povolil použití mobilních dat.
     *
     * @param context Kontext aplikace
     * @return true, pokud jsou mobilní data povolena
     */
    public static boolean isMobileDataAllowedByUser(Context context) {
        return new ShPSettings(context).get(ShPSettings.ALLOW_MOBILE_DATA, true);
    }


    /**
     * Vrátí true, pokud je povolený přenos dat podle nastavení uživatele a aktuální sítě.
     * WiFi/Ethernet je vždy povoleno, mobilní síť jen při zapnutém nastavení.
     *
     * @param context Kontext aplikace
     * @return true, pokud je přenos dat povolen
     */
    public static boolean isInternetAllowedBySettings(Context context) {
        if (isWifiConnected(context)) {
            return true;
        }
        return isMobileDataAllowedByUser(context) && isInternetAvailable(context);
    }


    /**
     * Vrátí true, pokud je potřeba uživatele upozornit na požadavek WiFi připojení.
     *
     * @param context Kontext aplikace
     * @return true, pokud je požadováno upozornění na WiFi
     */
    public static boolean shouldWarnWifiRequired(Context context) {
        return !isMobileDataAllowedByUser(context) && !isWifiConnected(context);
    }

}
