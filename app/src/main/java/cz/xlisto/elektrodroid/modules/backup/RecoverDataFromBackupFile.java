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
 * Pomocná třída pro obnovu databází aplikace ze záložních souborů.
 *
 * <p>Podporuje obnovu z lokálních záloh i z archivů uložených na Google Drive.
 * V případě ZIP záloh nejprve stáhne nebo rozbalí archiv do dočasného umístění,
 * následně obnoví jednotlivé databázové soubory a po dokončení provede úklid
 * dočasně vytvořených souborů.</p>
 */
public class RecoverDataFromBackupFile extends RecoverData {

    private static final String TAG = "RecoverDataFromFile";
    private static Drive drive;


    /**
     * Nastaví instanci služby Google Drive používanou pro obnovu ze vzdálených záloh.
     *
     * @param service inicializovaná služba Google Drive
     */
    public static void setDriveService(Drive service) {
        drive = service;
    }


    /**
     * Obnoví databázi ze ZIP souboru uloženého na Google Drive.
     *
     * <p>Soubor je nejprve stažen do cache aplikace a následně předán standardnímu
     * workflow obnovy ze ZIP archivu. Výsledek je vrácen zpět do UI přes callback.</p>
     *
     * @param context  kontext aplikace nebo activity
     * @param fileId   ID souboru na Google Drive
     * @param fileName název dočasného lokálního souboru v cache
     * @param callback callback vyvolaný po dokončení obnovy
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
            if (context instanceof Activity activity) {
                activity.runOnUiThread(() -> callback.onComplete(finalResult));
            } else {
                callback.onComplete(finalResult);
            }
        }).start();
    }


    /**
     * Obnoví databázi ze zadaného záložního souboru.
     *
     * <p>Pokud jde o ZIP archiv, rozbalí jej do dočasného umístění a postupně obnoví
     * jednotlivé databázové soubory. U nezipových záloh deleguje obnovu přímo na
     * {@link #recoverDatabaseFromFile(Context, DocumentFile)}.</p>
     *
     * @param context kontext aplikace
     * @param f       záložní soubor určený k obnově
     * @return {@code true}, pokud se obnovu podařilo dokončit úspěšně
     */
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
     * Obnoví jednu databázi ze záložního souboru.
     *
     * @param context kontext aplikace
     * @param f       soubor obsahující databázi pro obnovu
     * @return {@code true}, pokud byla databáze obnovena úspěšně
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
            File currentDB = resolveTargetDatabaseFile(context, f);
            if (currentDB == null)
                return false;

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

            ShPSubscriptionPoint shPSubscriptionPoint = new ShPSubscriptionPoint(context);
            shPSubscriptionPoint.set(ShPSubscriptionPoint.ID_SUBSCRIPTION_POINT_LONG, -1L);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "recoverDatabaseFromFile: " + e.getMessage());
            return false;
        }
    }


    /**
     * Určí cílový databázový soubor ve vnitřním úložišti podle názvu záložního souboru.
     *
     * @param context kontext aplikace
     * @param file    záložní soubor
     * @return cílový soubor databáze, nebo {@code null}, pokud název neodpovídá podporované DB
     */
    private static File resolveTargetDatabaseFile(Context context, DocumentFile file) {
        if (context == null || file == null)
            return null;

        String fileName = file.getName();
        if (fileName == null)
            return null;

        String currentDBPath;
        if (fileName.contains("cenik")) {
            currentDBPath = "//data//" + context.getPackageName() + "//databases//databaze_cenik";
        } else if (fileName.contains("odecet")) {
            currentDBPath = "//data//" + context.getPackageName() + "//databases//odecty_a_mista";
        } else {
            return null;
        }

        return new File(Environment.getDataDirectory(), currentDBPath);
    }


    /**
     * Rozbalí ZIP archiv zálohy do aktuálně vybrané lokální složky záloh.
     *
     * @param context      kontext aplikace
     * @param documentFile soubor se ZIP zálohou
     * @return seznam rozbalených souborů
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
     * Smaže dočasné soubory vytvořené při rozbalení ZIP zálohy.
     *
     * @param documentFiles seznam dočasných souborů určených ke smazání
     */
    public static void deleteDirectory(ArrayList<DocumentFile> documentFiles) {
        for (int i = 0; i < documentFiles.size(); i++) {
            documentFiles.get(i).delete();
        }
    }


    /**
     * Callback vracející výsledek dokončení obnovy databáze.
     */
    public interface RecoverDatabaseCallback {

        /**
         * Vrátí výsledek obnovy databáze.
         *
         * @param result {@code true}, pokud byla obnova úspěšná
         */
        void onComplete(boolean result);

    }

}
