package cz.xlisto.elektrodroid.shp;


import android.content.Context;


/**
 * Třída pro uložení základních nastavení aplikace
 */
public class ShPSettings extends ShP {

    public static final String SHOW_FAB = "showFab";
    public static final String SHOW_BOTTOM_NAVIGATION = "showBottomNavigation";
    public static final String SHOW_LEFT_NAVIGATION = "showLeftNavigation";


    public ShPSettings(Context context) {
        this.context = context;
    }

}
