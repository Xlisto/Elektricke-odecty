package cz.xlisto.elektrodroid.databaze;


import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;

import java.util.ArrayList;

import cz.xlisto.elektrodroid.models.HdoModel;
import cz.xlisto.elektrodroid.services.HdoAlarmScheduler;


/**
 * Třída pro přístup k datum HDO
 * Xlisto 26.05.2023 18:52
 */
public class DataHdoSource extends DataSource {

    /**
     * Vytvoří datový zdroj pro práci s HDO tabulkami.
     *
     * @param context kontext aplikace
     */
    public DataHdoSource(Context context) {
        super.context = context;
        dbHelper = new DbHelper(context);
    }


    /**
     * Načte HDO data z databáze a vyfiltruje je podle dne v týdnu.
     *
     * @param table     tabulka se záznamy HDO
     * @param dayOfWeek den v týdnu (Calendar)
     * @return seznam HDO záznamů
     */
    public ArrayList<HdoModel> loadHdo(String table, int dayOfWeek) {
        return loadHdo(table, dayOfWeek, null);
    }


    /**
     * Načte hdo data z databáze
     *
     * @param table Tabulka se záznamy HDO
     * @return ArrayList<HdoModel>
     */
    public ArrayList<HdoModel> loadHdo(String table) {
        return loadHdo(table, null, null);
    }


    /**
     * Načte HDO data z databáze, volitelně podle dne v týdnu a relé.
     *
     * @param table      tabulka se záznamy HDO
     * @param dayOfWeek  den v týdnu (jak jej vrací Calendar), nebo {@code null}
     * @param selectRele filtr pro sloupec relé, nebo {@code null}
     * @return seznam HDO záznamů
     */
    public ArrayList<HdoModel> loadHdo(String table, Integer dayOfWeek, String selectRele) {
        ensureAlarmColumns(table);
        String distributionArea = getDistributionArea(table);

        String selection = "";
        String selectionDayOfWeek = "";
        if (dayOfWeek != null) {
            selectionDayOfWeek = switch (dayOfWeek - 1) {
                case 0 -> DbHelper.COLUMN_SUN;
                case 1 -> DbHelper.COLUMN_MON;
                case 2 -> DbHelper.COLUMN_TUE;
                case 3 -> DbHelper.COLUMN_WED;
                case 4 -> DbHelper.COLUMN_THU;
                case 5 -> DbHelper.COLUMN_FRI;
                case 6 -> DbHelper.COLUMN_SAT;
                default -> selectionDayOfWeek;
            };
        }

        ArrayList<String> selectionArgsList = new ArrayList<>();
        //vyhledávání podle datumu - používá PRE
        //if (distributionArea != null && distributionArea.equals("PRE") && datumOd != null) {
        //deaktivováno z důvodu potřeby načítat celý seznam HDO pro výpočet do začátku platnosti dalšího HDO ve GraphTotalHdoView
        //selection += DbHelper.COLUMN_DATE_FROM + " = ?";
        //selectionArgsList.add(datumOd);
        //}
        //vyhledávání podle dne v týdnu
        if (distributionArea == null && dayOfWeek != null) {
            selection += selectionDayOfWeek + " = ? ";
            selectionArgsList.add("1");
        }
        //vyhledávání podle relé
        if (selectRele != null) {
            if (!selection.isEmpty()) selection += " AND ";
            selection += DbHelper.COLUMN_RELE + " = ? ";
            selectionArgsList.add(selectRele);
        }

        String[] selectionArgs = selectionArgsList.toArray(new String[0]);
        ArrayList<HdoModel> hdoModels = new ArrayList<>();
        Cursor cursor = database.query(table,
                null,
                selection,
                selectionArgs,
                null,
                null,
                "CASE " +
                        "WHEN rele LIKE '%TUV%' THEN 1 " +
                        "WHEN rele LIKE '%TAR%' THEN 2 " +
                        "WHEN rele LIKE '%PV%' THEN 3 " +
                        "ELSE 4 END, rele ASC");

        for (int i = 0; i < cursor.getCount(); i++) {
            cursor.moveToPosition(i);
            HdoModel hdoModel = new HdoModel(
                    cursor.getLong(cursor.getColumnIndexOrThrow(DbHelper.COLUMN_ID)),
                    cursor.getString(cursor.getColumnIndexOrThrow(DbHelper.COLUMN_RELE)),
                    cursor.getString(cursor.getColumnIndexOrThrow(DbHelper.COLUMN_DATE_FROM)),
                    cursor.getString(cursor.getColumnIndexOrThrow(DbHelper.COLUMN_DATE_UNTIL)),
                    cursor.getString(cursor.getColumnIndexOrThrow(DbHelper.COLUMN_TIME_FROM)),
                    cursor.getString(cursor.getColumnIndexOrThrow(DbHelper.COLUMN_TIME_UNTIL)),
                    cursor.getInt(cursor.getColumnIndexOrThrow(DbHelper.COLUMN_MON)),
                    cursor.getInt(cursor.getColumnIndexOrThrow(DbHelper.COLUMN_TUE)),
                    cursor.getInt(cursor.getColumnIndexOrThrow(DbHelper.COLUMN_WED)),
                    cursor.getInt(cursor.getColumnIndexOrThrow(DbHelper.COLUMN_THU)),
                    cursor.getInt(cursor.getColumnIndexOrThrow(DbHelper.COLUMN_FRI)),
                    cursor.getInt(cursor.getColumnIndexOrThrow(DbHelper.COLUMN_SAT)),
                    cursor.getInt(cursor.getColumnIndexOrThrow(DbHelper.COLUMN_SUN)),
                    cursor.getString(cursor.getColumnIndexOrThrow(DbHelper.COLUMN_DISTRIBUTION_AREA)),
                    getOptionalInt(cursor, DbHelper.COLUMN_NOTIFY_START),
                    getOptionalInt(cursor, DbHelper.COLUMN_NOTIFY_END)
            );
            hdoModels.add(hdoModel);
        }
        cursor.close();
        return hdoModels;
    }


    /**
     * Načte jeden HDO záznam podle jeho ID.
     *
     * @param table název HDO tabulky
     * @param id    ID záznamu
     * @return načtený model nebo {@code null}, pokud neexistuje
     */
    public HdoModel loadHdoById(String table, long id) {
        ensureAlarmColumns(table);
        Cursor cursor = database.query(table,
                null,
                DbHelper.COLUMN_ID + " = ?",
                new String[]{String.valueOf(id)},
                null,
                null,
                null,
                "1");

        if (cursor.getCount() == 0) {
            cursor.close();
            return null;
        }
        cursor.moveToFirst();
        HdoModel hdoModel = new HdoModel(
                cursor.getLong(cursor.getColumnIndexOrThrow(DbHelper.COLUMN_ID)),
                cursor.getString(cursor.getColumnIndexOrThrow(DbHelper.COLUMN_RELE)),
                cursor.getString(cursor.getColumnIndexOrThrow(DbHelper.COLUMN_DATE_FROM)),
                cursor.getString(cursor.getColumnIndexOrThrow(DbHelper.COLUMN_DATE_UNTIL)),
                cursor.getString(cursor.getColumnIndexOrThrow(DbHelper.COLUMN_TIME_FROM)),
                cursor.getString(cursor.getColumnIndexOrThrow(DbHelper.COLUMN_TIME_UNTIL)),
                cursor.getInt(cursor.getColumnIndexOrThrow(DbHelper.COLUMN_MON)),
                cursor.getInt(cursor.getColumnIndexOrThrow(DbHelper.COLUMN_TUE)),
                cursor.getInt(cursor.getColumnIndexOrThrow(DbHelper.COLUMN_WED)),
                cursor.getInt(cursor.getColumnIndexOrThrow(DbHelper.COLUMN_THU)),
                cursor.getInt(cursor.getColumnIndexOrThrow(DbHelper.COLUMN_FRI)),
                cursor.getInt(cursor.getColumnIndexOrThrow(DbHelper.COLUMN_SAT)),
                cursor.getInt(cursor.getColumnIndexOrThrow(DbHelper.COLUMN_SUN)),
                cursor.getString(cursor.getColumnIndexOrThrow(DbHelper.COLUMN_DISTRIBUTION_AREA)),
                getOptionalInt(cursor, DbHelper.COLUMN_NOTIFY_START),
                getOptionalInt(cursor, DbHelper.COLUMN_NOTIFY_END)
        );
        cursor.close();
        return hdoModel;
    }


    /**
     * Zjistí z jaké distribuce se data nachází
     *
     * @param table Tabulka se záznamy HDO
     */
    private String getDistributionArea(String table) {
        String distributionArea;
        Cursor cursor = database.query(table,
                new String[]{DbHelper.COLUMN_DISTRIBUTION_AREA},
                null,
                null,
                null,
                null,
                null,
                "1");

        if (cursor.getCount() == 0) return null;
        cursor.moveToFirst();
        distributionArea = cursor.getString(cursor.getColumnIndexOrThrow(DbHelper.COLUMN_DISTRIBUTION_AREA));
        cursor.close();
        return distributionArea;
    }


    /**
     * Uloží HdoModely do databáze
     *
     * @param hdoModels ArrayList<HdoModel>
     * @param table     String jméno tabulky
     */
    public void saveHdo(ArrayList<HdoModel> hdoModels, String table) {
        open();
        ensureAlarmColumns(table);
        ArrayList<Long> oldIds = loadIds(table);
        database.delete(table, null, null);
        for (HdoModel hdoModel : hdoModels) {
            long newId = database.insert(table, null, createContentValues(hdoModel));
            if (newId > 0) {
                hdoModel.setId(newId);
            }
        }
        close();
        HdoAlarmScheduler.cancelForIds(context, table, oldIds);
        HdoAlarmScheduler.rescheduleForTable(context, table);
    }


    /**
     * Uloží HdoModel do databáze
     *
     * @param hdoModel HdoModel
     * @param table    String jméno tabulky
     */
    public void saveHdo(HdoModel hdoModel, String table) {
        open();
        ensureAlarmColumns(table);
        long newId = database.insert(table, null, createContentValues(hdoModel));
        close();
        if (newId > 0) {
            hdoModel.setId(newId);
        }
        HdoAlarmScheduler.rescheduleForModel(context, table, hdoModel);
    }


    /**
     * Uloží aktualizovaný HdoModel do databáze
     *
     * @param hdoModel HdoModel
     * @param table    Tabulka se záznamy HDO
     */
    public void updateHdo(HdoModel hdoModel, String table) {
        open();
        ensureAlarmColumns(table);
        database.update(table, createContentValues(hdoModel), DbHelper.COLUMN_ID + " = ?", new String[]{String.valueOf(hdoModel.getId())});
        close();
        HdoAlarmScheduler.rescheduleForModel(context, table, hdoModel);
    }


    /**
     * Smaže HdoModel z databáze
     *
     * @param id    long  id hdomodelu
     * @param table Tabulka se záznamy HDO
     */
    public void deleteHdo(long id, String table) {
        open();
        ensureAlarmColumns(table);
        database.delete(table, DbHelper.COLUMN_ID + " = ?", new String[]{String.valueOf(id)});
        close();
        HdoAlarmScheduler.cancelForModel(context, table, id);
    }


    /**
     * Vrátí seznam rele
     *
     * @param table Tabulka se záznamy HDO
     * @return ArrayList<String> seznam rele
     */
    public ArrayList<String> getReles(String table) {

        ArrayList<String> reles = new ArrayList<>();
        Cursor cursor = database.query(table,
                new String[]{DbHelper.COLUMN_RELE},
                null,
                null,
                DbHelper.COLUMN_RELE,
                null,
                null);

        for (int i = 0; i < cursor.getCount(); i++) {
            cursor.moveToPosition(i);
            reles.add(cursor.getString(cursor.getColumnIndexOrThrow(DbHelper.COLUMN_RELE)));
        }
        cursor.close();
        return reles;
    }


    /**
     * Vytvoří ContentValues pro uložení do databáze
     *
     * @param hdoModel HdoModel
     * @return ContentValues hdo modelu
     */
    private ContentValues createContentValues(HdoModel hdoModel) {
        ContentValues values = new ContentValues();
        values.put(DbHelper.COLUMN_RELE, hdoModel.getRele());
        values.put(DbHelper.COLUMN_DATE_FROM, hdoModel.getDateFrom());
        values.put(DbHelper.COLUMN_DATE_UNTIL, hdoModel.getDateUntil());
        values.put(DbHelper.COLUMN_TIME_FROM, hdoModel.getTimeFrom());
        values.put(DbHelper.COLUMN_TIME_UNTIL, hdoModel.getTimeUntil());
        values.put(DbHelper.COLUMN_MON, hdoModel.getMon());
        values.put(DbHelper.COLUMN_TUE, hdoModel.getTue());
        values.put(DbHelper.COLUMN_WED, hdoModel.getWed());
        values.put(DbHelper.COLUMN_THU, hdoModel.getThu());
        values.put(DbHelper.COLUMN_FRI, hdoModel.getFri());
        values.put(DbHelper.COLUMN_SAT, hdoModel.getSat());
        values.put(DbHelper.COLUMN_SUN, hdoModel.getSun());
        values.put(DbHelper.COLUMN_DISTRIBUTION_AREA, hdoModel.getDistributionArea());
        values.put(DbHelper.COLUMN_NOTIFY_START, hdoModel.getNotifyStart());
        values.put(DbHelper.COLUMN_NOTIFY_END, hdoModel.getNotifyEnd());
        return values;
    }


    /**
     * Bezpečně načte integer hodnotu ze sloupce, který nemusí existovat.
     *
     * @param cursor     aktivní cursor
     * @param columnName název sloupce
     * @return hodnota sloupce nebo 0, pokud sloupec není dostupný
     */
    private int getOptionalInt(Cursor cursor, String columnName) {
        int index = cursor.getColumnIndex(columnName);
        return index >= 0 ? cursor.getInt(index) : 0;
    }


    /**
     * Načte seznam ID všech záznamů v tabulce.
     *
     * @param table název tabulky
     * @return seznam ID
     */
    private ArrayList<Long> loadIds(String table) {
        ArrayList<Long> ids = new ArrayList<>();
        Cursor cursor = database.query(table,
                new String[]{DbHelper.COLUMN_ID},
                null,
                null,
                null,
                null,
                null);
        for (int i = 0; i < cursor.getCount(); i++) {
            cursor.moveToPosition(i);
            ids.add(cursor.getLong(0));
        }
        cursor.close();
        return ids;
    }


    /**
     * Zajistí existenci sloupců pro notifikace HDO alarmů.
     *
     * @param table název HDO tabulky
     */
    private void ensureAlarmColumns(String table) {
        addColumnIfMissing(table, DbHelper.COLUMN_NOTIFY_START);
        addColumnIfMissing(table, DbHelper.COLUMN_NOTIFY_END);
    }


    /**
     * Přidá sloupec do tabulky, pokud ještě neexistuje.
     *
     * @param table  název tabulky
     * @param column název přidávaného sloupce
     */
    private void addColumnIfMissing(String table, String column) {
        try {
            database.execSQL("ALTER TABLE " + table + " ADD COLUMN " + column + " INTEGER DEFAULT 0");
        } catch (SQLException ignored) {
            // Sloupec už existuje nebo tabulka není dostupná.
        }
    }

}
