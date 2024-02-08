package cz.xlisto.odecty.databaze;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;

import java.util.ArrayList;

import cz.xlisto.odecty.models.HdoModel;


/**
 * Třída pro přístup k datum HDO
 * Xlisto 26.05.2023 18:52
 */
public class DataHdoSource extends DataSource {
    private static final String TAG = "DataHdoSource";


    public DataHdoSource(Context context) {
        super.context = context;
        dbHelper = new DbHelper(context);
    }


    /**
     * Načte hdo data z databáze a vyfiltruje je podle sloupce datumOd nebo po,ut,st,ct,pa,so,ne
     *
     * @param table Tabulka se záznamy HDO
     * @return ArrayList<HdoModel>
     */
    public ArrayList<HdoModel> loadHdo(String table, String datumOd, int dayOfWeek) {
        return loadHdo(table, datumOd, dayOfWeek, null);
    }


    /**
     * Načte hdo data z databáze
     *
     * @param table Tabulka se záznamy HDO
     * @return ArrayList<HdoModel>
     */
    public ArrayList<HdoModel> loadHdo(String table) {
        return loadHdo(table, null, null, null);
    }


    /**
     * Načte hdo data z databáze
     *
     * @param table      Tabulka se záznamy HDO
     * @param datumOd    String datum platnosti (PRE)
     * @param dayOfWeek  Integer den v týdnu (jak jej vrací Calendar)
     * @param selectRele String filtr pro sloupec rele
     * @return ArrayList<HdoModel>
     */
    public ArrayList<HdoModel> loadHdo(String table, String datumOd, Integer dayOfWeek, String selectRele) {
        String distributionArea = getDistributionArea(table);

        String selection = "";
        String selectionDayOfWeek = "";
        if (dayOfWeek != null) {
            switch (dayOfWeek - 1) {
                case 0:
                    selectionDayOfWeek = DbHelper.COLUMN_SUN;
                    break;
                case 1:
                    selectionDayOfWeek = DbHelper.COLUMN_MON;
                    break;
                case 2:
                    selectionDayOfWeek = DbHelper.COLUMN_TUE;
                    break;
                case 3:
                    selectionDayOfWeek = DbHelper.COLUMN_WED;
                    break;
                case 4:
                    selectionDayOfWeek = DbHelper.COLUMN_THU;
                    break;
                case 5:
                    selectionDayOfWeek = DbHelper.COLUMN_FRI;
                    break;
                case 6:
                    selectionDayOfWeek = DbHelper.COLUMN_SAT;
                    break;
            }
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
                    cursor.getString(cursor.getColumnIndexOrThrow(DbHelper.COLUMN_DISTRIBUTION_AREA))
            );
            hdoModels.add(hdoModel);
        }
        cursor.close();
        return hdoModels;
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


    public void clearHdo(String table) {
        open();
        database.delete(table, null, null);
        close();
    }


    /**
     * Uloží HdoModely do databáze
     *
     * @param hdoModels ArrayList<HdoModel>
     * @param table     String jméno tabulky
     */
    public void saveHdo(ArrayList<HdoModel> hdoModels, String table) {
        open();
        database.delete(table, null, null);
        for (HdoModel hdoModel : hdoModels) {
            database.insert(table, null, createContentValues(hdoModel));
        }
        close();
    }


    /**
     * Uloží HdoModel do databáze
     *
     * @param hdoModel HdoModel
     * @param table    String jméno tabulky
     */
    public void saveHdo(HdoModel hdoModel, String table) {
        open();
        database.insert(table, null, createContentValues(hdoModel));
        close();
    }


    /**
     * Uloží aktualizovaný HdoModel do databáze
     *
     * @param hdoModel HdoModel
     * @param table    Tabulka se záznamy HDO
     */
    public void updateHdo(HdoModel hdoModel, String table) {
        open();
        database.update(table, createContentValues(hdoModel), DbHelper.COLUMN_ID + " = ?", new String[]{String.valueOf(hdoModel.getId())});
        close();
    }


    /**
     * Smaže HdoModel z databáze
     *
     * @param id    long  id hdomodelu
     * @param table Tabulka se záznamy HDO
     */
    public void deleteHdo(long id, String table) {
        open();
        database.delete(table, DbHelper.COLUMN_ID + " = ?", new String[]{String.valueOf(id)});
        close();
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
        return values;
    }
}
