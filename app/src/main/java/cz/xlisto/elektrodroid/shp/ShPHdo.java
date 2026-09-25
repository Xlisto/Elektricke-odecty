package cz.xlisto.elektrodroid.shp;

import android.content.Context;

/**
 * Xlisto 13.06.2023 9:02
 */
public class ShPHdo extends ShP{

    public static final String ARG_RUNNING_SERVICE = "hdoRunningService";
    public static final String HDO_OLD_REMINDER_DISABLED = "hdoOldReminderDisabled";
    public static final String HDO_OLD_REMINDER_SNOOZE_UNTIL = "hdoOldReminderSnoozeUntil";

    public ShPHdo(Context context) {
        this.context = context;
    }
}
