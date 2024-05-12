package cz.xlisto.elektrodroid.utils;

import android.content.Context;

/**
 * Převádí hodnoty z px na dp a opačně
 * Xlisto 01.01.2024 14:38
 */
public class DensityUtils {
    private static final String TAG = "DensityUtils";


    /**
     * Převede hodnotu v dp na px
     *
     * @param context kontext aplikace
     * @param dp      hodnota v dp
     * @return hodnota v px
     */
    public static int dpToPx(Context context, float dp) {
        float density = context.getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }


    /**
     * Převede hodnotu v px na dp
     *
     * @param context kontext aplikace
     * @param px      hodnota v px
     * @return hodnota v dp
     */
    public static int pxToDp(Context context, float px) {
        float density = context.getResources().getDisplayMetrics().density;
        return Math.round(px / density);
    }
}
