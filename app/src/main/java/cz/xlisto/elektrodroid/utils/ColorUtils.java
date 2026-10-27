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


    /**
     * Zajistí dostatečný kontrast textu vůči pozadí (dle normy WCAG AA min. 4.5:1).
     * Pokud má fgColor vůči bgColor nízký kontrast, upraví se světlost (Value v HSV)
     * tak, aby byl text čitelný, ale zachoval si původní barevný odstín (Hue) uživatele.
     *
     * @param fgColor     barva popředí/textu zvolená uživatelem
     * @param bgColor     barva pozadí
     * @param minContrast minimální požadovaný kontrastní poměr (např. 4.5)
     * @return kontrastní barva vhodná pro vykreslení textu
     */
    public static int ensureContrastForText(int fgColor, int bgColor, double minContrast) {
        double currentContrast = androidx.core.graphics.ColorUtils.calculateContrast(fgColor, bgColor);
        if (currentContrast >= minContrast) {
            return fgColor;
        }

        boolean bgIsLight = androidx.core.graphics.ColorUtils.calculateLuminance(bgColor) > 0.5;
        float[] hsv = new float[3];
        Color.colorToHSV(fgColor, hsv);

        for (int i = 0; i < 25; i++) {
            if (bgIsLight) {
                hsv[2] -= 0.04f; // Ztmavování pro světlé pozadí
                if (hsv[2] < 0f) {
                    hsv[2] = 0f;
                    break;
                }
            } else {
                hsv[2] += 0.04f; // Zesvětlování pro tmavé pozadí
                if (hsv[2] > 1f) {
                    hsv[2] = 1f;
                    break;
                }
            }
            int candidate = Color.HSVToColor(Color.alpha(fgColor), hsv);
            if (androidx.core.graphics.ColorUtils.calculateContrast(candidate, bgColor) >= minContrast) {
                return candidate;
            }
        }

        // Pokud úprava HSV nestačí, použijeme standardní kontrastní text
        return bgIsLight ? 0xFF191C20 : 0xFFE2E2E9;
    }

}