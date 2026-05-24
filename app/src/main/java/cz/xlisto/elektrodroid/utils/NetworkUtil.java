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

    /**
     * Zjistí, zda je aktivní internetové připojení.
     *
     * @param context Kontext aplikace
     * @return true, pokud je aktivní internetové připojení, jinak false
     */
    public static boolean isInternetAvailable(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager != null) {
            return isInternetAvailableApi23(connectivityManager);
        }
        return false;
    }


    /**
     * Zjistí, zda je internet dostupný na zařízeních s API 23 a vyšším.
     *
     * @param connectivityManager Objekt `ConnectivityManager` pro získání informací o síti.
     * @return true, pokud je internet dostupný, jinak false
     */
    private static boolean isInternetAvailableApi23(ConnectivityManager connectivityManager) {
        Network[] networks = connectivityManager.getAllNetworks();
        for (Network network : networks) {
            NetworkCapabilities networkCapabilities = connectivityManager.getNetworkCapabilities(network);

            if (networkCapabilities != null && networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) && networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)) {
                if (networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                    Log.w("NetworkUtil", "Connected via WiFi");
                    return true;
                } else if (networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                    Log.w("NetworkUtil", "Connected via Mobile Data");
                    return true;
                } else if (networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) {
                    Log.w("NetworkUtil", "Connected via Ethernet");
                    return true;
                }
                return false;
            }
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
        if (connectivityManager != null) {
            return isWifiConnectedApi23(connectivityManager);
        }
        return false;
    }


    /**
     * Zjistí, zda je aktivní připojení k WiFi na zařízeních s API 23 a vyšším.
     *
     * @param connectivityManager Objekt `ConnectivityManager` pro získání informací o síti.
     * @return true, pokud je aktivní připojení k WiFi, jinak false
     */
    private static boolean isWifiConnectedApi23(ConnectivityManager connectivityManager) {
        Network[] networks = connectivityManager.getAllNetworks();
        for (Network network : networks) {
            NetworkCapabilities networkCapabilities = connectivityManager.getNetworkCapabilities(network);
            if (networkCapabilities != null && networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                return true;
            }
        }
        return false;
    }


    /**
     * Vrátí, zda uživatel v nastavení povolil použití mobilních dat.
     */
    public static boolean isMobileDataAllowedByUser(Context context) {
        return new ShPSettings(context).get(ShPSettings.ALLOW_MOBILE_DATA, true);
    }


    /**
     * Vrátí true, pokud je povolený přenos dat podle nastavení uživatele a aktuální sítě.
     * WiFi/Ethernet je vždy povoleno, mobilní síť jen při zapnutém nastavení.
     */
    public static boolean isInternetAllowedBySettings(Context context) {
        if (isWifiConnected(context)) {
            return true;
        }
        return isMobileDataAllowedByUser(context) && isInternetAvailable(context);
    }


    /**
     * Vrátí true, pokud je potřeba uživatele upozornit na požadavek WiFi připojení.
     */
    public static boolean shouldWarnWifiRequired(Context context) {
        return !isMobileDataAllowedByUser(context) && !isWifiConnected(context);
    }

}
