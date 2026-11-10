package cz.xlisto.elektrodroid.pricelist;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import cz.xlisto.elektrodroid.models.PriceListModel;
import cz.xlisto.elektrodroid.modules.pricelist.engine.PriceListCalculationEngine;
import cz.xlisto.elektrodroid.modules.pricelist.models.PriceListComparisonResult;
import cz.xlisto.elektrodroid.modules.pricelist.models.PriceListComparisonScenario;


public class PriceListCalculationEngineTest {

    private PriceListModel createSamplePriceList(String produkt, String sazba, double cenaVT, double cenaNT, double mesicniPlat, double distVT, double distNT, int year) {
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
        pl.setPoze1(10.0);
        pl.setPoze2(495.0);
        pl.setJ3(150.0); // Jistič 3x25A
        pl.setDph(21.0);
        pl.setPlatnostOD(cz.xlisto.elektrodroid.ownview.ViewHelper.parseCalendarFromString("1.1." + year).getTimeInMillis());
        pl.setPlatnostDO(cz.xlisto.elektrodroid.ownview.ViewHelper.parseCalendarFromString("31.12." + year).getTimeInMillis());
        return pl;
    }


    @Test
    public void testCalculationD01dVsD02d() {
        PriceListModel d01 = createSamplePriceList("Produkt 1", "D01d", 3000, 3000, 50, 2000, 2000, 2025);
        PriceListModel d02 = createSamplePriceList("Produkt 2", "D02d", 2500, 2500, 100, 1500, 1500, 2025);

        PriceListComparisonScenario scenario = new PriceListComparisonScenario(2000, 0, 12, 3, 25, 0, 0);

        PriceListComparisonResult resD01 = PriceListCalculationEngine.calculate(d01, scenario);
        PriceListComparisonResult resD02 = PriceListCalculationEngine.calculate(d02, scenario);

        assertNotNull(resD01);
        assertNotNull(resD02);

        // Ověření rozpadu ceny (Obchodní + Regulovaná + Daně = Bez DPH)
        double expectedNoDph1 = resD01.getMerchantTotalCost() + resD01.getRegulatedTotalCost() + resD01.getElectricityTax();
        assertEquals(expectedNoDph1, resD01.getTotalWithoutDph(), 0.01);

        double expectedWithDph1 = expectedNoDph1 + (expectedNoDph1 * 0.21);
        assertEquals(expectedWithDph1, resD01.getTotalWithDph(), 0.01);

        // D02d by mělo být levnější při 2 MWh díky nižší distribuci i nižší obchodní ceně za MWh navzdory vyššímu stálému platu
        assertTrue(resD02.getTotalWithDph() < resD01.getTotalWithDph());
    }


    @Test
    public void testPozeYear2026Zero() {
        PriceListModel pl2025 = createSamplePriceList("Test 2025", "D01d", 3000, 3000, 50, 1000, 1000, 2025);
        PriceListModel pl2026 = createSamplePriceList("Test 2026", "D01d", 3000, 3000, 50, 1000, 1000, 2026);
        pl2026.setPoze1(0.0);
        pl2026.setPoze2(0.0);

        PriceListComparisonScenario scenario = new PriceListComparisonScenario(1000, 0, 12, 3, 25, 0, 0);

        PriceListComparisonResult res2025 = PriceListCalculationEngine.calculate(pl2025, scenario);
        PriceListComparisonResult res2026 = PriceListCalculationEngine.calculate(pl2026, scenario);

        assertTrue(res2025.getPozeCost() > 0);
        assertEquals(0.0, res2026.getPozeCost(), 0.001);
    }


    @Test
    public void testEvaluateAllSorting() {
        PriceListModel d01 = createSamplePriceList("Dražší", "D01d", 4000, 4000, 100, 2000, 2000, 2025);
        PriceListModel d02 = createSamplePriceList("Levnější", "D02d", 2000, 2000, 50, 1000, 1000, 2025);

        List<PriceListModel> list = new ArrayList<>();
        list.add(d01);
        list.add(d02);

        PriceListComparisonScenario scenario = new PriceListComparisonScenario(3000, 0, 12, 3, 25, 0, 0);

        List<PriceListComparisonResult> results = PriceListCalculationEngine.evaluateAll(list, scenario);

        assertEquals(2, results.size());
        assertEquals("Levnější", results.get(0).getPriceList().getProdukt());
        assertTrue(results.get(0).isCheapest());
        assertEquals(0.0, results.get(0).getDiffVsCheapest(), 0.001);

        assertEquals("Dražší", results.get(1).getPriceList().getProdukt());
        org.junit.Assert.assertFalse(results.get(1).isCheapest());
        assertTrue(results.get(1).getDiffVsCheapest() > 0);
    }

}
