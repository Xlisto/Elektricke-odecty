package cz.xlisto.elektrodroid.databaze;


import static cz.xlisto.elektrodroid.databaze.DbHelper.TABLE_NAME_SETTINGS;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;

import static cz.xlisto.elektrodroid.databaze.DbHelper.COLUMN_ID;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import cz.xlisto.elektrodroid.models.SubscriptionPointModel;


/**
 * Přístup k databázi nastavení.
 *
 * <p>Poskytuje metody pro uložení a načítání různých nastavení aplikace do/z tabulky
 * {@code nastaveni}. Zahrnuje správu posunů času, výchozích stavů měřičů, barev VT/NT,
 * konfigurace HDO widgetů a aktuálního vybraného odběrného místa.</p>
 *
 * <p>Třída používá PREFIX_NAME a PREFIX_VALUE sloupce pro ukládání para-hodnot
 * (name-value pairs). Každá kategorie nastavení má svůj prefix (např. "posunCasu",
 * "firstMeter", "hdoWidgets", "aktualniOdberneMisto").</p>
 *
 * <p>Příklad použití:</p>
 * <pre>
 *     DataSettingsSource settingsSource = new DataSettingsSource(context);
 *     settingsSource.open();
 *     settingsSource.setCurrentSubscriptionPoint(subscriptionPointId);
 *     long current = settingsSource.loadCurrentSubscriptionPoint();
 *     settingsSource.close();
 * </pre>
 *
 * @author Xlisto
 * @version 26.05.2023
 */
public class DataSettingsSource extends DataSource {

    private static final String PREFIX_TIME_SHIFT = "posunCasu";
    private static final String PREFIX_FIRST_METER = "firstMeter";
    private static final String PREFIX_COLOR_VT = "colorVT";
    private static final String PREFIX_COLOR_NT = "colorNT";
    private static final String PREFIX_NAME = "jmeno";
    private static final String PREFIX_VALUE = "hodnota";
    private static final String PREFIX_HDO_WIDGETS = "hdoWidgets";
    private static final String PREFIX_PRICE_LIST_COMPARE_PARAMETERS = "priceListCompareParameters";
    private static final String PREFIX_CURRENT_SUBSCRIPTION_POINT = "aktualniOdberneMisto";
    private static final String PREFIX_READING_NOTIFICATION_ENABLED = "readingNotificationEnabled";
    private static final String PREFIX_READING_NOTIFICATION_FREQUENCY = "readingNotificationFrequency";
    private static final String PREFIX_READING_NOTIFICATION_DAY_OF_MONTH = "readingNotificationDayOfMonth";
    private static final String PREFIX_READING_NOTIFICATION_DAY_OF_WEEK = "readingNotificationDayOfWeek";
    private static final String PREFIX_READING_NOTIFICATION_TIME = "readingNotificationTime";
    private static final String[] SUBSCRIPTION_POINT_SETTING_PREFIXES = new String[]{
            PREFIX_TIME_SHIFT,
            PREFIX_FIRST_METER,
            PREFIX_HDO_WIDGETS,
            PREFIX_PRICE_LIST_COMPARE_PARAMETERS,
            PREFIX_READING_NOTIFICATION_ENABLED,
            PREFIX_READING_NOTIFICATION_FREQUENCY,
            PREFIX_READING_NOTIFICATION_DAY_OF_MONTH,
            PREFIX_READING_NOTIFICATION_DAY_OF_WEEK,
            PREFIX_READING_NOTIFICATION_TIME
    };


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
     * Uloží nebo aktualizuje JSON parametry porovnání ceníků pro dané odběrné místo.
     *
     * @param idSubscriptionPoint id odběrného místa
     * @param json                JSON s parametry porovnání ceníků
     */
    public void setPriceListCompareParameters(long idSubscriptionPoint, String json) {
        String name = loadPriceListCompareName(idSubscriptionPoint);
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
     * Sestaví a vrátí název položky pro ukládání parametrů porovnání ceníků
     * pro dané odběrné místo.
     *
     * @param idSubscriptionPoint id odběrného místa
     * @return název parametru (řetězec)
     */
    private String loadPriceListCompareName(long idSubscriptionPoint) {
        DataSubscriptionPointSource dataSubscriptionPointSource = new DataSubscriptionPointSource(context);
        dataSubscriptionPointSource.open();
        open();
        SubscriptionPointModel subscriptionPoint = dataSubscriptionPointSource.loadSubscriptionPoint(idSubscriptionPoint);
        String name;
        if (subscriptionPoint != null) {
            name = PREFIX_PRICE_LIST_COMPARE_PARAMETERS + subscriptionPoint.getIdMilins();
        } else {
            name = PREFIX_PRICE_LIST_COMPARE_PARAMETERS + idSubscriptionPoint;
        }
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
     * Načte uložené JSON parametry porovnání ceníků pro dané odběrné místo.
     * Pokud záznam neexistuje, vrátí prázdný řetězec.
     *
     * @param idSubscriptionPoint id odběrného místa
     * @return JSON řetězec s parametry nebo prázdný řetězec
     */
    public String loadPriceListCompareParameters(long idSubscriptionPoint) {
        String name = loadPriceListCompareName(idSubscriptionPoint);
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
     * Uloží, zda je notifikace zápisu pro odběrné místo povolena.
     */
    public void setReadingNotificationEnabled(long idSubscriptionPoint, boolean enabled) {
        String name = loadReadingNotificationSettingName(PREFIX_READING_NOTIFICATION_ENABLED, idSubscriptionPoint);
        String value = enabled ? "1" : "0";
        if (isExistsByName(name)) {
            updateByName(name, value);
        } else {
            insertByName(name, value);
        }
    }


    /**
     * Načte, zda je notifikace zápisu pro odběrné místo povolena.
     * Výchozí hodnota je false.
     */
    public boolean loadReadingNotificationEnabled(long idSubscriptionPoint) {
        String value = loadReadingNotificationValue(PREFIX_READING_NOTIFICATION_ENABLED, idSubscriptionPoint);
        return "1".equals(value) || "true".equalsIgnoreCase(value);
    }


    /**
     * Uloží frekvenci notifikace (0 = měsíčně, 1 = týdně).
     */
    public void setReadingNotificationFrequency(long idSubscriptionPoint, int frequency) {
        String name = loadReadingNotificationSettingName(PREFIX_READING_NOTIFICATION_FREQUENCY, idSubscriptionPoint);
        if (isExistsByName(name)) {
            updateByName(name, String.valueOf(frequency));
        } else {
            insertByName(name, String.valueOf(frequency));
        }
    }


    /**
     * Načte frekvenci notifikace. Výchozí hodnota je měsíčně (0).
     */
    public int loadReadingNotificationFrequency(long idSubscriptionPoint) {
        String value = loadReadingNotificationValue(PREFIX_READING_NOTIFICATION_FREQUENCY, idSubscriptionPoint);
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }


    /**
     * Uloží den v měsíci pro měsíční notifikaci.
     */
    public void setReadingNotificationDayOfMonth(long idSubscriptionPoint, String dayOfMonth) {
        String name = loadReadingNotificationSettingName(PREFIX_READING_NOTIFICATION_DAY_OF_MONTH, idSubscriptionPoint);
        if (isExistsByName(name)) {
            updateByName(name, dayOfMonth);
        } else {
            insertByName(name, dayOfMonth);
        }
    }


    /**
     * Načte den v měsíci pro notifikaci. Výchozí hodnota je "1".
     */
    public String loadReadingNotificationDayOfMonth(long idSubscriptionPoint) {
        String value = loadReadingNotificationValue(PREFIX_READING_NOTIFICATION_DAY_OF_MONTH, idSubscriptionPoint);
        return value.isEmpty() ? "1" : value;
    }


    /**
     * Uloží den v týdnu pro týdenní notifikaci.
     */
    public void setReadingNotificationDayOfWeek(long idSubscriptionPoint, int dayOfWeek) {
        String name = loadReadingNotificationSettingName(PREFIX_READING_NOTIFICATION_DAY_OF_WEEK, idSubscriptionPoint);
        if (isExistsByName(name)) {
            updateByName(name, String.valueOf(dayOfWeek));
        } else {
            insertByName(name, String.valueOf(dayOfWeek));
        }
    }


    /**
     * Načte den v týdnu pro notifikaci. Výchozí hodnota je 0 (pondělí).
     */
    public int loadReadingNotificationDayOfWeek(long idSubscriptionPoint) {
        String value = loadReadingNotificationValue(PREFIX_READING_NOTIFICATION_DAY_OF_WEEK, idSubscriptionPoint);
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }


    /**
     * Uloží čas notifikace ve formátu HH:mm.
     */
    public void setReadingNotificationTime(long idSubscriptionPoint, String time) {
        String name = loadReadingNotificationSettingName(PREFIX_READING_NOTIFICATION_TIME, idSubscriptionPoint);
        if (isExistsByName(name)) {
            updateByName(name, time);
        } else {
            insertByName(name, time);
        }
    }


    /**
     * Načte čas notifikace. Výchozí hodnota je "08:00".
     */
    public String loadReadingNotificationTime(long idSubscriptionPoint) {
        String value = loadReadingNotificationValue(PREFIX_READING_NOTIFICATION_TIME, idSubscriptionPoint);
        return value.isEmpty() ? "08:00" : value;
    }


    private String loadReadingNotificationValue(String prefix, long idSubscriptionPoint) {
        String name = loadReadingNotificationSettingName(prefix, idSubscriptionPoint);
        String selection = PREFIX_NAME + "=?";
        String[] args = new String[]{name};
        Cursor cursor = database.query(TABLE_NAME_SETTINGS,
                null,
                selection,
                args,
                null,
                null,
                null);

        String value = "";
        if (cursor.moveToFirst()) {
            value = cursor.getString(cursor.getColumnIndexOrThrow(PREFIX_VALUE));
        }
        cursor.close();
        return value;
    }


    private String loadReadingNotificationSettingName(String prefix, long idSubscriptionPoint) {
        DataSubscriptionPointSource dataSubscriptionPointSource = new DataSubscriptionPointSource(context);
        dataSubscriptionPointSource.open();
        open();
        SubscriptionPointModel subscriptionPoint = dataSubscriptionPointSource.loadSubscriptionPoint(idSubscriptionPoint);
        String name;
        if (subscriptionPoint != null) {
            name = prefix + subscriptionPoint.getIdMilins();
        } else {
            name = prefix + idSubscriptionPoint;
        }
        dataSubscriptionPointSource.close();
        return name;
    }


    /**
     * Uloží nebo aktualizuje ID aktuálně vybraného odběrného místa v tabulce nastavení.
     *
     * <p>Tato metoda je součástí mechanismu persistence pro aktuální vybrané odběrné místo.
     * Pokud záznam s klíčem "aktualniOdberneMisto" již existuje, bude aktualizován,
     * jinak bude vytvořen nový.</p>
     *
     * <p>Pozn: Toto je nižší vrstva ukládání; vyšší vrstva {@code SubscriptionPoint.setCurrentSelection()}
     * zároveň ukládá do SharedPreferences pro rychlý přístup.</p>
     *
     * @param idSubscriptionPoint id odběrného místa, které se má uložit
     * @see cz.xlisto.elektrodroid.utils.SubscriptionPoint#setCurrentSelection(Context, long)
     */
    public void setCurrentSubscriptionPoint(long idSubscriptionPoint) {
        String value = String.valueOf(idSubscriptionPoint);
        if (isExistsByName(PREFIX_CURRENT_SUBSCRIPTION_POINT))
            updateByName(PREFIX_CURRENT_SUBSCRIPTION_POINT, value);
        else
            insertByName(PREFIX_CURRENT_SUBSCRIPTION_POINT, value);
    }


    /**
     * Načte ID aktuálně vybraného odběrného místa z tabulky nastavení.
     *
     * <p>Tato metoda je určena zejména pro obnovu stavu po importu zálohy. Pokud záznam
     * v databázi neexistuje, vrátí -1L. Hodnota je parsována z řetězce a pokud parsování
     * selže (nevalidní formát čísla), vrátí se také -1L.</p>
     *
     * <p>Typické použití:</p>
     * <pre>
     *     long id = settings.loadCurrentSubscriptionPoint();
     *     if (id > 0) {
     *         // Validní ID načteno, lze jej použít
     *     }
     * </pre>
     *
     * @return id odběrného místa (> 0), nebo -1L pokud záznam neexistuje nebo neobsahuje validní číslo
     * @see cz.xlisto.elektrodroid.utils.SubscriptionPoint#applyCurrentFromSettings(Context)
     */
    public long loadCurrentSubscriptionPoint() {
        String selection = PREFIX_NAME + "=?";
        String[] args = new String[]{PREFIX_CURRENT_SUBSCRIPTION_POINT};
        Cursor cursor = database.query(TABLE_NAME_SETTINGS,
                null,
                selection,
                args,
                null,
                null,
                null);

        long subscriptionPointId = -1L;
        if (cursor.moveToFirst()) {
            String value = cursor.getString(cursor.getColumnIndexOrThrow(PREFIX_VALUE));
            try {
                subscriptionPointId = Long.parseLong(value);
            } catch (NumberFormatException ignored) {
            }
        }
        cursor.close();
        return subscriptionPointId;
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


    /**
     * Smaže všechna nastavení, která už nepatří žádnému existujícímu odběrnému místu.
     *
     * <p>Projde záznamy v tabulce nastavení a ponechá pouze ty položky, které mají podporovaný
     * prefix a jejich číselná přípona odpovídá některému z aktuálně uložených odběrných míst.</p>
     */
    public void deleteOrphanedSubscriptionPointSettings() {
        Set<Long> existingMilins = loadExistingSubscriptionPointMilins();
        Cursor cursor = database.query(TABLE_NAME_SETTINGS,
                new String[]{COLUMN_ID, PREFIX_NAME},
                null,
                null,
                null,
                null,
                null);

        ArrayList<Long> idsToDelete = new ArrayList<>();
        while (cursor.moveToNext()) {
            String name = cursor.getString(1);
            Long milins = extractSubscriptionPointMilins(name);
            if (milins != null && !existingMilins.contains(milins)) {
                idsToDelete.add(cursor.getLong(0));
            }
        }
        cursor.close();

        for (Long id : idsToDelete) {
            database.delete(TABLE_NAME_SETTINGS, COLUMN_ID + "=?", new String[]{String.valueOf(id)});
        }
    }


    /**
     * Načte množinu všech aktuálně uložených identifikátorů milisekund odběrných míst.
     *
     * @return množina platných identifikátorů milisekund
     */
    private Set<Long> loadExistingSubscriptionPointMilins() {
        Set<Long> milins = new HashSet<>();
        DataSubscriptionPointSource dataSubscriptionPointSource = new DataSubscriptionPointSource(context);
        dataSubscriptionPointSource.open();
        try {
            for (SubscriptionPointModel subscriptionPoint : dataSubscriptionPointSource.loadSubscriptionPoints()) {
                milins.add(Long.parseLong(subscriptionPoint.getIdMilins()));
            }
        } finally {
            dataSubscriptionPointSource.close();
        }
        return milins;
    }


    /**
     * Zkusí z názvu položky v tabulce nastavení získat číselnou příponu odběrného místa.
     *
     * @param name název položky v tabulce nastavení
     * @return přípona jako číslo, nebo {@code null}, pokud název neodpovídá podporovanému formátu
     */
    private Long extractSubscriptionPointMilins(String name) {
        if (name == null) {
            return null;
        }

        for (String prefix : SUBSCRIPTION_POINT_SETTING_PREFIXES) {
            if (name.startsWith(prefix)) {
                String suffix = name.substring(prefix.length());
                if (suffix.isEmpty()) {
                    return null;
                }
                try {
                    return Long.parseLong(suffix);
                } catch (NumberFormatException ignored) {
                    return null;
                }
            }
        }
        return null;
    }

}
