package cz.xlisto.elektrodroid.utils;


import java.util.Calendar;


/**
 * Neoměnitelný (immutable) model data uchovávající rok, měsíc a den v měsíci.
 * Hodnoty složek data jsou extrahovány z instance {@link Calendar} při vytváření objektu.
 */
public class DateUtil {

    /**
     * Rok (např. 2025).
     */
    private final int year;
    /** Měsíc v rozsahu 0–11 (hodnota z {@link Calendar#MONTH}, kde 0 = leden, 11 = prosinec). */
    private final int month;
    /** Den v měsíci v rozsahu 1–31 (hodnota z {@link Calendar#DAY_OF_MONTH}). */
    private final int day;


    /**
     * Vytvoří novou instanci {@link DateUtil} extrakcí složek data ze zadaného objektu {@link Calendar}.
     *
     * @param calendar zdrojové datum a čas ({@link Calendar})
     */
    public DateUtil(Calendar calendar) {
        year = calendar.get(Calendar.YEAR);
        month = calendar.get(Calendar.MONTH);
        day = calendar.get(Calendar.DAY_OF_MONTH);
    }


    /**
     * Vrátí rok.
     *
     * @return rok (např. 2025)
     */
    public int getYear() {
        return year;
    }


    /**
     * Vrátí měsíc v rozsahu 0–11 (0 = leden, 11 = prosinec).
     * <p>
     * Pro získání lidsky čitelného čísla měsíce v rozsahu 1–12 použijte výraz {@code getMonth() + 1}.
     *
     * @return měsíc jako číselná hodnota 0–11
     */
    public int getMonth() {
        return month;
    }


    /**
     * Vrátí den v měsíci.
     *
     * @return den v měsíci (1–31)
     */
    public int getDay() {
        return day;
    }

}
