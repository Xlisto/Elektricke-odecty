package cz.xlisto.elektrodroid.utils;

import android.content.Context;
import android.content.res.Configuration;

/**
 * Detekce nočního režimu (dark theme).
 */
public class DetectNightMode {

    /**
     * Zjistí, zda je aktivní noční režim (dark theme).
     *
     * @param context Context aplikace nebo aktivity
     * @return {@code true}, pokud je aktivní noční režim, jinak {@code false}
     */
    public static boolean isNightMode(Context context) {
        int currentNightMode = context.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        return currentNightMode == Configuration.UI_MODE_NIGHT_YES;
    }

}
