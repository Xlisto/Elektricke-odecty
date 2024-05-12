package cz.xlisto.elektrodroid.models;

/**
 * Model pro zobrazení souhrnu ceníku
 * Xlisto 12.12.2023 11:32
 */
public class PriceListSumModel {
    private static final String TAG = "PriceListSumModel";
    private final String rada;
    private final String area;
    private final long datum;
    private final String firma;
    private final int count;

    public PriceListSumModel(String rada, String area, long datum, String firma, int count) {
        this.rada = rada;
        this.area = area;
        this.datum = datum;
        this.firma = firma;
        this.count = count;
    }

    public String getRada() {
        return rada;
    }

    public String getArea() {
        return area;
    }

    public long getDatum() {
        return datum;
    }

    public String getFirma() {
        return firma;
    }

    public int getCount() {
        return count;
    }
}
