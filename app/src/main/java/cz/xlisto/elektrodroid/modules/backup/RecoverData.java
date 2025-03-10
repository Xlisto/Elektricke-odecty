package cz.xlisto.elektrodroid.modules.backup;

/**
 * Abstraktní třída RecoverData poskytuje základní konstanty a metody
 * pro zálohu a obnovu dat.
 */
public abstract class RecoverData {
    private static final String TAG = "RecoverData";
    public static final String DEF_URI = "content://com.android.externalstorage.documents/document/primary%3A";
    static final String DEF_TREE_URI = "/tree/primary%3A";
    private static final String[] filtersFileName = {".cenik", ".odecet", "ElektroDroid.zip", "El odecet.zip"};


    /**
     * Vrátí pole obsahující povolené přípony - filterFileName.
     *
     * @return Pole řetězců obsahující povolené přípony souborů.
     */
    public static String[] getFiltersFileName() {
        return filtersFileName;
    }
}
