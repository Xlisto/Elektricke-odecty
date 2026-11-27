package cz.xlisto.elektrodroid.modules.pricelist;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.slider.Slider;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import cz.xlisto.elektrodroid.R;
import cz.xlisto.elektrodroid.databaze.DataSubscriptionPointSource;
import cz.xlisto.elektrodroid.format.DecimalFormatHelper;
import cz.xlisto.elektrodroid.models.DistributionTariffConditions;
import cz.xlisto.elektrodroid.models.PriceListModel;
import cz.xlisto.elektrodroid.models.SubscriptionPointModel;
import cz.xlisto.elektrodroid.modules.pricelist.engine.BreakEvenCalculator;
import cz.xlisto.elektrodroid.modules.pricelist.engine.BreakEvenCalculator.BreakEvenResult;
import cz.xlisto.elektrodroid.modules.pricelist.engine.BreakEvenCalculator.Point2D;
import cz.xlisto.elektrodroid.modules.pricelist.engine.HistoricalReadingConsumptionHelper;
import cz.xlisto.elektrodroid.modules.pricelist.engine.HistoricalReadingConsumptionHelper.HistoricalConsumptionResult;
import cz.xlisto.elektrodroid.modules.pricelist.models.PriceListComparisonScenario;
import cz.xlisto.elektrodroid.ownview.PriceListProfitabilityChartView;
import cz.xlisto.elektrodroid.ownview.PriceListProfitabilityChartView.SeriesData;
import cz.xlisto.elektrodroid.repositories.DistributionTariffConditionsRepository;
import cz.xlisto.elektrodroid.shp.ShPSubscriptionPoint;

/**
 * Záložka „Výhodnost sazeb“ pro analýzu bodu zvratu (break-even point) a simulaci změny distribuční sazby.
 * Podporuje 2 posuvníky (VT a NT) pro nastavení modelové spotřeby, 1 posuvník pro volbu počtu roků historie spotřeby
 * a rozbalovací karty s technickými podmínkami přiznání jednotlivých distribučních sazeb.
 */
public class PriceListCompareProfitabilityFragment extends Fragment {

    private PriceListsViewModel viewModel;

    private RadioButton rbSourceHistory;

    private LinearLayout llModelSlidersContainer;
    private TextView tvVtSliderLabel;
    private Slider sliderVtMwh;

    private LinearLayout llNtSliderWrapper;
    private TextView tvNtSliderLabel;
    private Slider sliderNtMwh;

    private LinearLayout llHistorySliderContainer;
    private TextView tvHistoryYearsLabel;
    private Slider sliderHistoryYears;
    private TextView tvHistoryQualityInfo;

    private TextView tvBreakEvenSummaryText;
    private PriceListProfitabilityChartView chartProfitability;
    private LinearLayout llTechnicalConditionsContainer;

    private boolean isUpdatingFromCode = false;
    private final Set<String> expandedTariffs = new HashSet<>();
    private final int[] lineColors = new int[]{0xFF1E88E5, 0xFF43A047, 0xFFFB8C00, 0xFF8E24AA};


    /**
     * Vytvoří novou instanci {@link PriceListCompareProfitabilityFragment}.
     *
     * @return nová instance {@link PriceListCompareProfitabilityFragment}
     */
    public static PriceListCompareProfitabilityFragment newInstance() {
        return new PriceListCompareProfitabilityFragment();
    }


    /**
     * Vytvoří a vrátí rozvržení pohledu záložky výhodnosti sazeb.
     *
     * @param inflater           objekt pro nafouknutí rozvržení
     * @param container          kontejner rodičovského pohledu
     * @param savedInstanceState uložený stav fragmentu
     * @return nafouknutý pohled {@link View}
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_price_list_compare_profitability, container, false);
    }


    /**
     * Inicializuje UI prvky záložky, propojí posluchače posuvníků, volby přepínače zdroje spotřeby
     * a přihlásí se k odběru dat z {@link PriceListsViewModel}.
     *
     * @param view               pohled fragmentu
     * @param savedInstanceState uložený stav fragmentu
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(requireParentFragment()).get(PriceListsViewModel.class);

        RadioGroup rgProfitabilitySource = view.findViewById(R.id.rgProfitabilitySource);
        rbSourceHistory = view.findViewById(R.id.rbSourceHistory);

        llModelSlidersContainer = view.findViewById(R.id.llModelSlidersContainer);
        tvVtSliderLabel = view.findViewById(R.id.tvVtSliderLabel);
        sliderVtMwh = view.findViewById(R.id.sliderVtMwh);

        llNtSliderWrapper = view.findViewById(R.id.llNtSliderWrapper);
        tvNtSliderLabel = view.findViewById(R.id.tvNtSliderLabel);
        sliderNtMwh = view.findViewById(R.id.sliderNtMwh);

        llHistorySliderContainer = view.findViewById(R.id.llHistorySliderContainer);
        tvHistoryYearsLabel = view.findViewById(R.id.tvHistoryYearsLabel);
        sliderHistoryYears = view.findViewById(R.id.sliderHistoryYears);
        tvHistoryQualityInfo = view.findViewById(R.id.tvHistoryQualityInfo);

        tvBreakEvenSummaryText = view.findViewById(R.id.tvBreakEvenSummaryText);
        chartProfitability = view.findViewById(R.id.chartProfitability);
        llTechnicalConditionsContainer = view.findViewById(R.id.llTechnicalConditionsContainer);

        rgProfitabilitySource.setOnCheckedChangeListener((group, checkedId) -> {
            if (isUpdatingFromCode) return;
            onSourceModeChanged();
        });

        sliderVtMwh.addOnChangeListener((slider, value, fromUser) -> {
            if (fromUser && !isUpdatingFromCode) {
                updateVtFromSlider(value);
            }
        });

        sliderNtMwh.addOnChangeListener((slider, value, fromUser) -> {
            if (fromUser && !isUpdatingFromCode) {
                updateNtFromSlider(value);
            }
        });

        sliderHistoryYears.addOnChangeListener((slider, value, fromUser) -> {
            if (fromUser && !isUpdatingFromCode) {
                updateHistoryFromSlider((int) value);
            }
        });

        viewModel.getScenario().observe(getViewLifecycleOwner(), this::syncUIWithScenario);
        viewModel.getSelectedPriceLists().observe(getViewLifecycleOwner(), priceLists -> updateProfitabilityUI());
    }


    /**
     * Reaguje na změnu přepínače zdroje spotřeby (historická spotřeba vs. ručně zadaný model)
     * a aktualizuje viditelnost odpovídajících posuvníků a scénář ve {@link PriceListsViewModel}.
     */
    private void onSourceModeChanged() {
        if (rbSourceHistory.isChecked()) {
            llModelSlidersContainer.setVisibility(View.GONE);
            llHistorySliderContainer.setVisibility(View.VISIBLE);
            updateHistoryFromSlider((int) sliderHistoryYears.getValue());
        } else {
            llModelSlidersContainer.setVisibility(View.VISIBLE);
            llHistorySliderContainer.setVisibility(View.GONE);
            updateVtFromSlider(sliderVtMwh.getValue());
        }
    }


    /**
     * Aktualizuje spotřebu ve vysokém tarifu (VT) v měřítku kWh ve scénáři na základě hodnoty z posuvníku MWh.
     *
     * @param vtValueMwh hodnota spotřeby VT v MWh z posuvníku
     */
    private void updateVtFromSlider(float vtValueMwh) {
        PriceListComparisonScenario scenario = viewModel.getScenario().getValue();
        PriceListComparisonScenario activeScenario = scenario != null ? scenario : new PriceListComparisonScenario();

        activeScenario.setVtKwh(vtValueMwh * 1000.0);
        activeScenario.setNtKwh(sliderNtMwh.getValue() * 1000.0);
        viewModel.setScenario(activeScenario);
    }


    /**
     * Aktualizuje spotřebu v nízkém tarifu (NT) v měřítku kWh ve scénáři na základě hodnoty z posuvníku MWh.
     *
     * @param ntValueMwh hodnota spotřeby NT v MWh z posuvníku
     */
    private void updateNtFromSlider(float ntValueMwh) {
        PriceListComparisonScenario scenario = viewModel.getScenario().getValue();
        PriceListComparisonScenario activeScenario = scenario != null ? scenario : new PriceListComparisonScenario();

        activeScenario.setVtKwh(sliderVtMwh.getValue() * 1000.0);
        activeScenario.setNtKwh(ntValueMwh * 1000.0);
        viewModel.setScenario(activeScenario);
    }


    /**
     * Vypočítá průměrnou roční spotřebu z odečtů za zvolený počet roků do historie pro aktuální odběrné místo
     * a nastaví vypočtené hodnoty VT a NT do srovnávacího scénáře.
     *
     * @param yearsCount počet roků historie odečtů pro výpočet průměru
     */
    private void updateHistoryFromSlider(int yearsCount) {
        PriceListComparisonScenario scenario = viewModel.getScenario().getValue();
        PriceListComparisonScenario activeScenario = scenario != null ? scenario : new PriceListComparisonScenario();

        ShPSubscriptionPoint shP = new ShPSubscriptionPoint(requireContext());
        long idSub = shP.get(ShPSubscriptionPoint.ID_SUBSCRIPTION_POINT_LONG, -1L);
        if (idSub > 0) {
            DataSubscriptionPointSource dataSource = new DataSubscriptionPointSource(requireContext());
            dataSource.open();
            SubscriptionPointModel subPoint = dataSource.loadSubscriptionPoint(idSub);
            dataSource.close();

            if (subPoint != null) {
                List<Integer> availableYears = HistoricalReadingConsumptionHelper.getAvailableYears(requireContext(), subPoint);
                int maxAvailableYears = availableYears.isEmpty() ? yearsCount : Math.min(yearsCount, availableYears.size());
                if (!availableYears.isEmpty()) {
                    float maxYears = Math.max(1.0f, (float) availableYears.size());
                    sliderHistoryYears.setValueTo(maxYears);
                    setSliderValueSafely(sliderHistoryYears, maxAvailableYears);
                }

                HistoricalConsumptionResult histRes = HistoricalReadingConsumptionHelper.calculateAverageConsumptionForLastNYears(requireContext(), subPoint, maxAvailableYears);
                if (histRes.totalKwh > 0) {
                    activeScenario.setVtKwh(histRes.vtKwh);
                    activeScenario.setNtKwh(histRes.ntKwh);
                    tvHistoryQualityInfo.setText(histRes.qualityNote);
                } else {
                    tvHistoryQualityInfo.setText(R.string.no_historical_readings_found);
                }
            }
        }

        viewModel.setScenario(activeScenario);
    }


    /**
     * Bezpečně nastaví hodnotu posuvníku {@link Slider} tak, aby ležela v povoleném rozsahu min-max
     * a odpovídala nastavenému kroku.
     *
     * @param slider   posuvník {@link Slider} k nastavení
     * @param rawValue požadovaná hodnota k nastavení
     */
    private void setSliderValueSafely(Slider slider, float rawValue) {
        float min = slider.getValueFrom();
        float max = slider.getValueTo();
        float step = slider.getStepSize();

        float initialClamped = Math.max(min, Math.min(rawValue, max));
        float calculatedValue = initialClamped;

        if (step > 0) {
            float steps = Math.round((initialClamped - min) / step);
            calculatedValue = min + (steps * step);
        }

        float finalValue = Math.max(min, Math.min(calculatedValue, max));
        slider.setValue(finalValue);
    }


    /**
     * Synchronizuje prvky uživatelského rozhraní (posuvníky a textové popisky) s aktuálním stavem {@link PriceListComparisonScenario}.
     *
     * @param scenario aktuální srovnávací scénář spotřeby
     */
    private void syncUIWithScenario(PriceListComparisonScenario scenario) {
        if (scenario == null) return;
        isUpdatingFromCode = true;

        float vtMwh = (float) (scenario.getVtKwh() / 1000.0);
        float ntMwh = (float) (scenario.getNtKwh() / 1000.0);

        if (vtMwh > sliderVtMwh.getValueTo()) sliderVtMwh.setValueTo(Math.max(20.0f, vtMwh * 1.5f));
        if (ntMwh > sliderNtMwh.getValueTo()) sliderNtMwh.setValueTo(Math.max(20.0f, ntMwh * 1.5f));

        setSliderValueSafely(sliderVtMwh, vtMwh);
        setSliderValueSafely(sliderNtMwh, ntMwh);

        tvVtSliderLabel.setText(String.format(
                java.util.Locale.getDefault(),
                "Modelová spotřeba VT: %s MWh (%s kWh)",
                DecimalFormatHelper.df1.format(vtMwh),
                DecimalFormatHelper.df0.format(scenario.getVtKwh())
        ));

        tvNtSliderLabel.setText(String.format(
                java.util.Locale.getDefault(),
                "Modelová spotřeba NT: %s MWh (%s kWh)",
                DecimalFormatHelper.df1.format(ntMwh),
                DecimalFormatHelper.df0.format(scenario.getNtKwh())
        ));

        int yearsVal = (int) sliderHistoryYears.getValue();
        tvHistoryYearsLabel.setText(String.format(
                java.util.Locale.getDefault(),
                "Počet roků do historie pro průměr: %d %s",
                yearsVal,
                yearsVal == 1 ? "rok" : (yearsVal < 5 ? "roky" : "let")
        ));

        isUpdatingFromCode = false;
        updateProfitabilityUI();
    }


    /**
     * Spočítá bod zvratu a roční náklady pomocí {@link BreakEvenCalculator}, vytvoří datové řady
     * a překreslí graf výhodnosti {@link PriceListProfitabilityChartView} i rozbalovací karty technických podmínek.
     */
    private void updateProfitabilityUI() {
        PriceListComparisonScenario scenario = viewModel.getScenario().getValue();
        List<PriceListModel> selectedPriceLists = viewModel.getSelectedPriceLists().getValue();

        if (scenario == null || selectedPriceLists == null || selectedPriceLists.isEmpty()) {
            tvBreakEvenSummaryText.setText(R.string.select_at_least_two_pricelists);
            chartProfitability.setChartData(PriceListProfitabilityChartView.Mode.COST_VS_CONSUMPTION, new ArrayList<>(), null, 0);
            updateTechnicalConditionsUI(new ArrayList<>());
            return;
        }

        boolean isDualTariff = selectedPriceLists.stream().anyMatch(pl -> pl.getPriceNT() > 0 || (pl.getRate() != null && !pl.getRate().trim().equalsIgnoreCase("D01d") && !pl.getRate().trim().equalsIgnoreCase("D02d")));
        llNtSliderWrapper.setVisibility(isDualTariff ? View.VISIBLE : View.GONE);

        double currentTotalMwh = scenario.getTotalKwh() / 1000.0;
        double ntRatio = scenario.getNtRatio();

        BreakEvenResult breakEvenResult = BreakEvenCalculator.calculateMultiPriceListBreakEven(
                selectedPriceLists, ntRatio, scenario.getMonths(), (int) scenario.getCountPhaze(), (int) scenario.getPowerBreaker(), currentTotalMwh
        );

        tvBreakEvenSummaryText.setText(breakEvenResult.textSummary());

        // Generujeme křivky nákladů v závislosti na spotřebě 0..20 MWh
        double maxMwh = Math.max(10.0, currentTotalMwh * 1.5);
        List<SeriesData> seriesList = new ArrayList<>();

        for (int i = 0; i < Math.min(4, selectedPriceLists.size()); i++) {
            PriceListModel pl = selectedPriceLists.get(i);
            List<Point2D> curvePoints = BreakEvenCalculator.generateCostVsConsumptionCurve(
                    pl, ntRatio, scenario.getMonths(), (int) scenario.getCountPhaze(), (int) scenario.getPowerBreaker(), maxMwh, 20
            );
            String title = pl.getNameWithPeriod();
            seriesList.add(new SeriesData(title, lineColors[i % lineColors.length], curvePoints));
        }

        chartProfitability.setChartData(
                PriceListProfitabilityChartView.Mode.COST_VS_CONSUMPTION,
                seriesList,
                breakEvenResult,
                currentTotalMwh
        );

        updateTechnicalConditionsUI(selectedPriceLists);
    }


    /**
     * Vytvoří a zobrazí rozbalovací karty technických podmínek přiznání distribučních sazeb
     * pro unikátní sazby přítomné v seznamu porovnávaných ceníků.
     *
     * @param priceLists seznam porovnávaných ceníků {@link PriceListModel}
     */
    private void updateTechnicalConditionsUI(List<PriceListModel> priceLists) {
        if (llTechnicalConditionsContainer == null) return;
        llTechnicalConditionsContainer.removeAllViews();

        if (priceLists == null || priceLists.isEmpty()) {
            TextView tvEmpty = new TextView(requireContext());
            tvEmpty.setText(R.string.select_pricelists_to_show_conditions);
            tvEmpty.setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodyMedium);
            tvEmpty.setTextColor(0xFF757575);
            llTechnicalConditionsContainer.addView(tvEmpty);
            return;
        }

        Set<String> uniqueTariffCodes = new LinkedHashSet<>();
        for (PriceListModel pl : priceLists) {
            if (pl.getRate() != null && !pl.getRate().trim().isEmpty()) {
                uniqueTariffCodes.add(pl.getRate().trim());
            }
        }

        LayoutInflater inflater = LayoutInflater.from(requireContext());

        for (String code : uniqueTariffCodes) {
            DistributionTariffConditions cond = DistributionTariffConditionsRepository.getConditionsForTariff(code);
            if (cond == null) {
                cond = new DistributionTariffConditions(
                        code,
                        "Sazba " + code,
                        "Dvoutarifní / Jednotarifní",
                        "Podmínky přiznání pro tuto sazbu posuzuje distributor.",
                        "Podrobnější informace naleznete v oficiálním cenovém rozhodnutí ERÚ nebo u příslušného provozovatele distribuční soustavy.",
                        false,
                        "",
                        "https://www.cezdistribuce.cz/pro-zakazniky/potrebuji-vyresit/ceny-a-podminky/podminky-pro-priznani-distribucni-sazby"
                );
            }

            View itemView = inflater.inflate(R.layout.item_tariff_condition, llTechnicalConditionsContainer, false);
            MaterialCardView cardItem = itemView.findViewById(R.id.cardTariffItem);
            TextView tvCode = itemView.findViewById(R.id.tvTariffCode);
            TextView tvBadge = itemView.findViewById(R.id.tvLowTariffBadge);
            ImageView ivChevron = itemView.findViewById(R.id.ivChevron);
            TextView tvShortDesc = itemView.findViewById(R.id.tvShortDescription);
            LinearLayout llExpanded = itemView.findViewById(R.id.llExpandedContent);
            TextView tvHistNotice = itemView.findViewById(R.id.tvHistoricalNotice);
            TextView tvDetailed = itemView.findViewById(R.id.tvDetailedConditions);
            TextView tvOfficialUrl = itemView.findViewById(R.id.tvOfficialUrlLink);

            tvCode.setText(cond.tariffCode());
            tvBadge.setText(cond.lowTariffBadge());
            tvShortDesc.setText(cond.shortDescription());
            tvDetailed.setText(cond.detailedConditions());

            if (cond.isHistorical() && cond.historicalNotice() != null && !cond.historicalNotice().isEmpty()) {
                tvHistNotice.setVisibility(View.VISIBLE);
                tvHistNotice.setText(cond.historicalNotice());
            } else {
                tvHistNotice.setVisibility(View.GONE);
            }

            String currentCodeKey = cond.tariffCode();
            boolean isExpanded = expandedTariffs.contains(currentCodeKey);
            llExpanded.setVisibility(isExpanded ? View.VISIBLE : View.GONE);
            ivChevron.setImageResource(isExpanded ? R.drawable.ic_expand_less_24 : R.drawable.ic_expand_more_24);

            cardItem.setOnClickListener(v -> {
                boolean newExpandedState = llExpanded.getVisibility() != View.VISIBLE;
                llExpanded.setVisibility(newExpandedState ? View.VISIBLE : View.GONE);
                ivChevron.setImageResource(newExpandedState ? R.drawable.ic_expand_less_24 : R.drawable.ic_expand_more_24);

                if (newExpandedState) {
                    expandedTariffs.add(currentCodeKey);
                } else {
                    expandedTariffs.remove(currentCodeKey);
                }
            });

            String url = cond.officialUrl();
            tvOfficialUrl.setOnClickListener(v -> {
                try {
                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    startActivity(browserIntent);
                } catch (Exception ignored) {
                }
            });

            llTechnicalConditionsContainer.addView(itemView);
        }
    }
}
