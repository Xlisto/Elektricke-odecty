package cz.xlisto.odecty.databaze;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.util.Log;

import cz.xlisto.odecty.models.SubscriptionPointModel;

import static cz.xlisto.odecty.databaze.DbHelper.TABLE_NAME_SETTINGS;


/**
 * Přístup k databázi nastavení
 * Xlisto 26.05.2023 11:33
 */
public class DataSettingsSource extends DataSource {
    private static final String TAG = "DataSettingsSource";
    private static final String PREFIX_TIME_SHIFT = "posunCasu";


    public DataSettingsSource(Context context) {
        super.context = context;
        dbHelper = new DbHelper(context);
    }


    /**
     * Vrátí název parametru v tabulce s nastavením
     *
     * @param idSubscriptionPoint id odběrného místa
     * @return String název parametru uloženého v databázi
     */
    private String loadTimeShiftName(long idSubscriptionPoint) {

        DataSubscriptionPointSource dataSubscriptionPointSource = new DataSubscriptionPointSource(context);
        dataSubscriptionPointSource.open();
        open();
        SubscriptionPointModel subscriptionPoint = dataSubscriptionPointSource.loadSubscriptionPoint(idSubscriptionPoint);
        String timeShift = PREFIX_TIME_SHIFT + subscriptionPoint.getIdMilins();
        dataSubscriptionPointSource.close();
        return timeShift;
    }


    /**
     * Vrátí posun času pro dané odběrné místo.
     * Pokud záznam neexistuje, vytvoří jej s hodnotou 0.
     *
     * @param idSubscriptionPoint id odběrného místa
     * @return long posun času v milisekundách
     */
    public long loadTimeShift(long idSubscriptionPoint) {
        if(idSubscriptionPoint < 0) return 0;
        String timeShiftName = loadTimeShiftName(idSubscriptionPoint);

        String selection = "jmeno=?";
        String[] args = new String[]{timeShiftName};

        Cursor cursor = database.query(TABLE_NAME_SETTINGS,
                null,
                selection,
                args,
                null,
                null,
                null);

        cursor.moveToFirst();
        if (cursor.getCount() == 0) {
            ContentValues values = new ContentValues();
            values.put("jmeno", timeShiftName);
            values.put("hodnota", 0);
            database.insert(TABLE_NAME_SETTINGS, null, values);
            return 0;
        }

        long timeShiftValue = 0;

        if (cursor.getColumnIndex("hodnota") >= 0) {
            timeShiftValue = cursor.getLong(cursor.getColumnIndexOrThrow("hodnota"));
        }

        cursor.close();

        return timeShiftValue;
    }


    /**
     * Změní posun času pro dané odběrné místo
     *
     * @param idSubscriptionPoint id odběrného místa
     * @param timeShift           posun času v milisekundách
     */
    public void changeTimeShift(long idSubscriptionPoint, long timeShift) {
        String[] arguments = new String[]{loadTimeShiftName(idSubscriptionPoint)};
        open();
        ContentValues values = new ContentValues();
        values.put("hodnota", timeShift);
        database.update(TABLE_NAME_SETTINGS, values, "jmeno=?", arguments);
        close();
    }


    /**
     * Smaže posun času pro dané odběrné místo
     * @param idSubscriptionPoint id odběrného místa
     */
    public void deleteTimeShift(long idSubscriptionPoint) {
        String[] arguments = new String[]{loadTimeShiftName(idSubscriptionPoint)};
        open();
        database.delete(TABLE_NAME_SETTINGS, "jmeno=?", arguments);
        close();
    }
}
