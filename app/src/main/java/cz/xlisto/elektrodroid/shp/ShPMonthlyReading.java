package cz.xlisto.elektrodroid.shp;


import android.content.Context;


/**
 * Třída `ShPMonthlyReading`, která rozšiřuje třídu `ShP`.
 * Tato třída poskytuje konstanty a metody pro práci s měsíčními odečty v `SharedPreferences`.
 */
public class ShPMonthlyReading extends ShP {

    public static final String SHORT_LIST = "shortList";
    public static final String REGUL_PRICE = "regulPrice";
    public static final String ADD_BACKUP_NEW_READING = "addBackupNewReading";
    public static final String ADD_BACKUP_EDT_READING = "addBackupEdtReading";
    public static final String SEND_BACKUP_NEW_READING = "sendBackupNewReading";
    public static final String SEND_BACKUP_EDT_READING = "sendBackupEdtReading";


    public ShPMonthlyReading(Context context) {
        this.context = context;
    }

}
