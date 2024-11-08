package cz.xlisto.elektrodroid.modules.pricelist;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import cz.xlisto.elektrodroid.models.PriceListModel;
import cz.xlisto.elektrodroid.ownview.ViewHelper;


/**
 * Třída pro rozdělení seznamu ceníků podle data (1.7.2024) a vytvoření nové mapy s upravenými ceníky.
 */
public class PriceListSplitter {

    /**
     * Rozdělí seznam ceníků podle data a vytvoří novou mapu s upravenými ceníky.
     *
     * @param priceLists Seznam ceníků, které mají být rozděleny
     * @return Mapa obsahující dvojice původních a nových ceníků
     */
    public static Map<PriceListModel, PriceListModel> splitPriceLists(ArrayList<PriceListModel> priceLists) {
        Map<PriceListModel, PriceListModel> splitPriceListMap = new HashMap<>();

        for (PriceListModel originalPriceList : priceLists) {
            if (originalPriceList.getPlatnostDO() > ViewHelper.parseCalendarFromString("30.06.2024").getTimeInMillis()) {
                // Create a new price list with updated values
                PriceListModel newPriceList = new PriceListModel(
                        originalPriceList.getId(),
                        originalPriceList.getRada(),
                        originalPriceList.getProdukt(),
                        originalPriceList.getFirma(),
                        originalPriceList.getCenaVT(),
                        originalPriceList.getCenaNT(),
                        originalPriceList.getMesicniPlat(),
                        originalPriceList.getDan(),
                        originalPriceList.getSazba(),
                        originalPriceList.getDistVT(),
                        originalPriceList.getDistNT(),
                        originalPriceList.getJ0(),
                        originalPriceList.getJ1(),
                        originalPriceList.getJ2(),
                        originalPriceList.getJ3(),
                        originalPriceList.getJ4(),
                        originalPriceList.getJ5(),
                        originalPriceList.getJ6(),
                        originalPriceList.getJ7(),
                        originalPriceList.getJ8(),
                        originalPriceList.getJ9(),
                        originalPriceList.getJ10(),
                        originalPriceList.getJ11(),
                        originalPriceList.getJ12(),
                        originalPriceList.getJ13(),
                        originalPriceList.getJ14(),
                        originalPriceList.getSystemSluzby(),
                        9.24, // Updated value for cinnost
                        originalPriceList.getPoze1(),
                        originalPriceList.getPoze2(),
                        originalPriceList.getOze(),
                        originalPriceList.getOte(),
                        ViewHelper.parseCalendarFromString("01.07.2024").getTimeInMillis(),
                        originalPriceList.getPlatnostDO(),
                        originalPriceList.getDph(),
                        originalPriceList.getDistribuce(),
                        originalPriceList.getAutor(),
                        originalPriceList.getDatumVytvoreni(),
                        originalPriceList.getEmail()
                );

                // Update the original price list's end date
                originalPriceList.setPlatnostDO(ViewHelper.parseCalendarFromString("30.06.2024").getTimeInMillis());

                // Add the pair to the map
                splitPriceListMap.put(originalPriceList, newPriceList);
            }
        }

        return splitPriceListMap;
    }

}
