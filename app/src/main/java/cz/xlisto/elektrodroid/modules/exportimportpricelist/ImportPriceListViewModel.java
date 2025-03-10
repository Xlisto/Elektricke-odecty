package cz.xlisto.elektrodroid.modules.exportimportpricelist;


import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.documentfile.provider.DocumentFile;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.ArrayList;

import cz.xlisto.elektrodroid.permission.Files;


/**
 * ViewModel pro importování ceníků.
 * <p>
 * Tato třída poskytuje metody pro načítání a správu souborů ceníků.
 * Obsahuje `LiveData` objekty pro sledování seznamu souborů a stavu načítání.
 */
public class ImportPriceListViewModel extends ViewModel {

    private static final String TAG = "ImportPriceListViewModel";
    private static final String[] filtersFileName = {".json"};
    private final MutableLiveData<ArrayList<DocumentFile>> documentFilesLiveData = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);

    //Handler pro načtení souborů
    private final Handler handlerLoadFile = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(@NonNull android.os.Message msg) {
            super.handleMessage(msg);
            setDocumentFiles((ArrayList<DocumentFile>) msg.obj);
            isLoading.postValue(false);
            handlerLoadFile.removeCallbacksAndMessages(null);
            handlerLoadFile.removeMessages(22);
        }
    };


    /**
     * Vrátí `LiveData` objekt obsahující seznam souborů dokumentů.
     * <p>
     * Tato metoda vrací `LiveData` objekt, který obsahuje aktuální seznam souborů dokumentů.
     * Tento seznam je aktualizován metodou `setDocumentFiles`.
     *
     * @return `LiveData` objekt obsahující seznam souborů dokumentů.
     */
    public LiveData<ArrayList<DocumentFile>> getDocumentFiles() {
        return documentFilesLiveData;
    }


    /**
     * Nastaví seznam souborů dokumentů.
     * <p>
     * Tato metoda aktualizuje `LiveData` objekt `documentFilesLiveData` s novým seznamem souborů dokumentů.
     *
     * @param files Seznam souborů dokumentů, které mají být nastaveny.
     */
    public void setDocumentFiles(ArrayList<DocumentFile> files) {
        documentFilesLiveData.postValue(files);
    }


    /**
     * Načte soubory z daného URI.
     * <p>
     * Tato metoda spustí načítání souborů z daného URI pomocí třídy `Files`.
     * Aktualizuje stav načítání na `true` a po dokončení načítání nastaví seznam souborů.
     *
     * @param activity   Aktivita, ze které je metoda volána.
     * @param uri        URI adresář, ze kterého se mají soubory načíst.
     * @param resultTree Spouštěč výsledků aktivity pro načítání souborů.
     */
    public void loadFiles(Activity activity, Uri uri, ActivityResultLauncher<Intent> resultTree) {
        isLoading.postValue(true);
        new Files().loadFiles(activity, uri, filtersFileName, handlerLoadFile, resultTree, 22);
    }


    /**
     * Vrátí `LiveData` objekt obsahující stav načítání.
     * <p>
     * Tato metoda vrací `LiveData` objekt, který obsahuje aktuální stav načítání.
     * Stav načítání je aktualizován metodou `loadFiles`.
     *
     * @return `LiveData` objekt obsahující stav načítání.
     */
    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

}
