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

/**
 * Dialog ve stylu Material Design 3 pro filtraci období měsíčních odečtů.
 * Umožňuje ruční výběr počátečního a koncového data, výběr rozsahu v kalendáři
 * nebo volbu z rychlých předvolených období.
 */
public class MonthlyReadingFilterDialogFragment extends DialogFragment {

    /**
     * Klíč výsledku fragmentu předávaného skrze {@link androidx.fragment.app.FragmentManager#setFragmentResult}.
     */
    public static final String MONTHLY_READING_FILTER = "monthlyReadingFilter";

    /** Klíč pro počáteční datum v milisekundách. */
    public static final String FROM = "from";

    /** Klíč pro koncové datum v milisekundách. */
    public static final String TO = "to";

    /**
     * Počáteční datum filtrovaného období v milisekundách od počátku epochy.
     */
    private long from = 0L;

    /**
     * Koncové datum filtrovaného období v milisekundách od počátku epochy.
     */
    private long to = Long.MAX_VALUE;

    /**
     * Obalovací prvek {@link TextInputLayout} pro pole počátečního data.
     */
    private TextInputLayout tilDateFrom;

    /**
     * Obalovací prvek {@link TextInputLayout} pro pole koncového data.
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
     * Tlačítko rychlé předvolby pro období od nového roku.
     */
    private Chip chipFromNewYear;

    /**
     * Tlačítko rychlé předvolby pro období posledních 6 měsíců.
     */
    private Chip chipLast6Months;

    /**
     * Tlačítko rychlé předvolby pro období posledního roku.
     */
    private Chip chipLastYear;

    /**
     * Tlačítko rychlé předvolby pro zobrazení všech odečtů.
     */
    private Chip chipAllReadings;

    /**
     * Příznak zabraňující cyklickému volání posluchače událostí při programové změně stavu chipů.
     */
    private boolean isUserSelectingPreset = false;


    /**
     * Výchozí prázdný veřejný konstruktor vyžadovaný architekturou Android.
     */
    public MonthlyReadingFilterDialogFragment() {
        // Vyžadovaný prázdný veřejný konstruktor
    }


    /**
     * Vrátí formátovač data v požadovaném českém formátu d. M. yyyy.
     *
     * @return instanci {@link DateTimeFormatter} nastavenou na formát d. M. yyyy
     */
    private DateTimeFormatter getDateFormatter() {
        return DateTimeFormatter.ofPattern("d. M. yyyy", Locale.getDefault());
    }


    /**
     * Tovární metoda pro vytvoření nové instance dialogu filtrování období.
     *
     * @param from počáteční datum v milisekundách
     * @param to   koncové datum v milisekundách
     * @return nová instance {@link MonthlyReadingFilterDialogFragment} s nastavenými argumenty
     */
    public static MonthlyReadingFilterDialogFragment newInstance(long from, long to) {
        MonthlyReadingFilterDialogFragment fragment = new MonthlyReadingFilterDialogFragment();
        Bundle args = new Bundle();
        args.putLong(FROM, from);
        args.putLong(TO, to);
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
        if (savedInstanceState != null) {
            from = savedInstanceState.getLong(FROM, 0L);
            to = savedInstanceState.getLong(TO, Long.MAX_VALUE);
        } else if (getArguments() != null) {
            from = getArguments().getLong(FROM, 0L);
            to = getArguments().getLong(TO, Long.MAX_VALUE);
        }

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext(), R.style.DialogTheme);
        View view = requireActivity().getLayoutInflater().inflate(R.layout.fragment_monthly_reading_filter_dialog, null);

        initViews(view);
        setupListeners(view);
        updateUiState();

        builder.setView(view);
        return builder.create();
    }


    /**
     * Uloží stav hranic filtrovaného období při změně konfigurace nebo zničení fragmentu.
     *
     * @param outState bundle pro uložení stavu
     */
    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putLong(FROM, from);
        outState.putLong(TO, to);
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
        chipFromNewYear = view.findViewById(R.id.chipFromNewYear);
        chipLast6Months = view.findViewById(R.id.chipLast6Months);
        chipLastYear = view.findViewById(R.id.chipLastYear);
        chipAllReadings = view.findViewById(R.id.chipAllReadings);
    }


    /**
     * Nastaví posluchače událostí pro prvky uživatelského rozhraní.
     *
     * @param view kořenový náhled dialogu
     */
    private void setupListeners(View view) {
        MaterialButton btnOpenRangePicker = view.findViewById(R.id.btnOpenRangePicker);
        MaterialButton btnCancelFilter = view.findViewById(R.id.btnCancelFilter);
        MaterialButton btnApplyFilter = view.findViewById(R.id.btnApplyFilter);

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
            if (checkedId == R.id.chipFromNewYear) {
                applyPresetFromNewYear();
            } else if (checkedId == R.id.chipLast6Months) {
                applyPresetLast6Months();
            } else if (checkedId == R.id.chipLastYear) {
                applyPresetLastYear();
            } else if (checkedId == R.id.chipAllReadings) {
                applyPresetAllReadings();
            }
        };

        chipFromNewYear.setOnClickListener(presetListener);
        chipLast6Months.setOnClickListener(presetListener);
        chipLastYear.setOnClickListener(presetListener);
        chipAllReadings.setOnClickListener(presetListener);

        btnCancelFilter.setOnClickListener(v -> dismiss());
        btnApplyFilter.setOnClickListener(v -> applyFilter());
    }


    /**
     * Nastaví předvolbu období „Od nového roku“ (od 1. 1. aktuálního roku do dnešního dne).
     */
    private void applyPresetFromNewYear() {
        LocalDate today = LocalDate.now();
        LocalDate start = LocalDate.of(today.getYear(), 1, 1);
        from = startOfDayMillis(start);
        to = endOfDayMillis(today);
        updateUiState();
    }


    /**
     * Nastaví předvolbu období „Posledních 6 měsíců“ (od data před 6 měsíci do dnešního dne).
     */
    private void applyPresetLast6Months() {
        LocalDate today = LocalDate.now();
        LocalDate start = today.minusMonths(6);
        from = startOfDayMillis(start);
        to = endOfDayMillis(today);
        updateUiState();
    }


    /**
     * Nastaví předvolbu období „Poslední rok“ (od data před 1 rokem do dnešního dne).
     */
    private void applyPresetLastYear() {
        LocalDate today = LocalDate.now();
        LocalDate start = today.minusYears(1);
        from = startOfDayMillis(start);
        to = endOfDayMillis(today);
        updateUiState();
    }


    /**
     * Nastaví předvolbu „Všechny odečty“ bez omezení období.
     */
    private void applyPresetAllReadings() {
        from = 0L;
        to = Long.MAX_VALUE;
        updateUiState();
    }


    /**
     * Otevře {@link MaterialDatePicker} pro výběr počátečního data.
     */
    private void openDatePickerFrom() {
        LocalDate current = isAllReadings() ? LocalDate.now() : millisToLocalDate(from);
        long initialUtcMillis = current.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli();

        MaterialDatePicker<Long> datePicker = MaterialDatePicker.Builder.datePicker()
                .setTitleText(R.string.datum_od)
                .setSelection(initialUtcMillis)
                .build();

        datePicker.addOnPositiveButtonClickListener(selection -> {
            if (selection != null) {
                LocalDate selectedDate = Instant.ofEpochMilli(selection).atZone(ZoneOffset.UTC).toLocalDate();
                from = startOfDayMillis(selectedDate);
                if (isAllReadings()) {
                    to = endOfDayMillis(LocalDate.now());
                }
                updateUiState();
            }
        });
        datePicker.show(getParentFragmentManager(), "DATE_PICKER_FROM");
    }


    /**
     * Otevře {@link MaterialDatePicker} pro výběr koncového data.
     */
    private void openDatePickerTo() {
        LocalDate current = isAllReadings() ? LocalDate.now() : millisToLocalDate(to);
        long initialUtcMillis = current.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli();

        MaterialDatePicker<Long> datePicker = MaterialDatePicker.Builder.datePicker()
                .setTitleText(R.string.datum_do)
                .setSelection(initialUtcMillis)
                .build();

        datePicker.addOnPositiveButtonClickListener(selection -> {
            if (selection != null) {
                LocalDate selectedDate = Instant.ofEpochMilli(selection).atZone(ZoneOffset.UTC).toLocalDate();
                to = endOfDayMillis(selectedDate);
                if (from <= 0) {
                    from = startOfDayMillis(selectedDate.minusYears(1));
                }
                updateUiState();
            }
        });
        datePicker.show(getParentFragmentManager(), "DATE_PICKER_TO");
    }


    /**
     * Otevře {@link MaterialDatePicker} s rozsahem pro současný výběr počátečního i koncového data.
     */
    private void openRangePicker() {
        LocalDate start = isAllReadings() ? LocalDate.now().minusYears(1) : millisToLocalDate(from);
        LocalDate end = isAllReadings() ? LocalDate.now() : millisToLocalDate(to);

        long startUtc = start.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli();
        long endUtc = end.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli();

        Pair<Long, Long> initialSelection = new Pair<>(startUtc, endUtc);

        MaterialDatePicker<Pair<Long, Long>> rangePicker = MaterialDatePicker.Builder.dateRangePicker()
                .setTitleText(R.string.obdobi_mesicnich_odectu)
                .setSelection(initialSelection)
                .build();

        rangePicker.addOnPositiveButtonClickListener(selection -> {
            if (selection != null && selection.first != null && selection.second != null) {
                LocalDate startDate = Instant.ofEpochMilli(selection.first).atZone(ZoneOffset.UTC).toLocalDate();
                LocalDate endDate = Instant.ofEpochMilli(selection.second).atZone(ZoneOffset.UTC).toLocalDate();
                from = startOfDayMillis(startDate);
                to = endOfDayMillis(endDate);
                updateUiState();
            }
        });
        rangePicker.show(getParentFragmentManager(), "DATE_RANGE_PICKER");
    }


    /**
     * Obnoví stav prvků uživatelského rozhraní podle aktuálních hodnot {@code from} a {@code to}.
     */
    private void updateUiState() {
        tvDateRangeError.setVisibility(View.GONE);
        tilDateFrom.setError(null);
        tilDateTo.setError(null);

        DateTimeFormatter formatter = getDateFormatter();

        if (isAllReadings()) {
            etDateFrom.setText(R.string.vse);
            etDateTo.setText(R.string.vse);
        } else {
            LocalDate dFrom = millisToLocalDate(from);
            LocalDate dTo = millisToLocalDate(to);
            etDateFrom.setText(dFrom.format(formatter));
            etDateTo.setText(dTo.format(formatter));
        }

        isUserSelectingPreset = true;
        Integer matchingChipId = getMatchingPresetChipId();
        chipFromNewYear.setChecked(matchingChipId != null && matchingChipId == R.id.chipFromNewYear);
        chipLast6Months.setChecked(matchingChipId != null && matchingChipId == R.id.chipLast6Months);
        chipLastYear.setChecked(matchingChipId != null && matchingChipId == R.id.chipLastYear);
        chipAllReadings.setChecked(matchingChipId != null && matchingChipId == R.id.chipAllReadings);
        isUserSelectingPreset = false;
    }


    /**
     * Ověří platnost zadání a předá výsledek filtrování rodičovskému fragmentu skrze {@link androidx.fragment.app.FragmentManager}.
     */
    private void applyFilter() {
        if (!isAllReadings() && from > to) {
            tvDateRangeError.setText(R.string.invalid_date_range);
            tvDateRangeError.setVisibility(View.VISIBLE);
            return;
        }

        Bundle bundle = new Bundle();
        bundle.putLong(FROM, from);
        bundle.putLong(TO, to);
        getParentFragmentManager().setFragmentResult(MONTHLY_READING_FILTER, bundle);
        dismiss();
    }


    /**
     * Vrátí identifikátor chipu odpovídajícího aktuálně vybranému datumovému rozsahu.
     *
     * @return ID příslušného chipu z R.id nebo {@code null}, pokud zadaný rozsah neodpovídá žádné předvolbě
     */
    @Nullable
    private Integer getMatchingPresetChipId() {
        if (isAllReadings()) {
            return R.id.chipAllReadings;
        }

        LocalDate today = LocalDate.now();
        LocalDate dFrom = millisToLocalDate(from);
        LocalDate dTo = millisToLocalDate(to);

        // Kontrola předvolby „Od nového roku“
        LocalDate nyStart = LocalDate.of(today.getYear(), 1, 1);
        if (Objects.equals(dFrom, nyStart) && Objects.equals(dTo, today)) {
            return R.id.chipFromNewYear;
        }

        // Kontrola předvolby „Posledních 6 měsíců“
        LocalDate m6Start = today.minusMonths(6);
        if (Objects.equals(dFrom, m6Start) && Objects.equals(dTo, today)) {
            return R.id.chipLast6Months;
        }

        // Kontrola předvolby „Poslední rok“
        LocalDate y1Start = today.minusYears(1);
        if (Objects.equals(dFrom, y1Start) && Objects.equals(dTo, today)) {
            return R.id.chipLastYear;
        }

        return null;
    }


    /**
     * Zjistí, zda je nastaven filtr na všechny odečty (bez omezení období).
     *
     * @return {@code true}, pokud je zobrazen celý rozsah odečtů
     */
    private boolean isAllReadings() {
        return from <= 0L && to == Long.MAX_VALUE;
    }


    /**
     * Převede čas v milisekundách na {@link LocalDate} v systémovém časovém pásmu.
     *
     * @param millis čas v milisekundách od počátku epochy
     * @return odpovídající {@link LocalDate}
     */
    private LocalDate millisToLocalDate(long millis) {
        if (millis <= 0 || millis == Long.MAX_VALUE) {
            return LocalDate.now();
        }
        return Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate();
    }


    /**
     * Vrátí počátek dne (00:00:00) v milisekundách pro zadané datum v lokálním časovém pásmu.
     *
     * @param date datum
     * @return čas v milisekundách od počátku epochy
     */
    private long startOfDayMillis(LocalDate date) {
        return date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }


    /**
     * Vrátí konec dne (23:59:59.999) v milisekundách pro zadané datum v lokálním časovém pásmu.
     *
     * @param date datum
     * @return čas v milisekundách od počátku epochy
     */
    private long endOfDayMillis(LocalDate date) {
        return date.atTime(23, 59, 59, 999).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }
}
