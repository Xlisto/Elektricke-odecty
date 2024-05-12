package cz.xlisto.elektrodroid.shp;

import android.content.Context;

public class ShPMonthlyReading extends ShP{
    public static final String SHORT_LIST = "shortList";
    public static final String REGUL_PRICE = "regulPrice";
    public static final String ADD_BACKUP_NEW_READING = "addBackupNewReading";
    public static final String ADD_BACKUP_EDT_READING = "addBackupEdtReading";


    public ShPMonthlyReading(Context context) {
        this.context = context;
    }
}
