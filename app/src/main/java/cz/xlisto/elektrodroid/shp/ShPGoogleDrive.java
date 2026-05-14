package cz.xlisto.elektrodroid.shp;


import android.content.Context;


/**
 * Třída ShPGoogleDrive rozšiřuje třídu ShP a poskytuje metody pro práci s Google Drive.
 * Obsahuje konstantu pro stav přihlášení uživatele.
 */
public class ShPGoogleDrive extends ShP {

    public static final String APP_DATA_FOLDER = "appDataFolder";
    public static final String USER_SIGNED = "isUserSigned";
    public static final String USER_NAME = "userName";


    /**
     * Konstruktor pro ShPGoogleDrive.
     *
     * @param context Aplikační kontext.
     */
    public ShPGoogleDrive(Context context) {
        this.context = context;
    }

}
