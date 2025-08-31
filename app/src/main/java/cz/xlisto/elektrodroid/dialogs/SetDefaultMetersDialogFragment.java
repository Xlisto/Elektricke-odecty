package cz.xlisto.elektrodroid.dialogs;


import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.view.View;
import android.widget.CalendarView;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;

import java.util.Calendar;

import cz.xlisto.elektrodroid.R;
import cz.xlisto.elektrodroid.ownview.LabelEditText;


/**
 * Dialogové okno pro nastavení výchozích stavů měřičů, pro případ, když není použitá faktura
 * Xlisto 02.04.2024 21:53
 */
public class SetDefaultMetersDialogFragment extends DialogFragment {

    private static final String TAG = "SetDefaultMetersDialogFragment";
    public static final String FLAG_RESULT_DIALOG_FRAGMENT = "flagResultSetDefaultMetersDialogFragment";
    public static final String RESULT = "result";
    private static final String ARG_FIRST_METERS = "firstMeters";
    private LabelEditText labVT, labNT;
    private Calendar calendar;


    public static SetDefaultMetersDialogFragment newInstance(String firstMeters) {
        SetDefaultMetersDialogFragment fragment = new SetDefaultMetersDialogFragment();
        Bundle args = new Bundle();
        args.putString(ARG_FIRST_METERS, firstMeters);
        fragment.setArguments(args);
        return fragment;
    }


    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        View view = View.inflate(requireContext(), R.layout.dialog_set_first_meters, null);
        labVT = view.findViewById(R.id.letFirstVT);
        labNT = view.findViewById(R.id.letFirstNT);
        CalendarView calendarView = view.findViewById(R.id.cvFirstCalendar);
        calendar = Calendar.getInstance();

        if (getArguments() != null) {
            String argument = getArguments().getString(ARG_FIRST_METERS);
            if (argument != null) {
                if (!argument.isEmpty()) {
                    String[] data = argument.split(";");

                    calendar.setTimeInMillis(Long.parseLong(data[0]));
                    labVT.setDefaultText(data[1]);
                    labNT.setDefaultText(data[2]);

                    calendarView.setDate(calendar.getTimeInMillis());
                }
            }
        }

        if (savedInstanceState != null) {
            labVT.setDefaultText(savedInstanceState.getString("vt"));
            labNT.setDefaultText(savedInstanceState.getString("nt"));
            calendar.setTimeInMillis(savedInstanceState.getLong("calendar"));
            calendarView.setDate(calendar.getTimeInMillis());
        }

        calendarView.setOnDateChangeListener((view1, year, month, dayOfMonth) -> {
            calendar.set(year, month, dayOfMonth);
            //calendar.add(Calendar.DAY_OF_MONTH, -1);
        });

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle(getResources().getString(R.string.set_first_meters_title));
        builder.setView(view);
        builder.setIcon(R.drawable.ic_warning_png);
        builder.setPositiveButton(getResources().getString(R.string.ok), (dialog, which) ->
                saveData()
        );
        builder.setNegativeButton(getResources().getString(R.string.zrusit), (dialog, which) ->
                closeDialog()
        );
        return builder.create();
    }


    @Override
    public void onDetach() {
        super.onDetach();
        closeDialog();
    }


    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("vt", labVT.getText());
        outState.putString("nt", labNT.getText());
        outState.putLong("calendar", calendar.getTimeInMillis());
    }


    /**
     * Sestaví data a uloží je do databáze
     */
    private void saveData() {
        String vt = labVT.getText().isEmpty() ? "0" : labVT.getText();
        String nt = labNT.getText().isEmpty() ? "0" : labNT.getText();
        String saveData = calendar.getTimeInMillis() + ";" + vt.replace(",", ".") + ";" + nt.replace(",", ".");
        Bundle bundle = new Bundle();
        bundle.putString(RESULT, saveData);
        getParentFragmentManager().setFragmentResult(FLAG_RESULT_DIALOG_FRAGMENT, bundle);
    }


    /**
     * Zavolá listener pro zavření okna
     */
    private void closeDialog() {
        getParentFragmentManager().setFragmentResult(FLAG_RESULT_DIALOG_FRAGMENT, new Bundle());
    }

}
