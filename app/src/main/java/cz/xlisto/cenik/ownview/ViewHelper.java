package cz.xlisto.cenik.ownview;

import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.content.Context;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;

public class ViewHelper {
    /**
     * Přepočítá pixely v dp na skutečné pixely podle hustoty obrazovky
     * @param dp
     * @param context
     * @return
     */
    public static int convertDpToPx(int dp, Context context) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dp * scale + 0.5f);
    }

    /**
     * Vrátí instanci SimpleDateFormat s výchozím petternem dd.MM.yyyy
     * @return
     */
    public static SimpleDateFormat getSimpleDateFormat() {
        return new SimpleDateFormat("dd.MM.yyyy");
    }

    /**
     * Vrátí instanci SimpleDateFormat s možností definovat svůj vlastní pattern
     * příklad: dd.MM.yyyy (25.12.2020)
     * @param pattern
     * @return
     */
    public static SimpleDateFormat getSimpleDateFormat(String pattern) {
        return new SimpleDateFormat(pattern);
    }

    /**
     * Převede textový datum ve formátu dd.MM.yyyy do objektu Calendar.
     * Pokud se vyskytne chyba, vrací null.
     * @param string
     * @return
     */
    public static Calendar parseCalendarFromString(String string) {
        Calendar calendar = Calendar.getInstance();
        try {
            calendar.setTime(getSimpleDateFormat().parse(string));
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return  calendar;
    }

    /**
     * Převede objekt Calendar na textové datum formátu dd.MM.yyyy
     * @param calendar
     * @return
     */
    public static String parseStringFromCalendar(Calendar calendar) {
        return getSimpleDateFormat().format(calendar.getTime());
    }

    /**
     * Vrátí obejkt Calendar jako pole int v tomto pořadí {dd,MM,YYYY}
     * @param calendar
     * @return
     */
    public static int[] parseIntsFromCalendar(Calendar calendar) {
        return new int[]{calendar.get(Calendar.DAY_OF_MONTH),calendar.get(Calendar.MONDAY),calendar.get(Calendar.YEAR)};
    }

    /**
     * vrátí dnešní datum jako string ve formátu dd.MM.yyyy
     * @return
     */
    public static String getTodayDate(){
        Calendar calendar = Calendar.getInstance();
        return parseStringFromCalendar(calendar);
    }

    /**
     * Převede číslo long na textové datum ve formátu dd.MM.yyyy
     * @param l
     * @return
     */
    public static String convertLongToTime(long l) {
        return getSimpleDateFormat().format(l);
    }

    public static String yearOfLong(long l) {
        return getSimpleDateFormat("yyyy").format(l);
    }

    public static int yearIntOfLong(long l) {
        return Integer.parseInt(getSimpleDateFormat("yyyy").format(l));
    }



}

