package cz.xlisto.elektrodroid.databaze;


import static cz.xlisto.elektrodroid.databaze.DbHelper.TABLE_NAME_SETTINGS;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;

import cz.xlisto.elektrodroid.models.SubscriptionPointModel;


/**
 * Přístup k databázi nastavení
 * Xlisto 26.05.2023 11:33
 */
public class DataSettingsSource extends DataSource {

    private static final String TAG = "DataSettingsSource";
    private static final String PREFIX_TIME_SHIFT = "posunCasu";
    private static final String PREFIX_FIRST_METER = "firstMeter";
    private static final String PREFIX_COLOR_VT = "colorVT";
    private static final String PREFIX_COLOR_NT = "colorNT";
    private static final String PREFIX_NAME = "jmeno";
    private static final String PREFIX_VALUE = "hodnota";


    public DataSettingsSource(Context context) {
        super.context = context;
        dbHelper = new DbHelper(context);
    }


    /**
     * Vloží/aktualizuje výchozí stavy měřičů pro případ, že není použitá poslední faktura
     *
     * @param idSubscriptionPoint id odběrného místa
     * @param meters              String s datem a stavy měřičů
     */
    public void setFirstMeter(long idSubscriptionPoint, String meters) {
        String name = loadFirstMeterName(idSubscriptionPoint);
        if (isExistsFirstMeterName(idSubscriptionPoint)) {
            updateFirstMeters(name, meters);
        } else {
            insertFirstMeters(name, meters);
        }

    }


    /**
     * Uloží výchozí stavy měřičů pro případ, že není použitá poslední faktura
     *
     * @param name       selektor podle odběrného místa
     * @param parameters parametry
     */
    private void insertFirstMeters(String name, String parameters) {
        ContentValues values = new ContentValues();
        values.put(PREFIX_NAME, name);
        values.put(PREFIX_VALUE, parameters);
        database.insert(TABLE_NAME_SETTINGS, null, values);

    }


    /**
     * Aktualizuje výchozí stavy měřičů pro případ, že není použitá poslední faktura
     *
     * @param name       selektor podle odběrného místa
     * @param parameters parametry
     */
    private void updateFirstMeters(String name, String parameters) {
        String[] arguments = new String[]{name};
        ContentValues values = new ContentValues();
        values.put(PREFIX_VALUE, parameters);
        database.update(TABLE_NAME_SETTINGS, values, PREFIX_NAME + "=?", arguments);
    }


    /**
     * Načte výchozí stavy měřičů pro případ, že není použitá poslední faktura
     *
     * @param idSubscription id odběrného místa
     * @return String parametry
     */
    public String loadFirstMeters(long idSubscription) {
        String name = loadFirstMeterName(idSubscription);
        String selection = PREFIX_NAME + "=?";
        String[] args = new String[]{name};

        Cursor cursor = database.query(TABLE_NAME_SETTINGS,
                null,
                selection,
                args,
                null,
                null,
                null);

        cursor.moveToFirst();
        String parameters = "";
        if (cursor.getCount() > 0) {
            parameters = cursor.getString(cursor.getColumnIndexOrThrow(PREFIX_VALUE));
        }
        cursor.close();
        return parameters;
    }


    /**
     * Načte barvy pro VT a NT
     * Pokud záznam bude chybět - nastaví se výchozí červená a modrá
     *
     * @return pole celých čísel s barvami
     */
    public int[] loadColorVTNT() {

        String selection = PREFIX_NAME + "=?";
        String[] argsVT = new String[]{PREFIX_COLOR_VT};
        String[] argsNT = new String[]{PREFIX_COLOR_NT};

        Cursor cursorVT = database.query(TABLE_NAME_SETTINGS,
                null,
                selection,
                argsVT,
                null,
                null,
                null);

        Cursor cursorNT = database.query(TABLE_NAME_SETTINGS,
                null,
                selection,
                argsNT,
                null,
                null,
                null);

        cursorVT.moveToFirst();
        cursorNT.moveToFirst();

        int[] colors = new int[2];

        if (cursorVT.getCount() > 0)
            colors[0] = cursorVT.getInt(cursorVT.getColumnIndexOrThrow(PREFIX_VALUE));
        else
            colors[0] = 0xFFFF0000;//red

        if (cursorNT.getCount() > 0)
            colors[1] = cursorNT.getInt(cursorNT.getColumnIndexOrThrow(PREFIX_VALUE));
        else
            colors[1] = 0xFF0000FF;//blue

        cursorVT.close();
        cursorNT.close();

        return colors;
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
     * Načte název parametru v tabulce s nastavením pro nastavení výchozích stavů měřičů
     *
     * @param idSubscriptionPoint id odběrného místa
     * @return String název parametru uloženého v databázi
     */
    private String loadFirstMeterName(long idSubscriptionPoint) {
        DataSubscriptionPointSource dataSubscriptionPointSource = new DataSubscriptionPointSource(context);
        dataSubscriptionPointSource.open();
        open();
        SubscriptionPointModel subscriptionPoint = dataSubscriptionPointSource.loadSubscriptionPoint(idSubscriptionPoint);
        String firstMeter = PREFIX_FIRST_METER + subscriptionPoint.getIdMilins();
        dataSubscriptionPointSource.close();
        return firstMeter;
    }


    /**
     * Zkontroluje, zda existuje záznam pro výchozí stavy měřičů
     *
     * @param idSubscriptionPoint id odběrného místa
     * @return boolean true/false
     */
    private boolean isExistsFirstMeterName(long idSubscriptionPoint) {
        String name = loadFirstMeterName(idSubscriptionPoint);
        String selection = PREFIX_NAME + "=?";
        String[] args = new String[]{name};

        Cursor cursor = database.query(TABLE_NAME_SETTINGS,
                null,
                selection,
                args,
                null,
                null,
                null);
        if (cursor.getCount() > 0) {
            cursor.close();
            return true;
        } else {
            cursor.close();
            return false;
        }
    }


    /**
     * Vrátí posun času pro dané odběrné místo.
     * Pokud záznam neexistuje, vytvoří jej s hodnotou 0.
     *
     * @param idSubscriptionPoint id odběrného místa
     * @return long posun času v milisekundách
     */
    public long loadTimeShift(long idSubscriptionPoint) {
        if (idSubscriptionPoint < 0) return 0;
        String timeShiftName = loadTimeShiftName(idSubscriptionPoint);

        String selection = PREFIX_NAME + "=?";
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
            values.put(PREFIX_NAME, timeShiftName);
            values.put(PREFIX_VALUE, 0);
            database.insert(TABLE_NAME_SETTINGS, null, values);
            return 0;
        }

        long timeShiftValue = 0;

        if (cursor.getColumnIndex(PREFIX_VALUE) >= 0) {
            timeShiftValue = cursor.getLong(cursor.getColumnIndexOrThrow(PREFIX_VALUE));
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
        values.put(PREFIX_VALUE, timeShift);
        database.update(TABLE_NAME_SETTINGS, values, PREFIX_NAME + "=?", arguments);
        close();
    }


    /**
     * Smaže posun času pro dané odběrné místo
     *
     * @param idSubscriptionPoint id odběrného místa
     */
    public void deleteTimeShift(long idSubscriptionPoint) {
        String[] arguments = new String[]{loadTimeShiftName(idSubscriptionPoint)};
        delete(arguments);
    }


    /**
     * Smaže výchozí stavy měřičů
     *
     * @param idSubscriptionPoint id odběrného místa
     */
    public void deleteFirstMeters(long idSubscriptionPoint) {
        String[] arguments = new String[]{loadFirstMeterName(idSubscriptionPoint)};
        delete(arguments);
    }


    /**
     * Provede smazání podle where klauzule
     *
     * @param whereArgs pole argumentů
     */
    private void delete(String[] whereArgs) {
        database.delete(TABLE_NAME_SETTINGS, PREFIX_NAME + "=?", whereArgs);
    }

}
