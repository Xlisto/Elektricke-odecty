package cz.xlisto.elektrodroid.databaze;

import android.content.Context;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

/**
 * Abstraktní třída pro přístup k databázi odečtů, odběrných míst atd.
 * Xlisto 26.05.2023 17:20
 */
public abstract class DataSource {
    private static final String TAG = "DataSource";
    Context context;
    SQLiteDatabase database;
    DbHelper dbHelper;


    /**
     * Otevře spojení s databází odečtů, odběrných míst atd.
     *
     * @throws SQLException vyjímka při chybě při otevírání databáze
     */
    public void open() throws SQLException {
        if (database != null) {
            return;
        }

        try {
            database = dbHelper.getWritableDatabase();
        } catch (NullPointerException e) {
            e.printStackTrace();
            dbHelper = new DbHelper(context);
            database = dbHelper.getWritableDatabase();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    /**
     * Zavře spojení s databází ceníku
     */
    public void close() {
        if (dbHelper != null) {
            database.close();
        }
    }

}
