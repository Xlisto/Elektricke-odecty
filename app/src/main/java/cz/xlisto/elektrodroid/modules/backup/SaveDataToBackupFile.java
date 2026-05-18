package cz.xlisto.elektrodroid.modules.backup;


import android.content.Context;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.widget.Toast;

import androidx.documentfile.provider.DocumentFile;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.Calendar;
import java.util.Date;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import cz.xlisto.elektrodroid.BuildConfig;
import cz.xlisto.elektrodroid.R;
import cz.xlisto.elektrodroid.databaze.DataMonthlyReadingSource;
import cz.xlisto.elektrodroid.ownview.ViewHelper;
import cz.xlisto.elektrodroid.shp.ShPBackup;
import cz.xlisto.elektrodroid.utils.MyToast;


/**
 * Servisní třída pro vytvoření lokální ZIP zálohy databází aplikace.
 *
 * <p>Třída připraví doplňkový informační soubor, sestaví archiv s podporovanými
 * databázemi a uloží jej do uživatelem vybrané složky záloh přes SAF.
 * Následně vrací výsledek volajícímu přes volitelný {@link Handler}.</p>
 */
public class SaveDataToBackupFile extends RecoverData {

    private static final Logger LOGGER = LoggerFactory.getLogger(SaveDataToBackupFile.class.getName());


    /**
     * Vytvoří a uloží ZIP zálohu databází do aktuálně zvolené složky záloh.
     *
     * @param context kontext aplikace
     * @param handler volitelný handler pro zpětné předání vytvořeného souboru
     */
    public static void saveToZip(Context context, Handler handler) {
        ShPBackup shPBackup = new ShPBackup(context);
        Uri treeUri = Uri.parse(shPBackup.get(ShPBackup.FOLDER_BACKUP, DEF_TREE_URI));
        DocumentFile pickedDir = DocumentFile.fromTreeUri(context, treeUri);

        if (pickedDir == null || !pickedDir.canWrite()) {
            MyToast.makeText(context, context.getResources().getString(R.string.no_folder), Toast.LENGTH_SHORT).show();
            return;
        }

        //vytvoření a zápis do textového souboru s posledními záznamy
        Date date = new Date();
        String s = readDataFromDatabase(context);
        try (OutputStreamWriter outputStreamWriter = new OutputStreamWriter(context.openFileOutput("info.txt", Context.MODE_PRIVATE))) {
            outputStreamWriter.write(s);
        } catch (Exception e) {
            LOGGER.error("Error while writing to file info.txt", e);
        }

        //uložení zálohovaných souborů do ZIPu
        DocumentFile f = pickedDir.createFile("plain/text", generateNameFile(date.getTime()) + " " + RecoverData.getFiltersFileName()[3]);

        String applicationId = BuildConfig.APPLICATION_ID;
        File f1 = new File(Environment.getDataDirectory(), "//data//" + applicationId + "//databases//odecty_a_mista");
        File f2 = new File(Environment.getDataDirectory(), "//data//" + applicationId + "//databases//databaze_cenik");

        boolean isDirectory = false;
        for (DocumentFile file : pickedDir.listFiles()) {
            isDirectory = Objects.requireNonNull(file.getParentFile()).isDirectory();

        }
        //musí tady být kontrola na výběr a oprávnění složky. Pokud není, vrací chybu: requires android.permission.MANAGE_DOCUMENTS or android.permission.MANAGE_DOCUMENTS

        if (pickedDir.canWrite() && isDirectory) {
            try {
                File f3 = new File((context).getFilesDir() + "//info.txt");

                assert f != null;
                OutputStream fos = context.getContentResolver().openOutputStream(f.getUri());
                ZipOutputStream zos = new ZipOutputStream(fos);
                ZipEntry zeOdecet = new ZipEntry("odecet.db");
                ZipEntry zeCenik = new ZipEntry("cenik.db");
                ZipEntry zeInfo = new ZipEntry("info.txt");

                if (f1.exists()) {
                    writeToFile(f1, zeOdecet, zos);
                }

                if (f2.exists()) {
                    writeToFile(f2, zeCenik, zos);
                }

                if (f3.exists()) {
                    writeToFile(f3, zeInfo, zos);
                }

                zos.closeEntry();
                zos.close();
                Toast.makeText(context, context.getResources().getString(R.string.backup_created), Toast.LENGTH_SHORT).show();

                //smazání dočasného souboru info.txt
                boolean deleted = f3.delete();
                if (!deleted)
                    LOGGER.warn("Temporary file info.txt was not deleted");
            } catch (IOException e) {
                LOGGER.error("Error while writing to file", e);
            }
            if (handler != null)
                handler.sendMessage(handler.obtainMessage(1, f));
        }
    }


    /**
     * Zapíše jeden soubor jako položku do otevřeného ZIP archivu.
     *
     * @param file     soubor se zdrojovými daty
     * @param zipEntry cílová položka v ZIP archivu
     * @param zos      otevřený zip output stream
     * @throws IOException při chybě čtení nebo zápisu
     */
    private static void writeToFile(File file, ZipEntry zipEntry, ZipOutputStream zos) throws IOException {
        byte[] buffer = new byte[1024];
        int length;
        zos.putNextEntry(zipEntry);
        try (FileInputStream in = new FileInputStream(file)) {
            while ((length = in.read(buffer)) > 0) {
                zos.write(buffer, 0, length);
            }
        }
    }


    /**
     * Načte textový přehled posledních měsíčních odečtů ze všech odběrných míst.
     *
     * @param context kontext aplikace
     * @return textový výpis posledních odečtů
     */
    private static String readDataFromDatabase(Context context) {
        DataMonthlyReadingSource dataMonthlyReadingSource = new DataMonthlyReadingSource(context);
        dataMonthlyReadingSource.open();
        String s = dataMonthlyReadingSource.getLastMonthlyReadingAsText();
        dataMonthlyReadingSource.close();
        return s;
    }


    /**
     * Vygeneruje časovou část názvu záložního souboru podle zadaného timestampu.
     *
     * @param l čas v milisekundách od epochy
     * @return naformátovaný čas vhodný do názvu zálohy
     */
    private static String generateNameFile(long l) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(l);
        return ViewHelper.getSimpleDateTimeFormatForFiles().format(new Date(calendar.getTimeInMillis()));
    }

}
