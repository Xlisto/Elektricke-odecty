package cz.xlisto.elektrodroid.modules.monthlyreading;


import android.content.Context;

import java.util.ArrayList;
import java.util.Map;

import cz.xlisto.elektrodroid.databaze.DataMonthlyReadingSource;
import cz.xlisto.elektrodroid.models.MonthlyReadingModel;
import cz.xlisto.elektrodroid.models.PriceListModel;
import cz.xlisto.elektrodroid.models.SubscriptionPointModel;
import cz.xlisto.elektrodroid.modules.invoice.WithOutInvoiceService;
import cz.xlisto.elektrodroid.ownview.ViewHelper;
import cz.xlisto.elektrodroid.utils.SubscriptionPoint;

/**
 * Třída MonthlyReadingUpdater poskytuje metody pro aktualizaci měsíčních odečtů
 * a ceníků v automatické faktuře pro všechna odběrná místa.
 */
public class MonthlyReadingUpdater {

    /**
     * Aktualizuje ceníky rozdělené k 1.7.2024 v měsíčních odečtech, v automatické faktuře (režim auto) pro všechna odběrná místa.
     *
     * @param context       Kontext aplikace
     * @param mapPriceLists Mapa starých a nových ceníků
     */
    public static void updateMonthlyReadings(Context context, Map<PriceListModel, PriceListModel> mapPriceLists) {
        ArrayList<SubscriptionPointModel> subscriptionPoints = SubscriptionPoint.getAllSubscriptionPoints(context);

        //všechna odběrná místa
        for (SubscriptionPointModel subscriptionPoint : subscriptionPoints) {
            String tableO = subscriptionPoint.getTableO();
            long date = ViewHelper.parseCalendarFromString("1.07.2024").getTimeInMillis();
            DataMonthlyReadingSource dataMonthlyReadingSource = new DataMonthlyReadingSource(context);
            dataMonthlyReadingSource.open();
            ArrayList<MonthlyReadingModel> monthlyReadings = dataMonthlyReadingSource.loadMonthlyReading(tableO, date);

            for (MonthlyReadingModel reading : monthlyReadings) {
                PriceListModel newPriceList = findPriceListByOldId(mapPriceLists, reading.getPriceListId());
                if (newPriceList != null) {
                    reading.setPriceListId(newPriceList.getId());
                }
                dataMonthlyReadingSource.updateMonthlyReading(reading, reading.getId(), tableO);
            }
            dataMonthlyReadingSource.close();
            if (!mapPriceLists.isEmpty())
                WithOutInvoiceService.updateInvoice(context, subscriptionPoint);
        }

    }


    /**
     * Najde nový ceník podle starého ID ceníku.
     *
     * @param map Mapa starých a nových ceníků
     * @param id  ID starého ceníku
     * @return Nový ceník nebo null, pokud nebyl nalezen
     */
    private static PriceListModel findPriceListByOldId(Map<PriceListModel, PriceListModel> map, long id) {
        for (Map.Entry<PriceListModel, PriceListModel> entry : map.entrySet()) {
            if (entry.getKey().getId() == id) {
                return entry.getValue();
            }
        }
        return null;
    }

}
