package cz.xlisto.elektrodroid.dialogs;

import static cz.xlisto.elektrodroid.databaze.DataPriceListSource.VSE;
import static cz.xlisto.elektrodroid.shp.ShPFilter.AREA;
import static cz.xlisto.elektrodroid.shp.ShPFilter.COMPANY;
import static cz.xlisto.elektrodroid.shp.ShPFilter.DATE_END;
import static cz.xlisto.elektrodroid.shp.ShPFilter.DATE_START;
import static cz.xlisto.elektrodroid.shp.ShPFilter.PRODUKT;
import static cz.xlisto.elektrodroid.shp.ShPFilter.RADA;
import static cz.xlisto.elektrodroid.shp.ShPFilter.SAZBA;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.util.Pair;
import androidx.fragment.app.DialogFragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

import cz.xlisto.elektrodroid.R;
import cz.xlisto.elektrodroid.databaze.DBHelperPriceList;
import cz.xlisto.elektrodroid.databaze.DataPriceListSource;
import cz.xlisto.elektrodroid.ownview.ViewHelper;
import cz.xlisto.elektrodroid.shp.ShPFilter;


/**
 * Dialog ve stylu Material Design 3 pro filtraci ceníků.
 * Umožňuje filtrování podle řady, produktu, sazby, dodavatele, distribučního území
 * a nově poskytuje moderní výběr dat platnosti pomocí kalendáře, ručního vstupu
 * a rychlých předvoleb roků.
 */
public class PriceListFilterDialogFragment extends DialogFragment {

    public static final String FLAG_RESULT_FILTER_DIALOG_FRAGMENT = "flagPriceListFilterDialogFragment";
    public static final String RESULT = "result";

    private Spinner spRada, spProdukt, spSazba, spCompany, spArea;
    private TextInputLayout tilDateFrom, tilDateTo;
    private TextInputEditText etDateFrom, etDateTo;
    private TextView tvDateRangeError;
    private Chip chipAllDates, chipThisYear, chipLastYear, chipNextYear;

    private String rada, produkt, sazba, dodavatel, uzemi;
    private String dateStart = "%", dateEnd = "%";

    private ShPFilter shpFilter;
    private boolean isUserSelectingPreset = false;

    public PriceListFilterDialogFragment() {
        // Vyžadovaný prázdný veřejný konstruktor
    }

    public static PriceListFilterDialogFragment newInstance() {
        return new PriceListFilterDialogFragment();
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        shpFilter = new ShPFilter(getContext());
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext(), R.style.DialogTheme);
        View dialogView = getLayoutInflater().inflate(R.layout.fragment_price_list_filter, null);

        initViews(dialogView);
        setupListeners(dialogView);

        builder.setView(dialogView);
        builder.setTitle(R.string.filtr_ceniku);
        builder.setPositiveButton(R.string.filtr_nastav, null);
        builder.setNegativeButton(R.string.zrusit, (dialog, which) -> {
        });

        loadPreferences();
        setDataFromDatabase();
        updateDateUiState();

        AlertDialog dialog = builder.create();
        dialog.setOnShowListener(d -> {
            Button positiveButton = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
            if (positiveButton != null) {
                positiveButton.setOnClickListener(v -> {
                    if (isDateRangeValid()) {
                        savePreferences();
                        Bundle bundle = new Bundle();
                        bundle.putBoolean(RESULT, true);
                        getParentFragmentManager().setFragmentResult(FLAG_RESULT_FILTER_DIALOG_FRAGMENT, bundle);
                        dialog.dismiss();
                    }
                });
            }
        });

        return dialog;
    }

    @Override
    public void onStart() {
        super.onStart();
        DialogButtonColorHelper.apply(this);
    }


    private void initViews(View view) {
        spRada = view.findViewById(R.id.spRada);
        spProdukt = view.findViewById(R.id.spProdukt);
        spSazba = view.findViewById(R.id.spSazba);
        spCompany = view.findViewById(R.id.spCompany);
        spArea = view.findViewById(R.id.spArea);

        tilDateFrom = view.findViewById(R.id.tilDateFrom);
        tilDateTo = view.findViewById(R.id.tilDateTo);
        etDateFrom = view.findViewById(R.id.etDateFrom);
        etDateTo = view.findViewById(R.id.etDateTo);
        tvDateRangeError = view.findViewById(R.id.tvDateRangeError);

        chipAllDates = view.findViewById(R.id.chipAllDates);
        chipThisYear = view.findViewById(R.id.chipThisYear);
        chipLastYear = view.findViewById(R.id.chipLastYear);
        chipNextYear = view.findViewById(R.id.chipNextYear);
    }


    private void setupListeners(View view) {
        MaterialButton btnOpenRangePicker = view.findViewById(R.id.btnOpenRangePicker);
        Button btnResetFilter = view.findViewById(R.id.btnResetFilter);

        View.OnClickListener listenerFrom = v -> openDatePickerFrom();
        etDateFrom.setOnClickListener(listenerFrom);
        tilDateFrom.setEndIconOnClickListener(listenerFrom);

        View.OnClickListener listenerTo = v -> openDatePickerTo();
        etDateTo.setOnClickListener(listenerTo);
        tilDateTo.setEndIconOnClickListener(listenerTo);

        btnOpenRangePicker.setOnClickListener(v -> openRangePicker());

        View.OnClickListener presetListener = v -> {
            if (isUserSelectingPreset) return;
            int id = v.getId();
            if (id == R.id.chipAllDates) {
                applyPresetAllDates();
            } else if (id == R.id.chipThisYear) {
                applyPresetThisYear();
            } else if (id == R.id.chipLastYear) {
                applyPresetLastYear();
            } else if (id == R.id.chipNextYear) {
                applyPresetNextYear();
            }
        };

        chipAllDates.setOnClickListener(presetListener);
        chipThisYear.setOnClickListener(presetListener);
        chipLastYear.setOnClickListener(presetListener);
        chipNextYear.setOnClickListener(presetListener);

        btnResetFilter.setOnClickListener(v -> setItemSpinnerAll());
    }

    private void setDataFromDatabase() {
        DataPriceListSource dataPriceListSource = new DataPriceListSource(requireContext());
        dataPriceListSource.open();
        ArrayList<String> arrayListRada = dataPriceListSource.readPriceListCount(DBHelperPriceList.RADA);
        ArrayList<String> arrayListProdukt = dataPriceListSource.readPriceListCount(DBHelperPriceList.PRODUKT);
        ArrayList<String> arrayListSazba = dataPriceListSource.readPriceListCount(DBHelperPriceList.SAZBA);
        ArrayList<String> arrayListDodavatel = dataPriceListSource.readPriceListCount(DBHelperPriceList.FIRMA);
        ArrayList<String> arrayListUzemi = dataPriceListSource.readPriceListCount(DBHelperPriceList.DISTRIBUCE);
        dataPriceListSource.close();

        spRada.setAdapter(setStringAdapter(arrayListRada));
        spProdukt.setAdapter(setStringAdapter(arrayListProdukt));
        spSazba.setAdapter(setStringAdapter(arrayListSazba));
        spCompany.setAdapter(setStringAdapter(arrayListDodavatel));
        spArea.setAdapter(setStringAdapter(arrayListUzemi));

        setItemSpinner(arrayListRada, spRada, rada);
        setItemSpinner(arrayListProdukt, spProdukt, produkt);
        setItemSpinner(arrayListSazba, spSazba, sazba);
        setItemSpinner(arrayListDodavatel, spCompany, dodavatel);
        setItemSpinner(arrayListUzemi, spArea, uzemi);
    }

    private ArrayAdapter<String> setStringAdapter(ArrayList<String> arrayList) {
        return new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, arrayList);
    }


    private void setItemSpinner(ArrayList<String> arrayList, Spinner sp, String item) {
        for (int i = 0; i < arrayList.size(); i++) {
            if (arrayList.get(i).equals(item)) {
                sp.setSelection(i);
            }
        }
    }

    private void savePreferences() {
        rada = spRada.getSelectedItem().toString();
        produkt = spProdukt.getSelectedItem().toString();
        sazba = spSazba.getSelectedItem().toString();
        dodavatel = spCompany.getSelectedItem().toString();
        uzemi = spArea.getSelectedItem().toString();

        if (rada.equals(VSE)) rada = "%";
        if (produkt.equals(VSE)) produkt = "%";
        if (sazba.equals(VSE)) sazba = "%";
        if (dodavatel.equals(VSE)) dodavatel = "%";
        if (uzemi.equals(VSE)) uzemi = "%";

        shpFilter.set(RADA, rada);
        shpFilter.set(PRODUKT, produkt);
        shpFilter.set(SAZBA, sazba);
        shpFilter.set(COMPANY, dodavatel);
        shpFilter.set(AREA, uzemi);
        shpFilter.set(DATE_START, dateStart);
        shpFilter.set(DATE_END, dateEnd);
    }

    private void loadPreferences() {
        rada = shpFilter.get(RADA, VSE);
        produkt = shpFilter.get(PRODUKT, VSE);
        sazba = shpFilter.get(SAZBA, VSE);
        dodavatel = shpFilter.get(COMPANY, VSE);
        uzemi = shpFilter.get(AREA, VSE);
        dateStart = shpFilter.get(DATE_START, VSE);
        dateEnd = shpFilter.get(DATE_END, VSE);
    }

    private void setItemSpinnerAll() {
        spRada.setSelection(0);
        spSazba.setSelection(0);
        spProdukt.setSelection(0);
        spCompany.setSelection(0);
        spArea.setSelection(0);
        applyPresetAllDates();
    }


    private void applyPresetAllDates() {
        dateStart = "%";
        dateEnd = "%";
        updateDateUiState();
    }


    private void applyPresetThisYear() {
        int currentYear = LocalDate.now().getYear();
        dateStart = "01.01." + currentYear;
        dateEnd = "31.12." + currentYear;
        updateDateUiState();
    }


    private void applyPresetLastYear() {
        int lastYear = LocalDate.now().getYear() - 1;
        dateStart = "01.01." + lastYear;
        dateEnd = "31.12." + lastYear;
        updateDateUiState();
    }


    private void applyPresetNextYear() {
        int nextYear = LocalDate.now().getYear() + 1;
        dateStart = "01.01." + nextYear;
        dateEnd = "31.12." + nextYear;
        updateDateUiState();
    }


    private void openDatePickerFrom() {
        long initialUtcMillis = parseToUtcMillis(dateStart);
        MaterialDatePicker<Long> datePicker = MaterialDatePicker.Builder.datePicker()
                .setTitleText(R.string.datum_od)
                .setSelection(initialUtcMillis)
                .build();

        datePicker.addOnPositiveButtonClickListener(selection -> {
            if (selection != null) {
                LocalDate selectedDate = Instant.ofEpochMilli(selection).atZone(ZoneOffset.UTC).toLocalDate();
                dateStart = formatDate(selectedDate);
                updateDateUiState();
            }
        });
        datePicker.show(getParentFragmentManager(), "DATE_PICKER_FROM");
    }


    private void openDatePickerTo() {
        long initialUtcMillis = parseToUtcMillis(dateEnd);
        MaterialDatePicker<Long> datePicker = MaterialDatePicker.Builder.datePicker()
                .setTitleText(R.string.datum_do)
                .setSelection(initialUtcMillis)
                .build();

        datePicker.addOnPositiveButtonClickListener(selection -> {
            if (selection != null) {
                LocalDate selectedDate = Instant.ofEpochMilli(selection).atZone(ZoneOffset.UTC).toLocalDate();
                dateEnd = formatDate(selectedDate);
                updateDateUiState();
            }
        });
        datePicker.show(getParentFragmentManager(), "DATE_PICKER_TO");
    }


    private void openRangePicker() {
        long startMillis = parseToUtcMillis(dateStart);
        long endMillis = parseToUtcMillis(dateEnd);

        Pair<Long, Long> selection = new Pair<>(startMillis, endMillis);

        MaterialDatePicker<Pair<Long, Long>> rangePicker = MaterialDatePicker.Builder.dateRangePicker()
                .setTitleText(R.string.vybrat_rozsah_v_kalendari)
                .setSelection(selection)
                .build();

        rangePicker.addOnPositiveButtonClickListener(selectedRange -> {
            if (selectedRange != null && selectedRange.first != null && selectedRange.second != null) {
                LocalDate start = Instant.ofEpochMilli(selectedRange.first).atZone(ZoneOffset.UTC).toLocalDate();
                LocalDate end = Instant.ofEpochMilli(selectedRange.second).atZone(ZoneOffset.UTC).toLocalDate();
                dateStart = formatDate(start);
                dateEnd = formatDate(end);
                updateDateUiState();
            }
        });
        rangePicker.show(getParentFragmentManager(), "DATE_RANGE_PICKER");
    }


    private void updateDateUiState() {
        etDateFrom.setText(dateStart.equals("%") ? "" : dateStart);
        etDateTo.setText(dateEnd.equals("%") ? "" : dateEnd);

        boolean valid = isDateRangeValid();
        tvDateRangeError.setVisibility(valid ? View.GONE : View.VISIBLE);

        isUserSelectingPreset = true;
        updateChipStates();
        isUserSelectingPreset = false;
    }


    private void updateChipStates() {
        int currentYear = LocalDate.now().getYear();

        chipAllDates.setChecked(dateStart.equals("%") && dateEnd.equals("%"));
        chipThisYear.setChecked(dateStart.equals("01.01." + currentYear) && dateEnd.equals("31.12." + currentYear));
        chipLastYear.setChecked(dateStart.equals("01.01." + (currentYear - 1)) && dateEnd.equals("31.12." + (currentYear - 1)));
        chipNextYear.setChecked(dateStart.equals("01.01." + (currentYear + 1)) && dateEnd.equals("31.12." + (currentYear + 1)));
    }


    private boolean isDateRangeValid() {
        if (!dateStart.equals("%") && !dateEnd.equals("%") && !dateStart.isEmpty() && !dateEnd.isEmpty()) {
            Calendar calStart = ViewHelper.parseCalendarFromString(dateStart);
            Calendar calEnd = ViewHelper.parseCalendarFromString(dateEnd);
            return !calStart.after(calEnd);
        }
        return true;
    }


    private long parseToUtcMillis(String dateStr) {
        if (dateStr == null || dateStr.equals("%") || dateStr.trim().isEmpty()) {
            return LocalDate.now().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli();
        }
        Calendar cal = ViewHelper.parseCalendarFromString(dateStr);
        LocalDate localDate = LocalDate.of(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH));
        return localDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli();
    }


    private String formatDate(LocalDate date) {
        return String.format(Locale.GERMANY, "%02d.%02d.%04d", date.getDayOfMonth(), date.getMonthValue(), date.getYear());
    }

}
