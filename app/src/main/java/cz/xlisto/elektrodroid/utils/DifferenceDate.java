package cz.xlisto.elektrodroid.utils;

import java.util.Calendar;

/**
 * Rozdíl mezi dvěma daty
 * Created Xlisto by 17.05.2020
 * Updated Xlisto by 30.10.2021
 **/
public class DifferenceDate {
    /*private int day1, day2, month1, month2, year1, year2;
    private int dayR, monthR, yearR;*/
    private boolean reverse = false;//true pokud je první datum dřívější
    private Calendar cal1, cal2;
    //deklarace a inicializace pomocných proměnných
    private double months = 0;
    private double totalDaysCal1 = 0;//maximální počet dní v měsící
    private double totalDaysCal2 = 0;//maximální počet dní v měsíci
    private double countDaysCal1 = 0;//počet dní v měsíci
    private double countDaysCal2 = 0;//počet dní v měsíci
    private TypeDate typeDate;

    public DifferenceDate(Calendar cal1, Calendar cal2, TypeDate typeDate) {
        this.cal1 = cal1;
        this.cal2 = cal2;
        this.typeDate = typeDate;
    }

    public double getMonth() {
        //kontrola stejných datumu
        /*if ((cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR))
                && (cal1.get(Calendar.MONTH) == cal2.get(Calendar.MONTH)
                && (cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)))) {
        }*/

        //výpočet poměrné části v každém z měsíců
        totalDaysCal1 = cal1.getActualMaximum(Calendar.DAY_OF_MONTH);
        totalDaysCal2 = cal2.getActualMaximum(Calendar.DAY_OF_MONTH);
        countDaysCal1 = (totalDaysCal1 - cal1.get(Calendar.DATE)+1) / totalDaysCal1;
        if (typeDate == TypeDate.INVOICE)//při zobrazení faktur - počíátm včetně posledního data
            countDaysCal2 = (cal2.get(Calendar.DATE) ) / totalDaysCal2;
        if(typeDate == TypeDate.MONTH)//při zobrazení měsíčních odečtů - poslední datum nezahrnuji do výpočtu
            countDaysCal2 = (cal2.get(Calendar.DATE)-1) / totalDaysCal2;
        months = countDaysCal1 + countDaysCal2;

        //výpočet rozdílů měsíců, odečítám 1, protože jeden měsíc se počítá v předchozím odstavci
        int differentOfMonths = cal2.get(Calendar.MONTH) - cal1.get(Calendar.MONTH);
        //rozdíl roků
        int differentOfYears = cal2.get(Calendar.YEAR) - cal1.get(Calendar.YEAR);

        //pokud je záporný rozdíl měsíců, ponížít rozdíl roku o 1
        if (differentOfMonths < 0) {
            differentOfMonths += 12;
            differentOfYears -= 1;
        }
        //pokud je rozdíl roku více než 0, počet roků se vynásobí 12
        if (differentOfYears > 0)
            differentOfMonths += 12 * differentOfYears;
        months += differentOfMonths;
        return months - 1;
    }

    /*public DifferenceDate(int day1, int month1, int year1, int day2, int month2, int year2) {
        this.day1 = day1;
        this.day2 = day2;
        this.month1 = month1 + 1;
        this.month2 = month2 + 1;
        this.year1 = year1;
        this.year2 = year2;
        controll();
        calculationDate();
    }*/

/**
 * Výpočet rozdílu mezy daty
 */
    /*private void calculationDate() {
        dayR = day2 - day1;
        monthR = month2 - month1;
        yearR = year2 - year1;
        minusMonth();
        minusDay();
    }*/

/**
 * Zjisti maximální počet dní v měsici
 *
 * @param day
 * @param month
 * @param year
 * @return
 */
    /*private int monthDays(int day, int month, int year) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(year, month - 1, day);
        return calendar.getActualMaximum(Calendar.DAY_OF_MONTH);
    }*/

/**
 * Odečte dny, případně sníží měsíc o jeden
 */
    /*private void minusDay() {
        int maxDays = monthDays(day1, month1, year1);
        if (dayR < 0 && (monthR > 0 || yearR > 0)) {
            minusYear();
            dayR = maxDays + dayR;
        }
    }*/

/**
 * Odečte měsíc z dalšího roku
 */
    /*private void minusMonth() {
        if (monthR < 0 && yearR > 0) {
            yearR--;
            monthR = 12 + monthR;
        }
    }*/

/**
 * Odečte rok
 */
    /*private void minusYear() {
        monthR--;
        minusMonth();
    }

    public int getDayR() {
        return dayR;
    }

    public int getMonthR() {
        return monthR;
    }

    public int getYearR() {
        return yearR;
    }

    public double getMonth() {
        double m = (double) yearR * 12;
        m = m + monthR;
        if (day1 != day2) {
            int maxDays1 = monthDays(day1, month1, year1);
            int maxDays2 = monthDays(day2, month2, year2);
            double d = 0;
            if (month1 == month2) {
                d = (double) dayR / maxDays1;
            }
            if (month1 != month2) {
                double d1 = (double) (maxDays1 - day1) / maxDays1;
                double d2 = (double) day2 / maxDays2;
                d = d1 + d2;

            }
            //d = Math.round(d * 100);
            //d = d / 100;
            if (d >= 1)
                m--;
            m = m + d;
        }
        //Log.w("Day", "počet měsíce " + m);
        return m;
    }

    public boolean isReverse() {
        return reverse;
    }*/

    /*private void controll() {
        Calendar calendar1 = Calendar.getInstance();
        Calendar calendar2 = Calendar.getInstance();
        calendar1.set(year1, month1 - 1, day1);
        calendar2.set(year2, month2 - 1, day2);
        if (cal1.getTimeInMillis() > cal2.getTimeInMillis()) {
            reverse = true;
            int dayTemp = day1;
            int monthTemp = month1;
            int yearTemp = year1;
            day1 = day2;
            month1 = month2;
            year1 = year2;
            day2 = dayTemp;
            month2 = monthTemp;
            year2 = yearTemp;
        }
    }*/

    /**
     * Typ odečtů pro který se používá rozdíl datumu
     */
    public enum TypeDate {
        MONTH,
        INVOICE
    }
}
