package cz.xlisto.odecty.ownview;

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

import cz.xlisto.odecty.R;

public class OwnPriceListCompare extends RelativeLayout {
    private TextView tvTitle, tvLeft, tvRigth, tvDifferentLeft, tvDifferentRight;
    private View separator;

    public OwnPriceListCompare(Context context) {
        super(context);
    }

    public OwnPriceListCompare(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    @SuppressLint("ResourceType")
    private void init(Context context, AttributeSet attrs) {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.own_view_compare_price_list_item, this);

        tvTitle = findViewById(R.id.tvTitleComparePriceList);
        tvLeft = findViewById(R.id.tvLeftComparePriceList);
        tvRigth = findViewById(R.id.tvRightComparePriceList);
        tvDifferentLeft = findViewById(R.id.tvLeftDifferentComparePriceList);
        tvDifferentRight = findViewById(R.id.tvRightDifferentComparePriceList);
        separator = findViewById(R.id.separatorComparePriceList);

        TypedArray ta = getContext().obtainStyledAttributes(attrs, R.styleable.OwnPriceListCompare);
        String title = ta.getString(R.styleable.OwnPriceListCompare_title);
        boolean visibleSeparator = ta.getBoolean(R.styleable.OwnPriceListCompare_separator, true);
        boolean visibleDifferent = ta.getBoolean(R.styleable.OwnPriceListCompare_visibleDifferent, false);

        setTvTitle(title);
        visibleSeparator(visibleSeparator);
        visibleDifferent(visibleDifferent);
    }

    /**
     * Nastaví titulek
     * @param s
     */
    public void setTvTitle(String s) {
        if (s != null)
            tvTitle.setText(s);
    }

    /**
     * Nastaví text na levý textview s cenou
     * @param s
     */
    public void setLeft(String s) {
        tvLeft.setText(s);
    }

    /**
     * Nastaví text na pravý textview s cenou
     * @param s
     */
    public void setRight(String s) {
        tvRigth.setText(s);
    }

    /**
     * Sestaví výpočet rodízlu dílčí ceny podle množství kwh nebo měsíců a mnastaví jej na textview
     * @param d
     * @param df
     * @param quantity
     * @param type
     */
    public void setDifferent(double d, DecimalFormat df, double quantity, Type type) {
        double nD = d * (-1);
        String left = df.format(d);
        String right = df.format(nD);
        if (quantity != 1 && (!type.equals(Type.VT_NT)) ||  quantity != 2 && (type.equals(Type.VT_NT))) {
            left = df.format(d) + " * " + quantity + " = " + df.format(d * quantity);
            right = df.format(nD) + " * " + quantity + " = " + df.format(nD * quantity);
        }

        visibleDifferent(true);
        if(d==0 || quantity==0)
            visibleDifferent(false);
        tvDifferentLeft.setText(left);
        tvDifferentRight.setText(right);

        setColorDifferentView(d);
    }

    /**
     * Nastaví do textview rozdílnou cenu, pokud rozdíl není, textview skryje
     * @param d
     * @param df
     */
    public void setDifferent(double d,DecimalFormat df) {
        double dif1 = d;
        double dif2 = d * (-1);
        visibleDifferent(true);
        if(d==0)
            visibleDifferent(false);
        tvDifferentLeft.setText(df.format(dif1));
        tvDifferentRight.setText(df.format(dif2));
        setColorDifferentView(dif1);
    }

    /**
     * Nastaví textview rozdílnou barvu podle kladné/záporné hodnoty
     * @param d
     */
    private void setColorDifferentView(double d){
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
     * Zobrazí/skryje oddělovač obsažený na konci view
     * @param b
     */
    public void visibleSeparator(boolean b) {
        if (b)
            separator.setVisibility(VISIBLE);
        else separator.setVisibility(GONE);
    }

    /**
     * Zobrazí/skryje textview s rozdíly cen
     * @param b
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
     * Typ podle ceníkové jednotky
     */
    public enum Type {
        VT,
        NT,
        VT_NT,
        MONTH
    }
}
