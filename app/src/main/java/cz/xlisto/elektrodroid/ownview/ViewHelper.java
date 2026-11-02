package cz.xlisto.elektrodroid.ownview;


import android.content.Context;
import android.util.Log;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Locale;
import java.util.Objects;
import java.util.TimeZone;


/**
 * Pomocná třída pro práci s daty, časem a formátováním.
 */
public class ViewHelper {

    public static final String TAG = "ViewHelper";


    /**
     * Přepočítá pixely v dp na skutečné pixely podle hustoty obrazovky.
     *
     * @param dp      počet dp
     * @param context kontext aplikace
     * @return počet pixelů
     */
    public static int convertDpToPx(int dp, Context context) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dp * scale + 0.5f);
    }


    /**
     * Vrátí instanci SimpleDateFormat s výchozím vzorem dd.MM.yyyy.
     *
     * @return SimpleDateFormat s vzorem dd.MM.yyyy
     */
    public static SimpleDateFormat getSimpleDateFormat() {
        return new SimpleDateFormat("dd.MM.yyyy", Locale.GERMANY);
    }


    /**
     * Vrátí instanci SimpleDateFormat s výchozím vzorem dd.MM.yyyy  HH.mm.ss.
     * Znak : je zakázaný pro unix systémy, proto je použitá tečka.
     *
     * @return SimpleDateFormat s vzorem dd.MM.yyyy  HH.mm.ss
     */
    public static SimpleDateFormat getSimpleDateTimeFormatForFiles() {
        return new SimpleDateFormat("dd.MM.yyyy  HH.mm.ss", Locale.GERMANY);
    }


    /**
     * Vrátí instanci SimpleDateFormat s výchozím vzorem dd.MM.yyyy HH:mm:ss.
     *
     * @return SimpleDateFormat s vzorem dd.MM.yyyy HH:mm:ss
     */
    public static SimpleDateFormat getSimpleDateTimeFormat() {
        return new SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale.GERMANY);
    }


    /**
     * Vrátí instanci SimpleDateFormat s výchozím vzorem yyyy.
     *
     * @return SimpleDateFormat s vzorem yyyy
     */
    public static SimpleDateFormat getSimpleDateFormatYear() {
        return new SimpleDateFormat("yyyy", Locale.GERMANY);
    }


    /**
     * Vrátí instanci SimpleDateFormat s výchozím vzorem MM.
     *
     * @return SimpleDateFormat s vzorem MM
     */
    public static SimpleDateFormat getSimpleDateFormatMonth() {
        return new SimpleDateFormat("MM", Locale.GERMANY);
    }


    /**
     * Vrátí instanci SimpleDateFormat s možností definovat svůj vlastní vzor.
     * příklad: dd.MM.yyyy (25.12.2020)
     *
     * @param pattern vzor např. dd.MM.yyyy
     * @return SimpleDateFormat se zadaným vzorem
     */
    public static SimpleDateFormat getSimpleDateFormat(String pattern) {
        return new SimpleDateFormat(pattern, Locale.GERMANY);
    }


    /**
     * Převede textový datum ve formátu dd.MM.yyyy do objektu Calendar.
     * Pokud se vyskytne chyba, vrací aktuální Calendar.
     *
     * @param string datum ve formátu dd.MM.yyyy
     * @return Calendar
     */
    public static Calendar parseCalendarFromString(String string) {
        Calendar calendar = Calendar.getInstance();
        try {
            calendar.setTime(Objects.requireNonNull(getSimpleDateFormat().parse(string)));
        } catch (ParseException e) {
            Log.e(TAG, "Chyba při parsování data ze řetězce: " + string, e);
        }
        return calendar;
    }


    /**
     * Vrátí objekt Calendar jako pole int v tomto pořadí {dd, MM, YYYY}.
     *
     * @param calendar objekt Calendar
     * @return pole int v pořadí {den, měsíc (0-11), rok}
     */
    public static int[] parseIntsFromCalendar(Calendar calendar) {
        return new int[]{calendar.get(Calendar.DAY_OF_MONTH), calendar.get(Calendar.MONTH), calendar.get(Calendar.YEAR)};
    }


    /**
     * Převede číslo long na textové datum ve formátu dd.MM.yyyy.
     *
     * @param l datum v milisekundách
     * @return String datum ve formátu dd.MM.yyyy
     */
    public static String convertLongToDate(long l) {
        long offset = getOffsetTimeZones(l);
        return getSimpleDateFormat().format(l + offset);
    }


    /**
     * Převede textové datum ve formátu ISO s časovým posunem na datum dd.MM.yyyy.
     *
     * @param isoDateTime datum ve formátu ISO s časovým posunem
     * @return String datum ve formátu dd.MM.yyyy
     */
    public static String convertFormat(String isoDateTime) {
        DateTimeFormatter inputFormatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
        DateTimeFormatter outputFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");

        LocalDateTime dateTime = LocalDateTime.parse(isoDateTime, inputFormatter);

        return dateTime.format(outputFormatter);
    }


    /**
     * Vrátí rok z data v milisekundách jako celé číslo.
     *
     * @param l datum v milisekundách
     * @return rok jako int
     */
    public static int yearIntOfLong(long l) {
        long offset = getOffsetTimeZones(l);
        return Integer.parseInt(getSimpleDateFormat("yyyy").format(l + offset));
    }


    /**
     * Vypočítá offset časových pásem k českému časovému pásmu.
     *
     * @param l datum v milisekundách
     * @return offset v milisekundách pro dané datum
     */
    public static long getOffsetTimeZones(long l) {
        long offsetDef = TimeZone.getDefault().getOffset(l) * (-1);
        long offsetPra = TimeZone.getTimeZone("Europe/Prague").getOffset(l);

        return offsetDef + offsetPra;
    }

}
