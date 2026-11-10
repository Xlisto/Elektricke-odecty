package cz.xlisto.elektrodroid.modules.pricelist.engine;


import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.List;

import cz.xlisto.elektrodroid.format.DecimalFormatHelper;
import cz.xlisto.elektrodroid.models.PriceListModel;
import cz.xlisto.elektrodroid.models.PriceListRegulBuilder;
import cz.xlisto.elektrodroid.modules.pricelist.models.PriceListComparisonResult;
import cz.xlisto.elektrodroid.modules.pricelist.models.PriceListComparisonScenario;
import cz.xlisto.elektrodroid.utils.Calculation;


/**
 * Hlavní výpočetní engine pro vyhodnocení celkových nákladů, měsíčních průměrů a rozpadu ceny na obchodní složku, regulovanou složku a daně.
 * Respektuje historická pravidla daného období platnosti ceníku.
 */
public class PriceListCalculationEngine {

    /**
     * Vyhodnotí cenové nároky pro jeden ceník na základě zadaného scénáře.
     *
     * @param priceList ceník ke spočítání
     * @param scenario  scénář spotřeby a parametrů odběrného místa
     * @return strukturovaný výsledek porovnání {@link PriceListComparisonResult}
     */
    public static PriceListComparisonResult calculate(PriceListModel priceList, PriceListComparisonScenario scenario) {
        if (priceList == null) {
            return null;
        }

        double vtKwh = scenario.getVtKwh();
        double ntKwh = scenario.getNtKwh();
        double months = scenario.getMonths();
        int countPhaze = (int) Math.round(scenario.getCountPhaze());
        int powerBreaker = (int) Math.round(scenario.getPowerBreaker());

        return calculate(priceList, vtKwh, ntKwh, months, countPhaze, powerBreaker);
    }


    /**
     * Vyhodnotí cenové nároky pro jeden ceník na základě konkrétních číselných parametrů.
     *
     * @param priceList    ceník ke spočítání
     * @param vtKwh        spotřeba ve vysokém tarifu v kWh
     * @param ntKwh        spotřeba v nízkém tarifu v kWh
     * @param months       počet měsíců
     * @param countPhaze   počet fází (1 nebo 3)
     * @param powerBreaker hodnota jističe v A
     * @return strukturovaný výsledek porovnání {@link PriceListComparisonResult}
     */
    public static PriceListComparisonResult calculate(
            PriceListModel priceList,
            double vtKwh,
            double ntKwh,
            double months,
            int countPhaze,
            int powerBreaker
    ) {
        if (priceList == null) {
            return null;
        }

        int year = priceList.getRokPlatnost();
        if (year <= 0) {
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(priceList.getPlatnostOD());
            year = cal.get(Calendar.YEAR);
        }

        // Aplikujeme regulované úpravy (zastropování 2023, nulování POZE)
        PriceListModel activePriceList = priceList;
        PriceListRegulBuilder regulBuilder = new PriceListRegulBuilder(priceList, year);
        if (regulBuilder.isRegulPrice()) {
            activePriceList = regulBuilder.getRegulPriceList();
        }

        double vtMwh = vtKwh / 1000.0;
        double ntMwh = ntKwh / 1000.0;
        double totalMwh = vtMwh + ntMwh;

        // 1. Obchodní (neregulovaná) složka
        double merchantVtCost = activePriceList.getCenaVT() * vtMwh;
        double merchantNtCost = activePriceList.getCenaNT() * ntMwh;
        double merchantMonthlyCost = activePriceList.getMesicniPlat() * months;

        // 2. Regulovaná složka
        double distVtCost = activePriceList.getDistVT() * vtMwh;
        double distNtCost = activePriceList.getDistNT() * ntMwh;

        double monthlyBreakerPrice = Calculation.calculatePriceBreaker(activePriceList, countPhaze, powerBreaker);
        double breakerCost = monthlyBreakerPrice * months;

        double systemServicesCost = activePriceList.getSystemSluzby() * totalMwh;
        double oteCost = activePriceList.getCinnost() * months;

        // Výpočet POZE
        double pozeCost = calculatePozeCost(activePriceList, year, totalMwh, months, countPhaze, powerBreaker);

        // 3. Daně
        double electricityTax = activePriceList.getDan() * totalMwh;

        double totalWithoutDph = merchantVtCost + merchantNtCost + merchantMonthlyCost
                + distVtCost + distNtCost + breakerCost + systemServicesCost + oteCost + pozeCost + electricityTax;

        double dphRate = activePriceList.getDph();
        double dphAmount = totalWithoutDph * (dphRate / 100.0);
        double totalWithDph = totalWithoutDph + dphAmount;

        // Výpočet jednotkových cen s DPH
        double dphMultiplier = 1.0 + (dphRate / 100.0);

        double pozeByBreaker = (months * (countPhaze * powerBreaker)) * activePriceList.getPoze1();
        double pozeByConsumption = totalMwh * activePriceList.getPoze2();

        double unitPozePerMwh = 0.0;
        double monthlyPozeBreaker = 0.0;

        if (year == 2022 || year == 2023 || (year >= 2026 && activePriceList.getPoze1() == 0)) {
            unitPozePerMwh = (year >= 2026 && activePriceList.getPoze1() > 0) ? activePriceList.getPoze1() : 0.0;
            monthlyPozeBreaker = 0.0;
        } else if (year < PriceListModel.NEW_POZE_YEAR) {
            unitPozePerMwh = activePriceList.getOze();
        } else if (pozeByBreaker > 0 && pozeByConsumption > 0 && pozeByBreaker < pozeByConsumption) {
            monthlyPozeBreaker = (countPhaze * powerBreaker) * activePriceList.getPoze1();
        } else {
            unitPozePerMwh = activePriceList.getPoze2();
        }

        double otePerMwh = (year < PriceListModel.NEW_POZE_YEAR) ? activePriceList.getOte() : 0.0;
        double oteMonthly = (year >= PriceListModel.NEW_POZE_YEAR) ? activePriceList.getCinnost() : 0.0;

        // Obchodní + Regulace + Daně dohromady
        double fullVtPricePerMwhWithDph = (activePriceList.getCenaVT() + activePriceList.getDistVT() + activePriceList.getDan() + activePriceList.getSystemSluzby() + unitPozePerMwh + otePerMwh) * dphMultiplier;
        double fullNtPricePerMwhWithDph = (activePriceList.getCenaNT() + activePriceList.getDistNT() + activePriceList.getDan() + activePriceList.getSystemSluzby() + unitPozePerMwh + otePerMwh) * dphMultiplier;
        double fullMonthlyFixedCostWithDph = (activePriceList.getMesicniPlat() + monthlyBreakerPrice + oteMonthly + monthlyPozeBreaker) * dphMultiplier;

        // Čistě regulované položky stanovované ERÚ + státní daně s DPH (Distribuce + Daň + Systémové služby + POZE + OTE)
        double regulatedVtWithDph = (activePriceList.getDistVT() + activePriceList.getDan() + activePriceList.getSystemSluzby() + unitPozePerMwh + otePerMwh) * dphMultiplier;
        double regulatedNtWithDph = (activePriceList.getDistNT() + activePriceList.getDan() + activePriceList.getSystemSluzby() + unitPozePerMwh + otePerMwh) * dphMultiplier;
        double regulatedMonthlyWithDph = (monthlyBreakerPrice + oteMonthly + monthlyPozeBreaker) * dphMultiplier;

        double pozeConsumptionVal = 0.0;
        double pozeBreakerVal = 0.0;

        if (year == 2022 || year == 2023 || (year >= 2026 && activePriceList.getPoze1() == 0)) {
            pozeConsumptionVal = (year >= 2026 && activePriceList.getPoze1() > 0) ? activePriceList.getPoze1() : 0.0;
            pozeBreakerVal = 0.0;
        } else if (year < PriceListModel.NEW_POZE_YEAR) {
            pozeConsumptionVal = activePriceList.getOze();
            pozeBreakerVal = 0.0;
        } else {
            pozeConsumptionVal = activePriceList.getPoze2();
            pozeBreakerVal = activePriceList.getPoze1();
        }

        double pozeConsumptionWithDph = pozeConsumptionVal * dphMultiplier;
        double pozeBreakerWithDph = pozeBreakerVal * dphMultiplier;

        double pozeTotalWithDph = pozeCost * dphMultiplier;

        String pozeTypeDescription;
        if (year == 2022 || year == 2023 || (year >= 2026 && activePriceList.getPoze1() == 0)) {
            pozeTypeDescription = "POZE: 0 Kč s DPH (rok " + year + ")";
        } else if (year < PriceListModel.NEW_POZE_YEAR) {
            pozeTypeDescription = "POZE: dle spotřeby (" + DecimalFormatHelper.df0.format(pozeTotalWithDph) + " Kč s DPH)";
        } else if (pozeByBreaker > 0 && pozeByConsumption > 0 && pozeByBreaker < pozeByConsumption) {
            pozeTypeDescription = "POZE: dle jističe (" + DecimalFormatHelper.df0.format(pozeTotalWithDph) + " Kč s DPH)";
        } else {
            pozeTypeDescription = "POZE: dle spotřeby (" + DecimalFormatHelper.df0.format(pozeTotalWithDph) + " Kč s DPH)";
        }

        return new PriceListComparisonResult(
                priceList,
                vtKwh,
                ntKwh,
                months,
                merchantVtCost,
                merchantNtCost,
                merchantMonthlyCost,
                distVtCost,
                distNtCost,
                breakerCost,
                systemServicesCost,
                oteCost,
                pozeCost,
                electricityTax,
                dphRate,
                dphAmount,
                totalWithoutDph,
                totalWithDph,
                fullVtPricePerMwhWithDph,
                fullNtPricePerMwhWithDph,
                fullMonthlyFixedCostWithDph,
                regulatedVtWithDph,
                regulatedNtWithDph,
                regulatedMonthlyWithDph,
                pozeConsumptionWithDph,
                pozeBreakerWithDph,
                pozeTypeDescription
        );
    }


    /**
     * Vypočítá poplatek POZE s ohledem na pravidla daného roku.
     */
    private static double calculatePozeCost(
            PriceListModel priceList,
            int year,
            double totalMwh,
            double months,
            int countPhaze,
            int powerBreaker
    ) {
        // Rok 2022 (od října), 2023 a 2026 je POZE nulové (zrušeno / hromady státu)
        if (year == 2022 || year == 2023 || year >= 2026) {
            if (year >= 2026 && priceList.getPoze1() > 0) {
                return priceList.getPoze1() * totalMwh;
            }
            return 0.0;
        }

        if (year < PriceListModel.NEW_POZE_YEAR) {
            return priceList.getOze() * totalMwh;
        }

        double poze1 = priceList.getPoze1();
        double poze2 = priceList.getPoze2();

        if (poze1 <= 0 && poze2 <= 0) {
            return 0.0;
        }
        if (poze1 <= 0) {
            return totalMwh * poze2;
        }
        if (poze2 <= 0) {
            return (months * (countPhaze * powerBreaker)) * poze1;
        }

        double pozeByBreaker = (months * (countPhaze * powerBreaker)) * poze1;
        double pozeByConsumption = totalMwh * poze2;

        return Math.min(pozeByBreaker, pozeByConsumption);
    }


    /**
     * Vyhodnotí seznam ceníků, seřadí je od nejlevnějšího a vypočítá relativní rozdíly.
     *
     * @param priceLists seznam ceníků
     * @param scenario   scénář spotřeby a parametrů
     * @return seřazený seznam výsledků od nejlevnějšího ceníku
     */
    public static List<PriceListComparisonResult> evaluateAll(List<PriceListModel> priceLists, PriceListComparisonScenario scenario) {
        List<PriceListComparisonResult> results = new ArrayList<>();
        if (priceLists == null || priceLists.isEmpty()) {
            return results;
        }

        for (PriceListModel pl : priceLists) {
            PriceListComparisonResult res = calculate(pl, scenario);
            if (res != null) {
                results.add(res);
            }
        }

        if (results.isEmpty()) {
            return results;
        }

        // Seřadíme od nejlevnějšího celkového nákladu s DPH
        results.sort(Comparator.comparingDouble(PriceListComparisonResult::getTotalWithDph));

        PriceListComparisonResult cheapest = results.get(0);
        cheapest.setCheapest(true);
        cheapest.setDiffVsCheapest(0.0);
        cheapest.setPercentDiffVsCheapest(0.0);

        double cheapestTotal = cheapest.getTotalWithDph();

        for (int i = 1; i < results.size(); i++) {
            PriceListComparisonResult item = results.get(i);
            item.setCheapest(false);
            double diff = item.getTotalWithDph() - cheapestTotal;
            item.setDiffVsCheapest(diff);
            if (cheapestTotal > 0) {
                item.setPercentDiffVsCheapest((diff / cheapestTotal) * 100.0);
            } else {
                item.setPercentDiffVsCheapest(0.0);
            }
        }

        return results;
    }

}
