package cz.xlisto.elektrodroid.modules.pricelist.pdf;


import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import java.util.Objects;


/**
 * Datový model popisující jeden podporovaný formát PDF ceníku elektřiny pro vizuální nápovědu.
 * <p>
 * Odděluje metadata dodavatele a ukázkového dokumentu od uživatelského rozhraní.
 * Jednomu dodavateli (identifikovanému pomocí {@link #supplierId}) lze v budoucnu přiřadit
 * více variant formátů ceníku s vlastním {@link #id}, náhledem a popisem.
 */
public final class SupportedPriceList {

    @NonNull
    private final String id;
    @NonNull
    private final String supplierId;
    @StringRes
    private final int supplierNameResId;
    @NonNull
    private final String previewAssetPath;
    private final int samplePageCount;
    @StringRes
    private final int descriptionResId;
    @StringRes
    private final int warningResId;
    @Nullable
    private final String parserId;


    /**
     * Vytvoří novou definici podporovaného PDF ceníku.
     *
     * @param id                Jednoznačný identifikátor položky/varianty formátu
     * @param supplierId        Jednoznačný identifikátor dodavatele (např. {@code "cez"}, {@code "eon"})
     * @param supplierNameResId Identifikátor textového zdroje se zobrazovaným názvem dodavatele
     * @param previewAssetPath  Relativní cesta k náhledovému obrázku ve složce {@code assets}
     * @param samplePageCount   Počet stran ukázkového PDF dokumentu (pouze informativní údaj)
     * @param descriptionResId  Identifikátor textového zdroje s popisem struktury ceníku
     * @param warningResId      Volitelný identifikátor specifického upozornění ({@code 0}, pokud není)
     * @param parserId          Volitelný identifikátor odpovídajícího parseru
     */
    public SupportedPriceList(
            @NonNull String id,
            @NonNull String supplierId,
            @StringRes int supplierNameResId,
            @NonNull String previewAssetPath,
            int samplePageCount,
            @StringRes int descriptionResId,
            @StringRes int warningResId,
            @Nullable String parserId
    ) {
        this.id = Objects.requireNonNull(id, "id nesmí být null");
        this.supplierId = Objects.requireNonNull(supplierId, "supplierId nesmí být null");
        this.supplierNameResId = supplierNameResId;
        this.previewAssetPath = Objects.requireNonNull(previewAssetPath, "previewAssetPath nesmí být null");
        this.samplePageCount = samplePageCount;
        this.descriptionResId = descriptionResId;
        this.warningResId = warningResId;
        this.parserId = parserId;
    }


    /**
     * Vrátí jednoznačný identifikátor této definice podporovaného formátu.
     *
     * @return identifikátor formátu
     */
    @NonNull
    public String getId() {
        return id;
    }


    /**
     * Vrátí jednoznačný identifikátor dodavatele.
     *
     * @return identifikátor dodavatele
     */
    @NonNull
    public String getSupplierId() {
        return supplierId;
    }


    /**
     * Vrátí ID textového zdroje se zobrazovaným názvem dodavatele.
     *
     * @return string resource ID názvu dodavatele
     */
    @StringRes
    public int getSupplierNameResId() {
        return supplierNameResId;
    }


    /**
     * Vrátí cestu k náhledovému obrázku v {@code assets}.
     *
     * @return relativní cesta v {@code assets}
     */
    @NonNull
    public String getPreviewAssetPath() {
        return previewAssetPath;
    }


    /**
     * Vrátí informativní počet stran ukázkového PDF dokumentu.
     *
     * @return počet stran ukázky
     */
    public int getSamplePageCount() {
        return samplePageCount;
    }


    /**
     * Vrátí ID textového zdroje s popisem charakteristických znaků ceníku.
     *
     * @return string resource ID popisu
     */
    @StringRes
    public int getDescriptionResId() {
        return descriptionResId;
    }


    /**
     * Vrátí ID textového zdroje volitelného specifického upozornění, nebo {@code 0}.
     *
     * @return string resource ID upozornění nebo {@code 0}
     */
    @StringRes
    public int getWarningResId() {
        return warningResId;
    }


    /**
     * Zjistí, zda má tento dodavatel/formát nastaveno specifické upozornění.
     *
     * @return {@code true}, pokud je nastaveno upozornění
     */
    public boolean hasWarning() {
        return warningResId != 0;
    }


    /**
     * Vrátí volitelný identifikátor příslušného PDF parseru.
     *
     * @return identifikátor parseru nebo {@code null}
     */
    @Nullable
    public String getParserId() {
        return parserId;
    }

}
