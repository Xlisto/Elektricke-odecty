package cz.xlisto.elektrodroid.modules.pricelist.engine;

import java.util.ArrayList;
import java.util.List;

import cz.xlisto.elektrodroid.format.DecimalFormatHelper;
import cz.xlisto.elektrodroid.models.PriceListModel;
import cz.xlisto.elektrodroid.modules.pricelist.models.PriceListComparisonResult;

/**
 * Kalkulátor a analytický modul pro výpočet bodu zvratu (break-even point) a porovnání výhodnosti distribučních sazeb.
 * Podporuje analýzu 2, 3 i více ceníků s vyhodnocením nejvýhodnějších intervalů i nejméně výhodné (nejdražší) varianty.
 */
public class BreakEvenCalculator {

    public enum ResultStatus {
        EXACT_INTERSECTION,
        NO_INTERSECTION_A_CHEAPER,
        NO_INTERSECTION_B_CHEAPER,
        IDENTICAL_COSTS,
        INVALID_DATA
    }

    public static class Point2D {

        public final double x;
        public final double y;

        public Point2D(double x, double y) {
            this.x = x;
            this.y = y;
        }
    }

    public static class BreakEvenResult {

        public final ResultStatus status;
        public final double breakEvenMwh;
        public final double breakEvenKwh;
        public final double breakEvenNtPercent;
        public final List<Double> allBreakEvenMwhPoints;
        public final String textSummary;
        public final String technicalConditionNoticeA;
        public final String technicalConditionNoticeB;


        public BreakEvenResult(
                ResultStatus status,
                double breakEvenMwh,
                double breakEvenNtPercent,
                String textSummary,
                String technicalConditionNoticeA,
                String technicalConditionNoticeB
        ) {
            this(status, breakEvenMwh, breakEvenNtPercent, null, textSummary, technicalConditionNoticeA, technicalConditionNoticeB);
        }

        public BreakEvenResult(
                ResultStatus status,
                double breakEvenMwh,
                double breakEvenNtPercent,
                List<Double> allBreakEvenMwhPoints,
                String textSummary,
                String technicalConditionNoticeA,
                String technicalConditionNoticeB
        ) {
            this.status = status;
            this.breakEvenMwh = breakEvenMwh;
            this.breakEvenKwh = breakEvenMwh > 0 ? breakEvenMwh * 1000.0 : -1;
            this.breakEvenNtPercent = breakEvenNtPercent;
            this.allBreakEvenMwhPoints = allBreakEvenMwhPoints != null ? allBreakEvenMwhPoints : new ArrayList<>();
            this.textSummary = textSummary;
            this.technicalConditionNoticeA = technicalConditionNoticeA;
            this.technicalConditionNoticeB = technicalConditionNoticeB;
        }
    }

    /**
     * Vypočítá analýzu bodu zvratu pro libovolný počet vybraných ceníků (2, 3 nebo více).
     * Určí optimální intervaly pro nejvýhodnější ceník i nejméně výhodnou (nejdražší) variantu.
     *
     * @param priceLists   seznam porovnávaných ceníků
     * @param ntRatio      podíl spotřeby v NT (0.0 až 1.0)
     * @param months       počet měsíců
     * @param countPhaze   počet fází
     * @param powerBreaker jistič v A
     * @param currentMwh   aktuální celková spotřeba scénáře v MWh
     * @return {@link BreakEvenResult} s vyhodnocením nejlepší i nejhorší varianty a seznamem bodů zvratu
     */
    public static BreakEvenResult calculateMultiPriceListBreakEven(
            List<PriceListModel> priceLists,
            double ntRatio,
            double months,
            int countPhaze,
            int powerBreaker,
            double currentMwh
    ) {
        if (priceLists == null || priceLists.isEmpty()) {
            return new BreakEvenResult(ResultStatus.INVALID_DATA, -1, -1, "Nejsou vybrány žádné ceníky.", "", "");
        }
        if (priceLists.size() == 1) {
            String notice = getTechnicalConditionNotice(priceLists.get(0).getSazba());
            return new BreakEvenResult(ResultStatus.NO_INTERSECTION_A_CHEAPER, -1, -1,
                    "Pro analýzu bodu zvratu vyberte alespoň 2 ceníky.", notice, "");
        }
        if (priceLists.size() == 2) {
            return calculateBreakEvenConsumptionWithWorst(priceLists.get(0), priceLists.get(1), ntRatio, months, countPhaze, powerBreaker);
        }

        int numLists = priceLists.size();

        // 1. Vypočítáme roční fixní platby (0 spotřeba) a jednotkové ceny za MWh s DPH pro každý ceník
        double[] f = new double[numLists];
        double[] p = new double[numLists];

        double vt1 = (1.0 - ntRatio) * 1000.0;
        double nt1 = ntRatio * 1000.0;

        for (int i = 0; i < numLists; i++) {
            PriceListModel pl = priceLists.get(i);
            PriceListComparisonResult res0 = PriceListCalculationEngine.calculate(pl, 0, 0, months, countPhaze, powerBreaker);
            PriceListComparisonResult res1 = PriceListCalculationEngine.calculate(pl, vt1, nt1, months, countPhaze, powerBreaker);

            f[i] = res0 != null ? res0.getTotalWithDph() : 0;
            double total1 = res1 != null ? res1.getTotalWithDph() : f[i];
            p[i] = total1 - f[i];
        }

        // 2. Najdeme všechny kladné průsečíky dvojic přímek Ci(x) = fi + pi * x v rozsahu 0.05 až 50.0 MWh
        List<Double> intersections = new ArrayList<>();
        for (int i = 0; i < numLists; i++) {
            for (int j = i + 1; j < numLists; j++) {
                double deltaP = p[i] - p[j];
                double deltaF = f[j] - f[i];
                if (Math.abs(deltaP) > 1e-4) {
                    double xBe = deltaF / deltaP;
                    if (xBe > 0.05 && xBe <= 50.0) {
                        intersections.add(xBe);
                    }
                }
            }
        }

        intersections.sort(Double::compareTo);
        List<Double> uniqueIntersections = new ArrayList<>();
        for (double xBe : intersections) {
            boolean isDup = false;
            for (double existing : uniqueIntersections) {
                if (Math.abs(existing - xBe) < 0.05) {
                    isDup = true;
                    break;
                }
            }
            if (!isDup) {
                uniqueIntersections.add(xBe);
            }
        }

        // 3. Rozdělíme osu spotřeby na testovací intervaly a pro každý určíme NEJVÝHODNĚJŠÍ a NEJDRAŽŠÍ ceník
        List<Double> testPoints = new ArrayList<>();
        if (uniqueIntersections.isEmpty()) {
            testPoints.add(1.0);
        } else {
            testPoints.add(uniqueIntersections.get(0) / 2.0);
            for (int i = 0; i < uniqueIntersections.size() - 1; i++) {
                testPoints.add((uniqueIntersections.get(i) + uniqueIntersections.get(i + 1)) / 2.0);
            }
            testPoints.add(uniqueIntersections.get(uniqueIntersections.size() - 1) + 2.0);
        }

        List<PriceListModel> bestPerPoint = new ArrayList<>();
        List<PriceListModel> worstPerPoint = new ArrayList<>();

        for (double xTest : testPoints) {
            int bestIdx = 0;
            int worstIdx = 0;
            double minCost = Double.MAX_VALUE;
            double maxCost = -Double.MAX_VALUE;

            for (int i = 0; i < numLists; i++) {
                double cost = f[i] + p[i] * xTest;
                if (cost < minCost) {
                    minCost = cost;
                    bestIdx = i;
                }
                if (cost > maxCost) {
                    maxCost = cost;
                    worstIdx = i;
                }
            }
            bestPerPoint.add(priceLists.get(bestIdx));
            worstPerPoint.add(priceLists.get(worstIdx));
        }

        List<Double> actualSwitchPoints = new ArrayList<>();
        List<PriceListModel> switchBestPriceLists = new ArrayList<>();
        switchBestPriceLists.add(bestPerPoint.get(0));

        if (!uniqueIntersections.isEmpty()) {
            for (int i = 0; i < uniqueIntersections.size(); i++) {
                PriceListModel beforeBest = bestPerPoint.get(i);
                PriceListModel afterBest = bestPerPoint.get(i + 1);
                if (!beforeBest.equals(afterBest)) {
                    actualSwitchPoints.add(uniqueIntersections.get(i));
                    switchBestPriceLists.add(afterBest);
                }
            }
        }

        // 4. Sestavíme přehledný textový výsledek (Nejlepší varianta + Nejhorší varianta)
        StringBuilder sb = new StringBuilder();

        if (actualSwitchPoints.isEmpty()) {
            PriceListModel bestOverall = bestPerPoint.get(0);
            sb.append("Při libovolné spotřebě je nejvýhodnější ").append(bestOverall.getNameWithPeriod()).append(".");
        } else {
            java.text.DecimalFormat df2 = DecimalFormatHelper.df2;
            java.text.DecimalFormat df0 = DecimalFormatHelper.df0;

            for (int i = 0; i <= actualSwitchPoints.size(); i++) {
                PriceListModel currentBest = switchBestPriceLists.get(i);
                if (i == 0) {
                    double endBe = actualSwitchPoints.get(0);
                    sb.append("Do spotřeby ").append(df2.format(endBe)).append(" MWh/rok (")
                            .append(df0.format(endBe * 1000.0)).append(" kWh) je nejvýhodnější ")
                            .append(currentBest.getNameWithPeriod()).append(".");
                } else if (i == actualSwitchPoints.size()) {
                    double startBe = actualSwitchPoints.get(i - 1);
                    sb.append("\n\nOd spotřeby ").append(df2.format(startBe)).append(" MWh/rok je nejvýhodnější ")
                            .append(currentBest.getNameWithPeriod()).append(".");
                } else {
                    double startBe = actualSwitchPoints.get(i - 1);
                    double endBe = actualSwitchPoints.get(i);
                    sb.append("\n\nV rozmezí ").append(df2.format(startBe)).append(" – ").append(df2.format(endBe))
                            .append(" MWh/rok je nejvýhodnější ").append(currentBest.getNameWithPeriod()).append(".");
                }
            }
        }

        PriceListModel worstOverall = worstPerPoint.get(0);
        boolean sameWorstAlways = true;
        for (PriceListModel w : worstPerPoint) {
            if (!w.equals(worstOverall)) {
                sameWorstAlways = false;
                break;
            }
        }

        sb.append("\n\n");
        if (sameWorstAlways) {
            sb.append("Nejméně výhodnou (nejdražší) variantou v celém rozsahu spotřeby je ")
                    .append(worstOverall.getNameWithPeriod()).append(".");
        } else {
            int worstCurrIdx = 0;
            double maxCurrCost = -Double.MAX_VALUE;
            for (int i = 0; i < numLists; i++) {
                double cost = f[i] + p[i] * currentMwh;
                if (cost > maxCurrCost) {
                    maxCurrCost = cost;
                    worstCurrIdx = i;
                }
            }
            sb.append("Nejméně výhodnou (nejdražší) variantou při aktuální spotřebě je ")
                    .append(priceLists.get(worstCurrIdx).getNameWithPeriod()).append(".");
        }

        double primaryBe = actualSwitchPoints.isEmpty() ? -1 : actualSwitchPoints.get(0);
        ResultStatus status = actualSwitchPoints.isEmpty() ? ResultStatus.NO_INTERSECTION_A_CHEAPER : ResultStatus.EXACT_INTERSECTION;

        String noticeA = getTechnicalConditionNotice(priceLists.get(0).getSazba());
        String noticeB = getTechnicalConditionNotice(priceLists.get(1).getSazba());

        return new BreakEvenResult(
                status,
                primaryBe,
                -1,
                actualSwitchPoints,
                sb.toString(),
                noticeA,
                noticeB
        );
    }


    private static BreakEvenResult calculateBreakEvenConsumptionWithWorst(
            PriceListModel priceListA,
            PriceListModel priceListB,
            double ntRatio,
            double months,
            int countPhaze,
            int powerBreaker
    ) {
        BreakEvenResult baseRes = calculateBreakEvenConsumption(priceListA, priceListB, ntRatio, months, countPhaze, powerBreaker);
        if (baseRes.status != ResultStatus.EXACT_INTERSECTION) {
            return baseRes;
        }

        String nameA = priceListA.getNameWithPeriod();
        String nameB = priceListB.getNameWithPeriod();
        java.text.DecimalFormat df2 = DecimalFormatHelper.df2;
        double be = baseRes.breakEvenMwh;

        String worstNote;
        if (baseRes.textSummary.contains("je výhodnější " + nameA + ". Od")) {
            worstNote = "\n\nNejméně výhodnou (nejdražší) variantou do spotřeby " + df2.format(be) + " MWh/rok je " + nameB + ", od " + df2.format(be) + " MWh/rok " + nameA + ".";
        } else {
            worstNote = "\n\nNejméně výhodnou (nejdražší) variantou do spotřeby " + df2.format(be) + " MWh/rok je " + nameA + ", od " + df2.format(be) + " MWh/rok " + nameB + ".";
        }

        List<Double> points = new ArrayList<>();
        points.add(be);

        return new BreakEvenResult(
                baseRes.status,
                be,
                baseRes.breakEvenNtPercent,
                points,
                baseRes.textSummary + worstNote,
                baseRes.technicalConditionNoticeA,
                baseRes.technicalConditionNoticeB
        );
    }


    /**
     * Vypočítá bod zvratu spotřeby (v MWh) pro dva ceníky při zadaném poměru nízkého tarifu a jističi.
     */
    public static BreakEvenResult calculateBreakEvenConsumption(
            PriceListModel priceListA,
            PriceListModel priceListB,
            double ntRatio,
            double months,
            int countPhaze,
            int powerBreaker
    ) {
        if (priceListA == null || priceListB == null) {
            return new BreakEvenResult(ResultStatus.INVALID_DATA, -1, -1, "Neplatná data ceníků", "", "");
        }

        String noticeA = getTechnicalConditionNotice(priceListA.getSazba());
        String noticeB = getTechnicalConditionNotice(priceListB.getSazba());

        PriceListComparisonResult resA0 = PriceListCalculationEngine.calculate(priceListA, 0, 0, months, countPhaze, powerBreaker);
        PriceListComparisonResult resB0 = PriceListCalculationEngine.calculate(priceListB, 0, 0, months, countPhaze, powerBreaker);

        double fA = resA0.getTotalWithDph();
        double fB = resB0.getTotalWithDph();

        double vt1 = (1.0 - ntRatio) * 1000.0;
        double nt1 = ntRatio * 1000.0;
        PriceListComparisonResult resA1 = PriceListCalculationEngine.calculate(priceListA, vt1, nt1, months, countPhaze, powerBreaker);
        PriceListComparisonResult resB1 = PriceListCalculationEngine.calculate(priceListB, vt1, nt1, months, countPhaze, powerBreaker);

        double pA = resA1.getTotalWithDph() - fA;
        double pB = resB1.getTotalWithDph() - fB;

        double deltaF = fB - fA;
        double deltaP = pA - pB;

        if (Math.abs(deltaP) < 1e-4) {
            if (Math.abs(deltaF) < 1e-4) {
                return new BreakEvenResult(
                        ResultStatus.IDENTICAL_COSTS,
                        -1,
                        -1,
                        "Oba ceníky mají v celém rozsahu shodné celkové náklady.",
                        noticeA,
                        noticeB
                );
            }
            if (fA < fB) {
                return new BreakEvenResult(
                        ResultStatus.NO_INTERSECTION_A_CHEAPER,
                        -1,
                        -1,
                        "Ceník " + priceListA.getProdukt() + " (" + priceListA.getSazba() + ") je výhodnější v celém rozsahu spotřeby.",
                        noticeA,
                        noticeB
                );
            } else {
                return new BreakEvenResult(
                        ResultStatus.NO_INTERSECTION_B_CHEAPER,
                        -1,
                        -1,
                        "Ceník " + priceListB.getProdukt() + " (" + priceListB.getSazba() + ") je výhodnější v celém rozsahu spotřeby.",
                        noticeA,
                        noticeB
                );
            }
        }

        double eBreakEvenMwh = deltaF / deltaP;

        String nameA = priceListA.getNameWithPeriod();
        String nameB = priceListB.getNameWithPeriod();

        if (eBreakEvenMwh < 0) {
            if (pA < pB && fA < fB) {
                return new BreakEvenResult(
                        ResultStatus.NO_INTERSECTION_A_CHEAPER,
                        -1,
                        -1,
                        "Ceník " + nameA + " je výhodnější v celém rozsahu spotřeby.",
                        noticeA,
                        noticeB
                );
            } else {
                return new BreakEvenResult(
                        ResultStatus.NO_INTERSECTION_B_CHEAPER,
                        -1,
                        -1,
                        "Ceník " + nameB + " je výhodnější v celém rozsahu spotřeby.",
                        noticeA,
                        noticeB
                );
            }
        }

        String summary;
        if (fA < fB) {
            summary = String.format(java.util.Locale.getDefault(),
                    "Do spotřeby %.2f MWh/rok (%.0f kWh) je výhodnější %s. Od %.2f MWh/rok je výhodnější %s.",
                    eBreakEvenMwh, eBreakEvenMwh * 1000.0, nameA, eBreakEvenMwh, nameB);
        } else {
            summary = String.format(java.util.Locale.getDefault(),
                    "Do spotřeby %.2f MWh/rok (%.0f kWh) je výhodnější %s. Od %.2f MWh/rok je výhodnější %s.",
                    eBreakEvenMwh, eBreakEvenMwh * 1000.0, nameB, eBreakEvenMwh, nameA);
        }

        List<Double> points = new ArrayList<>();
        points.add(eBreakEvenMwh);

        return new BreakEvenResult(
                ResultStatus.EXACT_INTERSECTION,
                eBreakEvenMwh,
                -1,
                points,
                summary,
                noticeA,
                noticeB
        );
    }

    /**
     * Vypočítá řadu bodů závislosti celkových ročních nákladů na spotřebě v MWh.
     */
    public static List<Point2D> generateCostVsConsumptionCurve(
            PriceListModel priceList,
            double ntRatio,
            double months,
            int countPhaze,
            int powerBreaker,
            double maxMwh,
            int steps
    ) {
        List<Point2D> points = new ArrayList<>();
        if (priceList == null || steps <= 1) {
            return points;
        }

        double stepSize = maxMwh / steps;
        for (int i = 0; i <= steps; i++) {
            double mwh = i * stepSize;
            double vt = (1.0 - ntRatio) * mwh * 1000.0;
            double nt = ntRatio * mwh * 1000.0;
            PriceListComparisonResult res = PriceListCalculationEngine.calculate(priceList, vt, nt, months, countPhaze, powerBreaker);
            double cost = res != null ? res.getTotalWithDph() : 0.0;
            points.add(new Point2D(mwh, cost));
        }

        return points;
    }

    /**
     * Vypočítá řadu bodů závislosti celkových ročních nákladů na podílu NT (%) při konstantní celkové spotřebě v MWh.
     */
    public static List<Point2D> generateCostVsNtRatioCurve(
            PriceListModel priceList,
            double totalMwh,
            double months,
            int countPhaze,
            int powerBreaker,
            int steps
    ) {
        List<Point2D> points = new ArrayList<>();
        if (priceList == null || steps <= 1) {
            return points;
        }

        for (int i = 0; i <= steps; i++) {
            double ntPercent = (100.0 / steps) * i;
            double ntRatio = ntPercent / 100.0;
            double vt = (1.0 - ntRatio) * totalMwh * 1000.0;
            double nt = ntRatio * totalMwh * 1000.0;
            PriceListComparisonResult res = PriceListCalculationEngine.calculate(priceList, vt, nt, months, countPhaze, powerBreaker);
            double cost = res != null ? res.getTotalWithDph() : 0.0;
            points.add(new Point2D(ntPercent, cost));
        }

        return points;
    }

    /**
     * Vrátí informační text o technických podmínkách přiznání distribuční sazby.
     */
    public static String getTechnicalConditionNotice(String sazba) {
        if (sazba == null) return "";
        String cleanSazba = sazba.trim().toUpperCase();
        switch (cleanSazba) {
            case "D01D":
                return "D01d: Jednotarifní sazba pro odběrná místa s malou spotřebou (svícení, běžné spotřebiče).";
            case "D02D":
                return "D02d: Jednotarifní sazba pro běžné domácnosti se střední spotřebou.";
            case "D25D":
                return "D25d: Dvoutarifní sazba (8 hod NT) určená pro akumulační ohřev teplé užitkové vody (TUV). Vyžaduje blokování spotřebičů.";
            case "D26D":
                return "D26d: Dvoutarifní sazba (8 hod NT) pro akumulační vytápění a ohřev vody s vyšším instalovaným příkonem topných těles.";
            case "D35D":
                return "D35d: Dvoutarifní sazba (16 hod NT) pro vytápění tepelným čerpadlem nebo hybridy.";
            case "D45D":
                return "D45d: Dvoutarifní sazba (20 hod NT) pro přímotopné elektrické vytápění.";
            case "D57D":
                return "D57d: Dvoutarifní sazba (20 hod NT) pro elektrické vytápění (přímotopy, tepelná čerpadla, elektrokotle).";
            case "D61D":
                return "D61d: Víkendová sazba s levným tarifem od pátku 12:00 do neděle 22:00 (vhodná pro chaty a chalupy).";
            default:
                return "";
        }
    }
}
