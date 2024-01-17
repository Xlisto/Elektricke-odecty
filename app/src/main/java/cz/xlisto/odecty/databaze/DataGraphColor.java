package cz.xlisto.odecty.databaze;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collections;


/**
 * Třída pro přístup k barvám grafu z databáze SQLite - tabulka nastaveni
 * Xlisto 23.10.2023 18:26
 */
public class DataGraphColor extends DataSource {
    private static final String TAG = "DataGraphColor";
    private static final String ARG_COLOR_HISTORY = "colorHistory";


    public DataGraphColor(Context context) {
        super.context = context;
        dbHelper = new DbHelper(context);
    }


    /**
     * Načte číselné hodnoty barvy z databáze
     *
     * @return int[] pole s barvami
     */
    public int[] loadColors() {
        String[] columns = new String[]{"colorVT", "colorNT"};
        int[] colors = new int[2];
        for (int i = 0; i < 2; i++) {
            Cursor cursor = database.query(DbHelper.TABLE_NAME_SETTINGS, null, DbHelper.JMENO + " = ?", new String[]{columns[i]}, null, null, null);
            if (cursor.getCount() == 0) {
                colors[i] = 101010;
                continue;
            }
            cursor.moveToFirst();
            colors[i] = (cursor.getInt(2));
            cursor.close();
        }
        return colors;
    }


    /**
     * Uloží číselné hodnoty barvy do databáze
     *
     * @param colors int[] pole s barvami
     */
    public void saveColors(int[] colors) {
        String[] columns = new String[]{"colorVT", "colorNT"};
        for (int i = 0; i < 2; i++) {
            ContentValues values = new ContentValues();
            values.put(DbHelper.HODNOTA, colors[i]);
            database.update(DbHelper.TABLE_NAME_SETTINGS, values, DbHelper.JMENO + " = ?", new String[]{columns[i]});
        }
    }


    /**
     * Uloží historii barev do databáze
     *
     * @param colorsList ArrayList s barvami
     */
    public void saveColorsHistory(ArrayList<String> colorsList) {
        String colors = buildString(colorsList);
        Log.w(TAG, "saveColorsHistory: " + colors);
        database.update(DbHelper.TABLE_NAME_SETTINGS, buildContentValues(colors), DbHelper.JMENO + " = ?", new String[]{ARG_COLOR_HISTORY});
    }


    /**
     * Načte historii barev z databáze. Pokud žádná není vytvoří výchozí list
     *
     * @return ArrayList s barvami
     */
    public ArrayList<String> loadColorsHistory() {
        ArrayList<String> colorsList = new ArrayList<>();
        String[] columns = new String[]{ARG_COLOR_HISTORY};
        Cursor cursor = database.query(DbHelper.TABLE_NAME_SETTINGS, null, DbHelper.JMENO + " = ?", new String[]{columns[0]}, null, null, null);
        cursor.moveToFirst();
        if (cursor.getCount() == 0) {
            colorsList.add("#FB8B24;#9A031E");
            colorsList.add("#06D6A0;#1B9AAA");
            colorsList.add("#00A6FB;#DA2C38");
            String colors = buildString(colorsList);
            database.insert(DbHelper.TABLE_NAME_SETTINGS, null, buildContentValues(colors));

        } else {
            String[] colors = cursor.getString(2).split(":");
            Collections.addAll(colorsList, colors);
        }
        Log.w(TAG, "loadColorsHistory: " + cursor.getCount());
        cursor.close();
        return colorsList;

    }


    /**
     * Vytvoří řetězec z ArrayListu s barvami
     *
     * @param colors ArrayList s barvami
     * @return String s barvami
     */
    private String buildString(ArrayList<String> colors) {
        StringBuilder colorsString = new StringBuilder();
        for (String color : colors) {
            colorsString.append(color).append(":");
        }
        return colorsString.toString();
    }


    /**
     * Vytvoří ContentValues pro uložení do databáze
     *
     * @param value String s barvami
     * @return ContentValues
     */
    private ContentValues buildContentValues(String value) {
        ContentValues values = new ContentValues();
        values.put(DbHelper.JMENO, ARG_COLOR_HISTORY);
        values.put(DbHelper.HODNOTA, value);
        return values;
    }
}
