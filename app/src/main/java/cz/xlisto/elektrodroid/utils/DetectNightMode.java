package cz.xlisto.elektrodroid.utils;

import android.content.Context;

/**
 * Detekce nočního režimu
 * Xlisto 28.05.2023 21:55
 */
public class DetectNightMode {
    private static final String TAG = "DetectNightMode";

    /**
     * Detekce nočního režimu
     *
     * @param context Context
     * @return boolean
     */
    public static boolean isNightMode(Context context) {
        int currentNightMode = context.getResources().getConfiguration().uiMode & android.content.res.Configuration.UI_MODE_NIGHT_MASK;
        switch (currentNightMode) {
            case android.content.res.Configuration.UI_MODE_NIGHT_NO:
            case android.content.res.Configuration.UI_MODE_NIGHT_UNDEFINED:
                return false;
            case android.content.res.Configuration.UI_MODE_NIGHT_YES:
                return true;
        }
        return false;
    }
}
