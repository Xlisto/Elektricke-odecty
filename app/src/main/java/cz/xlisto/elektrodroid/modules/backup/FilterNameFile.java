package cz.xlisto.elektrodroid.modules.backup;


import android.util.Log;

import androidx.documentfile.provider.DocumentFile;

import java.util.Objects;

/**
 * Jednoduchý filtr názvů souborů podle povolených přípon.
 *
 * <p>Třída je určená pro práci s {@link DocumentFile} a umožňuje rozhodnout,
 * zda má být konkrétní soubor akceptován při načítání nebo zobrazování záloh.
 * Složky jsou vždy odmítnuty; pokud není zadaný žádný seznam přípon nebo je prázdný,
 * akceptují se všechny soubory.</p>
 */
public class FilterNameFile {

    private static final String TAG = FilterNameFile.class.getSimpleName();
    private final String[] mExtensions;

    /**
     * Vytvoří filtr s volitelným seznamem povolených přípon.
     *
     * @param mExtensions pole povolených přípon souborů (např. {@code .zip}); může být {@code null} nebo prázdné
     */
    public FilterNameFile(String[] mExtensions) {
        this.mExtensions = mExtensions;
    }

    /**
     * Ověří, zda má být zadaný soubor přijat podle typu položky a jeho přípony.
     *
     * <p>Vrací {@code false} pro složky. Pokud je nastaven seznam povolených přípon,
     * soubor je přijat pouze při shodě alespoň jedné přípony. Pokud seznam přípon
     * není nastaven nebo je prázdný, metoda přijme všechny soubory.</p>
     *
     * @param file testovaný dokumentový soubor ({@link DocumentFile})
     * @return {@code true}, pokud soubor vyhovuje pravidlům filtru; jinak {@code false}
     */
    public boolean accept(DocumentFile file) {
        if (file.isDirectory()) {
            // Kontrola, zda se jedná o složku
            return false;
        }
        if (mExtensions != null && mExtensions.length > 0) {
            for (String mExtension : mExtensions) {
                try {
                    if (Objects.requireNonNull(file.getName()).endsWith(mExtension)) {
                        // Název souboru končí požadovanou příponou
                        return true;
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error checking file extension: " + e.getMessage());
                }
            }
            // Název souboru neodpovídá žádné z povolených přípon
            return false;
        }
        // Nebyly nastaveny žádné přípony – akceptují se všechny přípony souborů
        return true;
    }
}
