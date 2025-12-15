package cz.xlisto.elektrodroid.utils;


import java.util.Calendar;


/**
 * Jednoduchý model data obsahující rok, měsíc a den.
 * Hodnoty jsou naplněny z objektu {@link Calendar} v konstruktoru.
 */
public class DateUtil {

    // Konstantní tag pro logování (může být použit v Log.* voláních).
    private static final String TAG = "Date";
    // Rok (např. 2025)
    private final int year;
    // Měsíc jako číslo 0-11 (Calendar.MONTH vrací 0 = leden, 11 = prosinec).
    // Poznámka: pokud potřebujete lidsky čitelný měsíc 1-12, je třeba k této hodnotě přičíst 1.
    private final int month;
    // Den v měsíci (1-31)
    private final int day;


    /**
     * Vytvoří instanci Date z daného Calendaru.
     * Hodnoty year/month/day jsou extrahovány při vytváření a pak neměnné.
     *
     * @param calendar zdrojové datum/čas
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
     * Vrátí měsíc v rozsahu 0-11 (0 = leden).
     * Pokud potřebujete měsíc 1-12 použijte {@code getMonth() + 1}.
     *
     * @return měsíc 0-11
     */
    public int getMonth() {
        return month;
    }


    /**
     * Vrátí den v měsíci (1-31).
     *
     * @return den v měsíci
     */
    public int getDay() {
        return day;
    }

}
