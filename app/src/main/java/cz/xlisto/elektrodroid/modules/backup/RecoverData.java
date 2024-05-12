package cz.xlisto.elektrodroid.modules.backup;

/**
 * Konstanty  pro zálohu a obnovu dat
 * Xlisto 06.12.2023 17:36
 */
public abstract class RecoverData {
    private static final String TAG = "RecoverData";
    public static final String DEF_URI = "content://com.android.externalstorage.documents/document/primary%3A";
    static final String DEF_TREE_URI = "/tree/primary%3A";
    private static final String[] filtersFileName = {".cenik", ".odecet", "ElektroDroid.zip", "El odecet.zip"};


    /**
     * Vrátí pole obsahující povolené přípony - filterFileName
     */
    public static String[] getFiltersFileName() {
        return filtersFileName;
    }
}
