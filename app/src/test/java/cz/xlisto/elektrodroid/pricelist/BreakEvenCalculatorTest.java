package cz.xlisto.elektrodroid.pricelist;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.List;

import cz.xlisto.elektrodroid.models.PriceListModel;
import cz.xlisto.elektrodroid.modules.pricelist.engine.BreakEvenCalculator;
import cz.xlisto.elektrodroid.modules.pricelist.engine.BreakEvenCalculator.BreakEvenResult;
import cz.xlisto.elektrodroid.modules.pricelist.engine.BreakEvenCalculator.Point2D;
import cz.xlisto.elektrodroid.modules.pricelist.engine.BreakEvenCalculator.ResultStatus;


public class BreakEvenCalculatorTest {

    private PriceListModel createSamplePriceList(String produkt, String sazba, double cenaVT, double cenaNT, double mesicniPlat, double distVT, double distNT) {
        PriceListModel pl = new PriceListModel(produkt);
        pl.setSazba(sazba);
        pl.setCenaVT(cenaVT);
        pl.setCenaNT(cenaNT);
        pl.setMesicniPlat(mesicniPlat);
        pl.setDistVT(distVT);
        pl.setDistNT(distNT);
        pl.setDan(28.30);
        pl.setSystemSluzby(100.0);
        pl.setCinnost(10.0);
        pl.setPoze1(0.0);
        pl.setPoze2(0.0);
        pl.setJ3(100.0); // 3x25A
        pl.setDph(21.0);
        pl.setPlatnostOD(cz.xlisto.elektrodroid.ownview.ViewHelper.parseCalendarFromString("1.1.2025").getTimeInMillis());
        pl.setPlatnostDO(cz.xlisto.elektrodroid.ownview.ViewHelper.parseCalendarFromString("31.12.2025").getTimeInMillis());
        return pl;
    }


    @Test
    public void testBreakEvenCalculationD01dVsD02d() {
        // D01d: Nižší stálý plat (50), ale vyšší cena distribuované MWh (3000)
        PriceListModel d01 = createSamplePriceList("Produkt D01", "D01d", 2000, 2000, 50, 3000, 3000);

        // D02d: Vyšší stálý plat (150), ale nižší cena distribuované MWh (1500)
        PriceListModel d02 = createSamplePriceList("Produkt D02", "D02d", 2000, 2000, 150, 1500, 1500);

        BreakEvenResult result = BreakEvenCalculator.calculateBreakEvenConsumption(d01, d02, 0.0, 12, 3, 25);

        assertNotNull(result);
        assertEquals(ResultStatus.EXACT_INTERSECTION, result.status);
        assertTrue(result.breakEvenMwh > 0);

        // Ověříme generování křivky
        List<Point2D> curveD01 = BreakEvenCalculator.generateCostVsConsumptionCurve(d01, 0.0, 12, 3, 25, 10.0, 10);
        assertEquals(11, curveD01.size());
        assertEquals(0.0, curveD01.get(0).x, 0.001);
        assertEquals(10.0, curveD01.get(10).x, 0.001);
    }


    @Test
    public void testBreakEvenNoIntersection() {
        PriceListModel expensive = createSamplePriceList("Dražší", "D01d", 4000, 4000, 200, 3000, 3000);
        PriceListModel cheap = createSamplePriceList("Levnější", "D01d", 2000, 2000, 50, 1000, 1000);

        BreakEvenResult result = BreakEvenCalculator.calculateBreakEvenConsumption(expensive, cheap, 0.0, 12, 3, 25);

        assertNotNull(result);
        assertEquals(ResultStatus.NO_INTERSECTION_B_CHEAPER, result.status);
    }

}
