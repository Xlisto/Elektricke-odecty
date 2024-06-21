package cz.xlisto.elektrodroid.utils;

/** Pomocná třída pro převod formátu čísel barev
 * Xlisto 27.10.2023 21:34
 */
public class ColorHelper {
    private static final String TAG = "ColorHelper";

    /**
     * Převede číslo barvy na hexadecimální řetězec v HTML formátu (bez alfa kanálu).
     * @param color číslo barvy
     * @return hexadecimální řetězec v HTML formátu
     */
    public static String colorToHtml(int color) {
        // Odstranění alfa kanálu z čísla barvy
        int colorWithoutAlpha = color & 0x00FFFFFF;

        // Převedení čísla barvy bez alfa kanálu na hexadecimální řetězec
        StringBuilder hexColor = new StringBuilder(Integer.toHexString(colorWithoutAlpha));

        // Pokud je hexColor kratší než 6 znaků, doplň nuly na začátek
        while (hexColor.length() < 6) {
            hexColor.insert(0, "0");
        }

        // Vytvoření řetězce v HTML formátu
        return "#" + hexColor;
    }

    /**
     * Převede hexadecimální řetězec v HTML formátu na číslo barvy.
     * @param htmlColor hexadecimální řetězec v HTML formátu
     * @return číslo barvy
     */
    public static int htmlToColor(String htmlColor) {
        // Ověření, zda vstupní řetězec obsahuje znak '#'
        if (htmlColor.startsWith("#")) {
            // Odstranění znaku '#' ze vstupního řetězce

            // Pokud je vstupní řetězec příliš krátký, doplnění nul na začátek
            StringBuilder htmlColorBuilder = new StringBuilder(htmlColor.substring(1));
            while (htmlColorBuilder.length() < 6) {
                htmlColorBuilder.insert(0, "0");
            }
            htmlColor = htmlColorBuilder.toString();

            // Převod hexadecimálního řetězce na číslo
            int color = 0;
            try {
                color = Integer.parseInt(htmlColor, 16);
            } catch (NumberFormatException e) {
                color = 101010;
                e.printStackTrace();
            }

            // Nastavení alfa kanálu na hodnotu 255 (plná průhlednost)
            color |= 0xFF000000;

            return color;
        } else {
            return -1; // neplatný vstup
        }
    }
}
