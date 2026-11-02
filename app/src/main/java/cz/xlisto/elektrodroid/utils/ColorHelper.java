package cz.xlisto.elektrodroid.utils;


import android.util.Log;


/**
 * Pomocná třída pro převod formátu čísel barev.
 * <p>
 * Xlisto 27.10.2023 21:34
 */
public class ColorHelper {

    private static final String TAG = "ColorHelper";


    /**
     * Převede číslo barvy na hexadecimální řetězec v HTML formátu (bez alfa kanálu).
     *
     * @param color číslo barvy
     * @return hexadecimální řetězec v HTML formátu
     */
    public static String colorToHtml(int color) {
        // Odstranění alfa kanálu z čísla barvy
        int colorWithoutAlpha = color & 0x00FFFFFF;

        // Převedení čísla barvy bez alfa kanálu na hexadecimální řetězec doplněný na 6 znaků
        return String.format("#%06X", colorWithoutAlpha);
    }


    /**
     * Převede hexadecimální řetězec v HTML formátu na číslo barvy.
     *
     * @param htmlColor hexadecimální řetězec v HTML formátu
     * @return číslo barvy nebo -1 při neplatném vstupu
     */
    public static int htmlToColor(String htmlColor) {
        if (htmlColor == null || !htmlColor.startsWith("#")) {
            return -1; // neplatný vstup
        }

        // Pokud je vstupní řetězec příliš krátký, doplnění nul na začátek
        StringBuilder htmlColorBuilder = new StringBuilder(htmlColor.substring(1));
        while (htmlColorBuilder.length() < 6) {
            htmlColorBuilder.insert(0, "0");
        }

        // Převod hexadecimálního řetězce na číslo
        int color;
        try {
            color = Integer.parseInt(htmlColorBuilder.toString(), 16);
        } catch (NumberFormatException e) {
            color = 101010;
            Log.e(TAG, "Chyba při převodu HTML barvy: " + htmlColor, e);
        }

        // Nastavení alfa kanálu na hodnotu 255 (plná neprůhlednost)
        color |= 0xFF000000;

        return color;
    }

}
