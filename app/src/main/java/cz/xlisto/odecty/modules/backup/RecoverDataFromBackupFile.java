package cz.xlisto.odecty.modules.backup;

import android.content.Context;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import androidx.documentfile.provider.DocumentFile;
import cz.xlisto.odecty.databaze.DataPriceListSource;
import cz.xlisto.odecty.databaze.DataSubscriptionPointSource;
import cz.xlisto.odecty.shp.ShPBackup;
import cz.xlisto.odecty.shp.ShPSubscriptionPoint;


/**
 * Obnoví data ze záložních souborů
 * Xlisto 13.05.2023 10:04
 */
public class RecoverDataFromBackupFile {
    private static final String TAG = "RecoverDataFromFile";
    private static final String DEF_URI = "content://com.android.externalstorage.documents/document/primary%3A";


    public static void recoverDatabaseFromZip(Context context,DocumentFile f) {
        if (f == null || context == null) {
            Toast.makeText(context, "Data se neobnovila", Toast.LENGTH_LONG).show();
            return;
        }

        if (Objects.requireNonNull(f.getName()).contains(".zip")) {
            //zip archiv, který se rozbalí do povolený složky
            ArrayList<DocumentFile> files = unzip(context,f);
            for (int i = 0; i < files.size(); i++) {
                Log.w(TAG, "Rozbalené soubory: " + files.get(i).getName());
                if (Objects.equals(files.get(i).getName(), "odecet.db") || Objects.equals(files.get(i).getName(), "cenik.db")) {
                    recoverDatabaseFromFile(context,files.get(i));
                }
            }
            //úklid složky temp
            deleteDirectory(files);
        } else {
            //původní záložní soubory
            recoverDatabaseFromFile(context,f);
        }
    }


    /**
     * Obnoví databázi ze záložních souborů
     * @param context kontext aplikace
     * @param f DocumentFile soubor
     */
    private static void recoverDatabaseFromFile(Context context, DocumentFile f) {
        if (f == null || context == null) {
            return;
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
                Log.w(TAG, " soubor přípona: .cenik");
                currentDBPath = "//data//" + context.getPackageName() + "//databases//databaze_cenik";
            }

            boolean odecet = f.getName().contains("odecet");
            if (odecet) {
                Log.w(TAG, " soubor přípona: .odecet");
                currentDBPath = "//data//" + context.getPackageName() + "//databases//odecty_a_mista";
            }

            File currentDB = new File(data, currentDBPath);//vnitřní databáze

            OutputStream src = new FileOutputStream(currentDB);
            InputStream dst = context.getContentResolver().openInputStream(f.getUri());

            int c;
            byte[] buffer = new byte[1024];
            while ((c = dst.read(buffer)) != -1) {
                src.write(buffer, 0, c);
            }

            src.close();
            src.flush();
            dst.close();

            //nastavení barev VT a NT
            //TODO: doplnit nastavení aplikace
            //new Databaze(getActivity()).getColorApp();//načtení barev z databáze a nastavení do sharedPreferences, otevírá si a zavírá databázy
            ShPSubscriptionPoint shPSubscriptionPoint = new ShPSubscriptionPoint(context);
            shPSubscriptionPoint.set(ShPSubscriptionPoint.ID_SUBSCRIPTION_POINT, -1L);
            Toast.makeText(context, "Úspěšně obnoveno. Zvolte aktuální odběrné místo.", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(context, "Bohužel se něco nepovedlo při obnově: \n" + e, Toast.LENGTH_LONG).show();
        }
    }


    /**
     * Rozbalení ZIP archivu a uložení do stávající složky
     *
     * @param documentFile soubor se zálohou
     * @return ArrayList<DocumentFile> seznam souborů
     */
    private static ArrayList<DocumentFile> unzip(Context context,DocumentFile documentFile) {
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
                    fos.write(buffer, 0, len);
                }

                fos.close();
                files.add(newFiled);
                ze = zis.getNextEntry();
            }
            zis.closeEntry();
            zis.close();
        } catch (IOException e) {
            e.printStackTrace();
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
}
