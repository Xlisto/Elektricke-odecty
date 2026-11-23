package cz.xlisto.elektrodroid.modules.dashboard;


import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;


/**
 * Pomocná třída pro výpočet odhadu (předpokládané spotřeby) neúplného aktuálního měsíce
 * a neúplného aktuálního roku a pro výpočet procentní změny mezi sousedními sloupci grafu.
 * <p>
 * Odhad zohledňuje:
 * <ul>
 *     <li>historickou spotřebu ve stejném kalendářním měsíci z předchozích let (pokud je dostupná)
 *     včetně meziročního trendu aktuálního odběrného místa,</li>
 *     <li>sezónnost (zimní topná sezóna vs. letní měsíce) s automatickou detekcí, zda odběrné místo
 *     vytápí elektřinou (podle poměru zimní/letní historie, případně podle distribuční sazby).</li>
 * </ul>
 */
public final class ConsumptionForecastHelper {

    /**
     * Sezónní váhy pro odběrné místo s elektrickým vytápěním / tepelným čerpadlem (leden..prosinec).
     * Zohledňují typický profil topné sezóny v ČR (říjen–duben).
     */
    private static final double[] HEATING_SEASON_WEIGHTS = {
            1.65, // I.  Leden
            1.45, // II. Únor
            1.20, // III. Březen
            0.90, // IV. Duben
            0.65, // V.  Květen
            0.55, // VI. Červen
            0.55, // VII. Červenec
            0.58, // VIII. Srpen
            0.72, // IX. Září
            1.00, // X.  Říjen
            1.30, // XI. Listopad
            1.55  // XII. Prosinec
    };

    /**
     * Sezónní váhy pro běžnou domácnost bez elektrického vytápění (leden..prosinec).
     * Mírně vyšší spotřeba v zimě (svícení, kratší dny), nižší v létě.
     */
    private static final double[] STANDARD_SEASON_WEIGHTS = {
            1.15, // I.  Leden
            1.10, // II. Únor
            1.03, // III. Březen
            0.96, // IV. Duben
            0.90, // V.  Květen
            0.87, // VI. Červen
            0.86, // VII. Červenec
            0.88, // VIII. Srpen
            0.95, // IX. Září
            1.02, // X.  Říjen
            1.11, // XI. Listopad
            1.17  // XII. Prosinec
    };

    private static final double MIN_TREND_RATIO = 0.65;
    private static final double MAX_TREND_RATIO = 1.50;
    private static final double WINTER_SUMMER_RATIO_STANDARD = 1.25;
    private static final double WINTER_SUMMER_RATIO_HEATING = 2.20;


    private ConsumptionForecastHelper() {
    }


    /**
     * Jednoduchý vstupní záznam měsíční spotřeby oddělený od Android/DB vrstev.
     *
     * @param month             1..12
     * @param coveredDayOfMonth 1..daysInMonth
     * @param spanDays          počet dní pokrytých spotřebou (>= 1)
     */
    public record MonthlyRecord(int year, int month, double vt, double nt, int coveredDayOfMonth,
                                int spanDays, @NonNull String shortLabel) {

        public MonthlyRecord(int year,
                             int month,
                             double vt,
                             double nt,
                             int coveredDayOfMonth,
                             int spanDays,
                             @NonNull String shortLabel) {
            this.year = year;
            this.month = month;
            this.vt = Math.max(0.0, vt);
            this.nt = Math.max(0.0, nt);
            int maxDays = getDaysInMonth(year, month);
            this.coveredDayOfMonth = Math.max(1, Math.min(maxDays, coveredDayOfMonth));
            this.spanDays = Math.max(1, spanDays);
            this.shortLabel = shortLabel;
        }


        public double getTotal() {
            return vt + nt;
        }

    }


    /**
     * Jednoduchý vstupní záznam roční spotřeby.
     */
    public record YearlyRecord(int year, double vt, double nt, @NonNull String label) {

        public YearlyRecord(int year, double vt, double nt, @NonNull String label) {
            this.year = year;
            this.vt = Math.max(0.0, vt);
            this.nt = Math.max(0.0, nt);
            this.label = label;
        }


        public double getTotal() {
            return vt + nt;
        }

    }


    /**
     * Sestaví položky pro měsíční sloupcový graf včetně předpokládané spotřeby aktuálního neúplného
     * měsíce a procentní změny nad sloupci (první zobrazený sloupec procento nikdy nemá).
     *
     * @param allMonthlyRecords všechny chronologicky seřazené měsíční záznamy odběrného místa
     * @param maxEntries        maximální počet zobrazených sloupců z konce seznamu
     * @param nowYear           aktuální kalendářní rok
     * @param nowMonth          aktuální kalendářní měsíc (1..12)
     * @param distributionRate  distribuční sazba odběrného místa (např. D57d, D02d), může být {@code null}
     * @return seznam položek {@link DashboardConsumptionBarChartView.BarEntry}
     */
    @NonNull
    public static List<DashboardConsumptionBarChartView.BarEntry> buildMonthlyEntries(
            @Nullable List<MonthlyRecord> allMonthlyRecords,
            int maxEntries,
            int nowYear,
            int nowMonth,
            @Nullable String distributionRate) {
        List<DashboardConsumptionBarChartView.BarEntry> result = new ArrayList<>();
        if (allMonthlyRecords == null || allMonthlyRecords.isEmpty() || maxEntries <= 0) {
            return result;
        }

        double[] seasonalWeights = computeEffectiveSeasonalWeights(allMonthlyRecords, nowYear, nowMonth, distributionRate);
        double[] historicalAvg = computeHistoricalMonthAverages(allMonthlyRecords, nowYear);

        int startIdx = Math.max(0, allMonthlyRecords.size() - maxEntries);
        List<Double> effectiveTotals = new ArrayList<>(allMonthlyRecords.size() - startIdx);
        List<Double> projectedTotals = new ArrayList<>(allMonthlyRecords.size() - startIdx);

        for (int i = startIdx; i < allMonthlyRecords.size(); i++) {
            MonthlyRecord record = allMonthlyRecords.get(i);
            double actual = record.getTotal();
            double projected = actual;

            if (isIncompleteCurrentMonth(record, nowYear, nowMonth)) {
                projected = forecastIncompleteMonth(
                        record,
                        allMonthlyRecords,
                        historicalAvg,
                        seasonalWeights,
                        nowYear,
                        nowMonth
                );
            }
            projectedTotals.add(projected);
            effectiveTotals.add(Math.max(actual, projected));
        }

        for (int offset = 0; offset < effectiveTotals.size(); offset++) {
            MonthlyRecord record = allMonthlyRecords.get(startIdx + offset);
            Double changePercent = computeChangePercent(effectiveTotals, offset);
            result.add(new DashboardConsumptionBarChartView.BarEntry(
                    record.shortLabel,
                    record.vt,
                    record.nt,
                    projectedTotals.get(offset),
                    changePercent
            ));
        }
        return result;
    }


    /**
     * Sestaví položky pro roční sloupcový graf včetně předpokládané spotřeby aktuálního neúplného
     * roku a procentní změny nad sloupci (první zobrazený sloupec procento nikdy nemá).
     *
     * @param allYearlyRecords  všechny chronologicky seřazené roční záznamy odběrného místa
     * @param allMonthlyRecords všechny chronologicky seřazené měsíční záznamy (pro přesnou sezónní a historickou predikci)
     * @param maxEntries        maximální počet zobrazených sloupců z konce seznamu
     * @param nowYear           aktuální kalendářní rok
     * @param nowMonth          aktuální kalendářní měsíc (1..12)
     * @param distributionRate  distribuční sazba odběrného místa (např. D57d, D02d), může být {@code null}
     * @return seznam položek {@link DashboardConsumptionBarChartView.BarEntry}
     */
    @NonNull
    public static List<DashboardConsumptionBarChartView.BarEntry> buildYearlyEntries(
            @Nullable List<YearlyRecord> allYearlyRecords,
            @Nullable List<MonthlyRecord> allMonthlyRecords,
            int maxEntries,
            int nowYear,
            int nowMonth,
            @Nullable String distributionRate) {
        List<DashboardConsumptionBarChartView.BarEntry> result = new ArrayList<>();
        if (allYearlyRecords == null || allYearlyRecords.isEmpty() || maxEntries <= 0) {
            return result;
        }

        List<MonthlyRecord> safeMonthly = allMonthlyRecords != null ? allMonthlyRecords : new ArrayList<>();
        double[] seasonalWeights = computeEffectiveSeasonalWeights(safeMonthly, nowYear, nowMonth, distributionRate);
        double[] historicalAvg = computeHistoricalMonthAverages(safeMonthly, nowYear);

        int startIdx = Math.max(0, allYearlyRecords.size() - maxEntries);
        List<Double> effectiveTotals = new ArrayList<>(allYearlyRecords.size() - startIdx);
        List<Double> projectedTotals = new ArrayList<>(allYearlyRecords.size() - startIdx);

        for (int i = startIdx; i < allYearlyRecords.size(); i++) {
            YearlyRecord yRecord = allYearlyRecords.get(i);
            double actual = yRecord.getTotal();
            double projected = actual;

            if (yRecord.year == nowYear) {
                projected = forecastCurrentYear(
                        yRecord,
                        safeMonthly,
                        historicalAvg,
                        seasonalWeights,
                        nowYear,
                        nowMonth
                );
            }
            projectedTotals.add(projected);
            effectiveTotals.add(Math.max(actual, projected));
        }

        for (int offset = 0; offset < effectiveTotals.size(); offset++) {
            YearlyRecord yRecord = allYearlyRecords.get(startIdx + offset);
            Double changePercent = computeChangePercent(effectiveTotals, offset);
            result.add(new DashboardConsumptionBarChartView.BarEntry(
                    yRecord.label,
                    yRecord.vt,
                    yRecord.nt,
                    projectedTotals.get(offset),
                    changePercent
            ));
        }
        return result;
    }


    /**
     * Vypočítá procentní změnu oproti předchozímu zobrazenému sloupci.
     * Pro první sloupec ({@code index == 0}) vždy vrací {@code null}.
     */
    @Nullable
    static Double computeChangePercent(@NonNull List<Double> effectiveTotals, int index) {
        if (index <= 0 || index >= effectiveTotals.size()) {
            return null;
        }
        double previous = effectiveTotals.get(index - 1);
        double current = effectiveTotals.get(index);
        if (previous <= 0.001) {
            return null;
        }
        return ((current - previous) / previous) * 100.0;
    }


    /**
     * Určí, zda daný měsíční záznam odpovídá aktuálnímu ještě nedokončenému kalendářnímu měsíci.
     */
    static boolean isIncompleteCurrentMonth(@NonNull MonthlyRecord record, int nowYear, int nowMonth) {
        if (record.year != nowYear || record.month != nowMonth) {
            return false;
        }
        int daysInMonth = getDaysInMonth(record.year, record.month);
        return record.coveredDayOfMonth < daysInMonth;
    }


    /**
     * Odhadne celkovou spotřebu za neúplný aktuální měsíc kombinací dosavadního tempa
     * a historického/sezónního očekávání podle průběhu měsíce.
     */
    static double forecastIncompleteMonth(@NonNull MonthlyRecord current,
                                          @NonNull List<MonthlyRecord> allMonthly,
                                          @NonNull double[] historicalAvg,
                                          @NonNull double[] seasonalWeights,
                                          int nowYear,
                                          int nowMonth) {
        double actual = current.getTotal();
        int daysInMonth = getDaysInMonth(current.year, current.month);
        int remainingDays = Math.max(0, daysInMonth - current.coveredDayOfMonth);
        if (remainingDays == 0) {
            return actual;
        }

        double actualDailyRate = actual / Math.max(1, current.spanDays);
        double priorFullMonth = estimateFullMonthFromHistoryOrSeason(
                current.month,
                allMonthly,
                historicalAvg,
                seasonalWeights,
                nowYear,
                nowMonth
        );

        double progress = Math.max(0.0, Math.min(1.0, current.coveredDayOfMonth / (double) daysInMonth));
        double expectedRemainingDailyRate;
        if (priorFullMonth > 0.0) {
            double priorDailyRate = priorFullMonth / daysInMonth;
            expectedRemainingDailyRate = progress * actualDailyRate + (1.0 - progress) * priorDailyRate;
        } else {
            expectedRemainingDailyRate = actualDailyRate;
        }

        double projected = actual + Math.max(0.0, expectedRemainingDailyRate) * remainingDays;
        return Math.max(actual, projected);
    }


    /**
     * Odhadne celkovou roční spotřebu pro aktuální rok sečtením:
     * <ol>
     *     <li>dosavadní reálné spotřeby aktuálního roku,</li>
     *     <li>případného zbytku aktuálního rozpracovaného měsíce,</li>
     *     <li>očekávané spotřeby všech zbývajících měsíců do konce prosince (s využitím historie
     *     stejných měsíců a meziročního trendu, případně sezónní křivky).</li>
     * </ol>
     */
    static double forecastCurrentYear(@NonNull YearlyRecord currentYearRecord,
                                      @NonNull List<MonthlyRecord> allMonthly,
                                      @NonNull double[] historicalAvg,
                                      @NonNull double[] seasonalWeights,
                                      int nowYear,
                                      int nowMonth) {
        double actualYearTotal = currentYearRecord.getTotal();

        // Najdeme všechny měsíce zaznamenané v aktuálním roce
        Map<Integer, MonthlyRecord> currentYearMonths = new HashMap<>();
        int lastRecordedMonth = 0;
        for (MonthlyRecord r : allMonthly) {
            if (r.year == nowYear) {
                currentYearMonths.put(r.month, r);
                if (r.month > lastRecordedMonth) {
                    lastRecordedMonth = r.month;
                }
            }
        }

        if (lastRecordedMonth == 0) {
            return actualYearTotal;
        }

        double additionalExpected = 0.0;

        // 1. Pokud je poslední zaznamenaný měsíc aktuálním neúplným měsícem, přičteme zbývající dny tohoto měsíce
        MonthlyRecord lastMonthRecord = currentYearMonths.get(lastRecordedMonth);
        if (lastMonthRecord != null && isIncompleteCurrentMonth(lastMonthRecord, nowYear, nowMonth)) {
            double fullLastMonth = forecastIncompleteMonth(
                    lastMonthRecord,
                    allMonthly,
                    historicalAvg,
                    seasonalWeights,
                    nowYear,
                    nowMonth
            );
            additionalExpected += Math.max(0.0, fullLastMonth - lastMonthRecord.getTotal());
        }

        // Pokud už je prosinec dokončený, není co predikovat
        if (lastRecordedMonth >= 12 && additionalExpected <= 0.0) {
            return actualYearTotal;
        }

        // 2. Spočítáme meziroční trend aktuálního roku vůči historickým průměrům stejných dokončených měsíců
        double yearTrend = computeYearTrendFactor(allMonthly, historicalAvg, nowYear, nowMonth);

        // Základní normalizovaná měsíční spotřeba (pro případ, že pro některý zbývající měsíc chybí historie)
        double baseNormalizedMonthly = computeBaseNormalizedMonthly(allMonthly, seasonalWeights, nowYear, nowMonth);

        // 3. Přičteme predikci pro všechny zbývající měsíce (lastRecordedMonth + 1 .. 12)
        for (int month = lastRecordedMonth + 1; month <= 12; month++) {
            double monthForecast;
            if (historicalAvg[month - 1] > 0.0) {
                monthForecast = historicalAvg[month - 1] * yearTrend;
            } else if (baseNormalizedMonthly > 0.0) {
                monthForecast = baseNormalizedMonthly * seasonalWeights[month - 1];
            } else {
                monthForecast = 0.0;
            }
            additionalExpected += Math.max(0.0, monthForecast);
        }

        return Math.max(actualYearTotal, actualYearTotal + additionalExpected);
    }


    /**
     * Odhadne plnou spotřebu pro daný kalendářní měsíc z historie stejného měsíce (s trendem)
     * nebo z nedávných dokončených měsíců přepočítaných sezónními vahami.
     */
    private static double estimateFullMonthFromHistoryOrSeason(int targetMonth,
                                                               @NonNull List<MonthlyRecord> allMonthly,
                                                               @NonNull double[] historicalAvg,
                                                               @NonNull double[] seasonalWeights,
                                                               int nowYear,
                                                               int nowMonth) {
        if (historicalAvg[targetMonth - 1] > 0.0) {
            double trend = computeRecentTrendFactor(allMonthly, historicalAvg, nowYear, nowMonth);
            return historicalAvg[targetMonth - 1] * trend;
        }

        // Bez historie stejného měsíce použijeme poslední až 3 dokončené měsíce normalizované sezónní vahou
        double sumActual = 0.0;
        double sumWeights = 0.0;
        int count = 0;
        for (int i = allMonthly.size() - 1; i >= 0 && count < 3; i--) {
            MonthlyRecord r = allMonthly.get(i);
            if (isIncompleteCurrentMonth(r, nowYear, nowMonth)) {
                continue;
            }
            if (r.getTotal() > 0.0) {
                sumActual += r.getTotal();
                sumWeights += seasonalWeights[r.month - 1];
                count++;
            }
        }
        if (sumWeights > 0.0) {
            return (sumActual / sumWeights) * seasonalWeights[targetMonth - 1];
        }
        return -1.0;
    }


    /**
     * Vypočítá efektivní sezónní váhy (1..12) podle detekce elektrického vytápění.
     * Pokud jsou v historii k dispozici zimní i letní dokončené měsíce, plynule interpoluje
     * mezi standardní a topnou křivkou podle reálného poměru zima/léto; jinak využije distribuční sazbu.
     */
    @NonNull
    static double[] computeEffectiveSeasonalWeights(@NonNull List<MonthlyRecord> allMonthly,
                                                    int nowYear,
                                                    int nowMonth,
                                                    @Nullable String distributionRate) {
        double heatingBlend = detectHeatingBlend(allMonthly, nowYear, nowMonth, distributionRate);
        double[] weights = new double[12];
        for (int i = 0; i < 12; i++) {
            weights[i] = STANDARD_SEASON_WEIGHTS[i]
                    + (HEATING_SEASON_WEIGHTS[i] - STANDARD_SEASON_WEIGHTS[i]) * heatingBlend;
        }
        return weights;
    }


    /**
     * Určí míru elektrického vytápění v intervalu {@code [0.0, 1.0]}.
     */
    static double detectHeatingBlend(@NonNull List<MonthlyRecord> allMonthly,
                                     int nowYear,
                                     int nowMonth,
                                     @Nullable String distributionRate) {
        double winterSum = 0.0;
        int winterCount = 0;
        double summerSum = 0.0;
        int summerCount = 0;

        for (MonthlyRecord r : allMonthly) {
            if (isIncompleteCurrentMonth(r, nowYear, nowMonth)) {
                continue;
            }
            double total = r.getTotal();
            if (total <= 0.0) {
                continue;
            }
            if (isWinterMonth(r.month)) {
                winterSum += total;
                winterCount++;
            } else if (isSummerMonth(r.month)) {
                summerSum += total;
                summerCount++;
            }
        }

        if (winterCount > 0 && summerCount > 0) {
            double winterAvg = winterSum / winterCount;
            double summerAvg = summerSum / summerCount;
            if (summerAvg > 0.001) {
                double ratio = winterAvg / summerAvg;
                double normalized = (ratio - WINTER_SUMMER_RATIO_STANDARD)
                        / (WINTER_SUMMER_RATIO_HEATING - WINTER_SUMMER_RATIO_STANDARD);
                return Math.max(0.0, Math.min(1.0, normalized));
            }
        }

        return isHeatingDistributionRate(distributionRate) ? 1.0 : 0.0;
    }


    /**
     * Rozpozná české distribuční sazby určené pro elektrické vytápění nebo tepelná čerpadla.
     */
    static boolean isHeatingDistributionRate(@Nullable String distributionRate) {
        if (distributionRate == null || distributionRate.trim().isEmpty()) {
            return false;
        }
        String compact = distributionRate.trim().replace(" ", "").toUpperCase(Locale.ROOT);
        return compact.startsWith("D35")
                || compact.startsWith("D45")
                || compact.startsWith("D55")
                || compact.startsWith("D56")
                || compact.startsWith("D57");
    }


    /**
     * Vrátí {@code true} pro hlavní zimní/topné měsíce (listopad–březen).
     */
    static boolean isWinterMonth(int month) {
        return month == 11 || month == 12 || month == 1 || month == 2 || month == 3;
    }


    /**
     * Vrátí {@code true} pro letní netopné měsíce (květen–září).
     */
    static boolean isSummerMonth(int month) {
        return month >= 5 && month <= 9;
    }


    /**
     * Spočítá vážený historický průměr pro každý kalendářní měsíc (1..12) z předchozích let
     * (tj. {@code year < nowYear}). Novější roky mají vyšší váhu.
     */
    @NonNull
    static double[] computeHistoricalMonthAverages(@NonNull List<MonthlyRecord> allMonthly,
                                                   int nowYear) {
        List<List<MonthlyRecord>> byMonth = new ArrayList<>(12);
        for (int i = 0; i < 12; i++) {
            byMonth.add(new ArrayList<>());
        }
        for (MonthlyRecord r : allMonthly) {
            if (r.year < nowYear && r.month >= 1 && r.month <= 12 && r.getTotal() > 0.0) {
                byMonth.get(r.month - 1).add(r);
            }
        }

        double[] averages = new double[12];
        for (int m = 0; m < 12; m++) {
            List<MonthlyRecord> records = byMonth.get(m);
            if (records.isEmpty()) {
                averages[m] = 0.0;
                continue;
            }
            // Nejnovější roky jsou na konci seznamu -> dáváme jim vyšší váhu
            double weightedSum = 0.0;
            double weightTotal = 0.0;
            int age = 0;
            for (int i = records.size() - 1; i >= 0 && age < 4; i--, age++) {
                double w = Math.pow(0.75, age);
                weightedSum += records.get(i).getTotal() * w;
                weightTotal += w;
            }
            averages[m] = weightTotal > 0.0 ? weightedSum / weightTotal : 0.0;
        }
        return averages;
    }


    /**
     * Spočítá meziroční trend aktuálního roku porovnáním jeho dokončených měsíců
     * s historickými průměry stejných měsíců.
     */
    private static double computeYearTrendFactor(@NonNull List<MonthlyRecord> allMonthly,
                                                 @NonNull double[] historicalAvg,
                                                 int nowYear,
                                                 int nowMonth) {
        double sumCurrent = 0.0;
        double sumHist = 0.0;
        for (MonthlyRecord r : allMonthly) {
            if (r.year != nowYear || isIncompleteCurrentMonth(r, nowYear, nowMonth)) {
                continue;
            }
            double hist = historicalAvg[r.month - 1];
            if (hist > 0.0 && r.getTotal() > 0.0) {
                sumCurrent += r.getTotal();
                sumHist += hist;
            }
        }
        if (sumHist <= 0.001) {
            return 1.0;
        }
        return clampTrend(sumCurrent / sumHist);
    }


    /**
     * Spočítá nedávný trend z posledních až 3 dokončených měsíců oproti jejich historickému průměru.
     */
    private static double computeRecentTrendFactor(@NonNull List<MonthlyRecord> allMonthly,
                                                   @NonNull double[] historicalAvg,
                                                   int nowYear,
                                                   int nowMonth) {
        double sumCurrent = 0.0;
        double sumHist = 0.0;
        int matched = 0;
        for (int i = allMonthly.size() - 1; i >= 0 && matched < 3; i--) {
            MonthlyRecord r = allMonthly.get(i);
            if (isIncompleteCurrentMonth(r, nowYear, nowMonth)) {
                continue;
            }
            double hist = historicalAvg[r.month - 1];
            if (hist > 0.0 && r.getTotal() > 0.0) {
                sumCurrent += r.getTotal();
                sumHist += hist;
                matched++;
            }
        }
        if (sumHist <= 0.001) {
            return 1.0;
        }
        return clampTrend(sumCurrent / sumHist);
    }


    /**
     * Spočítá sezónně normalizovanou měsíční spotřebu (kWh při váze 1.0) z dokončených měsíců
     * aktuálního roku, případně ze všech dokončených měsíců nebo z dosavadního tempa aktuálního měsíce.
     */
    private static double computeBaseNormalizedMonthly(@NonNull List<MonthlyRecord> allMonthly,
                                                       @NonNull double[] seasonalWeights,
                                                       int nowYear,
                                                       int nowMonth) {
        // 1. Preferujeme dokončené měsíce aktuálního roku
        double sumActual = 0.0;
        double sumWeight = 0.0;
        for (MonthlyRecord r : allMonthly) {
            if (r.year == nowYear && !isIncompleteCurrentMonth(r, nowYear, nowMonth) && r.getTotal() > 0.0) {
                sumActual += r.getTotal();
                sumWeight += seasonalWeights[r.month - 1];
            }
        }
        if (sumWeight > 0.0) {
            return sumActual / sumWeight;
        }

        // 2. Jinak všechny dokončené měsíce
        for (MonthlyRecord r : allMonthly) {
            if (!isIncompleteCurrentMonth(r, nowYear, nowMonth) && r.getTotal() > 0.0) {
                sumActual += r.getTotal();
                sumWeight += seasonalWeights[r.month - 1];
            }
        }
        if (sumWeight > 0.0) {
            return sumActual / sumWeight;
        }

        // 3. Fallback: pouze rozpracovaný aktuální měsíc -> přepočet denního tempa na celý měsíc
        for (MonthlyRecord r : allMonthly) {
            if (r.year == nowYear && r.getTotal() > 0.0) {
                int daysInMonth = getDaysInMonth(r.year, r.month);
                double estimatedFullMonth = (r.getTotal() / Math.max(1, r.spanDays)) * daysInMonth;
                return estimatedFullMonth / seasonalWeights[r.month - 1];
            }
        }
        return 0.0;
    }


    private static double clampTrend(double ratio) {
        return Math.max(MIN_TREND_RATIO, Math.min(MAX_TREND_RATIO, ratio));
    }


    /**
     * Vrátí počet dní v daném měsíci (zohledňuje přestupné roky).
     */
    public static int getDaysInMonth(int year, int month) {
        int safeMonth = Math.max(1, Math.min(12, month));
        Calendar cal = new GregorianCalendar(year, safeMonth - 1, 1);
        return cal.getActualMaximum(Calendar.DAY_OF_MONTH);
    }

}
