package cz.xlisto.elektrodroid.modules.exportimportpricelist;


import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.documentfile.provider.DocumentFile;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.ArrayList;
import java.util.List;

import cz.xlisto.elektrodroid.permission.Files;


/**
 * ViewModel pro importování ceníků.
 * <p>
 * Poskytuje lifecycle-aware stav a data pro UI komponenty při načítání souborů ceníků
 * z vybraného adresáře (URI). Hlavní odpovědnosti:
 * - Udržovat a vystavovat seznam nalezených souborů jako {@code LiveData<ArrayList<DocumentFile>>}.
 * - Indikovat stav načítání přes {@code LiveData<Boolean> isLoading}.
 * - Informovat o dostupnosti oprávnění k vybranému adresáři přes {@code LiveData<Boolean> hasPermission}.
 * <p>
 * Implementační poznámky:
 * - Asynchronní načítání souborů je prováděno pomocí pomocné třídy {@link Files} a {@link Handler},
 * výsledky jsou publikovány do {@code documentFilesLiveData}.
 * - Metoda {@link #checkPermission(Activity, Uri)} kontroluje oprávnění a nastavuje {@code hasPermission}
 * pomocí {@code setValue}, proto ji volat z UI vlákna nebo lifecycle-aware kontextu.
 * - UI by mělo pozorovat {@link #getDocumentFiles()}, {@link #getIsLoading()} a {@link #getHasPermission()} a
 * reagovat na změny (např. povolit tlačítka, zobrazit spinner, nebo spustit {@link #loadFiles(Activity, Uri, ActivityResultLauncher)}).
 * <p>
 * Použití (stručně):
 * 1. Zavolat {@link #checkPermission(Activity, Uri)} a pozorovat {@link #getHasPermission()}.
 * 2. Po potvrzení oprávnění zavolat {@link #loadFiles(Activity, Uri, ActivityResultLauncher)}.
 * 3. Pozorovat {@link #getDocumentFiles()} pro zobrazení výsledků a {@link #getIsLoading()} pro indikaci stavu.
 */
public class ImportPriceListViewModel extends ViewModel {

    /**
     * Tag pro logování.
     */
    private static final String TAG = "ImportPriceListViewModel";

    /** Filtry přípon souborů pro vyhledávání ceníků (podporovány soubory s příponou .json). */
    private static final String[] filtersFileName = {".json"};

    /** Identifikátor zprávy pro {@link Handler} při načítání souborů. */
    private static final int MSG_LOAD_FILES = 22;

    /** LiveData držící seznam nalezených souborů ceníků. */
    private final MutableLiveData<ArrayList<DocumentFile>> documentFilesLiveData = new MutableLiveData<>();

    /** LiveData indikující, zda právě probíhá asynchronní načítání souborů. */
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);

    /** LiveData indikující, zda aplikace má oprávnění k vybranému adresáři URI. */
    private final MutableLiveData<Boolean> hasPermission = new MutableLiveData<>(false);

    /**
     * Handler pro načtení souborů na hlavním vlákně.
     * <p>
     * - Běží na hlavním vlákně ({@link Looper#getMainLooper()}).
     * - Očekává {@code msg.obj} typu {@link List} obsahující {@link DocumentFile}. Pokud je {@code msg} nebo {@code msg.obj} null
     * nebo {@code msg.obj} jiného typu, zpráva se bezpečně ignoruje.
     * - V {@code handleMessage} provádí tyto kroky:
     * 1. bezpečná kontrola typu a volání {@link #setDocumentFiles(ArrayList)} pro aktualizaci {@code LiveData},
     * 2. nastavení {@code isLoading} na {@code false} přes {@code postValue(false)},
     * 3. odstranění všech plánovaných callbacků a zpráv a odstranění specifické zprávy
     * s kódem {@code MSG_LOAD_FILES}.
     * <p>
     * Účel: předat výsledky načítání na UI vlákno, zajistit thread-safe aktualizaci LiveData
     * a provést úklid handleru po dokončení nebo při chybě.
     */
    private final Handler handlerLoadFile = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(@NonNull android.os.Message msg) {
            super.handleMessage(msg);
            setDocumentFiles(extractDocumentFiles(msg.obj));
            isLoading.postValue(false);
            handlerLoadFile.removeCallbacksAndMessages(null);
            handlerLoadFile.removeMessages(MSG_LOAD_FILES);
        }
    };


    /**
     * Bezpečně převede payload zprávy na seznam {@link DocumentFile} bez unchecked castu.
     *
     * @param payload data přijatá v {@code Message.obj}
     * @return seznam souborů; při neplatném formátu prázdný seznam
     */
    private ArrayList<DocumentFile> extractDocumentFiles(@Nullable Object payload) {
        ArrayList<DocumentFile> files = new ArrayList<>();
        if (!(payload instanceof List<?> rawList)) {
            if (payload != null) {
                Log.w(TAG, "Unexpected loadFiles payload type: " + payload.getClass().getName());
            }
            return files;
        }

        for (Object item : rawList) {
            if (item instanceof DocumentFile documentFile) {
                files.add(documentFile);
            } else if (item != null) {
                Log.w(TAG, "Ignoring non-DocumentFile item in payload: " + item.getClass().getName());
            }
        }
        return files;
    }


    /**
     * Vrátí {@link LiveData} objekt obsahující seznam souborů dokumentů.
     * <p>
     * Tato metoda vrací {@link LiveData} objekt, který obsahuje aktuální seznam souborů dokumentů.
     * Tento seznam je aktualizován metodou {@link #setDocumentFiles(ArrayList)}.
     *
     * @return {@link LiveData} objekt obsahující seznam souborů dokumentů.
     */
    public LiveData<ArrayList<DocumentFile>> getDocumentFiles() {
        return documentFilesLiveData;
    }


    /**
     * Nastaví seznam souborů dokumentů.
     * <p>
     * Tato metoda aktualizuje {@link LiveData} objekt {@code documentFilesLiveData} s novým seznamem souborů dokumentů.
     *
     * @param files Seznam souborů dokumentů, které mají být nastaveny.
     */
    public void setDocumentFiles(ArrayList<DocumentFile> files) {
        documentFilesLiveData.postValue(files);
    }


    /**
     * Načte soubory z daného URI.
     * <p>
     * Tato metoda spustí načítání souborů z daného URI pomocí třídy {@link Files}.
     * Aktualizuje stav načítání na {@code true} a po dokončení načítání nastaví seznam souborů.
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
     * Vrátí {@link LiveData} objekt obsahující stav načítání.
     * <p>
     * Tato metoda vrací {@link LiveData} objekt, který obsahuje aktuální stav načítání.
     * Stav načítání je aktualizován metodou {@link #loadFiles(Activity, Uri, ActivityResultLauncher)}.
     *
     * @return {@link LiveData} objekt obsahující stav načítání.
     */
    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }


    /**
     * Vrátí {@link LiveData} indikující, zda aplikace má oprávnění k vybranému URI.
     * <p>
     * Hodnota je lifecycle-aware a měla by být pozorována z UI komponent (Activity/Fragment).
     * Aktualizuje se voláním {@link #checkPermission(Activity, Uri)} a poskytuje bezpečný způsob, jak reagovat na změny oprávnění.
     *
     * @return {@link LiveData} s informací o oprávnění k adresáři.
     */
    public LiveData<Boolean> getHasPermission() {
        return hasPermission;
    }


    /**
     * Zkontroluje oprávnění aplikace k zadanému adresáři (URI) a aktualizuje
     * interní {@link LiveData} {@code hasPermission}.
     * <p>
     * Metoda používá {@link Files#permissions(Activity, Uri)} pro ověření přístupu.
     * Výsledek (true = má oprávnění, false = nemá) je nastaven pomocí
     * {@code hasPermission.setValue(...)}, proto by měla být volána z UI vlákna
     * nebo z lifecycle-aware kontextu (Activity/Fragment).
     *
     * @param activity Activity použitá pro kontrolu oprávnění
     * @param uri      URI adresáře, jehož oprávnění se kontroluje
     */
    public void checkPermission(Activity activity, Uri uri) {
        boolean ok = Files.permissions(activity, uri);
        hasPermission.setValue(ok);
    }

}
