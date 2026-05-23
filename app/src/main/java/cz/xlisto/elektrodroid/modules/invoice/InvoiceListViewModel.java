package cz.xlisto.elektrodroid.modules.invoice;


import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;


/**
 * ViewModel pro seznam faktur
 */
public class InvoiceListViewModel extends ViewModel {

    private final MutableLiveData<Boolean> isShovedDialog = new MutableLiveData<>();


    /**
     * Nastaví příznak, zda je aktuálně zobrazen dialog.
     *
     * @param isLoading {@code true} pokud je dialog zobrazen, jinak {@code false}
     */
    public void setShovedDialog(boolean isLoading) {
        isShovedDialog.setValue(isLoading);
    }

}
