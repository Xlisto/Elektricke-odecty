package cz.xlisto.elektrodroid.modules.dashboard;


import static cz.xlisto.elektrodroid.models.PriceListModel.NEW_POZE_YEAR;
import static cz.xlisto.elektrodroid.utils.DensityUtils.dpToPx;
import static cz.xlisto.elektrodroid.utils.FragmentChange.Transaction.MOVE;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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
import androidx.core.graphics.ColorUtils;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.color.MaterialColors;
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
import cz.xlisto.elektrodroid.models.ConsuptionModel;
import cz.xlisto.elektrodroid.models.HdoModel;
import cz.xlisto.elektrodroid.models.InvoiceListModel;
import cz.xlisto.elektrodroid.models.InvoiceModel;
import cz.xlisto.elektrodroid.models.MonthlyConsuptionModel;
import cz.xlisto.elektrodroid.models.MonthlyReadingModel;
import cz.xlisto.elektrodroid.models.PozeModel;
import cz.xlisto.elektrodroid.models.PriceListModel;
import cz.xlisto.elektrodroid.models.PriceListRegulBuilder;
import cz.xlisto.elektrodroid.models.SubscriptionPointModel;
import cz.xlisto.elektrodroid.modules.graphmonth.ConsuptionContainer;
import cz.xlisto.elektrodroid.modules.graphmonth.GraphMonthFragment;
import cz.xlisto.elektrodroid.modules.hdo.HdoFragment;
import cz.xlisto.elektrodroid.modules.hdo.HdoTime;
import cz.xlisto.elektrodroid.modules.invoice.InvoiceAbstract;
import cz.xlisto.elektrodroid.modules.invoice.InvoiceAdapter;
import cz.xlisto.elektrodroid.modules.invoice.InvoiceListFragment;
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
 *     <li>Přepínač odběrného místa (lokální přepínání v rámci přehledu; globální výběr se přepne až při přechodu do jiného fragmentu)</li>
 *     <li>Aktuální cenu elektřiny (VT/NT) s přepínačem Celková/Obchodní cena, volbou DPH a informací o dalším přepnutí HDO</li>
 *     <li>Rychlé dlaždice: Měsíční odečty, 24hodinové analogové hodiny HDO, Nový odečet</li>
 *     <li>Spotřebu aktuálního období (VT/NT pruh + donut graf) s proklikem do aktuálního nevyfakturovaného období</li>
 *     <li>Přehled spotřeby (sloupcový graf s přepínačem Měsíční / Roční, procentní změnou a odhadem neúplného období) s proklikem do grafu</li>
 *     <li>Vyúčtování aktuálního období (Zálohy, Náklady, Přeplatek/Nedoplatek) a seznam posledních faktur</li>
 * </ul>
 * Rozložení sekcí je řízeno přes {@link DashboardLayoutManager} pro budoucí uživatelské přizpůsobení.
 */
public class NewDashBoardFragment extends Fragment implements OnPaddingChangeListener {

    /**
     * Maximální počet sloupců zobrazených ve sloupcovém grafu přehledu spotřeby.
     */
    private static final int MAX_BAR_ENTRIES = 5;
    /** Maximální počet posledních vyúčtovaných faktur zobrazených v sekci vyúčtování. */
    private static final int MAX_RECENT_INVOICES = 3;

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
    private MaterialButtonToggleGroup togglePriceType;
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
    private TextView tvHdoCountdown;

    // 2. Sekce: Rychlé dlaždice
    private NumbersMeter numbersMeterDashboardVT;
    private NumbersMeter numbersMeterDashboardNT;
    private DashboardHdoClockView viewHdoClock;
    private TextView tvLastReadingDate;

    // 3. Karta: Spotřeba období
    private TextView tvPeriodDateRange;
    private View viewPeriodVtColor;
    private TextView tvPeriodVtValue;
    private TextView tvPeriodVtPercent;
    private View layoutPeriodNtRow;
    private View viewPeriodNtColor;
    private TextView tvPeriodNtValue;
    private TextView tvPeriodNtPercent;
    private DashboardPeriodBarView viewPeriodBar;
    private DashboardDonutChartView viewPeriodDonut;

    // 4. Karta: Přehled spotřeby
    private TextView tvConsumptionChartSubtitle;
    private MaterialSwitch swConsumptionYearly;
    private DashboardConsumptionBarChartView viewConsumptionBarChart;
    private TextView tvConsumptionChangeNote;

    // 5. Karta: Vyúčtování a poslední faktury
    private TextView tvBillingPeriod;
    private TextView tvBillingAdvances;
    private TextView tvBillingCosts;
    private LinearLayout boxBillingBalance;
    private TextView tvBillingBalanceLabel;
    private TextView tvBillingBalanceValue;
    private DashboardBillingBalanceBarView viewBillingBalanceBar;
    private TextView tvBillingBarUnderpayment;
    private TextView tvBillingBarRatio;
    private TextView tvBillingBarOverpayment;
    private View layoutLastInvoicesSection;
    private LinearLayout llInvoicesContainer;
    private TextView tvNoRecentInvoices;

    /** Klíč pro uložení lokálně vybraného odběrného místa do {@link Bundle}. */
    private static final String STATE_SELECTED_LOCAL_POINT_ID = "stateSelectedLocalPointId";
    /** Doba trvání plynulých přechodových animací hodnot a stavů v přehledu (v ms). */
    private static final long PERIOD_ANIMATION_DURATION_MS = 550L;
    /**
     * Doba trvání plynulé animace čísel cen VT/NT při přepnutí typu ceny nebo DPH (v ms).
     */
    private static final long PRICE_ANIMATION_DURATION_MS = 350L;

    private ShPDashBoard shPDashBoard;
    private DashboardLayoutManager layoutManager;
    private ArrayList<SubscriptionPointModel> allSubscriptionPoints = new ArrayList<>();
    private int currentPointIndex = 0;
    private SubscriptionPointModel currentPoint;
    @Nullable
    private PriceListModel cachedCurrentPriceList;
    private long selectedLocalPointId = -1L;

    private double displayedPriceVtKwh = 0.0;
    private double displayedPriceNtKwh = 0.0;
    private boolean hasInitializedPriceValues = false;
    private ValueAnimator priceValuesAnimator;

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
    private float displayedTariffNtRatio = 0f;
    private boolean hasInitializedTariffHighlight = false;
    private GradientDrawable boxPriceVtBgDrawable;
    private GradientDrawable boxPriceNtBgDrawable;
    private ValueAnimator tariffHighlightAnimator;
    private String displayedHdoNextSwitchTimeText = "";
    private String displayedHdoNextSwitchLabel = "";
    private boolean hasInitializedHdoNextSwitch = false;
    private ValueAnimator hdoNextSwitchAnimator;
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
                refreshHdoAndPriceHighlightPeriodic();
            }
            hdoHandler.postDelayed(this, 30_000L);
        }
    };


    /**
     * Vytvoří novou instanci fragmentu {@link NewDashBoardFragment}.
     *
     * @return nová instance fragmentu
     */
    public static NewDashBoardFragment newInstance() {
        return new NewDashBoardFragment();
    }


    /**
     * Obnoví lokálně zvolené odběrné místo z uloženého stavu a zaregistruje posluchač
     * pro aktualizaci odběrných míst z dialogu {@link SubscriptionPointDialogFragment}.
     */
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


    /**
     * Uloží ID aktuálně lokálně vybraného odběrného místa pro případ znovuvytvoření fragmentu.
     */
    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putLong(STATE_SELECTED_LOCAL_POINT_ID, selectedLocalPointId);
    }


    /**
     * Nafoukne rozložení přehledové obrazovky {@code fragment_new_dashboard}.
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_new_dashboard, container, false);
    }


    /**
     * Inicializuje formátování čísel, správce předvoleb a rozložení, naváže UI komponenty,
     * zaregistruje posluchače a načte data odběrných míst.
     */
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
    }


    /**
     * Vyhledá všechny pohledy v rozložení fragmentu a naváže obsluhu kliknutí
     * na rychlé dlaždice, kartu spotřeby období, kartu přehledu spotřeby a souhrn vyúčtování.
     *
     * @param view kořenový pohled fragmentu
     */
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

        togglePriceType = view.findViewById(R.id.togglePriceType);
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
        tvHdoCountdown = view.findViewById(R.id.tvHdoCountdown);

        MaterialCardView cardQuickMonthlyReadings = view.findViewById(R.id.cardQuickMonthlyReadings);
        MaterialCardView cardQuickHdoClock = view.findViewById(R.id.cardQuickHdoClock);
        MaterialCardView cardQuickNewReading = view.findViewById(R.id.cardQuickNewReading);
        numbersMeterDashboardVT = view.findViewById(R.id.numbersMeterDashboardVT);
        numbersMeterDashboardNT = view.findViewById(R.id.numbersMeterDashboardNT);
        viewHdoClock = view.findViewById(R.id.viewHdoClock);
        tvLastReadingDate = view.findViewById(R.id.tvLastReadingDate);

        tvPeriodDateRange = view.findViewById(R.id.tvPeriodDateRange);
        viewPeriodVtColor = view.findViewById(R.id.viewPeriodVtColor);
        tvPeriodVtValue = view.findViewById(R.id.tvPeriodVtValue);
        tvPeriodVtPercent = view.findViewById(R.id.tvPeriodVtPercent);
        layoutPeriodNtRow = view.findViewById(R.id.layoutPeriodNtRow);
        viewPeriodNtColor = view.findViewById(R.id.viewPeriodNtColor);
        tvPeriodNtValue = view.findViewById(R.id.tvPeriodNtValue);
        tvPeriodNtPercent = view.findViewById(R.id.tvPeriodNtPercent);
        viewPeriodBar = view.findViewById(R.id.viewPeriodBar);
        viewPeriodDonut = view.findViewById(R.id.viewPeriodDonut);

        tvConsumptionChartSubtitle = view.findViewById(R.id.tvConsumptionChartSubtitle);
        swConsumptionYearly = view.findViewById(R.id.swConsumptionYearly);
        viewConsumptionBarChart = view.findViewById(R.id.viewConsumptionBarChart);
        tvConsumptionChangeNote = view.findViewById(R.id.tvConsumptionChangeNote);

        View layoutBillingSummary = view.findViewById(R.id.layoutBillingSummary);
        tvBillingPeriod = view.findViewById(R.id.tvBillingPeriod);
        tvBillingAdvances = view.findViewById(R.id.tvBillingAdvances);
        tvBillingCosts = view.findViewById(R.id.tvBillingCosts);
        boxBillingBalance = view.findViewById(R.id.boxBillingBalance);
        tvBillingBalanceLabel = view.findViewById(R.id.tvBillingBalanceLabel);
        tvBillingBalanceValue = view.findViewById(R.id.tvBillingBalanceValue);
        viewBillingBalanceBar = view.findViewById(R.id.viewBillingBalanceBar);
        tvBillingBarUnderpayment = view.findViewById(R.id.tvBillingBarUnderpayment);
        tvBillingBarRatio = view.findViewById(R.id.tvBillingBarRatio);
        tvBillingBarOverpayment = view.findViewById(R.id.tvBillingBarOverpayment);
        layoutLastInvoicesSection = view.findViewById(R.id.layoutLastInvoicesSection);
        llInvoicesContainer = view.findViewById(R.id.llInvoicesContainer);
        tvNoRecentInvoices = view.findViewById(R.id.tvNoRecentInvoices);
        MaterialButton btnShowAllInvoices = view.findViewById(R.id.btnShowAllInvoices);

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

        cardPeriodConsumption.setOnClickListener(v -> openCurrentUninvoicedPeriod());
        layoutBillingSummary.setOnClickListener(v -> openCurrentUninvoicedPeriod());
        if (btnShowAllInvoices != null) {
            btnShowAllInvoices.setOnClickListener(v -> openAllInvoicesSection());
        }

        cardConsumptionOverview.setOnClickListener(v -> {
            if (currentPoint == null) return;
            applyDashboardSubscriptionPointIfChanged();
            int initialPeriod = swConsumptionYearly.isChecked()
                    ? GraphMonthFragment.PERIOD_YEAR
                    : GraphMonthFragment.PERIOD_MONTH;
            FragmentChange.replace(requireActivity(), GraphMonthFragment.newInstance(initialPeriod), MOVE, true);
        });
    }


    /**
     * Přejde do existující sekce Fakturace ({@link InvoiceListFragment}) prostřednictvím
     * hlavního navigačního mechanismu v {@link MainActivity}. Před přechodem synchronizuje
     * případně lokálně přepnuté odběrné místo.
     */
    private void openAllInvoicesSection() {
        if (currentPoint == null || !isAdded()) return;
        applyDashboardSubscriptionPointIfChanged();
        if (getActivity() instanceof MainActivity mainActivity) {
            mainActivity.onCheckedNavigationItem(R.id.meni_invoice);
        } else {
            FragmentChange.replace(requireActivity(), InvoiceListFragment.newInstance(), MOVE, true);
        }
    }


    /**
     * Otevře obrazovku fakturace ({@link InvoiceTabFragment}) pro aktuální nevyfakturované období ({@code idFak = -1L}).
     * Před přechodem synchronizuje případně lokálně přepnuté odběrné místo a resetuje stav tlačítek v adaptérech.
     */
    private void openCurrentUninvoicedPeriod() {
        if (currentPoint == null) return;
        applyDashboardSubscriptionPointIfChanged();
        InvoiceAdapter.resetShowButtons();
        PaymentAdapter.resetShowButtons();
        InvoiceTabFragment tabFragment = InvoiceTabFragment.newInstance(
                currentPoint.getTableFAK(),
                currentPoint.getTableTED(),
                currentPoint.getTablePLATBY(),
                currentPoint.getTableO(),
                -1L,
                0,
                MyViewPagerAdapter.TypeTabs.INVOICE
        );
        FragmentChange.replace(requireActivity(), tabFragment, MOVE, true);
    }


    /**
     * Nastaví posluchač posunu nad {@link NestedScrollView}, který při posunu o více než 12 dp
     * přepíná kompaktní režim spodní navigace v {@link MainActivity}.
     */
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


    /**
     * Inicializuje přepínače z uložených předvoleb {@link ShPDashBoard} a naváže posluchače
     * pro změnu režimu ceny (celková / obchodní), DPH, typu grafu spotřeby a přepínání odběrného místa.
     */
    private void setupListeners() {
        boolean showTotalKwh = shPDashBoard.get(ShPDashBoard.PRICE_SHOW_TOTAL_KWH, true);
        boolean withVat = shPDashBoard.get(ShPDashBoard.PRICE_WITH_VAT, true);
        boolean yearlyChart = shPDashBoard.get(ShPDashBoard.CHART_YEARLY_MODE, false);

        togglePriceType.check(showTotalKwh ? R.id.btnPriceTotal : R.id.btnPriceCommercial);
        swPriceVat.setChecked(withVat);
        swConsumptionYearly.setChecked(yearlyChart);
        updateConsumptionSwitchLabels(yearlyChart);

        togglePriceType.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) return;
            boolean isTotal = checkedId == R.id.btnPriceTotal;
            shPDashBoard.set(ShPDashBoard.PRICE_SHOW_TOTAL_KWH, isTotal);
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


    /**
     * Aktualizuje podtitulek karty přehledu spotřeby a popisek přepínače podle zvoleného režimu grafu.
     *
     * @param isYearly {@code true} pro roční přehled, {@code false} pro měsíční přehled
     */
    private void updateConsumptionSwitchLabels(boolean isYearly) {
        tvConsumptionChartSubtitle.setText(isYearly
                ? R.string.dashboard_yearly_consumption_subtitle
                : R.string.dashboard_monthly_consumption_subtitle);
        swConsumptionYearly.setText(isYearly
                ? R.string.dashboard_toggle_yearly
                : R.string.dashboard_toggle_monthly);
    }


    /**
     * Načte všechna odběrná místa a určí zobrazené odběrné místo (přednostně dříve zvolené lokální,
     * jinak globálně aktivní). Při prázdném seznamu skryje obsah a zobrazí upozornění; jinak obnoví
     * Toolbar, přepínač odběrného místa a všechny datové sekce.
     */
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


    /**
     * Nastaví viditelnost kontejnerů s kartami přehledu pro rozložení na výšku i na šířku.
     *
     * @param visibility konstanta viditelnosti pohledu (např. {@link View#VISIBLE} nebo {@link View#GONE})
     */
    private void setContentContainersVisibility(int visibility) {
        if (llDashboardPortraitContainer != null) {
            llDashboardPortraitContainer.setVisibility(visibility);
        }
        if (llDashboardColumnsRow != null) {
            llDashboardColumnsRow.setVisibility(visibility);
        }
    }


    /**
     * Přepne odběrné místo pouze lokálně v rámci přehledu (nemění globální odběrné místo aplikace),
     * uloží index zobrazeného místa v {@link ShPDashBoard} a obnoví všechny sekce.
     *
     * @param index index cílového odběrného místa v seznamu {@code allSubscriptionPoints}
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


    /**
     * Aktualizuje zobrazený název odběrného místa a viditelnost, povolení i průhlednost
     * navigačních šipek pro přepínání mezi odběrnými místy.
     */
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


    /**
     * Obnoví všechny datové a grafické sekce přehledu pro aktuálně vybrané odběrné místo.
     * Sdílená data z databáze (uživatelské barvy VT/NT, posun času, poslední odečet a aktuální ceník)
     * načítá jednou za obnovení, aby se zabránilo zbytečným opakovaným dotazům do databáze.
     */
    private void refreshAllSections() {
        if (currentPoint == null || !isAdded()) return;

        int[] userColors = null;
        long timeShift = 0L;
        DataSettingsSource settingsSource = new DataSettingsSource(requireContext());
        try {
            settingsSource.open();
            userColors = settingsSource.loadColorVTNT();
            timeShift = settingsSource.loadTimeShift(currentPoint.getId());
        } catch (Exception ignored) {
        } finally {
            settingsSource.close();
        }

        MonthlyReadingModel lastReading = loadLastMonthlyReading(currentPoint);
        cachedCurrentPriceList = loadCurrentPriceList(currentPoint, lastReading);

        applyUserVtNtColors(userColors);
        refreshPriceValues();
        refreshMeterStateValues(lastReading);
        refreshHdoAndPriceHighlight(true, timeShift);
        refreshPeriodBillingAndInvoices(cachedCurrentPriceList);
        refreshConsumptionOverviewData(cachedCurrentPriceList);
    }


    /**
     * Promítne uživatelské barvy pro vysoký (VT) a nízký (NT) tarif do legendy,
     * poměrového pruhu, prstencového grafu i sloupcového grafu spotřeby.
     *
     * @param colors pole dvou ARGB barev {@code [colorVt, colorNt]}, nebo {@code null} pro výchozí barvy
     */
    private void applyUserVtNtColors(@Nullable int[] colors) {
        if (!isAdded()) return;
        int colorVt = MaterialColors.harmonizeWithPrimary(
                requireContext(),
                ContextCompat.getColor(requireContext(), R.color.dashboard_teal_vt)
        );
        int colorNt = MaterialColors.getColor(
                requireContext(),
                androidx.appcompat.R.attr.colorPrimary,
                ContextCompat.getColor(requireContext(), R.color.dashboard_teal_nt)
        );

        if (colors != null && colors.length >= 2) {
            colorVt = colors[0];
            colorNt = colors[1];
        }

        if (viewPeriodVtColor != null) {
            viewPeriodVtColor.setBackgroundColor(colorVt);
        }
        if (viewPeriodNtColor != null) {
            viewPeriodNtColor.setBackgroundColor(colorNt);
        }
        if (viewPeriodBar != null) {
            viewPeriodBar.setColors(colorVt, colorNt);
        }
        if (viewPeriodDonut != null) {
            viewPeriodDonut.setColors(colorVt, colorNt);
        }
        if (viewConsumptionBarChart != null) {
            viewConsumptionBarChart.setColors(colorVt, colorNt);
        }
    }


    /**
     * Načte poslední zaznamenaný měsíční odečet (podle data odečtu, nikoli pořadí vložení)
     * z databáze zadaného odběrného místa.
     *
     * @param point odběrné místo
     * @return poslední {@link MonthlyReadingModel}, nebo {@code null}, pokud žádný odečet neexistuje
     */
    @Nullable
    private MonthlyReadingModel loadLastMonthlyReading(@NonNull SubscriptionPointModel point) {
        DataMonthlyReadingSource readingSource = new DataMonthlyReadingSource(requireContext());
        try {
            readingSource.open();
            return readingSource.loadLastMonthlyReadingByDate(point.getTableO());
        } catch (Exception e) {
            return null;
        } finally {
            readingSource.close();
        }
    }


    /**
     * Aktualizuje grafické počítadlo stavu elektroměru (VT a NT) a datum posledního
     * zaznamenaného odečtu v oblasti rychlých akcí.
     *
     * @param lastReading poslední měsíční odečet odběrného místa podle data odečtu, nebo {@code null}
     */
    private void refreshMeterStateValues(@Nullable MonthlyReadingModel lastReading) {
        if (currentPoint == null || !isAdded()) return;

        double vt = (lastReading != null) ? lastReading.getVt() : 0.0;
        double nt = (lastReading != null) ? lastReading.getNt() : 0.0;

        if (numbersMeterDashboardVT != null && numbersMeterDashboardNT != null) {
            int maxMeter = 9_999_999;
            int vtTenths = (int) Math.min(maxMeter, Math.max(0L, Math.round(vt * 10.0)));
            int ntTenths = (int) Math.min(maxMeter, Math.max(0L, Math.round(nt * 10.0)));

            numbersMeterDashboardVT.setCurrentNumber(vtTenths);
            numbersMeterDashboardNT.setCurrentNumber(ntTenths);
        }

        if (tvLastReadingDate != null) {
            if (lastReading != null && lastReading.getDate() > 0L) {
                String formattedDate = ViewHelper.convertLongToDate(lastReading.getDate());
                tvLastReadingDate.setText(getString(R.string.dashboard_last_reading_format, formattedDate));
            } else {
                tvLastReadingDate.setText(R.string.dashboard_last_reading_none);
            }
        }
    }


    /**
     * Spočítá a zobrazí cenu VT a NT (Kč/kWh) z aktuálního ceníku odběrného místa
     * podle stavu přepínačů (Celková cena / Obchodní cena a DPH) s plynulou animací čísel.
     */
    private void refreshPriceValues() {
        if (currentPoint == null || !isAdded()) return;

        PriceListModel priceList = cachedCurrentPriceList;
        if (priceList == null) {
            animatePriceValues(0.0, 0.0);
            return;
        }

        PriceListModel effectivePriceList = new PriceListRegulBuilder(priceList).getRegulPriceList();
        boolean showTotal = togglePriceType.getCheckedButtonId() != R.id.btnPriceCommercial;
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

        animatePriceValues(vtPriceKwh, ntPriceKwh);
    }


    /**
     * Plynule odanimuje změnu číselných hodnot aktuální ceny VT a NT (Kč/kWh),
     * například při přepnutí mezi celkovou a obchodní cenou, změně DPH nebo přepnutí odběrného místa.
     * Při prvním zobrazení nastaví hodnoty okamžitě bez animace z nuly.
     *
     * @param targetVtKwh cílová cena vysokého tarifu (Kč/kWh)
     * @param targetNtKwh cílová cena nízkého tarifu (Kč/kWh)
     */
    private void animatePriceValues(double targetVtKwh, double targetNtKwh) {
        if (!isAdded()) return;

        final double targetVt = Math.max(0.0, targetVtKwh);
        final double targetNt = Math.max(0.0, targetNtKwh);

        if (priceValuesAnimator != null) {
            priceValuesAnimator.cancel();
            priceValuesAnimator = null;
        }

        if (!hasInitializedPriceValues) {
            displayedPriceVtKwh = targetVt;
            displayedPriceNtKwh = targetNt;
            hasInitializedPriceValues = true;
            tvPriceVtValue.setText(DecimalFormatHelper.df2.format(displayedPriceVtKwh));
            tvPriceNtValue.setText(DecimalFormatHelper.df2.format(displayedPriceNtKwh));
            return;
        }

        if (Math.abs(targetVt - displayedPriceVtKwh) < 0.0005
                && Math.abs(targetNt - displayedPriceNtKwh) < 0.0005) {
            displayedPriceVtKwh = targetVt;
            displayedPriceNtKwh = targetNt;
            tvPriceVtValue.setText(DecimalFormatHelper.df2.format(displayedPriceVtKwh));
            tvPriceNtValue.setText(DecimalFormatHelper.df2.format(displayedPriceNtKwh));
            return;
        }

        final double startVt = displayedPriceVtKwh;
        final double startNt = displayedPriceNtKwh;

        priceValuesAnimator = ValueAnimator.ofFloat(0f, 1f);
        priceValuesAnimator.setDuration(PRICE_ANIMATION_DURATION_MS);
        priceValuesAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        priceValuesAnimator.addUpdateListener(anim -> {
            if (!isAdded()) return;
            float fraction = (float) anim.getAnimatedValue();
            displayedPriceVtKwh = startVt + (targetVt - startVt) * fraction;
            displayedPriceNtKwh = startNt + (targetNt - startNt) * fraction;
            tvPriceVtValue.setText(DecimalFormatHelper.df2.format(displayedPriceVtKwh));
            tvPriceNtValue.setText(DecimalFormatHelper.df2.format(displayedPriceNtKwh));
        });
        priceValuesAnimator.start();
    }


    /**
     * Vyhledá aktuální ceník pro zadané odběrné místo. Nejprve čte ID ceníku z posledního
     * měsíčního odečtu, a pokud není k dispozici, z posledního záznamu v aktuálním období ({@code TED})
     * nebo z historie faktur ({@code FAK}).
     *
     * @param point       odběrné místo, pro které se ceník hledá
     * @param lastReading již načtený poslední měsíční odečet odběrného místa (může být {@code null})
     * @return načtený {@link PriceListModel}, nebo {@code null}, pokud ceník nelze určit či načíst
     */
    @Nullable
    private PriceListModel loadCurrentPriceList(@NonNull SubscriptionPointModel point,
                                                @Nullable MonthlyReadingModel lastReading) {
        long priceListId = (lastReading != null && lastReading.getPriceListId() > 0)
                ? lastReading.getPriceListId()
                : -1L;

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
     * Periodicky aktualizuje stav HDO (zda běží NT nebo VT, čas dalšího přepnutí, odpočet a ciferník 24h hodin)
     * bez úvodní animace ciferníku, včetně načtení časového posunu elektroměru z databáze nastavení.
     */
    private void refreshHdoAndPriceHighlightPeriodic() {
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

        refreshHdoAndPriceHighlight(false, timeShift);
    }


    /**
     * Aktualizuje stav HDO (zda běží NT nebo VT, čas dalšího přepnutí, odpočet a ciferník 24h hodin)
     * ze stejného zdroje HDO dat a se shodným časovým posunem elektroměru.
     *
     * @param animateClock {@code true}, pokud se má animovat přechod ciferníku (např. při změně odběrného místa)
     * @param timeShift    časový posun elektroměru v milisekundách
     */
    private void refreshHdoAndPriceHighlight(boolean animateClock, long timeShift) {
        if (currentPoint == null || !isAdded()) return;

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
            applyHdoNextSwitchDisplay(getString(R.string.dashboard_hdo_not_set), "", null);
            return;
        }

        long nowMillis = System.currentTimeMillis();
        long meterNowMillis = nowMillis + timeShift;
        Calendar meterCal = Calendar.getInstance();
        meterCal.setTimeInMillis(meterNowMillis);

        boolean isNtActive = HdoTime.checkHdo(activeRelayModels, meterCal);
        applyTariffBoxHighlight(isNtActive);

        int nextType = isNtActive ? HdoAlarmScheduler.TYPE_END : HdoAlarmScheduler.TYPE_START;
        long nextTriggerMillis = HdoAlarmScheduler.findNextTriggerForModels(activeRelayModels, nextType, nowMillis, timeShift);

        DashboardHdoFormatHelper.FormattedHdoSwitch formatted = (nextTriggerMillis > 0L)
                ? DashboardHdoFormatHelper.formatNextSwitch(nextTriggerMillis + timeShift, meterNowMillis)
                : DashboardHdoFormatHelper.FormattedHdoSwitch.invalid();

        if (formatted.valid()) {
            String targetLabel = getString(
                    formatted.isToday()
                            ? R.string.dashboard_hdo_next_switch_today_label
                            : R.string.dashboard_hdo_next_switch_day_label
            );
            String countdownText = (formatted.remainingText() != null)
                    ? getString(R.string.dashboard_hdo_remaining_format, formatted.remainingText())
                    : null;
            applyHdoNextSwitchDisplay(targetLabel, formatted.timeText(), countdownText);
        } else {
            applyHdoNextSwitchDisplay(getString(R.string.dashboard_hdo_invalid_data), "", null);
        }
    }


    /**
     * Aktualizuje zobrazení popisku, absolutního času následujícího přepnutí HDO a relativního
     * odpočtu zbývající doby. Při změně údajů plynule animuje přechod textů.
     *
     * @param targetLabel    cílový text popisku (např. {@code HDO: další přepnutí dnes v})
     * @param targetTimeText naformátovaný absolutní čas přepnutí (např. {@code 21:41} nebo {@code Čt 01:30}), nebo prázdný řetězec
     * @param countdownText  naformátovaný text zbývající doby do přepnutí, nebo {@code null} pro skrytí odpočtu
     */
    private void applyHdoNextSwitchDisplay(@NonNull String targetLabel,
                                           @NonNull String targetTimeText,
                                           @Nullable String countdownText) {
        if (!isAdded()) return;

        if (tvHdoCountdown != null) {
            if (countdownText != null && !countdownText.isEmpty()) {
                tvHdoCountdown.setText(countdownText);
                tvHdoCountdown.setVisibility(View.VISIBLE);
            } else {
                tvHdoCountdown.setText("");
                tvHdoCountdown.setVisibility(View.GONE);
            }
        }

        if (hdoNextSwitchAnimator != null) {
            hdoNextSwitchAnimator.cancel();
            hdoNextSwitchAnimator = null;
        }

        if (!hasInitializedHdoNextSwitch) {
            displayedHdoNextSwitchTimeText = targetTimeText;
            displayedHdoNextSwitchLabel = targetLabel;
            hasInitializedHdoNextSwitch = true;
            bindHdoNextSwitchDisplay(
                    targetTimeText,
                    targetTimeText,
                    targetLabel,
                    targetLabel,
                    1f
            );
            return;
        }

        final String startTimeText = (displayedHdoNextSwitchTimeText != null) ? displayedHdoNextSwitchTimeText : "";
        final String startLabel = (displayedHdoNextSwitchLabel == null || displayedHdoNextSwitchLabel.isEmpty())
                ? targetLabel
                : displayedHdoNextSwitchLabel;

        boolean sameLabel = startLabel.equals(targetLabel);
        boolean sameTime = startTimeText.equals(targetTimeText);

        if (sameLabel && sameTime) {
            displayedHdoNextSwitchTimeText = targetTimeText;
            displayedHdoNextSwitchLabel = targetLabel;
            bindHdoNextSwitchDisplay(
                    targetTimeText,
                    targetTimeText,
                    targetLabel,
                    targetLabel,
                    1f
            );
            return;
        }

        hdoNextSwitchAnimator = ValueAnimator.ofFloat(0f, 1f);
        hdoNextSwitchAnimator.setDuration(PERIOD_ANIMATION_DURATION_MS);
        hdoNextSwitchAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        hdoNextSwitchAnimator.addUpdateListener(anim -> {
            if (!isAdded()) return;
            float fraction = (float) anim.getAnimatedValue();
            bindHdoNextSwitchDisplay(
                    startTimeText,
                    targetTimeText,
                    startLabel,
                    targetLabel,
                    fraction
            );
        });
        hdoNextSwitchAnimator.start();
    }


    /**
     * Promítne průběžný krok animace přechodu popisku a času příští změny HDO do pohledů
     * na kartě aktuální ceny.
     *
     * @param startTimeText  výchozí naformátovaný čas přepnutí HDO
     * @param targetTimeText cílový naformátovaný čas přepnutí HDO
     * @param startLabel     výchozí text popisku
     * @param targetLabel    cílový text popisku
     * @param fraction       průběh animace v intervalu {@code 0f..1f}
     */
    private void bindHdoNextSwitchDisplay(@NonNull String startTimeText,
                                          @NonNull String targetTimeText,
                                          @NonNull String startLabel,
                                          @NonNull String targetLabel,
                                          float fraction) {
        if (!isAdded()) return;

        float maxShiftPx = dpToPx(requireContext(), 4f);

        if (startLabel.equals(targetLabel) || fraction >= 0.999f) {
            tvHdoNextSwitchLabel.setText(targetLabel);
            tvHdoNextSwitchLabel.setAlpha(1f);
            tvHdoNextSwitchLabel.setTranslationY(0f);
            displayedHdoNextSwitchLabel = targetLabel;
        } else if (fraction < 0.5f) {
            float outProgress = fraction * 2f;
            tvHdoNextSwitchLabel.setText(startLabel);
            tvHdoNextSwitchLabel.setAlpha(1f - outProgress);
            tvHdoNextSwitchLabel.setTranslationY(-maxShiftPx * outProgress);
            displayedHdoNextSwitchLabel = startLabel;
        } else {
            float inProgress = (fraction - 0.5f) * 2f;
            tvHdoNextSwitchLabel.setText(targetLabel);
            tvHdoNextSwitchLabel.setAlpha(inProgress);
            tvHdoNextSwitchLabel.setTranslationY(maxShiftPx * (1f - inProgress));
            displayedHdoNextSwitchLabel = targetLabel;
        }

        if (startTimeText.equals(targetTimeText) || fraction >= 0.999f) {
            tvHdoNextSwitchTime.setText(targetTimeText);
            tvHdoNextSwitchTime.setAlpha(1f);
            tvHdoNextSwitchTime.setTranslationY(0f);
            tvHdoNextSwitchTime.setVisibility(targetTimeText.isEmpty() ? View.GONE : View.VISIBLE);
            displayedHdoNextSwitchTimeText = targetTimeText;
        } else if (fraction < 0.5f) {
            float outProgress = fraction * 2f;
            tvHdoNextSwitchTime.setText(startTimeText);
            tvHdoNextSwitchTime.setVisibility(startTimeText.isEmpty() ? View.GONE : View.VISIBLE);
            tvHdoNextSwitchTime.setAlpha(1f - outProgress);
            tvHdoNextSwitchTime.setTranslationY(-maxShiftPx * outProgress);
            displayedHdoNextSwitchTimeText = startTimeText;
        } else {
            float inProgress = (fraction - 0.5f) * 2f;
            tvHdoNextSwitchTime.setText(targetTimeText);
            tvHdoNextSwitchTime.setVisibility(targetTimeText.isEmpty() ? View.GONE : View.VISIBLE);
            tvHdoNextSwitchTime.setAlpha(inProgress);
            tvHdoNextSwitchTime.setTranslationY(maxShiftPx * (1f - inProgress));
            displayedHdoNextSwitchTimeText = targetTimeText;
        }
    }


    /**
     * Aktualizuje zvýraznění aktivní sazby (VT / NT) na kartě aktuální ceny elektřiny.
     * Při změně aktivní sazby plynule animuje přechod barvy pozadí, ohraničení, textů i ikony zaškrtnutí.
     *
     * @param isNtActive {@code true}, pokud je aktivní nízký tarif (NT), jinak {@code false} (VT)
     */
    private void applyTariffBoxHighlight(boolean isNtActive) {
        if (!isAdded()) return;

        final float targetNtRatio = isNtActive ? 1f : 0f;

        if (tariffHighlightAnimator != null) {
            tariffHighlightAnimator.cancel();
            tariffHighlightAnimator = null;
        }

        if (!hasInitializedTariffHighlight) {
            displayedTariffNtRatio = targetNtRatio;
            hasInitializedTariffHighlight = true;
            bindTariffBoxHighlight(displayedTariffNtRatio);
            return;
        }

        if (Math.abs(targetNtRatio - displayedTariffNtRatio) < 0.001f) {
            displayedTariffNtRatio = targetNtRatio;
            bindTariffBoxHighlight(displayedTariffNtRatio);
            return;
        }

        final float startNtRatio = displayedTariffNtRatio;
        tariffHighlightAnimator = ValueAnimator.ofFloat(startNtRatio, targetNtRatio);
        tariffHighlightAnimator.setDuration(PERIOD_ANIMATION_DURATION_MS);
        tariffHighlightAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        tariffHighlightAnimator.addUpdateListener(anim -> {
            if (!isAdded()) return;
            displayedTariffNtRatio = (float) anim.getAnimatedValue();
            bindTariffBoxHighlight(displayedTariffNtRatio);
        });
        tariffHighlightAnimator.start();
    }


    /**
     * Aplikuje interpolovaný poměr zvýraznění aktivní sazby na boxy VT a NT na kartě aktuální ceny.
     *
     * @param ntActiveRatio míra aktivace nízkého tarifu v intervalu {@code 0f} (aktivní VT) až {@code 1f} (aktivní NT)
     */
    private void bindTariffBoxHighlight(float ntActiveRatio) {
        if (!isAdded()) return;

        float clampedNtRatio = Math.max(0f, Math.min(1f, ntActiveRatio));
        float vtActiveRatio = 1f - clampedNtRatio;

        int colorActiveBg = MaterialColors.getColor(
                requireContext(),
                androidx.appcompat.R.attr.colorPrimary,
                ContextCompat.getColor(requireContext(), R.color.dashboard_price_box_active_bg)
        );
        int colorSurfaceLow = MaterialColors.getColor(
                requireContext(),
                com.google.android.material.R.attr.colorSurfaceContainerLow,
                ContextCompat.getColor(requireContext(), R.color.dashboard_price_box_inactive_bg)
        );
        int colorInactiveBg = ColorUtils.setAlphaComponent(colorSurfaceLow, 217);
        int colorInactiveStroke = MaterialColors.getColor(
                requireContext(),
                com.google.android.material.R.attr.colorOutlineVariant,
                ContextCompat.getColor(requireContext(), R.color.dashboard_price_box_inactive_stroke)
        );
        int colorOnSurface = resolveAttrColor(com.google.android.material.R.attr.colorOnSurface);
        int colorOnActive = MaterialColors.getColor(
                requireContext(),
                com.google.android.material.R.attr.colorOnPrimary,
                ContextCompat.getColor(requireContext(), android.R.color.white)
        );
        float cornerRadiusPx = dpToPx(requireContext(), 14f);
        int strokeWidthPx = Math.max(1, dpToPx(requireContext(), 1f));

        if (boxPriceVtBgDrawable == null) {
            boxPriceVtBgDrawable = new GradientDrawable();
            boxPriceVtBgDrawable.setShape(GradientDrawable.RECTANGLE);
            boxPriceVtBgDrawable.setCornerRadius(cornerRadiusPx);
        }
        if (boxPriceNtBgDrawable == null) {
            boxPriceNtBgDrawable = new GradientDrawable();
            boxPriceNtBgDrawable.setShape(GradientDrawable.RECTANGLE);
            boxPriceNtBgDrawable.setCornerRadius(cornerRadiusPx);
        }

        bindSingleTariffBox(
                boxPriceVt, boxPriceVtBgDrawable,
                tvLabelVt, tvPriceVtValue, tvPriceVtUnit, ivCheckVt,
                vtActiveRatio, colorInactiveBg, colorActiveBg, colorInactiveStroke,
                colorOnSurface, colorOnActive, strokeWidthPx
        );
        bindSingleTariffBox(
                boxPriceNt, boxPriceNtBgDrawable,
                tvLabelNt, tvPriceNtValue, tvPriceNtUnit, ivCheckNt,
                clampedNtRatio, colorInactiveBg, colorActiveBg, colorInactiveStroke,
                colorOnSurface, colorOnActive, strokeWidthPx
        );
    }


    /**
     * Nastaví barvu pozadí, ohraničení, textů a viditelnost/škálu ikony zaškrtnutí
     * jednoho tarifního boxu (VT nebo NT) podle míry jeho aktivace.
     *
     * @param box                 kontejner tarifního boxu
     * @param bgDrawable          sdílený {@link GradientDrawable} použitý jako pozadí boxu
     * @param tvLabel             popisek sazby (VT/NT)
     * @param tvValue             textová hodnota ceny
     * @param tvUnit              text jednotky ceny
     * @param ivCheck             ikona zaškrtnutí aktivní sazby
     * @param activeRatio         míra aktivace v intervalu {@code 0f..1f}
     * @param colorInactiveBg     barva pozadí neaktivního boxu
     * @param colorActiveBg       barva pozadí aktivního boxu
     * @param colorInactiveStroke barva ohraničení neaktivního boxu
     * @param colorOnSurface      barva textu v neaktivním stavu
     * @param colorOnActive       barva textu a ikony v aktivním stavu
     * @param strokeWidthPx       šířka ohraničení v pixelech
     */
    private void bindSingleTariffBox(@NonNull View box,
                                     @NonNull GradientDrawable bgDrawable,
                                     @NonNull TextView tvLabel,
                                     @NonNull TextView tvValue,
                                     @NonNull TextView tvUnit,
                                     @NonNull ImageView ivCheck,
                                     float activeRatio,
                                     int colorInactiveBg,
                                     int colorActiveBg,
                                     int colorInactiveStroke,
                                     int colorOnSurface,
                                     int colorOnActive,
                                     int strokeWidthPx) {
        int bgColor = (int) argbEvaluator.evaluate(activeRatio, colorInactiveBg, colorActiveBg);
        int strokeColor = (int) argbEvaluator.evaluate(activeRatio, colorInactiveStroke, colorActiveBg);
        int textColor = (int) argbEvaluator.evaluate(activeRatio, colorOnSurface, colorOnActive);

        bgDrawable.setColor(bgColor);
        bgDrawable.setStroke(strokeWidthPx, strokeColor);
        if (box.getBackground() != bgDrawable) {
            box.setBackground(bgDrawable);
        }

        tvLabel.setTextColor(textColor);
        tvValue.setTextColor(textColor);
        tvUnit.setTextColor(textColor);
        ivCheck.setImageTintList(ColorStateList.valueOf(textColor));

        if (activeRatio <= 0.01f) {
            ivCheck.setVisibility(View.GONE);
            ivCheck.setAlpha(0f);
        } else {
            ivCheck.setVisibility(View.VISIBLE);
            ivCheck.setAlpha(activeRatio);
            float scale = 0.6f + 0.4f * activeRatio;
            ivCheck.setScaleX(scale);
            ivCheck.setScaleY(scale);
        }
    }


    /**
     * Načte data pro karty „Spotřeba období“, „Vyúčtování“ a seznam až {@link #MAX_RECENT_INVOICES}
     * posledních fakturačních období s proklikem do {@link InvoiceTabFragment}.
     *
     * @param fallbackPriceList aktuální ceník pro fallback rozpoznání jednosazbového tarifu
     */
    private void refreshPeriodBillingAndInvoices(@Nullable PriceListModel fallbackPriceList) {
        if (currentPoint == null || !isAdded()) return;

        DataInvoiceSource invoiceSource = new DataInvoiceSource(requireContext());
        DataPriceListSource priceListSource = new DataPriceListSource(requireContext());
        ArrayList<InvoiceListModel> invoiceLists;

        try {
            invoiceSource.open();
            priceListSource.open();

            invoiceLists = invoiceSource.loadInvoiceLists(currentPoint);

            // Určení aktivního období (nejprve období bez faktury idFak == -1L, případně nejnovější faktura)
            InvoiceListModel activePeriodModel = getActivePeriodModel(invoiceLists);

            double targetBillingAdvancesKc = 0.0;
            double targetBillingCostsKc = 0.0;
            long targetMinDate = 0L;
            long targetMaxDate = 0L;

            if (activePeriodModel != null) {
                long idFak = activePeriodModel.getIdFak();
                String table = (idFak == -1L) ? currentPoint.getTableTED() : currentPoint.getTableFAK();
                PeriodSummary summary = computeInvoiceSummary(invoiceSource, priceListSource, currentPoint, idFak, table, fallbackPriceList);

                targetMinDate = activePeriodModel.getMinDate();
                targetMaxDate = activePeriodModel.getMaxDate();

                double effectiveNtKwh = summary.isSingleTariff ? 0.0 : summary.ntKwh;
                animatePeriodConsumptionValues(summary.vtKwh, effectiveNtKwh, summary.isSingleTariff);
                viewPeriodBar.setConsumption(summary.vtKwh, effectiveNtKwh);
                viewPeriodDonut.setConsumption(summary.vtKwh, effectiveNtKwh);

                targetBillingAdvancesKc = summary.advancesKc;
                targetBillingCostsKc = summary.costsWithTaxKc;
            } else {
                boolean isSingle = fallbackPriceList != null && isSingleTariffRate(fallbackPriceList.getRate());
                animatePeriodConsumptionValues(0.0, 0.0, isSingle);
                viewPeriodBar.setConsumption(0, 0);
                viewPeriodDonut.setConsumption(0, 0);
            }

            // Poslední faktury (maximálně MAX_RECENT_INVOICES položek)
            llInvoicesContainer.removeAllViews();
            int addedInvoices = 0;
            LayoutInflater inflater = LayoutInflater.from(requireContext());
            List<TextView> invoiceConsumptionViews = new ArrayList<>();
            List<Double> targetInvoiceConsumptionsKwh = new ArrayList<>();
            List<TextView> invoiceAmountViews = new ArrayList<>();
            List<Double> targetInvoiceAmountsKc = new ArrayList<>();

            int colorOnSurface = resolveAttrColor(com.google.android.material.R.attr.colorOnSurface);
            int colorOnSurfaceVariant = resolveAttrColor(com.google.android.material.R.attr.colorOnSurfaceVariant);

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
                        currentPoint.getTableFAK(),
                        fallbackPriceList
                );

                View row = inflater.inflate(R.layout.item_dashboard_invoice_row, llInvoicesContainer, false);
                TextView tvNumber = row.findViewById(R.id.tvInvoiceNumber);
                TextView tvDates = row.findViewById(R.id.tvInvoiceDates);
                TextView tvConsumption = row.findViewById(R.id.tvInvoiceConsumption);
                TextView tvAmount = row.findViewById(R.id.tvInvoiceAmount);

                String rawNumber = invItem.getNumberInvoice();
                boolean isWithoutInvoiceNumber = rawNumber == null
                        || rawNumber.trim().isEmpty();
                if (isWithoutInvoiceNumber) {
                    tvNumber.setText(R.string.dashboard_invoice_without_number);
                    tvNumber.setTypeface(null, Typeface.ITALIC);
                    tvNumber.setTextColor(colorOnSurfaceVariant);
                } else {
                    tvNumber.setText(getString(R.string.dashboard_invoice_number_format, rawNumber.trim()));
                    tvNumber.setTypeface(null, Typeface.BOLD);
                    tvNumber.setTextColor(colorOnSurface);
                }

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

            layoutLastInvoicesSection.setVisibility(View.VISIBLE);
            llInvoicesContainer.setVisibility(addedInvoices > 0 ? View.VISIBLE : View.GONE);
            if (tvNoRecentInvoices != null) {
                tvNoRecentInvoices.setVisibility(addedInvoices == 0 ? View.VISIBLE : View.GONE);
            }

            if (viewBillingBalanceBar != null) {
                viewBillingBalanceBar.setValues(targetBillingAdvancesKc, targetBillingCostsKc);
            }
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


    /**
     * Vybere aktivní fakturační období ze seznamu: upřednostní nevyfakturované období ({@code idFak == -1L})
     * s platným rozsahem dat, jinak vrátí první období s platnými daty.
     *
     * @param invoiceLists seznam fakturačních období odběrného místa
     * @return vybraný {@link InvoiceListModel}, nebo {@code null}, pokud žádné vhodné období neexistuje
     */
    @Nullable
    private static InvoiceListModel getActivePeriodModel(ArrayList<InvoiceListModel> invoiceLists) {
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
        return activePeriodModel;
    }


    /**
     * Vykreslí štítek přeplatku/nedoplatku s interpolovanou barvou pozadí i textu,
     * zaokrouhlenou částkou a plynulou animací přechodu popisku.
     *
     * @param balanceKc    aktuální saldo (zálohy mínus náklady) v Kč
     * @param colorRatio   poměr přechodu barvy mezi přeplatkem ({@code 0f}) a nedoplatkem ({@code 1f})
     * @param startLabel   výchozí text popisku
     * @param targetLabel  cílový text popisku
     * @param animFraction průběh animace v intervalu {@code 0f..1f}
     */
    private void bindBalanceBadge(double balanceKc,
                                  float colorRatio,
                                  @NonNull String startLabel,
                                  @NonNull String targetLabel,
                                  float animFraction) {
        if (!isAdded()) return;

        long rounded = Math.round(balanceKc);
        float clampedColorRatio = Math.max(0f, Math.min(1f, colorRatio));

        int colorOverBg = MaterialColors.harmonizeWithPrimary(
                requireContext(),
                ContextCompat.getColor(requireContext(), R.color.dashboard_overpayment_bg)
        );
        int colorUnderBg = MaterialColors.harmonizeWithPrimary(
                requireContext(),
                ContextCompat.getColor(requireContext(), R.color.dashboard_underpayment_bg)
        );
        int colorOverText = MaterialColors.harmonizeWithPrimary(
                requireContext(),
                ContextCompat.getColor(requireContext(), R.color.dashboard_overpayment_text)
        );
        int colorUnderText = MaterialColors.harmonizeWithPrimary(
                requireContext(),
                ContextCompat.getColor(requireContext(), R.color.dashboard_underpayment_text)
        );

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


    /**
     * Vrátí lokalizovaný popisek stavu vyúčtování (přeplatek, nedoplatek nebo vyrovnáno)
     * podle znaménka zaokrouhleného salda.
     *
     * @param roundedBalanceKc zaokrouhlený rozdíl záloh a nákladů v Kč
     * @return lokalizovaný text popisku salda
     */
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


    /**
     * Aktualizuje textové popisky směru (nedoplatek / přeplatek) a procentuálního poměru
     * pod bilaterálním ukazatelem v kartě „Vyúčtování“.
     *
     * @param advancesKc aktuálně zobrazovaná výše záloh v Kč
     * @param costsKc    aktuálně zobrazovaná výše nákladů v Kč
     */
    private void bindBillingBalancePercentages(double advancesKc, double costsKc) {
        if (!isAdded()
                || tvBillingBarUnderpayment == null
                || tvBillingBarRatio == null
                || tvBillingBarOverpayment == null) {
            return;
        }

        double cleanAdvances = Math.max(0.0, advancesKc);
        double cleanCosts = Math.max(0.0, costsKc);
        long roundedAdvances = Math.round(cleanAdvances);
        long roundedCosts = Math.round(cleanCosts);
        long roundedBalance = roundedAdvances - roundedCosts;

        int costsPct = DashboardBillingBalanceBarView.calculateCostsToAdvancesPercent(cleanAdvances, cleanCosts);
        int balancePct = DashboardBillingBalanceBarView.calculateBalancePercent(cleanAdvances, cleanCosts);

        int colorMuted = resolveAttrColor(com.google.android.material.R.attr.colorOnSurfaceVariant);
        int colorOver = MaterialColors.harmonizeWithPrimary(
                requireContext(),
                ContextCompat.getColor(requireContext(), R.color.dashboard_overpayment_text)
        );
        int colorUnder = MaterialColors.harmonizeWithPrimary(
                requireContext(),
                ContextCompat.getColor(requireContext(), R.color.dashboard_underpayment_text)
        );

        // Středový popisek (poměr nákladů vůči zálohám)
        boolean isBalancedHighlight = false;
        if (roundedAdvances == 0L && roundedCosts == 0L) {
            tvBillingBarRatio.setText(R.string.dashboard_billing_bar_empty_percent);
        } else if (costsPct < 0) {
            tvBillingBarRatio.setText(R.string.dashboard_billing_bar_no_advances);
        } else if (roundedBalance == 0L) {
            tvBillingBarRatio.setText(R.string.dashboard_billing_bar_balanced_percent);
            isBalancedHighlight = true;
        } else if (costsPct > 999) {
            tvBillingBarRatio.setText(R.string.dashboard_billing_bar_costs_over_max);
        } else {
            tvBillingBarRatio.setText(getString(R.string.dashboard_billing_bar_costs_percent_format, costsPct));
        }
        tvBillingBarRatio.setTypeface(null, isBalancedHighlight ? Typeface.BOLD : Typeface.NORMAL);

        // Levý (nedoplatek) a pravý (přeplatek) popisek
        if (roundedBalance > 0L) {
            tvBillingBarUnderpayment.setText(R.string.dashboard_underpayment_label);
            tvBillingBarUnderpayment.setTextColor(colorMuted);
            tvBillingBarUnderpayment.setTypeface(null, Typeface.NORMAL);
            tvBillingBarUnderpayment.setAlpha(0.65f);

            if (balancePct > 0) {
                tvBillingBarOverpayment.setText(getString(R.string.dashboard_billing_bar_overpayment_percent_format, balancePct));
            } else {
                tvBillingBarOverpayment.setText(R.string.dashboard_billing_bar_overpayment_under_one);
            }
            tvBillingBarOverpayment.setTextColor(colorOver);
            tvBillingBarOverpayment.setTypeface(null, Typeface.BOLD);
            tvBillingBarOverpayment.setAlpha(1f);
        } else if (roundedBalance < 0L) {
            int underPct = Math.abs(balancePct);
            if (underPct > 999) {
                tvBillingBarUnderpayment.setText(R.string.dashboard_billing_bar_underpayment_over_max);
            } else if (underPct > 0) {
                tvBillingBarUnderpayment.setText(getString(R.string.dashboard_billing_bar_underpayment_percent_format, underPct));
            } else {
                tvBillingBarUnderpayment.setText(R.string.dashboard_billing_bar_underpayment_under_one);
            }
            tvBillingBarUnderpayment.setTextColor(colorUnder);
            tvBillingBarUnderpayment.setTypeface(null, Typeface.BOLD);
            tvBillingBarUnderpayment.setAlpha(1f);

            tvBillingBarOverpayment.setText(R.string.dashboard_overpayment_label);
            tvBillingBarOverpayment.setTextColor(colorMuted);
            tvBillingBarOverpayment.setTypeface(null, Typeface.NORMAL);
            tvBillingBarOverpayment.setAlpha(0.65f);
        } else {
            tvBillingBarUnderpayment.setText(R.string.dashboard_underpayment_label);
            tvBillingBarUnderpayment.setTextColor(colorMuted);
            tvBillingBarUnderpayment.setTypeface(null, Typeface.NORMAL);
            tvBillingBarUnderpayment.setAlpha(0.65f);

            tvBillingBarOverpayment.setText(R.string.dashboard_overpayment_label);
            tvBillingBarOverpayment.setTextColor(colorMuted);
            tvBillingBarOverpayment.setTypeface(null, Typeface.NORMAL);
            tvBillingBarOverpayment.setAlpha(0.65f);
        }
    }


    /**
     * Zkrátí textový rozsah dvou dat formátu {@code dd.MM.yyyy} vynecháním roku u počátečního data,
     * pokud obě data spadají do téhož kalendářního roku.
     *
     * @param startDate počáteční datum období
     * @param endDate   koncové datum období
     * @return naformátovaný textový rozsah období
     */
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


    /**
     * Spočítá souhrn spotřeby VT/NT, uhrazených záloh, celkových nákladů s DPH
     * (včetně POZE podle roku platnosti ceníku, dalších služeb a slev) a vyhodnotí,
     * zda se jedná o jednosazbový tarif.
     *
     * @param invoiceSource     otevřený zdroj faktur a plateb
     * @param priceListSource   otevřený zdroj ceníků
     * @param point             odběrné místo
     * @param idFak             identifikátor faktury, nebo {@code -1L} pro aktuální nevyfakturované období
     * @param table             název zdrojové tabulky ({@code TED} nebo {@code FAK})
     * @param fallbackPriceList aktuální ceník pro fallback rozpoznání jednosazbového tarifu
     * @return vypočtený souhrn období {@link PeriodSummary}
     */
    @NonNull
    private PeriodSummary computeInvoiceSummary(@NonNull DataInvoiceSource invoiceSource,
                                                @NonNull DataPriceListSource priceListSource,
                                                @NonNull SubscriptionPointModel point,
                                                long idFak,
                                                @NonNull String table,
                                                @Nullable PriceListModel fallbackPriceList) {
        ArrayList<InvoiceModel> records = invoiceSource.loadInvoices(idFak, table);
        double advances = invoiceSource.sumAdvancePaymentsOnly(idFak, point.getTablePLATBY());
        if (advances <= 0) {
            advances = invoiceSource.sumPayment(idFak, point.getTablePLATBY());
        }
        double discountWithTax = invoiceSource.sumDiscountWithTax(idFak, point.getTablePLATBY());

        if (records == null || records.isEmpty()) {
            boolean isSingle = fallbackPriceList != null && isSingleTariffRate(fallbackPriceList.getRate());
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
                    price[3] = ntVtMwh * (priceList.getValidityYear() == 2026
                            ? regulPriceList.getPoze1()
                            : regulPriceList.getPoze2());
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
            isSingleTariff = fallbackPriceList != null && isSingleTariffRate(fallbackPriceList.getRate());
        }

        double totalWithTax = Math.max(0.0, totalDph - discountWithTax);
        return new PeriodSummary(totalVtMwh * 1000.0, totalNtMwh * 1000.0, advances, totalWithTax, isSingleTariff);
    }


    /**
     * Plynule odanimuje přičítání/odečítání číselných hodnot VT a NT v kartě „Spotřeba období“
     * a postupné zobrazení či skrytí řádku NT u jednosazbového tarifu ({@code D 01d}, {@code D 02d}).
     *
     * @param targetVtKwh    cílová spotřeba VT v kWh
     * @param targetNtKwh    cílová spotřeba NT v kWh
     * @param isSingleTariff {@code true}, pokud jde o jednosazbový tarif a řádek NT se má skrýt
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

            double totalKwh = Math.max(0.0, displayedPeriodVtKwh) + Math.max(0.0, displayedPeriodNtKwh);
            int vtPct;
            int ntPct;
            if (totalKwh > 0.0001) {
                vtPct = (int) Math.round((Math.max(0.0, displayedPeriodVtKwh) / totalKwh) * 100.0);
                vtPct = Math.max(0, Math.min(100, vtPct));
                ntPct = 100 - vtPct;
            } else {
                vtPct = 0;
                ntPct = 0;
            }
            if (tvPeriodVtPercent != null) {
                tvPeriodVtPercent.setText(String.format(Locale.getDefault(), "(%d %%)", vtPct));
            }
            if (tvPeriodNtPercent != null) {
                tvPeriodNtPercent.setText(String.format(Locale.getDefault(), "(%d %%)", ntPct));
            }

            applyNtRowVisibilityFraction(displayedPeriodNtVisibility);
        });
        periodValuesAnimator.start();
    }


    /**
     * Plynule odanimuje přičítání/odečítání číselných hodnot a datumového rozsahu v kartě „Vyúčtování“
     * (období, zálohy, náklady, přeplatek/nedoplatek včetně přechodu barev a textu štítku
     * i spotřeby a celkové ceny s DPH v seznamu posledních faktur).
     *
     * @param targetAdvancesKc             cílová výše záloh v Kč
     * @param targetCostsKc                cílová výše nákladů v Kč
     * @param targetMinDateMillis          počáteční datum aktivního období v ms (nebo {@code <= 0})
     * @param targetMaxDateMillis          koncové datum aktivního období v ms (nebo {@code <= 0})
     * @param invoiceConsumptionViews      textová pole spotřeb v řádcích posledních faktur
     * @param targetInvoiceConsumptionsKwh cílové spotřeby posledních faktur v kWh
     * @param invoiceAmountViews           textová pole částek v řádcích posledních faktur
     * @param targetInvoiceAmountsKc       cílové částky posledních faktur s DPH v Kč
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
            bindBillingBalancePercentages(displayedBillingAdvancesKc, displayedBillingCostsKc);

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


    /**
     * Aktualizuje zobrazení rozsahu dat v kartě spotřeby období i v kartě vyúčtování
     * v průběhu animace, včetně plynulého zobrazení nebo skrytí při přechodu z/do prázdného stavu.
     *
     * @param startMinDate  výchozí počáteční datum v ms
     * @param startMaxDate  výchozí koncové datum v ms
     * @param targetMinDate cílové počáteční datum v ms
     * @param targetMaxDate cílové koncové datum v ms
     * @param fraction      průběh animace v intervalu {@code 0f..1f}
     */
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


    /**
     * Nastaví výšku, horní okraj, průhlednost a viditelnost řádku NT v kartě spotřeby období
     * podle průběhu přechodu mezi jednosazbovým a dvousazbovým tarifem.
     *
     * @param fraction míra zobrazení řádku NT v intervalu {@code 0f} (skryto) až {@code 1f} (plně zobrazeno)
     */
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
        } else {
            if (clamped >= 0.995f) {
                lp.height = ViewGroup.LayoutParams.WRAP_CONTENT;
                lp.topMargin = fullTopMargin;
                layoutPeriodNtRow.setAlpha(1f);
            } else {
                lp.height = Math.max(1, Math.round(ntRowMeasuredHeight * clamped));
                lp.topMargin = Math.round(fullTopMargin * clamped);
                layoutPeriodNtRow.setAlpha(clamped);
            }
            layoutPeriodNtRow.setLayoutParams(lp);
            layoutPeriodNtRow.setVisibility(View.VISIBLE);
        }
    }


    /**
     * Rozpozná, zda název distribuční sazby odpovídá jednosazbovému tarifu
     * ({@code D 01d} nebo {@code D 02d}, včetně varianty bez mezery).
     *
     * @param rate název distribuční sazby z ceníku
     * @return {@code true} pro jednosazbový tarif, jinak {@code false}
     */
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
     * Načte měsíční a roční spotřebu pro kartu "Přehled spotřeby" včetně výpočtu
     * procentní změny průměrné denní spotřeby (kWh/den) a odhadu neúplného aktuálního měsíce / roku.
     *
     * @param currentPriceList aktuální ceník pro určení distribuční sazby (může být {@code null})
     */
    private void refreshConsumptionOverviewData(@Nullable PriceListModel currentPriceList) {
        monthlyChartEntries.clear();
        yearlyChartEntries.clear();

        if (currentPoint == null || !isAdded()) return;

        String distributionRate = currentPriceList != null ? currentPriceList.getRate() : null;
        Calendar nowCal = Calendar.getInstance();
        int nowYear = nowCal.get(Calendar.YEAR);
        int nowMonth = nowCal.get(Calendar.MONTH) + 1;

        DataGraphMonth dataGraphMonth = new DataGraphMonth(requireContext());
        try {
            dataGraphMonth.open();
            ConsuptionContainer container = dataGraphMonth.loadConsuptions(currentPoint);
            if (container != null) {
                List<ConsumptionForecastHelper.MonthlyRecord> monthlyRecords = new ArrayList<>();
                ArrayList<ConsuptionModel> monthlyList = container.monthlyConsuption();
                if (monthlyList != null && !monthlyList.isEmpty()) {
                    Calendar dateCal = Calendar.getInstance();
                    for (int i = 0; i < monthlyList.size(); i++) {
                        ConsuptionModel m = monthlyList.get(i);
                        int month = parseMonthNumber(m.getDateMonth());
                        if (month < 1 || month > 12) continue;
                        int year = m.getYearAsInt();
                        int daysInMonth = ConsumptionForecastHelper.getDaysInMonth(year, month);
                        int coveredDay = daysInMonth;
                        int spanDays = daysInMonth;

                        if (m instanceof MonthlyConsuptionModel mm) {
                            long lastDate = mm.getLastDate();
                            if (lastDate > 0L) {
                                dateCal.setTimeInMillis(lastDate);
                                coveredDay = dateCal.get(Calendar.DAY_OF_MONTH);
                                long startDate = mm.getStartDate();
                                if (startDate > 0L && lastDate >= startDate) {
                                    long diffDays = Math.round((lastDate - startDate) / 86_400_000.0);
                                    spanDays = Math.max(1, (int) diffDays);
                                } else {
                                    spanDays = coveredDay;
                                }
                            }
                        }

                        String label = extractShortMonthLabel(m.getDateMonthAsStringShort());
                        monthlyRecords.add(new ConsumptionForecastHelper.MonthlyRecord(
                                year,
                                month,
                                m.getConsuptionVT() != null ? m.getConsuptionVT() : 0.0,
                                m.getConsuptionNT() != null ? m.getConsuptionNT() : 0.0,
                                coveredDay,
                                spanDays,
                                label
                        ));
                    }

                    monthlyChartEntries.addAll(ConsumptionForecastHelper.buildMonthlyEntries(
                            monthlyRecords,
                            MAX_BAR_ENTRIES,
                            nowYear,
                            nowMonth,
                            distributionRate
                    ));
                }

                ArrayList<ConsuptionModel> yearlyList = container.yearConsuption();
                if (yearlyList != null && !yearlyList.isEmpty()) {
                    List<ConsumptionForecastHelper.YearlyRecord> yearlyRecords = createYearlyRecords(yearlyList);

                    yearlyChartEntries.addAll(ConsumptionForecastHelper.buildYearlyEntries(
                            yearlyRecords,
                            monthlyRecords,
                            MAX_BAR_ENTRIES,
                            nowYear,
                            nowMonth,
                            distributionRate
                    ));
                }
            }
        } catch (Exception ignored) {
        } finally {
            dataGraphMonth.close();
        }

        refreshConsumptionChartDisplay();
    }


    /**
     * Převede roční modely spotřeby {@link ConsuptionModel} na vstupní záznamy
     * {@link ConsumptionForecastHelper.YearlyRecord} pro sestavení sloupcového grafu a odhadu.
     *
     * @param yearlyList seznam ročních spotřeb z databáze
     * @return seznam převedených ročních záznamů
     */
    @NonNull
    private static List<ConsumptionForecastHelper.YearlyRecord> createYearlyRecords(ArrayList<ConsuptionModel> yearlyList) {
        List<ConsumptionForecastHelper.YearlyRecord> yearlyRecords = new ArrayList<>(yearlyList.size());
        for (int i = 0; i < yearlyList.size(); i++) {
            ConsuptionModel y = yearlyList.get(i);
            String label = y.getYearAsString() != null ? y.getYearAsString() : "";
            yearlyRecords.add(new ConsumptionForecastHelper.YearlyRecord(
                    y.getYearAsInt(),
                    y.getConsuptionVT() != null ? y.getConsuptionVT() : 0.0,
                    y.getConsuptionNT() != null ? y.getConsuptionNT() : 0.0,
                    label
            ));
        }
        return yearlyRecords;
    }


    /**
     * Převede textovou reprezentaci čísla měsíce na celé číslo.
     *
     * @param monthStr textové číslo měsíce
     * @return číslo měsíce, nebo {@code -1} při prázdném či neplatném vstupu
     */
    private int parseMonthNumber(@Nullable String monthStr) {
        if (monthStr == null || monthStr.trim().isEmpty()) {
            return -1;
        }
        try {
            return Integer.parseInt(monthStr.trim());
        } catch (NumberFormatException e) {
            return -1;
        }
    }


    /**
     * Extrahuje zkrácený popisek měsíce (část před první mezerou) pro osu X sloupcového grafu.
     *
     * @param shortDateMonth zkrácený textový popis měsíce (např. měsíc a rok)
     * @return krátký popisek měsíce, nebo prázdný řetězec
     */
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


    /**
     * Předá grafu {@link DashboardConsumptionBarChartView} měsíční nebo roční sadu položek
     * podle aktuálního stavu přepínače a zobrazí vysvětlení procentních změn, pokud je
     * v grafu zobrazena alespoň jedna procentní změna.
     */
    private void refreshConsumptionChartDisplay() {
        boolean isYearly = swConsumptionYearly.isChecked();
        List<DashboardConsumptionBarChartView.BarEntry> activeEntries =
                isYearly ? yearlyChartEntries : monthlyChartEntries;
        viewConsumptionBarChart.setEntries(activeEntries);

        if (tvConsumptionChangeNote != null) {
            boolean hasAnyPercent = false;
            for (DashboardConsumptionBarChartView.BarEntry entry : activeEntries) {
                if (entry.changePercent() != null) {
                    hasAnyPercent = true;
                    break;
                }
            }
            tvConsumptionChangeNote.setVisibility(hasAnyPercent ? View.VISIBLE : View.GONE);
        }
    }


    /**
     * Vyhodnotí barvu z aktuálního motivu podle atributu; pokud atribut v motivu neexistuje,
     * vrátí výchozí tmavou barvu {@code 0xFF1C1B1F}.
     *
     * @param attrRes ID atributu barvy v motivu
     * @return vyhodnocená barva ve formátu ARGB
     */
    private int resolveAttrColor(int attrRes) {
        return MaterialColors.getColor(requireContext(), attrRes, 0xFF1C1B1F);
    }


    /**
     * Nastaví titulek obrazovky v Toolbaru, aktualizuje odsazení podle navigace,
     * znovu načte data odběrných míst a spustí periodické obnovování stavu HDO každých 30 sekund.
     */
    @Override
    public void onResume() {
        super.onResume();
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).setToolbarTitle(getString(R.string.dashboard));
        }
        updatePadding();
        loadSubscriptionPointsAndRefresh();
        hdoHandler.removeCallbacks(hdoRefreshRunnable);
        hdoHandler.postDelayed(hdoRefreshRunnable, 30_000L);
    }


    /**
     * Zastaví periodické obnovování stavu HDO a zruší všechny běžící přechodové animace.
     */
    @Override
    public void onPause() {
        super.onPause();
        hdoHandler.removeCallbacks(hdoRefreshRunnable);
        if (priceValuesAnimator != null) {
            priceValuesAnimator.cancel();
            priceValuesAnimator = null;
        }
        if (periodValuesAnimator != null) {
            periodValuesAnimator.cancel();
            periodValuesAnimator = null;
        }
        if (billingValuesAnimator != null) {
            billingValuesAnimator.cancel();
            billingValuesAnimator = null;
        }
        if (tariffHighlightAnimator != null) {
            tariffHighlightAnimator.cancel();
            tariffHighlightAnimator = null;
        }
        if (hdoNextSwitchAnimator != null) {
            hdoNextSwitchAnimator.cancel();
            hdoNextSwitchAnimator = null;
        }
    }


    /**
     * Přepočítá a nastaví vnitřní odsazení posuvného kontejneru podle rozměrů horní lišty,
     * postranní nebo spodní navigace a systémových okrajů z {@link MainActivity}.
     */
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


    /**
     * Reaguje na změnu odsazení navigace nebo systémových lišt přepočtem odsazení obsahu.
     */
    @Override
    public void onPaddingChanged() {
        updatePadding();
    }


    /**
     * Neměnný souhrn spotřeby, záloh, celkových nákladů a typu tarifu pro jedno fakturační období.
     *
     * @param vtKwh          spotřeba ve vysokém tarifu (kWh)
     * @param ntKwh          spotřeba v nízkém tarifu (kWh)
     * @param advancesKc     součet uhrazených záloh (Kč)
     * @param costsWithTaxKc celkové náklady období včetně DPH po odečtení slev (Kč)
     * @param isSingleTariff {@code true}, pokud jde o jednosazbový tarif
     */
    private record PeriodSummary(double vtKwh, double ntKwh, double advancesKc,
                                 double costsWithTaxKc, boolean isSingleTariff) {

    }

}
