package cz.xlisto.odecty.modules.backup;

import java.util.List;

import androidx.documentfile.provider.DocumentFile;

/**
 * Xlisto 24.04.2023 12:38
 */
public class SortFile {
    private static final String TAG = "SortFile";

    private static void quickSortDate(List<DocumentFile> list, int low, int high) {
        if (list.size() == 0)
            return;

        if (low >= high)
            return;

        // pick the pivot
        int middle = low + (high - low) / 2;
        DocumentFile pivot = list.get(middle);

        // make left < pivot and right > pivot
        int i = low, j = high;
        while (i <= j) {
            while (list.get(i).lastModified() > pivot.lastModified()) {
                i++;
            }

            while (list.get(j).lastModified() < pivot.lastModified()) {
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

    public static void quickSortDate(List<DocumentFile> list) {
        quickSortDate(list, 0, list.size() - 1);
    }
}
