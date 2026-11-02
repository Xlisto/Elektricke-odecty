package cz.xlisto.elektrodroid.utils;

import java.util.Calendar;

/**
 * Třída pro výpočet rozdílu (v měsících) mezi dvěma kalendářními daty.
 * <p>
 * Created Xlisto by 17.05.2020
 * Updated Xlisto by 30.10.2021
 **/
public class DifferenceDate {

    private final Calendar cal1;
    private final Calendar cal2;
    private final TypeDate typeDate;


    /**
     * Konstruktor pro nastavení dvou kalendářních dat a typu výpočtu.
     *
     * @param cal1     počáteční datum
     * @param cal2     koncové datum
     * @param typeDate typ výpočtu (MONTH pro odečty, INVOICE pro faktury)
     */
    public DifferenceDate(Calendar cal1, Calendar cal2, TypeDate typeDate) {
        this.cal1 = cal1;
        this.cal2 = cal2;
        this.typeDate = typeDate;
    }


    /**
     * Vypočítá počet měsíců mezi dvěma daty.
     *
     * @return počet měsíců jako desatinné číslo
     */
    public double getMonth() {
        // Výpočet poměrné části v každém z měsíců
        double totalDaysCal1 = cal1.getActualMaximum(Calendar.DAY_OF_MONTH);
        double totalDaysCal2 = cal2.getActualMaximum(Calendar.DAY_OF_MONTH);

        double countDaysCal1 = (totalDaysCal1 - cal1.get(Calendar.DATE) + 1) / totalDaysCal1;
        double countDaysCal2 = 0;

        if (typeDate == TypeDate.INVOICE) {
            // Při zobrazení faktur – počítám včetně posledního data
            countDaysCal2 = (cal2.get(Calendar.DATE)) / totalDaysCal2;
        } else if (typeDate == TypeDate.MONTH) {
            // Při zobrazení měsíčních odečtů – poslední datum nezahrnuji do výpočtu
            countDaysCal2 = (cal2.get(Calendar.DATE) - 1) / totalDaysCal2;
        }

        double months = countDaysCal1 + countDaysCal2;

        // Výpočet rozdílu měsíců, odečítám 1, protože jeden měsíc se počítá v předchozím odstavci
        int differentOfMonths = cal2.get(Calendar.MONTH) - cal1.get(Calendar.MONTH);
        int differentOfYears = cal2.get(Calendar.YEAR) - cal1.get(Calendar.YEAR);

        // Pokud je záporný rozdíl měsíců, ponížit rozdíl roku o 1
        if (differentOfMonths < 0) {
            differentOfMonths += 12;
            differentOfYears -= 1;
        }
        // Pokud je rozdíl roku více než 0, počet roků se vynásobí 12
        if (differentOfYears > 0) {
            differentOfMonths += 12 * differentOfYears;
        }
        months += differentOfMonths;
        return months - 1;
    }


    /**
     * Typ odečtů pro který se používá rozdíl datumu
     */
    public enum TypeDate {
        MONTH,
        INVOICE
    }

}
