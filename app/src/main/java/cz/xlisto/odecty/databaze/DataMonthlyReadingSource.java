package cz.xlisto.odecty.databaze;

import android.content.Context;
import android.database.Cursor;

import java.util.ArrayList;

import cz.xlisto.odecty.models.MonthlyReadingModel;
import cz.xlisto.odecty.models.SubscriptionPointModel;
import cz.xlisto.odecty.ownview.ViewHelper;

import static cz.xlisto.odecty.databaze.DbHelper.COLUMN_ID;


/**
 * Přístup k databázi měsíčních odečtů
 * Xlisto 01.12.2023 9:40
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
            sb.append("Datum: ").append(ViewHelper.convertLongToDate(monthlyReading.getDate())).append("   VT: ")
                    .append(monthlyReading.getVt()).append("   NT: ").append(monthlyReading.getNt()).append("\n")
                    .append("\n")
                    .append("=============================================================================\n");
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
     * Zjistí jestli zadaný měsíční odečet je první nebo nikoliv
     *
     * @param table          název tabulky
     * @param monthlyReading porovnávaný měsíční odečet
     */
    public boolean isMonthlyReadingFirst(String table, MonthlyReadingModel monthlyReading) {
        String orderBy = DbHelper.DATUM + " ASC LIMIT 1";

        Cursor cursor = database.query(table,
                new String[]{DbHelper.DATUM},
                null,
                null,
                null,
                null,
                orderBy);
        if (cursor.getCount() == 0) return false;
        cursor.moveToFirst();
        long firstDate = cursor.getLong(0);
        cursor.close();
        return firstDate == monthlyReading.getDate();
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
}
