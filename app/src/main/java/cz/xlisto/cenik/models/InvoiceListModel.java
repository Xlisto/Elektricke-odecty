package cz.xlisto.cenik.models;

/**
 * Xlisto 01.02.2023 19:35
 */
public class InvoiceListModel {
    private static final String TAG = "InvoiceModel";

    private long idFak;
    private String numberInvoice;
    private long minDate, maxDate, payments, reads;
    private double minVT, maxVT, minNT, maxNT;


    public InvoiceListModel(long idFak, String numberInvoice, long minDate, long maxDate, long payments, long reads, double minVT, double maxVT, double minNT, double maxNT) {
        this(numberInvoice);
        this.idFak = idFak;
        this.minDate = minDate;
        this.maxDate = maxDate;
        this.payments = payments;
        this.reads = reads;
        this.minVT = minVT;
        this.maxVT = maxVT;
        this.minNT = minNT;
        this.maxNT = maxNT;
    }

    public InvoiceListModel(String numberInvoice) {
        this.numberInvoice = numberInvoice;
    }

    public String getNumberInvoice() {
        return numberInvoice;
    }

    public long getIdFak() {
        return idFak;
    }

    public long getMinDate() {
        return minDate;
    }

    public long getMaxDate() {
        return maxDate;
    }

    public long getPayments() {
        return payments;
    }

    public long getReads() {
        return reads;
    }

    public double getMinVT() {
        return minVT;
    }

    public void setMinVT(double minVT) {
        this.minVT = minVT;
    }

    public double getMaxVT() {
        return maxVT;
    }

    public void setMaxVT(double maxVT) {
        this.maxVT = maxVT;
    }

    public double getMinNT() {
        return minNT;
    }

    public void setMinNT(double minNT) {
        this.minNT = minNT;
    }

    public double getMaxNT() {
        return maxNT;
    }

    public void setMaxNT(double maxNT) {
        this.maxNT = maxNT;
    }
}
