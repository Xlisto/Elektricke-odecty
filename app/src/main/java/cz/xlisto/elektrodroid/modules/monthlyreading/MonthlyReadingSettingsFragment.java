package cz.xlisto.elektrodroid.modules.monthlyreading;


import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.switchmaterial.SwitchMaterial;

import cz.xlisto.elektrodroid.MainActivity;
import cz.xlisto.elektrodroid.R;
import cz.xlisto.elektrodroid.dialogs.MonthlyReadingFilterDialogFragment;
import cz.xlisto.elektrodroid.dialogs.SubscriptionPointDialogFragment;
import cz.xlisto.elektrodroid.models.SubscriptionPointModel;
import cz.xlisto.elektrodroid.ownview.ViewHelper;
import cz.xlisto.elektrodroid.shp.ShPMonthlyReading;
import cz.xlisto.elektrodroid.utils.SubscriptionPoint;


/**
 * Fragment pro správy možností zobrazení měsíčních odečtů a souvisejících filtrů.
 * <p>
 * Umožňuje uživateli nastavit požadovaný režim zobrazení (Podrobné, Stručné, Krátké),
 * přepínat zohlednění regulovaných cen v r. 2023, vybrat filtrované období a změnit
 * aktivní odběrné místo. Změny se do nastavení a nadřazeného fragmentu aplikují po stisknutí
 * tlačítka POUŽÍT.
 */
public class MonthlyReadingSettingsFragment extends Fragment {

    /**
     * Tag pro logování zpráv z {@link MonthlyReadingSettingsFragment}.
     */
    public static final String TAG = "MonthlyReadingSettingsFragment";
    /**
     * Klíč příznaku pro výsledek předaný při uložení změn nastavení zobrazení.
     */
    public static final String FLAG_MONTHLY_READING_SETTINGS_CHANGED = "flagMonthlyReadingSettingsChanged";
    /**
     * Argument pro dolní hranici období.
     */
    public static final String ARG_FROM = "from";
    /**
     * Argument pro horní hranici období.
     */
    public static final String ARG_TO = "to";

    /**
     * Dolní hranice vybraného období (čas v milisekundách).
     */
    private long from = 0;
    /**
     * Horní hranice vybraného období (čas v milisekundách).
     */
    private long to = Long.MAX_VALUE;

    /**
     * Textové popisky pro zobrazení vybraného období a odběrného místa.
     */
    private TextView tvPeriodValue, tvSubscriptionPointValue;
    /**
     * Skupina přepínačů režimu zobrazení.
     */
    private RadioGroup rgDisplayMode;
    /**
     * Přepínač pro zohlednění zastropování cen v r. 2023.
     */
    private SwitchMaterial swRegulPriceSetting;

    /**
     * Správce preferencí měsíčních odečtů.
     */
    private ShPMonthlyReading shPMonthlyReading;


    /**
     * Výchozí prázdný konstruktor fragmentu vyžadovaný architekturou Android.
     */
    public MonthlyReadingSettingsFragment() {
        // Required empty public constructor
    }


    /**
     * Tovární metoda pro vytvoření nové instance {@link MonthlyReadingSettingsFragment}.
     *
     * @param from dolní hranice filtrovaného období v milisekundách
     * @param to   horní hranice filtrovaného období v milisekundách
     * @return nová instance {@link MonthlyReadingSettingsFragment}
     */
    public static MonthlyReadingSettingsFragment newInstance(long from, long to) {
        MonthlyReadingSettingsFragment fragment = new MonthlyReadingSettingsFragment();
        Bundle args = new Bundle();
        args.putLong(ARG_FROM, from);
        args.putLong(ARG_TO, to);
        fragment.setArguments(args);
        return fragment;
    }


    /**
     * Načte počáteční argumenty období a registruje posluchače výsledků pro dialogy filtru a odběrného místa.
     *
     * @param savedInstanceState uložený stav instance (může být {@code null})
     */
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            from = getArguments().getLong(ARG_FROM, 0);
            to = getArguments().getLong(ARG_TO, Long.MAX_VALUE);
        }

        // Posluchač filtru období z dialogu
        requireActivity().getSupportFragmentManager().setFragmentResultListener(
                MonthlyReadingFilterDialogFragment.MONTHLY_READING_FILTER,
                this,
                (requestKey, result) -> {
                    to = result.getLong(MonthlyReadingFilterDialogFragment.TO);
                    from = result.getLong(MonthlyReadingFilterDialogFragment.FROM);
                    if (from > 0) {
                        from = from + ViewHelper.getOffsetTimeZones(from);
                    }
                    updatePeriodLabel();
                }
        );

        // Posluchač změny odběrného místa
        requireActivity().getSupportFragmentManager().setFragmentResultListener(
                SubscriptionPointDialogFragment.FLAG_UPDATE_SUBSCRIPTION_POINT,
                this,
                (requestKey, result) -> updateSubscriptionPointLabel()
        );
    }


    /**
     * Vytváří zobrazení fragmentu z XML návrhu.
     *
     * @param inflater           objekt pro nafouknutí rozvržení
     * @param container          rodičovský kontejner (může být {@code null})
     * @param savedInstanceState uložený stav instance (může být {@code null})
     * @return zobrazení fragmentu {@link View}
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_monthly_reading_settings, container, false);
    }


    /**
     * Inicializuje ovládací prvky, načte aktuálně uložené preference a nastaví obsluhu událostí.
     *
     * @param view               vytvořené zobrazení fragmentu
     * @param savedInstanceState uložený stav instance (může být {@code null})
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (requireActivity() instanceof MainActivity) {
            ((MainActivity) requireActivity()).setToolbarTitle(getString(R.string.options_monthly_readings));
        }

        shPMonthlyReading = new ShPMonthlyReading(requireActivity());

        MaterialCardView cardPeriod = view.findViewById(R.id.cardPeriod);
        MaterialCardView cardSubscriptionPoint = view.findViewById(R.id.cardSubscriptionPoint);
        tvPeriodValue = view.findViewById(R.id.tvPeriodValue);
        tvSubscriptionPointValue = view.findViewById(R.id.tvSubscriptionPointValue);
        rgDisplayMode = view.findViewById(R.id.rgDisplayMode);
        RadioButton rbDetailed = view.findViewById(R.id.rbDetailed);
        RadioButton rbCondensed = view.findViewById(R.id.rbCondensed);
        RadioButton rbMinimal = view.findViewById(R.id.rbMinimal);
        swRegulPriceSetting = view.findViewById(R.id.swRegulPriceSetting);
        Button btnCancel = view.findViewById(R.id.btnCancelSettings);
        Button btnApply = view.findViewById(R.id.btnApplySettings);

        int currentDisplayMode = shPMonthlyReading.getDisplayMode();
        if (currentDisplayMode == ShPMonthlyReading.DISPLAY_MODE_MINIMAL) {
            rbMinimal.setChecked(true);
        } else if (currentDisplayMode == ShPMonthlyReading.DISPLAY_MODE_CONDENSED) {
            rbCondensed.setChecked(true);
        } else {
            rbDetailed.setChecked(true);
        }

        swRegulPriceSetting.setChecked(shPMonthlyReading.get(ShPMonthlyReading.REGUL_PRICE, false));

        updatePeriodLabel();
        updateSubscriptionPointLabel();

        cardPeriod.setOnClickListener(v -> {
            MonthlyReadingFilterDialogFragment dialogFragment = MonthlyReadingFilterDialogFragment.newInstance(from, to);
            dialogFragment.show(requireActivity().getSupportFragmentManager(), "MonthlyReadingFilterDialogFragment");
        });

        cardSubscriptionPoint.setOnClickListener(v ->
                SubscriptionPointDialogFragment.newInstance().show(
                        requireActivity().getSupportFragmentManager(),
                        SubscriptionPointDialogFragment.class.getSimpleName()
                )
        );

        btnCancel.setOnClickListener(v -> requireActivity().getSupportFragmentManager().popBackStack());

        btnApply.setOnClickListener(v -> applySettings());
    }


    /**
     * Aktualizuje textový popisek vybraného období na základě proměnných {@code from} a {@code to}.
     */
    private void updatePeriodLabel() {
        if (tvPeriodValue == null) return;
        if (from == 0 && to == Long.MAX_VALUE) {
            tvPeriodValue.setText(getString(R.string.all_periods));
        } else {
            tvPeriodValue.setText(getString(
                    R.string.show_period,
                    ViewHelper.convertLongToDate(from),
                    ViewHelper.convertLongToDate(to)
            ));
        }
    }


    /**
     * Aktualizuje textový popisek názvu aktuálně vybraného odběrného místa.
     */
    private void updateSubscriptionPointLabel() {
        if (tvSubscriptionPointValue == null) return;
        SubscriptionPointModel subscriptionPoint = SubscriptionPoint.load(requireActivity());
        if (subscriptionPoint != null) {
            tvSubscriptionPointValue.setText(subscriptionPoint.getName());
        } else {
            tvSubscriptionPointValue.setText(getString(R.string.create_place));
        }
    }


    /**
     * Uloží zvolené nastavení režimu zobrazení a regulované ceny do preferencí {@link ShPMonthlyReading},
     * předá výsledek s vybraným obdobím nadřazenému fragmentu a ukončí tento fragment.
     */
    private void applySettings() {
        int selectedMode = ShPMonthlyReading.DISPLAY_MODE_DETAILED;
        int checkedId = rgDisplayMode.getCheckedRadioButtonId();
        if (checkedId == R.id.rbMinimal) {
            selectedMode = ShPMonthlyReading.DISPLAY_MODE_MINIMAL;
        } else if (checkedId == R.id.rbCondensed) {
            selectedMode = ShPMonthlyReading.DISPLAY_MODE_CONDENSED;
        }

        shPMonthlyReading.setDisplayMode(selectedMode);
        shPMonthlyReading.set(ShPMonthlyReading.REGUL_PRICE, swRegulPriceSetting.isChecked());

        Bundle result = new Bundle();
        result.putLong(ARG_FROM, from);
        result.putLong(ARG_TO, to);
        getParentFragmentManager().setFragmentResult(FLAG_MONTHLY_READING_SETTINGS_CHANGED, result);

        requireActivity().getSupportFragmentManager().popBackStack();
    }

}
