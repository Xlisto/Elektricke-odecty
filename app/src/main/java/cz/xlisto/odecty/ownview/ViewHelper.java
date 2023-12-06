package cz.xlisto.odecty.ownview;

import android.content.Context;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.Objects;
import java.util.TimeZone;

public class ViewHelper {

    public static final String TAG = "ViewHelper";
    /**
     * Přepočítá pixely v dp na skutečné pixely podle hustoty obrazovky
     * @param dp - počet dp
     * @param context - kontext aplikace
     * @return int - počet pixelů
     */
    public static int convertDpToPx(int dp, Context context) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dp * scale + 0.5f);
    }


    /**
     * Vrátí instanci SimpleDateFormat s výchozím petternem dd.MM.yyyy
     * @return SimpleDateFormat s patternem dd.MM.yyyy
     */
    public static SimpleDateFormat getSimpleDateFormat() {
        return new SimpleDateFormat("dd.MM.yyyy", Locale.GERMANY);
    }


    /**
     * Vrátí instanci SimpleDateFormat s výchozím petternem dd.MM.yyyy  HH.mm.ss
     * znak : je zakázaný pro unix systémy, proto je použita tečka
     * @return SimpleDateFormat s patternem dd.MM.yyyy  HH.mm.ss
     */
    public static SimpleDateFormat getSimpleDateFormatForFiles() {
        return new SimpleDateFormat("dd.MM.yyyy  HH.mm.ss", Locale.GERMANY);
    }


    /**
     * Vrátí instanci SimpleDateFormat s výchozím petternem yyyy
     * @return SimpleDateFormat s patternem yyyy
     */
    public static SimpleDateFormat getSimpleDateFormatYear() {
        return new SimpleDateFormat("yyyy", Locale.GERMANY);
    }


    /**
     * Vrátí instanci SimpleDateFormat s výchozím petternem MM
     * @return SimpleDateFormat s patternem MM
     */
    public static SimpleDateFormat getSimpleDateFormatMonth() {
        return new SimpleDateFormat("MM", Locale.GERMANY);
    }


    /**
     * Vrátí instanci SimpleDateFormat s výchozím petternem hh:mm:ss
     * @return SimpleDateFormat s patternem hh:mm:ss
     */
    public static SimpleDateFormat getSimpleTimeFormat() {
        return new SimpleDateFormat("HH:mm:ss", Locale.GERMANY);
    }


    /**
     * Vrátí instanci SimpleDateFormat s možností definovat svůj vlastní pattern
     * příklad: dd.MM.yyyy (25.12.2020)
     * @param pattern  např.: dd.MM.yyyy
     * @return SimpleDateFormat s patternem např.: dd.MM.yyyy
     */
    public static SimpleDateFormat getSimpleDateFormat(String pattern) {
        return new SimpleDateFormat(pattern, Locale.GERMANY);
    }


    /**
     * Převede textový datum ve formátu dd.MM.yyyy do objektu Calendar.
     * Pokud se vyskytne chyba, vrací null.
     * @param string datum ve formátu dd.MM.yyyy
     * @return Calendar
     */
    public static Calendar parseCalendarFromString(String string) {
        Calendar calendar = Calendar.getInstance();
        try {
            calendar.setTime(Objects.requireNonNull(getSimpleDateFormat().parse(string)));
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return  calendar;
    }


    /**
     * Převede objekt Calendar na textové datum formátu dd.MM.yyyy
     * @param calendar objekt Calendar
     * @return String datum ve formátu dd.MM.yyyy
     */
    public static String parseStringFromCalendar(Calendar calendar) {
        return getSimpleDateFormat().format(calendar.getTime());
    }


    /**
     * Vrátí objekt Calendar jako pole int v tomto pořadí {dd,MM,YYYY}
     * @param calendar objekt Calendar
     * @return int[] pole int v tomto pořadí {dd,MM,YYYY}
     */
    public static int[] parseIntsFromCalendar(Calendar calendar) {
        return new int[]{calendar.get(Calendar.DAY_OF_MONTH),calendar.get(Calendar.MONDAY),calendar.get(Calendar.YEAR)};
    }


    /**
     * Vrátí dnešní datum jako string ve formátu dd.MM.yyyy
     * @return String datum ve formátu dd.MM.yyyy
     */
    public static String getTodayDate(){
        Calendar calendar = Calendar.getInstance();
        return parseStringFromCalendar(calendar);
    }


    /**
     * Převede číslo long na textové datum ve formátu dd.MM.yyyy
     * @param l datum v milisekundách
     * @return String datum ve formátu dd.MM.yyyy
     */
    public static String convertLongToDate(long l) {
        long offset = getOffsetTimeZones(l);
        return getSimpleDateFormat().format(l+offset);
    }

    public static String convertLongToTime(long l) {
        long offset = getOffsetTimeZones(l);
        return getSimpleTimeFormat().format(l+offset);
    }


    public static String yearOfLong(long l) {
        long offset = getOffsetTimeZones(l);
        return getSimpleDateFormat("yyyy").format(l+offset);
    }


    public static int yearIntOfLong(long l) {
        long offset = getOffsetTimeZones(l);
        return Integer.parseInt(getSimpleDateFormat("yyyy").format(l+offset));
    }


    /**
     * Vypočítá Offset časových pásem k našemu českému (hlavní využití při nastavení telefonu na jiná časová než je české)
     * @param l datum v milisekundách
     * @return long - offset v milisekundách pro dané datum
     */
    public static long getOffsetTimeZones(long l) {
        long offsetDef = TimeZone.getDefault().getOffset(l)*(-1);
        long offsetPra = TimeZone.getTimeZone("Europe/Prague").getOffset(l);

        return offsetDef+offsetPra;
    }



}

