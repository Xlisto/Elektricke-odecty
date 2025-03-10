package cz.xlisto.elektrodroid.databaze;


import static cz.xlisto.elektrodroid.databaze.DbHelper.CENIK_ID;
import static cz.xlisto.elektrodroid.databaze.DbHelper.COLUMN_ID;
import static cz.xlisto.elektrodroid.databaze.DbHelper.DATUM;
import static cz.xlisto.elektrodroid.databaze.DbHelper.GARANCE;
import static cz.xlisto.elektrodroid.databaze.DbHelper.NT;
import static cz.xlisto.elektrodroid.databaze.DbHelper.POZNAMKA;
import static cz.xlisto.elektrodroid.databaze.DbHelper.PRVNI_ODECET;
import static cz.xlisto.elektrodroid.databaze.DbHelper.VT;
import static cz.xlisto.elektrodroid.databaze.DbHelper.ZAPLACENO;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;

import java.util.ArrayList;

import cz.xlisto.elektrodroid.models.MonthlyReadingModel;
import cz.xlisto.elektrodroid.models.SubscriptionPointModel;
import cz.xlisto.elektrodroid.ownview.ViewHelper;


/**
 * Třída pro přístup k databázi měsíčních odečtů.
 * <p>
 * Tato třída rozšiřuje třídu `DataSource` a poskytuje metody pro práci s měsíčními odečty v databázi.
 */
public class DataMonthlyReadingSource extends DataSource {

    private static final String TAG = "DataMonthlyReadingSource";


    public DataMonthlyReadingSource(Context context) {
        super.context = context;
        dbHelper = new DbHelper(context);
    }


    /**
     * Vrátí poslední měsíční odečet jako text
     *
     * @return poslední měsíční odečet jako text
     */
    public String getLastMonthlyReadingAsText() {
        //načtení seznamu odběrných míst
        DataSubscriptionPointSource dataSubscriptionPointSource = new DataSubscriptionPointSource(context);
        dataSubscriptionPointSource.open();
        ArrayList<SubscriptionPointModel> subscriptionPointModelArrayList = dataSubscriptionPointSource.loadSubscriptionPoints();
        dataSubscriptionPointSource.close();

        StringBuilder sb = new StringBuilder();

        //seznam tabulek s měsíčními odečty
        for (SubscriptionPointModel subscriptionPointModel : subscriptionPointModelArrayList) {
            String table = subscriptionPointModel.getTableO();

            sb.append(subscriptionPointModel.getName()).append("\n")
                    .append("Poznámka: ").append(subscriptionPointModel.getDescription()).append("\n")
                    .append("Číslo elektroměru: ").append(subscriptionPointModel.getNumberElectricMeter())
                    .append("   Číslo odběrného místa: ").append(subscriptionPointModel.getNumberSubscriptionPoint()).append("\n")
                    .append("POSLEDNÍ ZAPSANÝ ODEČET:\n");

            //načtení posledního měsíčního odečtu
            MonthlyReadingModel monthlyReading = loadLastMonthlyReadingByDate(table);
            if (monthlyReading == null) {
                sb.append("Není zapsán žádný měsíční odečet\n");
            } else {
                sb.append("Datum: ").append(ViewHelper.convertLongToDate(monthlyReading.getDate())).append("   VT: ")
                        .append(monthlyReading.getVt()).append("   NT: ").append(monthlyReading.getNt()).append("\n")
                        .append("\n");
            }
            sb.append("=============================================================================\n");
        }

        return sb.toString();
    }


    /**
     * Načte poslední záznam VT a NT
     *
     * @param table název tabulky
     * @return pole s VT a NT
     */
    public double[] loadLastVtNt(String table) {
        String orderBy = DbHelper.DATUM + " DESC LIMIT 1";

        Cursor cursor = database.query(table,
                new String[]{DbHelper.VT, DbHelper.NT},
                null,
                null,
                null,
                null,
                orderBy);
        if (cursor.getCount() == 0) return new double[]{0, 0};
        cursor.moveToFirst();
        double vt = cursor.getDouble(0);
        double nt = cursor.getDouble(1);
        cursor.close();
        return new double[]{vt, nt};
    }


    /**
     * Načte měsíční záznam podle ID
     */
    public MonthlyReadingModel loadMonthlyReadingById(String table, long id) {
        Cursor cursor = database.query(table,
                null,
                COLUMN_ID + "=?",
                new String[]{String.valueOf(id)},
                null,
                null,
                null);
        if (cursor.getCount() == 0) return null;
        cursor.moveToFirst();
        MonthlyReadingModel monthlyReadingModel = createMonthlyReading(cursor);
        cursor.close();
        return monthlyReadingModel;
    }


    /**
     * Zjistí počet záznamů v tabulce s měsíčními odečty
     *
     * @param table název tabulky
     * @return počet záznamů
     */
    public int getCount(String table) {
        String[] columns = {"count(*)"};
        Cursor cursor = database.query(table,
                columns,
                null,
                null,
                null,
                null,
                null);
        cursor.moveToFirst();
        int count = cursor.getInt(0);
        cursor.close();
        return count;
    }


    /**
     * Načte poslední měsíční odečet podle data
     *
     * @param table databázová tabulka s měsíčními odečty
     * @return objekt MonthlyReadingModel - měsíční odečet
     */
    public MonthlyReadingModel loadLastMonthlyReadingByDate(String table) {
        String orderBy = DbHelper.DATUM + " DESC LIMIT 1";

        Cursor cursor = database.query(table,
                null,
                null,
                null,
                null,
                null,
                orderBy);

        if (cursor.getCount() == 0) return null;
        cursor.moveToFirst();
        MonthlyReadingModel monthlyReadingModel = createMonthlyReading(cursor);
        cursor.close();
        return monthlyReadingModel;
    }


    /**
     * Načte měsíční odečty v období od poslední faktury do konce měsíce
     *
     * @param table název tabulky s měsíčními odečty
     * @return seznam měsíčních odečtů
     */
    public ArrayList<MonthlyReadingModel> loadMonthlyReading(String table, long date) {
        String orderBy = DbHelper.DATUM + " ASC";
        ArrayList<MonthlyReadingModel> monthlyReadingModels = new ArrayList<>();
        Cursor cursor = database.query(table,
                null,
                DbHelper.DATUM + " > ?",
                new String[]{String.valueOf(date)},
                null,
                null,
                orderBy);
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            MonthlyReadingModel monthlyReadingModel = createMonthlyReading(cursor);
            monthlyReadingModels.add(monthlyReadingModel);
            cursor.moveToNext();
        }
        cursor.close();
        return monthlyReadingModels;
    }


    /**
     * Vloží měsíční odečet do databáze
     *
     * @param tableName      název tabulky
     * @param monthlyReading měsíční odečet
     * @return id záznamu
     */
    public long insertMonthlyReading(String tableName, MonthlyReadingModel monthlyReading) {
        return database.insert(tableName, null, createContentValue(monthlyReading));
    }

    /**
     * Smaže měsíční odečet podle ID
     *
     * @param itemId id měsíčního odečtu
     * @param table  název tabulky
     */
    public void deleteMonthlyReading(long itemId, String table) {
        database.delete(table, COLUMN_ID + "=?",
                new String[]{String.valueOf(itemId)});
    }


    /**
     * Aktualizuje měsíční odečet v databázi.
     *
     * @param monthlyReading objekt MonthlyReadingModel obsahující aktualizovaná data
     * @param itemId         id měsíčního odečtu, který má být aktualizován
     * @param tableName      název tabulky, ve které se má aktualizace provést
     */
    public void updateMonthlyReading(MonthlyReadingModel monthlyReading, long itemId, String tableName) {
        database.update(tableName, createContentValue(monthlyReading),
                COLUMN_ID + "=?", new String[]{String.valueOf(itemId)});

    }


    /**
     * Vytvoří objekt měsíčního odečtu z kurzoru
     *
     * @param cursor kurzor
     * @return měsíční odečet
     */
    private MonthlyReadingModel createMonthlyReading(Cursor cursor) {
        long id = cursor.getLong(0);
        double vt = cursor.getDouble(1);
        double nt = cursor.getDouble(2);
        long priceListId = cursor.getLong(3);
        long date = cursor.getLong(4);
        int firstReading = cursor.getInt(5);
        double payment = cursor.getDouble(6);
        String description = cursor.getString(7);
        double otherServices = cursor.getDouble(8);
        return new MonthlyReadingModel(
                id,
                date,
                vt, nt,
                payment,
                description,
                otherServices,
                priceListId,
                firstReading == 1);
    }


    /**
     * Sestaví data měsíčního odečtu pro zápis do databáze
     *
     * @param monthlyReading měsíční odečet
     * @return data měsíčního odečtu
     */
    private ContentValues createContentValue(MonthlyReadingModel monthlyReading) {
        ContentValues values = new ContentValues();
        values.put(VT, monthlyReading.getVt());
        values.put(NT, monthlyReading.getNt());
        values.put(ZAPLACENO, monthlyReading.getPayment());
        values.put(CENIK_ID, monthlyReading.getPriceListId());
        values.put(DATUM, monthlyReading.getDate());
        values.put(PRVNI_ODECET, monthlyReading.isChangeMeter());
        values.put(GARANCE, monthlyReading.getOtherServices());
        values.put(POZNAMKA, monthlyReading.getDescription());
        return values;
    }

}
