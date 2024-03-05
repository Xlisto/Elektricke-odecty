package cz.xlisto.odecty.modules.pricelist;

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
import androidx.fragment.app.Fragment;
import cz.xlisto.odecty.R;
import cz.xlisto.odecty.format.DecimalFormatHelper;
import cz.xlisto.odecty.models.PriceListModel;
import cz.xlisto.odecty.models.PriceListRegulBuilder;
import cz.xlisto.odecty.utils.DensityUtils;

import static androidx.annotation.Dimension.DP;


/**
 * Xlisto 01.03.2024 18:01
 */
public class PriceListCompareCompactFragment extends Fragment implements SelectedPriceListsInterface {
    private static final String TAG = "PriceListCompareCompactFragment";

    private PriceListModel priceListLeft = new PriceListModel(""), priceListRight = new PriceListModel(""), priceListLeftRegul, priceListRightRegul;
    private PriceListModel priceListLeftNERegul, priceListRightNERegul;
    private PriceListRegulBuilder priceListLeftRegulBuilder, priceListRightRegulBuilder;
    private View view;
    private TableLayout tableLayout;
    private String[] left = new String[40];
    private String[] right = new String[40];
    private int[][] indexIdTable;
    private double vt = 1;
    private double nt = 1;
    private double month = 12;
    private double phaze = 3;
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


    public static PriceListCompareCompactFragment newInstance() {
        return new PriceListCompareCompactFragment();
    }


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_price_list_compare_compact, container, false);
        tableLayout = view.findViewById(R.id.tableLayout);

        createTable();
        return view;
    }


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


    @Override
    public void onResume() {
        super.onResume();
        resizeTextViews();
    }


    private double[] vypocetSpotreby(double spotrebaVt, double spotrebaNt, PriceListModel pr) {
        double vysledekVt = 0.0; //když je v ceníku prázdný místo, Double se načte jako 0.0, zde se provádí kontrola na druh ceníku
        double vysledekNt = 0.0;
        boolean chyba = true;
        if ((pr.getCinnost() + pr.getPoze1() + pr.getPoze2()) == 0.0) {//pokud je starý ceník
            vysledekVt = (pr.getCenaVT() + pr.getDan() + pr.getDistVT() + pr.getSystemSluzby() + pr.getOze() + pr.getOte()) * spotrebaVt;
            vysledekNt = (pr.getCenaNT() + pr.getDan() + pr.getDistNT() + pr.getSystemSluzby() + pr.getOze() + pr.getOte()) * spotrebaNt;
            chyba = false;
        }
        if ((pr.getOze() + pr.getOte()) == 0.0) {
            vysledekVt = (pr.getCenaVT() + pr.getDan() + pr.getDistVT() + pr.getSystemSluzby()) * spotrebaVt;//zde u porovnávání poze2 nepřičítat, u odečtů naopak přičítat ke spotřebe
            vysledekNt = (pr.getCenaNT() + pr.getDan() + pr.getDistNT() + pr.getSystemSluzby()) * spotrebaNt;
            chyba = false;
        }
        if (chyba) return new double[]{0.0, 0.0};
        return new double[]{vysledekVt, vysledekNt};
    }


    private Double vypocetMesicniPlatby(PriceListModel pr, double faze, double prikon, double pocetMesicu) {
        double vysledek;
        double cenaZaJistic = 0.0;
        double pocetAmperuNavic;
        pocetMesicu = zaokrouhli(pocetMesicu);//zaokrouhlení na dvě desetinná místa
        if (faze == 1.0) {
            if (prikon <= 25) {
                cenaZaJistic = pr.getJ0();
            } else {
                pocetAmperuNavic = prikon % 25;//Zbytek po dělení - příklad Fáze 1x30 tj. 30/25 = 1 a zbytek 5 (výsledek), výpočet ceny sazba za 1x25A+(počet Amperu navíc*cena)
                cenaZaJistic = pr.getJ0() + (pocetAmperuNavic * pr.getJ9());
            }
        }
        if (faze == 3.0) {
            if (prikon <= 10) {
                cenaZaJistic = pr.getJ0();
            }
            if ((prikon > 10) && (prikon <= 16)) {
                cenaZaJistic = pr.getJ1();
            }
            if ((prikon > 16) && (prikon <= 20)) {
                cenaZaJistic = pr.getJ2();
            }
            if ((prikon > 20) && (prikon <= 25)) {
                cenaZaJistic = pr.getJ3();
            }
            if ((prikon > 25) && (prikon <= 32)) {
                cenaZaJistic = pr.getJ4();
            }
            if ((prikon > 32) && (prikon <= 40)) {
                cenaZaJistic = pr.getJ5();
            }
            if ((prikon > 40) && (prikon <= 50)) {
                cenaZaJistic = pr.getJ6();
            }
            if ((prikon > 50) && (prikon <= 63)) {
                cenaZaJistic = pr.getJ7();
            }
            if (pr.getJ8() == 0.0) {
                if ((prikon > 63) && (prikon <= 80)) {
                    cenaZaJistic = pr.getJ10();
                }
                if ((prikon > 80) && (prikon <= 100)) {
                    cenaZaJistic = pr.getJ11();
                }
                if ((prikon > 100) && (prikon <= 125)) {
                    cenaZaJistic = pr.getJ12();
                }
                if ((prikon > 125) && (prikon <= 160)) {
                    cenaZaJistic = pr.getJ13();
                }
                if (prikon > 160) {
                    pocetAmperuNavic = prikon - 160;
                    cenaZaJistic = pr.getJ13() + (pocetAmperuNavic * pr.getJ14());
                }
            } else {
                if (prikon > 63) {
                    pocetAmperuNavic = prikon - 63;
                    cenaZaJistic = pr.getJ7() + (pocetAmperuNavic * pr.getJ8());
                }
            }

        }
        if (pr.getCinnost() == 0.0) {//starý ceník
            vysledek = (pr.getMesicniPlat() + cenaZaJistic) * pocetMesicu;
        } else {//nový ceník
            vysledek = (pr.getMesicniPlat() + cenaZaJistic + pr.getCinnost()) * pocetMesicu;
        }
        return vysledek;
    }


    private double[] vypocetPOZE(double spotreba, Double pocetMesicu, Double faze, Double prikon, PriceListModel pr) {
        double[] vysledek = {0.0, 0.0, 0.0};
        double p1 = spotreba * pr.getPoze2();//POZE dle spotřeby
        double p2 = (pocetMesicu * (faze * prikon)) * pr.getPoze1();//POZE dle hodnoty jističe
        vysledek[2] = Math.min(p1, p2);
        vysledek[1] = p1;
        vysledek[0] = p2;
        return vysledek;
    }


    private double zaokrouhli(double d) {//zaokrouhlování na 3 desetinná místa
        d = (d + 0.0005) * 1000.0;
        d = (int) d;
        d = d / 1000.0;
        return d;
    }


    @Override
    public void onPriceListsSelected(PriceListModel priceListLeft, PriceListModel priceListRight, PriceListCompareBoxFragment.ConsuptionContainer container) {
        if (priceListLeft != null) {
            this.priceListLeft = priceListLeft;
            this.priceListLeftNERegul = priceListLeft;
            this.priceListLeftRegulBuilder = new PriceListRegulBuilder(priceListLeftNERegul);
            this.left = buildArray(priceListLeft);


        }
        if (priceListRight != null) {
            this.priceListRight = priceListRight;
            this.priceListRightNERegul = priceListRight;
            this.priceListRightRegulBuilder = new PriceListRegulBuilder(priceListRightNERegul);
            this.right = buildArray(priceListRight);
        }
        this.vt = container.vt;
        this.nt = container.nt;
        this.month = container.month;
        this.phaze = container.phaze;
        this.power = container.power;
        this.servicesL = container.servicesL;
        this.servicesR = container.servicesR;
        updateTable();
        onResume();
    }


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
                    if (i == 3 || i == 4 || i == 6 || i == 8 || i == 9 || i == 25 || i == 28 || i == 29 || i == 30 || i == 31 || i == 32 || i == 35)
                        tv.setText(kcMwh);
                    if (i == 5 || i >= 10 && i <= 24 || i == 33 || i == 36)
                        tv.setText(kcMonth);
                    if (i == 26)
                        tv.setText(kcOmMonth);
                    if (i == 27 || i == 34)
                        tv.setText(kcAmMonth);
                    if (i == 37 || i == 38 || i == 39)
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
                line.setBackgroundColor(getResources().getColor(R.color.color_axis)); // Nastavení barvy čáry
                tableLayout.addView(line);
            }
            tableLayout.addView(row);
        }

    }


    private void updateTable() {

        double[] spotrebaLeft = vypocetSpotreby(vt, nt, priceListLeft);
        double[] spotrebaRight = vypocetSpotreby(vt, nt, priceListRight);
        double platbaLeft = vypocetMesicniPlatby(priceListLeft, phaze, power, month);
        double platbaRight = vypocetMesicniPlatby(priceListRight, phaze, power, month);
        double[] pozeLeft = vypocetPOZE(vt + nt, month, phaze, power, priceListLeft);
        double[] pozeRight = vypocetPOZE(vt + nt, month, phaze, power, priceListRight);
        double celkemBezDphLeft = spotrebaLeft[0] + spotrebaLeft[1] + platbaLeft + pozeLeft[2] + servicesL;
        double celkemBezDphRight = spotrebaRight[0] + spotrebaRight[1] + platbaRight + pozeRight[2] + servicesR;
        double dphLeft = celkemBezDphLeft * priceListLeft.getDph() / 100;
        double dphRight = celkemBezDphRight * priceListRight.getDph() / 100;
        double celkemSDphLeft = celkemBezDphLeft + dphLeft;
        double celkemSDphRight = celkemBezDphRight + dphRight;
        for (int i = 0; i < indexIdTable.length; i++) {
            for (int j = 0; j < 5; j++) {
                TextView tv = view.findViewById(indexIdTable[i][j]);
                if (j == 1) {
                    tv.setText(left[i]);
                    if (i == 31) {//cena spotřeby VT
                        tv.setText(DecimalFormatHelper.df2.format(spotrebaLeft[0]));
                    }
                    if (i == 32) {//cena spotřeby NT
                        tv.setText(DecimalFormatHelper.df2.format(spotrebaLeft[1]));
                    }
                    if (i == 33) {//cena měsíční platby
                        tv.setText(DecimalFormatHelper.df2.format(platbaLeft));
                    }
                    if (i == 34) {
                        tv.setText(DecimalFormatHelper.df2.format(pozeLeft[0]));
                    }
                    if (i == 35) {
                        tv.setText(DecimalFormatHelper.df2.format(pozeLeft[1]));
                    }
                    if (i == 36) {
                        tv.setText(DecimalFormatHelper.df2.format(servicesL));
                    }
                    if (i == 37) {
                        tv.setText(DecimalFormatHelper.df2.format(celkemBezDphLeft));
                    }
                    if (i == 38) {
                        tv.setText(DecimalFormatHelper.df2.format(dphLeft));
                    }
                    if (i == 39) {
                        tv.setText(DecimalFormatHelper.df2.format(celkemSDphLeft));
                    }

                }
                if (j == 2) {
                    tv.setText(right[i]);
                    if (i == 31) {//cena spotřeby VT
                        tv.setText(DecimalFormatHelper.df2.format(spotrebaRight[0]));
                    }
                    if (i == 32) {//cena spotřeby NT
                        tv.setText(DecimalFormatHelper.df2.format(spotrebaRight[1]));
                    }
                    if (i == 33) {//cena měsíční platby
                        tv.setText(DecimalFormatHelper.df2.format(platbaRight));
                    }
                    if (i == 34) {
                        tv.setText(DecimalFormatHelper.df2.format(pozeRight[0]));
                    }
                    if (i == 35) {
                        tv.setText(DecimalFormatHelper.df2.format(pozeRight[1]));
                    }
                    if (i == 36) {
                        tv.setText(DecimalFormatHelper.df2.format(servicesR));
                    }
                    if (i == 37) {
                        tv.setText(DecimalFormatHelper.df2.format(celkemBezDphRight));
                    }
                    if (i == 38) {
                        tv.setText(DecimalFormatHelper.df2.format(dphRight));
                    }
                    if (i == 39) {
                        tv.setText(DecimalFormatHelper.df2.format(celkemSDphRight));
                    }
                }
                if (j == 3) {
                    if (isNumeric(left[i]) && isNumeric(right[i])) {
                        tv.setText(DecimalFormatHelper.df2.format((parseDouble(left[i]) - parseDouble(right[i]))));
                        if (i == 31) {//rozdíl ceny VT
                            tv.setText(DecimalFormatHelper.df2.format(spotrebaLeft[0] - spotrebaRight[0]));
                        }
                        if (i == 32) {//rozdíl ceny NT
                            tv.setText(DecimalFormatHelper.df2.format(spotrebaLeft[1] - spotrebaRight[1]));
                        }
                        if (i == 33) {//rozdíl ceny měsíční platby
                            tv.setText(DecimalFormatHelper.df2.format(platbaLeft - platbaRight));
                        }
                        if (i == 34) {
                            tv.setText(DecimalFormatHelper.df2.format(pozeLeft[0] - pozeRight[0]));
                        }
                        if (i == 35) {
                            tv.setText(DecimalFormatHelper.df2.format(pozeLeft[1] - pozeRight[1]));
                        }
                        if (i == 36) {
                            tv.setText(DecimalFormatHelper.df2.format(servicesL - servicesR));
                        }
                        if (i == 37) {
                            tv.setText(DecimalFormatHelper.df2.format(celkemBezDphLeft - celkemBezDphRight));
                        }
                        if (i == 38) {
                            tv.setText(DecimalFormatHelper.df2.format(dphLeft - dphRight));
                        }
                        if (i == 39) {
                            tv.setText(DecimalFormatHelper.df2.format(celkemSDphLeft - celkemSDphRight));
                        }
                    }
                }

            }
        }
        for (int[] ints : indexIdTable) {
            boolean[] b = new boolean[3];
            for (int j = 0; j < 5; j++) {
                TextView tv = view.findViewById(ints[j]);

                if (j >= 1 && j <= 3) {
                    b[j - 1] = tv.getText().toString().equals("0,00");
                }
            }
            if (b[0] && b[1] && b[2]) {
                for (int j = 0; j < 5; j++) {
                    TextView tv = view.findViewById(ints[j]);
                    tv.setVisibility(View.GONE);
                }
            }
        }
    }


    /**
     * Vytvoří pole pro výpis ceníku
     *
     * @param pr - ceník
     * @return - pole pro výpis ceníku
     */
    private String[] buildArray(PriceListModel pr) {
        return new String[]{pr.getProdukt(), pr.getFirma(), pr.getDistribuce(), DecimalFormatHelper.df2.format(pr.getCenaVT()), DecimalFormatHelper.df2.format(pr.getCenaNT()),
                DecimalFormatHelper.df2.format(pr.getMesicniPlat()), DecimalFormatHelper.df2.format(pr.getDan()), pr.getSazba(), DecimalFormatHelper.df2.format(pr.getDistVT()), DecimalFormatHelper.df2.format(pr.getDistNT()),
                DecimalFormatHelper.df2.format(pr.getJ0()), DecimalFormatHelper.df2.format(pr.getJ1()), DecimalFormatHelper.df2.format(pr.getJ2()), DecimalFormatHelper.df2.format(pr.getJ3()), DecimalFormatHelper.df2.format(pr.getJ4()),
                DecimalFormatHelper.df2.format(pr.getJ5()), DecimalFormatHelper.df2.format(pr.getJ6()), DecimalFormatHelper.df2.format(pr.getJ7()), DecimalFormatHelper.df2.format(pr.getJ14()), DecimalFormatHelper.df2.format(pr.getJ13()),
                DecimalFormatHelper.df2.format(pr.getJ10()), DecimalFormatHelper.df2.format(pr.getJ11()), DecimalFormatHelper.df2.format(pr.getJ12()), DecimalFormatHelper.df2.format(pr.getJ8()), DecimalFormatHelper.df2.format(pr.getJ9()),
                DecimalFormatHelper.df2.format(pr.getSystemSluzby()), DecimalFormatHelper.df2.format(pr.getCinnost()), DecimalFormatHelper.df2.format(pr.getPoze1()), DecimalFormatHelper.df2.format(pr.getPoze2()), DecimalFormatHelper.df2.format(pr.getOze()),
                DecimalFormatHelper.df2.format(pr.getOte()),
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
     * v cyklu spustí měření šířky textů podle nastavení velikosti písma
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
            float sizeRow = 0;
            for (int j = 0; j < titles.length; j++) {
                TextView tv = view.findViewById(indexIdTable[j][i]);
                tv.setTextSize(DP, textSize);
                int padding = tv.getPaddingEnd() + tv.getPaddingStart();

                Paint paint = new Paint();
                paint.setTextSize(textSize + 1);
                tv.getFontFeatureSettings();

                float textWidth = paint.measureText(tv.getText().toString() + DensityUtils.dpToPx(requireContext(), padding));

                if (sizeRow < textWidth) {
                    sizeRow = textWidth;
                }
            }
            width[i] = sizeRow;//maximální šířka sloupce
        }

        float totalWidth = 0;
        for (float v : width) {
            totalWidth += v;//součet všech šířekk sloupců
        }
        return (int) totalWidth;
    }
}
