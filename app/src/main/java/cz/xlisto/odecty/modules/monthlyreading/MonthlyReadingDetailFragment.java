package cz.xlisto.odecty.modules.monthlyreading;

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

import java.util.Calendar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import cz.xlisto.odecty.R;
import cz.xlisto.odecty.databaze.DataMonthlyReadingSource;
import cz.xlisto.odecty.databaze.DataPriceListSource;
import cz.xlisto.odecty.format.DecimalFormatHelper;
import cz.xlisto.odecty.models.MonthlyReadingModel;
import cz.xlisto.odecty.models.PriceListModel;
import cz.xlisto.odecty.models.PriceListRegulBuilder;
import cz.xlisto.odecty.models.SubscriptionPointModel;
import cz.xlisto.odecty.ownview.ViewHelper;
import cz.xlisto.odecty.utils.Calculation;
import cz.xlisto.odecty.utils.DensityUtils;
import cz.xlisto.odecty.utils.DifferenceDate;
import cz.xlisto.odecty.utils.SubscriptionPoint;

import static androidx.annotation.Dimension.DP;


/**
 * Zobrazí detail záznamu měsíčního odečtu
 * Xlisto 17.03.2024 18:34
 */
public class MonthlyReadingDetailFragment extends Fragment {
    private static final String TAG = "MonthlyReadingDetailFragment";
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
    private TextView[][] tvsVt = new TextView[14][3];
    private String[] tableVt;
    private double[] priceVt;
    private boolean showRegulPrice;


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
                getResources().getString(R.string.cinnost_operatora_trhu),
                getResources().getString(R.string.celkem),
                getResources().getString(R.string.poze_dle_spotreby)

        };

        double jistic = Calculation.calculatePriceBreaker(priceList, subscriptionPoint.getCountPhaze(), subscriptionPoint.getPhaze());
        double cinnost = Math.max(priceList.getCinnost(), priceList.getOte());

        priceVt = new double[]{
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
        TextView tvNT = view.findViewById(R.id.tvNTMonthDetail);
        tvVTMetersStart = view.findViewById(R.id.tvVTMetersStartMonthDetail);
        tvNTMetersStart = view.findViewById(R.id.tvNTMetersStartMonthDetail);
        TextView tvVTDash = view.findViewById(R.id.tvVTMetersDashMonthDetail);
        TextView tvNTDash = view.findViewById(R.id.tvNTMetersDashMonthDetail);
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
        double month = Calculation.differentMonth(monthlyReadingPrevious.getDate(), monthlyReadingCurrently.getDate(), DifferenceDate.TypeDate.MONTH);


        tvDate.setText(String.valueOf(ViewHelper.convertLongToDate(monthlyReadingCurrently.getDate())));
        tvDateScope.setText(String.format(getResources().getString(R.string.string_dash_string) + " (" + month + ")", dateFrom, dateTo));

        tvVTMetersStart.setText(DecimalFormatHelper.df2.format(monthlyReadingPrevious.getVt()));
        tvNTMetersStart.setText(DecimalFormatHelper.df2.format(monthlyReadingPrevious.getNt()));

        tvVTMetersEnd.setText(DecimalFormatHelper.df2.format(monthlyReadingCurrently.getVt()));
        tvVTMetersEnd.setText(String.format(getResources().getString(R.string.consuption2), monthlyReadingCurrently.getVt()));
        tvNTMetersEnd.setText(String.format(getResources().getString(R.string.consuption2), monthlyReadingCurrently.getNt()));

        double consuptionVt = monthlyReadingCurrently.getVt() - monthlyReadingPrevious.getVt();
        double consuptionNt = monthlyReadingCurrently.getNt() - monthlyReadingPrevious.getNt();

        tvVTConsuption.setText(String.format(getResources().getString(R.string.consuption2), consuptionVt));
        tvNTConsuption.setText(String.format(getResources().getString(R.string.consuption2), consuptionNt));

        int marginInPx = DensityUtils.dpToPx(requireContext(), 5);

        if (tlVt.getChildCount() == 0)
            tvsVt = createTable(tlVt, tableVt, priceVt, marginInPx, consuptionVt, consuptionNt, month);
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
            double consuption = 0;
            if (i <= 4)
                consuption = consuptionVt;
            else if (i <= 9)
                consuption = consuptionNt;
            else if (i <= 13) {
                consuption = month;
                multiplier = 1;
            } else if (i == 14) {
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
                if (j == 0)
                    tv.setText(tableString[i]);
                else if (j == 1) {
                    tv.setText(DecimalFormatHelper.df2.format(price[i]));
                    tv.setGravity(Gravity.END); // Přidání gravity na konec
                } else {
                    tv.setText(String.format(getResources().getString(R.string.string_price), DecimalFormatHelper.df2.format(price[i] * consuption / multiplier)));
                    tv.setGravity(Gravity.END); // Přidání gravity na konec
                }
                if (i == 4 || i == 9 || i == 13 || i == 14)
                    tv.setTypeface(Typeface.DEFAULT_BOLD);
                row.addView(tv);
                tvs[i][j] = tv;
            }
            tl.addView(row);

            // Přidání mezery po specifických řádcích
            if (i == 4 || i == 9 || i == 13) {
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
}
