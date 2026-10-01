package cz.xlisto.elektrodroid.subscriptionpoint;


import org.junit.Test;

import cz.xlisto.elektrodroid.modules.subscriptionpoint.AnnualYearData;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;


public class AnnualGraphTest {

    @Test
    public void testAnnualYearDataCalculation() {
        AnnualYearData yearData = new AnnualYearData(
                2025,
                2100.0,
                1500.0,
                16128.0,
                7590.0,
                1740.0,
                25458.0,
                7680.0,
                5060.0,
                7071.66,
                145.0,
                12.0,
                false,
                false,
                null,
                true
        );

        assertEquals(2025, yearData.year());
        assertEquals(3600.0, yearData.getTotalConsumption(), 0.001);
        assertEquals(2100.0, yearData.consumptionVT(), 0.001);
        assertEquals(1500.0, yearData.consumptionNT(), 0.001);
        assertEquals(25458.0, yearData.totalCost(), 0.001);
        assertEquals(7680.0, yearData.avgPriceVT(), 0.001);
        assertEquals(5060.0, yearData.avgPriceNT(), 0.001);
        assertEquals(145.0, yearData.avgMonthlyFixedCost(), 0.001);
        assertTrue(yearData.isDualTariff());
        assertFalse(yearData.isCurrentYear());
        assertFalse(yearData.isEstimated());
    }


    @Test
    public void testSingleTariffYearData() {
        AnnualYearData yearData = new AnnualYearData(
                2024,
                1800.0,
                0.0,
                12600.0,
                0.0,
                1200.0,
                13800.0,
                7000.0,
                0.0,
                7666.66,
                100.0,
                12.0,
                false,
                true,
                null,
                false
        );

        assertEquals(1800.0, yearData.getTotalConsumption(), 0.001);
        assertEquals(1800.0, yearData.consumptionVT(), 0.001);
        assertEquals(0.0, yearData.consumptionNT(), 0.001);
        assertEquals(100.0, yearData.avgMonthlyFixedCost(), 0.001);
        assertFalse(yearData.isDualTariff());
        assertTrue(yearData.isEstimated());
    }

}
