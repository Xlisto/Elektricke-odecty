package cz.xlisto.elektrodroid.ownview;


import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.text.DecimalFormat;

import cz.xlisto.elektrodroid.R;


/**
 * Vlastní {@link RelativeLayout} prvek pro vizuální porovnání dvou ceníků elektrické energie.
 * <p>
 * Zobrazuje název ceníkové položky, hodnoty z obou porovnávaných ceníků (vlevo a vpravo),
 * případný vypočtený rozdíl s rozlišením barev (úspora / navýšení) a volitelný spodní oddělovač.
 */
public class OwnPriceListCompare extends RelativeLayout {

    /**
     * Štítek pro název ceníkové položky
     */
    private TextView tvTitle;
    /**
     * Štítek pro hodnotu z prvního ceníku
     */
    private TextView tvLeft;
    /**
     * Štítek pro hodnotu z druhého ceníku
     */
    private TextView tvRight;
    /**
     * Štítek pro zobrazení vypočteného rozdílu z pohledu prvního ceníku
     */
    private TextView tvDifferentLeft;
    /**
     * Štítek pro zobrazení vypočteného rozdílu z pohledu druhého ceníku
     */
    private TextView tvDifferentRight;
    /**
     * Vizualizační oddělovač na spodním okraji prvku
     */
    private View separator;


    /**
     * Konstruktor pro programové vytvoření komponenty.
     *
     * @param context kontext aplikace
     */
    public OwnPriceListCompare(Context context) {
        super(context);
    }


    /**
     * Konstruktor pro vytvoření komponenty z XML layoutu se stylovatelnými atributy.
     *
     * @param context kontext aplikace
     * @param attrs   množina atributů předaná z XML layoutu
     */
    public OwnPriceListCompare(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }


    /**
     * Inicializuje komponentu, nafoukne layout a načte vlastní atributy z XML.
     *
     * @param context kontext aplikace
     * @param attrs   množina atributů předaná z XML layoutu
     */
    @SuppressLint("ResourceType")
    private void init(Context context, AttributeSet attrs) {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.own_view_compare_price_list_item, this);

        tvTitle = findViewById(R.id.tvTitleComparePriceList);
        tvLeft = findViewById(R.id.tvLeftComparePriceList);
        tvRight = findViewById(R.id.tvRightComparePriceList);
        tvDifferentLeft = findViewById(R.id.tvLeftDifferentComparePriceList);
        tvDifferentRight = findViewById(R.id.tvRightDifferentComparePriceList);
        separator = findViewById(R.id.separatorComparePriceList);

        TypedArray ta = getContext().obtainStyledAttributes(attrs, R.styleable.OwnPriceListCompare);
        try {
            String title = ta.getString(R.styleable.OwnPriceListCompare_title);
            boolean visibleSeparator = ta.getBoolean(R.styleable.OwnPriceListCompare_separator, true);
            boolean visibleDifferent = ta.getBoolean(R.styleable.OwnPriceListCompare_visibleDifferent, false);

            setTvTitle(title);
            visibleSeparator(visibleSeparator);
            visibleDifferent(visibleDifferent);
        } finally {
            ta.recycle();
        }
    }


    /**
     * Nastaví titulek ceníkové položky.
     *
     * @param s text titulku (pokud je {@code null}, text se nezmění)
     */
    public void setTvTitle(String s) {
        if (s != null)
            tvTitle.setText(s);
    }


    /**
     * Nastaví text s cenou nebo hodnotou pro levý štítek (první ceník).
     *
     * @param s text ke zobrazení
     */
    public void setLeft(String s) {
        tvLeft.setText(s);
    }


    /**
     * Nastaví text s cenou nebo hodnotou pro pravý štítek (druhý ceník).
     *
     * @param s text ke zobrazení
     */
    public void setRight(String s) {
        tvRight.setText(s);
    }


    /**
     * Sestaví a nastaví zobrazení výpočtu rozdílu ceníkových položek roznásobených množstvím (kWh nebo měsíce).
     * <p>
     * Pokud je rozdíl nebo množství nulové, sekce s rozdíly se automaticky skryje.
     * Texty rozdílů se barevně odliší (červená pro nárůst ceny, zelená pro pokles).
     *
     * @param d        rozdíl jednotkové ceny
     * @param df       formátovač pro zobrazení číselných a měnových hodnot
     * @param quantity množství (počet kWh nebo měsíců)
     * @param type     typ ceníkové položky dle {@link Type}
     */
    public void setDifferent(double d, DecimalFormat df, double quantity, Type type) {
        double nD = d * (-1);
        String left = df.format(d);
        String right = df.format(nD);
        if (quantity != 1 && (!type.equals(Type.VT_NT)) || quantity != 2 && (type.equals(Type.VT_NT))) {
            left = df.format(d) + " * " + quantity + " = " + df.format(d * quantity);
            right = df.format(nD) + " * " + quantity + " = " + df.format(nD * quantity);
        }

        visibleDifferent(true);
        if (d == 0 || quantity == 0)
            visibleDifferent(false);
        tvDifferentLeft.setText(left);
        tvDifferentRight.setText(right);

        setColorDifferentView(d);
    }


    /**
     * Nastaví zobrazení přímého rozdílu cen bez násobení množstvím.
     * <p>
     * Pokud je rozdíl nulový, sekce s rozdíly se skryje. Nastavené hodnoty se barevně odliší.
     *
     * @param d  rozdíl jednotkových cen
     * @param df formátovač pro zobrazení číselných a měnových hodnot
     */
    public void setDifferent(double d, DecimalFormat df) {
        double dif2 = d * (-1);
        visibleDifferent(true);
        if (d == 0)
            visibleDifferent(false);
        tvDifferentLeft.setText(df.format(d));
        tvDifferentRight.setText(df.format(dif2));
        setColorDifferentView(d);
    }


    /**
     * Nastaví barvu textu pro štítky s rozdílem v závislosti na znaménku hodnoty.
     * <p>
     * Pro {@code d > 0} je levá hodnota červená a pravá zelená.
     * Pro {@code d <= 0} je levá hodnota zelená a pravá červená.
     *
     * @param d hodnota rozdílu
     */
    private void setColorDifferentView(double d) {
        String cRed = "#e20707";
        String cGreen = "#00961d";
        if (d > 0) {
            tvDifferentLeft.setTextColor(Color.parseColor(cRed));
            tvDifferentRight.setTextColor(Color.parseColor(cGreen));
        } else {
            tvDifferentLeft.setTextColor(Color.parseColor(cGreen));
            tvDifferentRight.setTextColor(Color.parseColor(cRed));
        }
    }


    /**
     * Zobrazí nebo skryje vizuální oddělovač na spodním okraji prvku.
     *
     * @param b {@code true} pro zobrazení oddělovače ({@link View#VISIBLE}), {@code false} pro jeho skrytí ({@link View#GONE})
     */
    public void visibleSeparator(boolean b) {
        if (b)
            separator.setVisibility(VISIBLE);
        else separator.setVisibility(GONE);
    }


    /**
     * Zobrazí nebo skryje štítky s rozdíly cen.
     *
     * @param b {@code true} pro zobrazení štítků ({@link View#VISIBLE}), {@code false} pro jejich skrytí ({@link View#GONE})
     */
    public void visibleDifferent(boolean b) {
        if (b) {
            tvDifferentLeft.setVisibility(VISIBLE);
            tvDifferentRight.setVisibility(VISIBLE);
        } else {
            tvDifferentLeft.setVisibility(GONE);
            tvDifferentRight.setVisibility(GONE);
        }
    }


    /**
     * Typ ceníkové položky určující účel a způsob výpočtu rozdílu.
     */
    public enum Type {
        /** Vysoký tarif (VT) */
        VT,
        /**
         * Nízký tarif (NT)
         */
        NT,
        /** Kombinace vysokého a nízkého tarifu (VT + NT) */
        VT_NT,
        /** Měsíční platba */
        MONTH
    }

}
