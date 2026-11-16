package cz.xlisto.elektrodroid.modules.pricelist;


import static androidx.annotation.Dimension.DP;

import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.materialswitch.MaterialSwitch;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import cz.xlisto.elektrodroid.R;
import cz.xlisto.elektrodroid.format.DecimalFormatHelper;
import cz.xlisto.elektrodroid.models.PriceListModel;
import cz.xlisto.elektrodroid.models.PriceListRegulBuilder;
import cz.xlisto.elektrodroid.utils.Calculation;
import cz.xlisto.elektrodroid.utils.DensityUtils;


/**
 * Kompaktní tabulkové zobrazení porovnání dvou ceníků elektřiny.
 * Vytváří dynamický {@link TableLayout} s rozepsáním jednotlivých složek cen, vyčíslením rozdílů
 * a celkovým součtem bez DPH i s DPH s automatickým přizpůsobením velikosti písma.
 */
public class PriceListCompareCompactFragment extends Fragment implements SelectedPriceListsInterface {

    private PriceListModel priceListLeft = new PriceListModel(""), priceListRight = new PriceListModel(""),
            priceListLeftRegul = new PriceListModel(""), priceListRightRegul = new PriceListModel("");
    private PriceListModel priceListLeftNERegul = new PriceListModel(""), priceListRightNERegul = new PriceListModel("");
    private PriceListRegulBuilder priceListLeftRegulBuilder, priceListRightRegulBuilder;
    private View view;
    private MaterialSwitch swLeft, swRight;
    private TextView tvDescription;
    private TableLayout tableLayout;
    private String[] left = new String[40];
    private String[] right = new String[40];
    private int[][] indexIdTable;
    private double vt = 1;
    private double nt = 1;
    private double month = 12;
    private double phase = 3;
    private double power = 25;
    private double servicesL = 0;
    private double servicesR = 0;
    private int screenWidth = 0;
    private final int[] titles = {R.string.produkt, R.string.dodavatel, R.string.distribucni_uzemi, R.string.neregul_vt, R.string.neregul_nt,
            R.string.mesicni_plat2, R.string.dan_elektriny, R.string.sazba_distribuce, R.string.regul_vt, R.string.regul_nt,
            R.string.jistic_do_10, R.string.jistic_nad_10, R.string.jistic_nad_16, R.string.jistic_nad_20, R.string.jistic_nad_25,
            R.string.jistic_nad_32, R.string.jistic_nad_40, R.string.jistic_nad_50, R.string.jistic_nad_63, R.string.jistic_nad_80,
            R.string.jistic_nad_100, R.string.jistic_nad_125, R.string.jistic_nad_160, R.string.jistic_nad_63_za_A, R.string.jistic_nad_25_za_A,
            R.string.systemove_sluzby, R.string.cinnost_operatora_trhu, R.string.poze_dle_jistice, R.string.poze_dle_spotreby, R.string.podpora_vykupu_el_z_oze_kvet_a_dz,
            R.string.cinnost_ote,
            R.string.celkem_vt, R.string.celkem_nt, R.string.mesicni_platy, R.string.poze_dle_jistice, R.string.poze_dle_spotreby, R.string.dalsi_sluzby,
            R.string.celkem_bez_dph, R.string.dph, R.string.celkem_s_dph
    };


    /**
     * Vytvoří novou instanci fragmentu {@link PriceListCompareCompactFragment}.
     *
     * @return nová instance {@link PriceListCompareCompactFragment}
     */
    public static PriceListCompareCompactFragment newInstance() {
        return new PriceListCompareCompactFragment();
    }


    /**
     * Inicializuje pomocné objekty pro práci s regulovanými cenami.
     */
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        priceListLeftRegulBuilder = new PriceListRegulBuilder(priceListLeftNERegul);
        priceListRightRegulBuilder = new PriceListRegulBuilder(priceListRightNERegul);
    }


    /**
     * Vytvoří zobrazení fragmentu, inicializuje přepínače regulovaných cen a vytvoří srovnávací tabulku.
     *
     * @param inflater           objekt pro nafouknutí rozvržení
     * @param container          kontejner rodičovského pohledu
     * @param savedInstanceState uložený stav fragmentu
     * @return nafouknutý pohled {@link View}
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_price_list_compare_compact, container, false);
        tableLayout = view.findViewById(R.id.tableLayout);
        swLeft = view.findViewById(R.id.swLeft);
        swRight = view.findViewById(R.id.swRight);
        tvDescription = view.findViewById(R.id.tvDescriptionPriceListCompareCompact);

        swLeft.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                priceListLeft = priceListLeftRegul;
            } else {
                priceListLeft = priceListLeftNERegul;
            }
            left = buildArray(priceListLeft);
            updateTable();
        });

        swRight.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                priceListRight = priceListRightRegul;
            } else {
                priceListRight = priceListRightNERegul;
            }
            right = buildArray(priceListRight);
            updateTable();
        });


        createTable();
        showTvDescription();
        return view;
    }


    /**
     * Zaregistruje posluchač rozvržení pro dynamické přizpůsobení velikosti písma v tabulce podle šířky obrazovky.
     *
     * @param view               pohled fragmentu
     * @param savedInstanceState uložený stav fragmentu
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // Zjištění šířky rodičovského view, po připojení fragmentu

        view.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                if (isAdded()) {
                    // Odstranění listeneru, aby se kód vykonal pouze jednou
                    view.getViewTreeObserver().removeOnGlobalLayoutListener(this);

                    // Nyní jsou rozměry k dispozici
                    screenWidth = view.getWidth() - view.getPaddingStart() - view.getPaddingEnd();

                    resizeTextViews();
                }
            }
        });

    }


    /**
     * Přepočítá a přizpůsobí velikost písma v tabulce při obnovení fragmentu.
     */
    @Override
    public void onResume() {
        super.onResume();
        resizeTextViews();
    }


    /**
     * Zobrazí popis, pokud je cena regulovaná
     */
    private void showTvDescription() {
        if (priceListLeftRegulBuilder.isRegulPrice() || priceListRightRegulBuilder.isRegulPrice()) {
            tvDescription.setVisibility(View.VISIBLE);
        } else {
            tvDescription.setVisibility(View.GONE);
        }
    }


    /**
     * Výpočet ceny spotřeby
     *
     * @param consumptionVt - spotřeba VT
     * @param consumptionNt - spotřeba NT
     * @param priceList     - ceník
     * @return - cena spotřeby
     */
    private double[] priceConsumption(double consumptionVt, double consumptionNt, PriceListModel priceList) {
        double priceVt = 0.0; //když je v ceníku prázdný místo, Double se načte jako 0.0, zde se provádí kontrola na druh ceníku
        double priceNt = 0.0;
        boolean isError = true;
        if ((priceList.getActivity() + priceList.getPoze1() + priceList.getPoze2()) == 0.0) {//pokud je starý ceník
            priceVt = (priceList.getPriceVT() + priceList.getTax() + priceList.getDistVT() + priceList.getSystemServices() + priceList.getOze() + priceList.getOte()) * consumptionVt;
            priceNt = (priceList.getPriceNT() + priceList.getTax() + priceList.getDistNT() + priceList.getSystemServices() + priceList.getOze() + priceList.getOte()) * consumptionNt;
            isError = false;
        }
        if ((priceList.getOze() + priceList.getOte()) == 0.0) {
            priceVt = (priceList.getPriceVT() + priceList.getTax() + priceList.getDistVT() + priceList.getSystemServices()) * consumptionVt;//zde u porovnávání poze2 nepřičítat, u odečtů naopak přičítat ke spotřebe
            priceNt = (priceList.getPriceNT() + priceList.getTax() + priceList.getDistNT() + priceList.getSystemServices()) * consumptionNt;
            isError = false;
        }
        if (isError) return new double[]{0.0, 0.0};
        return new double[]{priceVt, priceNt};
    }


    /**
     * Výpočet ceny za měsíce
     *
     * @param priceList ceník
     * @param phase     fáze
     * @param power     příkon
     * @param months    měsíce
     * @return cena za měsíce
     */
    private double paymentMonths(PriceListModel priceList, double phase, double power, double months) {
        double priceBreaker = Calculation.calculatePriceBreaker(priceList, (int) phase, (int) power);//cena jističe
        double priceMonth = priceBreaker + priceList.getActivity() + priceList.getMonthlyPayment();//cena jističe + cinnost + měsíční platba
        return priceMonth * months;//cena jističe + cinnost + měsíční platba * počet měsíců = celková cena za měsíce
    }


    /**
     * Výpočet POZE
     *
     * @param consumption - spotřeba
     * @param monthsCount - počet měsíců
     * @param phase       - fáze
     * @param power       - příkon
     * @param priceList   - ceník
     * @return - pole s výsledky POZE
     */
    private double[] getPOZE(double consumption, Double monthsCount, Double phase, Double power, PriceListModel priceList) {
        double[] result = {0.0, 0.0, 0.0};
        double p1 = consumption * priceList.getPoze2();//POZE dle spotřeby
        double p2 = (monthsCount * (phase * power)) * priceList.getPoze1();//POZE dle hodnoty jističe
        result[2] = Math.min(p1, p2);
        result[1] = p1;
        result[0] = p2;
        return result;
    }


    /**
     * Převezme vybrané ceníky a vstupní parametry a obnoví výstupní tabulku.
     *
     * @param priceListLeft  ceník na levé straně porovnání
     * @param priceListRight ceník na pravé straně porovnání
     * @param container      vstupní parametry spotřeby a služeb
     */
    @Override
    public void onPriceListsSelected(PriceListModel priceListLeft, PriceListModel priceListRight, ConsuptionContainer container) {
        if (priceListLeft != null) {
            this.priceListLeft = priceListLeft;
            this.priceListLeftNERegul = priceListLeft;
            this.priceListLeftRegulBuilder = new PriceListRegulBuilder(priceListLeftNERegul);
            this.priceListLeftRegul = priceListLeftRegulBuilder.getRegulPriceList();
            this.left = buildArray(priceListLeft);
            swLeft.setChecked(false);
            swLeft.setVisibility(priceListLeftRegulBuilder.isRegulPrice() ? View.VISIBLE : View.GONE);
        }
        if (priceListRight != null) {
            this.priceListRight = priceListRight;
            this.priceListRightNERegul = priceListRight;
            this.priceListRightRegulBuilder = new PriceListRegulBuilder(priceListRightNERegul);
            this.priceListRightRegul = priceListRightRegulBuilder.getRegulPriceList();
            this.right = buildArray(priceListRight);
            swRight.setChecked(false);
            swRight.setVisibility(priceListRightRegulBuilder.isRegulPrice() ? View.VISIBLE : View.GONE);
        }

        this.vt = container.vt();
        this.nt = container.nt();
        this.month = container.month();
        this.phase = container.phaze();
        this.power = container.power();
        this.servicesL = container.servicesL();
        this.servicesR = container.servicesR();
        updateTable();
        showTvDescription();
    }


    /**
     * Vytvoří a naplní dynamický {@link TableLayout} všemi řádky cenových položek, měrnými jednotkami
     * a sloupci pro levý ceník, pravý ceník a jejich vyčíslený rozdíl.
     */
    private void createTable() {
        String kcMonth = getResources().getString(R.string.kc_mesic);
        String kcMwh = getResources().getString(R.string.kc_MWh);
        String kcOmMonth = getResources().getString(R.string.kc_om);
        String kcAmMonth = getResources().getString(R.string.kc_1A);
        String kc = getResources().getString(R.string.kc);

        left = buildArray(priceListLeft);
        right = buildArray(priceListRight);
        indexIdTable = new int[titles.length][5];

        for (int i = 0; i < titles.length; i++) {
            TableRow row = new TableRow(requireContext());
            row.setLayoutParams(new TableLayout.LayoutParams(TableLayout.LayoutParams.WRAP_CONTENT, TableLayout.LayoutParams.WRAP_CONTENT));
            row.setId(View.generateViewId());
            for (int j = 0; j < 5; j++) {
                TextView tv = new TextView(requireContext());
                tv.setId(View.generateViewId());
                tv.setTextSize(DP, 24);
                if (j == 0) {
                    tv.setText(getResources().getString(titles[i]));
                } else if (j == 1) {//levá strana
                    tv.setText(left[i]);
                    tv.setGravity(Gravity.END);
                } else if (j == 2) {//pravá strana
                    tv.setText(right[i]);
                    tv.setGravity(Gravity.END);
                } else if (j == 3) {//rozdíl
                    tv.setGravity(Gravity.END);
                    if (i == 0)
                        tv.setText(getResources().getString(R.string.rozdil));
                    if (i == 1 || i == 2)
                        tv.setText("");
                } else {//měrné jednotky
                    if (i <= 2)
                        tv.setText("");
                    if (i == 3 || i == 4 || i == 6 || i == 8 || i == 9 || i == 25 || i == 28 || i == 29 || i == 30)
                        tv.setText(kcMwh);
                    if (i == 5 || (i >= 10 && i <= 24))
                        tv.setText(kcMonth);
                    if (i == 26)
                        tv.setText(kcOmMonth);
                    if (i == 27)
                        tv.setText(kcAmMonth);
                    if (i >= 31 && i <= 39)
                        tv.setText(kc);
                }
                indexIdTable[i][j] = tv.getId();
                if (i == 39)//tučné písmo
                    tv.setTypeface(tv.getTypeface(), Typeface.BOLD);
                int padding = DensityUtils.dpToPx(requireContext(), 5);
                tv.setPadding(padding, 5, padding, 5); // Malý padding pro lepší čitelnost

                row.addView(tv);

            }

            if (i == 31 || i == 37) {
                View line = new View(requireContext());
                line.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, 2)); // 2px vysoká čára
                line.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.color_axis)); // Nastavení barvy čáry
                tableLayout.addView(line);
            }
            tableLayout.addView(row);
        }

    }


    /**
     * Přepočítá výstupní hodnoty pro levou i pravou stranu, rozdíly a celkové ceny s/bez DPH
     * a aktualizuje texty v tabulce s automatickým skrytím řádků s nulovými cenami.
     */
    private void updateTable() {
        double[] consumptionLeft = priceConsumption(vt, nt, priceListLeft);
        double[] consumptionRight = priceConsumption(vt, nt, priceListRight);
        double paymentLeft = paymentMonths(priceListLeft, phase, power, month);
        double paymentRight = paymentMonths(priceListRight, phase, power, month);
        double[] pozeLeft = getPOZE(vt + nt, month, phase, power, priceListLeft);
        double[] pozeRight = getPOZE(vt + nt, month, phase, power, priceListRight);
        double totalWithoutVatLeft = consumptionLeft[0] + consumptionLeft[1] + paymentLeft + pozeLeft[2] + servicesL;
        double totalWithoutVatRight = consumptionRight[0] + consumptionRight[1] + paymentRight + pozeRight[2] + servicesR;
        double vatLeft = totalWithoutVatLeft * priceListLeft.getVat() / 100;
        double vatRight = totalWithoutVatRight * priceListRight.getVat() / 100;
        double totalWithVatLeft = totalWithoutVatLeft + vatLeft;
        double totalWithVatRight = totalWithoutVatRight + vatRight;
        for (int i = 0; i < indexIdTable.length; i++) {
            for (int j = 0; j < 5; j++) {
                TextView tv = view.findViewById(indexIdTable[i][j]);
                if (j == 1) {
                    tv.setText(left[i]);
                    if (i == 31) {//cena spotřeby VT
                        tv.setText(DecimalFormatHelper.df2.format(consumptionLeft[0]));
                    }
                    if (i == 32) {//cena spotřeby NT
                        tv.setText(DecimalFormatHelper.df2.format(consumptionLeft[1]));
                    }
                    if (i == 33) {//cena měsíční platby
                        tv.setText(DecimalFormatHelper.df2.format(paymentLeft));
                    }
                    if (i == 34) {//cena poze dle jističe
                        tv.setText(DecimalFormatHelper.df2.format(pozeLeft[0]));
                    }
                    if (i == 35) {//cena poze dle spotřeby
                        tv.setText(DecimalFormatHelper.df2.format(pozeLeft[1]));
                    }
                    if (i == 36) {//cena dalších služeb
                        tv.setText(DecimalFormatHelper.df2.format(servicesL));
                    }
                    if (i == 37) {//cena celkem bez DPH
                        tv.setText(DecimalFormatHelper.df2.format(totalWithoutVatLeft));
                    }
                    if (i == 38) {//cena DPH
                        tv.setText(DecimalFormatHelper.df2.format(vatLeft));
                    }
                    if (i == 39) {//cena celkem s DPH
                        tv.setText(DecimalFormatHelper.df2.format(totalWithVatLeft));
                    }

                }
                if (j == 2) {
                    tv.setText(right[i]);
                    if (i == 31) {//cena spotřeby VT
                        tv.setText(DecimalFormatHelper.df2.format(consumptionRight[0]));
                    }
                    if (i == 32) {//cena spotřeby NT
                        tv.setText(DecimalFormatHelper.df2.format(consumptionRight[1]));
                    }
                    if (i == 33) {//cena měsíční platby
                        tv.setText(DecimalFormatHelper.df2.format(paymentRight));
                    }
                    if (i == 34) {//cena poze dle jističe
                        tv.setText(DecimalFormatHelper.df2.format(pozeRight[0]));
                    }
                    if (i == 35) {//cena poze dle spotřeby
                        tv.setText(DecimalFormatHelper.df2.format(pozeRight[1]));
                    }
                    if (i == 36) {//cena dalších služeb
                        tv.setText(DecimalFormatHelper.df2.format(servicesR));
                    }
                    if (i == 37) {//cena celkem bez DPH
                        tv.setText(DecimalFormatHelper.df2.format(totalWithoutVatRight));
                    }
                    if (i == 38) {//cena DPH
                        tv.setText(DecimalFormatHelper.df2.format(vatRight));
                    }
                    if (i == 39) {//cena celkem s DPH
                        tv.setText(DecimalFormatHelper.df2.format(totalWithVatRight));
                    }
                }
                if (j == 3) {
                    if (isNumeric(left[i]) && isNumeric(right[i])) {
                        tv.setText(DecimalFormatHelper.df2.format((parseDouble(left[i]) - parseDouble(right[i]))));
                        if (i == 31) {//rozdíl ceny VT
                            tv.setText(DecimalFormatHelper.df2.format(consumptionLeft[0] - consumptionRight[0]));
                        }
                        if (i == 32) {//rozdíl ceny NT
                            tv.setText(DecimalFormatHelper.df2.format(consumptionLeft[1] - consumptionRight[1]));
                        }
                        if (i == 33) {//rozdíl ceny měsíční platby
                            tv.setText(DecimalFormatHelper.df2.format(paymentLeft - paymentRight));
                        }
                        if (i == 34) {//rozdíl ceny poze dle jističe
                            tv.setText(DecimalFormatHelper.df2.format(pozeLeft[0] - pozeRight[0]));
                        }
                        if (i == 35) {//rozdíl ceny poze dle spotřeby
                            tv.setText(DecimalFormatHelper.df2.format(pozeLeft[1] - pozeRight[1]));
                        }
                        if (i == 36) {//rozdíl ceny dalších služeb
                            tv.setText(DecimalFormatHelper.df2.format(servicesL - servicesR));
                        }
                        if (i == 37) {//rozdíl ceny celkem bez DPH
                            tv.setText(DecimalFormatHelper.df2.format(totalWithoutVatLeft - totalWithoutVatRight));
                        }
                        if (i == 38) {//rozdíl ceny DPH
                            tv.setText(DecimalFormatHelper.df2.format(vatLeft - vatRight));
                        }
                        if (i == 39) {//rozdíl ceny celkem s DPH
                            tv.setText(DecimalFormatHelper.df2.format(totalWithVatLeft - totalWithVatRight));
                        }
                    }
                }

            }
        }

        //cyklus pro skrytí prvků, pokud je cena 0,00 ve všech sloupcích
        for (int[] ints : indexIdTable) {
            boolean hideAll = true; // Předpokládáme, že všechny prvky budou skryté, dokud nenajdeme důkaz o opaku
            TextView[] textViews = new TextView[5];

            // Předem uložíme všechny TextView prvky do pole pro snadnější přístup
            for (int j = 0; j < 5; j++) {
                textViews[j] = view.findViewById(ints[j]);
            }

            // Kontrola, zda text ve vybraných TextView prvcích odpovídá "0,00"
            for (int j = 1; j <= 3; j++) {
                if (!textViews[j].getText().toString().equals("0,00")) {
                    hideAll = false; // Pokud alespoň jeden prvek neodpovídá, nebudou všechny prvky skryty
                    break; // Už není potřeba kontrolovat další prvky
                }
            }

            // Nastavení viditelnosti všech TextView prvků na základě výsledku kontroly
            for (TextView tv : textViews) {
                tv.setVisibility(hideAll ? View.GONE : View.VISIBLE);
            }
        }
    }


    /**
     * Vytvoří pole pro výpis ceníku
     *
     * @param priceList - ceník
     * @return - pole pro výpis ceníku
     */
    private String[] buildArray(PriceListModel priceList) {
        return new String[]{priceList.getProduct(), priceList.getCompany(), priceList.getDistribution(), DecimalFormatHelper.df2.format(priceList.getPriceVT()), DecimalFormatHelper.df2.format(priceList.getPriceNT()),
                DecimalFormatHelper.df2.format(priceList.getMonthlyPayment()), DecimalFormatHelper.df2.format(priceList.getTax()), priceList.getRate(), DecimalFormatHelper.df2.format(priceList.getDistVT()), DecimalFormatHelper.df2.format(priceList.getDistNT()),
                DecimalFormatHelper.df2.format(priceList.getJ0()), DecimalFormatHelper.df2.format(priceList.getJ1()), DecimalFormatHelper.df2.format(priceList.getJ2()), DecimalFormatHelper.df2.format(priceList.getJ3()), DecimalFormatHelper.df2.format(priceList.getJ4()),
                DecimalFormatHelper.df2.format(priceList.getJ5()), DecimalFormatHelper.df2.format(priceList.getJ6()), DecimalFormatHelper.df2.format(priceList.getJ7()), DecimalFormatHelper.df2.format(priceList.getJ14()), DecimalFormatHelper.df2.format(priceList.getJ13()),
                DecimalFormatHelper.df2.format(priceList.getJ10()), DecimalFormatHelper.df2.format(priceList.getJ11()), DecimalFormatHelper.df2.format(priceList.getJ12()), DecimalFormatHelper.df2.format(priceList.getJ8()), DecimalFormatHelper.df2.format(priceList.getJ9()),
                DecimalFormatHelper.df2.format(priceList.getSystemServices()), DecimalFormatHelper.df2.format(priceList.getActivity()), DecimalFormatHelper.df2.format(priceList.getPoze1()), DecimalFormatHelper.df2.format(priceList.getPoze2()), DecimalFormatHelper.df2.format(priceList.getOze()),
                DecimalFormatHelper.df2.format(priceList.getOte()),
                "0", "0", "0", "0", "0", "0", "0", "0", "0"
        };
    }


    /**
     * Kontrola, zda-li se jedná o číslo
     *
     * @param str - vstupní řetězec
     * @return - true, pokud se jedná o číslo
     */
    public boolean isNumeric(String str) {
        if (!str.matches("^-?\\d+([,.]\\d+)?$")) {
            return false;
        }

        try {
            String normalized = str.replace(',', '.');
            Double.parseDouble(normalized);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }


    /**
     * Převede řetězec na číslo
     *
     * @param str - vstupní řetězec
     * @return - číslo
     */
    private double parseDouble(String str) {
        try {
            String normalized = str.replace(',', '.');
            return Double.parseDouble(normalized);
        } catch (NumberFormatException e) {
            return 0;
        }
    }


    /**
     * V cyklu spustí měření šířky textů podle nastavení velikosti písma
     */
    private void resizeTextViews() {
        if (isAdded()) {
            int textSize = 5;
            int width = 0;

            while (width < screenWidth) {
                textSize++;
                width = onMeasureTextViews(textSize);
            }
        }
    }


    /**
     * Změří šířku textů podle nastavení velikosti písma
     *
     * @param textSize - velikost písma
     * @return - šířka textů (šířka nejširších sloupců)
     */
    private int onMeasureTextViews(int textSize) {
        float[] width = new float[5];

        for (int i = 0; i < 5; i++) {
            float maxRowWidth = 0;
            for (int j = 0; j < titles.length; j++) {
                TextView tv = view.findViewById(indexIdTable[j][i]);
                tv.setTextSize(DP, textSize);
                int padding = tv.getPaddingEnd() + tv.getPaddingStart();

                Paint paint = new Paint();
                paint.setTextSize(textSize + 1);
                tv.getFontFeatureSettings();

                float textWidth = paint.measureText(tv.getText().toString() + DensityUtils.dpToPx(requireContext(), padding));

                if (maxRowWidth < textWidth) {
                    maxRowWidth = textWidth;
                }
            }
            width[i] = maxRowWidth;//maximální šířka sloupce
        }

        float totalWidth = 0;
        for (float columnWidth : width) {
            totalWidth += columnWidth;//součet všech šířek sloupců
        }
        return (int) totalWidth;
    }
}
