package cz.xlisto.elektrodroid.utils;


import android.content.Context;


/**
 * Pomocná třída pro přepočet rozměrových jednotek hustoty obrazovky (z dp na pixely).
 */
public class DensityUtils {

    private DensityUtils() {
        // Soukromý konstruktor pro zabránění instanciace nástrojové třídy
    }

    /**
     * Převede hodnotu z nezávislých pixelů (dp) na fyzické pixely (px) podle hustoty displeje zařízení.
     *
     * @param context kontext aplikace pro získání metrik displeje ({@link Context})
     * @param dp      hodnota v dp k převodu
     * @return přepočítaná hodnota v pixelech (px) zaokrouhlená na celé číslo
     */
    public static int dpToPx(Context context, float dp) {
        float density = context.getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

}
