package cz.xlisto.elektrodroid.shp;


import android.content.Context;


/**
 * Správce nastavení měsíčních odečtů v {@link android.content.SharedPreferences}.
 * Poskytuje konstanty klíčů a pomocné metody pro čtení a zápis preferencí zobrazení,
 * regulovaných cen a stavů zálohování měsíčních odečtů.
 */
public class ShPMonthlyReading extends ShP {

    /**
     * Klíč preferencí pro zjednodušené zobrazení (zachováno pro zpětnou kompatibilitu).
     */
    public static final String SHORT_LIST = "shortList";
    /**
     * Klíč preferencí pro režim zobrazení odečtů.
     */
    public static final String DISPLAY_MODE = "displayMode";
    /** Klíč preferencí pro zohlednění zastropování cen v roce 2023. */
    public static final String REGUL_PRICE = "regulPrice";
    /** Klíč preferencí pro zálohu nového měsíčního odečtu. */
    public static final String ADD_BACKUP_NEW_READING = "addBackupNewReading";
    /** Klíč preferencí pro zálohu upraveného měsíčního odečtu. */
    public static final String ADD_BACKUP_EDT_READING = "addBackupEdtReading";
    /** Klíč preferencí pro odeslání zálohy nového měsíčního odečtu. */
    public static final String SEND_BACKUP_NEW_READING = "sendBackupNewReading";
    /** Klíč preferencí pro odeslání zálohy upraveného měsíčního odečtu. */
    public static final String SEND_BACKUP_EDT_READING = "sendBackupEdtReading";

    /**
     * Hodnota režimu zobrazení: podrobné zobrazení (zobrazuje kompletní detaily a výpočty).
     */
    public static final int DISPLAY_MODE_DETAILED = 0;
    /**
     * Hodnota režimu zobrazení: stručné zobrazení (skrývá zálohy, ikonu a název ceníku).
     */
    public static final int DISPLAY_MODE_CONDENSED = 1;
    /**
     * Hodnota režimu zobrazení: krátké zobrazení (zobrazuje pouze spotřebu, cenu a bilanci).
     */
    public static final int DISPLAY_MODE_MINIMAL = 2;


    /**
     * Vytvoří novou instanci {@link ShPMonthlyReading}.
     *
     * @param context kontext aplikace ({@link Context}) pro přístup k {@link android.content.SharedPreferences}
     */
    public ShPMonthlyReading(Context context) {
        this.context = context;
    }


    /**
     * Vrátí aktuálně nastavený režim zobrazení měsíčních odečtů.
     * <p>
     * Vrací hodnotu {@link #DISPLAY_MODE_DETAILED} (0), {@link #DISPLAY_MODE_CONDENSED} (1)
     * nebo {@link #DISPLAY_MODE_MINIMAL} (2). Pokud hodnota {@link #DISPLAY_MODE} ještě
     * není uložena, určí se režim podle staršího klíče {@link #SHORT_LIST}.
     *
     * @return číselný identifikátor režimu zobrazení
     */
    public int getDisplayMode() {
        getShp();
        if (shp.contains(DISPLAY_MODE)) {
            return get(DISPLAY_MODE, DISPLAY_MODE_DETAILED);
        }
        boolean shortList = get(SHORT_LIST, false);
        return shortList ? DISPLAY_MODE_CONDENSED : DISPLAY_MODE_DETAILED;
    }


    /**
     * Uloží zvolený režim zobrazení měsíčních odečtů.
     * Automaticky synchronizuje klíč {@link #SHORT_LIST} pro zachování zpětné kompatibility.
     *
     * @param mode číselný identifikátor režimu zobrazení ({@link #DISPLAY_MODE_DETAILED},
     *             {@link #DISPLAY_MODE_CONDENSED} nebo {@link #DISPLAY_MODE_MINIMAL})
     */
    public void setDisplayMode(int mode) {
        set(DISPLAY_MODE, mode);
        set(SHORT_LIST, mode == DISPLAY_MODE_CONDENSED || mode == DISPLAY_MODE_MINIMAL);
    }

}
