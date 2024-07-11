package cz.xlisto.elektrodroid.permission;


import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.documentfile.provider.DocumentFile;

import com.google.android.material.snackbar.Snackbar;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import cz.xlisto.elektrodroid.R;
import cz.xlisto.elektrodroid.modules.backup.FilterNameFile;
import cz.xlisto.elektrodroid.modules.backup.SortFile;
import cz.xlisto.elektrodroid.shp.ShPBackup;


/**
 * Xlisto 06.12.2023 20:17
 *
 * @see <a href="https://developer.android.com/training/data-storage/shared/documents-files#grant-access-directory">Grant access to a directory</a>
 **/
public class Files {
    private static final String TAG = "Files";
    public static final String DEF_URI = "content://com.android.externalstorage.documents/document/primary%3A";
    //"content://com.android.externalstorage.documents/document/primary:"
    public ExecutorService executorService = Executors.newSingleThreadExecutor();


    /**
     * Načte seznam souborů ve složce
     *
     * @param activity        activity aplikace
     * @param uri             Uri složky
     * @param filtersFileName pole názvů souborů
     * @param handler         handler pro zpracování výsledku
     * @param resultTree      callback pro výběr složky
     * @param what            číslo zprávy
     */
    public void loadFiles(Activity activity, Uri uri, String[] filtersFileName, Handler handler, ActivityResultLauncher<Intent> resultTree, int what) {
        if (permissions(activity, uri)) {
            executorService.execute(() -> {
                DocumentFile pickedDir = DocumentFile.fromTreeUri(activity, uri);
                if (pickedDir != null) {
                    final ArrayList<DocumentFile> documentFiles = new ArrayList<>(); //seznam souborů
                    if (pickedDir.canRead()) {
                        //documentFiles.clear();
                        for (DocumentFile file : pickedDir.listFiles()) {

                            try {
                                if (!file.isDirectory()) {
                                    if (new FilterNameFile(filtersFileName).accept(file)) {
                                        documentFiles.add(file);
                                    }
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                        SortFile.quickSortDate(documentFiles);
                    }
                    handler.sendMessage(handler.obtainMessage(what, documentFiles));

                }
            });
        } else {
            Snackbar.make(Objects.requireNonNull(activity.getCurrentFocus()), activity.getResources().getString(R.string.add_permissions), Snackbar.LENGTH_LONG)
                    .setAction(activity.getResources().getString(R.string.select), v -> openTree(true, activity, resultTree))
                    .show();
        }
    }


    /**
     * Zkontroluje výskyt souboru ve složce
     *
     * @param activity activity aplikace
     * @param uri      Uri složky
     * @param nameFile název souboru
     * @return true pokud existuje
     */
    public boolean existJSONFile(Activity activity, String nameFile,Uri uri) {
        if (permissions(activity, uri)) {
            DocumentFile folder = DocumentFile.fromTreeUri(activity, uri);
            if (folder != null) {
                DocumentFile file = folder.findFile(nameFile);
                return file != null;
            }
        }
        return false;
    }


    /**
     * Uložení ceník v JSON formátu do souboru
     *
     * @param json     JSON
     * @param nameFile název souboru
     * @param uri      Uri složky
     */
    public void saveJSONFile(Activity activity, View view, String json, String nameFile, Uri uri, ActivityResultLauncher<Intent> resultTree) {

        if (permissions(activity, uri)) {
            DocumentFile folder = DocumentFile.fromTreeUri(activity, uri);
            if (folder != null) {
                DocumentFile file = folder.findFile(nameFile);
                if (file == null)
                    file = folder.createFile("application/json", nameFile);
                OutputStream out;
                try {
                    assert file != null;
                    out = activity.getContentResolver().openOutputStream(file.getUri());
                    assert out != null;
                    out.write(json.getBytes());
                    out.flush();
                    out.close();
                    Toast.makeText(activity, "Uloženo", Toast.LENGTH_SHORT).show();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        } else {

            Snackbar.make(view, activity.getResources().getString(R.string.add_permissions), Snackbar.LENGTH_LONG)
                    .setAction(activity.getResources().getString(R.string.select), v -> openTree(true, activity, resultTree))
                    .show();
        }
    }


    /**
     * Dotaz se na povolení oprávnění složky
     *
     * @return true pokud je povoleno
     */
    static public boolean permissions(Activity activity, Uri uri) {
        return Permissions.getInstance().isPermissionStorage(activity, uri);
    }


    /**
     * Otevření aktivity pro výběr složky
     *
     * @param showRoot true zobrazí root složku, false zobrazí poslední vybranou složku
     */
    static public void openTree(boolean showRoot, Activity activity, ActivityResultLauncher<Intent> resultTree) {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);

        Uri uri = Uri.parse(DEF_URI);

        if (showRoot)
            intent.putExtra("android.provider.extra.INITIAL_URI", uri);
        else {       //content://com.android.externalstorage.documents/document/primary:
            ShPBackup shPBackup = new ShPBackup(activity);
            Uri uriTemp = Uri.parse(shPBackup.get(ShPBackup.FOLDER_BACKUP, DEF_URI));
            intent.putExtra("android.provider.extra.INITIAL_URI", uriTemp);
        }

        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION
                | Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
                | Intent.FLAG_GRANT_PREFIX_URI_PERMISSION);

        resultTree.launch(intent);
    }


    /**
     * Nastavení trvalého oprávnění po restartu aplikace
     *
     * @param data Uri v objektu intent
     */
    public static void activityResult(Intent data, Activity activity) {
        Uri uri = data.getData();
        assert uri != null;
        ShPBackup shPBackup = new ShPBackup(activity);
        shPBackup.set(ShPBackup.FOLDER_BACKUP, uri.toString());

        //Nastavení trvalého oprávnění po restartu aplikace
        activity.grantUriPermission(activity.getPackageName(), uri, Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        activity.getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
    }
}
