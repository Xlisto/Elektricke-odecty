package cz.xlisto.elektrodroid.dialogs;


import android.app.Dialog;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Objects;

import cz.xlisto.elektrodroid.R;
import cz.xlisto.elektrodroid.ownview.ViewHelper;


/**
 * Dialog ve stylu Material Design 3 pro výběr a nastavení platnosti ceníku (OD - DO).
 * Poskytuje možnost ruční volby data, vybraného rozsahu v kalendáři a rychlých předvoleb
 * (Minulý rok, Tento rok, Následující rok), které vždy nastaví období od 1. 1. do 31. 12. daného roku.
 */
public class PriceListValidityDialogFragment extends DialogFragment {

    /**
     * Klíč výsledku fragmentu předávaného skrze FragmentManager.
     */
    public static final String PRICE_LIST_VALIDITY = "priceListValidity";

    /**
     * Klíč pro počáteční datum ve formátu textového řetězce.
     */
    public static final String DATE_FROM = "dateFrom";

    /**
     * Klíč pro koncové datum ve formátu textového řetězce.
     */
    public static final String DATE_TO = "dateTo";

    /**
     * Počáteční vybrané datum platnosti ceníku.
     */
    private LocalDate localDateFrom;

    /**
     * Koncové vybrané datum platnosti ceníku.
     */
    private LocalDate localDateTo;

    /**
     * Obalovací prvek {@link TextInputLayout} pro výstupní pole počátečního data.
     */
    private TextInputLayout tilDateFrom;

    /**
     * Obalovací prvek {@link TextInputLayout} pro výstupní pole koncového data.
     */
    private TextInputLayout tilDateTo;

    /**
     * Textové pole {@link TextInputEditText} pro zobrazení počátečního data.
     */
    private TextInputEditText etDateFrom;

    /**
     * Textové pole {@link TextInputEditText} pro zobrazení koncového data.
     */
    private TextInputEditText etDateTo;

    /**
     * Textové zobrazení pro chybové hlášení při neplatném rozsahu dat.
     */
    private TextView tvDateRangeError;

    /**
     * Tlačítko rychlé předvolby pro minulý rok.
     */
    private Chip chipLastYear;

    /**
     * Tlačítko rychlé předvolby pro tento rok.
     */
    private Chip chipThisYear;

    /**
     * Tlačítko rychlé předvolby pro následující rok.
     */
    private Chip chipNextYear;

    /**
     * Příznak zabraňující cyklickému volání posluchače událostí při programové změně stavu chipů.
     */
    private boolean isUserSelectingPreset = false;


    /**
     * Výchozí prázdný veřejný konstruktor.
     */
    public PriceListValidityDialogFragment() {
        // Vyžadovaný prázdný veřejný konstruktor
    }


    /**
     * Vrátí formátovač data v požadovaném českém formátu d. M. yyyy.
     *
     * @return {@link DateTimeFormatter}
     */
    private DateTimeFormatter getDateFormatter() {
        return DateTimeFormatter.ofPattern("d. M. yyyy", Locale.getDefault());
    }


    /**
     * Tovární metoda pro vytvoření nové instance dialogu platnosti ceníku.
     *
     * @param dateFrom počáteční datum platnosti ceníku jako textový řetězec
     * @param dateTo   koncové datum platnosti ceníku jako textový řetězec
     * @return nová instance {@link PriceListValidityDialogFragment}
     */
    public static PriceListValidityDialogFragment newInstance(@Nullable String dateFrom, @Nullable String dateTo) {
        PriceListValidityDialogFragment fragment = new PriceListValidityDialogFragment();
        Bundle args = new Bundle();
        args.putString(DATE_FROM, dateFrom);
        args.putString(DATE_TO, dateTo);
        fragment.setArguments(args);
        return fragment;
    }


    /**
     * Vytvoří a nakonfiguruje dialogové okno s vlastním zobrazením.
     *
     * @param savedInstanceState uložený stav fragmentu při opětném vytvoření
     * @return vytvořená instance {@link Dialog}
     */
    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        String fromStr = null;
        String toStr = null;

        if (savedInstanceState != null) {
            fromStr = savedInstanceState.getString(DATE_FROM);
            toStr = savedInstanceState.getString(DATE_TO);
        } else if (getArguments() != null) {
            fromStr = getArguments().getString(DATE_FROM);
            toStr = getArguments().getString(DATE_TO);
        }

        initDates(fromStr, toStr);

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext(), R.style.DialogTheme);
        View view = requireActivity().getLayoutInflater().inflate(R.layout.fragment_price_list_validity_dialog, null);

        initViews(view);
        setupListeners(view);
        updateUiState();

        builder.setView(view);
        return builder.create();
    }


    /**
     * Uloží stav hranic platnosti ceníku při změně konfigurace nebo zničení fragmentu.
     *
     * @param outState bundle pro uložení stavu
     */
    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(DATE_FROM, localDateFrom.format(getDateFormatter()));
        outState.putString(DATE_TO, localDateTo.format(getDateFormatter()));
    }


    /**
     * Po zobrazení dialogu sjednotí barvy tlačítek s ostatními dialogy v aplikaci.
     */
    @Override
    public void onStart() {
        super.onStart();
        DialogButtonColorHelper.apply(this);
    }


    /**
     * Inicializuje výchozí objekty {@link LocalDate} z předaných textových řetězců.
     *
     * @param fromStr počáteční datum nebo {@code null}
     * @param toStr   koncové datum nebo {@code null}
     */
    private void initDates(@Nullable String fromStr, @Nullable String toStr) {
        int currentYear = LocalDate.now().getYear();

        if (fromStr != null && !fromStr.trim().isEmpty()) {
            try {
                localDateFrom = ViewHelper.parseCalendarFromString(fromStr)
                        .getTime().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            } catch (Exception e) {
                localDateFrom = LocalDate.of(currentYear, 1, 1);
            }
        } else {
            localDateFrom = LocalDate.of(currentYear, 1, 1);
        }

        if (toStr != null && !toStr.trim().isEmpty()) {
            try {
                localDateTo = ViewHelper.parseCalendarFromString(toStr)
                        .getTime().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            } catch (Exception e) {
                localDateTo = LocalDate.of(currentYear, 12, 31);
            }
        } else {
            localDateTo = LocalDate.of(currentYear, 12, 31);
        }
    }


    /**
     * Inicializuje zobrazení jednotlivých prvků layoutu.
     *
     * @param view kořenový náhled dialogu
     */
    private void initViews(View view) {
        tilDateFrom = view.findViewById(R.id.tilDateFrom);
        tilDateTo = view.findViewById(R.id.tilDateTo);
        etDateFrom = view.findViewById(R.id.etDateFrom);
        etDateTo = view.findViewById(R.id.etDateTo);
        tvDateRangeError = view.findViewById(R.id.tvDateRangeError);
        chipLastYear = view.findViewById(R.id.chipLastYear);
        chipThisYear = view.findViewById(R.id.chipThisYear);
        chipNextYear = view.findViewById(R.id.chipNextYear);
    }


    /**
     * Nastaví posluchače událostí pro prvky uživatelského rozhraní.
     *
     * @param view kořenový náhled dialogu
     */
    private void setupListeners(View view) {
        MaterialButton btnOpenRangePicker = view.findViewById(R.id.btnOpenRangePicker);
        MaterialButton btnCancelValidity = view.findViewById(R.id.btnCancelValidity);
        MaterialButton btnApplyValidity = view.findViewById(R.id.btnApplyValidity);

        View.OnClickListener listenerFrom = v -> openDatePickerFrom();
        etDateFrom.setOnClickListener(listenerFrom);
        tilDateFrom.setEndIconOnClickListener(listenerFrom);

        View.OnClickListener listenerTo = v -> openDatePickerTo();
        etDateTo.setOnClickListener(listenerTo);
        tilDateTo.setEndIconOnClickListener(listenerTo);

        btnOpenRangePicker.setOnClickListener(v -> openRangePicker());

        View.OnClickListener presetListener = v -> {
            if (isUserSelectingPreset) {
                return;
            }
            int checkedId = v.getId();
            if (checkedId == R.id.chipLastYear) {
                applyPresetLastYear();
            } else if (checkedId == R.id.chipThisYear) {
                applyPresetThisYear();
            } else if (checkedId == R.id.chipNextYear) {
                applyPresetNextYear();
            }
        };

        chipLastYear.setOnClickListener(presetListener);
        chipThisYear.setOnClickListener(presetListener);
        chipNextYear.setOnClickListener(presetListener);

        btnCancelValidity.setOnClickListener(v -> dismiss());
        btnApplyValidity.setOnClickListener(v -> applyValidity());
    }


    /**
     * Nastaví předvolbu platnosti ceníku pro minulý rok (1. 1. – 31. 12. minulého roku).
     */
    private void applyPresetLastYear() {
        int year = LocalDate.now().getYear() - 1;
        localDateFrom = LocalDate.of(year, 1, 1);
        localDateTo = LocalDate.of(year, 12, 31);
        updateUiState();
    }


    /**
     * Nastaví předvolbu platnosti ceníku pro tento rok (1. 1. – 31. 12. aktuálního roku).
     */
    private void applyPresetThisYear() {
        int year = LocalDate.now().getYear();
        localDateFrom = LocalDate.of(year, 1, 1);
        localDateTo = LocalDate.of(year, 12, 31);
        updateUiState();
    }


    /**
     * Nastaví předvolbu platnosti ceníku pro následující rok (1. 1. – 31. 12. příštího roku).
     */
    private void applyPresetNextYear() {
        int year = LocalDate.now().getYear() + 1;
        localDateFrom = LocalDate.of(year, 1, 1);
        localDateTo = LocalDate.of(year, 12, 31);
        updateUiState();
    }


    /**
     * Otevře {@link MaterialDatePicker} pro výběr počátečního data.
     */
    private void openDatePickerFrom() {
        long initialUtcMillis = localDateFrom.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli();

        MaterialDatePicker<Long> datePicker = MaterialDatePicker.Builder.datePicker()
                .setTitleText(R.string.datum_od)
                .setSelection(initialUtcMillis)
                .build();

        datePicker.addOnPositiveButtonClickListener(selection -> {
            if (selection != null) {
                localDateFrom = Instant.ofEpochMilli(selection).atZone(ZoneOffset.UTC).toLocalDate();
                updateUiState();
            }
        });
        datePicker.show(getParentFragmentManager(), "PRICE_LIST_DATE_PICKER_FROM");
    }


    /**
     * Otevře {@link MaterialDatePicker} pro výběr koncového data.
     */
    private void openDatePickerTo() {
        long initialUtcMillis = localDateTo.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli();

        MaterialDatePicker<Long> datePicker = MaterialDatePicker.Builder.datePicker()
                .setTitleText(R.string.datum_do)
                .setSelection(initialUtcMillis)
                .build();

        datePicker.addOnPositiveButtonClickListener(selection -> {
            if (selection != null) {
                localDateTo = Instant.ofEpochMilli(selection).atZone(ZoneOffset.UTC).toLocalDate();
                updateUiState();
            }
        });
        datePicker.show(getParentFragmentManager(), "PRICE_LIST_DATE_PICKER_TO");
    }


    /**
     * Otevře {@link MaterialDatePicker} s rozsahem pro současný výběr počátečního i koncového data.
     */
    private void openRangePicker() {
        long startUtc = localDateFrom.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli();
        long endUtc = localDateTo.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli();

        Pair<Long, Long> initialSelection = new Pair<>(startUtc, endUtc);

        MaterialDatePicker<Pair<Long, Long>> rangePicker = MaterialDatePicker.Builder.dateRangePicker()
                .setTitleText(R.string.platnost_ceniku)
                .setSelection(initialSelection)
                .build();

        rangePicker.addOnPositiveButtonClickListener(selection -> {
            if (selection != null && selection.first != null && selection.second != null) {
                localDateFrom = Instant.ofEpochMilli(selection.first).atZone(ZoneOffset.UTC).toLocalDate();
                localDateTo = Instant.ofEpochMilli(selection.second).atZone(ZoneOffset.UTC).toLocalDate();
                updateUiState();
            }
        });
        rangePicker.show(getParentFragmentManager(), "PRICE_LIST_DATE_RANGE_PICKER");
    }


    /**
     * Obnoví stav prvků uživatelského rozhraní podle aktuálních hodnot {@code localDateFrom} a {@code localDateTo}.
     */
    private void updateUiState() {
        tvDateRangeError.setVisibility(View.GONE);
        tilDateFrom.setError(null);
        tilDateTo.setError(null);

        DateTimeFormatter formatter = getDateFormatter();

        etDateFrom.setText(localDateFrom.format(formatter));
        etDateTo.setText(localDateTo.format(formatter));

        isUserSelectingPreset = true;
        Integer matchingChipId = getMatchingPresetChipId();
        chipLastYear.setChecked(matchingChipId != null && matchingChipId == R.id.chipLastYear);
        chipThisYear.setChecked(matchingChipId != null && matchingChipId == R.id.chipThisYear);
        chipNextYear.setChecked(matchingChipId != null && matchingChipId == R.id.chipNextYear);
        isUserSelectingPreset = false;
    }


    /**
     * Ověří platnost zadání a předá výsledek platnosti ceníku rodičovskému fragmentu skrze {@link androidx.fragment.app.FragmentManager}.
     */
    private void applyValidity() {
        if (localDateFrom.isAfter(localDateTo)) {
            tvDateRangeError.setText(R.string.invalid_date_range);
            tvDateRangeError.setVisibility(View.VISIBLE);
            return;
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy", Locale.getDefault());

        Bundle bundle = new Bundle();
        bundle.putString(DATE_FROM, localDateFrom.format(formatter));
        bundle.putString(DATE_TO, localDateTo.format(formatter));
        getParentFragmentManager().setFragmentResult(PRICE_LIST_VALIDITY, bundle);
        dismiss();
    }


    /**
     * Vrátí identifikátor chipu odpovídajícího aktuálně vybranému datumovému rozsahu platnosti ceníku.
     *
     * @return ID příslušného chipu z R.id nebo {@code null}, pokud zadaný rozsah neodpovídá žádné předvolbě
     */
    @Nullable
    private Integer getMatchingPresetChipId() {
        int currentYear = LocalDate.now().getYear();

        // Minulý rok (1.1. - 31.12. minulého roku)
        LocalDate lyStart = LocalDate.of(currentYear - 1, 1, 1);
        LocalDate lyEnd = LocalDate.of(currentYear - 1, 12, 31);
        if (Objects.equals(localDateFrom, lyStart) && Objects.equals(localDateTo, lyEnd)) {
            return R.id.chipLastYear;
        }

        // Tento rok (1.1. - 31.12. letošního roku)
        LocalDate tyStart = LocalDate.of(currentYear, 1, 1);
        LocalDate tyEnd = LocalDate.of(currentYear, 12, 31);
        if (Objects.equals(localDateFrom, tyStart) && Objects.equals(localDateTo, tyEnd)) {
            return R.id.chipThisYear;
        }

        // Následující rok (1.1. - 31.12. příštího roku)
        LocalDate nyStart = LocalDate.of(currentYear + 1, 1, 1);
        LocalDate nyEnd = LocalDate.of(currentYear + 1, 12, 31);
        if (Objects.equals(localDateFrom, nyStart) && Objects.equals(localDateTo, nyEnd)) {
            return R.id.chipNextYear;
        }

        return null;
    }

}
