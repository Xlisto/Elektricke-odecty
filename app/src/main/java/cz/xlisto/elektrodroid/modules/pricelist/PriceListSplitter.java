package cz.xlisto.elektrodroid.modules.pricelist;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import cz.xlisto.elektrodroid.models.PriceListModel;
import cz.xlisto.elektrodroid.ownview.ViewHelper;


/**
 * Pomocná třída poskytující statické metody pro rozdělení seznamu ceníků podle
 * pevně definovaných prahových dat (např. 30.06.2024 → 01.07.2024, 31.08.2025 → 01.09.2025).
 * Pro každý ceník, jehož platnost končí po prahu, vytvoří nový ceník s upravenou
 * hodnotou `cinnost` a platností od nového data; původní ceník je upraven tak, že
 * jeho `platnostDO` bude nastavena na hodnotu prahu.
 * <p>
 * Metody jsou statické a mají vedlejší efekt: modifikují objekty v předaném seznamu
 * (nastavení `platnostDO`) a vrací mapu, kde klíč je upravený původní ceník a hodnota
 * je nově vytvořený ceník.
 * <p>
 * Poznámky:
 * - Neprovádí hloubkové kopírování dalších referencí; očekává se, že konstruktor
 * `PriceListModel` vytvoří samostatnou instanci.
 * - Třída není instancovatelná a není určena pro paralelní volání bez synchronizace,
 * pokud stejný seznam/objekty mohou upravovat jiné vlákna.
 *
 * @see #splitPriceLists2024(ArrayList)
 * @see #splitPriceLists2025(ArrayList)
 */
public class PriceListSplitter {

    /**
     * Rozdělí ceníky použitím pevného prahu 30.06.2024 → 01.07.2024 a použije
     * hodnotu `cinnost` = 9.24 pro nově vytvořené záznamy.
     * <p>
     * Vedlejší efekty:
     * - U každého původního záznamu, jehož `platnostDO` je po prahu,
     * se nastaví `platnostDO` na hodnotu prahu.
     * - Vytvoří se nový `PriceListModel` s upraveným `cinnost` a `platnostOD`.
     * <p>
     * Parametry:
     * - priceLists: seznam ceníků, které mají být zkontrolovány (může být null nebo prázdný).
     * <p>
     * Návratová hodnota:
     * - mapa, kde klíč je upravený původní ceník a hodnota je nově vytvořený ceník.
     * <p>
     * Poznámky:
     * - Metoda deleguje logiku na `splitPriceListsGeneric`.
     */
    public static Map<PriceListModel, PriceListModel> splitPriceLists2024(ArrayList<PriceListModel> priceLists) {
        return splitPriceListsGeneric(priceLists, "30.06.2024", "01.07.2024", 9.24);
    }


    /**
     * Rozdělí ceníky použitím pevného prahu 31.08.2025 → 01.09.2025 a použije
     * hodnotu `cinnost` = 12.45 pro nově vytvořené záznamy.
     * <p>
     * Vedlejší efekty:
     * - U každého původního záznamu, jehož `platnostDO` je po prahu,
     * se nastaví `platnostDO` na hodnotu prahu.
     * - Vytvoří se nový `PriceListModel` s upraveným `cinnost` a `platnostOD`.
     * <p>
     * Parametry:
     * - priceLists: seznam ceníků, které mají být zkontrolovány (může být null nebo prázdný).
     * <p>
     * Návratová hodnota:
     * - mapa, kde klíč je upravený původní ceník a hodnota je nově vytvořený ceník.
     * <p>
     * Poznámky:
     * - Metoda deleguje logiku na `splitPriceListsGeneric`.
     */
    public static Map<PriceListModel, PriceListModel> splitPriceLists2025(ArrayList<PriceListModel> priceLists) {
        return splitPriceListsGeneric(priceLists, "31.08.2025", "01.09.2025", 12.45);
    }


    /**
     * Obecná pomocná metoda pro rozdělení seznamu ceníků podle zadaného prahu.
     * <p>
     * Popis:
     * - Parsuje vstupní datumy (`cutOffDateStr`, `newFromDateStr`) pomocí `ViewHelper`.
     * - Pro každý `PriceListModel` v `priceLists`, jehož `platnostDO` je větší než
     * parsovaný `cutOff`, se vytvoří nový `PriceListModel` s `platnostOD` = `newFrom`
     * a se změněnou hodnotou `cinnost` na `updatedCinnost`.
     * - Původní záznam se upraví tak, že jeho `platnostDO` je nastavena na `cutOff`.
     * <p>
     * Parametry:
     * - priceLists: seznam ceníků k zpracování (null nebo prázdný seznam vrací prázdnou mapu).
     * - cutOffDateStr: datum prahu (formát očekávaný `ViewHelper`).
     * - newFromDateStr: datum platnosti nového ceníku (formát očekávaný `ViewHelper`).
     * - updatedCinnost: hodnota `cinnost`, kterou má mít nově vytvořený ceník.
     * <p>
     * Návratová hodnota:
     * - mapa původní → nový ceník pro všechny záznamy, které byly rozděleny.
     * <p>
     * Vedlejší efekty a omezení:
     * - Metoda modifikuje instance v předaném seznamu (nastavuje `platnostDO` u původních záznamů).
     * - Neprovádí hloubkové kopírování referencí — očekává se, že konstruktor `PriceListModel`
     * vytvoří samostatnou instanci. Pokud tomu tak není, může dojít k sdílení referencí.
     * - Není bezpečná pro paralelní volání bez vnější synchronizace, pokud stejné objekty
     * mohou být upravovány z jiných vláken.
     * - Pokud `priceLists` je null nebo prázdný, vrací se prázdná mapa.
     */
    private static Map<PriceListModel, PriceListModel> splitPriceListsGeneric(ArrayList<PriceListModel> priceLists,
                                                                              String cutOffDateStr,
                                                                              String newFromDateStr,
                                                                              double updatedCinnost) {
        Map<PriceListModel, PriceListModel> splitPriceListMap = new HashMap<>();
        if (priceLists == null || priceLists.isEmpty()) {
            return splitPriceListMap;
        }

        long cutOff = ViewHelper.parseCalendarFromString(cutOffDateStr).getTimeInMillis();
        long newFrom = ViewHelper.parseCalendarFromString(newFromDateStr).getTimeInMillis();

        for (PriceListModel originalPriceList : priceLists) {
            if (originalPriceList != null && originalPriceList.getPlatnostDO() > cutOff) {
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
                        updatedCinnost,
                        originalPriceList.getPoze1(),
                        originalPriceList.getPoze2(),
                        originalPriceList.getOze(),
                        originalPriceList.getOte(),
                        newFrom,
                        originalPriceList.getPlatnostDO(),
                        originalPriceList.getDph(),
                        originalPriceList.getDistribuce(),
                        originalPriceList.getAutor(),
                        originalPriceList.getDatumVytvoreni(),
                        originalPriceList.getEmail()
                );

                originalPriceList.setPlatnostDO(cutOff);
                splitPriceListMap.put(originalPriceList, newPriceList);
            }
        }
        return splitPriceListMap;
    }

}
