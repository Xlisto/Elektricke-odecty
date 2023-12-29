package cz.xlisto.odecty.models;

import androidx.annotation.NonNull;

/**
 * Model součtu záznamů faktur
 * <p>
 * Xlisto 26.12.2023 21:13
 */
public class InvoiceSumModel {
    private static final String TAG = "InvoiceSumModel";
    private final long number;
    private final long id_fak;
    private final long dateStart;
    private final long dateEnd;
    private final double totalVT;
    private final double totalNT;


    public InvoiceSumModel(long id_fak, long number, long dateStart, long dateEnd, double totalVT, double totalNT) {
        this.id_fak = id_fak;
        this.number = number;
        this.dateStart = dateStart;
        this.dateEnd = dateEnd;
        this.totalVT = totalVT;
        this.totalNT = totalNT;
    }


    public long getId_fak() {
        return id_fak;
    }


    public long getNumber() {return number;}


    public long getDateStart() {
        return dateStart;
    }


    public long getDateEnd() {
        return dateEnd;
    }


    public double getTotalVT() { return totalVT;}


    public double getTotalNT() {
        return totalNT;
    }


    public double getTotal() {return totalVT + totalNT;}


    @NonNull
    @Override
    public String toString() {
        return "InvoiceSumModel{" +
                ", id_fak=" + id_fak +
                ", number=" + number +
                ", dateStart=" + dateStart +
                ", dateEnd=" + dateEnd +
                ", totalVT=" + totalVT +
                ", totalNT=" + totalNT +
                '}';
    }
}
