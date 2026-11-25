package cz.xlisto.elektrodroid.modules.pricelist.models;


import java.io.Serializable;

import cz.xlisto.elektrodroid.models.PriceListModel;


/**
 * Přepravka vyhodnocených cenových výsledků pro jeden ceník v rámci srovnávacího scénáře.
 * Obsahuje celkové náklady, rozpad na obchodní složku (stanovenou dodavatelem) a regulovanou složku (stanovenou ERÚ).
 */
public class PriceListComparisonResult implements Serializable {

    private final PriceListModel priceList;

    private final double merchantTotalCost;

    private final double pozeCost;
    private final double regulatedTotalCost;

    // Daně
    private final double electricityTax;
    private final double dphRate;

    // Součty
    private final double totalWithoutDph;
    private final double totalWithDph;
    private final double averageMonthlyCostWithDph;

    // Kompletní jednotkové ceny s DPH (Obchodní + Regulovaná + Daně)
    private final double fullVtPricePerMwhWithDph;
    private final double fullNtPricePerMwhWithDph;
    private final double fullMonthlyFixedCostWithDph;

    // Samostatné regulované jednotkové ceny s DPH (Distribuce + Daně + Systémové služby + POZE + OTE)
    private final double regulatedVtWithDph;
    private final double regulatedNtWithDph;
    private final double regulatedMonthlyWithDph;
    private final double pozeConsumptionWithDph;
    private final double pozeBreakerWithDph;

    // Popis použitého typu POZE
    private final String pozeTypeDescription;

    // Rozdíly oproti nejlevnějšímu ceníku
    private double diffVsCheapest;
    private double percentDiffVsCheapest;
    private boolean isCheapest;


    public PriceListComparisonResult(
            PriceListModel priceList,
            double months,
            double merchantVtCost,
            double merchantNtCost,
            double merchantMonthlyCost,
            double distVtCost,
            double distNtCost,
            double breakerCost,
            double systemServicesCost,
            double oteCost,
            double pozeCost,
            double electricityTax,
            double dphRate,
            double totalWithoutDph,
            double totalWithDph,
            double fullVtPricePerMwhWithDph,
            double fullNtPricePerMwhWithDph,
            double fullMonthlyFixedCostWithDph,
            double regulatedVtWithDph,
            double regulatedNtWithDph,
            double regulatedMonthlyWithDph,
            double pozeConsumptionWithDph,
            double pozeBreakerWithDph,
            String pozeTypeDescription
    ) {
        this.priceList = priceList;
        // Obchodní (neregulovaná) složka bez DPH
        this.merchantTotalCost = merchantVtCost + merchantNtCost + merchantMonthlyCost;
        // Regulovaná složka bez DPH
        this.pozeCost = pozeCost;
        this.regulatedTotalCost = distVtCost + distNtCost + breakerCost + systemServicesCost + oteCost + pozeCost;
        this.electricityTax = electricityTax;
        this.dphRate = dphRate;
        this.totalWithoutDph = totalWithoutDph;
        this.totalWithDph = totalWithDph;
        this.averageMonthlyCostWithDph = months > 0 ? totalWithDph / months : totalWithDph;
        this.fullVtPricePerMwhWithDph = fullVtPricePerMwhWithDph;
        this.fullNtPricePerMwhWithDph = fullNtPricePerMwhWithDph;
        this.fullMonthlyFixedCostWithDph = fullMonthlyFixedCostWithDph;
        this.regulatedVtWithDph = regulatedVtWithDph;
        this.regulatedNtWithDph = regulatedNtWithDph;
        this.regulatedMonthlyWithDph = regulatedMonthlyWithDph;
        this.pozeConsumptionWithDph = pozeConsumptionWithDph;
        this.pozeBreakerWithDph = pozeBreakerWithDph;
        this.pozeTypeDescription = pozeTypeDescription;
    }


    public PriceListModel getPriceList() {
        return priceList;
    }


    public double getMerchantTotalCost() {
        return merchantTotalCost;
    }


    public double getPozeCost() {
        return pozeCost;
    }


    public double getRegulatedTotalCost() {
        return regulatedTotalCost;
    }


    public double getElectricityTax() {
        return electricityTax;
    }


    public double getTotalWithoutDph() {
        return totalWithoutDph;
    }


    public double getTotalWithDph() {
        return totalWithDph;
    }


    public double getAverageMonthlyCostWithDph() {
        return averageMonthlyCostWithDph;
    }


    public double getFullVtPricePerMwhWithDph() {
        return fullVtPricePerMwhWithDph;
    }


    public double getFullNtPricePerMwhWithDph() {
        return fullNtPricePerMwhWithDph;
    }


    public double getFullMonthlyFixedCostWithDph() {
        return fullMonthlyFixedCostWithDph;
    }


    public double getRegulatedVtWithDph() {
        return regulatedVtWithDph;
    }


    public double getRegulatedNtWithDph() {
        return regulatedNtWithDph;
    }


    public double getRegulatedMonthlyWithDph() {
        return regulatedMonthlyWithDph;
    }


    public double getPozeConsumptionWithDph() {
        return pozeConsumptionWithDph;
    }


    public double getPozeBreakerWithDph() {
        return pozeBreakerWithDph;
    }


    public String getPozeTypeDescription() {
        return pozeTypeDescription != null ? pozeTypeDescription : "";
    }


    public double getDiffVsCheapest() {
        return diffVsCheapest;
    }


    public void setDiffVsCheapest(double diffVsCheapest) {
        this.diffVsCheapest = diffVsCheapest;
    }


    public double getPercentDiffVsCheapest() {
        return percentDiffVsCheapest;
    }


    public void setPercentDiffVsCheapest(double percentDiffVsCheapest) {
        this.percentDiffVsCheapest = percentDiffVsCheapest;
    }


    public boolean isCheapest() {
        return isCheapest;
    }


    public void setCheapest(boolean cheapest) {
        isCheapest = cheapest;
    }


    // Obchodní ceny stanovené dodavatelem s DPH
    public double getMerchantVtWithDph() {
        return priceList.getPriceVT() * (1.0 + dphRate / 100.0);
    }


    public double getMerchantNtWithDph() {
        return priceList.getPriceNT() * (1.0 + dphRate / 100.0);
    }


    public double getMerchantMonthlyWithDph() {
        return priceList.getMonthlyPayment() * (1.0 + dphRate / 100.0);
    }

}
