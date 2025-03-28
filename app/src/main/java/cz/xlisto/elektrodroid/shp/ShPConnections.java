package cz.xlisto.elektrodroid.shp;


import android.content.Context;


/**
 * Třída ShPConnections rozšiřuje třídu ShP a poskytuje specifické nastavení pro připojení.
 */
public class ShPConnections extends ShP {

    // Konstantní klíč pro povolení mobilního připojení
    public static final String ALLOW_MOBILE_CONNECTION = "allowMobileConnection";


    /**
     * Konstruktor třídy ShPConnections.
     *
     * @param context Kontext aplikace, který je potřebný pro přístup k SharedPreferences.
     */
    public ShPConnections(Context context) {
        this.context = context;
    }

}
