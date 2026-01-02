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


/**
 * ViewModel pro správu záloh a načítání souborů.
 *
 * <p>Spravuje stav a data pro UI při práci se zálohami:
 * - poskytuje {@code LiveData<ArrayList<DocumentFile>} s nalezenými soubory,
 * - poskytuje {@code LiveData<Boolean>} indikující, zda probíhá načítání,
 * - poskytuje {@code LiveData<Boolean>} s informací o oprávnění k adresáři.</p>
 *
 * <p>Načítání souborů probíhá asynchronně přes {@code Files.loadFiles} a interní
 * {@code Handler}, výsledky jsou publikovány do {@code documentFilesLiveData}.
 * Kontrola oprávnění je dostupná přes {@code checkPermission}.</p>
 */
public class BackupViewModel extends ViewModel {

    private static final String TAG = "BackupViewModel";
    private static final int MSG_LOAD_FILES = 11;
    private final MutableLiveData<ArrayList<DocumentFile>> documentFilesLiveData = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> hasPermission = new MutableLiveData<>(false);

    /**
     * Handler běžící na hlavním vlákně (Looper.getMainLooper()) pro zpracování výsledků
     * asynchronního načítání souborů.
     * <p>
     * Očekává, že `msg.obj` bude instance `ArrayList<DocumentFile>`. Po přijetí zprávy:
     * - publikujeme výsledky voláním `setDocumentFiles(...)`,
     * - odstraníme všechny naplánované callbacky a zprávy z tohoto handleru,
     * - odstraníme případné další zprávy se stejným kódem definovaným v `MSG_LOAD_FILES`,
     * - nastavíme `isLoading` na `false` (pomocí `postValue`).
     * <p>
     * Poznámka: protože handler běží na hlavním vlákně, jsou aktualizace `LiveData`
     * bezpečné. Pokud `msg.obj` nemá očekávaný typ, může dojít k `ClassCastException`.
     */
    private final Handler handlerLoadFile = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(@NonNull android.os.Message msg) {
            super.handleMessage(msg);
            setDocumentFiles((ArrayList<DocumentFile>) msg.obj);
            handlerLoadFile.removeCallbacksAndMessages(null);
            handlerLoadFile.removeMessages(MSG_LOAD_FILES);
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
        new Files().loadFiles(activity, uri, RecoverData.getFiltersFileName(), handlerLoadFile, resultTree, MSG_LOAD_FILES);
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


    /**
     * Vrátí `LiveData<Boolean>` indikující, zda aplikace má oprávnění k vybranému URI.
     *
     * <p>Hodnota je lifecycle\-aware a měla by být pozorována z UI komponent (Activity/Fragment).
     * Aktualizuje se voláním `checkPermission` a poskytuje bezpečný způsob, jak reagovat na změny oprávnění.</p>
     *
     * @return `LiveData<Boolean>` s informací o oprávnění k adresáři.
     */
    public LiveData<Boolean> getHasPermission() {
        return hasPermission;
    }


    /**
     * Zkontroluje oprávnění aplikace k zadanému adresáři (URI) a aktualizuje
     * interní `LiveData<Boolean>` `hasPermission`.
     *
     * <p>Metoda používá `Files.permissions(Activity, Uri)` pro ověření přístupu.
     * Výsledek (true = má oprávnění, false = nemá) je nastaven pomocí
     * `hasPermission.setValue(...)`, proto by měla být volána z UI vlákna
     * nebo z lifecycle-aware kontextu (Activity/Fragment).</p>
     *
     * @param activity Activity použitá pro kontrolu oprávnění
     * @param uri      URI adresáře, jehož oprávnění se kontroluje
     */
    public void checkPermission(Activity activity, Uri uri) {
        boolean ok = Files.permissions(activity, uri);
        hasPermission.setValue(ok);
    }

}
