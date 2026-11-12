package cz.xlisto.elektrodroid.modules.pricelist.download;


import java.io.Serializable;


/**
 * Model položky dodavatele pro stažení PDF ceníků.
 */
public class SupplierDownloadModel implements Serializable {

    private final String name;
    private final String url;


    /**
     * Vytvoří novou instanci položky dodavatele.
     *
     * @param name Název dodavatele
     * @param url  Webová adresa stránky s ceníky ke stažení
     */
    public SupplierDownloadModel(String name, String url) {
        this.name = name;
        this.url = url;
    }


    /**
     * Vrátí název dodavatele.
     *
     * @return Název dodavatele
     */
    public String getName() {
        return name;
    }


    /**
     * Vrátí webovou adresu stránky s ceníky ke stažení.
     *
     * @return URL adresa
     */
    public String getUrl() {
        return url;
    }

}
