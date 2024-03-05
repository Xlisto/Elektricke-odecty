package cz.xlisto.odecty.modules.pricelist;

import cz.xlisto.odecty.models.PriceListModel;


public interface SelectedPriceListsInterface {
    void onPriceListsSelected(PriceListModel priceListLeft, PriceListModel priceListRight, PriceListCompareBoxFragment.ConsuptionContainer consuptionContainer);
}
