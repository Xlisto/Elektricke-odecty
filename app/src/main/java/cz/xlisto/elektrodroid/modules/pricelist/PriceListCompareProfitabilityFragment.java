package cz.xlisto.elektrodroid.modules.pricelist;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.slider.Slider;

import java.util.ArrayList;
import java.util.List;

import cz.xlisto.elektrodroid.R;
import cz.xlisto.elektrodroid.databaze.DataSubscriptionPointSource;
import cz.xlisto.elektrodroid.format.DecimalFormatHelper;
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
import cz.xlisto.elektrodroid.shp.ShPSubscriptionPoint;

/**
 * Záložka „Výhodnost sazeb“ pro analýzu bodu zvratu (break-even point) a simulaci změny distribuční sazby.
 * Podporuje 2 posuvníky (VT a NT) pro modelovou spotřebu a 1 posuvník pro počet roků historie.
 */
public class PriceListCompareProfitabilityFragment extends Fragment {

    private PriceListsViewModel viewModel;

    private RadioGroup rgProfitabilitySource;
    private RadioButton rbSourceModel;
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
    private TextView tvTechnicalNoticeA;
    private TextView tvTechnicalNoticeB;

    private boolean isUpdatingFromCode = false;
    private final int[] lineColors = new int[]{0xFF1E88E5, 0xFF43A047, 0xFFFB8C00, 0xFF8E24AA};

    public static PriceListCompareProfitabilityFragment newInstance() {
        return new PriceListCompareProfitabilityFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_price_list_compare_profitability, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(requireParentFragment()).get(PriceListsViewModel.class);

        rgProfitabilitySource = view.findViewById(R.id.rgProfitabilitySource);
        rbSourceModel = view.findViewById(R.id.rbSourceModel);
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
        tvTechnicalNoticeA = view.findViewById(R.id.tvTechnicalNoticeA);
        tvTechnicalNoticeB = view.findViewById(R.id.tvTechnicalNoticeB);

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

        viewModel.getScenario().observe(getViewLifecycleOwner(), scenario -> syncUIWithScenario(scenario));
        viewModel.getSelectedPriceLists().observe(getViewLifecycleOwner(), priceLists -> updateProfitabilityUI());
    }


    private void onSourceModeChanged() {
        PriceListComparisonScenario scenario = viewModel.getScenario().getValue();
        if (scenario == null) scenario = new PriceListComparisonScenario();

        if (rbSourceHistory.isChecked()) {
            llModelSlidersContainer.setVisibility(View.GONE);
            llHistorySliderContainer.setVisibility(View.VISIBLE);
            scenario.setConsumptionSource(PriceListComparisonScenario.ConsumptionSource.HISTORICAL_YEAR);
            updateHistoryFromSlider((int) sliderHistoryYears.getValue());
        } else {
            llModelSlidersContainer.setVisibility(View.VISIBLE);
            llHistorySliderContainer.setVisibility(View.GONE);
            scenario.setConsumptionSource(PriceListComparisonScenario.ConsumptionSource.MANUAL);
            updateVtFromSlider(sliderVtMwh.getValue());
        }
    }


    private void updateVtFromSlider(float vtValueMwh) {
        PriceListComparisonScenario scenario = viewModel.getScenario().getValue();
        if (scenario == null) scenario = new PriceListComparisonScenario();

        scenario.setVtKwh(vtValueMwh * 1000.0);
        scenario.setNtKwh(sliderNtMwh.getValue() * 1000.0);
        viewModel.setScenario(scenario);
    }


    private void updateNtFromSlider(float ntValueMwh) {
        PriceListComparisonScenario scenario = viewModel.getScenario().getValue();
        if (scenario == null) scenario = new PriceListComparisonScenario();

        scenario.setVtKwh(sliderVtMwh.getValue() * 1000.0);
        scenario.setNtKwh(ntValueMwh * 1000.0);
        viewModel.setScenario(scenario);
    }


    private void updateHistoryFromSlider(int yearsCount) {
        PriceListComparisonScenario scenario = viewModel.getScenario().getValue();
        if (scenario == null) scenario = new PriceListComparisonScenario();

        ShPSubscriptionPoint shP = new ShPSubscriptionPoint(requireContext());
        long idSub = shP.get(ShPSubscriptionPoint.ID_SUBSCRIPTION_POINT_LONG, -1L);
        if (idSub > 0) {
            DataSubscriptionPointSource dataSource = new DataSubscriptionPointSource(requireContext());
            dataSource.open();
            SubscriptionPointModel subPoint = dataSource.loadSubscriptionPoint(idSub);
            dataSource.close();

            if (subPoint != null) {
                List<Integer> availableYears = HistoricalReadingConsumptionHelper.getAvailableYears(requireContext(), subPoint);
                if (!availableYears.isEmpty()) {
                    float maxYears = Math.max(1.0f, (float) availableYears.size());
                    sliderHistoryYears.setValueTo(maxYears);
                    if (yearsCount > maxYears) yearsCount = (int) maxYears;
                    setSliderValueSafely(sliderHistoryYears, yearsCount);
                }

                HistoricalConsumptionResult histRes = HistoricalReadingConsumptionHelper.calculateAverageConsumptionForLastNYears(requireContext(), subPoint, yearsCount);
                if (histRes.totalKwh > 0) {
                    scenario.setVtKwh(histRes.vtKwh);
                    scenario.setNtKwh(histRes.ntKwh);
                    tvHistoryQualityInfo.setText(histRes.qualityNote);
                } else {
                    tvHistoryQualityInfo.setText("Historické odečty nebyly nalezeny");
                }
            }
        }

        viewModel.setScenario(scenario);
    }


    private void setSliderValueSafely(Slider slider, float rawValue) {
        float min = slider.getValueFrom();
        float max = slider.getValueTo();
        float step = slider.getStepSize();

        float clamped = Math.max(min, Math.min(rawValue, max));

        if (step > 0) {
            float steps = Math.round((clamped - min) / step);
            clamped = min + (steps * step);
        }

        clamped = Math.max(min, Math.min(clamped, max));
        slider.setValue(clamped);
    }


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


    private void updateProfitabilityUI() {
        PriceListComparisonScenario scenario = viewModel.getScenario().getValue();
        List<PriceListModel> selectedPriceLists = viewModel.getSelectedPriceLists().getValue();

        if (scenario == null || selectedPriceLists == null || selectedPriceLists.isEmpty()) {
            tvBreakEvenSummaryText.setText("Pro porovnání výhodnosti sazeb vyberte alespoň 2 ceníky");
            chartProfitability.setChartData(PriceListProfitabilityChartView.Mode.COST_VS_CONSUMPTION, new ArrayList<>(), null, 0);
            return;
        }

        boolean isDualTariff = selectedPriceLists.stream().anyMatch(pl -> pl.getCenaNT() > 0 || (pl.getSazba() != null && !pl.getSazba().trim().equalsIgnoreCase("D01d") && !pl.getSazba().trim().equalsIgnoreCase("D02d")));
        llNtSliderWrapper.setVisibility(isDualTariff ? View.VISIBLE : View.GONE);

        double currentTotalMwh = scenario.getTotalKwh() / 1000.0;
        double ntRatio = scenario.getNtRatio();

        PriceListModel plA = selectedPriceLists.get(0);
        PriceListModel plB = selectedPriceLists.size() > 1 ? selectedPriceLists.get(1) : selectedPriceLists.get(0);

        BreakEvenResult breakEvenResult = BreakEvenCalculator.calculateBreakEvenConsumption(
                plA, plB, ntRatio, scenario.getMonths(), (int) scenario.getCountPhaze(), (int) scenario.getPowerBreaker()
        );

        tvBreakEvenSummaryText.setText(breakEvenResult.textSummary);
        tvTechnicalNoticeA.setText(breakEvenResult.technicalConditionNoticeA);
        tvTechnicalNoticeB.setText(breakEvenResult.technicalConditionNoticeB);

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
    }
}
