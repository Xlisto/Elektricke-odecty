package cz.xlisto.elektrodroid.modules.widgets;


import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.util.Calendar;
import java.util.TimeZone;


/**
 * Jednotkové testy pro {@link WidgetFormatHelper}.
 */
public class WidgetFormatHelperTest {

    private static final TimeZone UTC = TimeZone.getTimeZone("UTC");


    private static long utcDate(int year, int monthZeroBased, int day) {
        Calendar cal = Calendar.getInstance(UTC);
        cal.set(year, monthZeroBased, day, 12, 0, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis();
    }


    @Test
    public void formatsPricesAndAmountsInCzechFormat() {
        assertEquals("5,42", WidgetFormatHelper.formatPriceKwh(5.424));
        assertEquals("0,00", WidgetFormatHelper.formatPriceKwh(-2.5));
        assertEquals("1\u00A0245 kWh", WidgetFormatHelper.formatKwh(1245.2));
        assertEquals("24\u00A0500 Kč", WidgetFormatHelper.formatCzk(24499.6));
    }


    @Test
    public void formatsShortPeriodRangeSameAndCrossYear() {
        long start2026 = utcDate(2026, Calendar.JANUARY, 1);
        long end2026 = utcDate(2026, Calendar.NOVEMBER, 24);
        assertEquals("01.01. – 24.11.2026", WidgetFormatHelper.formatShortPeriodRange(start2026, end2026, UTC));

        long start2025 = utcDate(2025, Calendar.OCTOBER, 15);
        assertEquals("15.10.2025 – 24.11.2026", WidgetFormatHelper.formatShortPeriodRange(start2025, end2026, UTC));

        assertEquals("", WidgetFormatHelper.formatShortPeriodRange(0L, end2026, UTC));
    }


    @Test
    public void formatsSingleDateInCzechFormat() {
        long end2026 = utcDate(2026, Calendar.DECEMBER, 31);
        assertEquals("31.12.2026", WidgetFormatHelper.formatDate(end2026, UTC));
        assertEquals("", WidgetFormatHelper.formatDate(0L, UTC));
    }


    @Test
    public void computesTariffPercentagesSummingToHundred() {
        int[] zero = WidgetFormatHelper.computeTariffPercentages(0.0, 0.0);
        assertEquals(0, zero[0]);
        assertEquals(0, zero[1]);

        int[] split = WidgetFormatHelper.computeTariffPercentages(333.3, 666.7);
        assertEquals(33, split[0]);
        assertEquals(67, split[1]);
        assertEquals(100, split[0] + split[1]);
    }

}
