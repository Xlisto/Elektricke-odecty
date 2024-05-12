package cz.xlisto.elektrodroid.shp;

import android.content.Context;

/**
 * Xlisto 28.12.2023 21:03
 */
public class ShPDashBoard extends ShP {
    private static final String TAG = "ShPDashBoard";
    public static final String IS_SHOW_TOTAL = "isShowTotal";
    public static final String SHOW_INVOICE_SUM = "showInvoiceSum";


    public ShPDashBoard(Context context) {
        this.context = context;
    }
}
