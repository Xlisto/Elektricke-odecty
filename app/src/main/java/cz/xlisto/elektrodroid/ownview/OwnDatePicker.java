package cz.xlisto.elektrodroid.ownview;


import android.app.DatePickerDialog;
import android.content.Context;
import android.util.Log;

import java.util.Calendar;


/**
 * Vlastní dialog pro výběr data s calendarovým výběrem.
 * <p>
 * Zabaluje Android DatePickerDialog s podporou parsování
 * a formátování data ve formátu dd.MM.yyyy.
 */
public class OwnDatePicker {

    /**
     * Zobrazí dialog pro výběr data.
     * <p>
     * Uživateli umožní vybrat datum pomocí kalendárního dialógu.
     * Vybrané datum se předá do callbacku OnDateListener.
     *
     * @param context        kontext aplikace
     * @param onDateListener callback pro obdržení vybraného data
     * @param oldDate        původní datum ve formátu dd.MM.yyyy
     */
    public static void showDialog(Context context, OnDateListener onDateListener, String oldDate) {
        Calendar calendarOld = Calendar.getInstance();
        try {
            calendarOld = ViewHelper.parseCalendarFromString(oldDate);
        } catch (Exception e) {
            Log.e("OwnDatePicker", ""+e);
        }

        DatePickerDialog dpd = new DatePickerDialog(context, (view, year, monthOfYear, dayOfMonth) -> {
            String newDate = dayOfMonth + "." + (monthOfYear + 1) + "." + year;
            Calendar calendarNew = Calendar.getInstance();
            calendarNew.set(year, monthOfYear, dayOfMonth, 0, 0, 0);
            calendarNew.set(Calendar.MILLISECOND, 0);
            onDateListener.getDate(newDate);

        }, calendarOld.get(Calendar.YEAR), calendarOld.get(Calendar.MONTH), calendarOld.get(Calendar.DAY_OF_MONTH));//nastavuje výchozí hodnoty
        dpd.show();
    }


    /**
     * Callback rozhraní pro obdržení vybraného data.
     */
    public interface OnDateListener {

        /**
         * Zavolá se, když uživatel vybere datum.
         *
         * @param date vybrané datum ve formátu dd.MM.yyyy
         */
        void getDate(String date);

    }

}


