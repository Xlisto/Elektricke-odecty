package cz.xlisto.elektrodroid.modules.pricelist;


import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
 */
public class PriceListCompareProfitabilityFragment extends Fragment {

    private PriceListsViewModel viewModel;

    private RadioGroup rgProfitabilitySource;
    private RadioButton rbSourceModel;
    private RadioButton rbSourceHistory;
    private TextView tvHistoryQualityInfo;

    private TextView tvBreakEvenSummaryText;
    private TextView tvNtSliderLabel;
    private Slider sliderNtRatio;

    private PriceListProfitabilityChartView chartProfitability;
    private TextView tvTechnicalNoticeA;
    private TextView tvTechnicalNoticeB;

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
        tvHistoryQualityInfo = view.findViewById(R.id.tvHistoryQualityInfo);

        tvBreakEvenSummaryText = view.findViewById(R.id.tvBreakEvenSummaryText);
        tvNtSliderLabel = view.findViewById(R.id.tvNtSliderLabel);
        sliderNtRatio = view.findViewById(R.id.sliderNtRatio);

        chartProfitability = view.findViewById(R.id.chartProfitability);
        tvTechnicalNoticeA = view.findViewById(R.id.tvTechnicalNoticeA);
        tvTechnicalNoticeB = view.findViewById(R.id.tvTechnicalNoticeB);

        rgProfitabilitySource.setOnCheckedChangeListener((group, checkedId) -> updateProfitabilityUI());

        sliderNtRatio.addOnChangeListener((slider, value, fromUser) -> {
            tvNtSliderLabel.setText(String.format(java.util.Locale.getDefault(), "Podíl nízkého tarifu (NT): %.0f %%", value));
            updateProfitabilityUI();
        });

        viewModel.getScenario().observe(getViewLifecycleOwner(), scenario -> updateProfitabilityUI());
        viewModel.getSelectedPriceLists().observe(getViewLifecycleOwner(), priceLists -> updateProfitabilityUI());
    }


    private void updateProfitabilityUI() {
        PriceListComparisonScenario scenario = viewModel.getScenario().getValue();
        List<PriceListModel> selectedPriceLists = viewModel.getSelectedPriceLists().getValue();

        if (scenario == null || selectedPriceLists == null || selectedPriceLists.isEmpty()) {
            tvBreakEvenSummaryText.setText("Pro porovnání výhodnosti sazeb vyberte alespoň 2 ceníky");
            chartProfitability.setChartData(PriceListProfitabilityChartView.Mode.COST_VS_CONSUMPTION, new ArrayList<>(), null, 0);
            return;
        }

        boolean useHistory = rbSourceHistory.isChecked();
        double currentTotalMwh = scenario.getTotalKwh() / 1000.0;
        double ntRatio = sliderNtRatio.getValue() / 100.0;

        if (useHistory) {
            tvHistoryQualityInfo.setVisibility(View.VISIBLE);
            ShPSubscriptionPoint shP = new ShPSubscriptionPoint(requireContext());
            long idSub = shP.get(ShPSubscriptionPoint.ID_SUBSCRIPTION_POINT_LONG, -1L);
            if (idSub > 0) {
                DataSubscriptionPointSource dataSource = new DataSubscriptionPointSource(requireContext());
                dataSource.open();
                SubscriptionPointModel subPoint = dataSource.loadSubscriptionPoint(idSub);
                dataSource.close();

                if (subPoint != null) {
                    HistoricalConsumptionResult histRes = HistoricalReadingConsumptionHelper.calculateConsumptionForYear(requireContext(), subPoint, -1);
                    if (histRes.totalKwh > 0) {
                        currentTotalMwh = histRes.totalKwh / 1000.0;
                        ntRatio = histRes.ntPercent / 100.0;
                        tvHistoryQualityInfo.setText("Kvalita dat: " + histRes.qualityNote + " (Rok " + histRes.year + ")");
                    } else {
                        tvHistoryQualityInfo.setText("Historické odečty nebyly nalezeny (použita modelová spotřeba)");
                    }
                }
            }
        } else {
            tvHistoryQualityInfo.setVisibility(View.GONE);
        }

        PriceListModel plA = selectedPriceLists.get(0);
        PriceListModel plB = selectedPriceLists.size() > 1 ? selectedPriceLists.get(1) : selectedPriceLists.get(0);

        BreakEvenResult breakEvenResult = BreakEvenCalculator.calculateBreakEvenConsumption(
                plA, plB, ntRatio, scenario.getMonths(), (int) scenario.getCountPhaze(), (int) scenario.getPowerBreaker()
        );

        tvBreakEvenSummaryText.setText(breakEvenResult.textSummary);
        tvTechnicalNoticeA.setText(breakEvenResult.technicalConditionNoticeA);
        tvTechnicalNoticeB.setText(breakEvenResult.technicalConditionNoticeB);

        // Generujeme křivky nákladů v závislosti na spotřebě 0..10 MWh
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
