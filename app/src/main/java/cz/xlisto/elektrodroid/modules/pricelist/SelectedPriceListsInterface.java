package cz.xlisto.elektrodroid.modules.pricelist;


import cz.xlisto.elektrodroid.models.PriceListModel;


/**
 * Rozhraní pro předání vybraných ceníků a parametrů spotřeby při jejich porovnávání.
 * Slouží k mezigrafické komunikaci mezi komponentami porovnání ceníků.
 */
public interface SelectedPriceListsInterface {

    /**
     * Voláno při výběru nebo změně porovnávaných ceníků a parametrů spotřeby.
     *
     * @param priceListLeft       první (levý) {@link PriceListModel} určený k porovnání
     * @param priceListRight      druhý (pravý) {@link PriceListModel} určený k porovnání
     * @param consuptionContainer kontejner {@link ConsuptionContainer} se vstupními parametry spotřeby pro výpočet
     */
    void onPriceListsSelected(PriceListModel priceListLeft, PriceListModel priceListRight, ConsuptionContainer consuptionContainer);
}
