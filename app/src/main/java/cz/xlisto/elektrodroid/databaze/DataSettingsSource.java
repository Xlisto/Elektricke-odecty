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
    private static final String PREFIX_HDO_WIDGETS = "hdoWidgets";


    public DataSettingsSource(Context context) {
        super.context = context;
        dbHelper = new DbHelper(context);
    }


    /**
     * Vloží nebo aktualizuje výchozí stavy měřičů pro dané odběrné místo.
     * Používá se v případě, že není použita poslední faktura.
     *
     * @param idSubscriptionPoint id odběrného místa
     * @param meters              řetězec obsahující datum a stavy měřičů
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
     * Vloží nový záznam s výchozími stavy měřičů do tabulky nastavení.
     *
     * @param name       název parametru (selektor podle odběrného místa)
     * @param parameters hodnoty/parametry (např. datum a stavy měřičů)
     */
    private void insertFirstMeters(String name, String parameters) {
        ContentValues values = new ContentValues();
        values.put(PREFIX_NAME, name);
        values.put(PREFIX_VALUE, parameters);
        database.insert(TABLE_NAME_SETTINGS, null, values);

    }


    /**
     * Aktualizuje existující záznam výchozích stavů měřičů.
     *
     * @param name       název parametru (selektor podle odběrného místa)
     * @param parameters nové hodnoty/parametry
     */
    private void updateFirstMeters(String name, String parameters) {
        String[] arguments = new String[]{name};
        ContentValues values = new ContentValues();
        values.put(PREFIX_VALUE, parameters);
        database.update(TABLE_NAME_SETTINGS, values, PREFIX_NAME + "=?", arguments);
    }


    /**
     * Načte řetězec s výchozími stavy měřičů pro dané odběrné místo.
     * Pokud záznam neexistuje, vrátí prázdný řetězec.
     *
     * @param idSubscription id odběrného místa
     * @return řetězec obsahující parametry (nebo prázdný řetězec)
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
     * Načte barvy pro VT (vysoký tarif) a NT (nízký tarif).
     * Pokud záznamy v databázi chybí, vrátí výchozí barvy (červená pro VT, modrá pro NT).
     *
     * @return pole dvou integer hodnot [barvaVT, barvaNT]
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
     * Sestaví a vrátí název položky v tabulce nastavení pro posun času
     * pro dané odběrné místo.
     *
     * @param idSubscriptionPoint id odběrného místa
     * @return název parametru (řetězec) použitý v tabulce nastavení
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
     * Sestaví a vrátí název položky v tabulce nastavení pro výchozí stavy měřičů
     * pro dané odběrné místo.
     *
     * @param idSubscriptionPoint id odběrného místa
     * @return název parametru (řetězec) použitý v tabulce nastavení
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
     * Zjistí, zda v tabulce nastavení existuje záznam výchozích stavů měřičů
     * pro dané odběrné místo.
     *
     * @param idSubscriptionPoint id odběrného místa
     * @return true pokud záznam existuje, jinak false
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
     * Načte posun času (v milisekundách) pro dané odběrné místo.
     * Pokud záznam neexistuje, vytvoří nový záznam s hodnotou 0 a vrátí 0.
     *
     * @param idSubscriptionPoint id odběrného místa
     * @return posun času v milisekundách
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
     * Změní (aktualizuje) posun času pro dané odběrné místo v tabulce nastavení.
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
     * Odstraní záznam s posunem času pro dané odběrné místo.
     *
     * @param idSubscriptionPoint id odběrného místa
     */
    public void deleteTimeShift(long idSubscriptionPoint) {
        String[] arguments = new String[]{loadTimeShiftName(idSubscriptionPoint)};
        delete(arguments);
    }


    /**
     * Odstraní záznam s výchozími stavy měřičů pro dané odběrné místo.
     *
     * @param idSubscriptionPoint id odběrného místa
     */
    public void deleteFirstMeters(long idSubscriptionPoint) {
        String[] arguments = new String[]{loadFirstMeterName(idSubscriptionPoint)};
        delete(arguments);
    }


    /**
     * Pomocná metoda pro smazání záznamu v tabulce nastavení podle jména.
     *
     * @param whereArgs pole argumentů pro WHERE klauzuli (např. název parametru)
     */
    private void delete(String[] whereArgs) {
        database.delete(TABLE_NAME_SETTINGS, PREFIX_NAME + "=?", whereArgs);
    }

    /**
     * Uloží nebo aktualizuje JSON konfiguraci widgetů HDO pro dané odběrné místo.
     *
     * @param idSubscriptionPoint id odběrného místa
     * @param json                JSON s konfigurací widgetů
     */
    public void setHdoWidgets(long idSubscriptionPoint, String json) {
        String name = loadHdoWidgetsName(idSubscriptionPoint);
        if (isExistsByName(name)) {
            updateByName(name, json);
        } else {
            insertByName(name, json);
        }
    }

    /**
     * Vloží záznam do tabulky nastavení podle názvu.
     *
     * @param name       název parametru
     * @param parameters hodnota parametru
     */
    private void insertByName(String name, String parameters) {
        ContentValues values = new ContentValues();
        values.put(PREFIX_NAME, name);
        values.put(PREFIX_VALUE, parameters);
        database.insert(TABLE_NAME_SETTINGS, null, values);
    }

    /**
     * Aktualizuje záznam v tabulce nastavení podle názvu.
     *
     * @param name       název parametru
     * @param parameters nová hodnota parametru
     */
    private void updateByName(String name, String parameters) {
        String[] arguments = new String[]{name};
        ContentValues values = new ContentValues();
        values.put(PREFIX_VALUE, parameters);
        database.update(TABLE_NAME_SETTINGS, values, PREFIX_NAME + "=?", arguments);
    }

    /**
     * Sestaví a vrátí název položky pro ukládání konfigurace HDO widgetů
     * pro dané odběrné místo.
     *
     * @param idSubscriptionPoint id odběrného místa
     * @return název parametru (řetězec)
     */
    private String loadHdoWidgetsName(long idSubscriptionPoint) {
        DataSubscriptionPointSource dataSubscriptionPointSource = new DataSubscriptionPointSource(context);
        dataSubscriptionPointSource.open();
        open();
        SubscriptionPointModel subscriptionPoint = dataSubscriptionPointSource.loadSubscriptionPoint(idSubscriptionPoint);
        String name = PREFIX_HDO_WIDGETS + subscriptionPoint.getIdMilins();
        dataSubscriptionPointSource.close();
        return name;
    }

    /**
     * Načte uloženou JSON konfiguraci HDO widgetů pro dané odběrné místo.
     * Pokud záznam neexistuje, vrátí prázdný řetězec.
     *
     * @param idSubscriptionPoint id odběrného místa
     * @return JSON řetězec s konfigurací nebo prázdný řetězec
     */
    public String loadHdoWidgets(long idSubscriptionPoint) {
        String name = loadHdoWidgetsName(idSubscriptionPoint);
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
        String json = "";
        if (cursor.getCount() > 0) {
            json = cursor.getString(cursor.getColumnIndexOrThrow(PREFIX_VALUE));
        }
        cursor.close();
        return json;
    }

    /**
     * Pomocná metoda pro ověření existence záznamu podle názvu.
     *
     * @param name název parametru
     * @return true pokud záznam existuje, jinak false
     */
    private boolean isExistsByName(String name) {
        String selection = PREFIX_NAME + "=?";
        String[] args = new String[]{name};
        Cursor cursor = database.query(TABLE_NAME_SETTINGS,
                null,
                selection,
                args,
                null,
                null,
                null);
        boolean exists = cursor.getCount() > 0;
        cursor.close();
        return exists;
    }

}
