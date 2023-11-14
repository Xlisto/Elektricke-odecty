package cz.xlisto.odecty.utils;

import android.content.Context;
import android.content.res.Configuration;

/**
 * Třída pro zjištění, zda-li se jedná o landscape nebo portrait mód.
 * Xlisto 12.11.2023 20:40
 */
public class DetectScreenMode {
    private static final String TAG = "DetectScreenMode";


    /**
     * Zjistí, zda-li se jedná o landscape mód.
     * @param context Kontext aplikace
     * @return True, pokud se jedná o landscape mód, jinak false.
     */
    public static boolean isLandscape(Context context) {
        return context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;
    }
}
