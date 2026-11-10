package cz.xlisto.elektrodroid.modules.pricelist;

import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.widget.TextViewCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.color.MaterialColors;

import java.util.List;

import cz.xlisto.elektrodroid.R;
import cz.xlisto.elektrodroid.format.DecimalFormatHelper;
import cz.xlisto.elektrodroid.modules.pricelist.engine.PriceListCalculationEngine;
import cz.xlisto.elektrodroid.modules.pricelist.models.PriceListComparisonResult;
import cz.xlisto.elektrodroid.modules.pricelist.models.PriceListComparisonScenario;
import cz.xlisto.elektrodroid.ownview.PriceListCompareChartView;

/**
 * Záložka „Přehled“ pro zobrazení žebříčku ceníků seřazeného od nejlevnějšího s vyznačením úspor a oddělených tabulek pro obchodní, regulované a celkové součtové ceny.
 * U jednotarifních sazeb (D01d, D02d) automaticky skrývá řádky nízkého tarifu (NT).
 */
public class PriceListCompareOverviewFragment extends Fragment {

    private TextView tvOverviewParamsSummary;
    private LinearLayout llKeyPricesUnregulatedContainer;
    private MaterialCardView cardKeyPricesRegulated;
    private LinearLayout llKeyPricesRegulatedContainer;
    private LinearLayout llKeyPricesTotalContainer;
    private PriceListCompareChartView chartOverview;
    private LinearLayout llOverviewListContainer;

    private final int[][] pillColors = new int[][]{
            {0xFFE8F0FE, 0xFF1565C0}, // Světle modrá, tmavě modrý kód
            {0xFFE8F5E9, 0xFF2E7D32}, // Světle zelená, tmavě zelený kód
            {0xFFF3E5F5, 0xFF6A1B9A}, // Světle fialová, tmavě fialový kód
            {0xFFFFF3E0, 0xFFE65100}, // Světle oranžová, tmavě oranžový kód
            {0xFFE0F7FA, 0xFF00838F}  // Světle azurová, tmavě azurový kód
    };

    public static PriceListCompareOverviewFragment newInstance() {
        return new PriceListCompareOverviewFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_price_list_compare_overview, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        PriceListsViewModel viewModel = new ViewModelProvider(requireParentFragment()).get(PriceListsViewModel.class);

        tvOverviewParamsSummary = view.findViewById(R.id.tvOverviewParamsSummary);
        llKeyPricesUnregulatedContainer = view.findViewById(R.id.llKeyPricesUnregulatedContainer);
        cardKeyPricesRegulated = view.findViewById(R.id.cardKeyPricesRegulated);
        llKeyPricesRegulatedContainer = view.findViewById(R.id.llKeyPricesRegulatedContainer);
        llKeyPricesTotalContainer = view.findViewById(R.id.llKeyPricesTotalContainer);
        chartOverview = view.findViewById(R.id.chartOverview);
        llOverviewListContainer = view.findViewById(R.id.llOverviewListContainer);

        viewModel.getScenario().observe(getViewLifecycleOwner(), this::updateScenarioSummary);
        viewModel.getComparisonResults().observe(getViewLifecycleOwner(), this::updateResultsUI);
    }

    private void updateScenarioSummary(PriceListComparisonScenario scenario) {
        if (scenario == null) return;
        double vtMwh = scenario.getVtKwh() / 1000.0;
        double ntMwh = scenario.getNtKwh() / 1000.0;

        String summary = String.format(
                java.util.Locale.getDefault(),
                "Spotřeba: %.2f MWh VT + %.2f MWh NT (celkem %.2f MWh) • Jistič: %.0fx%.0f A • Období: %.0f měs.",
                vtMwh, ntMwh, vtMwh + ntMwh,
                scenario.getCountPhaze(), scenario.getPowerBreaker(), scenario.getMonths()
        );
        tvOverviewParamsSummary.setText(summary);
    }

    private void updateResultsUI(List<PriceListComparisonResult> results) {
        if (results == null || results.isEmpty()) {
            llOverviewListContainer.removeAllViews();
            llKeyPricesUnregulatedContainer.removeAllViews();
            llKeyPricesRegulatedContainer.removeAllViews();
            llKeyPricesTotalContainer.removeAllViews();
            cardKeyPricesRegulated.setVisibility(View.GONE);
            chartOverview.setData(null, PriceListCompareChartView.DisplayMetric.TOTAL_ANNUAL_COST);
            return;
        }

        // 1. Obchodní ceny
        buildKeyPricesTable(llKeyPricesUnregulatedContainer, results, false);

        // 2. Regulované ceny (pokud se liší)
        if (areRegulatedComponentsDifferent(results)) {
            cardKeyPricesRegulated.setVisibility(View.VISIBLE);
            buildKeyPricesTable(llKeyPricesRegulatedContainer, results, true);
        } else {
            cardKeyPricesRegulated.setVisibility(View.GONE);
        }

        // 3. Celkové ceny pod obchodními i regulovanými cenami
        buildTotalPricesTable(llKeyPricesTotalContainer, results);

        chartOverview.setData(results, PriceListCompareChartView.DisplayMetric.TOTAL_ANNUAL_COST);
        llOverviewListContainer.removeAllViews();

        int textColorPrimary = MaterialColors.getColor(requireContext(), com.google.android.material.R.attr.colorOnSurface, ContextCompat.getColor(requireContext(), R.color.color_axis));
        int textColorSecondary = MaterialColors.getColor(requireContext(), com.google.android.material.R.attr.colorOnSurfaceVariant, 0xFF757575);

        for (int i = 0; i < results.size(); i++) {
            PriceListComparisonResult res = results.get(i);

            MaterialCardView card = new MaterialCardView(requireContext());
            card.setRadius(12 * getResources().getDisplayMetrics().density);
            card.setCardElevation(2 * getResources().getDisplayMetrics().density);
            card.setStrokeWidth((int) (1 * getResources().getDisplayMetrics().density));
            card.setStrokeColor(ContextCompat.getColor(requireContext(), R.color.color_axis));

            LinearLayout cardContent = new LinearLayout(requireContext());
            cardContent.setOrientation(LinearLayout.VERTICAL);
            int p = (int) (14 * getResources().getDisplayMetrics().density);
            cardContent.setPadding(p, p, p, p);

            // Titulek v žebříčku ceníků
            TextView tvTitle = new TextView(requireContext());
            tvTitle.setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_TitleMedium);
            String titleText = (i + 1) + ". " + res.getPriceList().getNameWithPeriod();
            tvTitle.setText(titleText);
            tvTitle.setTextColor(textColorPrimary);
            cardContent.addView(tvTitle);

            // Subtitulek
            TextView tvSubtitle = new TextView(requireContext());
            tvSubtitle.setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodySmall);
            String periodText = res.getPriceList().getPlatnostText();
            String pozeInfo = res.getPozeTypeDescription();
            StringBuilder sbSubtitle = new StringBuilder();
            sbSubtitle.append("Dodavatel: ").append(res.getPriceList().getFirma())
                    .append(" • Distribuce: ").append(res.getPriceList().getDistribuce());
            if (!periodText.isEmpty()) {
                sbSubtitle.append(" • Platnost: ").append(periodText);
            }
            if (!pozeInfo.isEmpty()) {
                sbSubtitle.append(" • ").append(pozeInfo);
            }
            tvSubtitle.setText(sbSubtitle.toString());
            tvSubtitle.setTextColor(textColorSecondary);
            cardContent.addView(tvSubtitle);

            // Cena
            TextView tvPrice = new TextView(requireContext());
            tvPrice.setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_HeadlineSmall);
            String priceText = String.format(java.util.Locale.getDefault(), "%s Kč s DPH", DecimalFormatHelper.df0.format(res.getTotalWithDph()));
            tvPrice.setText(priceText);
            tvPrice.setTextColor(textColorPrimary);
            cardContent.addView(tvPrice);

            // Rozdíl
            TextView tvDiff = new TextView(requireContext());
            tvDiff.setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodyMedium);

            if (res.isCheapest()) {
                tvDiff.setText("★ NEJVÝHODNĚJŠÍ VARIANTA");
                tvDiff.setTextColor(0xFF2E7D32); // Zelená
                card.setStrokeColor(0xFF2E7D32);
                card.setStrokeWidth((int) (2 * getResources().getDisplayMetrics().density));
            } else {
                PriceListComparisonResult cheapest = results.get(0);
                boolean sameSeries = res.getPriceList().getProdukt().equalsIgnoreCase(cheapest.getPriceList().getProdukt())
                        || (res.getPriceList().getRada() != null && res.getPriceList().getRada().equalsIgnoreCase(cheapest.getPriceList().getRada()));

                String diffText;
                if (sameSeries && !cheapest.getPriceList().getPlatnostText().isEmpty()) {
                    diffText = String.format(
                            java.util.Locale.getDefault(),
                            "+%s Kč (+%.1f %%) oproti ceníku stejné řady z období (%s)",
                            DecimalFormatHelper.df0.format(res.getDiffVsCheapest()),
                            res.getPercentDiffVsCheapest(),
                            cheapest.getPriceList().getPlatnostText()
                    );
                } else {
                    diffText = String.format(
                            java.util.Locale.getDefault(),
                            "+%s Kč (+%.1f %%) oproti %s (%s)",
                            DecimalFormatHelper.df0.format(res.getDiffVsCheapest()),
                            res.getPercentDiffVsCheapest(),
                            cheapest.getPriceList().getProdukt(),
                            cheapest.getPriceList().getPlatnostText()
                    );
                }
                tvDiff.setText(diffText);
                tvDiff.setTextColor(0xFFD32F2F); // Červená
            }
            cardContent.addView(tvDiff);

            card.addView(cardContent);

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            lp.setMargins(0, 0, 0, (int) (12 * getResources().getDisplayMetrics().density));
            card.setLayoutParams(lp);

            llOverviewListContainer.addView(card);
        }
    }


    private boolean isAllSingleTariff(List<PriceListComparisonResult> results) {
        if (results == null || results.isEmpty()) return false;
        for (PriceListComparisonResult res : results) {
            if (!PriceListCalculationEngine.isSingleTariff(res.getPriceList())) {
                return false;
            }
        }
        return true;
    }

    private boolean areRegulatedComponentsDifferent(List<PriceListComparisonResult> results) {
        if (results == null || results.size() < 2) return false;
        PriceListComparisonResult first = results.get(0);
        for (int i = 1; i < results.size(); i++) {
            PriceListComparisonResult other = results.get(i);
            if (Math.abs(first.getRegulatedVtWithDph() - other.getRegulatedVtWithDph()) > 0.01
                    || Math.abs(first.getRegulatedNtWithDph() - other.getRegulatedNtWithDph()) > 0.01
                    || Math.abs(first.getRegulatedMonthlyWithDph() - other.getRegulatedMonthlyWithDph()) > 0.01
                    || Math.abs(first.getRegulatedTotalCost() - other.getRegulatedTotalCost()) > 0.01) {
                return true;
            }
        }
        return false;
    }

    private void buildKeyPricesTable(LinearLayout container, List<PriceListComparisonResult> results, boolean isRegulated) {
        container.removeAllViews();
        float density = getResources().getDisplayMetrics().density;
        boolean allSingle = isAllSingleTariff(results);

        int textColorPrimary = MaterialColors.getColor(requireContext(), com.google.android.material.R.attr.colorOnSurface, ContextCompat.getColor(requireContext(), R.color.color_axis));
        int textColorSecondary = MaterialColors.getColor(requireContext(), com.google.android.material.R.attr.colorOnSurfaceVariant, 0xFF757575);

        LinearLayout leftLabelsCol = new LinearLayout(requireContext());
        leftLabelsCol.setOrientation(LinearLayout.VERTICAL);
        leftLabelsCol.setPadding(0, 0, (int) (12 * density), 0);

        int rowCount = isRegulated ? 5 : 3;
        String[] rowIcons = isRegulated
                ? new String[]{"⚡", "🌙", "🪙", "🌿", "🌿"}
                : new String[]{"⚡", "🌙", "🪙"};
        String[] rowTitles = isRegulated
                ? new String[]{"Distribuce VT", "Distribuce NT", "Platba za jistič", "POZE dle spotřeby", "POZE dle jističe"}
                : new String[]{"Vysoký tarif", "Nízký tarif", "Stálá platba"};
        String[] rowUnits = isRegulated
                ? new String[]{"Kč/MWh", "Kč/MWh", "Kč/měsíc", "Kč/MWh (max. 495 bez DPH)", "Kč/A/měsíc"}
                : new String[]{"Kč/MWh", "Kč/MWh", "Kč/měsíc"};

        TextView headerBlank = new TextView(requireContext());
        headerBlank.setHeight((int) (24 * density));
        leftLabelsCol.addView(headerBlank);

        for (int r = 0; r < rowCount; r++) {
            if (allSingle && r == 1) continue; // Skryjeme řádek NT pro jednotarifní ceníky

            LinearLayout rowLabelLayout = new LinearLayout(requireContext());
            rowLabelLayout.setOrientation(LinearLayout.HORIZONTAL);
            rowLabelLayout.setGravity(Gravity.CENTER_VERTICAL);
            rowLabelLayout.setMinimumHeight((int) (52 * density));

            TextView tvIcon = new TextView(requireContext());
            tvIcon.setText(rowIcons[r]);
            tvIcon.setTextSize(16);
            tvIcon.setPadding(0, 0, (int) (8 * density), 0);
            rowLabelLayout.addView(tvIcon);

            LinearLayout titleUnitBox = new LinearLayout(requireContext());
            titleUnitBox.setOrientation(LinearLayout.VERTICAL);

            TextView tvTitle = new TextView(requireContext());
            tvTitle.setText(rowTitles[r]);
            tvTitle.setTypeface(null, Typeface.BOLD);
            tvTitle.setTextSize(13);
            tvTitle.setTextColor(textColorPrimary);
            titleUnitBox.addView(tvTitle);

            TextView tvUnit = new TextView(requireContext());
            tvUnit.setText(rowUnits[r]);
            tvUnit.setTextSize(10);
            tvUnit.setTextColor(textColorSecondary);
            titleUnitBox.addView(tvUnit);

            rowLabelLayout.addView(titleUnitBox);
            leftLabelsCol.addView(rowLabelLayout);
        }

        container.addView(leftLabelsCol);

        for (int col = 0; col < results.size(); col++) {
            PriceListComparisonResult res = results.get(col);
            int[] palette = pillColors[col % pillColors.length];

            LinearLayout priceCol = new LinearLayout(requireContext());
            priceCol.setOrientation(LinearLayout.VERTICAL);
            priceCol.setPadding((int) (4 * density), 0, (int) (4 * density), 0);

            TextView colHeader = new TextView(requireContext());
            colHeader.setText(res.getPriceList().getProdukt());
            colHeader.setTypeface(null, Typeface.BOLD);
            colHeader.setTextSize(11);
            colHeader.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
            colHeader.setPadding(0, 0, (int) (12 * density), 0);
            colHeader.setTextColor(palette[1]);
            colHeader.setHeight((int) (24 * density));
            priceCol.addView(colHeader);

            double[] rowVals = new double[rowCount];
            if (!isRegulated) {
                rowVals[0] = res.getMerchantVtWithDph();
                rowVals[1] = res.getMerchantNtWithDph();
                rowVals[2] = res.getMerchantMonthlyWithDph();
            } else {
                rowVals[0] = res.getRegulatedVtWithDph();
                rowVals[1] = res.getRegulatedNtWithDph();
                rowVals[2] = res.getRegulatedMonthlyWithDph();
                rowVals[3] = res.getPozeConsumptionWithDph();
                rowVals[4] = res.getPozeBreakerWithDph();
            }

            for (int r = 0; r < rowCount; r++) {
                if (allSingle && r == 1) continue; // Skryjeme řádek NT pro jednotarifní ceníky

                double currentVal = rowVals[r];

                double minVal = Double.MAX_VALUE;
                double maxVal = -Double.MAX_VALUE;
                for (PriceListComparisonResult other : results) {
                    double otherVal = !isRegulated ? getMerchantRowVal(other, r) : getRegulatedRowVal(other, r);
                    if (otherVal > 0) {
                        if (otherVal < minVal) minVal = otherVal;
                        if (otherVal > maxVal) maxVal = otherVal;
                    }
                }
                boolean isMin = (maxVal - minVal > 0.01) && (Math.abs(currentVal - minVal) < 0.01) && (currentVal > 0);

                FrameLayout pillWrapper = new FrameLayout(requireContext());
                LinearLayout.LayoutParams wrapperLp = new LinearLayout.LayoutParams(
                        (int) (105 * density),
                        (int) (46 * density)
                );
                wrapperLp.setMargins(0, (int) (3 * density), 0, (int) (3 * density));
                pillWrapper.setLayoutParams(wrapperLp);

                GradientDrawable bgPill = new GradientDrawable();
                bgPill.setCornerRadius(10 * density);
                bgPill.setColor(palette[0]);
                pillWrapper.setBackground(bgPill);

                TextView tvValue = new TextView(requireContext());
                tvValue.setText(DecimalFormatHelper.df2.format(currentVal));
                tvValue.setTypeface(null, Typeface.BOLD);
                tvValue.setTextColor(palette[1]);
                tvValue.setSingleLine(true);
                tvValue.setMaxLines(1);
                tvValue.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
                tvValue.setPadding((int) (4 * density), (int) (6 * density), (int) (12 * density), (int) (2 * density));
                TextViewCompat.setAutoSizeTextTypeUniformWithConfiguration(tvValue, 8, 13, 1, TypedValue.COMPLEX_UNIT_SP);

                FrameLayout.LayoutParams valLp = new FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT
                );
                valLp.gravity = Gravity.END | Gravity.CENTER_VERTICAL;
                tvValue.setLayoutParams(valLp);
                pillWrapper.addView(tvValue);

                if (isMin) {
                    TextView tvCrown = new TextView(requireContext());
                    tvCrown.setText("👑");
                    tvCrown.setTextSize(12);
                    FrameLayout.LayoutParams crownLp = new FrameLayout.LayoutParams(
                            FrameLayout.LayoutParams.WRAP_CONTENT,
                            FrameLayout.LayoutParams.WRAP_CONTENT
                    );
                    crownLp.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
                    crownLp.setMargins(0, (int) (-6 * density), 0, 0);
                    tvCrown.setLayoutParams(crownLp);
                    pillWrapper.addView(tvCrown);
                }

                priceCol.addView(pillWrapper);
            }

            container.addView(priceCol);
        }
    }

    private void buildTotalPricesTable(LinearLayout container, List<PriceListComparisonResult> results) {
        container.removeAllViews();
        float density = getResources().getDisplayMetrics().density;
        boolean allSingle = isAllSingleTariff(results);

        int textColorPrimary = MaterialColors.getColor(requireContext(), com.google.android.material.R.attr.colorOnSurface, ContextCompat.getColor(requireContext(), R.color.color_axis));
        int textColorSecondary = MaterialColors.getColor(requireContext(), com.google.android.material.R.attr.colorOnSurfaceVariant, 0xFF757575);

        LinearLayout leftLabelsCol = new LinearLayout(requireContext());
        leftLabelsCol.setOrientation(LinearLayout.VERTICAL);
        leftLabelsCol.setPadding(0, 0, (int) (12 * density), 0);

        String[] rowIcons = new String[]{"⚡", "🌙", "🪙"};
        String[] rowTitles = new String[]{"Celkem VT", "Celkem NT", "Celkové fixní platby"};
        String[] rowUnits = new String[]{"Kč/MWh", "Kč/MWh", "Kč/měsíc"};

        TextView headerBlank = new TextView(requireContext());
        headerBlank.setHeight((int) (24 * density));
        leftLabelsCol.addView(headerBlank);

        for (int r = 0; r < 3; r++) {
            if (allSingle && r == 1) continue; // Skryjeme řádek Celkem NT pro jednotarifní ceníky

            LinearLayout rowLabelLayout = new LinearLayout(requireContext());
            rowLabelLayout.setOrientation(LinearLayout.HORIZONTAL);
            rowLabelLayout.setGravity(Gravity.CENTER_VERTICAL);
            rowLabelLayout.setMinimumHeight((int) (52 * density));

            TextView tvIcon = new TextView(requireContext());
            tvIcon.setText(rowIcons[r]);
            tvIcon.setTextSize(16);
            tvIcon.setPadding(0, 0, (int) (8 * density), 0);
            rowLabelLayout.addView(tvIcon);

            LinearLayout titleUnitBox = new LinearLayout(requireContext());
            titleUnitBox.setOrientation(LinearLayout.VERTICAL);

            TextView tvTitle = new TextView(requireContext());
            tvTitle.setText(rowTitles[r]);
            tvTitle.setTypeface(null, Typeface.BOLD);
            tvTitle.setTextSize(13);
            tvTitle.setTextColor(textColorPrimary);
            titleUnitBox.addView(tvTitle);

            TextView tvUnit = new TextView(requireContext());
            tvUnit.setText(rowUnits[r]);
            tvUnit.setTextSize(10);
            tvUnit.setTextColor(textColorSecondary);
            titleUnitBox.addView(tvUnit);

            rowLabelLayout.addView(titleUnitBox);
            leftLabelsCol.addView(rowLabelLayout);
        }

        container.addView(leftLabelsCol);

        for (int col = 0; col < results.size(); col++) {
            PriceListComparisonResult res = results.get(col);
            int[] palette = pillColors[col % pillColors.length];

            LinearLayout priceCol = new LinearLayout(requireContext());
            priceCol.setOrientation(LinearLayout.VERTICAL);
            priceCol.setPadding((int) (4 * density), 0, (int) (4 * density), 0);

            TextView colHeader = new TextView(requireContext());
            colHeader.setText(res.getPriceList().getProdukt());
            colHeader.setTypeface(null, Typeface.BOLD);
            colHeader.setTextSize(11);
            colHeader.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
            colHeader.setPadding(0, 0, (int) (12 * density), 0);
            colHeader.setTextColor(palette[1]);
            colHeader.setHeight((int) (24 * density));
            priceCol.addView(colHeader);

            double[] rowVals = new double[]{
                    res.getFullVtPricePerMwhWithDph(),
                    res.getFullNtPricePerMwhWithDph(),
                    res.getFullMonthlyFixedCostWithDph()
            };

            for (int r = 0; r < 3; r++) {
                if (allSingle && r == 1)
                    continue; // Skryjeme řádek Celkem NT pro jednotarifní ceníky

                double currentVal = rowVals[r];

                double minVal = Double.MAX_VALUE;
                double maxVal = -Double.MAX_VALUE;
                for (PriceListComparisonResult other : results) {
                    double otherVal = getTotalRowVal(other, r);
                    if (otherVal > 0) {
                        if (otherVal < minVal) minVal = otherVal;
                        if (otherVal > maxVal) maxVal = otherVal;
                    }
                }
                boolean isMin = (maxVal - minVal > 0.01) && (Math.abs(currentVal - minVal) < 0.01) && (currentVal > 0);

                FrameLayout pillWrapper = new FrameLayout(requireContext());
                LinearLayout.LayoutParams wrapperLp = new LinearLayout.LayoutParams(
                        (int) (105 * density),
                        (int) (46 * density)
                );
                wrapperLp.setMargins(0, (int) (3 * density), 0, (int) (3 * density));
                pillWrapper.setLayoutParams(wrapperLp);

                GradientDrawable bgPill = new GradientDrawable();
                bgPill.setCornerRadius(10 * density);
                bgPill.setColor(palette[0]);
                pillWrapper.setBackground(bgPill);

                TextView tvValue = new TextView(requireContext());
                tvValue.setText(DecimalFormatHelper.df2.format(currentVal));
                tvValue.setTypeface(null, Typeface.BOLD);
                tvValue.setTextColor(palette[1]);
                tvValue.setSingleLine(true);
                tvValue.setMaxLines(1);
                tvValue.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
                tvValue.setPadding((int) (4 * density), (int) (6 * density), (int) (12 * density), (int) (2 * density));
                TextViewCompat.setAutoSizeTextTypeUniformWithConfiguration(tvValue, 8, 13, 1, TypedValue.COMPLEX_UNIT_SP);

                FrameLayout.LayoutParams valLp = new FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT
                );
                valLp.gravity = Gravity.END | Gravity.CENTER_VERTICAL;
                tvValue.setLayoutParams(valLp);
                pillWrapper.addView(tvValue);

                if (isMin) {
                    TextView tvCrown = new TextView(requireContext());
                    tvCrown.setText("👑");
                    tvCrown.setTextSize(12);
                    FrameLayout.LayoutParams crownLp = new FrameLayout.LayoutParams(
                            FrameLayout.LayoutParams.WRAP_CONTENT,
                            FrameLayout.LayoutParams.WRAP_CONTENT
                    );
                    crownLp.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
                    crownLp.setMargins(0, (int) (-6 * density), 0, 0);
                    tvCrown.setLayoutParams(crownLp);
                    pillWrapper.addView(tvCrown);
                }

                priceCol.addView(pillWrapper);
            }

            container.addView(priceCol);
        }
    }

    private double getMerchantRowVal(PriceListComparisonResult res, int row) {
        return switch (row) {
            case 0 -> res.getMerchantVtWithDph();
            case 1 -> res.getMerchantNtWithDph();
            case 2 -> res.getMerchantMonthlyWithDph();
            default -> 0;
        };
    }

    private double getRegulatedRowVal(PriceListComparisonResult res, int row) {
        return switch (row) {
            case 0 -> res.getRegulatedVtWithDph();
            case 1 -> res.getRegulatedNtWithDph();
            case 2 -> res.getRegulatedMonthlyWithDph();
            case 3 -> res.getPozeConsumptionWithDph();
            case 4 -> res.getPozeBreakerWithDph();
            default -> 0;
        };
    }

    private double getTotalRowVal(PriceListComparisonResult res, int row) {
        return switch (row) {
            case 0 -> res.getFullVtPricePerMwhWithDph();
            case 1 -> res.getFullNtPricePerMwhWithDph();
            case 2 -> res.getFullMonthlyFixedCostWithDph();
            default -> 0;
        };
    }

}
