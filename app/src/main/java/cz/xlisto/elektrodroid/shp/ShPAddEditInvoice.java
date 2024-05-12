package cz.xlisto.elektrodroid.shp;

import android.content.Context;

/**
 * Třída pro ukládání nastavení přidání/)pravení faktury.
 * Xlisto 05.02.2023 9:32
 */
public class ShPAddEditInvoice extends ShP {
    public static final String LOAD_PREFERENCES = "loadPreferences";
    public static final String TABLE = "table";
    public static final String ID = "id";
    public static final String BTN_DATE_OF = "btnDate1";
    public static final String BTN_DATE_TO = "btnDate2";
    public static final String VT_START = "vtStart";
    public static final String NT_START = "ntStart";
    public static final String VT_END = "vtEnd";
    public static final String NT_END = "ntEnd";
    public static final String OTHER_SERVICES = "other";
    public static final String SELECTED_ID_PRICE = "selectedIdPrice";
    public static final String SELECTED_ID_INVOICE = "selectedIdInvoice";


    public ShPAddEditInvoice(Context context) {
        this.context = context;
    }
}
