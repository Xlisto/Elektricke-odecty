package cz.xlisto.elektrodroid.modules.pricelist;


import cz.xlisto.elektrodroid.models.PriceListModel;


public interface SelectedPriceListsInterface {
    void onPriceListsSelected(PriceListModel priceListLeft, PriceListModel priceListRight, PriceListCompareBoxFragment.ConsuptionContainer consuptionContainer);
}
