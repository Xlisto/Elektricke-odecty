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
 * Poskytuje lifecycle\-aware stav a data pro UI komponenty při načítání souborů ceníků
 * z vybraného adresáře (URI). Hlavní odpovědnosti:
 * - Udržovat a vystavovat seznam nalezených souborů jako `LiveData<ArrayList<DocumentFile>>`.
 * - Indikovat stav načítání přes `LiveData<Boolean> isLoading`.
 * - Informovat o dostupnosti oprávnění k vybranému adresáři přes `LiveData<Boolean> hasPermission`.
 * <p>
 * Implementační poznámky:
 * - Asynchronní načítání souborů je prováděno pomocí pomocné třídy `Files` a `Handleru`,
 * výsledky jsou publikovány do `documentFilesLiveData`.
 * - Metoda `checkPermission(Activity, Uri)` kontroluje oprávnění a nastavuje `hasPermission`
 * pomocí `setValue`, proto ji volat z UI vlákna nebo lifecycle\-aware kontextu.
 * - UI by mělo pozorovat `getDocumentFiles()`, `getIsLoading()` a `getHasPermission()` a
 * reagovat na změny (např. povolit tlačítka, zobrazit spinner, nebo spustit `loadFiles`).
 * <p>
 * Použití (stručně):
 * 1. Zavolat `checkPermission(...)` a pozorovat `getHasPermission()`.
 * 2. Po potvrzení oprávnění zavolat `loadFiles(...)`.
 * 3. Pozorovat `getDocumentFiles()` pro zobrazení výsledků a `getIsLoading()` pro indikaci stavu.
 */
public class ImportPriceListViewModel extends ViewModel {

    private static final String TAG = "ImportPriceListViewModel";
    private static final String[] filtersFileName = {".json"};
    private static final int MSG_LOAD_FILES = 22;
    private final MutableLiveData<ArrayList<DocumentFile>> documentFilesLiveData = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> hasPermission = new MutableLiveData<>(false);

    /**
     * Handler pro načtení souborů na hlavním vlákně.
     * <p>
     * - Běží na hlavním vlákně (`Looper.getMainLooper()`).
     * - Očekává `msg.obj` typu `ArrayList<DocumentFile>`. Pokud je `msg` nebo `msg.obj` null
     * nebo `msg.obj` jiného typu, zpráva se bezpečně ignoruje (stále se provede úklid).
     * - V `handleMessage` provádí tyto kroky:
     * 1. bezpečná kontrola typu a volání `setDocumentFiles(...)` pro aktualizaci `LiveData`,
     * 2. nastavení `isLoading` na `false` přes `postValue(false)`,
     * 3. odstranění všech plánovaných callbacků a zpráv a odstranění specifické zprávy
     * s kódem `MSG_LOAD_FILES`.
     * <p>
     * Účel: předat výsledky načítání na UI vlákno, zajistit thread-safe aktualizaci LiveData
     * a provést úklid handleru po dokončení nebo při chybě.
     */
    private final Handler handlerLoadFile = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(@NonNull android.os.Message msg) {
            super.handleMessage(msg);
            setDocumentFiles((ArrayList<DocumentFile>) msg.obj);
            isLoading.postValue(false);
            handlerLoadFile.removeCallbacksAndMessages(null);
            handlerLoadFile.removeMessages(MSG_LOAD_FILES);
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
        new Files().loadFiles(activity, uri, filtersFileName, handlerLoadFile, resultTree, MSG_LOAD_FILES);
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
