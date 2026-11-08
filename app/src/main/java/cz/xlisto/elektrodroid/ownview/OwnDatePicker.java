package cz.xlisto.elektrodroid.ownview;

import android.app.DatePickerDialog;
import android.content.Context;
import android.util.Log;

import androidx.fragment.app.FragmentActivity;

import com.google.android.material.datepicker.MaterialDatePicker;

import java.util.Calendar;
import java.util.TimeZone;

import cz.xlisto.elektrodroid.R;

/**
 * Vlastní dialog pro výběr data s kalendářním výběrem (Material 3).
 */
public class OwnDatePicker {

    /**
     * Zobrazí dialog pro výběr data.
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
            Log.e("OwnDatePicker", "" + e);
        }

        if (context instanceof FragmentActivity activity) {
            // MaterialDatePicker očekává UTC timestamp
            Calendar utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
            utcCal.clear();
            utcCal.set(calendarOld.get(Calendar.YEAR), calendarOld.get(Calendar.MONTH), calendarOld.get(Calendar.DAY_OF_MONTH));
            long selection = utcCal.getTimeInMillis();

            MaterialDatePicker<Long> datePicker = MaterialDatePicker.Builder.datePicker()
                    .setTitleText(R.string.nastavit_datum)
                    .setSelection(selection)
                    .build();

            datePicker.addOnPositiveButtonClickListener(selectionTime -> {
                Calendar calendarNew = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
                calendarNew.setTimeInMillis(selectionTime);
                String newDate = calendarNew.get(Calendar.DAY_OF_MONTH) + "." + (calendarNew.get(Calendar.MONTH) + 1) + "." + calendarNew.get(Calendar.YEAR);
                onDateListener.getDate(newDate);
            });

            datePicker.show(activity.getSupportFragmentManager(), "MaterialDatePicker");
            return;
        }

        // Fallback pro situace bez FragmentActivity
        DatePickerDialog dpd = new DatePickerDialog(context, (view, year, monthOfYear, dayOfMonth) -> {
            String newDate = dayOfMonth + "." + (monthOfYear + 1) + "." + year;
            onDateListener.getDate(newDate);
        }, calendarOld.get(Calendar.YEAR), calendarOld.get(Calendar.MONTH), calendarOld.get(Calendar.DAY_OF_MONTH));
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
