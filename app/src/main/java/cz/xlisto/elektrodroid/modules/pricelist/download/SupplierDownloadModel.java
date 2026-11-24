package cz.xlisto.elektrodroid.modules.pricelist.download;


import java.io.Serializable;


/**
 * Model položky dodavatele pro stažení PDF ceníků.
 */
public record SupplierDownloadModel(String name, String url) implements Serializable {

    /**
     * Vytvoří novou instanci položky dodavatele.
     *
     * @param name Název dodavatele
     * @param url  Webová adresa stránky s ceníky ke stažení
     */
    public SupplierDownloadModel {
    }


    /**
     * Vrátí název dodavatele.
     *
     * @return Název dodavatele
     */
    @Override
    public String name() {
        return name;
    }


    /**
     * Vrátí webovou adresu stránky s ceníky ke stažení.
     *
     * @return URL adresa
     */
    @Override
    public String url() {
        return url;
    }

}
