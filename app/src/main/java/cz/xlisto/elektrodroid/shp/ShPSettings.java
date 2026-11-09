package cz.xlisto.elektrodroid.shp;


import android.content.Context;


/**
 * Třída pro uložení základních nastavení aplikace
 */
public class ShPSettings extends ShP {

    public static final String SHOW_FAB = "showFab";
    public static final String ADD_BUTTON_TYPE = "addButtonType";
    public static final int ADD_BUTTON_TYPE_FAB = 0;
    public static final int ADD_BUTTON_TYPE_APP_BAR = 1;

    public static final String SHOW_BOTTOM_NAVIGATION = "showBottomNavigation";
    public static final String SHOW_LEFT_NAVIGATION = "showLeftNavigation";
    public static final String ALLOW_MOBILE_DATA = "allowMobileData";
    public static final String USE_HDO_SET_ALARM = "useHdoSetAlarm";


    public ShPSettings(Context context) {
        this.context = context;
    }


    /**
     * Vrací zvolený typ tlačítka pro přidávání záznamů (0 = FAB, 1 = APP_BAR).
     */
    public int getAddButtonType() {
        int defaultVal = get(SHOW_FAB, true) ? ADD_BUTTON_TYPE_FAB : ADD_BUTTON_TYPE_APP_BAR;
        return get(ADD_BUTTON_TYPE, defaultVal);
    }


    /**
     * Uloží zvolený typ tlačítka pro přidávání záznamů.
     */
    public void setAddButtonType(int type) {
        set(ADD_BUTTON_TYPE, type);
    }

}
