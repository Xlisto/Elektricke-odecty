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
 * Pomocná třída pro hromadnou aktualizaci měsíčních odečtů podle nově
 * rozdělených ceníků.
 * <p>
 * Účel:
 * - Provede pro všechna odběrná místa aktualizaci záznamů {@code MonthlyReadingModel},
 * které spadají do platnosti zadaného data {@code newStart}, a nahradí v nich
 * odkaz na starý ceník novým ceníkem podle mapy {@code mapPriceLists}.
 * <p>
 * Hlavní chování a vedlejší efekty:
 * - Pro každé odběrné místo načte odpovídající záznamy z DB pomocí
 * {@code DataMonthlyReadingSource}, aktualizuje je a uloží zpět do DB.
 * - Po zpracování odběrného místa volá {@code WithOutInvoiceService.updateInvoice}
 * pokud je {@code mapPriceLists} neprázdná.
 * - Změní ID ceníků v modelech měsíčních odečtů (trvalá změna v databázi).
 * <p>
 * Implementační poznámky:
 * - Všechny metody jsou statické; třída je bezinstanční a nekontroluje vláknování.
 * Volající zodpovídá za spuštění v pozadí, pokud to vyžaduje UI nebo dlouhý běh.
 * - Metoda může vyhodit {@code RuntimeException} při chybách čtení/zápisu nebo
 * při selhání parsování data (viz {@link cz.xlisto.elektrodroid.ownview.ViewHelper}).
 *
 * @see #updateMonthlyReadings(Context, Map, String)
 * @see cz.xlisto.elektrodroid.databaze.DataMonthlyReadingSource
 * @see cz.xlisto.elektrodroid.ownview.ViewHelper#parseCalendarFromString(String)
 * @see cz.xlisto.elektrodroid.modules.invoice.WithOutInvoiceService#updateInvoice(Context, cz.xlisto.elektrodroid.models.SubscriptionPointModel)
 */
public class MonthlyReadingUpdater {

    /**
     * Aktualizuje měsíční odečty pro všechna odběrná místa podle nově rozdělených ceníků.
     * <p>
     * Metoda načte všechna odběrná místa, pro každé otevře zdroj {@code DataMonthlyReadingSource},
     * převede {@code newStart} pomocí {@link ViewHelper#parseCalendarFromString} na čas v ms,
     * načte odpovídající záznamy {@link MonthlyReadingModel}, nahradí v nich odkaz na ceník podle
     * mapy {@code mapPriceLists} a provede aktualizaci v databázi. Po zpracování zdroj uzavře.
     * Pokud je mapa ceníků neprázdná, zavolá {@link WithOutInvoiceService#updateInvoice} pro dané odběrné místo.
     * <p>
     * Vedlejší efekty: zápisy do databáze (update) a případná aktualizace faktury; mění id ceníků v modelech čtení.
     *
     * @param context       kontext aplikace
     * @param mapPriceLists mapa přiřazení původní {@link PriceListModel} → nový {@link PriceListModel}
     * @param newStart      datum začátku platnosti nového ceníku (řetězec parsovatelný {@link ViewHelper#parseCalendarFromString}, např. "1.7.2024")
     * @throws RuntimeException při chybě čtení nebo zápisu do databáze nebo při selhání parsování data
     */
    public static void updateMonthlyReadings(Context context, Map<PriceListModel, PriceListModel> mapPriceLists, String newStart) {
        ArrayList<SubscriptionPointModel> subscriptionPoints = SubscriptionPoint.getAllSubscriptionPoints(context);

        //všechna odběrná místa
        for (SubscriptionPointModel subscriptionPoint : subscriptionPoints) {
            String tableO = subscriptionPoint.getTableO();
            long date = ViewHelper.parseCalendarFromString(newStart).getTimeInMillis();
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
