package cz.xlisto.elektrodroid.modules.monthlyreading;


import android.content.Context;

import androidx.documentfile.provider.DocumentFile;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import cz.xlisto.elektrodroid.models.PriceListModel;
import cz.xlisto.elektrodroid.modules.backup.GoogleDriveService;


/**
 * ViewModel pro správu měsíčních odečtů.
 */
public class MonthlyReadingViewModel extends ViewModel {

    private final MutableLiveData<Boolean> uploadResult = new MutableLiveData<>();
    private final MutableLiveData<Boolean> showingProgressBar = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isChangeMeter = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isFirst = new MutableLiveData<>();
    private final MutableLiveData<PriceListModel> selectedPriceList = new MutableLiveData<>();
    private final MutableLiveData<MonthlyReadingWidgetContainer> widgetContainer = new MutableLiveData<>();


    public MonthlyReadingViewModel() {
        isChangeMeter.setValue(false);
        isFirst.setValue(false);
        selectedPriceList.setValue(new PriceListModel());

    }


    /**
     * Vrací LiveData objekt pro výsledek nahrávání souboru.
     *
     * @return LiveData<Boolean> objekt
     */
    public LiveData<Boolean> getUploadResult() {
        return uploadResult;
    }


    /**
     * Nahraje soubor na Google Drive.
     *
     * @param context     Kontext aplikace
     * @param backupFile  Soubor k nahrání
     * @param accountName Název účtu Google
     * @param folderId    ID složky na Google Drive
     */
    public void uploadFileToGoogleDrive(Context context, DocumentFile backupFile, String accountName, String folderId) {
        GoogleDriveService googleDriveService = new GoogleDriveService(context, accountName, folderId);
        googleDriveService.setOnDriveServiceListener(() -> {
            boolean result = googleDriveService.uploadFile(backupFile, folderId);
            uploadResult.postValue(result);
        });
    }


    /**
     * Vrací LiveData objekt pro zobrazení progressbaru.
     *
     * @return LiveData<Boolean> objekt
     */
    public LiveData<Boolean> getShowingProgressBar() {
        return showingProgressBar;
    }


    /**
     * Zobrazí progress bar
     */
    public void showProgressBar() {
        showingProgressBar.postValue(true);
    }


    /**
     * Nastaví hodnotu indikující, zda se jedná o výměnu elektroměru.
     *
     * @param isChangeMeter boolean hodnota, která určuje, zda se jedná o výměnu elektroměru
     */
    public void setIsChangeMeter(boolean isChangeMeter) {
        this.isChangeMeter.postValue(isChangeMeter);
    }


    /**
     * Nastaví hodnotu indikující, zda se jedná o první načtení.
     *
     * @param isFirstLoad boolean hodnota, která určuje, zda se jedná o první načtení
     */
    public void setIsFirstLoad(boolean isFirstLoad) {
        this.isFirst.postValue(isFirstLoad);
    }


    /**
     * Nastaví vybraný ceník.
     *
     * @param priceList ceník
     */
    public void setSelectedPriceList(PriceListModel priceList) {
        selectedPriceList.postValue(priceList);
    }


    /**
     * Nastaví objekt s aktualními hodnotami widgetů.
     * @param monthlyReadingWidgetContainer objekt s hodnotami widgetů
     */
    public void setWidgetContainer(MonthlyReadingWidgetContainer monthlyReadingWidgetContainer) {
        this.widgetContainer.postValue(monthlyReadingWidgetContainer);
    }


    /**
     * Vrací LiveData objekt, který indikuje, zda se jedná o výměnu elektroměru.
     *
     * @return LiveData<Boolean> objekt
     */
    public LiveData<Boolean> getChangeMeter() {
        return isChangeMeter;
    }


    /**
     * Vrací LiveData objekt, který indikuje, zda se jedná o první načtení.
     *
     * @return LiveData<Boolean> objekt
     */
    public LiveData<Boolean> getIsFirst() {
        return isFirst;
    }


    /**
     * Vrací LiveData objekt s vybraným ceníkem
     *
     * @return LiveData<PriceListModel> objekt ceníku
     */
    public LiveData<PriceListModel> getSelectedPriceList() {
        return selectedPriceList;
    }


    /**
     * Vrací LiveData objekt s hodnotami widgetů
     * @return LiveData<WidgetContainer> objekt s hodnotami widgetů
     */
    public LiveData<MonthlyReadingWidgetContainer> getWidgetContainer() {
        return widgetContainer;
    }

}
