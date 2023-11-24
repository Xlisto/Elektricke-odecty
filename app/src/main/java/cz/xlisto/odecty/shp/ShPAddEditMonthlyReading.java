package cz.xlisto.odecty.shp;

import android.content.Context;

public class ShPAddEditMonthlyReading extends ShP {
    public static final String ARG_VT_MONTHLY_READING = "vtMonthlyReading";
    public static final String ARG_NT_MONTHLY_READING = "ntMonthlyReading";
    public static final String ARG_PAYMENT_MONTHLY_READING = "paymentMonthlyReading";
    public static final String ARG_SHOW_DESCRIPTION_MONTHLY_READING = "descriptionMonthlyReading";
    public static final String ARG_OTHER_MONTHLY_READING = "otherMonthlyReading";
    public static final String ARG_SHOW_ADD_PAYMENT_MONTHLY_READING = "add_paymentMonthlyReading";
    public static final String ARG_FIRST_READING_MONTHLY_READING = "first_readingMonthlyReading";
    public static final String ARG_DATE_MONTHLY_READING = "dateMonthlyReading";



    public ShPAddEditMonthlyReading(Context context) {
        this.context = context;
    }
}
