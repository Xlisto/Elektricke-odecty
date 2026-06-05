package cz.xlisto.elektrodroid.modules.pricelist;


import cz.xlisto.elektrodroid.models.PriceListModel;


/**
 * Callback rozhraní pro předání vybraných ceníků a parametrů porovnání.
 */
public interface SelectedPriceListsInterface {
    /**
     * Předá aktuálně vybrané ceníky a vstupní parametry spotřeby.
     *
     * @param priceListLeft        ceník na levé straně porovnání
     * @param priceListRight       ceník na pravé straně porovnání
     * @param consuptionContainer  vstupní parametry výpočtu
     */
    void onPriceListsSelected(PriceListModel priceListLeft, PriceListModel priceListRight, ConsuptionContainer consuptionContainer);
}
