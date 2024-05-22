package cz.xlisto.elektrodroid.models;


import androidx.annotation.NonNull;

import java.util.ArrayList;

import cz.xlisto.elektrodroid.ownview.ViewHelper;


/**
 * Xlisto 21.08.2023 3:41
 */
public class MonthlyConsuptionModel implements ConsuptionModel {

    private static final String TAG = "MonthlyConsuptionModel";

    private final ArrayList<Double> VT = new ArrayList<>();
    private final ArrayList<Double> NT = new ArrayList<>();
    private final ArrayList<Long> dates = new ArrayList<>();
    private final ArrayList<Integer> firstReader = new ArrayList<>();
    private Double consuptionVT;
    private Double consuptionNT;
    private int anim;
    private int max;


    public MonthlyConsuptionModel(Double actualVT, Double actualNT, Double previousVT, Double previousNT, Long actualDate, int firstReading) {
        this.VT.add(actualVT);
        this.NT.add(actualNT);
        this.consuptionVT = actualVT - previousVT;
        this.consuptionNT = actualNT - previousNT;
        this.dates.add(actualDate);
        this.firstReader.add(firstReading);
    }


    /**
     * Vrátí datum jako string
     *
     * @return Čísla měsíců měsíční spotřeby
     */
    public String getDateMonth() {
        return ViewHelper.getSimpleDateFormatMonth().format(getDate(this.dates.size() - 1));
    }


    /**
     * Číslo měsíců v římské podobě
     *
     * @return Čísla měsíců měsíční spotřeby jako římské číslo
     */
    public String getDateMonthAsStringShort() {
        switch (getDateMonth()) {
            case "01":
                return "I.";
            case "02":
                return "II.";
            case "03":
                return "III.";
            case "04":
                return "IV.";
            case "05":
                return "V.";
            case "06":
                return "VI.";
            case "07":
                return "VII.";
            case "08":
                return "VIII.";
            case "09":
                return "IX.";
            case "10":
                return "X.";
            case "11":
                return "XI.";
            case "12":
                return "XII.";

            default:
                return "";

        }
    }


    /**
     * Číslo měsíců v textové podobě
     *
     * @return Čísla měsíců měsíční spotřeby jako text
     */
    public String getDateMonthAsStringLong() {
        switch (getDateMonth()) {
            case "01":
                return "Leden";
            case "02":
                return "Únor";
            case "03":
                return "Březen";
            case "04":
                return "Duben";
            case "05":
                return "Květen";
            case "06":
                return "Červen";
            case "07":
                return "Červenec";
            case "08":
                return "Srpen";
            case "09":
                return "Září";
            case "10":
                return "Říjen";
            case "11":
                return "Listopad";
            case "12":
                return "Prosinec";

            default:
                return "";

        }
    }


    /**
     * @return Čísla roku měsíční spotřeby
     */
    public String getYearAsString() {
        return ViewHelper.getSimpleDateFormatYear().format(getDate(this.dates.size() - 1));
    }


    /**
     * Vrátí datum jako string
     *
     * @return Čísla roku měsíční spotřeby
     */
    public int getYearAsInt() {
        return Integer.parseInt(getYearAsString());
    }


    /**
     * Vrátí spotřebu za každý měsíc
     *
     * @return Množství spotřeby ve VT za každý měsíc
     */
    public Double getConsuptionVT() {
        return consuptionVT;
    }


    /**
     * Vrátí spotřebu za každý měsíc
     *
     * @return Množství spotřeby NT za každý měsíc
     */
    public Double getConsuptionNT() {
        return consuptionNT;
    }


    /**
     * Animovaná spotřeba
     *
     * @return spotřeba VT
     */
    public Integer getConsuptionVTAnimace() {
        //Log.w("E","E1 "+anim);
        return (int) (getConsuptionVT() / max * anim);
    }


    /**
     * Animovaná spotřeba
     *
     * @return spotřeba NT
     */
    public Integer getConsuptionNTAnimace() {
        return (int) (getConsuptionNT() / max * anim);
    }


    /**
     * Nastaví animaci
     *
     * @param anim animace
     * @param max  max
     */
    public void setAnimate(int anim, int max) {
        this.anim = anim;
        this.max = max;
    }


    /**
     * Přidá další odečet VT do aktuálního měsíce
     *
     * @param VT odečet VT
     */
    public void setVT(Double VT) {
        this.VT.add(VT);
    }


    /**
     * Přidá další odečet NT do aktuálního měsíce
     *
     * @param NT odečet NT
     */
    public void setNT(Double NT) {
        this.NT.add(NT);
    }


    /**
     * Přidá další datum odečtu do aktuálního měsíce
     *
     * @param dates datum odečtu
     */
    public void setDates(Long dates) {
        this.dates.add(dates);
    }


    /**
     * Nastaví jestli se jedná o první odečet či nikoliv. U prvních odečtů se nepočítá spotřeba
     *
     * @param i 0 = není první odečet; 1 = je první odečet
     */
    public void setFirstReader(int i) {
        this.firstReader.add(i);
    }


    /**
     * Vrátí spotřebu VT
     *
     * @return spotřeba VT
     */
    public Double getVT() {
        Double meter = 0.0;
        for (int i = 0; i < VT.size(); i++) {
            if (meter < getVT(i)) meter = getVT(i);
        }
        return meter;
    }


    /**
     * Vrátí spotřebu NT
     *
     * @return spotřeba NT
     */
    public Double getNT() {
        Double meter = 0.0;
        for (int i = 0; i < NT.size(); i++) {
            if (meter < getNT(i)) meter = getNT(i);
        }
        return meter;
    }


    /**
     * Vrátí datum
     *
     * @param i index
     * @return datum na indexu i
     */
    public Long getDate(int i) {
        return dates.get(i);
    }


    /**
     * Vrátí spotřebu VT
     *
     * @param i index
     * @return vrátí spotřebu na indexu i
     */
    private Double getVT(int i) {
        return VT.get(i);
    }


    /**
     * Vrátí spotřebu NT
     *
     * @param i index
     * @return vrátí spotřebu na indexu i
     */
    private Double getNT(int i) {
        return NT.get(i);
    }


    /**
     * Přidá spořebu VT
     *
     * @param spotrebaVT spořeba jež se připočítá ke stávající spotřebě
     */
    public void addConsuptionVT(Double spotrebaVT) {
        this.consuptionVT = this.consuptionVT + spotrebaVT;
    }


    /**
     * Přidá spořebu NT
     *
     * @param spotrebaNT spořeba jež se připočítá ke stávající spotřebě
     */
    public void addConsuptionNT(Double spotrebaNT) {
        this.consuptionNT = this.consuptionNT + spotrebaNT;
    }


    @NonNull
    @Override
    public String toString() {
        return "Spotřeba: " + getDateMonth() + "/" + getYearAsInt() + " VT:" + getConsuptionVT() + " NT:" + getConsuptionNT();
    }

}
