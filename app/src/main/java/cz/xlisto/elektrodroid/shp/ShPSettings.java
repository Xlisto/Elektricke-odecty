package cz.xlisto.elektrodroid.shp;


import android.content.Context;


/**
 * Třída pro uložení základních nastavení aplikace
 */
public class ShPSettings extends ShP {

    public static final String SHOW_FAB = "showFab";
    public static final String SHOW_BOTTOM_NAVIGATION = "showBottomNavigation";
    public static final String SHOW_LEFT_NAVIGATION = "showLeftNavigation";
    public static final String ALLOW_MOBILE_DATA = "allowMobileData";
    public static final String USE_HDO_SET_ALARM = "useHdoSetAlarm";

    // Nastavení pro notifikace zápisu odečtu
    public static final String READING_NOTIFICATION_ENABLED = "readingNotificationEnabled";
    public static final String READING_NOTIFICATION_FREQUENCY = "readingNotificationFrequency";
    public static final String READING_NOTIFICATION_DAY_OF_MONTH = "readingNotificationDayOfMonth";
    public static final String READING_NOTIFICATION_DAY_OF_WEEK = "readingNotificationDayOfWeek";
    public static final String READING_NOTIFICATION_TIME = "readingNotificationTime";


    public ShPSettings(Context context) {
        this.context = context;
    }

}
