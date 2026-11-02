package cz.xlisto.elektrodroid.models;


import androidx.annotation.NonNull;


/**
 * Model součtu záznamů faktur
 * <p>
 * Xlisto 26.12.2023 21:13
 */
public record InvoiceSumModel(long id_fak, long number, long dateStart, long dateEnd,
                              double totalVT, double totalNT) {

    public double getTotal() {
        return totalVT + totalNT;
    }


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
