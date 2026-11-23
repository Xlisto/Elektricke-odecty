package cz.xlisto.elektrodroid.modules.dashboard;


import static cz.xlisto.elektrodroid.models.PriceListModel.NEW_POZE_YEAR;
import static cz.xlisto.elektrodroid.utils.DensityUtils.dpToPx;
import static cz.xlisto.elektrodroid.utils.FragmentChange.Transaction.MOVE;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.materialswitch.MaterialSwitch;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import cz.xlisto.elektrodroid.MainActivity;
import cz.xlisto.elektrodroid.R;
import cz.xlisto.elektrodroid.databaze.DataGraphMonth;
import cz.xlisto.elektrodroid.databaze.DataHdoSource;
import cz.xlisto.elektrodroid.databaze.DataInvoiceSource;
import cz.xlisto.elektrodroid.databaze.DataMonthlyReadingSource;
import cz.xlisto.elektrodroid.databaze.DataPriceListSource;
import cz.xlisto.elektrodroid.databaze.DataSettingsSource;
import cz.xlisto.elektrodroid.format.DecimalFormatHelper;
import cz.xlisto.elektrodroid.format.SimpleDateFormatHelper;
import cz.xlisto.elektrodroid.models.ConsuptionModel;
import cz.xlisto.elektrodroid.models.HdoModel;
import cz.xlisto.elektrodroid.models.InvoiceListModel;
import cz.xlisto.elektrodroid.models.InvoiceModel;
import cz.xlisto.elektrodroid.models.MonthlyReadingModel;
import cz.xlisto.elektrodroid.models.PozeModel;
import cz.xlisto.elektrodroid.models.PriceListModel;
import cz.xlisto.elektrodroid.models.PriceListRegulBuilder;
import cz.xlisto.elektrodroid.models.SubscriptionPointModel;
import cz.xlisto.elektrodroid.modules.graphmonth.ConsuptionContainer;
import cz.xlisto.elektrodroid.modules.hdo.HdoFragment;
import cz.xlisto.elektrodroid.modules.hdo.HdoTime;
import cz.xlisto.elektrodroid.modules.invoice.InvoiceAbstract;
import cz.xlisto.elektrodroid.modules.invoice.InvoiceAdapter;
import cz.xlisto.elektrodroid.modules.invoice.InvoiceTabFragment;
import cz.xlisto.elektrodroid.modules.invoice.MyViewPagerAdapter;
import cz.xlisto.elektrodroid.modules.monthlyreading.MonthlyReadingAddFragment;
import cz.xlisto.elektrodroid.modules.monthlyreading.MonthlyReadingFragment;
import cz.xlisto.elektrodroid.modules.payment.PaymentAdapter;
import cz.xlisto.elektrodroid.ownview.NumbersMeter;
import cz.xlisto.elektrodroid.ownview.ViewHelper;
import cz.xlisto.elektrodroid.dialogs.SubscriptionPointDialogFragment;
import cz.xlisto.elektrodroid.services.HdoAlarmScheduler;
import cz.xlisto.elektrodroid.shp.ShPDashBoard;
import cz.xlisto.elektrodroid.shp.ShPHdo;
import cz.xlisto.elektrodroid.ui.OnPaddingChangeListener;
import cz.xlisto.elektrodroid.utils.Calculation;
import cz.xlisto.elektrodroid.utils.DifferenceDate;
import cz.xlisto.elektrodroid.utils.FragmentChange;
import cz.xlisto.elektrodroid.utils.MainActivityHelper;
import cz.xlisto.elektrodroid.utils.SubscriptionPoint;


/**
 * Nová přehledová obrazovka (Dashboard) podle moderního grafického návrhu.
 * <p>
 * Zobrazuje:
 * <ul>
 *     <li>Přepínač odběrného místa</li>
 *     <li>Aktuální cenu elektřiny (VT/NT) s přepínači Celková/Neregulovaná a DPH + čas dalšího přepnutí HDO</li>
 *     <li>Rychlé dlaždice: Měsíční odečty, 24hodinové analogové hodiny HDO, Nový odečet</li>
 *     <li>Spotřebu aktuálního období (VT/NT pruh + donut graf)</li>
 *     <li>Přehled spotřeby (sloupcový graf s přepínačem Měsíční / Roční)</li>
 *     <li>Vyúčtování aktuálního období (Zálohy, Náklady, Přeplatek/Nedoplatek) a seznam posledních faktur</li>
 * </ul>
 * Rozložení sekcí je řízeno přes {@link DashboardLayoutManager} pro budoucí uživatelské přizpůsobení.
 */
public class NewDashBoardFragment extends Fragment implements OnPaddingChangeListener {

    private static final int MAX_BAR_ENTRIES = 5;
    private static final int MAX_RECENT_INVOICES = 4;

    private NestedScrollView nsvNewDashboard;
    private ImageButton btnPrevSubscriptionPoint;
    private ImageButton btnNextSubscriptionPoint;
    private TextView tvSubscriptionPointName;
    private TextView tvAlertNoSubscriptionPoint;

    private ViewGroup llDashboardPortraitContainer;
    private ViewGroup llDashboardColumnsRow;
    private ViewGroup llDashboardLeftColumn;
    private ViewGroup llDashboardRightColumn;

    // Sekce (widgety) pro DashboardLayoutManager
    private View cardCurrentPrice;
    private View layoutQuickActions;
    private View cardPeriodConsumption;
    private View cardConsumptionOverview;
    private View cardBilling;

    // 1. Karta: Aktuální cena elektřiny
    private MaterialSwitch swPriceTotalKwh;
    private MaterialSwitch swPriceVat;
    private LinearLayout boxPriceVt;
    private LinearLayout boxPriceNt;
    private TextView tvLabelVt;
    private TextView tvLabelNt;
    private ImageView ivCheckVt;
    private ImageView ivCheckNt;
    private TextView tvPriceVtValue;
    private TextView tvPriceNtValue;
    private TextView tvPriceVtUnit;
    private TextView tvPriceNtUnit;
    private TextView tvHdoNextSwitchLabel;
    private TextView tvHdoNextSwitchTime;

    // 2. Sekce: Rychlé dlaždice
    private NumbersMeter numbersMeterDashboardVT;
    private NumbersMeter numbersMeterDashboardNT;
    private DashboardHdoClockView viewHdoClock;

    // 3. Karta: Spotřeba období
    private TextView tvPeriodDateRange;
    private TextView tvPeriodVtValue;
    private View layoutPeriodNtRow;
    private TextView tvPeriodNtValue;
    private DashboardPeriodBarView viewPeriodBar;
    private DashboardDonutChartView viewPeriodDonut;

    // 4. Karta: Přehled spotřeby
    private TextView tvConsumptionChartSubtitle;
    private MaterialSwitch swConsumptionYearly;
    private DashboardConsumptionBarChartView viewConsumptionBarChart;

    // 5. Karta: Vyúčtování a poslední faktury
    private TextView tvBillingPeriod;
    private TextView tvBillingAdvances;
    private TextView tvBillingCosts;
    private LinearLayout boxBillingBalance;
    private TextView tvBillingBalanceLabel;
    private TextView tvBillingBalanceValue;
    private View layoutLastInvoicesSection;
    private LinearLayout llInvoicesContainer;

    private static final String STATE_SELECTED_LOCAL_POINT_ID = "stateSelectedLocalPointId";
    private static final long PERIOD_ANIMATION_DURATION_MS = 550L;

    private ShPDashBoard shPDashBoard;
    private DashboardLayoutManager layoutManager;
    private ArrayList<SubscriptionPointModel> allSubscriptionPoints = new ArrayList<>();
    private int currentPointIndex = 0;
    private SubscriptionPointModel currentPoint;
    private long selectedLocalPointId = -1L;

    private double displayedPeriodVtKwh = 0.0;
    private double displayedPeriodNtKwh = 0.0;
    private float displayedPeriodNtVisibility = 1f;
    private boolean hasInitializedPeriodValues = false;
    private int ntRowMeasuredHeight = 0;
    private ValueAnimator periodValuesAnimator;

    private double displayedBillingAdvancesKc = 0.0;
    private double displayedBillingCostsKc = 0.0;
    private long displayedBillingMinDate = 0L;
    private long displayedBillingMaxDate = 0L;
    private float displayedBalanceColorRatio = 0f;
    private String displayedBalanceLabel = "";
    private boolean hasInitializedBillingValues = false;
    private GradientDrawable balanceBgDrawable;
    private final ArgbEvaluator argbEvaluator = new ArgbEvaluator();
    private final List<Double> displayedRecentInvoiceAmountsKc = new ArrayList<>();
    private final List<Double> displayedRecentInvoiceConsumptionsKwh = new ArrayList<>();
    private ValueAnimator billingValuesAnimator;

    private final List<DashboardConsumptionBarChartView.BarEntry> monthlyChartEntries = new ArrayList<>();
    private final List<DashboardConsumptionBarChartView.BarEntry> yearlyChartEntries = new ArrayList<>();
    private DecimalFormat integerFormat;

    private final Handler hdoHandler = new Handler(Looper.getMainLooper());
    private final Runnable hdoRefreshRunnable = new Runnable() {
        @Override
        public void run() {
            if (isAdded() && currentPoint != null) {
                refreshHdoAndPriceHighlight(false);
            }
            hdoHandler.postDelayed(this, 30_000L);
        }
    };


    public static NewDashBoardFragment newInstance() {
        return new NewDashBoardFragment();
    }


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            selectedLocalPointId = savedInstanceState.getLong(STATE_SELECTED_LOCAL_POINT_ID, -1L);
        }
        requireActivity().getSupportFragmentManager().setFragmentResultListener(
                SubscriptionPointDialogFragment.FLAG_UPDATE_SUBSCRIPTION_POINT,
                this,
                (requestKey, result) -> {
                    selectedLocalPointId = -1L;
                    loadSubscriptionPointsAndRefresh();
                }
        );
    }


    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putLong(STATE_SELECTED_LOCAL_POINT_ID, selectedLocalPointId);
    }


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_new_dashboard, container, false);
    }


    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        requireActivity().invalidateOptionsMenu();

        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.forLanguageTag("cs-CZ"));
        symbols.setGroupingSeparator(' ');
        integerFormat = new DecimalFormat("#,##0", symbols);
        integerFormat.setGroupingUsed(true);

        shPDashBoard = new ShPDashBoard(requireContext());
        layoutManager = new DashboardLayoutManager(requireContext());

        bindViews(view);
        setupScrollListener();
        applyModularLayout();
        setupListeners();
        loadSubscriptionPointsAndRefresh();
    }


    private void bindViews(@NonNull View view) {
        nsvNewDashboard = view.findViewById(R.id.nsvNewDashboard);
        btnPrevSubscriptionPoint = view.findViewById(R.id.btnPrevSubscriptionPoint);
        btnNextSubscriptionPoint = view.findViewById(R.id.btnNextSubscriptionPoint);
        tvSubscriptionPointName = view.findViewById(R.id.tvSubscriptionPointName);
        tvAlertNoSubscriptionPoint = view.findViewById(R.id.tvAlertNoSubscriptionPoint);

        llDashboardPortraitContainer = view.findViewById(R.id.llDashboardPortraitContainer);
        llDashboardColumnsRow = view.findViewById(R.id.llDashboardColumnsRow);
        llDashboardLeftColumn = view.findViewById(R.id.llDashboardLeftColumn);
        llDashboardRightColumn = view.findViewById(R.id.llDashboardRightColumn);

        cardCurrentPrice = view.findViewById(R.id.cardCurrentPrice);
        layoutQuickActions = view.findViewById(R.id.layoutQuickActions);
        cardPeriodConsumption = view.findViewById(R.id.cardPeriodConsumption);
        cardConsumptionOverview = view.findViewById(R.id.cardConsumptionOverview);
        cardBilling = view.findViewById(R.id.cardBilling);

        swPriceTotalKwh = view.findViewById(R.id.swPriceTotalKwh);
        swPriceVat = view.findViewById(R.id.swPriceVat);
        boxPriceVt = view.findViewById(R.id.boxPriceVt);
        boxPriceNt = view.findViewById(R.id.boxPriceNt);
        tvLabelVt = view.findViewById(R.id.tvLabelVt);
        tvLabelNt = view.findViewById(R.id.tvLabelNt);
        ivCheckVt = view.findViewById(R.id.ivCheckVt);
        ivCheckNt = view.findViewById(R.id.ivCheckNt);
        tvPriceVtValue = view.findViewById(R.id.tvPriceVtValue);
        tvPriceNtValue = view.findViewById(R.id.tvPriceNtValue);
        tvPriceVtUnit = view.findViewById(R.id.tvPriceVtUnit);
        tvPriceNtUnit = view.findViewById(R.id.tvPriceNtUnit);
        tvHdoNextSwitchLabel = view.findViewById(R.id.tvHdoNextSwitchLabel);
        tvHdoNextSwitchTime = view.findViewById(R.id.tvHdoNextSwitchTime);

        MaterialCardView cardQuickMonthlyReadings = view.findViewById(R.id.cardQuickMonthlyReadings);
        MaterialCardView cardQuickHdoClock = view.findViewById(R.id.cardQuickHdoClock);
        MaterialCardView cardQuickNewReading = view.findViewById(R.id.cardQuickNewReading);
        numbersMeterDashboardVT = view.findViewById(R.id.numbersMeterDashboardVT);
        numbersMeterDashboardNT = view.findViewById(R.id.numbersMeterDashboardNT);
        viewHdoClock = view.findViewById(R.id.viewHdoClock);

        tvPeriodDateRange = view.findViewById(R.id.tvPeriodDateRange);
        tvPeriodVtValue = view.findViewById(R.id.tvPeriodVtValue);
        layoutPeriodNtRow = view.findViewById(R.id.layoutPeriodNtRow);
        tvPeriodNtValue = view.findViewById(R.id.tvPeriodNtValue);
        viewPeriodBar = view.findViewById(R.id.viewPeriodBar);
        viewPeriodDonut = view.findViewById(R.id.viewPeriodDonut);

        tvConsumptionChartSubtitle = view.findViewById(R.id.tvConsumptionChartSubtitle);
        swConsumptionYearly = view.findViewById(R.id.swConsumptionYearly);
        viewConsumptionBarChart = view.findViewById(R.id.viewConsumptionBarChart);

        tvBillingPeriod = view.findViewById(R.id.tvBillingPeriod);
        tvBillingAdvances = view.findViewById(R.id.tvBillingAdvances);
        tvBillingCosts = view.findViewById(R.id.tvBillingCosts);
        boxBillingBalance = view.findViewById(R.id.boxBillingBalance);
        tvBillingBalanceLabel = view.findViewById(R.id.tvBillingBalanceLabel);
        tvBillingBalanceValue = view.findViewById(R.id.tvBillingBalanceValue);
        layoutLastInvoicesSection = view.findViewById(R.id.layoutLastInvoicesSection);
        llInvoicesContainer = view.findViewById(R.id.llInvoicesContainer);

        cardQuickMonthlyReadings.setOnClickListener(v -> {
            if (currentPoint == null) return;
            applyDashboardSubscriptionPointIfChanged();
            FragmentChange.replace(requireActivity(), MonthlyReadingFragment.newInstance(), MOVE, true);
        });

        cardQuickHdoClock.setOnClickListener(v -> {
            if (currentPoint == null) return;
            applyDashboardSubscriptionPointIfChanged();
            FragmentChange.replace(requireActivity(), HdoFragment.newInstance(), MOVE, true);
        });

        cardQuickNewReading.setOnClickListener(v -> {
            if (currentPoint == null) return;
            applyDashboardSubscriptionPointIfChanged();
            MonthlyReadingAddFragment addFragment = MonthlyReadingAddFragment.newInstance(
                    currentPoint.getTableO(),
                    currentPoint.getTablePLATBY()
            );
            FragmentChange.replace(requireActivity(), addFragment, MOVE, true);
        });
    }


    private void setupScrollListener() {
        if (nsvNewDashboard == null) return;
        final int thresholdPx = (int) (12 * getResources().getDisplayMetrics().density);
        nsvNewDashboard.setOnScrollChangeListener((NestedScrollView.OnScrollChangeListener) (v, scrollX, scrollY, oldScrollX, oldScrollY) -> {
            int dy = scrollY - oldScrollY;
            if (Math.abs(dy) >= thresholdPx && getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).setBottomNavCompact(dy > 0);
            }
        });
    }


    /**
     * Aplikuje modulární rozložení sekcí pomocí {@link DashboardLayoutManager}.
     */
    private void applyModularLayout() {
        Map<DashboardSectionType, View> sectionMap = new EnumMap<>(DashboardSectionType.class);
        sectionMap.put(DashboardSectionType.CURRENT_PRICE, cardCurrentPrice);
        sectionMap.put(DashboardSectionType.QUICK_ACTIONS, layoutQuickActions);
        sectionMap.put(DashboardSectionType.PERIOD_CONSUMPTION, cardPeriodConsumption);
        sectionMap.put(DashboardSectionType.CONSUMPTION_OVERVIEW, cardConsumptionOverview);
        sectionMap.put(DashboardSectionType.BILLING_AND_INVOICES, cardBilling);

        layoutManager.applyLayout(
                sectionMap,
                llDashboardPortraitContainer,
                llDashboardLeftColumn,
                llDashboardRightColumn
        );
    }


    private void setupListeners() {
        boolean showTotalKwh = shPDashBoard.get(ShPDashBoard.PRICE_SHOW_TOTAL_KWH, true);
        boolean withVat = shPDashBoard.get(ShPDashBoard.PRICE_WITH_VAT, true);
        boolean yearlyChart = shPDashBoard.get(ShPDashBoard.CHART_YEARLY_MODE, false);

        swPriceTotalKwh.setChecked(showTotalKwh);
        updatePriceTotalSwitchLabel(showTotalKwh);
        swPriceVat.setChecked(withVat);
        swConsumptionYearly.setChecked(yearlyChart);
        updateConsumptionSwitchLabels(yearlyChart);

        swPriceTotalKwh.setOnCheckedChangeListener((buttonView, isChecked) -> {
            shPDashBoard.set(ShPDashBoard.PRICE_SHOW_TOTAL_KWH, isChecked);
            updatePriceTotalSwitchLabel(isChecked);
            refreshPriceValues();
        });

        swPriceVat.setOnCheckedChangeListener((buttonView, isChecked) -> {
            shPDashBoard.set(ShPDashBoard.PRICE_WITH_VAT, isChecked);
            refreshPriceValues();
        });

        swConsumptionYearly.setOnCheckedChangeListener((buttonView, isChecked) -> {
            shPDashBoard.set(ShPDashBoard.CHART_YEARLY_MODE, isChecked);
            updateConsumptionSwitchLabels(isChecked);
            refreshConsumptionChartDisplay();
        });

        btnPrevSubscriptionPoint.setOnClickListener(v -> {
            if (currentPointIndex > 0) {
                currentPointIndex--;
                selectSubscriptionPoint(currentPointIndex);
            }
        });

        btnNextSubscriptionPoint.setOnClickListener(v -> {
            if (currentPointIndex < allSubscriptionPoints.size() - 1) {
                currentPointIndex++;
                selectSubscriptionPoint(currentPointIndex);
            }
        });
    }


    private void updatePriceTotalSwitchLabel(boolean isTotal) {
        swPriceTotalKwh.setText(isTotal
                ? R.string.dashboard_price_total_label
                : R.string.dashboard_price_unregulated_label);
    }


    private void updateConsumptionSwitchLabels(boolean isYearly) {
        tvConsumptionChartSubtitle.setText(isYearly
                ? R.string.dashboard_yearly_consumption_subtitle
                : R.string.dashboard_monthly_consumption_subtitle);
        swConsumptionYearly.setText(isYearly
                ? R.string.dashboard_toggle_yearly
                : R.string.dashboard_toggle_monthly);
    }


    private void loadSubscriptionPointsAndRefresh() {
        allSubscriptionPoints = SubscriptionPoint.getAllSubscriptionPoints(requireContext());
        SubscriptionPointModel activeGlobal = SubscriptionPoint.load(requireContext());

        if (allSubscriptionPoints == null || allSubscriptionPoints.isEmpty() || activeGlobal == null) {
            currentPoint = null;
            tvSubscriptionPointName.setText(R.string.no_subsriptionpoint);
            btnPrevSubscriptionPoint.setVisibility(View.INVISIBLE);
            btnNextSubscriptionPoint.setVisibility(View.INVISIBLE);
            tvAlertNoSubscriptionPoint.setVisibility(View.VISIBLE);
            setContentContainersVisibility(View.GONE);
            return;
        }

        tvAlertNoSubscriptionPoint.setVisibility(View.GONE);
        setContentContainersVisibility(View.VISIBLE);

        long targetPointId = (selectedLocalPointId != -1L) ? selectedLocalPointId : activeGlobal.getId();
        currentPointIndex = -1;
        for (int idx = 0; idx < allSubscriptionPoints.size(); idx++) {
            if (allSubscriptionPoints.get(idx).getId() == targetPointId) {
                currentPointIndex = idx;
                break;
            }
        }
        if (currentPointIndex < 0) {
            currentPointIndex = 0;
            for (int idx = 0; idx < allSubscriptionPoints.size(); idx++) {
                if (allSubscriptionPoints.get(idx).getId() == activeGlobal.getId()) {
                    currentPointIndex = idx;
                    break;
                }
            }
        }

        currentPoint = allSubscriptionPoints.get(currentPointIndex);
        selectedLocalPointId = currentPoint.getId();
        MainActivityHelper.updateToolbarAndLoadData(requireActivity());
        updateSwitcherUi();
        refreshAllSections();
    }


    private void setContentContainersVisibility(int visibility) {
        if (llDashboardPortraitContainer != null) {
            llDashboardPortraitContainer.setVisibility(visibility);
        }
        if (llDashboardColumnsRow != null) {
            llDashboardColumnsRow.setVisibility(visibility);
        }
    }


    /**
     * Přepne odběrné místo pouze lokálně v rámci přehledu (nemění globální odběrné místo aplikace).
     */
    private void selectSubscriptionPoint(int index) {
        if (index < 0 || index >= allSubscriptionPoints.size()) {
            return;
        }
        currentPointIndex = index;
        currentPoint = allSubscriptionPoints.get(index);
        selectedLocalPointId = currentPoint.getId();
        shPDashBoard.set(ShPDashBoard.SHOW_INVOICE_SUM, index);
        updateSwitcherUi();
        refreshAllSections();
    }


    /**
     * Pokud se lokálně zobrazené odběrné místo v přehledu liší od globálního odběrného místa aplikace,
     * při přechodu do jiného fragmentu přepne globální odběrné místo a zobrazí uživateli upozornění.
     */
    private void applyDashboardSubscriptionPointIfChanged() {
        if (currentPoint == null || !isAdded()) {
            return;
        }
        SubscriptionPointModel globalPoint = SubscriptionPoint.load(requireContext());
        if (globalPoint == null || globalPoint.getId() != currentPoint.getId()) {
            SubscriptionPoint.setCurrentSelection(requireContext(), currentPoint.getId());
            MainActivityHelper.updateToolbarAndLoadData(requireActivity());
            Toast.makeText(
                    requireContext(),
                    getString(R.string.dashboard_subscription_point_switched, currentPoint.getName()),
                    Toast.LENGTH_SHORT
            ).show();
        }
    }


    private void updateSwitcherUi() {
        if (currentPoint == null) return;
        tvSubscriptionPointName.setText(currentPoint.getName());

        int count = allSubscriptionPoints.size();
        if (count <= 1) {
            btnPrevSubscriptionPoint.setVisibility(View.INVISIBLE);
            btnNextSubscriptionPoint.setVisibility(View.INVISIBLE);
        } else {
            btnPrevSubscriptionPoint.setVisibility(View.VISIBLE);
            btnNextSubscriptionPoint.setVisibility(View.VISIBLE);
            boolean canPrev = currentPointIndex > 0;
            boolean canNext = currentPointIndex < count - 1;
            btnPrevSubscriptionPoint.setEnabled(canPrev);
            btnPrevSubscriptionPoint.setAlpha(canPrev ? 1.0f : 0.35f);
            btnNextSubscriptionPoint.setEnabled(canNext);
            btnNextSubscriptionPoint.setAlpha(canNext ? 1.0f : 0.35f);
        }
    }


    private void refreshAllSections() {
        if (currentPoint == null || !isAdded()) return;
        refreshPriceValues();
        refreshMeterStateValues();
        refreshHdoAndPriceHighlight(true);
        refreshPeriodBillingAndInvoices();
        refreshConsumptionOverviewData();
    }


    /**
     * Načte poslední stav elektroměru (VT a NT) z měsíčních odečtů a zobrazí jej v grafických počítadlech {@link NumbersMeter}.
     */
    private void refreshMeterStateValues() {
        if (currentPoint == null || !isAdded()) return;
        if (numbersMeterDashboardVT == null || numbersMeterDashboardNT == null) return;

        double vt = 0.0;
        double nt = 0.0;
        DataMonthlyReadingSource readingSource = new DataMonthlyReadingSource(requireContext());
        try {
            readingSource.open();
            double[] lastVtNt = readingSource.loadLastVtNt(currentPoint.getTableO());
            if (lastVtNt != null && lastVtNt.length >= 2) {
                vt = lastVtNt[0];
                nt = lastVtNt[1];
            }
        } catch (Exception ignored) {
        } finally {
            readingSource.close();
        }

        int maxMeter = 9_999_999;
        int vtTenths = (int) Math.min(maxMeter, Math.max(0L, Math.round(vt * 10.0)));
        int ntTenths = (int) Math.min(maxMeter, Math.max(0L, Math.round(nt * 10.0)));

        numbersMeterDashboardVT.setCurrentNumber(vtTenths);
        numbersMeterDashboardNT.setCurrentNumber(ntTenths);
    }


    /**
     * Načte aktuální ceník pro vybrané odběrné místo a spočítá cenu VT a NT (Kč/kWh)
     * podle stavu přepínačů (Celková / Neregulovaná a S DPH / Bez DPH).
     */
    private void refreshPriceValues() {
        if (currentPoint == null || !isAdded()) return;

        PriceListModel priceList = loadCurrentPriceList(currentPoint);
        if (priceList == null) {
            tvPriceVtValue.setText("0,00");
            tvPriceNtValue.setText("0,00");
            return;
        }

        PriceListModel effectivePriceList = new PriceListRegulBuilder(priceList).getRegulPriceList();
        boolean showTotal = swPriceTotalKwh.isChecked();
        boolean withVat = swPriceVat.isChecked();

        double vtPriceKwh;
        double ntPriceKwh;

        if (showTotal) {
            double[] totalPrices = Calculation.calculatePriceForPriceListKwh(effectivePriceList, currentPoint);
            vtPriceKwh = totalPrices[0];
            ntPriceKwh = totalPrices[1];
        } else {
            vtPriceKwh = effectivePriceList.getPriceVT() / 1000.0;
            ntPriceKwh = effectivePriceList.getPriceNT() / 1000.0;
        }

        if (withVat) {
            double vatMultiplier = 1.0 + (effectivePriceList.getVat() / 100.0);
            vtPriceKwh *= vatMultiplier;
            ntPriceKwh *= vatMultiplier;
        }

        tvPriceVtValue.setText(DecimalFormatHelper.df2.format(vtPriceKwh));
        tvPriceNtValue.setText(DecimalFormatHelper.df2.format(ntPriceKwh));
    }


    @Nullable
    private PriceListModel loadCurrentPriceList(@NonNull SubscriptionPointModel point) {
        long priceListId = -1L;

        DataMonthlyReadingSource readingSource = new DataMonthlyReadingSource(requireContext());
        try {
            readingSource.open();
            MonthlyReadingModel lastReading = readingSource.loadLastMonthlyReadingByDate(point.getTableO());
            if (lastReading != null && lastReading.getPriceListId() > 0) {
                priceListId = lastReading.getPriceListId();
            }
        } catch (Exception ignored) {
        } finally {
            readingSource.close();
        }

        if (priceListId <= 0) {
            DataInvoiceSource invoiceSource = new DataInvoiceSource(requireContext());
            try {
                invoiceSource.open();
                InvoiceModel lastTed = invoiceSource.loadLastInvoiceByDateFromAll(point.getTableTED());
                if (lastTed != null && lastTed.getIdPriceList() > 0) {
                    priceListId = lastTed.getIdPriceList();
                } else {
                    InvoiceModel lastFak = invoiceSource.loadLastInvoiceByDateFromAll(point.getTableFAK());
                    if (lastFak != null && lastFak.getIdPriceList() > 0) {
                        priceListId = lastFak.getIdPriceList();
                    }
                }
            } catch (Exception ignored) {
            } finally {
                invoiceSource.close();
            }
        }

        if (priceListId <= 0) {
            return null;
        }

        DataPriceListSource priceListSource = new DataPriceListSource(requireContext());
        try {
            priceListSource.open();
            return priceListSource.readPrice(priceListId);
        } catch (Exception e) {
            return null;
        } finally {
            priceListSource.close();
        }
    }


    /**
     * Aktualizuje stav HDO (zda běží NT nebo VT, čas dalšího přepnutí a ciferník 24h hodin).
     *
     * @param animateClock true pokud se má animovat přechod ciferníku (např. při změně odběrného místa)
     */
    private void refreshHdoAndPriceHighlight(boolean animateClock) {
        if (currentPoint == null || !isAdded()) return;

        long timeShift = 0L;
        DataSettingsSource settingsSource = new DataSettingsSource(requireContext());
        try {
            settingsSource.open();
            timeShift = settingsSource.loadTimeShift(currentPoint.getId());
        } catch (Exception ignored) {
        } finally {
            settingsSource.close();
        }

        ArrayList<HdoModel> allHdoModels = new ArrayList<>();
        ArrayList<HdoModel> activeRelayModels = new ArrayList<>();
        DataHdoSource hdoSource = new DataHdoSource(requireContext());
        try {
            hdoSource.open();
            allHdoModels = hdoSource.loadHdo(currentPoint.getTableHDO());
            ArrayList<String> allReles = hdoSource.getReles(currentPoint.getTableHDO());
            ShPHdo shPHdo = new ShPHdo(requireContext());
            String selectedMainRelay = shPHdo.get(ShPHdo.ARG_MAIN_HDO_RELAY, "");
            if ((selectedMainRelay.isEmpty() || !allReles.contains(selectedMainRelay)) && !allReles.isEmpty()) {
                selectedMainRelay = allReles.get(0);
            }
            if (!selectedMainRelay.isEmpty()) {
                activeRelayModels = hdoSource.loadHdo(currentPoint.getTableHDO(), null, selectedMainRelay);
            } else {
                activeRelayModels = allHdoModels;
            }
        } catch (Exception ignored) {
        } finally {
            hdoSource.close();
        }

        viewHdoClock.setHdoData(allHdoModels, timeShift, animateClock);

        if (activeRelayModels == null || activeRelayModels.isEmpty()) {
            applyTariffBoxHighlight(false);
            tvHdoNextSwitchLabel.setText(R.string.dashboard_hdo_not_set);
            tvHdoNextSwitchTime.setText("");
            return;
        }

        long nowMillis = System.currentTimeMillis();
        Calendar meterCal = Calendar.getInstance();
        meterCal.setTimeInMillis(nowMillis + timeShift);

        boolean isNtActive = HdoTime.checkHdo(activeRelayModels, meterCal);
        applyTariffBoxHighlight(isNtActive);

        int nextType = isNtActive ? HdoAlarmScheduler.TYPE_END : HdoAlarmScheduler.TYPE_START;
        long nextTriggerMillis = HdoAlarmScheduler.findNextTriggerForModels(activeRelayModels, nextType, nowMillis, timeShift);

        if (nextTriggerMillis > 0) {
            tvHdoNextSwitchLabel.setText(R.string.dashboard_hdo_next_switch_label);
            String formattedTime = SimpleDateFormatHelper.getOnlyTime().format(nextTriggerMillis + timeShift);
            tvHdoNextSwitchTime.setText(formattedTime);
        } else {
            tvHdoNextSwitchLabel.setText(R.string.dashboard_hdo_not_set);
            tvHdoNextSwitchTime.setText("");
        }
    }


    private void applyTariffBoxHighlight(boolean isNtActive) {
        int colorOnSurface = resolveAttrColor(com.google.android.material.R.attr.colorOnSurface);
        int colorWhite = ContextCompat.getColor(requireContext(), android.R.color.white);

        if (isNtActive) {
            boxPriceVt.setBackgroundResource(R.drawable.bg_dashboard_price_box_inactive);
            tvLabelVt.setTextColor(colorOnSurface);
            tvPriceVtValue.setTextColor(colorOnSurface);
            tvPriceVtUnit.setTextColor(colorOnSurface);
            ivCheckVt.setVisibility(View.GONE);

            boxPriceNt.setBackgroundResource(R.drawable.bg_dashboard_price_box_active);
            tvLabelNt.setTextColor(colorWhite);
            tvPriceNtValue.setTextColor(colorWhite);
            tvPriceNtUnit.setTextColor(colorWhite);
            ivCheckNt.setVisibility(View.VISIBLE);
        } else {
            boxPriceVt.setBackgroundResource(R.drawable.bg_dashboard_price_box_active);
            tvLabelVt.setTextColor(colorWhite);
            tvPriceVtValue.setTextColor(colorWhite);
            tvPriceVtUnit.setTextColor(colorWhite);
            ivCheckVt.setVisibility(View.VISIBLE);

            boxPriceNt.setBackgroundResource(R.drawable.bg_dashboard_price_box_inactive);
            tvLabelNt.setTextColor(colorOnSurface);
            tvPriceNtValue.setTextColor(colorOnSurface);
            tvPriceNtUnit.setTextColor(colorOnSurface);
            ivCheckNt.setVisibility(View.GONE);
        }
    }


    /**
     * Načte data pro "Spotřeba období", "Vyúčtování" a "Poslední faktury".
     */
    private void refreshPeriodBillingAndInvoices() {
        if (currentPoint == null || !isAdded()) return;

        DataInvoiceSource invoiceSource = new DataInvoiceSource(requireContext());
        DataPriceListSource priceListSource = new DataPriceListSource(requireContext());
        ArrayList<InvoiceListModel> invoiceLists;

        try {
            invoiceSource.open();
            priceListSource.open();

            invoiceLists = invoiceSource.loadInvoiceLists(currentPoint);

            // Určení aktivního období (nejprve období bez faktury idFak == -1L, případně nejnovější faktura)
            InvoiceListModel activePeriodModel = null;
            for (InvoiceListModel item : invoiceLists) {
                if (item.getIdFak() == -1L && item.getMinDate() > 0 && item.getMaxDate() > 0) {
                    activePeriodModel = item;
                    break;
                }
            }
            if (activePeriodModel == null && !invoiceLists.isEmpty()) {
                for (InvoiceListModel item : invoiceLists) {
                    if (item.getMinDate() > 0 && item.getMaxDate() > 0) {
                        activePeriodModel = item;
                        break;
                    }
                }
            }

            double targetBillingAdvancesKc = 0.0;
            double targetBillingCostsKc = 0.0;
            long targetMinDate = 0L;
            long targetMaxDate = 0L;

            if (activePeriodModel != null) {
                long idFak = activePeriodModel.getIdFak();
                String table = (idFak == -1L) ? currentPoint.getTableTED() : currentPoint.getTableFAK();
                PeriodSummary summary = computeInvoiceSummary(invoiceSource, priceListSource, currentPoint, idFak, table);

                targetMinDate = activePeriodModel.getMinDate();
                targetMaxDate = activePeriodModel.getMaxDate();

                double effectiveNtKwh = summary.isSingleTariff ? 0.0 : summary.ntKwh;
                animatePeriodConsumptionValues(summary.vtKwh, effectiveNtKwh, summary.isSingleTariff);
                viewPeriodBar.setConsumption(summary.vtKwh, effectiveNtKwh);
                viewPeriodDonut.setConsumption(summary.vtKwh, effectiveNtKwh);

                targetBillingAdvancesKc = summary.advancesKc;
                targetBillingCostsKc = summary.costsWithTaxKc;
            } else {
                PriceListModel fallbackPl = loadCurrentPriceList(currentPoint);
                boolean isSingle = fallbackPl != null && isSingleTariffRate(fallbackPl.getRate());
                animatePeriodConsumptionValues(0.0, 0.0, isSingle);
                viewPeriodBar.setConsumption(0, 0);
                viewPeriodDonut.setConsumption(0, 0);
            }

            // Poslední faktury (pouze vyúčtované faktury idFak != -1L)
            llInvoicesContainer.removeAllViews();
            int addedInvoices = 0;
            LayoutInflater inflater = LayoutInflater.from(requireContext());
            List<TextView> invoiceConsumptionViews = new ArrayList<>();
            List<Double> targetInvoiceConsumptionsKwh = new ArrayList<>();
            List<TextView> invoiceAmountViews = new ArrayList<>();
            List<Double> targetInvoiceAmountsKc = new ArrayList<>();

            for (int pos = 0; pos < invoiceLists.size(); pos++) {
                InvoiceListModel invItem = invoiceLists.get(pos);
                if (invItem.getIdFak() == -1L) {
                    continue;
                }
                if (addedInvoices >= MAX_RECENT_INVOICES) {
                    break;
                }

                PeriodSummary invSummary = computeInvoiceSummary(
                        invoiceSource,
                        priceListSource,
                        currentPoint,
                        invItem.getIdFak(),
                        currentPoint.getTableFAK()
                );

                View row = inflater.inflate(R.layout.item_dashboard_invoice_row, llInvoicesContainer, false);
                TextView tvNumber = row.findViewById(R.id.tvInvoiceNumber);
                TextView tvDates = row.findViewById(R.id.tvInvoiceDates);
                TextView tvConsumption = row.findViewById(R.id.tvInvoiceConsumption);
                TextView tvAmount = row.findViewById(R.id.tvInvoiceAmount);

                tvNumber.setText(getString(R.string.dashboard_invoice_number_format, invItem.getNumberInvoice()));
                if (invItem.getMinDate() > 0 && invItem.getMaxDate() > 0) {
                    String dFrom = ViewHelper.convertLongToDate(invItem.getMinDate());
                    String dTo = ViewHelper.convertLongToDate(invItem.getMaxDate());
                    tvDates.setText(String.format(Locale.getDefault(), "%s – %s", dFrom, dTo));
                } else {
                    tvDates.setText("");
                }

                double totalInvoiceKwh = invSummary.vtKwh + (invSummary.isSingleTariff ? 0.0 : invSummary.ntKwh);
                invoiceConsumptionViews.add(tvConsumption);
                targetInvoiceConsumptionsKwh.add(totalInvoiceKwh);
                invoiceAmountViews.add(tvAmount);
                targetInvoiceAmountsKc.add(invSummary.costsWithTaxKc);

                final long clickedIdFak = invItem.getIdFak();
                final int clickedPos = pos;
                row.setOnClickListener(v -> {
                    if (currentPoint == null) return;
                    applyDashboardSubscriptionPointIfChanged();
                    InvoiceAdapter.resetShowButtons();
                    PaymentAdapter.resetShowButtons();
                    InvoiceTabFragment tabFragment = InvoiceTabFragment.newInstance(
                            currentPoint.getTableFAK(),
                            currentPoint.getTableTED(),
                            currentPoint.getTablePLATBY(),
                            currentPoint.getTableO(),
                            clickedIdFak,
                            clickedPos,
                            MyViewPagerAdapter.TypeTabs.INVOICE
                    );
                    FragmentChange.replace(requireActivity(), tabFragment, MOVE, true);
                });

                llInvoicesContainer.addView(row);
                addedInvoices++;
            }

            layoutLastInvoicesSection.setVisibility(addedInvoices > 0 ? View.VISIBLE : View.GONE);
            animateBillingValues(
                    targetBillingAdvancesKc,
                    targetBillingCostsKc,
                    targetMinDate,
                    targetMaxDate,
                    invoiceConsumptionViews,
                    targetInvoiceConsumptionsKwh,
                    invoiceAmountViews,
                    targetInvoiceAmountsKc
            );
        } catch (Exception ignored) {
        } finally {
            priceListSource.close();
            invoiceSource.close();
        }
    }


    private void bindBalanceBadge(double balanceKc,
                                  float colorRatio,
                                  @NonNull String startLabel,
                                  @NonNull String targetLabel,
                                  float animFraction) {
        if (!isAdded()) return;

        long rounded = Math.round(balanceKc);
        float clampedColorRatio = Math.max(0f, Math.min(1f, colorRatio));

        int colorOverBg = ContextCompat.getColor(requireContext(), R.color.dashboard_overpayment_bg);
        int colorUnderBg = ContextCompat.getColor(requireContext(), R.color.dashboard_underpayment_bg);
        int colorOverText = ContextCompat.getColor(requireContext(), R.color.dashboard_overpayment_text);
        int colorUnderText = ContextCompat.getColor(requireContext(), R.color.dashboard_underpayment_text);

        int bgColor = (int) argbEvaluator.evaluate(clampedColorRatio, colorOverBg, colorUnderBg);
        int textColor = (int) argbEvaluator.evaluate(clampedColorRatio, colorOverText, colorUnderText);

        if (balanceBgDrawable == null) {
            balanceBgDrawable = new GradientDrawable();
            balanceBgDrawable.setShape(GradientDrawable.RECTANGLE);
            balanceBgDrawable.setCornerRadius(dpToPx(requireContext(), 12f));
        }
        balanceBgDrawable.setColor(bgColor);
        if (boxBillingBalance.getBackground() != balanceBgDrawable) {
            boxBillingBalance.setBackground(balanceBgDrawable);
        }

        tvBillingBalanceLabel.setTextColor(textColor);
        tvBillingBalanceValue.setTextColor(textColor);
        tvBillingBalanceValue.setText(String.format(Locale.getDefault(), "%s Kč", integerFormat.format(Math.abs(rounded))));

        if (startLabel.equals(targetLabel) || animFraction >= 0.999f) {
            tvBillingBalanceLabel.setText(targetLabel);
            tvBillingBalanceLabel.setAlpha(1f);
            tvBillingBalanceLabel.setTranslationY(0f);
            tvBillingBalanceLabel.setScaleX(1f);
            tvBillingBalanceLabel.setScaleY(1f);
            displayedBalanceLabel = targetLabel;
        } else {
            float maxShiftPx = dpToPx(requireContext(), 5f);
            if (animFraction < 0.5f) {
                float outProgress = animFraction * 2f;
                tvBillingBalanceLabel.setText(startLabel);
                tvBillingBalanceLabel.setAlpha(1f - outProgress);
                tvBillingBalanceLabel.setTranslationY(-maxShiftPx * outProgress);
                float scale = 1f - 0.12f * outProgress;
                tvBillingBalanceLabel.setScaleX(scale);
                tvBillingBalanceLabel.setScaleY(scale);
                displayedBalanceLabel = startLabel;
            } else {
                float inProgress = (animFraction - 0.5f) * 2f;
                tvBillingBalanceLabel.setText(targetLabel);
                tvBillingBalanceLabel.setAlpha(inProgress);
                tvBillingBalanceLabel.setTranslationY(maxShiftPx * (1f - inProgress));
                float scale = 0.88f + 0.12f * inProgress;
                tvBillingBalanceLabel.setScaleX(scale);
                tvBillingBalanceLabel.setScaleY(scale);
                displayedBalanceLabel = targetLabel;
            }
        }
    }


    @NonNull
    private String getBalanceLabelForValue(long roundedBalanceKc) {
        if (roundedBalanceKc > 0) {
            return getString(R.string.dashboard_overpayment_label);
        } else if (roundedBalanceKc < 0) {
            return getString(R.string.dashboard_underpayment_label);
        } else {
            return getString(R.string.dashboard_balanced_label);
        }
    }


    @NonNull
    private String formatShortPeriodRange(@NonNull String startDate, @NonNull String endDate) {
        if (startDate.length() >= 10 && endDate.length() >= 10) {
            String startYear = startDate.substring(6);
            String endYear = endDate.substring(6);
            if (startYear.equals(endYear)) {
                return startDate.substring(0, 6) + " – " + endDate;
            }
        }
        return startDate + " – " + endDate;
    }


    @NonNull
    private PeriodSummary computeInvoiceSummary(@NonNull DataInvoiceSource invoiceSource,
                                                @NonNull DataPriceListSource priceListSource,
                                                @NonNull SubscriptionPointModel point,
                                                long idFak,
                                                @NonNull String table) {
        ArrayList<InvoiceModel> records = invoiceSource.loadInvoices(idFak, table);
        double advances = invoiceSource.sumAdvancePaymentsOnly(idFak, point.getTablePLATBY());
        if (advances <= 0) {
            advances = invoiceSource.sumPayment(idFak, point.getTablePLATBY());
        }
        double discountWithTax = invoiceSource.sumDiscountWithTax(idFak, point.getTablePLATBY());

        if (records == null || records.isEmpty()) {
            PriceListModel fallbackPl = loadCurrentPriceList(point);
            boolean isSingle = fallbackPl != null && isSingleTariffRate(fallbackPl.getRate());
            return new PeriodSummary(0, 0, advances, 0, isSingle);
        }

        PozeModel poze = Calculation.getPoze(records, point.getCountPhaze(), point.getPhaze(), requireContext());
        double totalDph = 0.0;
        double totalVtMwh = 0.0;
        double totalNtMwh = 0.0;
        boolean hasAnyRate = false;
        boolean allSingleTariff = true;

        for (int i = 0; i < records.size(); i++) {
            InvoiceModel invoice = records.get(i);
            PriceListModel priceList = priceListSource.readPrice(invoice.getIdPriceList());
            if (priceList == null) {
                priceList = new PriceListModel();
            }
            if (priceList.getRate() != null && !priceList.getRate().trim().isEmpty()) {
                hasAnyRate = true;
                if (!isSingleTariffRate(priceList.getRate())) {
                    allSingleTariff = false;
                }
            }
            PriceListModel regulPriceList = new PriceListRegulBuilder(priceList, invoice).getRegulPriceList();
            String dateOf = ViewHelper.convertLongToDate(invoice.getDateFrom());
            String dateTo = ViewHelper.convertLongToDate(invoice.getDateTo());
            double differentDate = Calculation.differentMonth(dateOf, dateTo, DifferenceDate.TypeDate.INVOICE);
            double[] price = Calculation.calculatePriceWithoutPozeMwH(regulPriceList, point);
            double vtMwh = Math.max(0.0, (invoice.getVtEnd() - invoice.getVtStart()) / 1000.0);
            double ntMwh = Math.max(0.0, (invoice.getNtEnd() - invoice.getNtStart()) / 1000.0);
            double ntVtMwh = vtMwh + ntMwh;

            totalVtMwh += vtMwh;
            totalNtMwh += ntMwh;

            price[0] *= vtMwh;
            price[1] *= ntMwh;
            price[2] *= differentDate;

            if (poze.getTypePoze() == PozeModel.TypePoze.POZE2) {
                if (priceList.getValidityYear() < NEW_POZE_YEAR) {
                    price[3] = ntVtMwh * regulPriceList.getOze();
                } else {
                    price[3] = ntVtMwh * regulPriceList.getPoze2();
                    if (priceList.getValidityYear() == 2026) {
                        price[3] = ntVtMwh * regulPriceList.getPoze1();
                    }
                }
            } else {
                price[3] = point.getCountPhaze() * point.getPhaze() * differentDate * regulPriceList.getPoze1();
            }

            double otherServices = invoice.getOtherServices() * differentDate;
            for (double p : price) {
                totalDph += p + (p * priceList.getVat() / 100.0);
            }
            totalDph += otherServices + (otherServices * priceList.getVat() / 100.0);

            if (i == records.size() - 1) {
                double discountWithoutTax = invoiceSource.sumDiscountWithoutTax(idFak, point.getTablePLATBY());
                double discountWithTaxInTotal = invoiceSource.sumDiscountWithTaxInTotal(idFak, point.getTablePLATBY());
                totalDph -= discountWithoutTax + (discountWithoutTax * priceList.getVat() / 100.0) + discountWithTaxInTotal;
            }
        }

        boolean isSingleTariff = hasAnyRate && allSingleTariff && (totalNtMwh <= 0.0);
        if (!hasAnyRate && totalNtMwh <= 0.0) {
            PriceListModel fallbackPl = loadCurrentPriceList(point);
            isSingleTariff = fallbackPl != null && isSingleTariffRate(fallbackPl.getRate());
        }

        double totalWithTax = Math.max(0.0, totalDph - discountWithTax);
        return new PeriodSummary(totalVtMwh * 1000.0, totalNtMwh * 1000.0, advances, totalWithTax, isSingleTariff);
    }


    /**
     * Plynule odanimuje přičítání/odečítání číselných hodnot VT a NT v kartě "Spotřeba období"
     * a postupné zobrazení či skrytí řádku NT u jednosazbového tarifu (D 01d, D 02d).
     */
    private void animatePeriodConsumptionValues(double targetVtKwh, double targetNtKwh, boolean isSingleTariff) {
        final double targetVt = Math.max(0.0, targetVtKwh);
        final double targetNt = isSingleTariff ? 0.0 : Math.max(0.0, targetNtKwh);
        final float targetNtVis = isSingleTariff ? 0f : 1f;

        if (periodValuesAnimator != null) {
            periodValuesAnimator.cancel();
            periodValuesAnimator = null;
        }

        if (!hasInitializedPeriodValues) {
            displayedPeriodNtVisibility = targetNtVis;
            applyNtRowVisibilityFraction(targetNtVis);
            hasInitializedPeriodValues = true;
        }

        final double startVt = displayedPeriodVtKwh;
        final double startNt = displayedPeriodNtKwh;
        final float startNtVis = displayedPeriodNtVisibility;

        periodValuesAnimator = ValueAnimator.ofFloat(0f, 1f);
        periodValuesAnimator.setDuration(PERIOD_ANIMATION_DURATION_MS);
        periodValuesAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        periodValuesAnimator.addUpdateListener(anim -> {
            if (!isAdded()) return;
            float fraction = (float) anim.getAnimatedValue();
            displayedPeriodVtKwh = startVt + (targetVt - startVt) * fraction;
            displayedPeriodNtKwh = startNt + (targetNt - startNt) * fraction;
            displayedPeriodNtVisibility = startNtVis + (targetNtVis - startNtVis) * fraction;

            tvPeriodVtValue.setText(String.format(Locale.getDefault(), "%s kWh", integerFormat.format(Math.round(displayedPeriodVtKwh))));
            tvPeriodNtValue.setText(String.format(Locale.getDefault(), "%s kWh", integerFormat.format(Math.round(displayedPeriodNtKwh))));
            applyNtRowVisibilityFraction(displayedPeriodNtVisibility);
        });
        periodValuesAnimator.start();
    }


    /**
     * Plynule odanimuje přičítání/odečítání číselných hodnot a datumového rozsahu v kartě "Vyúčtování"
     * (Období, Zálohy, Náklady, Přeplatek/Nedoplatek včetně plynulého přechodu barev a textu štítku
     * i spotřeby a celkové ceny s DPH v seznamu Posledních faktur).
     */
    private void animateBillingValues(double targetAdvancesKc,
                                      double targetCostsKc,
                                      long targetMinDateMillis,
                                      long targetMaxDateMillis,
                                      @NonNull List<TextView> invoiceConsumptionViews,
                                      @NonNull List<Double> targetInvoiceConsumptionsKwh,
                                      @NonNull List<TextView> invoiceAmountViews,
                                      @NonNull List<Double> targetInvoiceAmountsKc) {
        final double targetAdvances = Math.max(0.0, targetAdvancesKc);
        final double targetCosts = Math.max(0.0, targetCostsKc);
        final long targetRoundedBalance = Math.round(targetAdvances - targetCosts);
        final float targetColorRatio = (targetRoundedBalance < 0) ? 1f : 0f;
        final String targetLabel = getBalanceLabelForValue(targetRoundedBalance);

        if (billingValuesAnimator != null) {
            billingValuesAnimator.cancel();
            billingValuesAnimator = null;
        }

        if (!hasInitializedBillingValues) {
            displayedBalanceColorRatio = targetColorRatio;
            displayedBalanceLabel = targetLabel;
            displayedBillingMinDate = targetMinDateMillis;
            displayedBillingMaxDate = targetMaxDateMillis;
            hasInitializedBillingValues = true;
        }

        final double startAdvances = displayedBillingAdvancesKc;
        final double startCosts = displayedBillingCostsKc;
        final long startMinDate = displayedBillingMinDate;
        final long startMaxDate = displayedBillingMaxDate;
        final float startColorRatio = displayedBalanceColorRatio;
        final String startLabel = (displayedBalanceLabel == null || displayedBalanceLabel.isEmpty())
                ? targetLabel
                : displayedBalanceLabel;
        final List<Double> startInvoiceAmounts = new ArrayList<>(displayedRecentInvoiceAmountsKc);
        final List<Double> startInvoiceConsumptions = new ArrayList<>(displayedRecentInvoiceConsumptionsKwh);

        displayedRecentInvoiceAmountsKc.clear();
        for (int i = 0; i < targetInvoiceAmountsKc.size(); i++) {
            displayedRecentInvoiceAmountsKc.add(i < startInvoiceAmounts.size() ? startInvoiceAmounts.get(i) : 0.0);
        }

        displayedRecentInvoiceConsumptionsKwh.clear();
        for (int i = 0; i < targetInvoiceConsumptionsKwh.size(); i++) {
            displayedRecentInvoiceConsumptionsKwh.add(i < startInvoiceConsumptions.size() ? startInvoiceConsumptions.get(i) : 0.0);
        }

        billingValuesAnimator = ValueAnimator.ofFloat(0f, 1f);
        billingValuesAnimator.setDuration(PERIOD_ANIMATION_DURATION_MS);
        billingValuesAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        billingValuesAnimator.addUpdateListener(anim -> {
            if (!isAdded()) return;
            float fraction = (float) anim.getAnimatedValue();
            displayedBillingAdvancesKc = startAdvances + (targetAdvances - startAdvances) * fraction;
            displayedBillingCostsKc = startCosts + (targetCosts - startCosts) * fraction;
            displayedBalanceColorRatio = startColorRatio + (targetColorRatio - startColorRatio) * fraction;

            bindAnimatedPeriodDates(startMinDate, startMaxDate, targetMinDateMillis, targetMaxDateMillis, fraction);

            tvBillingAdvances.setText(String.format(Locale.getDefault(), "%s Kč", integerFormat.format(Math.round(displayedBillingAdvancesKc))));
            tvBillingCosts.setText(String.format(Locale.getDefault(), "%s Kč", integerFormat.format(Math.round(displayedBillingCostsKc))));
            bindBalanceBadge(
                    displayedBillingAdvancesKc - displayedBillingCostsKc,
                    displayedBalanceColorRatio,
                    startLabel,
                    targetLabel,
                    fraction
            );

            int count = Math.min(invoiceAmountViews.size(), targetInvoiceAmountsKc.size());
            for (int i = 0; i < count; i++) {
                double fromAmount = (i < startInvoiceAmounts.size()) ? startInvoiceAmounts.get(i) : 0.0;
                double toAmount = targetInvoiceAmountsKc.get(i);
                double currentAmount = fromAmount + (toAmount - fromAmount) * fraction;
                if (i < displayedRecentInvoiceAmountsKc.size()) {
                    displayedRecentInvoiceAmountsKc.set(i, currentAmount);
                }
                invoiceAmountViews.get(i).setText(
                        getString(R.string.dashboard_invoice_amount_with_vat_format, integerFormat.format(Math.round(currentAmount)))
                );
            }

            int consCount = Math.min(invoiceConsumptionViews.size(), targetInvoiceConsumptionsKwh.size());
            for (int i = 0; i < consCount; i++) {
                double fromCons = (i < startInvoiceConsumptions.size()) ? startInvoiceConsumptions.get(i) : 0.0;
                double toCons = targetInvoiceConsumptionsKwh.get(i);
                double currentCons = fromCons + (toCons - fromCons) * fraction;
                if (i < displayedRecentInvoiceConsumptionsKwh.size()) {
                    displayedRecentInvoiceConsumptionsKwh.set(i, currentCons);
                }
                invoiceConsumptionViews.get(i).setText(
                        getString(R.string.dashboard_invoice_consumption_format, integerFormat.format(Math.round(currentCons)))
                );
            }
        });
        billingValuesAnimator.start();
    }


    private void bindAnimatedPeriodDates(long startMinDate,
                                         long startMaxDate,
                                         long targetMinDate,
                                         long targetMaxDate,
                                         float fraction) {
        boolean hasStart = startMinDate > 0L && startMaxDate > 0L;
        boolean hasTarget = targetMinDate > 0L && targetMaxDate > 0L;

        if (hasStart && hasTarget) {
            displayedBillingMinDate = Math.round(startMinDate + (targetMinDate - startMinDate) * (double) fraction);
            displayedBillingMaxDate = Math.round(startMaxDate + (targetMaxDate - startMaxDate) * (double) fraction);
            String startDate = ViewHelper.convertLongToDate(displayedBillingMinDate);
            String endDate = ViewHelper.convertLongToDate(displayedBillingMaxDate);

            tvPeriodDateRange.setAlpha(1f);
            tvPeriodDateRange.setText(String.format(Locale.getDefault(), "%s – %s", startDate, endDate));

            tvBillingPeriod.setAlpha(1f);
            tvBillingPeriod.setText(getString(R.string.dashboard_billing_period_format, formatShortPeriodRange(startDate, endDate)));
        } else if (hasTarget) {
            displayedBillingMinDate = targetMinDate;
            displayedBillingMaxDate = targetMaxDate;
            String startDate = ViewHelper.convertLongToDate(targetMinDate);
            String endDate = ViewHelper.convertLongToDate(targetMaxDate);

            tvPeriodDateRange.setAlpha(fraction);
            tvPeriodDateRange.setText(String.format(Locale.getDefault(), "%s – %s", startDate, endDate));

            tvBillingPeriod.setAlpha(fraction);
            tvBillingPeriod.setText(getString(R.string.dashboard_billing_period_format, formatShortPeriodRange(startDate, endDate)));
        } else if (hasStart) {
            if (fraction < 0.995f) {
                String startDate = ViewHelper.convertLongToDate(startMinDate);
                String endDate = ViewHelper.convertLongToDate(startMaxDate);
                tvPeriodDateRange.setAlpha(1f - fraction);
                tvPeriodDateRange.setText(String.format(Locale.getDefault(), "%s – %s", startDate, endDate));
                tvBillingPeriod.setAlpha(1f - fraction);
                tvBillingPeriod.setText(getString(R.string.dashboard_billing_period_format, formatShortPeriodRange(startDate, endDate)));
            } else {
                displayedBillingMinDate = 0L;
                displayedBillingMaxDate = 0L;
                tvPeriodDateRange.setAlpha(1f);
                tvPeriodDateRange.setText("");
                tvBillingPeriod.setAlpha(1f);
                tvBillingPeriod.setText("");
            }
        } else {
            displayedBillingMinDate = 0L;
            displayedBillingMaxDate = 0L;
            tvPeriodDateRange.setAlpha(1f);
            tvPeriodDateRange.setText("");
            tvBillingPeriod.setAlpha(1f);
            tvBillingPeriod.setText("");
        }
    }


    private void applyNtRowVisibilityFraction(float fraction) {
        if (layoutPeriodNtRow == null || !isAdded()) {
            return;
        }
        float clamped = Math.max(0f, Math.min(1f, fraction));
        int fullTopMargin = Math.max(0, dpToPx(requireContext(), 6f));

        if (ntRowMeasuredHeight <= 0) {
            if (layoutPeriodNtRow.getHeight() > 0) {
                ntRowMeasuredHeight = layoutPeriodNtRow.getHeight();
            } else {
                int widthSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
                int heightSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
                layoutPeriodNtRow.measure(widthSpec, heightSpec);
                ntRowMeasuredHeight = Math.max(dpToPx(requireContext(), 20f), layoutPeriodNtRow.getMeasuredHeight());
            }
        }

        ViewGroup.LayoutParams rawLp = layoutPeriodNtRow.getLayoutParams();
        if (!(rawLp instanceof ViewGroup.MarginLayoutParams lp)) {
            layoutPeriodNtRow.setAlpha(clamped);
            layoutPeriodNtRow.setVisibility(clamped <= 0.005f ? View.GONE : View.VISIBLE);
            return;
        }

        if (clamped <= 0.005f) {
            layoutPeriodNtRow.setAlpha(0f);
            layoutPeriodNtRow.setVisibility(View.GONE);
        } else if (clamped >= 0.995f) {
            lp.height = ViewGroup.LayoutParams.WRAP_CONTENT;
            lp.topMargin = fullTopMargin;
            layoutPeriodNtRow.setLayoutParams(lp);
            layoutPeriodNtRow.setAlpha(1f);
            layoutPeriodNtRow.setVisibility(View.VISIBLE);
        } else {
            lp.height = Math.max(1, Math.round(ntRowMeasuredHeight * clamped));
            lp.topMargin = Math.round(fullTopMargin * clamped);
            layoutPeriodNtRow.setLayoutParams(lp);
            layoutPeriodNtRow.setAlpha(clamped);
            layoutPeriodNtRow.setVisibility(View.VISIBLE);
        }
    }


    private boolean isSingleTariffRate(@Nullable String rate) {
        if (rate == null || rate.trim().isEmpty()) {
            return false;
        }
        String trimmed = rate.trim();
        if (InvoiceAbstract.D01.equalsIgnoreCase(trimmed) || InvoiceAbstract.D02.equalsIgnoreCase(trimmed)) {
            return true;
        }
        String compact = trimmed.replace(" ", "");
        return "D01d".equalsIgnoreCase(compact) || "D02d".equalsIgnoreCase(compact);
    }


    /**
     * Načte měsíční a roční spotřebu pro kartu "Přehled spotřeby".
     */
    private void refreshConsumptionOverviewData() {
        monthlyChartEntries.clear();
        yearlyChartEntries.clear();

        if (currentPoint == null || !isAdded()) return;

        DataGraphMonth dataGraphMonth = new DataGraphMonth(requireContext());
        try {
            dataGraphMonth.open();
            ConsuptionContainer container = dataGraphMonth.loadConsuptions(currentPoint);
            if (container != null) {
                ArrayList<ConsuptionModel> monthlyList = container.monthlyConsuption();
                if (monthlyList != null && !monthlyList.isEmpty()) {
                    int startIdx = Math.max(0, monthlyList.size() - MAX_BAR_ENTRIES);
                    for (int i = startIdx; i < monthlyList.size(); i++) {
                        ConsuptionModel m = monthlyList.get(i);
                        String label = extractShortMonthLabel(m.getDateMonthAsStringShort());
                        monthlyChartEntries.add(new DashboardConsumptionBarChartView.BarEntry(
                                label,
                                m.getConsuptionVT() != null ? m.getConsuptionVT() : 0.0,
                                m.getConsuptionNT() != null ? m.getConsuptionNT() : 0.0
                        ));
                    }
                }

                ArrayList<ConsuptionModel> yearlyList = container.yearConsuption();
                if (yearlyList != null && !yearlyList.isEmpty()) {
                    int startIdx = Math.max(0, yearlyList.size() - MAX_BAR_ENTRIES);
                    for (int i = startIdx; i < yearlyList.size(); i++) {
                        ConsuptionModel y = yearlyList.get(i);
                        String label = y.getYearAsString() != null ? y.getYearAsString() : "";
                        yearlyChartEntries.add(new DashboardConsumptionBarChartView.BarEntry(
                                label,
                                y.getConsuptionVT() != null ? y.getConsuptionVT() : 0.0,
                                y.getConsuptionNT() != null ? y.getConsuptionNT() : 0.0
                        ));
                    }
                }
            }
        } catch (Exception ignored) {
        } finally {
            dataGraphMonth.close();
        }

        refreshConsumptionChartDisplay();
    }


    @NonNull
    private String extractShortMonthLabel(@Nullable String shortDateMonth) {
        if (shortDateMonth == null || shortDateMonth.trim().isEmpty()) {
            return "";
        }
        String trimmed = shortDateMonth.trim();
        int spaceIdx = trimmed.indexOf(' ');
        if (spaceIdx > 0) {
            return trimmed.substring(0, spaceIdx);
        }
        return trimmed;
    }


    private void refreshConsumptionChartDisplay() {
        boolean isYearly = swConsumptionYearly.isChecked();
        viewConsumptionBarChart.setEntries(isYearly ? yearlyChartEntries : monthlyChartEntries);
    }


    private int resolveAttrColor(int attrRes) {
        TypedValue tv = new TypedValue();
        if (requireContext().getTheme().resolveAttribute(attrRes, tv, true)) {
            return tv.data;
        }
        return 0xFF1C1B1F;
    }


    @Override
    public void onResume() {
        super.onResume();
        updatePadding();
        loadSubscriptionPointsAndRefresh();
        hdoHandler.removeCallbacks(hdoRefreshRunnable);
        hdoHandler.postDelayed(hdoRefreshRunnable, 30_000L);
    }


    @Override
    public void onPause() {
        super.onPause();
        hdoHandler.removeCallbacks(hdoRefreshRunnable);
        if (periodValuesAnimator != null) {
            periodValuesAnimator.cancel();
            periodValuesAnimator = null;
        }
        if (billingValuesAnimator != null) {
            billingValuesAnimator.cancel();
            billingValuesAnimator = null;
        }
    }


    public void updatePadding() {
        if (!isAdded()) return;
        if (getActivity() instanceof MainActivity mainAct) {
            float density = getResources().getDisplayMetrics().density;
            int topPaddingPx = mainAct.getTopBarTotalHeight();
            int bottomPaddingPx = mainAct.isNavRail()
                    ? mainAct.getEffectiveNavBarInsetBottom() + (int) (16 * density)
                    : mainAct.getEffectiveNavBarInsetBottom() + (int) (112 * density);
            int leftPaddingPx = mainAct.getLeftNavTotalWidth();
            int rightPaddingPx = mainAct.getEffectiveNavBarInsetRight();

            if (nsvNewDashboard != null) {
                nsvNewDashboard.setAlpha(1.0f);
                nsvNewDashboard.setClipToPadding(false);
                nsvNewDashboard.setPadding(leftPaddingPx, topPaddingPx, rightPaddingPx, bottomPaddingPx);
            }
        }
    }


    @Override
    public void onPaddingChanged() {
        updatePadding();
    }


    private static class PeriodSummary {

        final double vtKwh;
        final double ntKwh;
        final double advancesKc;
        final double costsWithTaxKc;
        final boolean isSingleTariff;


        PeriodSummary(double vtKwh, double ntKwh, double advancesKc, double costsWithTaxKc, boolean isSingleTariff) {
            this.vtKwh = vtKwh;
            this.ntKwh = ntKwh;
            this.advancesKc = advancesKc;
            this.costsWithTaxKc = costsWithTaxKc;
            this.isSingleTariff = isSingleTariff;
        }

    }

}
