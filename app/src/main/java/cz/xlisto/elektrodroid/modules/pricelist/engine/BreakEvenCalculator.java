package cz.xlisto.elektrodroid.modules.pricelist.engine;


import java.util.ArrayList;
import java.util.List;

import cz.xlisto.elektrodroid.models.PriceListModel;
import cz.xlisto.elektrodroid.modules.pricelist.models.PriceListComparisonResult;


/**
 * Kalkulátor a analytický modul pro výpočet bodu zvratu (break-even point) a porovnání výhodnosti distribučních sazeb.
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
            this.status = status;
            this.breakEvenMwh = breakEvenMwh;
            this.breakEvenKwh = breakEvenMwh * 1000.0;
            this.breakEvenNtPercent = breakEvenNtPercent;
            this.textSummary = textSummary;
            this.technicalConditionNoticeA = technicalConditionNoticeA;
            this.technicalConditionNoticeB = technicalConditionNoticeB;
        }

    }


    /**
     * Vypočítá bod zvratu spotřeby (v MWh) pro dva ceníky při zadaném poměru nízkého tarifu a jističi.
     *
     * @param priceListA   první ceník
     * @param priceListB   druhý ceník
     * @param ntRatio      podíl spotřeby v NT (0.0 až 1.0)
     * @param months       počet měsíců
     * @param countPhaze   počet fází
     * @param powerBreaker jistič v A
     * @return {@link BreakEvenResult} obsahující výsledek a bod zvratu
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

        // Spočíteme fixní roční náklady (0 spotřeba)
        PriceListComparisonResult resA0 = PriceListCalculationEngine.calculate(priceListA, 0, 0, months, countPhaze, powerBreaker);
        PriceListComparisonResult resB0 = PriceListCalculationEngine.calculate(priceListB, 0, 0, months, countPhaze, powerBreaker);

        double fA = resA0.getTotalWithDph();
        double fB = resB0.getTotalWithDph();

        // Spočíteme náklady při spotřebě 1 MWh
        double vt1 = (1.0 - ntRatio) * 1000.0;
        double nt1 = ntRatio * 1000.0;
        PriceListComparisonResult resA1 = PriceListCalculationEngine.calculate(priceListA, vt1, nt1, months, countPhaze, powerBreaker);
        PriceListComparisonResult resB1 = PriceListCalculationEngine.calculate(priceListB, vt1, nt1, months, countPhaze, powerBreaker);

        double pA = resA1.getTotalWithDph() - fA; // Jednotková cena za 1 MWh s DPH
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

        if (eBreakEvenMwh < 0) {
            // Průsečík je v záporné spotřebě
            if (pA < pB && fA < fB) {
                return new BreakEvenResult(
                        ResultStatus.NO_INTERSECTION_A_CHEAPER,
                        -1,
                        -1,
                        "Ceník " + priceListA.getProdukt() + " (" + priceListA.getSazba() + ") je výhodnější v celém rozsahu kladné spotřeby.",
                        noticeA,
                        noticeB
                );
            } else {
                return new BreakEvenResult(
                        ResultStatus.NO_INTERSECTION_B_CHEAPER,
                        -1,
                        -1,
                        "Ceník " + priceListB.getProdukt() + " (" + priceListB.getSazba() + ") je výhodnější v celém rozsahu kladné spotřeby.",
                        noticeA,
                        noticeB
                );
            }
        }

        String nameA = priceListA.getNameWithPeriod();
        String nameB = priceListB.getNameWithPeriod();

        boolean sameProductSeries = priceListA.getProdukt().equalsIgnoreCase(priceListB.getProdukt())
                || (priceListA.getRada() != null && priceListA.getRada().equalsIgnoreCase(priceListB.getRada()));

        String summary;
        if (fA < fB) {
            if (sameProductSeries) {
                summary = String.format(java.util.Locale.getDefault(),
                        "Do spotřeby %.2f MWh/rok (%.0f kWh) je výhodnější ceník %s ze staršího/jiného období (%s). Od %.2f MWh/rok je výhodnější ceník stejné produktové řady z období (%s).",
                        eBreakEvenMwh, eBreakEvenMwh * 1000.0, nameA, priceListA.getPlatnostText(), eBreakEvenMwh, priceListB.getPlatnostText());
            } else {
                summary = String.format(java.util.Locale.getDefault(),
                        "Do spotřeby %.2f MWh/rok (%.0f kWh) je výhodnější %s. Od %.2f MWh/rok je výhodnější %s.",
                        eBreakEvenMwh, eBreakEvenMwh * 1000.0, nameA, eBreakEvenMwh, nameB);
            }
        } else {
            if (sameProductSeries) {
                summary = String.format(java.util.Locale.getDefault(),
                        "Do spotřeby %.2f MWh/rok (%.0f kWh) je výhodnější ceník %s ze staršího/jiného období (%s). Od %.2f MWh/rok je výhodnější ceník stejné produktové řady z období (%s).",
                        eBreakEvenMwh, eBreakEvenMwh * 1000.0, nameB, priceListB.getPlatnostText(), eBreakEvenMwh, priceListA.getPlatnostText());
            } else {
                summary = String.format(java.util.Locale.getDefault(),
                        "Do spotřeby %.2f MWh/rok (%.0f kWh) je výhodnější %s. Od %.2f MWh/rok je výhodnější %s.",
                        eBreakEvenMwh, eBreakEvenMwh * 1000.0, nameB, eBreakEvenMwh, nameA);
            }
        }

        return new BreakEvenResult(
                ResultStatus.EXACT_INTERSECTION,
                eBreakEvenMwh,
                -1,
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
