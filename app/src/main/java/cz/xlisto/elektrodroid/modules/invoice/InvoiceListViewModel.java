package cz.xlisto.elektrodroid.modules.invoice;


import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;


/**
 * ViewModel pro seznam faktur
 */
public class InvoiceListViewModel extends ViewModel {

    private static final String TAG = "InvoiceListViewModel";
    private final MutableLiveData<Boolean> isShovedDialog = new MutableLiveData<>();


    public void setShovedDialog(boolean isLoading) {
        isShovedDialog.setValue(isLoading);
    }


    public MutableLiveData<Boolean> isShowingDialog() {
        return isShovedDialog;
    }

}
