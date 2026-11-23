package cz.xlisto.elektrodroid.modules.dashboard;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


/**
 * Jednotkové testy pro {@link ConsumptionForecastHelper} – procentní změny a odhad
 * neúplného aktuálního měsíce i roku se zohledněním sezónnosti (topení) a historie.
 */
public class ConsumptionForecastHelperTest {

    @Test
    public void buildMonthlyEntries_firstBarHasNoPercent_subsequentBarsShowPercentChange() {
        List<ConsumptionForecastHelper.MonthlyRecord> records = Arrays.asList(
                new ConsumptionForecastHelper.MonthlyRecord(2026, 4, 200, 200, 30, 30, "IV."),
                new ConsumptionForecastHelper.MonthlyRecord(2026, 5, 150, 150, 31, 31, "V."),
                new ConsumptionForecastHelper.MonthlyRecord(2026, 6, 180, 180, 30, 30, "VI.")
        );

        List<DashboardConsumptionBarChartView.BarEntry> entries =
                ConsumptionForecastHelper.buildMonthlyEntries(records, 5, 2026, 8, "D02d");

        assertEquals(3, entries.size());
        // Na prvním sloupci procentní hodnota nikdy není
        assertNull(entries.get(0).changePercent);
        assertFalse(entries.get(0).hasForecast());

        // 400 -> 300 = -25 %
        Double pct1 = entries.get(1).changePercent;
        assertNotNull(pct1);
        assertEquals(-25.0, pct1, 0.01);

        // 300 -> 360 = +20 %
        Double pct2 = entries.get(2).changePercent;
        assertNotNull(pct2);
        assertEquals(20.0, pct2, 0.01);
        assertFalse(entries.get(2).hasForecast());
    }


    @Test
    public void buildMonthlyEntries_incompleteCurrentMonthUsesHistoricalSameMonth() {
        List<ConsumptionForecastHelper.MonthlyRecord> records = Arrays.asList(
                // Historie z minulého roku (listopad = topný měsíc 900 kWh, říjen = 600 kWh)
                new ConsumptionForecastHelper.MonthlyRecord(2025, 10, 200, 400, 31, 31, "X."),
                new ConsumptionForecastHelper.MonthlyRecord(2025, 11, 300, 600, 30, 30, "XI."),
                // Aktuální rok: říjen dokončený 600 kWh (stejný trend 1.0), listopad zatím jen 10 dní = 300 kWh
                new ConsumptionForecastHelper.MonthlyRecord(2026, 10, 200, 400, 31, 31, "X."),
                new ConsumptionForecastHelper.MonthlyRecord(2026, 11, 100, 200, 10, 10, "XI.")
        );

        List<DashboardConsumptionBarChartView.BarEntry> entries =
                ConsumptionForecastHelper.buildMonthlyEntries(records, 5, 2026, 11, "D57d");

        DashboardConsumptionBarChartView.BarEntry currentNov = entries.get(entries.size() - 1);
        assertTrue(currentNov.hasForecast());
        assertEquals(300.0, currentNov.getTotal(), 0.01);
        // 10 dní = 300 kWh a historie listopadu = 900 kWh -> odhad celého listopadu je 900 kWh
        assertEquals(900.0, currentNov.projectedTotal, 1.0);
        // Oproti říjnu (600 kWh) je odhad 900 kWh (+50 %)
        assertNotNull(currentNov.changePercent);
        assertEquals(50.0, currentNov.changePercent, 0.5);
    }


    @Test
    public void buildMonthlyEntries_heatingTariffPredictsHigherWinterThanStandardTariff() {
        // Bez loňské historie: říjen 500 kWh, v listopadu zatím jen 5 dní (75 kWh)
        List<ConsumptionForecastHelper.MonthlyRecord> records = Arrays.asList(
                new ConsumptionForecastHelper.MonthlyRecord(2026, 10, 250, 250, 31, 31, "X."),
                new ConsumptionForecastHelper.MonthlyRecord(2026, 11, 35, 40, 5, 5, "XI.")
        );

        List<DashboardConsumptionBarChartView.BarEntry> heatingEntries =
                ConsumptionForecastHelper.buildMonthlyEntries(records, 5, 2026, 11, "D57d");
        List<DashboardConsumptionBarChartView.BarEntry> standardEntries =
                ConsumptionForecastHelper.buildMonthlyEntries(records, 5, 2026, 11, "D02d");

        double heatingProjected = heatingEntries.get(1).projectedTotal;
        double standardProjected = standardEntries.get(1).projectedTotal;

        assertTrue(heatingEntries.get(1).hasForecast());
        assertTrue(standardEntries.get(1).hasForecast());
        // S topnou sazbou D57d musí být listopadový odhad výrazně vyšší než s běžnou sazbou D02d
        assertTrue(heatingProjected > standardProjected + 50.0);
    }


    @Test
    public void buildYearlyEntries_incompleteCurrentYearUsesHistoricalRemainingMonths() {
        List<ConsumptionForecastHelper.MonthlyRecord> monthly = new ArrayList<>();
        // Rok 2025: 12 měsíců (léto 300 kWh, zima 900 kWh) -> celkem 7200 kWh
        int[] vals2025 = {900, 800, 600, 500, 300, 300, 300, 300, 400, 600, 1000, 1200};
        double sum2025 = 0;
        for (int m = 1; m <= 12; m++) {
            int days = ConsumptionForecastHelper.getDaysInMonth(2025, m);
            monthly.add(new ConsumptionForecastHelper.MonthlyRecord(2025, m, vals2025[m - 1] * 0.3, vals2025[m - 1] * 0.7, days, days, m + "."));
            sum2025 += vals2025[m - 1];
        }

        // Rok 2026: prvních 8 měsíců přesně stejných jako 2025 (4000 kWh), zbývají 9..12 (3200 kWh v 2025)
        double sum2026SoFar = 0;
        for (int m = 1; m <= 8; m++) {
            int days = ConsumptionForecastHelper.getDaysInMonth(2026, m);
            monthly.add(new ConsumptionForecastHelper.MonthlyRecord(2026, m, vals2025[m - 1] * 0.3, vals2025[m - 1] * 0.7, days, days, m + "."));
            sum2026SoFar += vals2025[m - 1];
        }

        List<ConsumptionForecastHelper.YearlyRecord> yearly = Arrays.asList(
                new ConsumptionForecastHelper.YearlyRecord(2025, sum2025 * 0.3, sum2025 * 0.7, "2025"),
                new ConsumptionForecastHelper.YearlyRecord(2026, sum2026SoFar * 0.3, sum2026SoFar * 0.7, "2026")
        );

        List<DashboardConsumptionBarChartView.BarEntry> entries =
                ConsumptionForecastHelper.buildYearlyEntries(yearly, monthly, 5, 2026, 9, "D57d");

        assertEquals(2, entries.size());
        assertNull(entries.get(0).changePercent);
        assertFalse(entries.get(0).hasForecast());

        DashboardConsumptionBarChartView.BarEntry entry2026 = entries.get(1);
        assertTrue(entry2026.hasForecast());
        assertEquals(sum2026SoFar, entry2026.getTotal(), 0.01);
        // S identickým trendem (1.0) musí predikce roku 2026 přesně odpovídat roku 2025 (7200 kWh)
        assertEquals(sum2025, entry2026.projectedTotal, 1.0);
        assertNotNull(entry2026.changePercent);
        assertEquals(0.0, entry2026.changePercent, 0.1);
    }


    @Test
    public void buildYearlyEntries_summerOnlyDataWithHeatingTariffProjectsHighWinterRemainder() {
        // Bez předchozích let: zaznamenány pouze letní měsíce květen–srpen 2026 po 300 kWh (celkem 1200 kWh)
        List<ConsumptionForecastHelper.MonthlyRecord> monthly = Arrays.asList(
                new ConsumptionForecastHelper.MonthlyRecord(2026, 5, 150, 150, 31, 31, "V."),
                new ConsumptionForecastHelper.MonthlyRecord(2026, 6, 150, 150, 30, 30, "VI."),
                new ConsumptionForecastHelper.MonthlyRecord(2026, 7, 150, 150, 31, 31, "VII."),
                new ConsumptionForecastHelper.MonthlyRecord(2026, 8, 150, 150, 31, 31, "VIII.")
        );
        List<ConsumptionForecastHelper.YearlyRecord> yearly = List.of(
                new ConsumptionForecastHelper.YearlyRecord(2026, 600, 600, "2026")
        );

        List<DashboardConsumptionBarChartView.BarEntry> heatingEntries =
                ConsumptionForecastHelper.buildYearlyEntries(yearly, monthly, 5, 2026, 9, "D57d");
        List<DashboardConsumptionBarChartView.BarEntry> standardEntries =
                ConsumptionForecastHelper.buildYearlyEntries(yearly, monthly, 5, 2026, 9, "D02d");

        double heatingTotal = heatingEntries.get(0).projectedTotal;
        double standardTotal = standardEntries.get(0).projectedTotal;

        // Topná domácnost spotřebuje v září–prosinci znatelně více než netopná
        assertTrue(heatingTotal > standardTotal + 400.0);
    }


    @Test
    public void buildYearlyEntries_completedDecemberHasNoForecast() {
        List<ConsumptionForecastHelper.MonthlyRecord> monthly = List.of(
                new ConsumptionForecastHelper.MonthlyRecord(2026, 12, 300, 300, 31, 31, "XII.")
        );
        List<ConsumptionForecastHelper.YearlyRecord> yearly = List.of(
                new ConsumptionForecastHelper.YearlyRecord(2026, 300, 300, "2026")
        );

        List<DashboardConsumptionBarChartView.BarEntry> entries =
                ConsumptionForecastHelper.buildYearlyEntries(yearly, monthly, 5, 2026, 12, "D02d");

        assertFalse(entries.get(0).hasForecast());
        assertEquals(600.0, entries.get(0).projectedTotal, 0.01);
    }

}
