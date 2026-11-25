package cz.xlisto.elektrodroid.models;


import androidx.annotation.NonNull;

import java.util.ArrayList;

import cz.xlisto.elektrodroid.ownview.ViewHelper;


/**
 * Xlisto 21.08.2023 3:41
 */
public class MonthlyConsuptionModel implements ConsuptionModel {

    private final ArrayList<Long> dates = new ArrayList<>();
    private long startDate = -1L;
    private Double consuptionVT;
    private Double consuptionNT;
    private int anim;
    private int max;


    public MonthlyConsuptionModel(Double actualVT, Double actualNT, Double previousVT, Double previousNT, Long actualDate) {
        this.consuptionVT = actualVT - previousVT;
        this.consuptionNT = actualNT - previousNT;
        this.dates.add(actualDate);
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
        return switch (getDateMonth()) {
            case "01" -> "I.";
            case "02" -> "II.";
            case "03" -> "III.";
            case "04" -> "IV.";
            case "05" -> "V.";
            case "06" -> "VI.";
            case "07" -> "VII.";
            case "08" -> "VIII.";
            case "09" -> "IX.";
            case "10" -> "X.";
            case "11" -> "XI.";
            case "12" -> "XII.";
            default -> "";
        };
    }


    /**
     * Číslo měsíců v textové podobě
     *
     * @return Čísla měsíců měsíční spotřeby jako text
     */
    public String getDateMonthAsStringLong() {
        return switch (getDateMonth()) {
            case "01" -> "Leden";
            case "02" -> "Únor";
            case "03" -> "Březen";
            case "04" -> "Duben";
            case "05" -> "Květen";
            case "06" -> "Červen";
            case "07" -> "Červenec";
            case "08" -> "Srpen";
            case "09" -> "Září";
            case "10" -> "Říjen";
            case "11" -> "Listopad";
            case "12" -> "Prosinec";
            default -> "";
        };
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
     * Přidá další datum odečtu do aktuálního měsíce
     *
     * @param dates datum odečtu
     */
    public void setDates(Long dates) {
        this.dates.add(dates);
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
     * Nastaví počáteční datum období (datum předchozího odečtu po posunu).
     *
     * @param startDate počáteční datum v milisekundách
     */
    public void setStartDate(long startDate) {
        this.startDate = startDate;
    }


    /**
     * Vrátí počáteční datum období (datum předchozího odečtu po posunu), nebo {@code -1L}, pokud není nastaveno.
     *
     * @return počáteční datum v milisekundách
     */
    public long getStartDate() {
        return startDate;
    }


    /**
     * Vrátí poslední zaznamenané datum odečtu v tomto měsíci, nebo {@code -1L}, pokud neexistuje.
     *
     * @return poslední datum v milisekundách
     */
    public long getLastDate() {
        return dates.isEmpty() ? -1L : dates.get(dates.size() - 1);
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
