package cz.xlisto.elektrodroid.modules.backup;


import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;

import cz.xlisto.elektrodroid.shp.ShPBackup;


/**
 * Centrální plánovač čekající fronty uploadu lokálních záloh na Google Drive.
 *
 * <p>Třída pouze připraví a naplánuje {@link PendingBackupUploadWorker}, pokud v
 * {@link ShPBackup} existují validní metadata fronty (uživatel + seznam souborů).
 * Samotné odeslání souborů provádí worker.</p>
 */
public final class PendingBackupUploadScheduler {

    /**
     * Utility třída - instancování není povoleno.
     */
    private PendingBackupUploadScheduler() {
    }


    /**
     * Naplánuje čekající upload, pokud existují metadata fronty v SharedPreferences.
     *
     * <p>Metoda načte účet a názvy souborů z perzistentní fronty, sestaví input data
     * pro worker a úlohu vloží jako unique work.</p>
     *
     * @param context         aplikační kontext
     * @param replaceExisting {@code true} = nahradí existující unique work,
     *                        {@code false} = ponechá existující běžící/čekající work
     */
    public static void scheduleIfNeeded(@NonNull Context context, boolean replaceExisting) {
        ShPBackup shPBackup = new ShPBackup(context);
        String userName = shPBackup.get(ShPBackup.PENDING_WIFI_UPLOAD_USER_NAME, "");
        if (userName.isEmpty())
            return;

        ArrayList<String> pendingFileNames = loadPendingFileNames(shPBackup);
        if (pendingFileNames.isEmpty())
            return;

        String backupFolderUri = shPBackup.get(ShPBackup.FOLDER_BACKUP, RecoverData.DEF_URI);
        Data inputData = new Data.Builder()
                .putString(PendingBackupUploadWorker.KEY_USER_NAME, userName)
                .putString(PendingBackupUploadWorker.KEY_BACKUP_FOLDER_URI, backupFolderUri)
                .putStringArray(PendingBackupUploadWorker.KEY_FILE_NAMES, pendingFileNames.toArray(new String[0]))
                .build();

        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(PendingBackupUploadWorker.class)
                .setInputData(inputData)
                .setConstraints(constraints)
                .addTag(PendingBackupUploadWorker.WORK_TAG_PENDING_UPLOAD_WIFI)
                .build();

        ExistingWorkPolicy policy = replaceExisting ? ExistingWorkPolicy.REPLACE : ExistingWorkPolicy.KEEP;
        WorkManager.getInstance(context).enqueueUniqueWork(
                PendingBackupUploadWorker.UNIQUE_WORK_NAME_PENDING_UPLOAD_WIFI,
                policy,
                request
        );
    }


    /**
     * Načte názvy čekajících souborů z perzistentní fronty.
     *
     * @param shPBackup wrapper nad SharedPreferences s uloženými metadaty fronty
     * @return seznam unikátních názvů souborů; při neplatných datech prázdný seznam
     */
    @NonNull
    private static ArrayList<String> loadPendingFileNames(@NonNull ShPBackup shPBackup) {
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

