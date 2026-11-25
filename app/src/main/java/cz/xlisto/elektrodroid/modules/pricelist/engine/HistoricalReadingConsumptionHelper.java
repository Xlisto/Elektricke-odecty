package cz.xlisto.elektrodroid.modules.pricelist.engine;


import android.content.Context;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;

import cz.xlisto.elektrodroid.models.SubscriptionPointModel;
import cz.xlisto.elektrodroid.modules.subscriptionpoint.AnnualOverviewDataBuilder;
import cz.xlisto.elektrodroid.modules.subscriptionpoint.AnnualYearData;


/**
 * Pomocný modul pro výpočet skutečné a průměrné roční spotřeby z měsíčních odečtů odběrného místa.
 */
public class HistoricalReadingConsumptionHelper {

    public static class HistoricalConsumptionResult {

        public final double vtKwh;
        public final double ntKwh;
        public final double totalKwh;
        public final String qualityNote;


        public HistoricalConsumptionResult(
                double vtKwh,
                double ntKwh,
                String qualityNote
        ) {
            this.vtKwh = Math.max(0, vtKwh);
            this.ntKwh = Math.max(0, ntKwh);
            this.totalKwh = this.vtKwh + this.ntKwh;
            this.qualityNote = qualityNote;
        }

    }


    /**
     * Zjistí seznam dostupných roků s odečty pro zadané odběrné místo.
     *
     * @param context           kontext aplikace
     * @param subscriptionPoint odběrné místo
     * @return seznam celých roků seřazený sestupně
     */
    public static List<Integer> getAvailableYears(Context context, SubscriptionPointModel subscriptionPoint) {
        List<Integer> years = new ArrayList<>();
        if (subscriptionPoint == null || context == null) {
            return years;
        }

        AnnualOverviewDataBuilder builder = new AnnualOverviewDataBuilder(context);
        ArrayList<AnnualYearData> allData = builder.buildAllAnnualData(subscriptionPoint);

        for (AnnualYearData yd : allData) {
            if (yd.totalCost() > 0 || yd.consumptionVT() > 0 || yd.consumptionNT() > 0) {
                years.add(yd.year());
            }
        }

        years.sort(Collections.reverseOrder());
        return years;
    }


    /**
     * Načte a vypočítá skutečnou spotřebu za zadaný rok pro odběrné místo.
     *
     * @param context           kontext aplikace
     * @param subscriptionPoint odběrné místo
     * @param targetYear        požadovaný rok (např. 2024 nebo -1 pro nejnovější rok)
     * @return {@link HistoricalConsumptionResult} s vypočtenými hodnotami
     */
    public static HistoricalConsumptionResult calculateConsumptionForYear(
            Context context,
            SubscriptionPointModel subscriptionPoint,
            int targetYear
    ) {
        if (subscriptionPoint == null || context == null) {
            return new HistoricalConsumptionResult(0, 0, "Není vybráno odběrné místo");
        }

        AnnualOverviewDataBuilder builder = new AnnualOverviewDataBuilder(context);
        ArrayList<AnnualYearData> allData = builder.buildAllAnnualData(subscriptionPoint);

        if (allData.isEmpty()) {
            return new HistoricalConsumptionResult(0, 0, "Žádné odečty v databázi");
        }

        AnnualYearData matchedData = null;
        if (targetYear > 0) {
            for (AnnualYearData yd : allData) {
                if (yd.year() == targetYear) {
                    matchedData = yd;
                    break;
                }
            }
        }

        if (matchedData == null) {
            // Použijeme nejnovější dostupný rok s daty
            for (int i = allData.size() - 1; i >= 0; i--) {
                AnnualYearData yd = allData.get(i);
                if (yd.consumptionVT() > 0 || yd.consumptionNT() > 0) {
                    matchedData = yd;
                    break;
                }
            }
        }

        if (matchedData == null || (matchedData.consumptionVT() <= 0 && matchedData.consumptionNT() <= 0)) {
            return new HistoricalConsumptionResult(0, 0, "Nedostatek dat pro rok " + targetYear);
        }

        double vt = matchedData.consumptionVT();
        double nt = matchedData.consumptionNT();

        String qualityNote = !matchedData.isEstimated()
                ? "Přesná spotřeba podle hraničních odečtů roku " + matchedData.year()
                : "Odhadovaná spotřeba podle interpolace odečtů pro rok " + matchedData.year();

        return new HistoricalConsumptionResult(vt, nt, qualityNote);
    }


    /**
     * Vypočítá průměrnou roční spotřebu ze všech dostupných roků s odečty.
     *
     * @param context           kontext aplikace
     * @param subscriptionPoint odběrné místo
     * @return {@link HistoricalConsumptionResult} s průměrnými hodnotami
     */
    public static HistoricalConsumptionResult calculateAverageAnnualConsumption(
            Context context,
            SubscriptionPointModel subscriptionPoint
    ) {
        if (subscriptionPoint == null || context == null) {
            return new HistoricalConsumptionResult(0, 0, "Není vybráno odběrné místo");
        }

        AnnualOverviewDataBuilder builder = new AnnualOverviewDataBuilder(context);
        ArrayList<AnnualYearData> allData = builder.buildAllAnnualData(subscriptionPoint);

        double totalVt = 0;
        double totalNt = 0;
        int count = 0;

        Calendar now = Calendar.getInstance();
        int currentYear = now.get(Calendar.YEAR);

        for (AnnualYearData yd : allData) {
            // Pro průměr započítáme pouze ukončené roky s platnou spotřebou
            if (yd.year() < currentYear && (yd.consumptionVT() > 0 || yd.consumptionNT() > 0)) {
                totalVt += yd.consumptionVT();
                totalNt += yd.consumptionNT();
                count++;
            }
        }

        if (count == 0) {
            // Pokud nemáme ukončené roky, zkusíme libovolné dostupné roky
            for (AnnualYearData yd : allData) {
                if (yd.consumptionVT() > 0 || yd.consumptionNT() > 0) {
                    totalVt += yd.consumptionVT();
                    totalNt += yd.consumptionNT();
                    count++;
                }
            }
        }

        if (count == 0) {
            return new HistoricalConsumptionResult(0, 0, "Žádné dokončené roky s odečty pro průměr");
        }

        double avgVt = totalVt / count;
        double avgNt = totalNt / count;
        String qualityNote = String.format(java.util.Locale.getDefault(), "Průměrná roční spotřeba vypočtená z %d dostupných let", count);

        return new HistoricalConsumptionResult(avgVt, avgNt, qualityNote);
    }


    /**
     * Vypočítá průměrnou roční spotřebu za zadaný počet posledních dostupných roků s odečty.
     *
     * @param context           kontext aplikace
     * @param subscriptionPoint odběrné místo
     * @param recentYearsCount  počet posledních roků k započítání
     * @return {@link HistoricalConsumptionResult} s průměrnou spotřebou
     */
    public static HistoricalConsumptionResult calculateAverageConsumptionForLastNYears(
            Context context,
            SubscriptionPointModel subscriptionPoint,
            int recentYearsCount
    ) {
        if (subscriptionPoint == null || context == null || recentYearsCount <= 0) {
            return new HistoricalConsumptionResult(0, 0, "Není vybráno odběrné místo");
        }

        AnnualOverviewDataBuilder builder = new AnnualOverviewDataBuilder(context);
        ArrayList<AnnualYearData> allData = builder.buildAllAnnualData(subscriptionPoint);
        List<Integer> availableYears = getAvailableYears(context, subscriptionPoint);

        if (allData.isEmpty() || availableYears.isEmpty()) {
            return new HistoricalConsumptionResult(0, 0, "Žádné odečty v databázi");
        }

        double totalVt = 0;
        double totalNt = 0;
        int count = 0;

        int yearsToInclude = Math.min(recentYearsCount, availableYears.size());
        List<Integer> targetYears = availableYears.subList(0, yearsToInclude);

        for (AnnualYearData yd : allData) {
            if (targetYears.contains(yd.year()) && (yd.consumptionVT() > 0 || yd.consumptionNT() > 0)) {
                totalVt += yd.consumptionVT();
                totalNt += yd.consumptionNT();
                count++;
            }
        }

        if (count == 0) {
            return new HistoricalConsumptionResult(0, 0, "Nedostatek dat pro zadané období");
        }

        double avgVt = totalVt / count;
        double avgNt = totalNt / count;
        String qualityNote = (count == 1)
                ? String.format(java.util.Locale.getDefault(), "Spotřeba z nejnovějšího roku odečtů (%d)", targetYears.get(0))
                : String.format(java.util.Locale.getDefault(), "Průměrná roční spotřeba za poslední %d roky odečtů", count);

        return new HistoricalConsumptionResult(avgVt, avgNt, qualityNote);
    }
}
