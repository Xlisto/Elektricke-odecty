package cz.xlisto.elektrodroid.modules.widgets;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Calendar;
import java.util.Collections;
import java.util.TimeZone;

import cz.xlisto.elektrodroid.models.PriceListModel;
import cz.xlisto.elektrodroid.models.SubscriptionPointModel;
import cz.xlisto.elektrodroid.modules.dashboard.DashboardHdoFormatHelper;


/**
 * Jednotkové testy pro {@link WidgetDataService} ověřující kontrolu platnosti ceníků (včetně
 * přechodného období po vypršení ceníku s orientační cenou), výpočet celkové vs. obchodní ceny
 * (s DPH i bez DPH) a rozpoznání jednosazbových tarifů.
 */
public class WidgetDataServiceTest {

    private static final TimeZone UTC = TimeZone.getTimeZone("UTC");


    private static long utcMillis(int year, int monthZeroBased, int day, int hour, int minute) {
        Calendar cal = Calendar.getInstance(UTC);
        cal.set(year, monthZeroBased, day, hour, minute, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis();
    }


    private static PriceListModel createSamplePriceList(long id, long validFrom, long validTo, String rate) {
        return new PriceListModel(
                id,
                "Řada",
                "Produkt",
                "Dodavatel",
                3000.0, // priceVT (Kč/MWh) -> 3.00 Kč/kWh
                2000.0, // priceNT (Kč/MWh) -> 2.00 Kč/kWh
                120.0,  // monthlyPayment
                28.3,   // tax
                rate,
                1500.0, // distVT (Kč/MWh)
                500.0,  // distNT (Kč/MWh)
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                200.0,  // systemServices
                10.0,   // activity
                10.0,   // poze1
                495.0,  // poze2
                0.0,    // oze
                10.0,   // ote
                validFrom,
                validTo,
                21.0,   // vat %
                "ČEZ",
                "Test",
                validFrom,
                ""
        );
    }


    private static WidgetDataService.HdoSnapshot emptyHdoSnapshot() {
        return new WidgetDataService.HdoSnapshot(
                Collections.emptyList(),
                false,
                false,
                -1L,
                0L,
                DashboardHdoFormatHelper.FormattedHdoSwitch.invalid()
        );
    }


    @Test
    public void evaluatePriceListValidityDetectsMissingValidExpiredAndFutureLists() {
        long from = utcMillis(2026, Calendar.JANUARY, 1, 0, 0);
        long to = utcMillis(2026, Calendar.DECEMBER, 31, 0, 0);
        PriceListModel list = createSamplePriceList(10L, from, to, "D 57d");

        assertEquals(WidgetDataService.PriceListValidityStatus.MISSING,
                WidgetDataService.evaluatePriceListValidity(null, utcMillis(2026, Calendar.JUNE, 1, 12, 0), UTC));
        assertEquals(WidgetDataService.PriceListValidityStatus.MISSING,
                WidgetDataService.evaluatePriceListValidity(new PriceListModel(), utcMillis(2026, Calendar.JUNE, 1, 12, 0), UTC));

        // Před začátkem platnosti
        assertEquals(WidgetDataService.PriceListValidityStatus.NOT_YET_VALID,
                WidgetDataService.evaluatePriceListValidity(list, utcMillis(2025, Calendar.DECEMBER, 31, 23, 59), UTC));

        // Uprostřed platnosti i v poslední den platnosti ve 23:59
        assertEquals(WidgetDataService.PriceListValidityStatus.VALID,
                WidgetDataService.evaluatePriceListValidity(list, utcMillis(2026, Calendar.JUNE, 15, 10, 0), UTC));
        assertEquals(WidgetDataService.PriceListValidityStatus.VALID,
                WidgetDataService.evaluatePriceListValidity(list, utcMillis(2026, Calendar.DECEMBER, 31, 23, 59), UTC));

        // Po konci platnosti
        assertEquals(WidgetDataService.PriceListValidityStatus.EXPIRED,
                WidgetDataService.evaluatePriceListValidity(list, utcMillis(2027, Calendar.JANUARY, 1, 0, 0), UTC));
    }


    @Test
    public void buildCurrentPriceSnapshotProvidesIndicativePricesDuringTransitionAfterExpiry() {
        long from2026 = utcMillis(2026, Calendar.JANUARY, 1, 0, 0);
        long to2026 = utcMillis(2026, Calendar.DECEMBER, 31, 0, 0);
        PriceListModel list2026 = createSamplePriceList(10L, from2026, to2026, "D 57d");
        SubscriptionPointModel point = new SubscriptionPointModel(1L, "Domov", "", 1700001L, 3, 25, "111", "222");
        WidgetConfig config = new WidgetConfig(101, WidgetType.CURRENT_PRICE, 1L, 1700001L, "Domov", false, true);

        // Leden 2027: ceník 2026 vypršel, nový ceník ještě nebyl přiřazen
        long jan2027 = utcMillis(2027, Calendar.JANUARY, 15, 10, 0);
        WidgetDataService.CurrentPriceSnapshot expiredSnapshot = WidgetDataService.buildCurrentPriceSnapshot(
                list2026, point, config, emptyHdoSnapshot(), 0xFFFF0000, 0xFF0000FF, jan2027, UTC);

        assertEquals(WidgetDataService.PriceListValidityStatus.EXPIRED, expiredSnapshot.priceListStatus());
        assertTrue(expiredSnapshot.isPriceListExpired());
        assertTrue(expiredSnapshot.hasDisplayablePrices());
        assertEquals(to2026, expiredSnapshot.priceListValidToMillis());
        assertEquals(3.63, expiredSnapshot.vtPriceKwh(), 0.0001);
        assertEquals(2.42, expiredSnapshot.ntPriceKwh(), 0.0001);

        // Platný ceník v roce 2026
        long mid2026 = utcMillis(2026, Calendar.JUNE, 15, 10, 0);
        WidgetDataService.CurrentPriceSnapshot validSnapshot = WidgetDataService.buildCurrentPriceSnapshot(
                list2026, point, config, emptyHdoSnapshot(), 0xFFFF0000, 0xFF0000FF, mid2026, UTC);
        assertEquals(WidgetDataService.PriceListValidityStatus.VALID, validSnapshot.priceListStatus());
        assertFalse(validSnapshot.isPriceListExpired());
        assertTrue(validSnapshot.hasDisplayablePrices());
        assertEquals(3.63, validSnapshot.vtPriceKwh(), 0.0001);

        // Budoucí ceník a chybějící ceník nesmí zobrazit cenu
        long dec2025 = utcMillis(2025, Calendar.DECEMBER, 15, 10, 0);
        WidgetDataService.CurrentPriceSnapshot futureSnapshot = WidgetDataService.buildCurrentPriceSnapshot(
                list2026, point, config, emptyHdoSnapshot(), 0xFFFF0000, 0xFF0000FF, dec2025, UTC);
        assertEquals(WidgetDataService.PriceListValidityStatus.NOT_YET_VALID, futureSnapshot.priceListStatus());
        assertFalse(futureSnapshot.hasDisplayablePrices());
        assertEquals(from2026, futureSnapshot.priceListValidFromMillis());
        assertEquals(0.0, futureSnapshot.vtPriceKwh(), 0.0001);

        WidgetDataService.CurrentPriceSnapshot missingSnapshot = WidgetDataService.buildCurrentPriceSnapshot(
                null, point, config, emptyHdoSnapshot(), 0xFFFF0000, 0xFF0000FF, mid2026, UTC);
        assertEquals(WidgetDataService.PriceListValidityStatus.MISSING, missingSnapshot.priceListStatus());
        assertFalse(missingSnapshot.hasDisplayablePrices());
        assertEquals(0.0, missingSnapshot.vtPriceKwh(), 0.0001);
    }


    @Test
    public void computeUnitPricesSupportsCommercialAndTotalWithAndWithoutVat() {
        long from = utcMillis(2026, Calendar.JANUARY, 1, 0, 0);
        long to = utcMillis(2026, Calendar.DECEMBER, 31, 0, 0);
        PriceListModel list = createSamplePriceList(10L, from, to, "D 57d");
        SubscriptionPointModel point = new SubscriptionPointModel(1L, "Domov", "", 1700001L, 3, 25, "111", "222");

        // Obchodní bez DPH: 3000/1000 = 3.00 VT, 2000/1000 = 2.00 NT
        double[] commNoVat = WidgetDataService.computeUnitPrices(list, point, false, false);
        assertEquals(3.0, commNoVat[0], 0.0001);
        assertEquals(2.0, commNoVat[1], 0.0001);

        // Obchodní s DPH (21 %): 3.63 VT, 2.42 NT
        double[] commWithVat = WidgetDataService.computeUnitPrices(list, point, false, true);
        assertEquals(3.63, commWithVat[0], 0.0001);
        assertEquals(2.42, commWithVat[1], 0.0001);

        // Celková cena musí být vyšší než obchodní a s DPH přesně * 1.21
        double[] totalNoVat = WidgetDataService.computeUnitPrices(list, point, true, false);
        double[] totalWithVat = WidgetDataService.computeUnitPrices(list, point, true, true);
        assertTrue(totalNoVat[0] > commNoVat[0]);
        assertTrue(totalNoVat[1] > commNoVat[1]);
        assertEquals(totalNoVat[0] * 1.21, totalWithVat[0], 0.0001);
        assertEquals(totalNoVat[1] * 1.21, totalWithVat[1], 0.0001);
    }


    @Test
    public void recognizesSingleTariffRates() {
        assertTrue(WidgetDataService.isSingleTariffRate("D 01d"));
        assertTrue(WidgetDataService.isSingleTariffRate("D01d"));
        assertTrue(WidgetDataService.isSingleTariffRate("D 02d"));
        assertTrue(WidgetDataService.isSingleTariffRate("d02d"));
        assertFalse(WidgetDataService.isSingleTariffRate("D 25d"));
        assertFalse(WidgetDataService.isSingleTariffRate("D 57d"));
        assertFalse(WidgetDataService.isSingleTariffRate(""));
        assertFalse(WidgetDataService.isSingleTariffRate(null));
    }

}
