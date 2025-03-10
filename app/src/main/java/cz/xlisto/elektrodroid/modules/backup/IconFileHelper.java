package cz.xlisto.elektrodroid.modules.backup;


import android.content.Context;

import cz.xlisto.elektrodroid.R;


/**
 * Třída IconFileHelper poskytuje metody pro nastavení správné ikony a typu záložního souboru.
 */
public class IconFileHelper {

    private static boolean cenik = false;
    private static boolean odecet = false;
    private static boolean zip_old = false;
    private static boolean zip_now = false;


    /**
     * Vrátí ikonu na základě názvu souboru a typu MIME.
     *
     * @param name     Název souboru.
     * @param mimeType Typ MIME souboru.
     * @return Identifikátor zdroje ikony.
     */
    public static int getIcon(String name, String mimeType) {
        if (mimeType.equals("application/vnd.google-apps.folder")) {
            return R.mipmap.google_folder;
        } else
            return getIcon(name);
    }


    /**
     * Vrátí ikonu na základě názvu souboru.
     *
     * @param name Název souboru.
     * @return Identifikátor zdroje ikony.
     */
    public static int getIcon(String name) {
        isContains(name);
        if (cenik) {
            return R.mipmap.ic_cenik;
        } else if (odecet) {
            return R.mipmap.ic_odecet;
        } else if (zip_old) {
            return R.mipmap.ic_odecet;
        } else if (zip_now) {
            return R.mipmap.ic_odecet_new;
        } else {
            return R.mipmap.file;
        }
    }


    /**
     * Vrátí typ záložního souboru na základě názvu souboru.
     *
     * @param name    Název souboru.
     * @param context Kontext aplikace pro přístup k prostředkům.
     * @return Řetězec obsahující typ záložního souboru.
     */
    public static String getType(String name, Context context) {
        isContains(name);
        if (cenik) {
            return context.getResources().getString(R.string.backup_pricelist);
        } else if (odecet) {
            return context.getResources().getString(R.string.backup_readings);
        } else if (zip_old || zip_now) {
            return context.getResources().getString(R.string.backup_zip);
        } else {
            return "";
        }
    }


    /**
     * Vrátí název souboru bez specifických přípon.
     *
     * @param name Název souboru.
     * @return Název souboru bez přípon.
     */
    public static String getName(String name) {
        isContains(name);
        return name.replace(".cenik", "").replace(".odecet", "").replace(" ElektroDroid.zip", "").replace("El odecet.zip", "");

    }


    /**
     * Nastaví příznaky na základě toho, zda název souboru obsahuje specifické přípony.
     *
     * @param name Název souboru.
     */
    private static void isContains(String name) {
        cenik = name.contains(".cenik");
        odecet = name.contains(".odecet");
        zip_old = name.contains("ElektroDroid.zip");
        zip_now = name.contains("El odecet.zip");
    }

}
