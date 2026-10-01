package cz.xlisto.elektrodroid.modules.subscriptionpoint;


import android.content.Context;

import androidx.core.graphics.ColorUtils;

import cz.xlisto.elektrodroid.utils.DetectNightMode;


/**
 * Pomocná třída pro vyhledání vysoce kontrastních barev cenových čar
 * v kombinovaném grafu roční spotřeby a průměrné ceny.
 */
public class AnnualGraphColorHelper {

    // Paleta předdefinovaných výrazných barev vhodných pro grafy
    private static final int[] PALETTE = new int[]{
            0xFFE53935, // Červená (Red 600)
            0xFF1E88E5, // Modrá (Blue 600)
            0xFFF57C00, // Oranžová (Orange 700)
            0xFF8E24AA, // Fialová (Purple 600)
            0xFF00ACC1, // Tyrkysová (Cyan 600)
            0xFF43A047, // Zelená (Green 600)
            0xFFD81B60, // Růžová (Pink 600)
            0xFFFDD835, // Žlutá/Zlatá (Yellow 600)
            0xFF3949AB, // Indigo (Indigo 600)
            0xFF00897B, // Teal (Teal 600)
            0xFF6D4C41, // Hnědá (Brown 600)
            0xFFFF6F00, // Tmavě oranžová
            0xFF00E676, // Jasně zelená
            0xFFFF1744  // Jasně červená
    };


    public record LineColors(int colorPriceVT, int colorPriceNT, int colorPriceTotal) {

    }


    /**
     * Deterministicky vybere 3 kontrastní barvy pro cenové čáry (Cena VT, Cena NT, Cena Celkem).
     *
     * @param context Kontext pro zjištění režimu (Light/Dark)
     * @param colorVT Barva sloupce VT
     * @param colorNT Barva sloupce NT
     * @param bgColor Barva pozadí grafu
     * @return Objekt {@link LineColors} obsahující trojici barev
     */
    public static LineColors selectLineColors(Context context, int colorVT, int colorNT, int bgColor) {
        boolean isDark = DetectNightMode.isNightMode(context);

        double bestScore = -1;
        int bestVT = PALETTE[0];
        int bestNT = PALETTE[1 % PALETTE.length];
        int bestTotal = PALETTE[2 % PALETTE.length];

        for (int i = 0; i < PALETTE.length; i++) {
            int cVT = PALETTE[i];
            double contrastBgVT = ColorUtils.calculateContrast(cVT, bgColor);
            if (contrastBgVT < 2.5) continue;

            for (int j = 0; j < PALETTE.length; j++) {
                if (i == j) continue;
                int cNT = PALETTE[j];
                double contrastBgNT = ColorUtils.calculateContrast(cNT, bgColor);
                if (contrastBgNT < 2.5) continue;

                for (int k = 0; k < PALETTE.length; k++) {
                    if (k == i || k == j) continue;
                    int cTotal = PALETTE[k];
                    double contrastBgTotal = ColorUtils.calculateContrast(cTotal, bgColor);
                    if (contrastBgTotal < 2.5) continue;

                    // Odlišitelnost čar mezi sebou
                    double contrastVT_NT = ColorUtils.calculateContrast(cVT, cNT);
                    double contrastVT_Total = ColorUtils.calculateContrast(cVT, cTotal);
                    double contrastNT_Total = ColorUtils.calculateContrast(cNT, cTotal);

                    // Odlišitelnost od sloupců VT a NT
                    double contrastVT_BarVT = ColorUtils.calculateContrast(cVT, colorVT);
                    double contrastNT_BarNT = ColorUtils.calculateContrast(cNT, colorNT);
                    double contrastTotal_BarVT = ColorUtils.calculateContrast(cTotal, colorVT);
                    double contrastTotal_BarNT = ColorUtils.calculateContrast(cTotal, colorNT);

                    // Součet vážených skóre
                    double score = contrastBgVT + contrastBgNT + contrastBgTotal * 1.5
                            + contrastVT_NT + contrastVT_Total + contrastNT_Total
                            + contrastVT_BarVT * 0.5 + contrastNT_BarNT * 0.5
                            + contrastTotal_BarVT * 0.5 + contrastTotal_BarNT * 0.5;

                    if (score > bestScore) {
                        bestScore = score;
                        bestVT = cVT;
                        bestNT = cNT;
                        bestTotal = cTotal;
                    }
                }
            }
        }

        // Výchozí záložní barvy, pokud by nebylo nalezeno dostatečné skóre
        if (bestScore < 0) {
            if (isDark) {
                bestVT = 0xFFFF5252;    // Světle červená
                bestNT = 0xFFFFD740;    // Světle žlutá
                bestTotal = 0xFF448AFF; // Světle modrá
            } else {
                bestVT = 0xFFD32F2F;    // Tmavě červená
                bestNT = 0xFFF57F17;    // Tmavě žlutá/oranžová
                bestTotal = 0xFF1976D2; // Tmavě modrá
            }
        }

        return new LineColors(bestVT, bestNT, bestTotal);
    }

}
