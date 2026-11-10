package cz.xlisto.elektrodroid.modules.pricelist.models;


import java.io.Serializable;

import cz.xlisto.elektrodroid.models.PriceListModel;


/**
 * Přepravka vyhodnocených cenových výsledků pro jeden ceník v rámci srovnávacího scénáře.
 * Obsahuje celkové náklady, rozpad na obchodní složku (stanovenou dodavatelem) a regulovanou složku (stanovenou ERÚ).
 */
public class PriceListComparisonResult implements Serializable {

    private final PriceListModel priceList;
    private final double vtKwh;
    private final double ntKwh;
    private final double totalKwh;
    private final double months;

    // Obchodní (neregulovaná) složka bez DPH
    private final double merchantVtCost;
    private final double merchantNtCost;
    private final double merchantMonthlyCost;
    private final double merchantTotalCost;

    // Regulovaná složka bez DPH
    private final double distVtCost;
    private final double distNtCost;
    private final double breakerCost;
    private final double systemServicesCost;
    private final double oteCost;
    private final double pozeCost;
    private final double regulatedTotalCost;

    // Daně
    private final double electricityTax;
    private final double dphRate;
    private final double dphAmount;

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
            double vtKwh,
            double ntKwh,
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
            double dphAmount,
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
        this.vtKwh = vtKwh;
        this.ntKwh = ntKwh;
        this.totalKwh = vtKwh + ntKwh;
        this.months = months;
        this.merchantVtCost = merchantVtCost;
        this.merchantNtCost = merchantNtCost;
        this.merchantMonthlyCost = merchantMonthlyCost;
        this.merchantTotalCost = merchantVtCost + merchantNtCost + merchantMonthlyCost;
        this.distVtCost = distVtCost;
        this.distNtCost = distNtCost;
        this.breakerCost = breakerCost;
        this.systemServicesCost = systemServicesCost;
        this.oteCost = oteCost;
        this.pozeCost = pozeCost;
        this.regulatedTotalCost = distVtCost + distNtCost + breakerCost + systemServicesCost + oteCost + pozeCost;
        this.electricityTax = electricityTax;
        this.dphRate = dphRate;
        this.dphAmount = dphAmount;
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


    public double getVtKwh() {
        return vtKwh;
    }


    public double getNtKwh() {
        return ntKwh;
    }


    public double getTotalKwh() {
        return totalKwh;
    }


    public double getMonths() {
        return months;
    }


    public double getMerchantVtCost() {
        return merchantVtCost;
    }


    public double getMerchantNtCost() {
        return merchantNtCost;
    }


    public double getMerchantMonthlyCost() {
        return merchantMonthlyCost;
    }


    public double getMerchantTotalCost() {
        return merchantTotalCost;
    }


    public double getDistVtCost() {
        return distVtCost;
    }


    public double getDistNtCost() {
        return distNtCost;
    }


    public double getBreakerCost() {
        return breakerCost;
    }


    public double getSystemServicesCost() {
        return systemServicesCost;
    }


    public double getOteCost() {
        return oteCost;
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


    public double getDphRate() {
        return dphRate;
    }


    public double getDphAmount() {
        return dphAmount;
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
        return priceList.getCenaVT() * (1.0 + dphRate / 100.0);
    }


    public double getMerchantNtWithDph() {
        return priceList.getCenaNT() * (1.0 + dphRate / 100.0);
    }


    public double getMerchantMonthlyWithDph() {
        return priceList.getMesicniPlat() * (1.0 + dphRate / 100.0);
    }


    public double getWeightedAverageMerchantPricePerMwhWithDph() {
        double totalMwh = totalKwh / 1000.0;
        if (totalMwh <= 0) return 0.0;
        return ((merchantVtCost + merchantNtCost) * (1.0 + dphRate / 100.0)) / totalMwh;
    }


    public double getDistVtWithDphOnly() {
        return priceList.getDistVT() * (1.0 + dphRate / 100.0);
    }


    public double getDistNtWithDphOnly() {
        return priceList.getDistNT() * (1.0 + dphRate / 100.0);
    }


    public double getBreakerMonthlyWithDphOnly() {
        double monthly = months > 0 ? (breakerCost / months) : 0.0;
        return monthly * (1.0 + dphRate / 100.0);
    }


    public double getRegulatedUnitAveragePerMwhWithDph() {
        double totalMwh = totalKwh / 1000.0;
        if (totalMwh <= 0) return 0.0;
        return (regulatedTotalCost * (1.0 + dphRate / 100.0)) / totalMwh;
    }

}
