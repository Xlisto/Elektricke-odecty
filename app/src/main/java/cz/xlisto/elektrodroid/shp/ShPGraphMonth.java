package cz.xlisto.elektrodroid.shp;

import android.content.Context;

/**
 * Třída pro ukládání nastavení měsíčního/ročního grafu spotřeby.
 * Xlisto 31.10.2023 18:26
 */
public class ShPGraphMonth extends ShP {
    private static final String TAG = "ShPGraphMonth";
    public static final String ARG_IS_SHOW_PERIOD = "isShowPeriod";
    public static final String ARG_IS_SHOW_VT = "isShowVT";
    public static final String ARG_IS_SHOW_NT = "isShowNT";
    public static final String ARG_TYPE_GRAPH = "isShowLineGraph";
    public static final String ARG_COMPARE_MONTH = "compareMonth";

    public ShPGraphMonth(Context context) {
        this.context = context;
    }
}
