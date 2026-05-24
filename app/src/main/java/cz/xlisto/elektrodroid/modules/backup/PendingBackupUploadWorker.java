package cz.xlisto.elektrodroid.modules.backup;


import android.content.Context;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.documentfile.provider.DocumentFile;
import androidx.work.ListenableWorker;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import cz.xlisto.elektrodroid.shp.ShPBackup;
import cz.xlisto.elektrodroid.utils.NetworkUtil;


/**
 * Worker zpracovávající čekající frontu lokálních záloh určených k odeslání na Google Drive.
 *
 * <p>Úloha se spouští přes {@link androidx.work.WorkManager} a čte metadata fronty primárně
 * ze {@link ShPBackup}. Pokud je dostupné WiFi připojení, pokusí se nahrát všechny čekající
 * soubory. Neúspěšné položky vrací zpět do fronty a vrací {@code retry}, aby se zkusily znovu
 * při dalším běhu worku.</p>
 */
public class PendingBackupUploadWorker extends Worker {

    public static final String UNIQUE_WORK_NAME_PENDING_UPLOAD_WIFI = "uniqueWorkPendingUploadWifi";
    public static final String WORK_TAG_PENDING_UPLOAD_WIFI = "workTagPendingUploadWifi";
    public static final String KEY_USER_NAME = "keyUserName";
    public static final String KEY_BACKUP_FOLDER_URI = "keyBackupFolderUri";
    public static final String KEY_FILE_NAMES = "keyFileNames";


    /**
     * Vytvoří instanci workeru s kontextem aplikace a parametry z WorkManageru.
     *
     * @param context      kontext aplikace
     * @param workerParams parametry předané WorkManagerem
     */
    public PendingBackupUploadWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }


    /**
     * Hlavní vstupní bod workeru.
     *
     * <p>Postup:
     * <ol>
     *   <li>ověří dostupnost WiFi,</li>
     *   <li>načte uživatele a frontu souborů,</li>
     *   <li>připraví {@link GoogleDriveService},</li>
     *   <li>odešle položky fronty,</li>
     *   <li>podle výsledku frontu vyčistí nebo aktualizuje a vrátí odpovídající stav.</li>
     * </ol>
     *
     * @return {@code success} při plném úspěchu, {@code retry} při dočasném selhání,
     * {@code failure} při neplatných vstupních datech
     */
    @NonNull
    @Override
    public ListenableWorker.Result doWork() {
        Context context = getApplicationContext();
        if (!NetworkUtil.isWifiConnected(context)) {
            return ListenableWorker.Result.retry();
        }

        ShPBackup shPBackup = new ShPBackup(context);
        String userName = shPBackup.get(ShPBackup.PENDING_WIFI_UPLOAD_USER_NAME, "");
        if (userName == null || userName.trim().isEmpty())
            userName = getInputData().getString(KEY_USER_NAME);

        String backupFolderUriValue = getInputData().getString(KEY_BACKUP_FOLDER_URI);
        ArrayList<String> fileNames = loadPendingFileNames(shPBackup);
        if (fileNames.isEmpty()) {
            String[] inputFileNames = getInputData().getStringArray(KEY_FILE_NAMES);
            if (inputFileNames != null) {
                for (String inputFileName : inputFileNames) {
                    if (inputFileName != null && !inputFileName.trim().isEmpty() && !fileNames.contains(inputFileName))
                        fileNames.add(inputFileName);
                }
            }
        }

        if (userName == null || userName.trim().isEmpty() || backupFolderUriValue == null || fileNames.isEmpty()) {
            clearPendingWifiUploadMetadata(context);
            return ListenableWorker.Result.failure();
        }

        DocumentFile backupFolder = DocumentFile.fromTreeUri(context, Uri.parse(backupFolderUriValue));
        if (backupFolder == null || !backupFolder.canRead()) {
            clearPendingWifiUploadMetadata(context);
            return ListenableWorker.Result.failure();
        }

        GoogleDriveService googleDriveService = new GoogleDriveService(context, userName);
        CountDownLatch latch = new CountDownLatch(1);
        AtomicBoolean serviceReady = new AtomicBoolean(false);

        googleDriveService.setOnDriveServiceListener(new GoogleDriveService.OnDriverServiceListener() {
            @Override
            public void onDriveServiceReady() {
                serviceReady.set(true);
                latch.countDown();
            }

            @Override
            public void onDriveServiceError(String errorMessage) {
                latch.countDown();
            }
        });

        try {
            if (!latch.await(45, TimeUnit.SECONDS) || !serviceReady.get()) {
                return ListenableWorker.Result.retry();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return ListenableWorker.Result.retry();
        }

        int attempted = 0;
        ArrayList<String> failedFileNames = new ArrayList<>();
        for (String fileName : fileNames) {
            if (fileName == null || fileName.trim().isEmpty()) {
                continue;
            }

            DocumentFile file = backupFolder.findFile(fileName);
            if (file == null || !file.isFile()) {
                continue;
            }

            attempted++;
            boolean uploaded = googleDriveService.uploadFile(file, true);
            if (!uploaded)
                failedFileNames.add(fileName);
        }

        if (attempted == 0) {
            clearPendingWifiUploadMetadata(context);
            return ListenableWorker.Result.failure();
        }

        if (!failedFileNames.isEmpty()) {
            persistPendingWifiUploadMetadata(context, userName, failedFileNames);
            return ListenableWorker.Result.retry();
        }

        clearPendingWifiUploadMetadata(context);
        return ListenableWorker.Result.success();
    }


    /**
     * Smaže metadata čekající fronty uploadu v {@link ShPBackup}.
     *
     * @param context kontext aplikace
     */
    private void clearPendingWifiUploadMetadata(@NonNull Context context) {
        ShPBackup shPBackup = new ShPBackup(context);
        shPBackup.set(ShPBackup.PENDING_WIFI_UPLOAD_USER_NAME, "");
        shPBackup.set(ShPBackup.PENDING_WIFI_UPLOAD_FILE_NAMES, "");
    }


    /**
     * Uloží neodeslané položky zpět do perzistentní fronty.
     *
     * <p>Metoda se používá při částečném neúspěchu uploadu, aby se při příštím běhu worku
     * opakovaly pouze neodeslané soubory.</p>
     *
     * @param context   kontext aplikace
     * @param userName  Google účet, pod kterým se má upload opakovat
     * @param fileNames seznam názvů souborů, které se nepodařilo odeslat
     */
    private void persistPendingWifiUploadMetadata(@NonNull Context context,
                                                  @NonNull String userName,
                                                  @NonNull ArrayList<String> fileNames) {
        ShPBackup shPBackup = new ShPBackup(context);
        if (fileNames.isEmpty()) {
            clearPendingWifiUploadMetadata(context);
            return;
        }

        JSONArray jsonArray = new JSONArray();
        for (String fileName : fileNames) {
            if (fileName != null && !fileName.trim().isEmpty())
                jsonArray.put(fileName);
        }

        shPBackup.set(ShPBackup.PENDING_WIFI_UPLOAD_USER_NAME, userName);
        shPBackup.set(ShPBackup.PENDING_WIFI_UPLOAD_FILE_NAMES, jsonArray.toString());
    }


    /**
     * Načte seznam názvů souborů z perzistentní fronty uploadu.
     *
     * @param shPBackup wrapper nad SharedPreferences s uloženými metadaty
     * @return seznam unikátních názvů souborů; při chybě parsování prázdný seznam
     */
    @NonNull
    private ArrayList<String> loadPendingFileNames(@NonNull ShPBackup shPBackup) {
        ArrayList<String> fileNames = new ArrayList<>();
        String fileNamesJson = shPBackup.get(ShPBackup.PENDING_WIFI_UPLOAD_FILE_NAMES, "");
        if (fileNamesJson.isEmpty())
            return fileNames;

        try {
            JSONArray jsonArray = new JSONArray(fileNamesJson);
            for (int i = 0; i < jsonArray.length(); i++) {
                String fileName = jsonArray.optString(i, "");
                if (!fileName.isEmpty() && !fileNames.contains(fileName))
                    fileNames.add(fileName);
            }
        } catch (JSONException ignored) {
        }

        return fileNames;
    }
}





