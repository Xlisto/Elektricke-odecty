package cz.xlisto.cenik.shp;

import android.content.Context;

/**
 * Xlisto 05.02.2023 9:32
 */
public class ShPAddEditInvoice extends ShP{
    private static final String TAG = "ShPAddEditInvoice";

    public static final String LOAD_PREFERENCES = "load_preferences";
    public static final String TABLE = "table";
    public static final String ID = "id";
    public static final String BTNDATE_OF = "btnDate1";
    public static final String BTNDATE_TO = "btnDate2";
    public static final String VT_START = "vt_start";
    public static final String NT_START = "nt_start";
    public static final String VT_END = "vt_end";
    public static final String NT_END = "nt_end";
    public static final String OTHER_SERVICES = "other";
    public static final String SELECTED_ID_PRICE = "selectedIdPrice";
    public static final String SELECTED_ID_INVOICE = "selectedIdInvoice";

    public ShPAddEditInvoice(Context context) {
        this.context = context;
    }
}
