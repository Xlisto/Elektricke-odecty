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
import android.widget.Button;
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
import java.util.Arrays;
import java.util.Calendar;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

import cz.xlisto.elektrodroid.R;
import cz.xlisto.elektrodroid.databaze.DBHelperPriceList;
import cz.xlisto.elektrodroid.databaze.DataPriceListSource;
import cz.xlisto.elektrodroid.ownview.ViewHelper;
import cz.xlisto.elektrodroid.shp.ShPFilter;

/**
 * Dialog ve stylu Material Design 3 pro filtraci ceníků.
 * Umožňuje vícenásobný výběr položek (multi-select) pro řadu, produkt, sazbu, dodavatele i distribuční území
 * a poskytuje výběr dat platnosti pomocí kalendáře a rychlých předvoleb roků.
 */
public class PriceListFilterDialogFragment extends DialogFragment {

    public static final String FLAG_RESULT_FILTER_DIALOG_FRAGMENT = "flagPriceListFilterDialogFragment";
    public static final String RESULT = "result";

    private TextInputLayout tilRada, tilProdukt, tilSazba, tilCompany, tilArea;
    private TextInputEditText etRada, etProdukt, etSazba, etCompany, etArea;

    private TextInputLayout tilDateFrom, tilDateTo;
    private TextInputEditText etDateFrom, etDateTo;
    private TextView tvDateRangeError;
    private Chip chipAllDates, chipThisYear, chipLastYear, chipNextYear;

    private final Set<String> selectedRada = new LinkedHashSet<>();
    private final Set<String> selectedProdukt = new LinkedHashSet<>();
    private final Set<String> selectedSazba = new LinkedHashSet<>();
    private final Set<String> selectedCompany = new LinkedHashSet<>();
    private final Set<String> selectedArea = new LinkedHashSet<>();

    private ArrayList<String> optionsRada = new ArrayList<>();
    private ArrayList<String> optionsProdukt = new ArrayList<>();
    private ArrayList<String> optionsSazba = new ArrayList<>();
    private ArrayList<String> optionsCompany = new ArrayList<>();
    private ArrayList<String> optionsArea = new ArrayList<>();

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


    private void disableKeyboard(TextInputEditText editText) {
        if (editText != null) {
            editText.setFocusable(false);
            editText.setFocusableInTouchMode(false);
            editText.setLongClickable(false);
            editText.setInputType(android.text.InputType.TYPE_NULL);
        }
    }

    private void initViews(View view) {
        tilRada = view.findViewById(R.id.tilRada);
        etRada = view.findViewById(R.id.etRada);

        tilProdukt = view.findViewById(R.id.tilProdukt);
        etProdukt = view.findViewById(R.id.etProdukt);

        tilSazba = view.findViewById(R.id.tilSazba);
        etSazba = view.findViewById(R.id.etSazba);

        tilCompany = view.findViewById(R.id.tilCompany);
        etCompany = view.findViewById(R.id.etCompany);

        tilArea = view.findViewById(R.id.tilArea);
        etArea = view.findViewById(R.id.etArea);

        tilDateFrom = view.findViewById(R.id.tilDateFrom);
        tilDateTo = view.findViewById(R.id.tilDateTo);
        etDateFrom = view.findViewById(R.id.etDateFrom);
        etDateTo = view.findViewById(R.id.etDateTo);
        tvDateRangeError = view.findViewById(R.id.tvDateRangeError);

        chipAllDates = view.findViewById(R.id.chipAllDates);
        chipThisYear = view.findViewById(R.id.chipThisYear);
        chipLastYear = view.findViewById(R.id.chipLastYear);
        chipNextYear = view.findViewById(R.id.chipNextYear);

        disableKeyboard(etRada);
        disableKeyboard(etProdukt);
        disableKeyboard(etSazba);
        disableKeyboard(etCompany);
        disableKeyboard(etArea);
        disableKeyboard(etDateFrom);
        disableKeyboard(etDateTo);
    }

    private void setupListeners(View view) {
        MaterialButton btnOpenRangePicker = view.findViewById(R.id.btnOpenRangePicker);
        Button btnResetFilter = view.findViewById(R.id.btnResetFilter);

        View.OnClickListener clickRada = v -> openMultiChoiceDialog(R.string.sp_produktova_rada, optionsRada, selectedRada, etRada);
        etRada.setOnClickListener(clickRada);
        tilRada.setEndIconOnClickListener(clickRada);

        View.OnClickListener clickProdukt = v -> openMultiChoiceDialog(R.string.sp_produkt, optionsProdukt, selectedProdukt, etProdukt);
        etProdukt.setOnClickListener(clickProdukt);
        tilProdukt.setEndIconOnClickListener(clickProdukt);

        View.OnClickListener clickSazba = v -> openMultiChoiceDialog(R.string.sazba_distribuce, optionsSazba, selectedSazba, etSazba);
        etSazba.setOnClickListener(clickSazba);
        tilSazba.setEndIconOnClickListener(clickSazba);

        View.OnClickListener clickCompany = v -> openMultiChoiceDialog(R.string.sp_dodavatele, optionsCompany, selectedCompany, etCompany);
        etCompany.setOnClickListener(clickCompany);
        tilCompany.setEndIconOnClickListener(clickCompany);

        View.OnClickListener clickArea = v -> openMultiChoiceDialog(R.string.distribucni_uzemi, optionsArea, selectedArea, etArea);
        etArea.setOnClickListener(clickArea);
        tilArea.setEndIconOnClickListener(clickArea);

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

        btnResetFilter.setOnClickListener(v -> resetAllFilters());
    }


    private void openMultiChoiceDialog(
            int titleRes,
            ArrayList<String> optionsList,
            Set<String> selectedSet,
            TextInputEditText editText
    ) {
        if (optionsList == null || optionsList.isEmpty()) return;

        CharSequence[] items = optionsList.toArray(new CharSequence[0]);
        boolean[] checkedItems = new boolean[items.length];

        if (selectedSet.isEmpty() || selectedSet.contains(VSE) || selectedSet.contains("%")) {
            checkedItems[0] = true;
        } else {
            for (int idx = 0; idx < items.length; idx++) {
                if (selectedSet.contains(items[idx].toString())) {
                    checkedItems[idx] = true;
                }
            }
        }

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext(), R.style.DialogTheme);
        builder.setTitle(titleRes);
        builder.setMultiChoiceItems(items, checkedItems, (dialog, which, isChecked) -> {
            if (which == 0) {
                if (isChecked) {
                    for (int idx = 1; idx < checkedItems.length; idx++) {
                        checkedItems[idx] = false;
                        ((AlertDialog) dialog).getListView().setItemChecked(idx, false);
                    }
                }
            } else {
                if (isChecked) {
                    checkedItems[0] = false;
                    ((AlertDialog) dialog).getListView().setItemChecked(0, false);
                }
            }
        });

        builder.setPositiveButton(R.string.vybrat, (dialog, which) -> {
            selectedSet.clear();
            boolean isAllChecked = checkedItems[0];
            if (!isAllChecked) {
                for (int idx = 1; idx < checkedItems.length; idx++) {
                    if (checkedItems[idx]) {
                        selectedSet.add(items[idx].toString());
                    }
                }
            }

            updateFieldText(selectedSet, editText);
        });

        builder.setNegativeButton(R.string.zrusit, null);
        AlertDialog dialog = builder.create();
        dialog.show();
    }


    private void updateFieldText(Set<String> selectedSet, TextInputEditText editText) {
        if (selectedSet == null || selectedSet.isEmpty() || selectedSet.contains(VSE) || selectedSet.contains("%")) {
            editText.setText(VSE);
        } else {
            editText.setText(String.join(", ", selectedSet));
        }
    }

    private void setDataFromDatabase() {
        DataPriceListSource dataPriceListSource = new DataPriceListSource(requireContext());
        dataPriceListSource.open();
        optionsRada = dataPriceListSource.readPriceListCount(DBHelperPriceList.RADA);
        optionsProdukt = dataPriceListSource.readPriceListCount(DBHelperPriceList.PRODUKT);
        optionsSazba = dataPriceListSource.readPriceListCount(DBHelperPriceList.SAZBA);
        optionsCompany = dataPriceListSource.readPriceListCount(DBHelperPriceList.FIRMA);
        optionsArea = dataPriceListSource.readPriceListCount(DBHelperPriceList.DISTRIBUCE);
        dataPriceListSource.close();

        updateFieldText(selectedRada, etRada);
        updateFieldText(selectedProdukt, etProdukt);
        updateFieldText(selectedSazba, etSazba);
        updateFieldText(selectedCompany, etCompany);
        updateFieldText(selectedArea, etArea);
    }


    private String formatSetToPreference(Set<String> set) {
        if (set == null || set.isEmpty() || set.contains(VSE) || set.contains("%")) {
            return "%";
        }
        return String.join(",", set);
    }


    private void parsePreferenceToSet(String prefValue, Set<String> targetSet) {
        targetSet.clear();
        if (prefValue == null || prefValue.equals("%") || prefValue.equalsIgnoreCase(VSE) || prefValue.trim().isEmpty()) {
            targetSet.add(VSE);
            return;
        }

        if (prefValue.contains(",")) {
            String[] parts = prefValue.split(",");
            for (String p : parts) {
                String trimmed = p.trim();
                if (!trimmed.isEmpty() && !trimmed.equals("%") && !trimmed.equalsIgnoreCase(VSE)) {
                    targetSet.add(trimmed);
                }
            }
        } else {
            targetSet.add(prefValue.trim());
        }

        if (targetSet.isEmpty()) {
            targetSet.add(VSE);
        }
    }

    private void savePreferences() {
        shpFilter.set(RADA, formatSetToPreference(selectedRada));
        shpFilter.set(PRODUKT, formatSetToPreference(selectedProdukt));
        shpFilter.set(SAZBA, formatSetToPreference(selectedSazba));
        shpFilter.set(COMPANY, formatSetToPreference(selectedCompany));
        shpFilter.set(AREA, formatSetToPreference(selectedArea));
        shpFilter.set(DATE_START, dateStart);
        shpFilter.set(DATE_END, dateEnd);
    }

    private void loadPreferences() {
        parsePreferenceToSet(shpFilter.get(RADA, VSE), selectedRada);
        parsePreferenceToSet(shpFilter.get(PRODUKT, VSE), selectedProdukt);
        parsePreferenceToSet(shpFilter.get(SAZBA, VSE), selectedSazba);
        parsePreferenceToSet(shpFilter.get(COMPANY, VSE), selectedCompany);
        parsePreferenceToSet(shpFilter.get(AREA, VSE), selectedArea);

        dateStart = shpFilter.get(DATE_START, VSE);
        dateEnd = shpFilter.get(DATE_END, VSE);
    }


    private void resetAllFilters() {
        selectedRada.clear();
        selectedRada.add(VSE);

        selectedProdukt.clear();
        selectedProdukt.add(VSE);

        selectedSazba.clear();
        selectedSazba.add(VSE);

        selectedCompany.clear();
        selectedCompany.add(VSE);

        selectedArea.clear();
        selectedArea.add(VSE);

        updateFieldText(selectedRada, etRada);
        updateFieldText(selectedProdukt, etProdukt);
        updateFieldText(selectedSazba, etSazba);
        updateFieldText(selectedCompany, etCompany);
        updateFieldText(selectedArea, etArea);

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


    String formatDate(LocalDate date) {
        return String.format(Locale.GERMANY, "%02d.%02d.%04d", date.getDayOfMonth(), date.getMonthValue(), date.getYear());
    }
}
