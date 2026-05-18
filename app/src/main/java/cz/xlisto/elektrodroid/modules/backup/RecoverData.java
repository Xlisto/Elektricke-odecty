package cz.xlisto.elektrodroid.modules.backup;

/**
 * Základní abstraktní třída pro práci se zálohami aplikace.
 *
 * <p>Zapouzdřuje sdílené konstanty používané při výběru cílové složky záloh
 * a při filtrování podporovaných typů souborů. Slouží jako společný základ
 * pro třídy zajišťující ukládání i obnovu záloh.</p>
 */
public abstract class RecoverData {
    /** Výchozí URI používané jako fallback při práci se stromovým výběrem složky. */
    public static final String DEF_URI = "content://com.android.externalstorage.documents/document/primary%3A";

    /** Výchozí tree URI reprezentující kořen primárního externího úložiště. */
    static final String DEF_TREE_URI = "/tree/primary%3A";

    /** Podporované názvy/přípony souborů používané při filtrování záloh. */
    private static final String[] filtersFileName = {".cenik", ".odecet", "ElektroDroid.zip", "El odecet.zip"};


    /**
     * Vrátí seznam podporovaných názvů a přípon souborů záloh.
     *
     * <p>Výsledek se používá zejména při filtrování lokálních souborů ve vybrané složce,
     * aby byly zobrazeny pouze relevantní typy záloh aplikace.</p>
     *
     * @return pole řetězců reprezentující podporované názvy nebo přípony záloh
     */
    public static String[] getFiltersFileName() {
        return filtersFileName;
    }
}
