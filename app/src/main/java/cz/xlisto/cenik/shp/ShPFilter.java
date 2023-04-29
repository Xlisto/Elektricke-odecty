package cz.xlisto.cenik.shp;

import android.content.Context;

import cz.xlisto.cenik.shp.ShP;

public class ShPFilter extends ShP {

    public static final String RADA = "filtrRada";
    public static final String PRODUKT = "filtrProdukt";
    public static final String SAZBA = "filtrSazba";
    public static final String DODAVATEL = "filtrDodavatel";
    public static final String UZEMI = "filtrUzemi";
    public static final String DATUM = "filtrDatum";
    public static final String DEFAULT = "%";
    public static final String POSITION = "position";

    public ShPFilter(Context context) {
        this.context = context;
    }



}
