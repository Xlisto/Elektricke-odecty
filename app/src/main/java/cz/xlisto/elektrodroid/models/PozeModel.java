package cz.xlisto.elektrodroid.models;


import androidx.annotation.NonNull;


/**
 * Model uchovávající obě varianty poplatku za podporované zdroje energie (POZE):
 * {@code poze1} podle hodnoty hlavního jističe a {@code poze2} podle spotřeby.
 * Umožňuje jejich postupnou kumulaci napříč obdobími a určení výhodnější (nižší) varianty.
 * <p>
 * Xlisto 15.02.2023 14:19
 */
public class PozeModel {

    double poze1, poze2;


    /**
     * Vytvoří instanci modelu s počátečními částkami obou variant POZE.
     *
     * @param poze1 částka POZE vypočtená podle hodnoty hlavního jističe (v Kč bez DPH)
     * @param poze2 částka POZE vypočtená podle spotřeby (v Kč bez DPH)
     */
    public PozeModel(double poze1, double poze2) {
        this.poze1 = poze1;
        this.poze2 = poze2;
    }


    /**
     * Vrací částku POZE vypočtenou podle hodnoty hlavního jističe.
     *
     * @return částka POZE dle jističe (v Kč bez DPH)
     */
    public double getPoze1() {
        return poze1;
    }


    /**
     * Vrací částku POZE vypočtenou podle spotřeby.
     *
     * @return částka POZE dle spotřeby (v Kč bez DPH)
     */
    public double getPoze2() {
        return poze2;
    }


    /**
     * Přičte dílčí částku k variantě POZE podle hodnoty hlavního jističe.
     *
     * @param p přičítaná částka POZE dle jističe (v Kč bez DPH)
     */
    public void addPoze1(double p) {
        poze1 += p;
    }


    /**
     * Přičte dílčí částku k variantě POZE podle spotřeby.
     *
     * @param p přičítaná částka POZE dle spotřeby (v Kč bez DPH)
     */
    public void addPoze2(double p) {
        poze2 += p;
    }


    /**
     * Určí výhodnější typ POZE porovnáním obou uložených částek.
     * Při rovnosti hodnot ({@code poze1 == poze2}) vrací {@link TypePoze#POZE2}.
     *
     * @return {@link TypePoze#POZE1}, pokud je {@code poze1 < poze2}, jinak {@link TypePoze#POZE2}
     */
    public TypePoze getTypePoze() {
        if (poze1 < poze2)
            return TypePoze.POZE1;
        else
            return TypePoze.POZE2;
    }


    /**
     * Vrací textovou reprezentaci instance s hodnotami {@code poze1} a {@code poze2}.
     *
     * @return řetězcová reprezentace objektu {@link PozeModel}
     */
    @NonNull
    @Override
    public String toString() {
        return "PozeModel{" +
                "poze1=" + poze1 +
                ", poze2=" + poze2 +
                '}';
    }


    /**
     * Varianta výpočtu poplatku za podporované zdroje energie (POZE).
     */
    public enum TypePoze {
        /**
         * POZE vypočtené podle hodnoty hlavního jističe.
         */
        POZE1,
        /** POZE vypočtené podle spotřeby (max. 495 Kč/MWh). */
        POZE2
    }

}
