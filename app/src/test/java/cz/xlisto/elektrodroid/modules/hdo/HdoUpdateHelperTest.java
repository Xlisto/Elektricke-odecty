package cz.xlisto.elektrodroid.modules.hdo;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Calendar;
import java.util.Date;

import org.junit.Test;


public class HdoUpdateHelperTest {

    private Date createDate(int year, int month, int day, int hour, int min, int sec) {
        Calendar cal = Calendar.getInstance();
        cal.set(year, month - 1, day, hour, min, sec);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }


    @Test
    public void testEgdCrossYearPeriodActiveInNovember() {
        // Platnost 01.11 - 31.01, testujeme dnešek v listopadu 2026 (5.11.2026)
        Date refDate = createDate(2026, 11, 5, 12, 0, 0);
        Date[] range = HdoUpdateHelper.parseDateRange("01.11.", "31.01.", refDate);

        assertNotNull(range);
        assertNotNull(range[0]);
        assertNotNull(range[1]);

        Calendar calFrom = Calendar.getInstance();
        calFrom.setTime(range[0]);
        Calendar calUntil = Calendar.getInstance();
        calUntil.setTime(range[1]);

        assertEquals(2026, calFrom.get(Calendar.YEAR));
        assertEquals(Calendar.NOVEMBER, calFrom.get(Calendar.MONTH));
        assertEquals(1, calFrom.get(Calendar.DAY_OF_MONTH));

        assertEquals(2027, calUntil.get(Calendar.YEAR));
        assertEquals(Calendar.JANUARY, calUntil.get(Calendar.MONTH));
        assertEquals(31, calUntil.get(Calendar.DAY_OF_MONTH));

        // Datum konce platnosti (31.1.2027) nesmí být před dneškem (5.11.2026) -> Kód je platný!
        assertTrue("31.01.2027 sould be after 05.11.2026", range[1].after(refDate));
    }


    @Test
    public void testEgdCrossYearPeriodActiveInJanuary() {
        // Platnost 01.11 - 31.01, testujeme dnešek v lednu 2027 (15.1.2027)
        Date refDate = createDate(2027, 1, 15, 12, 0, 0);
        Date[] range = HdoUpdateHelper.parseDateRange("01.11.", "31.01.", refDate);

        assertNotNull(range);
        assertNotNull(range[0]);
        assertNotNull(range[1]);

        Calendar calFrom = Calendar.getInstance();
        calFrom.setTime(range[0]);
        Calendar calUntil = Calendar.getInstance();
        calUntil.setTime(range[1]);

        assertEquals(2026, calFrom.get(Calendar.YEAR));
        assertEquals(Calendar.NOVEMBER, calFrom.get(Calendar.MONTH));

        assertEquals(2027, calUntil.get(Calendar.YEAR));
        assertEquals(Calendar.JANUARY, calUntil.get(Calendar.MONTH));

        assertTrue("31.01.2027 should be after 15.01.2027", range[1].after(refDate));
    }


    @Test
    public void testEgdCrossYearPeriodExpiredInFebruary() {
        // Platnost 01.11 - 31.01, testujeme dnešek v únoru 2027 (10.2.2027)
        Date refDate = createDate(2027, 2, 10, 12, 0, 0);
        Date[] range = HdoUpdateHelper.parseDateRange("01.11.", "31.01.", refDate);

        assertNotNull(range);
        assertNotNull(range[1]);

        Calendar calUntil = Calendar.getInstance();
        calUntil.setTime(range[1]);

        assertEquals(2027, calUntil.get(Calendar.YEAR));
        assertEquals(Calendar.JANUARY, calUntil.get(Calendar.MONTH));

        assertTrue("31.01.2027 should be before 10.02.2027", range[1].before(refDate));
    }


    @Test
    public void testEgdSameYearPeriodActiveInJune() {
        // Platnost 01.04 - 31.08, testujeme dnešek v červnu 2026
        Date refDate = createDate(2026, 6, 15, 12, 0, 0);
        Date[] range = HdoUpdateHelper.parseDateRange("01.04.", "31.08.", refDate);

        assertNotNull(range);
        assertNotNull(range[0]);
        assertNotNull(range[1]);

        Calendar calFrom = Calendar.getInstance();
        calFrom.setTime(range[0]);
        Calendar calUntil = Calendar.getInstance();
        calUntil.setTime(range[1]);

        assertEquals(2026, calFrom.get(Calendar.YEAR));
        assertEquals(Calendar.APRIL, calFrom.get(Calendar.MONTH));

        assertEquals(2026, calUntil.get(Calendar.YEAR));
        assertEquals(Calendar.AUGUST, calUntil.get(Calendar.MONTH));

        assertTrue("31.08.2026 should be after 15.06.2026", range[1].after(refDate));
    }


    @Test
    public void testEgdSameYearPeriodExpiredInNovember() {
        // Platnost 01.09 - 31.10, testujeme dnešek v listopadu 2026 (5.11.2026)
        Date refDate = createDate(2026, 11, 5, 12, 0, 0);
        Date[] range = HdoUpdateHelper.parseDateRange("01.09.", "31.10.", refDate);

        assertNotNull(range);
        assertNotNull(range[1]);

        Calendar calUntil = Calendar.getInstance();
        calUntil.setTime(range[1]);

        assertEquals(2026, calUntil.get(Calendar.YEAR));
        assertEquals(Calendar.OCTOBER, calUntil.get(Calendar.MONTH));

        assertTrue("31.10.2026 should be before 05.11.2026", range[1].before(refDate));
    }


    @Test
    public void testExactYearPeriod() {
        Date refDate = createDate(2026, 11, 5, 12, 0, 0);
        Date[] range = HdoUpdateHelper.parseDateRange("01.11.2026", "31.01.2027", refDate);

        assertNotNull(range);
        assertNotNull(range[0]);
        assertNotNull(range[1]);

        Calendar calFrom = Calendar.getInstance();
        calFrom.setTime(range[0]);
        Calendar calUntil = Calendar.getInstance();
        calUntil.setTime(range[1]);

        assertEquals(2026, calFrom.get(Calendar.YEAR));
        assertEquals(2027, calUntil.get(Calendar.YEAR));
    }


    @Test
    public void testVariationsOfEgdFormats() {
        Date refDate = createDate(2026, 11, 5, 12, 0, 0);

        // "1.11." - "31.1."
        Date[] range1 = HdoUpdateHelper.parseDateRange("1.11.", "31.1.", refDate);
        assertNotNull(range1);
        Calendar cUntil1 = Calendar.getInstance();
        cUntil1.setTime(range1[1]);
        assertEquals(2027, cUntil1.get(Calendar.YEAR));
        assertEquals(Calendar.JANUARY, cUntil1.get(Calendar.MONTH));

        // "01.11" - "31.01"
        Date[] range2 = HdoUpdateHelper.parseDateRange("01.11", "31.01", refDate);
        assertNotNull(range2);
        Calendar cUntil2 = Calendar.getInstance();
        cUntil2.setTime(range2[1]);
        assertEquals(2027, cUntil2.get(Calendar.YEAR));
        assertEquals(Calendar.JANUARY, cUntil2.get(Calendar.MONTH));
    }

}
