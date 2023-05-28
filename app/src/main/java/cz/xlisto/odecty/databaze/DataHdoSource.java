package cz.xlisto.odecty.databaze;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;

import java.util.ArrayList;

import cz.xlisto.odecty.models.HdoModel;

/**
 * Xlisto 26.05.2023 18:52
 */
public class DataHdoSource extends DataSource {
    private static final String TAG = "DataHdoSource";


    public DataHdoSource(Context context) {
        super.context = context;
        dbHelper = new DbHelper(context);
    }


    /**
     * Načte hdo data z databáze
     *
     * @param table Tabulka se záznamy HDO
     * @return ArrayList<HdoModel>
     */
    public ArrayList<HdoModel> loadHdo(String table) {
        open();

        ArrayList<HdoModel> hdoModels = new ArrayList<>();
        Cursor cursor = database.query(table,
                null,
                null,
                null,
                null,
                null,
                null);

        for (int i = 0; i < cursor.getCount(); i++) {
            cursor.moveToPosition(i);
            HdoModel hdoModel = new HdoModel(
                    cursor.getLong(cursor.getColumnIndexOrThrow(DbHelper.COLUMN_ID)),
                    cursor.getString(cursor.getColumnIndexOrThrow(DbHelper.COLUMN_RELE)),
                    cursor.getString(cursor.getColumnIndexOrThrow(DbHelper.COLUMN_TIME_FROM)),
                    cursor.getString(cursor.getColumnIndexOrThrow(DbHelper.COLUMN_TIME_UNTIL)),
                    cursor.getInt(cursor.getColumnIndexOrThrow(DbHelper.COLUMN_MON)),
                    cursor.getInt(cursor.getColumnIndexOrThrow(DbHelper.COLUMN_TUE)),
                    cursor.getInt(cursor.getColumnIndexOrThrow(DbHelper.COLUMN_WED)),
                    cursor.getInt(cursor.getColumnIndexOrThrow(DbHelper.COLUMN_THU)),
                    cursor.getInt(cursor.getColumnIndexOrThrow(DbHelper.COLUMN_FRI)),
                    cursor.getInt(cursor.getColumnIndexOrThrow(DbHelper.COLUMN_SAT)),
                    cursor.getInt(cursor.getColumnIndexOrThrow(DbHelper.COLUMN_SUN))
            );
            hdoModels.add(hdoModel);
        }
        cursor.close();
        close();
        return hdoModels;
    }


    /**
     * Uloží HdoModel do databáze
     *
     * @param hdoModel HdoModel
     * @param table String jméno tabulky
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
     * @param table Tabulka se záznamy HDO
     */
    public void updateHdo(HdoModel hdoModel, String table) {
        open();
        database.update(table, createContentValues(hdoModel), DbHelper.COLUMN_ID + " = ?", new String[] {hdoModel.getId() + ""});
        close();
    }


    /**
     * Smaže HdoModel z databáze
     *
     * @param id long  id hdomodelu
     * @param table Tabulka se záznamy HDO
     */
    public void deleteHdo(long id, String table) {
        open();
        database.delete(table, DbHelper.COLUMN_ID + " = ?", new String[] {id + ""});
        close();
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
        values.put(DbHelper.COLUMN_TIME_FROM, hdoModel.getTimeFrom());
        values.put(DbHelper.COLUMN_TIME_UNTIL, hdoModel.getTimeUntil());
        values.put(DbHelper.COLUMN_MON, hdoModel.getMon());
        values.put(DbHelper.COLUMN_TUE, hdoModel.getTue());
        values.put(DbHelper.COLUMN_WED, hdoModel.getWed());
        values.put(DbHelper.COLUMN_THU, hdoModel.getThu());
        values.put(DbHelper.COLUMN_FRI, hdoModel.getFri());
        values.put(DbHelper.COLUMN_SAT, hdoModel.getSat());
        values.put(DbHelper.COLUMN_SUN, hdoModel.getSun());
        return values;
    }
}
