package cz.xlisto.elektrodroid.modules.backup;


import android.content.Context;

import cz.xlisto.elektrodroid.R;


/**
 * Nastavení správné ikony záložního souboru a typu
 */
public class IconFileHelper {

    private static boolean cenik = false;
    private static boolean odecet = false;
    private static boolean zip_old = false;
    private static boolean zip_now = false;


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
            return R.mipmap.ic_file;
        }
    }


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


    public static String getName(String name) {
        isContains(name);
        return name.replace(".cenik", "").replace(".odecet", "").replace(" ElektroDroid.zip", "").replace("El odecet.zip", "");

    }


    private static void isContains(String name) {
        cenik = name.contains(".cenik");
        odecet = name.contains(".odecet");
        zip_old = name.contains("ElektroDroid.zip");
        zip_now = name.contains("El odecet.zip");
    }

}
