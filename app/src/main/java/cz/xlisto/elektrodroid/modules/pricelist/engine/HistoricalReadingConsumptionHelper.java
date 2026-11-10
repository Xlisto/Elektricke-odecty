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
        public final double ntPercent;
        public final int year;
        public final boolean isExact;
        public final boolean isEstimated;
        public final String qualityNote;
        public final List<Integer> availableYears;


        public HistoricalConsumptionResult(
                double vtKwh,
                double ntKwh,
                int year,
                boolean isExact,
                boolean isEstimated,
                String qualityNote,
                List<Integer> availableYears
        ) {
            this.vtKwh = Math.max(0, vtKwh);
            this.ntKwh = Math.max(0, ntKwh);
            this.totalKwh = this.vtKwh + this.ntKwh;
            this.ntPercent = this.totalKwh > 0 ? (this.ntKwh / this.totalKwh) * 100.0 : 0.0;
            this.year = year;
            this.isExact = isExact;
            this.isEstimated = isEstimated;
            this.qualityNote = qualityNote;
            this.availableYears = availableYears != null ? availableYears : new ArrayList<>();
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
            return new HistoricalConsumptionResult(0, 0, targetYear, false, true, "Není vybráno odběrné místo", new ArrayList<>());
        }

        AnnualOverviewDataBuilder builder = new AnnualOverviewDataBuilder(context);
        ArrayList<AnnualYearData> allData = builder.buildAllAnnualData(subscriptionPoint);
        List<Integer> availableYears = getAvailableYears(context, subscriptionPoint);

        if (allData.isEmpty()) {
            return new HistoricalConsumptionResult(0, 0, targetYear, false, true, "Žádné odečty v databázi", availableYears);
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
            return new HistoricalConsumptionResult(0, 0, targetYear, false, true, "Nedostatek dat pro rok " + targetYear, availableYears);
        }

        int year = matchedData.year();
        double vt = matchedData.consumptionVT();
        double nt = matchedData.consumptionNT();
        boolean isEstimated = matchedData.isEstimated();
        boolean isExact = !isEstimated;

        String qualityNote;
        if (isExact) {
            qualityNote = "Přesná spotřeba podle hraničních odečtů roku " + year;
        } else {
            qualityNote = "Odhadovaná spotřeba podle interpolace odečtů pro rok " + year;
        }

        return new HistoricalConsumptionResult(vt, nt, year, isExact, isEstimated, qualityNote, availableYears);
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
            return new HistoricalConsumptionResult(0, 0, -1, false, true, "Není vybráno odběrné místo", new ArrayList<>());
        }

        AnnualOverviewDataBuilder builder = new AnnualOverviewDataBuilder(context);
        ArrayList<AnnualYearData> allData = builder.buildAllAnnualData(subscriptionPoint);
        List<Integer> availableYears = getAvailableYears(context, subscriptionPoint);

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
            return new HistoricalConsumptionResult(0, 0, -1, false, true, "Žádné dokončené roky s odečty pro průměr", availableYears);
        }

        double avgVt = totalVt / count;
        double avgNt = totalNt / count;
        String qualityNote = String.format(java.util.Locale.getDefault(), "Průměrná roční spotřeba vypočtená z %d dostupných let", count);

        return new HistoricalConsumptionResult(avgVt, avgNt, -1, false, true, qualityNote, availableYears);
    }

}
