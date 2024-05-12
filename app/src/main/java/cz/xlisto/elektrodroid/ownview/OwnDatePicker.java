package cz.xlisto.elektrodroid.ownview;


import android.app.DatePickerDialog;
import android.content.Context;

import java.util.Calendar;

import cz.xlisto.elektrodroid.modules.pricelist.PriceListAddFragment;


/**
 * Vlastní dialog pro výběr data
 */
public class OwnDatePicker {

    private static String TAG = PriceListAddFragment.class.getSimpleName();


    public static void showDialog(Context context, OnDateListener onDateListener, String oldDate) {
        Calendar calendarOld = Calendar.getInstance();
        try {
            //Date date = ViewHelper.parseDate(oldDate);
            calendarOld = ViewHelper.parseCalendarFromString(oldDate);
        } catch (Exception e) {
            e.printStackTrace();
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


    public interface OnDateListener {

        void getDate(String date);

    }

}


