package cz.xlisto.odecty.databaze;

import android.content.Context;
import android.database.Cursor;

import java.util.ArrayList;

import cz.xlisto.odecty.models.MonthlyReadingModel;
import cz.xlisto.odecty.models.SubscriptionPointModel;
import cz.xlisto.odecty.ownview.ViewHelper;

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
            MonthlyReadingModel monthlyReading = loadLastMonthlyReading(table);
            sb.append("Datum: ").append(ViewHelper.convertLongToDate(monthlyReading.getDate())).append("   VT: ")
                    .append(monthlyReading.getVt()).append("   NT: ").append(monthlyReading.getNt()).append("\n")
                    .append("\n")
                    .append("=============================================================================\n");
        }

        return sb.toString();
    }


    /**
     * Načte poslední měsíční odečet
     *
     * @param table databázová tabulka s měsíčními odečty
     * @return objekt MonthlyReadingModel - měsíční odečet
     */
    private MonthlyReadingModel loadLastMonthlyReading(String table) {
        String orderBy = DbHelper.DATUM + " DESC LIMIT 1";

        Cursor cursor = database.query(table,
                null,
                null,
                null,
                null,
                null,
                orderBy);


        cursor.moveToFirst();
        MonthlyReadingModel monthlyReadingModel = createMonthlyReading(cursor);
        cursor.close();
        return monthlyReadingModel;
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