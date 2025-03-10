package cz.xlisto.elektrodroid.modules.backup;


import android.app.Activity;
import android.content.Context;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import androidx.documentfile.provider.DocumentFile;

import com.google.api.services.drive.Drive;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import cz.xlisto.elektrodroid.R;
import cz.xlisto.elektrodroid.databaze.DataPriceListSource;
import cz.xlisto.elektrodroid.databaze.DataSubscriptionPointSource;
import cz.xlisto.elektrodroid.shp.ShPBackup;
import cz.xlisto.elektrodroid.shp.ShPSubscriptionPoint;


/**
 * Třída RecoverDataFromBackupFile rozšiřuje třídu RecoverData a poskytuje metody
 * pro obnovu dat ze záložních ZIP souborů uložených na Google Drive.
 */
public class RecoverDataFromBackupFile extends RecoverData {

    private static final String TAG = "RecoverDataFromFile";
    private static Drive drive;


    /**
     * Nastaví službu Google Drive.
     *
     * @param service Služba Google Drive, která se má nastavit.
     */
    public static void setDriveService(Drive service) {
        drive = service;
    }


    /**
     * Obnoví databázi ze ZIP souboru uloženého na Google Drive.
     *
     * @param context  Kontext aplikace.
     * @param fileId   ID souboru na Google Drive.
     * @param fileName Název souboru.
     * @param callback Callback, který se zavolá po dokončení obnovy databáze.
     */
    public static void recoverDatabaseFromZipGoogleDrive(Context context, String fileId, String fileName, RecoverDatabaseCallback callback) {
        new Thread(() -> {
            java.io.File tempFile = new java.io.File(context.getCacheDir(), fileName);
            boolean result = false;
            try (OutputStream outputStream = new FileOutputStream(tempFile)) {
                drive.files().get(fileId).executeMediaAndDownloadTo(outputStream);
                DocumentFile documentFile = DocumentFile.fromFile(tempFile);
                result = recoverDatabaseFromZip(context, documentFile);
            } catch (IOException e) {
                Log.e(TAG, "recoverDatabaseFromZipGoogleDrive: " + e.getMessage());
            }
            boolean finalResult = result;
            ((Activity) context).runOnUiThread(() -> callback.onComplete(finalResult));
        }).start();
    }


    public static boolean recoverDatabaseFromZip(Context context, DocumentFile f) {
        if (f == null) {
            Toast.makeText(context, context.getResources().getString(R.string.not_restored_data), Toast.LENGTH_LONG).show();
            return false;
        }

        if (Objects.requireNonNull(f.getName()).contains(".zip")) {
            Boolean[] b = new Boolean[2];
            //zip archiv, který se rozbalí do povolený složky
            ArrayList<DocumentFile> files = unzip(context, f);
            for (int i = 0; i < files.size(); i++) {
                if (Objects.equals(files.get(i).getName(), "odecet.db") || Objects.equals(files.get(i).getName(), "cenik.db")) {
                    b[i] = recoverDatabaseFromFile(context, files.get(i));
                }
            }
            //úklid složky temp
            deleteDirectory(files);

            if (b[0] == null || b[1] == null)
                return false;
            return b[0] && b[1];
        } else {
            //původní záložní soubory
            return recoverDatabaseFromFile(context, f);
        }
    }


    /**
     * Obnoví databázi ze záložních souborů
     *
     * @param context kontext aplikace
     * @param f       DocumentFile soubor
     */
    private static boolean recoverDatabaseFromFile(Context context, DocumentFile f) {
        if (f == null || context == null) {
            return false;
        }
        //kontrola databáze, zdali existuje, pokud ne vytvoří se a pak se následně přepíše obnovovaným souborem
        DataSubscriptionPointSource dataSubscriptionPointSource = new DataSubscriptionPointSource(context);
        DataPriceListSource dataPriceListSource = new DataPriceListSource(context);
        dataSubscriptionPointSource.open();
        dataPriceListSource.open();
        dataSubscriptionPointSource.close();
        dataPriceListSource.close();

        try {
            File data = Environment.getDataDirectory();
            String currentDBPath = "";

            boolean cenik = Objects.requireNonNull(f.getName()).contains("cenik");
            if (cenik) {
                currentDBPath = "//data//" + context.getPackageName() + "//databases//databaze_cenik";
            }

            boolean odecet = f.getName().contains("odecet");
            if (odecet) {
                currentDBPath = "//data//" + context.getPackageName() + "//databases//odecty_a_mista";
            }

            File currentDB = new File(data, currentDBPath);//vnitřní databáze

            OutputStream src;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                src = Files.newOutputStream(currentDB.toPath());

            } else {
                src = new FileOutputStream(currentDB);
            }

            InputStream dst = context.getContentResolver().openInputStream(f.getUri());

            int c;
            byte[] buffer = new byte[1024];
            while (true) {
                assert dst != null;
                if ((c = dst.read(buffer)) == -1) break;
                src.write(buffer, 0, c);
            }

            src.close();
            src.flush();
            dst.close();

            //nastavení barev VT a NT
            //TODO: doplnit nastavení aplikace

            //new Databaze(getActivity()).getColorApp();//načtení barev z databáze a nastavení do sharedPreferences, otevírá si a zavírá databázy
            ShPSubscriptionPoint shPSubscriptionPoint = new ShPSubscriptionPoint(context);
            shPSubscriptionPoint.set(ShPSubscriptionPoint.ID_SUBSCRIPTION_POINT_LONG, -1L);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "recoverDatabaseFromFile: " + e.getMessage());
            return false;
        }
    }


    /**
     * Rozbalení ZIP archivu a uložení do stávající složky
     *
     * @param documentFile soubor se zálohou
     * @return ArrayList<DocumentFile> seznam souborů
     */
    private static ArrayList<DocumentFile> unzip(Context context, DocumentFile documentFile) {
        ShPBackup shPBackup = new ShPBackup(context);
        Uri treeUri = Uri.parse(shPBackup.get(ShPBackup.FOLDER_BACKUP, DEF_URI));
        DocumentFile pickedDir = DocumentFile.fromTreeUri(context, treeUri);

        ArrayList<DocumentFile> files = new ArrayList<>();
        byte[] buffer = new byte[1024];
        try {
            //vložení zip souboru
            ZipInputStream zis = new ZipInputStream(context.getContentResolver().openInputStream(documentFile.getUri()));

            //seznam souborů v zipu
            ZipEntry ze = zis.getNextEntry();
            while (ze != null) {
                String fileName = ze.getName();
                DocumentFile newFiled = Objects.requireNonNull(pickedDir).findFile(fileName);
                if (newFiled == null) {
                    newFiled = pickedDir.createFile("bin", fileName);
                }

                OutputStream fos = context.getContentResolver().openOutputStream(Objects.requireNonNull(newFiled).getUri());

                int len;
                while ((len = zis.read(buffer)) > 0) {
                    assert fos != null;
                    fos.write(buffer, 0, len);
                }

                assert fos != null;
                fos.close();
                files.add(newFiled);
                ze = zis.getNextEntry();
            }
            zis.closeEntry();
            zis.close();
        } catch (IOException e) {
            Log.e(TAG, "unzip: " + e.getMessage());
        }
        return files;
    }


    /**
     * Smazání dočasných souborů z rozbaleného ZIPu
     */
    public static void deleteDirectory(ArrayList<DocumentFile> documentFiles) {
        for (int i = 0; i < documentFiles.size(); i++) {
            documentFiles.get(i).delete();
        }
    }


    public interface RecoverDatabaseCallback {

        void onComplete(boolean result);

    }

}
