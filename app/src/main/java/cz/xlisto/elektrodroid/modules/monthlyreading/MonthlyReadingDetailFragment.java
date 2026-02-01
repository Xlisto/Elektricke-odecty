package cz.xlisto.elektrodroid.modules.monthlyreading;


import static android.view.View.GONE;
import static android.view.View.VISIBLE;
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
import java.util.Arrays;
import android.util.Log;

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
 * Fragment zobrazující detail záznamu měsíčního odečtu energie a provádějící související cenové výpočty.
 * <p>
 * Účel:
 * - zobrazit informace o měsíčním odečtu (datums, start/end stavy, spotřeby),
 * - načíst příslušný ceník a subscription point, případně přepočítat regulovanou cenu,
 * - vytvořit tabulku položek s jednotkovými cenami a částkami (VT/NT, daně, služby, měsíční platby apod.),
 * - upravit velikost písma a rozložení tak, aby se texty vešly do dostupné šířky.
 * <p>
 * Hlavní odpovědnosti:
 * - správa UI prvků v detailech měsíčního odečtu,
 * - komunikace s ViewModel a lokálními datovými zdroji (`DataMonthlyReadingSource`, `DataPriceListSource`),
 * - provádění výpočtů přes utilitní třídy (`Calculation`, `PriceListRegulBuilder`).
 * <p>
 * Vedlejší účinky:
 * - mění stav `viewModel`u a interní pole (např. `priceArray`),
 * - modifikuje UI (přidávání řádků do `TableLayout`, nastavování textů a stylů),
 * - otevírá a zavírá datové zdroje.
 * <p>
 * Životní cyklus a omezení:
 * - vyžaduje platný kontext fragmentu (metody volat pouze pokud je fragment připojen),
 * - většina operací musí probíhat na hlavním (UI) vlákně — není thread-safe,
 * - očekává konzistentní data v `priceList` a `subscriptionPoint` (není plně validováno uvnitř metody).
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


    /**
     * Vytvoří novou instanci {@code MonthlyReadingDetailFragment} a uloží do ní
     * předané identifikátory a volbu zobrazení regulované ceny jako argumenty.
     * <p>
     * Postup:
     * - vytvoří {@link Bundle}, vloží do něj {@code idCurrent}, {@code idPrevious} a {@code showRegulPrice},
     * - vytvoří nový fragment, nastaví mu argumenty a vrátí instanci.
     * <p>
     * Vedlejší účinky:
     * - žádné globální; metoda vytváří a vrací nový fragment s nastavenými argumenty.
     * <p>
     * Požadavky a omezení:
     * - předané identifikátory by měly být platné pro pozdější načtení dat (např. {@code -1} může znamenat nepřítomný záznam),
     * - metoda je statická tovární metoda vhodná pro vytváření fragmentu s přednastavenými argumenty.
     *
     * @param idCurrent      identifikátor aktuálního měsíčního odečtu (např. z databáze)
     * @param idPrevious     identifikátor předchozího měsíčního odečtu
     * @param showRegulPrice pokud {@code true}, fragment bude při inicializaci žádat zobrazení regulované ceny
     * @return nově vytvořená a inicializovaná instance {@code MonthlyReadingDetailFragment}
     */
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
        // Pokud zatím nemáme načtené záznamy, pokusíme se je načíst.
        if (monthlyReadingCurrently == null || monthlyReadingPrevious == null) {
            loadMonthlyReading();
            if (monthlyReadingCurrently == null || monthlyReadingPrevious == null) {
                Log.w(TAG, "Monthly readings not available in onResume() - skipping UI update");
                return; // nic dál nezobrazujeme, aby nedošlo k NPE
            }
        }
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
            tvNTMetersStart.setVisibility(GONE);
            tvNTMetersEnd.setVisibility(GONE);
            tvNTConsuption.setVisibility(GONE);
            tvNT.setVisibility(GONE);
            tvNTDash.setVisibility(GONE);
            showedNt = false;
        } else {
            tvNTMetersStart.setVisibility(VISIBLE);
            tvNTMetersEnd.setVisibility(VISIBLE);
            tvNTConsuption.setVisibility(VISIBLE);
            tvNT.setVisibility(VISIBLE);
            tvNTDash.setVisibility(VISIBLE);
            showedNt = true;
        }

        // skrytí detailů, pokud je to záznam o výměně
        if (monthlyReadingCurrently.getFirst()) {
            rlPrice.setVisibility(GONE);
            rlConsuption.setVisibility(GONE);
            tvDateScope.setVisibility(GONE);
        } else {
            rlPrice.setVisibility(VISIBLE);
            rlConsuption.setVisibility(VISIBLE);
            tvDateScope.setVisibility(VISIBLE);
        }

        if (tlVt.getChildCount() == 0)
            tvsVt = createTable(tlVt, tableVt, priceArray, marginInPx, consuptionVt, consuptionNt, month);
    }


    /**
     * Načte měsíční odečet a příslušný ceník a uloží je do viewModelu a lokálních polí.
     * <p>
     * Postup:
     * - načte `SubscriptionPoint` z lokální konfigurace,
     * - otevře `DataMonthlyReadingSource` a načte záznamy pro `idCurrent` a `idPrevious`,
     * - uloží načtené záznamy do `viewModel`u,
     * - pokud existuje platný `priceListId`, otevře `DataPriceListSource` a načte `PriceList`,
     * - pokud je aktivní `showRegulPrice`, přepočte ceník přes `PriceListRegulBuilder`,
     * - zavře otevřené datové zdroje a nakonec zavolá `setPriceArray()` pro aktualizaci interního pole cen.
     * <p>
     * Vedlejší účinky:
     * - mění stav `viewModel`u (nastavuje subscriptionPoint, monthlyReadingCurrently, monthlyReadingPrevious a priceList),
     * - upravuje interní pole `priceArray`,
     * - otevírá a zavírá databázové zdroje.
     * <p>
     * Požadavky a omezení:
     * - používá `requireContext()` -> musí být volána, když je fragment připojen (např. v `onCreate`/`onViewCreated`),
     * - není thread-safe; očekává se volání na hlavním vlákně podle existující logiky fragmentu.
     */
    private void loadMonthlyReading() {
        subscriptionPoint = SubscriptionPoint.load(requireContext());
        // Ověříme, že subscriptionPoint existuje, protože dál používáme jeho hodnoty (např. getTableO()).
        if (subscriptionPoint == null) {
            Log.w(TAG, "SubscriptionPoint is null in loadMonthlyReading() - skipping DB reads");
            viewModel.setSubscriptionPoint(null);
            priceList = null;
            monthlyReadingCurrently = null;
            monthlyReadingPrevious = null;
            setPriceArray();
            return;
        }
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
     * Upravení velikosti písma v poli TextView tak, aby se texty vešly do dostupné šířky.
     * <p>
     * Postup:
     * - ověří, že fragment je připojen (`isAdded()`),
     * - spustí měření a nastavení velikosti písma pro pole `tvsConsuption` a `tvsVt`
     * voláním `onMeasureTextViews(...)`, které iterativně zvětšuje velikost písma
     * dokud součet šířek nevyhovuje šířce obrazovky.
     * <p>
     * Vedlejší účinky:
     * - mění velikost (`setTextSize`) jednotlivých TextView,
     * - používá a čte pole `screenWidth` a layouty `rlConsuption`/`rlPrice`.
     * <p>
     * Požadavky a omezení:
     * - musí být volána, když je fragment připojen a jsou dostupné rozměry (např. po layoutu),
     * - závisí na `requireContext()` uvnitř volaných metod -> volat na hlavním vlákně,
     * - není thread-safe.
     */
    private void resizeTextViews() {
        if (isAdded()) {
            // Zabezpečíme, že rodičovské layouty i pole TextView existují před měřením
            if (rlConsuption != null) {
                onMeasureTextViews(tvsConsuption, rlConsuption);
            }
            if (rlPrice != null) {
                onMeasureTextViews(tvsVt, rlPrice);
            }
        }
    }


    /**
     * Měří šířky textů v matici TextView a iterativně zvětšuje velikost písma,
     * dokud součet šířek sloupců nepřesáhne dostupnou šířku v rámci daného RelativeLayoutu.
     * <p>
     * Postup:
     * - zjistí marginy rodičovského RelativeLayoutu `rl`,
     * - opakovaně zvýší `textSize` a aplikuje jej na všechny nenulové TextView v `textViews`,
     * - pro každý sloupec spočítá maximální šířku mezi řádky pomocí `Paint.measureText`,
     * zohlední marginy jednotlivých TextView a tučný styl pro vybrané řádky,
     * - zastaví, když celková šířka přesáhne dostupnou šířku obrazovky (pole `screenWidth`).
     * <p>
     * Vedlejší účinky:
     * - upravuje velikost písma voláním `setTextSize` na prvcích `textViews`,
     * - používá `requireContext()` a `DensityUtils` (musí být volána na UI vlákně),
     * - čte hodnotu `screenWidth` z okolního fragmentu/instanční proměnné.
     * <p>
     * Požadavky a omezení:
     * - volat pouze na hlavním (UI) vlákně a když je fragment připojen (`isAdded()`),
     * - předpokládá, že `textViews` má konzistentní počet sloupců (`textViews[0].length`),
     * - některé položky v matici mohou být `null` (např. při skrytí NT sazeb) — metoda s tím počítá.
     *
     * @param textViews matice TextView kde první rozměr představuje řádky a druhý sloupce;
     *                  mohou se v ní vyskytovat `null` hodnoty, které jsou přeskočeny
     * @param rl        rodičovský RelativeLayout použitý pro získání marginů a určení dostupné šířky
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
     * Vytvoří řádky a buňky v zadaném TableLayoutu podle řetězců v tableString a hodnot v price.
     * <p>
     * Popis:
     * - projde pole `tableString` a pro každou položku vytvoří `TableRow` se třemi `TextView` (popis, jednotková cena, celkem),
     * - při neaktivním zobrazení nízkého tarifu (`showedNt == false`) přeskočí položky NT (indexy 6..10),
     * - vypočítá hodnotu "celkem" podle odpovídající ceny z pole `price`, použitím odpovídající spotřeby
     * (`consuptionVt`, `consuptionNt`, `month`) a interního násobitele,
     * - nastaví layout parametry, marginy, zarovnání a tučné písmo pro vybrané řádky,
     * - přidá oddělovací mezery po definovaných blocích řádků.
     * <p>
     * Vedlejší účinky:
     * - modifikuje `tl` (přidává řádků a `TextView`),
     * - vytváří a vrací matici `TextView[][]` obsahující odkazy na vytvořené buňky,
     * - používá `requireContext()` a `DensityUtils` (metodu volat pouze na UI vlákně).
     * <p>
     * Požadavky a omezení:
     * - metoda musí být volána na hlavním vlákně (UI) a fragment musí být připojen,
     * - očekává, že `price` má délku odpovídající položkám (metoda přistupuje k `price[i - 1]`),
     * - při skrytí NT mohou být některé řádky přeskočeny.
     *
     * @param tl           cílový TableLayout, do kterého se přidávají vytvořené řádky
     * @param tableString  pole názvů/popisů řádků (první prvek je hlavička)
     * @param price        pole cen odpovídající položkám (indexace `price[i - 1]` pro řádek `i`)
     * @param marginInPx   horizontální margin v pixelech použitý pro každý TextView
     * @param consuptionVt spotřeba pro VT použitá při výpočtu částek pro příslušné řádky
     * @param consuptionNt spotřeba pro NT použitá při výpočtu částek pro příslušné řádky
     * @param month        počet měsíců / měsíční množství použitý pro položky vykazované "za měsíc"
     * @return matice `TextView[][]` s rozměry `[tableString.length][3]` obsahující odkazy na vytvořené buňky (sloupce: 0=popis,1=jednotková cena,2=celkem)
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
     * Inicializuje interní pole `priceArray` na základě aktuálního `priceList`
     * a parametrů `subscriptionPoint`.
     * <p>
     * Postup:
     * - vypočítá jednorázovou cenu jističe voláním `Calculation.calculatePriceBreaker(...)`,
     * - vybere hodnotu pro "činnost" jako maximum mezi `priceList.getCinnost()` a `priceList.getOte()`,
     * - naplní pole `priceArray` hodnotami cen jednotlivých položek ve stejném pořadí,
     * jak je očekává `createTable(...)` (VT, NT, daně, služby, součty, měsíční platy atd.).
     * <p>
     * Vedlejší účinky:
     * - přepisuje pole `priceArray`.
     * <p>
     * Požadavky a omezení:
     * - očekává platný `priceList` a `subscriptionPoint` (není ověřováno v metodě),
     * - musí být volána na hlavním vlákně a když je fragment připojen (přístup přes `subscriptionPoint`).
     */
    private void setPriceArray() {
        // Ošetření situací, kdy zatím nejsou dostupná data — zabrání NPE v release buildu.
        if (priceList == null || subscriptionPoint == null) {
            // Pokud chybí ceník nebo informace o odběrném místě, nastavíme pole cen na nuly
            // tak, aby volající kód (UI) nezpůsobil NPE při renderování.
            priceArray = new double[15];
            Arrays.fill(priceArray, 0.0);
            return;
        }

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

        // Kontrola roku data aktuálního měsíčního odečtu a případná korekce položky "poze".
        // Pokud nemáme ještě načtený `monthlyReadingCurrently`, vynecháme kontrolu a ponecháme výchozí hodnotu (poze2).
        if (monthlyReadingCurrently != null) {
            Calendar calendarStart = Calendar.getInstance();
            calendarStart.setTimeInMillis(monthlyReadingCurrently.getDate());
            int year = calendarStart.get(Calendar.YEAR);
            if (year == 2026)
                priceArray[14] = priceList.getPoze1();
        }
    }


    /**
     * Nastaví zobrazení regulované/ne-regulované ceny a přepočítá zobrazené hodnoty.
     * <p>
     * Postup:
     * - aktualizuje hodnotu `showRegulPrice` ve `viewModel`u i lokální proměnné,
     * - vymaže existující řádky v `tlVt`,
     * - zavolá `loadMonthlyReading()` pro načtení/aktualizaci dat a `priceArray`,
     * - znovu vytvoří tabulku voláním `createTable(...)` a upraví velikosti textů voláním `resizeTextViews()`.
     * <p>
     * Vedlejší účinky:
     * - mění stav `viewModel`u,
     * - modifikuje UI (`tlVt`, `tvsVt`) a interní pole (`priceArray`),
     * - využívá `requireContext()` v podvolaných metodách.
     * <p>
     * Požadavky a omezení:
     * - volat na hlavním vlákně (UI) a když je fragment připojen,
     * - operace může být náročná při opakovaném volání (rekonstrukce tabulky).
     *
     * @param showRegulPrice true pokud má být zobrazena regulovaná cena, false jinak
     */
    public void setShowRegulPrice(boolean showRegulPrice) {
        viewModel.setShowRegulPrice(showRegulPrice);
        this.showRegulPrice = showRegulPrice;
        // Pokud view (tlVt) ještě není připravené, pouze aktualizujeme stav a načteme data.
        if (tlVt == null) {
            loadMonthlyReading();
            return;
        }

        tlVt.removeAllViews();
        loadMonthlyReading();

        // Vytvoření tabulky jen tehdy, když máme cenu pole a kontext
        if (priceArray != null) {
            tvsVt = createTable(tlVt, tableVt, priceArray, marginInPx, consuptionVt, consuptionNt, month);
            resizeTextViews();
        }
    }

}
