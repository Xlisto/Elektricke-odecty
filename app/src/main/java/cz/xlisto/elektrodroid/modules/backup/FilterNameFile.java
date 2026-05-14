package cz.xlisto.elektrodroid.modules.backup;


import android.util.Log;

import androidx.documentfile.provider.DocumentFile;

import java.util.Objects;

/**
 * Jednoduchý filtr názvů souborů podle povolených přípon.
 *
 * <p>Třída je určená pro práci s {@link DocumentFile} a umožňuje rozhodnout,
 * zda má být konkrétní soubor akceptován při načítání záloh. Složky jsou vždy
 * odmítnuty; pokud není zadaný žádný seznam přípon, akceptují se všechny soubory.</p>
 */
public class FilterNameFile {
    private static final String TAG = Class.class.getSimpleName();
    private final String[] mExtensions;

    /**
     * Vytvoří filtr s volitelným seznamem povolených přípon.
     *
     * @param mExtensions pole přípon (např. {@code .zip}); může být {@code null}
     */
    public FilterNameFile(String[] mExtensions) {
        this.mExtensions = mExtensions;
    }

    /**
     * Ověří, zda má být soubor přijat podle typu položky a přípony názvu.
     *
     * <p>Vrací {@code false} pro složky. Pokud je nastaven seznam přípon,
     * soubor je přijat pouze při shodě alespoň jedné přípony. Pokud seznam
     * přípon není nastaven, metoda přijme všechny soubory.</p>
     *
     * @param file testovaný dokumentový soubor
     * @return {@code true}, pokud soubor splňuje pravidla filtru
     */
    public boolean accept(DocumentFile file) {
        if (file.isDirectory()) {
            // kontrola na to zda je to složka
            return false;
        }
        if (mExtensions != null && mExtensions.length > 0) {
            for (String mExtension : mExtensions) {
                try {
                    if (Objects.requireNonNull(file.getName()).endsWith(mExtension)) {
                        // The filename ends with the extension
                        return true;
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error checking file extension: " + e.getMessage());
                }
            }
            // The filename did not match any of the extensions
            return false;
        }
        // No extensions has been set. Accept all file extensions.
        return true;
    }
}
