package cz.xlisto.elektrodroid.utils;


import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.util.Log;


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
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                return isInternetAvailableApi23(connectivityManager);
            } else {
                return isInternetAvailableApiOld(connectivityManager);
            }
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
     * Zjistí, zda je internet dostupný na zařízeních s API nižším než 23.
     *
     * @param connectivityManager Objekt `ConnectivityManager` pro získání informací o síti.
     * @return true, pokud je internet dostupný, jinak false
     */
    private static boolean isInternetAvailableApiOld(ConnectivityManager connectivityManager) {
        NetworkInfo[] networks = connectivityManager.getAllNetworkInfo();
        Log.w("NetworkUtil", "isInternetAvailable: networks: " + networks.length);
        for (NetworkInfo networkInfo : networks) {
            if (networkInfo != null && networkInfo.isConnected()) {
                if (networkInfo.getType() == ConnectivityManager.TYPE_WIFI) {
                    Log.w("NetworkUtil", "Connected via WiFi");
                    return true;
                } else if (networkInfo.getType() == ConnectivityManager.TYPE_MOBILE) {
                    Log.w("NetworkUtil", "Connected via Mobile Data");
                    return true;
                } else if (networkInfo.getType() == ConnectivityManager.TYPE_ETHERNET) {
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
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                return isWifiConnectedApi23(connectivityManager);
            } else {
                return isWifiConnectedLegacy(connectivityManager);
            }
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
     * Zjistí, zda je aktivní připojení k WiFi na zařízeních s API nižším než 23.
     *
     * @param connectivityManager Objekt `ConnectivityManager` pro získání informací o síti.
     * @return true, pokud je aktivní připojení k WiFi, jinak false
     */
    private static boolean isWifiConnectedLegacy(ConnectivityManager connectivityManager) {
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected() && activeNetworkInfo.getType() == ConnectivityManager.TYPE_WIFI;
    }

}

