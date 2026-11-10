package cz.xlisto.elektrodroid.modules.pricelist;


import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import java.util.List;

import cz.xlisto.elektrodroid.R;
import cz.xlisto.elektrodroid.modules.pricelist.models.PriceListComparisonResult;
import cz.xlisto.elektrodroid.ownview.PriceListCompareChartView;
import cz.xlisto.elektrodroid.ownview.PriceListCompareChartView.DisplayMetric;


/**
 * Záložka „Grafy“ pro vizualizaci vybraných cenových veličin a struktury ceny.
 */
public class PriceListCompareChartsFragment extends Fragment {

    private PriceListsViewModel viewModel;
    private Spinner spMetricSelector;
    private PriceListCompareChartView chartMetric;

    private final DisplayMetric[] metrics = new DisplayMetric[]{
            DisplayMetric.TOTAL_ANNUAL_COST,
            DisplayMetric.AVG_MONTHLY_COST,
            DisplayMetric.VT_PRICE,
            DisplayMetric.NT_PRICE,
            DisplayMetric.FIXED_MONTHLY_FEE,
            DisplayMetric.REGULATED_COMPONENT,
            DisplayMetric.UNREGULATED_COMPONENT,
            DisplayMetric.PRICE_STRUCTURE_STACKED,
            DisplayMetric.DIFF_VS_CHEAPEST
    };

    private final String[] metricNames = new String[]{
            "Celkové roční náklady (Kč)",
            "Průměrné měsíční náklady (Kč/měs)",
            "Obchodní cena VT (Kč/MWh)",
            "Obchodní cena NT (Kč/MWh)",
            "Fixní měsíční platby (Kč/měs)",
            "Regulovaná složka (Kč/rok)",
            "Neregulovaná složka (Kč/rok)",
            "Struktura ceny (Obchodní + Regulace + Daně)",
            "Rozdíl oproti nejlevnějšímu (Kč)"
    };


    public static PriceListCompareChartsFragment newInstance() {
        return new PriceListCompareChartsFragment();
    }


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_price_list_compare_charts, container, false);
    }


    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(requireParentFragment()).get(PriceListsViewModel.class);

        spMetricSelector = view.findViewById(R.id.spMetricSelector);
        chartMetric = view.findViewById(R.id.chartMetric);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, metricNames);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spMetricSelector.setAdapter(adapter);

        spMetricSelector.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                updateChart();
            }


            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        viewModel.getComparisonResults().observe(getViewLifecycleOwner(), results -> updateChart());
    }


    private void updateChart() {
        List<PriceListComparisonResult> results = viewModel.getComparisonResults().getValue();
        int selectedIndex = spMetricSelector.getSelectedItemPosition();
        if (selectedIndex < 0 || selectedIndex >= metrics.length) selectedIndex = 0;

        DisplayMetric metric = metrics[selectedIndex];
        chartMetric.setData(results, metric);
    }

}
