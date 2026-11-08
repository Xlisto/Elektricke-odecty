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

import cz.xlisto.elektrodroid.R;
import cz.xlisto.elektrodroid.ownview.ViewHelper;


/**
 * Dialog ve stylu Material Design 3 pro výběr a nastavení datumového rozsahu části faktury (OD - DO).
 * Poskytuje ruční volbu data pro pole Od a Do a zrychlený výběr rozsahu v kalendáři bez přednastavených termínových tlačítek.
 */
public class InvoiceDateRangeDialogFragment extends DialogFragment {

    /**
     * Klíč výsledku fragmentu předávaného skrze FragmentManager.
     */
    public static final String INVOICE_DATE_RANGE = "invoiceDateRange";

    /**
     * Klíč pro počáteční datum ve formátu textového řetězce.
     */
    public static final String DATE_FROM = "dateFrom";

    /**
     * Klíč pro koncové datum ve formátu textového řetězce.
     */
    public static final String DATE_TO = "dateTo";

    /**
     * Počáteční vybrané datum faktury.
     */
    private LocalDate localDateFrom;

    /**
     * Koncové vybrané datum faktury.
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
     * Výchozí prázdný veřejný konstruktor.
     */
    public InvoiceDateRangeDialogFragment() {
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
     * Tovární metoda pro vytvoření nové instance dialogu období faktury.
     *
     * @param dateFrom počáteční datum faktury jako textový řetězec
     * @param dateTo   koncové datum faktury jako textový řetězec
     * @return nová instance {@link InvoiceDateRangeDialogFragment}
     */
    public static InvoiceDateRangeDialogFragment newInstance(@Nullable String dateFrom, @Nullable String dateTo) {
        InvoiceDateRangeDialogFragment fragment = new InvoiceDateRangeDialogFragment();
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
        View view = requireActivity().getLayoutInflater().inflate(R.layout.dialog_invoice_date_range, null);

        initViews(view);
        setupListeners(view);
        updateUiState();

        builder.setView(view);
        return builder.create();
    }


    /**
     * Uloží stav hranic data faktury při změně konfigurace nebo zničení fragmentu.
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

        if (fromStr != null && !fromStr.trim().isEmpty() && !fromStr.equalsIgnoreCase("Datum")) {
            try {
                localDateFrom = ViewHelper.parseCalendarFromString(fromStr)
                        .getTime().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            } catch (Exception e) {
                localDateFrom = LocalDate.of(currentYear, 1, 1);
            }
        } else {
            localDateFrom = LocalDate.of(currentYear, 1, 1);
        }

        if (toStr != null && !toStr.trim().isEmpty() && !toStr.equalsIgnoreCase("Datum")) {
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
    }


    /**
     * Nastaví posluchače událostí pro prvky uživatelského rozhraní.
     *
     * @param view kořenový náhled dialogu
     */
    private void setupListeners(View view) {
        MaterialButton btnOpenRangePicker = view.findViewById(R.id.btnOpenRangePicker);
        MaterialButton btnCancelDateRange = view.findViewById(R.id.btnCancelDateRange);
        MaterialButton btnApplyDateRange = view.findViewById(R.id.btnApplyDateRange);

        View.OnClickListener listenerFrom = v -> openDatePickerFrom();
        etDateFrom.setOnClickListener(listenerFrom);
        tilDateFrom.setEndIconOnClickListener(listenerFrom);

        View.OnClickListener listenerTo = v -> openDatePickerTo();
        etDateTo.setOnClickListener(listenerTo);
        tilDateTo.setEndIconOnClickListener(listenerTo);

        btnOpenRangePicker.setOnClickListener(v -> openRangePicker());

        btnCancelDateRange.setOnClickListener(v -> dismiss());
        btnApplyDateRange.setOnClickListener(v -> applyDateRange());
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
        datePicker.show(getParentFragmentManager(), "INVOICE_DATE_PICKER_FROM");
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
        datePicker.show(getParentFragmentManager(), "INVOICE_DATE_PICKER_TO");
    }


    /**
     * Otevře {@link MaterialDatePicker} s rozsahem pro současný výběr počátečního i koncového data.
     */
    private void openRangePicker() {
        long startUtc = localDateFrom.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli();
        long endUtc = localDateTo.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli();

        Pair<Long, Long> initialSelection = new Pair<>(startUtc, endUtc);

        MaterialDatePicker<Pair<Long, Long>> rangePicker = MaterialDatePicker.Builder.dateRangePicker()
                .setTitleText(R.string.obdobi_faktury)
                .setSelection(initialSelection)
                .build();

        rangePicker.addOnPositiveButtonClickListener(selection -> {
            if (selection != null && selection.first != null && selection.second != null) {
                localDateFrom = Instant.ofEpochMilli(selection.first).atZone(ZoneOffset.UTC).toLocalDate();
                localDateTo = Instant.ofEpochMilli(selection.second).atZone(ZoneOffset.UTC).toLocalDate();
                updateUiState();
            }
        });
        rangePicker.show(getParentFragmentManager(), "INVOICE_DATE_RANGE_PICKER");
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
    }


    /**
     * Ověří platnost zadání a předá výsledek datumu faktury rodičovskému fragmentu skrze {@link androidx.fragment.app.FragmentManager}.
     */
    private void applyDateRange() {
        if (localDateFrom.isAfter(localDateTo)) {
            tvDateRangeError.setText(R.string.invalid_date_range);
            tvDateRangeError.setVisibility(View.VISIBLE);
            return;
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy", Locale.getDefault());

        Bundle bundle = new Bundle();
        bundle.putString(DATE_FROM, localDateFrom.format(formatter));
        bundle.putString(DATE_TO, localDateTo.format(formatter));
        getParentFragmentManager().setFragmentResult(INVOICE_DATE_RANGE, bundle);
        dismiss();
    }

}
