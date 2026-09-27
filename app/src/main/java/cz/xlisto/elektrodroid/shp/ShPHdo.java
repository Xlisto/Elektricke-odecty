package cz.xlisto.elektrodroid.shp;

import android.content.Context;

/**
 * Xlisto 13.06.2023 9:02
 */
public class ShPHdo extends ShP{

    public static final String ARG_RUNNING_SERVICE = "hdoRunningService";
    public static final String ARG_HDO_CLOCK_MINIMIZED = "hdoClockMinimized";
    public static final String ARG_MAIN_HDO_RELAY = "mainHdoRelay";
    public static final String ARG_HDO_VIEW_MODE_GRAPHIC = "hdoViewModeGraphic";
    public static final String HDO_OLD_REMINDER_DISABLED = "hdoOldReminderDisabled";
    public static final String HDO_OLD_REMINDER_SNOOZE_UNTIL = "hdoOldReminderSnoozeUntil";

    public ShPHdo(Context context) {
        this.context = context;
    }


    public boolean isGraphicMode() {
        return get(ARG_HDO_VIEW_MODE_GRAPHIC, false);
    }


    public void setGraphicMode(boolean isGraphic) {
        set(ARG_HDO_VIEW_MODE_GRAPHIC, isGraphic);
    }
}
