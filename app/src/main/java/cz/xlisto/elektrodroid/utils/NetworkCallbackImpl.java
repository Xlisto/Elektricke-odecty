package cz.xlisto.elektrodroid.utils;


import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.util.Log;

import androidx.annotation.NonNull;


/**
 * Třída `NetworkCallbackImpl`, která rozšiřuje třídu `ConnectivityManager.NetworkCallback`.
 * Tato třída implementuje metody pro sledování změn v síťovém připojení.
 */
public class NetworkCallbackImpl extends ConnectivityManager.NetworkCallback {

    private static final String TAG = "NetworkCallbackImpl";
    private final NetworkChangeListener networkChangeListener;


    public NetworkCallbackImpl(NetworkChangeListener networkChangeListener) {
        this.networkChangeListener = networkChangeListener;
    }


    /**
     * Metoda, která se volá, když je síť dostupná.
     *
     * @param network Objekt `Network`, který reprezentuje dostupnou síť.
     */
    @Override
    public void onAvailable(@NonNull Network network) {
        super.onAvailable(network);
        Log.w("NetworkCallback", "Network is available");
        if (networkChangeListener != null)
            networkChangeListener.onNetworkAvailable();
    }


    /**
     * Metoda, která se volá, když je síť ztracena.
     *
     * @param network Objekt `Network`, který reprezentuje ztracenou síť.
     */
    @Override
    public void onLost(@NonNull Network network) {
        super.onLost(network);
        Log.w("NetworkCallback", "Network is lost");
        if (networkChangeListener != null)
            networkChangeListener.onNetworkLost();
    }


    /**
     * Metoda, která se volá, když se změní schopnosti sítě.
     *
     * @param network             Objekt `Network`, který reprezentuje síť, jejíž schopnosti se změnily.
     * @param networkCapabilities Objekt `NetworkCapabilities`, který obsahuje nové schopnosti sítě.
     */
    @Override
    public void onCapabilitiesChanged(@NonNull Network network, @NonNull NetworkCapabilities networkCapabilities) {
        super.onCapabilitiesChanged(network, networkCapabilities);
        if (networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
            Log.w("NetworkCallback", "Connected via WiFi");
        } else if (networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
            Log.w("NetworkCallback", "Connected via Mobile Data");
        } else if (networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) {
            Log.w("NetworkCallback", "Connected via Ethernet");
        } else {
            Log.w("NetworkCallback", "Connected via Unknown");
        }
    }


    public interface NetworkChangeListener {

        void onNetworkAvailable();

        void onNetworkLost();
        //void onNetworkCapabilitiesChanged(NetworkCapabilities networkCapabilities);
    }

}
