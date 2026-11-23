package cz.xlisto.elektrodroid.shp;

import android.content.Context;

/**
 * Xlisto 28.12.2023 21:03
 */
public class ShPDashBoard extends ShP {

    public static final String IS_SHOW_TOTAL = "isShowTotal";
    public static final String SHOW_INVOICE_SUM = "showInvoiceSum";
    public static final String USE_NEW_DASHBOARD = "useNewDashboard";
    public static final String PRICE_SHOW_TOTAL_KWH = "priceShowTotalKwh";
    public static final String PRICE_WITH_VAT = "priceWithVat";
    public static final String CHART_YEARLY_MODE = "chartYearlyMode";
    public static final String DASHBOARD_LAYOUT_CONFIG = "dashboardLayoutConfig";


    public ShPDashBoard(Context context) {
        this.context = context;
    }
}
