package cz.xlisto.elektrodroid.modules.backup;


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


public class BackupViewModel extends ViewModel {

    private static final String TAG = "BackupViewModel";
    private final MutableLiveData<ArrayList<DocumentFile>> documentFilesLiveData = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);

    //Handler pro načtení souborů
    private final Handler handlerLoadFile = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(@NonNull android.os.Message msg) {
            super.handleMessage(msg);
            setDocumentFiles((ArrayList<DocumentFile>) msg.obj);
            handlerLoadFile.removeCallbacksAndMessages(null);
            handlerLoadFile.removeMessages(1);
            isLoading.postValue(false);
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
     * Načte soubory z určeného URI.
     * <p>
     * Tato metoda nastaví stav načítání na true a zavolá metodu `loadFiles` třídy `Files`,
     * která načte soubory z daného URI pomocí zadaného `ActivityResultLauncher`.
     *
     * @param activity   Aktivita, ze které je metoda volána.
     * @param uri        URI složky, ze které se mají načíst soubory.
     * @param resultTree Launcher pro výsledek aktivity, který se použije pro načtení souborů.
     */
    public void loadFiles(Activity activity, Uri uri, ActivityResultLauncher<Intent> resultTree) {
        isLoading.postValue(true);
        new Files().loadFiles(activity, uri, RecoverData.getFiltersFileName(), handlerLoadFile, resultTree, 1);
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
