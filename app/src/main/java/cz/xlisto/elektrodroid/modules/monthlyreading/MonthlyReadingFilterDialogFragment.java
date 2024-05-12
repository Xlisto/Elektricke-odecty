package cz.xlisto.elektrodroid.modules.monthlyreading;


import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import java.util.Calendar;

import cz.xlisto.elektrodroid.R;

/**
 * Xlisto 19.05.2023 11:21
 */
public class MonthlyReadingFilterDialogFragment extends DialogFragment {
    private static final String TAG = "MonthlyReadingFilterDialogFragment";
    public static final String MONTHLY_READING_FILTER = "monthlyReadingFilter";
    public static final String FROM = "from";
    public static final String TO = "to";

    public MonthlyReadingFilterDialogFragment() {
        // Required empty public constructor
    }

    public static MonthlyReadingFilterDialogFragment newInstance(long from, long to) {
        MonthlyReadingFilterDialogFragment fragment = new MonthlyReadingFilterDialogFragment();
        Bundle args = new Bundle();
        args.putLong(FROM, from);
        args.putLong(TO, to);
        fragment.setArguments(args);
        return fragment;
    }


    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {


        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
        View view = requireActivity().getLayoutInflater().inflate(R.layout.fragment_monthly_reading_filter_dialog, null);

        Button btnFilter = view.findViewById(R.id.btnMonthlyReadingFilterReset);

        Calendar calendarFrom = Calendar.getInstance();
        Calendar calendarTo = Calendar.getInstance();

        calendarFrom.set(Calendar.YEAR, calendarFrom.get(Calendar.YEAR) - 1);
        calendarFrom.set(Calendar.DAY_OF_MONTH, 1);

        DatePicker datePickerFrom = view.findViewById(R.id.dpMonthlyReadingFrom);
        DatePicker datePickerTo = view.findViewById(R.id.dpMonthlyReadingTo);

        if (getArguments() != null) {
            long from = getArguments().getLong(FROM);
            long to = getArguments().getLong(TO);
            if (from > 0 && to < Long.MAX_VALUE) {
                calendarFrom.setTimeInMillis(from);
                calendarTo.setTimeInMillis(to);
                datePickerTo.init(calendarTo.get(Calendar.YEAR), calendarTo.get(Calendar.MONTH), calendarTo.get(Calendar.DAY_OF_MONTH), null);

            }
        }

        datePickerFrom.init(calendarFrom.get(Calendar.YEAR), calendarFrom.get(Calendar.MONTH), calendarFrom.get(Calendar.DAY_OF_MONTH), null);

        builder.setView(view);
        builder.setPositiveButton("Filtrovat", (dialog, which) -> {
            long from = getMillisFromDatePicker(datePickerFrom);
            long to = getMillisFromDatePicker(datePickerTo);
            Bundle bundle = new Bundle();
            bundle.putLong(FROM, from);
            bundle.putLong(TO, to);
            getParentFragmentManager().setFragmentResult(MONTHLY_READING_FILTER, bundle);
        });
        btnFilter.setOnClickListener(v -> {
            Bundle bundle = new Bundle();
            bundle.putLong(FROM, 0L);
            bundle.putLong(TO, Long.MAX_VALUE);
            getParentFragmentManager().setFragmentResult(MONTHLY_READING_FILTER, bundle);
            dismiss();
        });
        builder.setNegativeButton("Zrušit", (dialog, which) -> {
        });
        return builder.create();
    }


    /**
     * Vrátí milisekundy z DatePickeru
     *
     * @param datePicker DatePicker
     * @return long
     */
    private long getMillisFromDatePicker(DatePicker datePicker) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.YEAR, datePicker.getYear());
        calendar.set(Calendar.MONTH, datePicker.getMonth());
        calendar.set(Calendar.DAY_OF_MONTH, datePicker.getDayOfMonth());
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }
}
