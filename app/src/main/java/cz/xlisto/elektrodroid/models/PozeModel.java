package cz.xlisto.elektrodroid.models;

import androidx.annotation.NonNull;

/**
 * Model pro poze
 * Xlisto 15.02.2023 14:19
 */
public class PozeModel {
    private static final String TAG = "PozeModel";
    double poze1,poze2;
    TypePoze typePoze;


    public PozeModel(double poze1, double poze2) {
        this.poze1 = poze1;
        this.poze2 = poze2;
    }


    public PozeModel(TypePoze typePoze) {
        this.typePoze = typePoze;
    }


    public double getPoze1() {
        return poze1;
    }


    public double getPoze2() {
        return poze2;
    }


    public void addPoze1(double p) {
        poze1 += p;
    }


    public void addPoze2(double p) {
        poze2 += p;
    }


    /**
     * Vybere menší POZE
     * Spotřeba se zadává v MWh
     *
     * @return menší POZE
     */
    public double getPoze() {
        return Math.min(poze1, poze2);
    }


    /**
     * Vybere menší POZE
     * Spotřeba se zadává v MWh
     *
     * @return menší POZE
     */
    public double getMinPoze() {
        return getPoze();
    }


    /**
     * Zjistí, zda-li je použitý max POZE (495)
     *
     * @return false = POZE podle jističe; true = POZE podle spotřeby
     */
    public boolean isMAXPoze() {
        return !(poze1 < poze2);
    }


    /**
     * Vrátí typ poze
     *
     * @return typ poze
     */
    public TypePoze getTypePoze() {
        if (poze1 < poze2)
            return TypePoze.POZE1;
        else
            return TypePoze.POZE2;
    }


    @NonNull
    @Override
    public String toString() {
        return "PozeModel{" +
                "poze1=" + poze1 +
                ", poze2=" + poze2 +
                ", typePoze=" + typePoze +
                '}';
    }


    /**
     * Typy poze
     * POZE1 - podle hodnoty jističe
     * POZE2 - podle spotřeby - max. 495 kč/MWh
     */
    public enum TypePoze {
        POZE1,
        POZE2
    }
}
