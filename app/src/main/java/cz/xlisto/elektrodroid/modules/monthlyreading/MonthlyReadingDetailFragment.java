package cz.xlisto.elektrodroid.modules.monthlyreading;


import static androidx.annotation.Dimension.DP;
import static cz.xlisto.elektrodroid.format.DecimalFormatHelper.df2;

import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.RelativeLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import java.util.Calendar;

import cz.xlisto.elektrodroid.R;
import cz.xlisto.elektrodroid.databaze.DataMonthlyReadingSource;
import cz.xlisto.elektrodroid.databaze.DataPriceListSource;
import cz.xlisto.elektrodroid.models.MonthlyReadingModel;
import cz.xlisto.elektrodroid.models.PriceListModel;
import cz.xlisto.elektrodroid.models.PriceListRegulBuilder;
import cz.xlisto.elektrodroid.models.SubscriptionPointModel;
import cz.xlisto.elektrodroid.ownview.ViewHelper;
import cz.xlisto.elektrodroid.utils.Calculation;
import cz.xlisto.elektrodroid.utils.DensityUtils;
import cz.xlisto.elektrodroid.utils.DifferenceDate;
import cz.xlisto.elektrodroid.utils.SubscriptionPoint;


/**
 * Zobrazí detail záznamu měsíčního odečtu
 * Xlisto 17.03.2024 18:34
 */
public class MonthlyReadingDetailFragment extends Fragment {

    public static final String TAG = "MonthlyReadingDetailFragment";
    private static final String ARG_ID_CURRENT = "id_current";
    private static final String ARG_ID_PREVIOUS = "id_previous";
    private static final String ARG_SHOW_REGUL_PRICE = "showRegulPrice";
    private long idCurrent, idPrevious;
    private MonthlyReadingModel monthlyReadingCurrently, monthlyReadingPrevious;
    private PriceListModel priceList;
    private SubscriptionPointModel subscriptionPoint;
    private MonthlyReadingDetailViewModel viewModel;
    private TextView tvDate;
    private TextView tvDateScope;
    private TextView tvNT;
    private TextView tvNTDash;
    private TextView tvVTMetersStart;
    private TextView tvNTMetersStart;
    private TextView tvVTMetersEnd;
    private TextView tvNTMetersEnd;
    private TextView tvVTConsuption;
    private TextView tvNTConsuption;
    private TableLayout tlVt;
    private RelativeLayout rlConsuption, rlPrice;
    private int screenWidth = 0;
    private final TextView[][] tvsConsuption = new TextView[2][5];
    private TextView[][] tvsVt = new TextView[15][3];
    private String[] tableVt;
    private double[] priceArray;
    private boolean showRegulPrice;
    private double month;
    private double consuptionVt;
    private double consuptionNt;
    private int marginInPx;
    private boolean showedNt = true;


    public static MonthlyReadingDetailFragment newInstance(long idCurrent, long idPrevious, boolean showRegulPrice) {

        Bundle args = new Bundle();
        args.putLong(ARG_ID_CURRENT, idCurrent);
        args.putLong(ARG_ID_PREVIOUS, idPrevious);
        args.putBoolean(ARG_SHOW_REGUL_PRICE, showRegulPrice);
        MonthlyReadingDetailFragment fragment = new MonthlyReadingDetailFragment();
        fragment.setArguments(args);
        return fragment;
    }


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = new ViewModelProvider(this).get(MonthlyReadingDetailViewModel.class);
        if (getArguments() != null) {
            idCurrent = getArguments().getLong(ARG_ID_CURRENT);
            idPrevious = getArguments().getLong(ARG_ID_PREVIOUS);
            showRegulPrice = getArguments().getBoolean(ARG_SHOW_REGUL_PRICE);
            viewModel.setShowRegulPrice(showRegulPrice);
        }

        //obnovení z viewModelu, pokud bude null, provede se načítání z databáze
        monthlyReadingCurrently = viewModel.getMonthlyReadingCurrently().getValue();
        monthlyReadingPrevious = viewModel.getMonthlyReadingPrevious().getValue();
        priceList = viewModel.getPriceList().getValue();
        subscriptionPoint = viewModel.getSubscriptionPoint().getValue();
        showRegulPrice = Boolean.TRUE.equals(viewModel.getShowRegulPrice().getValue());

        if (monthlyReadingCurrently == null) {
            loadMonthlyReading();
        }

        tableVt = new String[]{
                getResources().getString(R.string.polozka),
                getResources().getString(R.string.neregul_vt),
                getResources().getString(R.string.regul_vt),
                getResources().getString(R.string.dan_z_elektriny),
                getResources().getString(R.string.systemove_sluzby),
                getResources().getString(R.string.celkem_vt),
                getResources().getString(R.string.neregul_nt),
                getResources().getString(R.string.regul_nt),
                getResources().getString(R.string.dan_z_elektriny),
                getResources().getString(R.string.systemove_sluzby),
                getResources().getString(R.string.celkem_nt),
                getResources().getString(R.string.mesicni_platy),
                getResources().getString(R.string.mesicni_plat_za_jistic),
                getResources().getString(R.string.provoz_nesitove_infrastruktury),
                getResources().getString(R.string.celkem),
                getResources().getString(R.string.poze_dle_spotreby)

        };

        setPriceArray();

    }


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_monthly_reading_detail, container, false);
    }


    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        tvDate = view.findViewById(R.id.tvDateMonthDetail);
        tvDateScope = view.findViewById(R.id.tvDateScopeMonthDetail);
        TextView tvVT = view.findViewById(R.id.tvVTMonthDetail);
        tvNT = view.findViewById(R.id.tvNTMonthDetail);
        tvVTMetersStart = view.findViewById(R.id.tvVTMetersStartMonthDetail);
        tvNTMetersStart = view.findViewById(R.id.tvNTMetersStartMonthDetail);
        TextView tvVTDash = view.findViewById(R.id.tvVTMetersDashMonthDetail);
        tvNTDash = view.findViewById(R.id.tvNTMetersDashMonthDetail);
        tvVTMetersEnd = view.findViewById(R.id.tvVTMetersEndMonthDetail);
        tvNTMetersEnd = view.findViewById(R.id.tvNTMetersEndMonthDetail);
        tvVTConsuption = view.findViewById(R.id.tvVTConsuptionMonthDetail);
        tvNTConsuption = view.findViewById(R.id.tvNTConsuptionMonthDetail);
        tlVt = view.findViewById(R.id.tvTableVTMonthDetail);
        rlConsuption = view.findViewById(R.id.rlConsuptionMonthDetail);
        rlPrice = view.findViewById(R.id.rlPriceMonthDetail);

        tvsConsuption[0] = new TextView[]{tvVT, tvVTMetersStart, tvVTDash, tvVTMetersEnd, tvVTConsuption};
        tvsConsuption[1] = new TextView[]{tvNT, tvNTMetersStart, tvNTDash, tvNTMetersEnd, tvNTConsuption};

        view.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                if (isAdded()) {
                    // Odstranění posluchače, aby se kód provedl pouze jednou
                    view.getViewTreeObserver().removeOnGlobalLayoutListener(this);

                    // Nyní jsou rozměry k dispozici
                    screenWidth = view.getWidth() - view.getPaddingStart() - view.getPaddingEnd();

                    resizeTextViews();
                }
            }
        });
    }


    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putLong(ARG_ID_CURRENT, idCurrent);
    }


    @Override
    public void onResume() {
        super.onResume();
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(monthlyReadingCurrently.getDate());
        calendar.add(Calendar.DAY_OF_MONTH, -1);
        String dateFrom = ViewHelper.convertLongToDate(monthlyReadingPrevious.getDate());
        String dateTo = ViewHelper.convertLongToDate(calendar.getTimeInMillis());
        month = Calculation.differentMonth(monthlyReadingPrevious.getDate(), monthlyReadingCurrently.getDate(), DifferenceDate.TypeDate.MONTH);

        //nastavení pro zobrazení staršího popisu "Činnost operátora trhu"
        calendar = Calendar.getInstance();
        calendar.set(Calendar.YEAR, 2024);
        calendar.set(Calendar.MONTH, Calendar.JULY);
        calendar.set(Calendar.DAY_OF_MONTH, 30);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        if (calendar.getTimeInMillis() > (monthlyReadingCurrently.getDate())) {
            tableVt[13] = getResources().getString(R.string.cinnost_operatora_trhu);
        }

        tvDate.setText(String.valueOf(ViewHelper.convertLongToDate(monthlyReadingCurrently.getDate())));
        tvDateScope.setText(String.format(getResources().getString(R.string.string_dash_string) + " (" + month + ")", dateFrom, dateTo));

        tvVTMetersStart.setText(df2.format(monthlyReadingPrevious.getVt()));
        tvNTMetersStart.setText(df2.format(monthlyReadingPrevious.getNt()));

        tvVTMetersEnd.setText(df2.format(monthlyReadingCurrently.getVt()));
        tvVTMetersEnd.setText(String.format(getResources().getString(R.string.consuption2), monthlyReadingCurrently.getVt()));
        tvNTMetersEnd.setText(String.format(getResources().getString(R.string.consuption2), monthlyReadingCurrently.getNt()));

        consuptionVt = monthlyReadingCurrently.getVt() - monthlyReadingPrevious.getVt();
        consuptionNt = monthlyReadingCurrently.getNt() - monthlyReadingPrevious.getNt();

        tvVTConsuption.setText(String.format(getResources().getString(R.string.consuption2), consuptionVt));
        tvNTConsuption.setText(String.format(getResources().getString(R.string.consuption2), consuptionNt));

        marginInPx = DensityUtils.dpToPx(requireContext(), 5);

        //skrytí NT sazeb, pokud není použita
        if (monthlyReadingPrevious.getNt() == 0 && monthlyReadingCurrently.getNt() == 0) {
            tvNTMetersStart.setVisibility(View.GONE);
            tvNTMetersEnd.setVisibility(View.GONE);
            tvNTConsuption.setVisibility(View.GONE);
            tvNT.setVisibility(View.GONE);
            tvNTDash.setVisibility(View.GONE);
            showedNt = false;
        } else {
            tvNTMetersStart.setVisibility(View.VISIBLE);
            tvNTMetersEnd.setVisibility(View.VISIBLE);
            tvNTConsuption.setVisibility(View.VISIBLE);
            tvNT.setVisibility(View.VISIBLE);
            tvNTDash.setVisibility(View.VISIBLE);
            showedNt = true;
        }

        if (tlVt.getChildCount() == 0)
            tvsVt = createTable(tlVt, tableVt, priceArray, marginInPx, consuptionVt, consuptionNt, month);
    }


    /**
     * Načte měsíční odečet a příslušný ceník
     */
    private void loadMonthlyReading() {
        subscriptionPoint = SubscriptionPoint.load(requireContext());
        viewModel.setSubscriptionPoint(subscriptionPoint);

        DataMonthlyReadingSource monthlyReadingSource = new DataMonthlyReadingSource(requireContext());
        monthlyReadingSource.open();
        monthlyReadingCurrently = monthlyReadingSource.loadMonthlyReadingById(subscriptionPoint.getTableO(), idCurrent);
        monthlyReadingPrevious = monthlyReadingSource.loadMonthlyReadingById(subscriptionPoint.getTableO(), idPrevious);
        viewModel.setMonthlyReadingCurrently(monthlyReadingCurrently);
        viewModel.setMonthlyReadingPrevious(monthlyReadingPrevious);
        monthlyReadingSource.close();

        if (monthlyReadingCurrently != null && monthlyReadingCurrently.getPriceListId() != -1L) {
            DataPriceListSource priceListSource = new DataPriceListSource(requireContext());
            priceListSource.open();
            priceList = priceListSource.readPrice(monthlyReadingCurrently.getPriceListId());

            if (showRegulPrice)
                priceList = new PriceListRegulBuilder(priceList, monthlyReadingPrevious).getRegulPriceList();

            viewModel.setPriceList(priceList);
            priceListSource.close();
        }
        setPriceArray();
    }


    /**
     * V cyklu spustí měření šířky textů podle nastavení velikosti písma
     */
    private void resizeTextViews() {
        if (isAdded()) {
            onMeasureTextViews(tvsConsuption, rlConsuption);
            onMeasureTextViews(tvsVt, rlPrice);
        }
    }


    /**
     * Měří šířku textů a zvětší písmo tak, aby se texty vešly do šířky
     *
     * @param textViews TextView[][] s textViews
     * @param rl        RelativeLayout
     */
    private void onMeasureTextViews(TextView[][] textViews, RelativeLayout rl) {
        float totalWidth = 0;
        int textSize = 20;
        ViewGroup.MarginLayoutParams layoutParamsRl = (ViewGroup.MarginLayoutParams) rl.getLayoutParams();

        int marginStartRl = layoutParamsRl.getMarginStart();
        int marginEndRl = layoutParamsRl.getMarginEnd();

        while (totalWidth < (screenWidth - marginStartRl - marginEndRl)) {

            //nastavení velikosti
            for (TextView[] tvs : textViews) {
                for (TextView tv : tvs) {
                    if (tv != null)//nutné při skrytí NT sazeb
                        tv.setTextSize(DP, textSize);
                }
            }

            textSize++;

            int columnCount = textViews[0].length;
            totalWidth = 0;
            Paint paintNormal = new Paint();
            Paint paintBold = new Paint();
            for (int i = 0; i < columnCount; i++) {

                paintNormal.setTextSize(textSize);
                paintBold.setTypeface(Typeface.DEFAULT_BOLD);
                paintBold.setTextSize(textSize);

                float width;
                float maxWidth = 0;
                for (int j = 0; j < textViews.length; j++) {
                    TextView[] tvs = textViews[j];
                    StringBuilder text = new StringBuilder();
                    if (tvs[i] == null) //nutné při skrytí NT sazeb
                        continue;
                    text.append(tvs[i].getText().toString());
                    TableRow.LayoutParams layoutParamsTableRow = (TableRow.LayoutParams) tvs[i].getLayoutParams();
                    int marginStart = layoutParamsTableRow.getMarginStart();
                    int marginEnd = layoutParamsTableRow.getMarginEnd();
                    int margin = marginStart + marginEnd;
                    if (j == 4 || j == 9 || j == 13)
                        width = paintBold.measureText(text.toString()) + margin;
                    else
                        width = paintNormal.measureText(text.toString()) + margin;

                    maxWidth = Math.max(maxWidth, width);
                }
                totalWidth += maxWidth;
            }
            totalWidth += DensityUtils.dpToPx(requireContext(), 10);
        }
    }


    /**
     * Vytvoří tabulku
     *
     * @param tl           TableLayout
     * @param tableString  String[] s texty jednotlivých položek ceny
     * @param price        double[] s cenami jednotlivých položek ceny
     * @param marginInPx   int s marginem v PX
     * @param consuptionVt double s spotřebou VT
     * @param consuptionNt double s spotřebou NT
     * @param month        double s měsícem
     * @return TextView[][] s textViews v tableLayoutu
     */
    private TextView[][] createTable(TableLayout tl, String[] tableString, double[] price, int marginInPx, double consuptionVt, double consuptionNt, double month) {
        TextView[][] tvs = new TextView[tableString.length][3];
        int multiplier = 1000;
        for (int i = 0; i < tableString.length; i++) {
            //přeskočení řádků, které se nezobrazují nízský tarif, pokud není žádná spotřeba a stav je 0
            if (!showedNt && i >= 6 && i <= 10) {
                continue;
            }

            double consuption = 0;
            if (i <= 5)
                consuption = consuptionVt;
            else if (i <= 10)
                consuption = consuptionNt;
            else if (i <= 14) {
                consuption = month;
                multiplier = 1;
            } else if (i == 15) {
                consuption = consuptionVt + consuptionNt;
                multiplier = 1000;
            }

            TableRow row = new TableRow(requireContext());
            row.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.WRAP_CONTENT));
            row.setId(View.generateViewId());
            for (int j = 0; j < 3; j++) {
                TextView tv = new TextView(requireContext());
                TableRow.LayoutParams layoutParams = new TableRow.LayoutParams(
                        TableRow.LayoutParams.WRAP_CONTENT,
                        TableRow.LayoutParams.WRAP_CONTENT
                );
                layoutParams.setMargins(marginInPx, 0, marginInPx, 0); // Nastavení marginů: start, top, end, bottom
                tv.setLayoutParams(layoutParams);
                tv.setId(View.generateViewId());
                if (i == 0) {//hlavička tabulky
                    tv.setGravity(Gravity.CENTER);
                    if (j == 0)
                        tv.setText(tableString[i]);
                    else if (j == 1)
                        tv.setText(getString(R.string.jed_cena));
                    else tv.setText(getString(R.string.celkem));
                } else {//těloo tabulky
                    if (j == 0)
                        tv.setText(tableString[i]);
                    else if (j == 1) {
                        String unit = getResources().getString(R.string.mwh);
                        if (i == 11 || i == 12 || i == 13 || i == 14)
                            unit = getResources().getString(R.string.mes);
                        tv.setText(getResources().getString(R.string.formatted_price, df2.format(price[i - 1]), unit));
                        tv.setGravity(Gravity.END); // Přidání gravity na konec
                    } else {
                        tv.setText(String.format(getResources().getString(R.string.string_price), df2.format(price[i - 1] * consuption / multiplier)));
                        tv.setGravity(Gravity.END); // Přidání gravity na konec
                    }
                }
                if (i == 0 || i == 5 || i == 10 || i == 14 || i == 15)
                    tv.setTypeface(Typeface.DEFAULT_BOLD);
                row.addView(tv);
                tvs[i][j] = tv;
            }
            tl.addView(row);

            // Přidání mezery po specifických řádcích
            if (i == 5 || i == 10 || i == 14) {
                TableRow spaceRow = new TableRow(requireContext());
                TableRow.LayoutParams spaceLayoutParams = new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, DensityUtils.dpToPx(requireContext(), 800)); // Převod DP na PX pro výšku
                spaceRow.setLayoutParams(spaceLayoutParams);
                TextView tv = new TextView(requireContext());
                spaceRow.addView(tv);
                tl.addView(spaceRow);
            }
        }
        return tvs;
    }


    /**
     * Nastaví pole cen
     */
    private void setPriceArray() {
        double jistic = Calculation.calculatePriceBreaker(priceList, subscriptionPoint.getCountPhaze(), subscriptionPoint.getPhaze());
        double cinnost = Math.max(priceList.getCinnost(), priceList.getOte());

        priceArray = new double[]{
                priceList.getCenaVT(),
                priceList.getDistVT(),
                priceList.getDan(),
                priceList.getSystemSluzby(),
                priceList.getCenaVT() + priceList.getDistVT() + priceList.getDan() + priceList.getSystemSluzby(),
                priceList.getCenaNT(),
                priceList.getDistNT(),
                priceList.getDan(),
                priceList.getSystemSluzby(),
                priceList.getCenaNT() + priceList.getDistNT() + priceList.getDan() + priceList.getSystemSluzby(),
                priceList.getMesicniPlat(),
                jistic,
                cinnost,
                priceList.getMesicniPlat() + jistic + cinnost,
                priceList.getPoze2()
        };
    }


    /**
     * Nastaví zobrazení při změně ne/regulované ceny
     *
     * @param showRegulPrice boolean s hodnotou zobrazení regulované ceny
     */
    public void setShowRegulPrice(boolean showRegulPrice) {
        viewModel.setShowRegulPrice(showRegulPrice);
        this.showRegulPrice = showRegulPrice;
        tlVt.removeAllViews();
        loadMonthlyReading();

        tvsVt = createTable(tlVt, tableVt, priceArray, marginInPx, consuptionVt, consuptionNt, month);
        resizeTextViews();
    }
}
