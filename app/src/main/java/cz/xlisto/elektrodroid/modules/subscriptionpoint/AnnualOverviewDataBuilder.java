package cz.xlisto.elektrodroid.modules.subscriptionpoint;


import android.content.Context;
import android.database.Cursor;
import android.util.Log;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;

import cz.xlisto.elektrodroid.databaze.DataInvoiceSource;
import cz.xlisto.elektrodroid.databaze.DataMonthlyReadingSource;
import cz.xlisto.elektrodroid.databaze.DataPriceListSource;
import cz.xlisto.elektrodroid.databaze.DbHelper;
import cz.xlisto.elektrodroid.models.InvoiceModel;
import cz.xlisto.elektrodroid.models.MonthlyReadingModel;
import cz.xlisto.elektrodroid.models.PozeModel;
import cz.xlisto.elektrodroid.models.PriceListModel;
import cz.xlisto.elektrodroid.models.PriceListRegulBuilder;
import cz.xlisto.elektrodroid.models.SubscriptionPointModel;
import cz.xlisto.elektrodroid.utils.Calculation;
import cz.xlisto.elektrodroid.utils.DifferenceDate;


/**
 * Agregátor a výpočetní logický modul pro přehledový kombinovaný graf roční spotřeby a průměrné ceny.
 */
public class AnnualOverviewDataBuilder {

    private final Context context;


    public record PointEntry(long date, double vt, double nt, long priceListId, boolean isInvoice) {

    }


    public AnnualOverviewDataBuilder(Context context) {
        this.context = context;
    }


    /**
     * Sestaví seznam ročních dat pro zadané odběrné místo.
     * Zobrazí vždy 4 roky (aktuální rok a 3 předcházející, např. 2023, 2024, 2025, 2026 pro rok 2026).
     * Pokud pro některý rok data chybí, vytvoří se prázdný záznam pro zachování vyhrazeného slotu.
     *
     * @param subscriptionPoint Odběrné místo
     * @return Seznam {@link AnnualYearData} pro zobrazení v grafu
     */
    public ArrayList<AnnualYearData> buildAnnualData(SubscriptionPointModel subscriptionPoint) {
        ArrayList<AnnualYearData> resultList = new ArrayList<>();
        if (subscriptionPoint == null) {
            return resultList;
        }

        Calendar now = Calendar.getInstance();
        int currentYear = now.get(Calendar.YEAR);

        int startYear = currentYear - 3;

        // Načteme všechny body (odečty + faktury)
        ArrayList<PointEntry> allPoints = loadAllMeterPoints(subscriptionPoint);
        if (!allPoints.isEmpty()) {
            allPoints.sort(Comparator.comparingLong(p -> p.date));
        }

        for (int yr = startYear; yr <= currentYear; yr++) {
            boolean isCurrent = (yr == currentYear);
            AnnualYearData yearData = null;
            if (!allPoints.isEmpty()) {
                yearData = calculateForYear(subscriptionPoint, allPoints, yr, isCurrent);
            }

            if (yearData == null) {
                // Pokud pro daný rok nejsou data v DB, vytvoříme prázdný datový objekt pro zachování slotu na grafu
                yearData = new AnnualYearData(
                        yr,
                        0, // VT
                        0, // NT
                        0, // varCostVT
                        0, // varCostNT
                        0, // fixedCostTotal
                        0, // totalCostAll
                        0, // avgPriceVT
                        0, // avgPriceNT
                        0, // avgPriceTotal
                        0, // avgMonthlyFixedCost
                        0, // totalMonthsInYear
                        isCurrent,
                        false, // isEstimated
                        isCurrent ? "(leden)" : null,
                        false // isDualTariff
                );
            }
            resultList.add(yearData);
        }

        return resultList;
    }


    /**
     * Načte všechny odečty z databáze a stavy z faktur pro dané odběrné místo.
     */
    private ArrayList<PointEntry> loadAllMeterPoints(SubscriptionPointModel subscriptionPoint) {
        ArrayList<PointEntry> points = new ArrayList<>();

        // 1. Měsíční odečty
        DataMonthlyReadingSource readingSource = new DataMonthlyReadingSource(context);
        readingSource.open();
        try {
            String tableO = subscriptionPoint.getTableO();
            String orderBy = DbHelper.DATUM + " ASC, " + DbHelper.PRVNI_ODECET + " ASC";
            Cursor cursor = readingSource.getDatabase().query(tableO,
                    null,
                    null,
                    null,
                    null,
                    null,
                    orderBy);

            while (cursor.moveToNext()) {
                MonthlyReadingModel mr = readingSource.createMonthlyReading(cursor);
                if (mr != null) {
                    points.add(new PointEntry(mr.getDate(), mr.getVt(), mr.getNt(), mr.getPriceListId(), false));
                }
            }
            cursor.close();
        } catch (Exception e) {
            Log.e("AnnualOverviewDataBuilder", "Error loading monthly readings", e);
        } finally {
            readingSource.close();
        }

        // 2. Faktury (pokud existují)
        DataInvoiceSource invoiceSource = new DataInvoiceSource(context);
        invoiceSource.open();
        try {
            String tableFAK = subscriptionPoint.getTableFAK();
            Cursor cursor = invoiceSource.getDatabase().query(tableFAK, null, null, null, null, null, DbHelper.COLUMN_DATE_FROM + " ASC");
            while (cursor.moveToNext()) {
                InvoiceModel inv = invoiceSource.createInvoice(cursor);
                if (inv != null) {
                    points.add(new PointEntry(inv.getDateFrom(), inv.getVtStart(), inv.getNtStart(), inv.getIdPriceList(), true));
                    points.add(new PointEntry(inv.getDateTo(), inv.getVtEnd(), inv.getNtEnd(), inv.getIdPriceList(), true));
                }
            }
            cursor.close();
        } catch (Exception e) {
            Log.e("AnnualOverviewDataBuilder", "Error loading invoices", e);
        } finally {
            invoiceSource.close();
        }

        return points;
    }


    /**
     * Vypočítá roční spotřebu a váženou průměrnou cenu pro zadaný rok.
     */
    private AnnualYearData calculateForYear(SubscriptionPointModel subscriptionPoint,
                                            ArrayList<PointEntry> points,
                                            int year,
                                            boolean isCurrentYear) {
        Calendar calStart = Calendar.getInstance();
        calStart.set(year, Calendar.JANUARY, 1, 0, 0, 0);
        calStart.set(Calendar.MILLISECOND, 0);
        long startOfYear = calStart.getTimeInMillis();

        Calendar calEnd = Calendar.getInstance();
        if (isCurrentYear) {
            calEnd.setTimeInMillis(System.currentTimeMillis());
        } else {
            calEnd.set(year, Calendar.DECEMBER, 31, 23, 59, 59);
            calEnd.set(Calendar.MILLISECOND, 999);
        }
        long endOfYear = calEnd.getTimeInMillis();

        boolean isEstimated = false;

        // Najdeme odečty v daném roce
        ArrayList<PointEntry> yearPoints = new ArrayList<>();
        for (PointEntry p : points) {
            if (p.date >= startOfYear && p.date <= endOfYear) {
                yearPoints.add(p);
            }
        }

        if (yearPoints.isEmpty()) {
            // Nemáme žádný odečet v daném roce, zkusíme zda existují odečty před a po
            PointEntry prev = findNearestBefore(points, startOfYear);
            PointEntry next = findNearestAfter(points, endOfYear);
            if (prev == null || next == null || prev.date >= next.date) {
                return null; // Nedostatek dat pro tento rok
            }
            // Získat odhad k začátku a konci roku
            PointEntry startEst = interpolatePoint(prev, next, startOfYear);
            PointEntry endEst = interpolatePoint(prev, next, endOfYear);
            yearPoints.add(startEst);
            yearPoints.add(endEst);
            isEstimated = true;
        } else {
            // Mám alespoň nějaké body
            PointEntry minPoint = yearPoints.get(0);
            PointEntry maxPoint = yearPoints.get(yearPoints.size() - 1);

            // Zkontrolujeme, zda první bod je blízko začátku roku
            if (Math.abs(minPoint.date - startOfYear) > 2 * 86400000L && !isCurrentYear) {
                PointEntry prev = findNearestBefore(points, startOfYear);
                if (prev != null && prev.date < minPoint.date) {
                    PointEntry startEst = interpolatePoint(prev, minPoint, startOfYear);
                    yearPoints.add(0, startEst);
                    isEstimated = true;
                }
            }

            // Zkontrolujeme konec roku (pokud není aktuální rok)
            if (!isCurrentYear) {
                if (Math.abs(maxPoint.date - endOfYear) > 2 * 86400000L) {
                    PointEntry next = findNearestAfter(points, endOfYear);
                    if (next != null && next.date > maxPoint.date) {
                        PointEntry endEst = interpolatePoint(maxPoint, next, endOfYear);
                        yearPoints.add(endEst);
                        isEstimated = true;
                    }
                }
            }
        }

        // Uspořádat body za rok
        yearPoints.sort(Comparator.comparingLong(p -> p.date));

        // Nyní z po sobě jdoucích dvojic bodů v `yearPoints` spočítáme spotřebu a náklady
        double totalVT = 0;
        double totalNT = 0;
        double varCostVTTotal = 0;
        double varCostNTTotal = 0;
        double fixedCostTotal = 0;
        double totalMonthsInYear = 0;

        DataPriceListSource priceListSource = new DataPriceListSource(context);
        priceListSource.open();

        try {
            for (int i = 0; i < yearPoints.size() - 1; i++) {
                PointEntry p1 = yearPoints.get(i);
                PointEntry p2 = yearPoints.get(i + 1);

                double dVT = Math.max(0, p2.vt - p1.vt);
                double dNT = Math.max(0, p2.nt - p1.nt);
                if (dVT == 0 && dNT == 0 && p1.date == p2.date) continue;

                totalVT += dVT;
                totalNT += dNT;

                long priceListId = p2.priceListId > 0 ? p2.priceListId : p1.priceListId;
                PriceListModel rawPriceList = null;
                if (priceListId > 0) {
                    rawPriceList = priceListSource.readPrice(priceListId);
                }
                if (rawPriceList == null) {
                    ArrayList<PriceListModel> allPriceLists = priceListSource.readPriceList();
                    if (!allPriceLists.isEmpty()) {
                        rawPriceList = allPriceLists.get(0);
                    }
                }

                PriceListModel priceList;
                if (rawPriceList != null) {
                    priceList = new PriceListRegulBuilder(rawPriceList, year).getRegulPriceList();
                } else {
                    priceList = new PriceListModel();
                }

                // Výpočet délky úseku v měsících (využijeme MONTH pro měsíční odečty, INVOICE pro faktury)
                DifferenceDate.TypeDate typeDate = (p1.isInvoice || p2.isInvoice) ? DifferenceDate.TypeDate.INVOICE : DifferenceDate.TypeDate.MONTH;
                double durationMonths = Calculation.differentMonth(p1.date, p2.date, typeDate);
                if (durationMonths <= 0)
                    durationMonths = (p2.date - p1.date) / (30.4375 * 86400000.0);
                if (durationMonths <= 0) durationMonths = 0.001;

                totalMonthsInYear += durationMonths;

                double dphRatio = 1.0 + (priceList.getDph() / 100.0);

                // Čistě variabilní ceny za kWh -> převod na Kč s DPH za kWh (BEZ rozpočítání fixních nákladů)
                double vtUnitKwhWithDph = ((priceList.getCenaVT() + priceList.getDistVT() + priceList.getDan() + priceList.getSystemSluzby()) / 1000.0) * dphRatio;
                double ntUnitKwhWithDph = ((priceList.getCenaNT() + priceList.getDistNT() + priceList.getDan() + priceList.getSystemSluzby()) / 1000.0) * dphRatio;

                double varCostVT = dVT * vtUnitKwhWithDph;
                double varCostNT = dNT * ntUnitKwhWithDph;

                // Stálá měsíční platba s DPH (měsíční plat + jistič + činnost)
                double breakerPrice = Calculation.calculatePriceBreaker(priceList, subscriptionPoint.getCountPhaze(), subscriptionPoint.getPhaze());
                double cinnostOte = Math.max(priceList.getCinnost(), priceList.getOte());
                double monthlyFixedWithDph = (priceList.getMesicniPlat() + breakerPrice + cinnostOte) * dphRatio;

                // POZE s DPH (pokud se počítá v daném roce)
                double pozeCostSegment = 0;
                if (year != 2023 && year != 2026 && year != 2027) {
                    double consMWh = (dVT + dNT) / 1000.0;
                    double pozeVal = Calculation.getPozeByType(priceList, subscriptionPoint.getCountPhaze(), subscriptionPoint.getPhaze(), consMWh, durationMonths, PozeModel.TypePoze.POZE2);
                    pozeCostSegment = pozeVal * dphRatio;
                }

                varCostVTTotal += varCostVT;
                varCostNTTotal += varCostNT;
                fixedCostTotal += (durationMonths * monthlyFixedWithDph) + pozeCostSegment;
            }
        } finally {
            priceListSource.close();
        }

        if (totalVT == 0 && totalNT == 0 && fixedCostTotal == 0) {
            return null; // Žádná data pro tento rok
        }

        boolean isDualTariff = totalNT > 0;

        double totalCostAll = varCostVTTotal + varCostNTTotal + fixedCostTotal;

        // Vážené průměrné VARIABILNÍ ceny v Kč/MWh (bez fixních nákladů)
        double totalMWhVT = totalVT / 1000.0;
        double totalMWhNT = totalNT / 1000.0;
        double totalMWhAll = (totalVT + totalNT) / 1000.0;

        double avgPriceVT = totalMWhVT > 0 ? (varCostVTTotal / totalMWhVT) : 0;
        double avgPriceNT = totalMWhNT > 0 ? (varCostNTTotal / totalMWhNT) : 0;

        // Celková průměrná cena na MWh (včetně fixních nákladů rozpočtených na celkovou spotřebu MWh)
        double avgPriceTotal = totalMWhAll > 0 ? (totalCostAll / totalMWhAll) : 0;

        // Průměrný měsíční fixní náklad v Kč/měsíc s DPH
        double avgMonthlyFixedCost = totalMonthsInYear > 0 ? (fixedCostTotal / totalMonthsInYear) : 0;

        // Štítek měsíců pro aktuální rok
        String currentYearMonthsRange = getCurrentYearMonthsRange(isCurrentYear, yearPoints);

        return new AnnualYearData(
                year,
                totalVT,
                totalNT,
                varCostVTTotal,
                varCostNTTotal,
                fixedCostTotal,
                totalCostAll,
                avgPriceVT,
                avgPriceNT,
                avgPriceTotal,
                avgMonthlyFixedCost,
                totalMonthsInYear,
                isCurrentYear,
                isEstimated,
                currentYearMonthsRange,
                isDualTariff
        );
    }


    @Nullable
    private static String getCurrentYearMonthsRange(boolean isCurrentYear, ArrayList<PointEntry> yearPoints) {
        String currentYearMonthsRange = null;
        if (isCurrentYear) {
            long maxDateInYear = yearPoints.get(yearPoints.size() - 1).date;
            Calendar calMax = Calendar.getInstance();
            calMax.setTimeInMillis(maxDateInYear);
            int lastMonthIndex = calMax.get(Calendar.MONTH);
            String[] monthNames = new String[]{"leden", "únor", "březen", "duben", "květen", "červen", "červenec", "srpen", "září", "říjen", "listopad", "prosinec"};
            currentYearMonthsRange = "(leden–" + monthNames[lastMonthIndex] + ")";
        }
        return currentYearMonthsRange;
    }


    private PointEntry findNearestBefore(ArrayList<PointEntry> points, long targetDate) {
        PointEntry res = null;
        for (PointEntry p : points) {
            if (p.date < targetDate) {
                if (res == null || p.date > res.date) {
                    res = p;
                }
            }
        }
        return res;
    }


    private PointEntry findNearestAfter(ArrayList<PointEntry> points, long targetDate) {
        PointEntry res = null;
        for (PointEntry p : points) {
            if (p.date > targetDate) {
                if (res == null || p.date < res.date) {
                    res = p;
                }
            }
        }
        return res;
    }


    private PointEntry interpolatePoint(PointEntry p1, PointEntry p2, long targetDate) {
        if (p1.date >= p2.date) return p1;
        double ratio = (double) (targetDate - p1.date) / (double) (p2.date - p1.date);
        double estVT = p1.vt + (p2.vt - p1.vt) * ratio;
        double estNT = p1.nt + (p2.nt - p1.nt) * ratio;
        long priceId = ratio < 0.5 ? p1.priceListId : p2.priceListId;
        return new PointEntry(targetDate, estVT, estNT, priceId, false);
    }

}
