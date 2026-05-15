package cz.xlisto.elektrodroid.modules.backup;


import android.app.Activity;
import android.content.Context;
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
 * ViewModel pro správu lokálních záloh a stavu jejich nahrávání na Google Drive.
 *
 * <p>Třída drží data i stav operací nezávisle na životním cyklu Fragmentu, takže
 * UI může bezpečně přežít změny konfigurace (např. rotaci obrazovky). Poskytuje:</p>
 * <ul>
 *   <li>seznam načtených lokálních záloh ({@link #getDocumentFiles()}),</li>
 *   <li>stav načítání ({@link #getIsLoading()}),</li>
 *   <li>informaci o oprávnění k přístupu do složky ({@link #getHasPermission()}),</li>
 *   <li>stav hromadného uploadu na Google Drive ({@link #getUploadState()}).</li>
 * </ul>
 */
public class BackupViewModel extends ViewModel {

    private static final String TAG = "BackupViewModel";
    private static final int MSG_LOAD_FILES = 11;
    private final MutableLiveData<ArrayList<DocumentFile>> documentFilesLiveData = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> hasPermission = new MutableLiveData<>(false);

    // ---- Upload state ----

    public enum UploadStatus {
        IDLE, IN_PROGRESS, FINISHED, FAILED
    }

    /**
     * Neměnný stav uploadu lokálních záloh na Google Drive.
     *
     * @param status        aktuální fáze operace
     * @param success       informace o úspěchu při finálním stavu
     * @param uploadedCount počet dosud nahraných souborů
     * @param totalCount    celkový počet souborů určených k nahrání
     * @param errorMessage  chybová zpráva pro UI (pouze při FAILED)
     */
    public record UploadState(UploadStatus status, boolean success, int uploadedCount, int totalCount,
                              @Nullable String errorMessage) {

        /**
         * Vrátí výchozí klidový stav bez aktivního uploadu.
         *
         * @return stav {@link UploadStatus#IDLE}
         */
        public static UploadState idle() {
            return new UploadState(UploadStatus.IDLE, false, 0, 0, null);
        }

        /**
         * Vrátí průběžný stav uploadu.
         *
         * @param processed počet již zpracovaných souborů
         * @param total     celkový počet souborů
         * @return stav {@link UploadStatus#IN_PROGRESS}
         */
        public static UploadState inProgress(int processed, int total) {
            return new UploadState(UploadStatus.IN_PROGRESS, false, processed, total, null);
        }

        /**
         * Vrátí finální stav po dokončení uploadu.
         *
         * @param success       {@code true}, pokud byly nahrány všechny soubory
         * @param uploadedCount počet úspěšně nahraných souborů
         * @param totalCount    celkový počet souborů
         * @return stav {@link UploadStatus#FINISHED}
         */
        public static UploadState finished(boolean success, int uploadedCount, int totalCount) {
            return new UploadState(UploadStatus.FINISHED, success, uploadedCount, totalCount, null);
        }

        /**
         * Vrátí chybový stav uploadu.
         *
         * @param errorMessage popis chyby určený pro zobrazení uživateli
         * @return stav {@link UploadStatus#FAILED}
         */
        public static UploadState failed(String errorMessage) {
            return new UploadState(UploadStatus.FAILED, false, 0, 0, errorMessage);
        }
    }

    private final MutableLiveData<UploadState> uploadStateLiveData = new MutableLiveData<>(UploadState.idle());

    /**
     * Vrátí lifecycle-aware stream stavu uploadu na Google Drive.
     *
     * @return {@link LiveData} s hodnotou {@link UploadState}
     */
    public LiveData<UploadState> getUploadState() {
        return uploadStateLiveData;
    }

    /**
     * Indikuje, zda právě probíhá upload.
     *
     * @return {@code true}, pokud je aktuální stav {@link UploadStatus#IN_PROGRESS}
     */
    public boolean isUploadInProgress() {
        UploadState state = uploadStateLiveData.getValue();
        return state != null && state.status() == UploadStatus.IN_PROGRESS;
    }

    /**
     * Resetuje stav uploadu do výchozího klidového stavu.
     */
    public void resetUploadToIdle() {
        uploadStateLiveData.postValue(UploadState.idle());
    }

    /**
     * Spustí nahrávání vybraných souborů na Google Drive v pozadí.
     * Stav průběhu je dostupný přes {@link #getUploadState()}.
     *
     * @param context           kontext použitý pro inicializaci Google Drive služby
     * @param userName          uživatelské jméno Google účtu
     * @param selectedFiles     seznam souborů k nahrání
     * @param overwriteExisting {@code true}, pokud se mají stejnojmenné soubory na Drive přepsat
     */
    public void startUpload(Context context, String userName, List<DocumentFile> selectedFiles, boolean overwriteExisting) {
        if (isUploadInProgress())
            return;

        if (selectedFiles == null || selectedFiles.isEmpty()) {
            uploadStateLiveData.postValue(UploadState.failed("No files selected"));
            return;
        }

        int totalCount = selectedFiles.size();
        uploadStateLiveData.postValue(UploadState.inProgress(0, totalCount));

        GoogleDriveService googleDriveService = new GoogleDriveService(context, userName);
        googleDriveService.setOnDriveServiceListener(new GoogleDriveService.OnDriverServiceListener() {
            @Override
            public void onDriveServiceReady() {
                int successCount = 0;
                for (int i = 0; i < selectedFiles.size(); i++) {
                    DocumentFile documentFile = selectedFiles.get(i);
                    if (documentFile != null && googleDriveService.uploadFile(documentFile, overwriteExisting))
                        successCount++;
                    uploadStateLiveData.postValue(UploadState.inProgress(i + 1, totalCount));
                }
                final boolean success = successCount == totalCount;
                uploadStateLiveData.postValue(UploadState.finished(success, successCount, totalCount));
            }

            @Override
            public void onDriveServiceError(String errorMessage) {
                uploadStateLiveData.postValue(UploadState.failed(errorMessage));
            }
        });
    }

    /**
     * Handler na hlavním vlákně pro dokončení asynchronního načtení souborů.
     *
     * <p>Očekává, že {@code msg.obj} obsahuje {@code ArrayList<DocumentFile>}.
     * Po přijetí výsledku publikuje data do {@link #documentFilesLiveData}
     * a ukončí stav načítání.</p>
     */
    private final Handler handlerLoadFile = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(@NonNull android.os.Message msg) {
            super.handleMessage(msg);
            setDocumentFiles(extractDocumentFiles(msg.obj));
            handlerLoadFile.removeCallbacksAndMessages(null);
            handlerLoadFile.removeMessages(MSG_LOAD_FILES);
            isLoading.postValue(false);
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
            if (payload != null)
                Log.w(TAG, "Unexpected loadFiles payload type: " + payload.getClass().getName());
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
     * Vrátí stream aktuálního seznamu lokálních záložních souborů.
     *
     * @return {@link LiveData} se seznamem {@link DocumentFile}
     */
    public LiveData<ArrayList<DocumentFile>> getDocumentFiles() {
        return documentFilesLiveData;
    }


    /**
     * Publikuje nový seznam lokálních záloh.
     *
     * @param files seznam nalezených souborů
     */
    public void setDocumentFiles(ArrayList<DocumentFile> files) {
        documentFilesLiveData.postValue(files);
    }


    /**
     * Spustí asynchronní načítání souborů ze zvoleného adresáře.
     *
     * @param activity   hostitelská aktivita
     * @param uri        URI složky se zálohami
     * @param resultTree launcher pro případné dožádání oprávnění
     */
    public void loadFiles(Activity activity, Uri uri, ActivityResultLauncher<Intent> resultTree) {
        isLoading.postValue(true);
        new Files().loadFiles(activity, uri, RecoverData.getFiltersFileName(), handlerLoadFile, resultTree, MSG_LOAD_FILES);
    }


    /**
     * Vrátí stream stavu načítání lokálních záloh.
     *
     * @return {@link LiveData} s hodnotou {@code true}, pokud probíhá načítání
     */
    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }


    /**
     * Vrátí stream informace o oprávnění k přístupu do zvolené složky.
     *
     * @return {@link LiveData} s informací, zda je přístup povolen
     */
    public LiveData<Boolean> getHasPermission() {
        return hasPermission;
    }


    /**
     * Ověří oprávnění aplikace k danému URI a publikuje výsledek do `hasPermission`.
     *
     * @param activity aktivita použitá při kontrole oprávnění
     * @param uri      URI adresáře, ke kterému se má ověřit přístup
     */
    public void checkPermission(Activity activity, Uri uri) {
        boolean ok = Files.permissions(activity, uri);
        hasPermission.setValue(ok);
    }

}
