package cz.xlisto.cenik.models;

/**
 * Xlisto 15.02.2023 14:19
 */
public class PozeModel {
    private static final String TAG = "PozeModel";

    double poze1;
    double poze2;

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
     * @return
     */
    public double getPoze() {
        if (poze1 < poze2)
            return poze1;
        else
            return poze2;
    }

    /**
     * Vybere menší POZE
     * Spotřeba se zadává v MWh
     *
     * @return
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
        if (poze1 < poze2)
            return false;
        return true;
    }

    public TypePoze getTypePoze() {
        if (poze1 < poze2)
            return TypePoze.POZE1;
        else
            return TypePoze.POZE2;
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
