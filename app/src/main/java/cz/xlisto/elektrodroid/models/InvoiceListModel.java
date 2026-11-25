package cz.xlisto.elektrodroid.models;


/**
 * Datový model představující souhrnné informace o faktuře nebo fakturačním období.
 *
 * @param idFak         identifikátor faktury v databázi (-1 pro období bez faktury)
 * @param numberInvoice číslo faktury nebo název období
 * @param minDate       počáteční datum fakturovaného období v milisekundách
 * @param maxDate       koncové datum fakturovaného období v milisekundách
 * @param payments      počet plateb přiřazených k dané faktuře
 * @param reads         počet záznamů odečtů v daném fakturačním období
 * @param minVT         počáteční stav vysokého tarifu (VT)
 * @param maxVT         koncový stav vysokého tarifu (VT)
 * @param minNT         počáteční stav nízkého tarifu (NT)
 * @param maxNT         koncový stav nízkého tarifu (NT)
 */
public record InvoiceListModel(
        long idFak,
        String numberInvoice,
        long minDate,
        long maxDate,
        long payments,
        long reads,
        double minVT,
        double maxVT,
        double minNT,
        double maxNT
) {

    /**
     * Vrátí číslo faktury nebo název období.
     *
     * @return číslo faktury
     */
    public String getNumberInvoice() {
        return numberInvoice;
    }


    /**
     * Vrátí identifikátor faktury.
     *
     * @return ID faktury
     */
    public long getIdFak() {
        return idFak;
    }


    /**
     * Vrátí počáteční datum fakturovaného období.
     *
     * @return počáteční datum v milisekundách
     */
    public long getMinDate() {
        return minDate;
    }


    /**
     * Vrátí koncové datum fakturovaného období.
     *
     * @return koncové datum v milisekundách
     */
    public long getMaxDate() {
        return maxDate;
    }


    /**
     * Vrátí počet plateb přiřazených k dané faktuře.
     *
     * @return počet plateb
     */
    public long getPayments() {
        return payments;
    }


    /**
     * Vrátí počet záznamů odečtů v období.
     *
     * @return počet odečtů
     */
    public long getReads() {
        return reads;
    }


    /**
     * Vrátí počáteční stav vysokého tarifu.
     *
     * @return počáteční stav VT
     */
    public double getMinVT() {
        return minVT;
    }


    /**
     * Vrátí koncový stav vysokého tarifu.
     *
     * @return koncový stav VT
     */
    public double getMaxVT() {
        return maxVT;
    }


    /**
     * Vrátí počáteční stav nízkého tarifu.
     *
     * @return počáteční stav NT
     */
    public double getMinNT() {
        return minNT;
    }


    /**
     * Vrátí koncový stav nízkého tarifu.
     *
     * @return koncový stav NT
     */
    public double getMaxNT() {
        return maxNT;
    }

}
