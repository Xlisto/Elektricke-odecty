package cz.xlisto.elektrodroid.models;


import androidx.annotation.NonNull;


/**
 * Xlisto 24.08.2023 20:59
 */
public class YearConsuptionModel implements ConsuptionModel {

    private static final String TAG = "YearConsuptionModel";
    private Double consuptionVT;
    private Double consuptionNT;
    private int year;
    private int anim;
    private int max;


    /**
     * Konstruktor
     *
     * @param consuptionVT spotřeba VT
     * @param consuptionNT spotřeba NT
     * @param year         rok
     */
    public YearConsuptionModel(Double consuptionVT, Double consuptionNT, int year) {
        this.consuptionVT = consuptionVT;
        this.consuptionNT = consuptionNT;
        this.year = year;
    }


    /**
     * Vrátí spotřebu VT
     *
     * @return spotřeba VT
     */
    public Double getConsuptionVT() {
        return consuptionVT;
    }


    /**
     * Vrátí spotřebu NT
     *
     * @return spotřeba NT
     */
    public Double getConsuptionNT() {
        return consuptionNT;
    }


    /**
     * Vrátí rok
     *
     * @return rok
     */
    public int getYearAsInt() {
        return year;
    }


    /**
     * Vrátí měsíc - v tomto případě null, protože je to roční spotřeba
     *
     * @return null
     */
    @Override
    public String getDateMonth() {
        return null;
    }


    /**
     * Nastaví datum
     *
     * @param dates datum v long
     */
    @Override
    public void setDates(Long dates) {

    }


    /**
     * Nastaví příznak prvního odečtu
     *
     * @param i 1 - první odečet; 0 - není první odečet
     */
    @Override
    public void setFirstReader(int i) {

    }


    /**
     * Vrátí rok jako String
     *
     * @return rok jako String
     */
    public String getYearAsString() {
        return String.valueOf(getYearAsInt());
    }


    /**
     * Přidá spotřebu VT
     *
     * @param spotrebaVT spořeba jež se připočítá ke stávající spotřebě
     */
    public void addConsuptionVT(Double spotrebaVT) {
        this.consuptionVT = this.consuptionVT + spotrebaVT;
    }


    /**
     * Přidá spotřebu NT
     *
     * @param spotrebaNT spořeba jež se připočítá ke stávající spotřebě
     */
    public void addConsuptionNT(Double spotrebaNT) {
        this.consuptionNT = this.consuptionNT + spotrebaNT;
    }


    /**
     * Nastaví rok
     *
     * @param year rok
     */
    public void setYear(int year) {
        this.year = year;
    }


    /**
     * Animovaná spotřeba
     *
     * @return hodnota roční spotřeby VT pro animaci
     */
    public Integer getConsuptionVTAnimace() {
        return (int) (getConsuptionVT() / max * anim);
    }


    /**
     * Animovaná spotřeba
     *
     * @return hodnota roční spotřeby NT pro animaci
     */
    public Integer getConsuptionNTAnimace() {
        return (int) (getConsuptionNT() / max * anim);
    }


    /**
     * Nastaví parametrů animace
     *
     * @param anim počet snímků animace
     * @param max maximální hodnota
     */
    public void setAnimate(int anim, int max) {
        this.anim = anim;
        this.max = max;
    }


    @NonNull
    @Override
    public String toString() {
        return "Roční spotřeba: " + getYearAsInt() + " VT:" + getConsuptionVT() + " NT:" + getConsuptionNT();
    }


    @Override
    public String getDateMonthAsStringShort() {
        return "";
    }


    @Override
    public String getDateMonthAsStringLong() {
        return "";
    }

}
