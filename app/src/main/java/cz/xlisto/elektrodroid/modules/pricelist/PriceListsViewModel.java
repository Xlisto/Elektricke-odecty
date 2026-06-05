package cz.xlisto.elektrodroid.modules.pricelist;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import cz.xlisto.elektrodroid.models.PriceListModel;


/**
 * ViewModel pro porovnávání ceníků.
 * Uchovává levý a pravý ceník spolu se vstupními parametry výpočtu.
 * Xlisto 01.03.2024 16:47
 */
public class PriceListsViewModel extends ViewModel {
    private final MutableLiveData<PriceListModel> priceListLeft = new MutableLiveData<>();
    private final MutableLiveData<PriceListModel> priceListRight = new MutableLiveData<>();
    private final MutableLiveData<ConsuptionContainer> consuptionContainer = new MutableLiveData<>();


    /**
     * Uloží vybraný ceník pro levou stranu porovnání.
     */
    public void setPriceListLeft(PriceListModel priceListLeft) {
        this.priceListLeft.setValue(priceListLeft);
    }


    /**
     * Uloží vybraný ceník pro pravou stranu porovnání.
     */
    public void setPriceListRight(PriceListModel priceListRight) {
        this.priceListRight.setValue(priceListRight);
    }

    /**
     * Uloží vstupní parametry porovnání ceníků.
     */
    public void setConsuptionContainer(ConsuptionContainer consuptionContainer) {
        this.consuptionContainer.setValue(consuptionContainer);
    }

    /**
     * @return LiveData s ceníkem pro levou stranu porovnání
     */
    public MutableLiveData<PriceListModel> getPriceListLeft() {
        return priceListLeft;
    }

    /**
     * @return LiveData s ceníkem pro pravou stranu porovnání
     */
    public MutableLiveData<PriceListModel> getPriceListRight() {
        return priceListRight;
    }

    /**
     * @return LiveData se vstupními parametry porovnání
     */
    public MutableLiveData<ConsuptionContainer> getConsuptionContainer() {
        return consuptionContainer;
    }
}
