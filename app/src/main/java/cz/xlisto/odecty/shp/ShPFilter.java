package cz.xlisto.odecty.shp;

import android.content.Context;

public class ShPFilter extends ShP {
    public static final String RADA = "filtrRada";
    public static final String PRODUKT = "filtrProdukt";
    public static final String SAZBA = "filtrSazba";
    public static final String COMPANY = "filtrDodavatel";
    public static final String AREA = "filtrUzemi";
    public static final String DATE_START = "filtrDateStart";
    public static final String DATE_END = "filtrDateEnd";
    public static final String DEFAULT = "%";
    public static final String POSITION = "position";


    public ShPFilter(Context context) {
        this.context = context;
    }



}
