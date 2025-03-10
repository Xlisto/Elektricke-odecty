package cz.xlisto.elektrodroid.shp;


import android.content.Context;

/**
 * Třída `ShPAddEditMonthlyReading`, která rozšiřuje třídu `ShP`.
 * Tato třída poskytuje metody pro přidávání a úpravu měsíčních odečtů.
 */
public class ShPAddEditMonthlyReading extends ShP {

    public static final String ARG_VT_MONTHLY_READING = "vtMonthlyReading";
    public static final String ARG_NT_MONTHLY_READING = "ntMonthlyReading";
    public static final String ARG_PAYMENT_MONTHLY_READING = "paymentMonthlyReading";
    public static final String ARG_SHOW_DESCRIPTION_MONTHLY_READING = "descriptionMonthlyReading";
    public static final String ARG_DESCRIPTION = "description";
    public static final String ARG_OTHER_MONTHLY_READING = "otherMonthlyReading";
    public static final String ARG_SHOW_ADD_PAYMENT_MONTHLY_READING = "add_paymentMonthlyReading";
    public static final String ARG_FIRST_READING_MONTHLY_READING = "first_readingMonthlyReading";
    public static final String ARG_DATE_MONTHLY_READING = "dateMonthlyReading";
    public static final String ARG_LAST_ID_SELECTED_PRICE_LIST = "lastIdSelectedPriceList";


    public ShPAddEditMonthlyReading(Context context) {
        this.context = context;
    }

}
