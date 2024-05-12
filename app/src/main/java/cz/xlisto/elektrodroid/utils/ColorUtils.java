package cz.xlisto.elektrodroid.utils;

import android.graphics.Color;

/**
 * Pomocné metody pro práci s barvami
 * Xlisto 01.01.2024 16:50
 */
public class ColorUtils {
    private static final String TAG = "ColorUtils";


    /**
     * Ztmaví barvu
     *
     * @param color barva
     * @return barva
     */
    public static int darkerColor(int color) {
        float[] hsv = new float[3];
        Color.colorToHSV(color, hsv);
        hsv[2] *= 0.8f; // Snížení hodnoty světlosti o 20%
        return Color.HSVToColor(hsv);
    }


    /**
     * Zesvětlí barvu a nastaví průhlednost
     *
     * @param color barva
     * @return barva
     */
    public static int lighterColor(int color) {
        float[] hsv = new float[3];
        Color.colorToHSV(color, hsv);
        hsv[2] *= 1.2f; // Zvýšení hodnoty světlosti o 20%
        return Color.HSVToColor(30, hsv); //30:nastavení průhlednosti
    }
}
