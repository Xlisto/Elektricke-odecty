package cz.xlisto.elektrodroid.modules.backup;


import androidx.documentfile.provider.DocumentFile;

import java.util.List;

/**
 * Pomocná třída pro řazení lokálních záložních souborů.
 *
 * <p>Řazení probíhá primárně podle data a času zakódovaného v názvu zálohy,
 * protože metadata souboru získaná přes SAF nemusí být při kopírování nebo obnově
 * vždy spolehlivě zachována. Pokud název neobsahuje parsovatelný čas zálohy,
 * použije se jako fallback hodnota {@link DocumentFile#lastModified()}.</p>
 */
public class SortFile {

    /**
     * Rekurzivně seřadí zadaný rozsah seznamu záloh sestupně podle efektivního času.
     *
     * @param list seznam souborů k seřazení
     * @param low  dolní index tříděného rozsahu
     * @param high horní index tříděného rozsahu
     */
    private static void quickSortDate(List<DocumentFile> list, int low, int high) {
        if (list.isEmpty())
            return;

        if (low >= high)
            return;

        // pick the pivot
        int middle = low + (high - low) / 2;
        DocumentFile pivot = list.get(middle);

        // make left < pivot and right > pivot
        int i = low, j = high;
        while (i <= j) {
            while (getEffectiveTimestamp(list.get(i)) > getEffectiveTimestamp(pivot)) {
                i++;
            }

            while (getEffectiveTimestamp(list.get(j)) < getEffectiveTimestamp(pivot)) {
                j--;
            }

            if (i <= j) {
                DocumentFile temp = list.get(i);
                list.set(i, list.get(j));
                list.set(j, temp);
                i++;
                j--;
            }
        }

        // recursively sort two sub parts
        if (low < j)
            quickSortDate(list, low, j);

        if (high > i)
            quickSortDate(list, i, high);
    }

    /**
     * Seřadí seznam záložních souborů sestupně od nejnovější po nejstarší.
     *
     * @param list seznam záloh k seřazení
     */
    public static void quickSortDate(List<DocumentFile> list) {
        quickSortDate(list, 0, list.size() - 1);
    }


    /**
     * Vrátí čas použitelný pro řazení záloh.
     *
     * <p>Primárně se pokusí vyčíst datum a čas přímo z názvu souboru zálohy,
     * protože při kopírování přes SAF nemusí provider spolehlivě zachovat metadata
     * vytvoření/poslední změny. Pokud název neodpovídá očekávanému formátu,
     * použije se fallback na {@link DocumentFile#lastModified()}.</p>
     */
    private static long getEffectiveTimestamp(DocumentFile documentFile) {
        if (documentFile == null)
            return Long.MIN_VALUE;

        Long timestampFromName = BackupFileTimestampHelper.extractTimestampFromName(documentFile.getName());
        if (timestampFromName != null)
            return timestampFromName;

        return documentFile.lastModified();
    }
}
