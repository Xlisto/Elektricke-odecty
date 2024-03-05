package cz.xlisto.odecty.modules.pricelist;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import cz.xlisto.odecty.models.PriceListModel;


/**
 * ViewModel pro porovnávání ceníků
 * Xlisto 01.03.2024 16:47
 */
public class PriceListsViewModel extends ViewModel {
    private static final String TAG = "PriceListsViewModel";
    private final MutableLiveData<PriceListModel> priceListLeft = new MutableLiveData<>();
    private final MutableLiveData<PriceListModel> priceListRight = new MutableLiveData<>();
    private final MutableLiveData<PriceListCompareBoxFragment.ConsuptionContainer> consuptionContainer = new MutableLiveData<>();


    public void setPriceListLeft(PriceListModel priceListLeft) {
        this.priceListLeft.setValue(priceListLeft);
    }


    public void setPriceListRight(PriceListModel priceListRight) {
        this.priceListRight.setValue(priceListRight);
    }

    public void setConsuptionContainer(PriceListCompareBoxFragment.ConsuptionContainer consuptionContainer) {
        this.consuptionContainer.setValue(consuptionContainer);
    }

    public MutableLiveData<PriceListModel> getPriceListLeft() {
        return priceListLeft;
    }

    public MutableLiveData<PriceListModel> getPriceListRight() {
        return priceListRight;
    }

    public MutableLiveData<PriceListCompareBoxFragment.ConsuptionContainer> getConsuptionContainer() {
        return consuptionContainer;
    }
}
