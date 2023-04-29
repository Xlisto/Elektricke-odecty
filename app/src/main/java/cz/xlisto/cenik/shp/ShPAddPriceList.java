package cz.xlisto.cenik.shp;

import android.content.Context;

import cz.xlisto.cenik.shp.ShP;

public class ShPAddPriceList extends ShP {
    public static final String PLATNOST_OD = "platnost_ceniku_od";
    public static final String PLATNOST_DO = "platnost_ceniku_do";
    public static final String RADA = "etRada";
    public static final String DODAVATEL = "etDodavatel";
    public static final String PRODUKT = "etProdukt";
    public static final String DIST_UZEMI = "dist_uzemi";



    public ShPAddPriceList(Context context) {
        this.context = context;
    }
}
