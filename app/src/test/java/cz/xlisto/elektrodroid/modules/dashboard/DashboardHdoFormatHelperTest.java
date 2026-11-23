package cz.xlisto.elektrodroid.modules.dashboard;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Calendar;
import java.util.TimeZone;


/**
 * Jednotkové testy pro {@link DashboardHdoFormatHelper} – formátování absolutního času
 * dalšího přepnutí HDO, českých zkratek dnů v týdnu a relativního odpočtu zbývající doby.
 */
public class DashboardHdoFormatHelperTest {

    private static final TimeZone PRAGUE_TZ = TimeZone.getTimeZone("Europe/Prague");


    @Test
    public void formatNextSwitch_sameDayReturnsTodayFormatAndRemainingTime() {
        long now = november2026Millis(23, 19, 26);
        long target = november2026Millis(23, 21, 41);

        DashboardHdoFormatHelper.FormattedHdoSwitch result =
                DashboardHdoFormatHelper.formatNextSwitch(target, now, PRAGUE_TZ);

        assertTrue(result.valid());
        assertTrue(result.isToday());
        assertEquals("21:41", result.timeText());
        assertEquals("2 h 15 min", result.remainingText());
    }


    @Test
    public void formatNextSwitch_otherDayIncludesCzechDayAbbreviation() {
        // 25. 11. 2026 je středa, 26. 11. 2026 je čtvrtek
        long now = november2026Millis(25, 22, 0);
        long target = november2026Millis(26, 1, 30);

        DashboardHdoFormatHelper.FormattedHdoSwitch result =
                DashboardHdoFormatHelper.formatNextSwitch(target, now, PRAGUE_TZ);

        assertTrue(result.valid());
        assertFalse(result.isToday());
        assertEquals("Čt 01:30", result.timeText());
        assertEquals("3 h 30 min", result.remainingText());
    }


    @Test
    public void formatNextSwitch_invalidOrPastTimestampReturnsInvalid() {
        long now = november2026Millis(23, 12, 0);

        DashboardHdoFormatHelper.FormattedHdoSwitch zeroTarget =
                DashboardHdoFormatHelper.formatNextSwitch(0L, now, PRAGUE_TZ);
        assertFalse(zeroTarget.valid());
        assertEquals("", zeroTarget.timeText());
        assertNull(zeroTarget.remainingText());

        DashboardHdoFormatHelper.FormattedHdoSwitch pastTarget =
                DashboardHdoFormatHelper.formatNextSwitch(now - 60_000L, now, PRAGUE_TZ);
        assertFalse(pastTarget.valid());

        DashboardHdoFormatHelper.FormattedHdoSwitch equalTarget =
                DashboardHdoFormatHelper.formatNextSwitch(now, now, PRAGUE_TZ);
        assertFalse(equalTarget.valid());

        DashboardHdoFormatHelper.FormattedHdoSwitch invalidNow =
                DashboardHdoFormatHelper.formatNextSwitch(now + 60_000L, 0L, PRAGUE_TZ);
        assertFalse(invalidNow.valid());
    }


    @Test
    public void formatCzechDayOfWeekShort_mapsAllSevenDays() {
        assertEquals("Po", DashboardHdoFormatHelper.formatCzechDayOfWeekShort(Calendar.MONDAY));
        assertEquals("Út", DashboardHdoFormatHelper.formatCzechDayOfWeekShort(Calendar.TUESDAY));
        assertEquals("St", DashboardHdoFormatHelper.formatCzechDayOfWeekShort(Calendar.WEDNESDAY));
        assertEquals("Čt", DashboardHdoFormatHelper.formatCzechDayOfWeekShort(Calendar.THURSDAY));
        assertEquals("Pá", DashboardHdoFormatHelper.formatCzechDayOfWeekShort(Calendar.FRIDAY));
        assertEquals("So", DashboardHdoFormatHelper.formatCzechDayOfWeekShort(Calendar.SATURDAY));
        assertEquals("Ne", DashboardHdoFormatHelper.formatCzechDayOfWeekShort(Calendar.SUNDAY));
    }


    @Test
    public void formatRemainingDuration_formatsSubMinuteMinutesHoursAndDays() {
        assertNull(DashboardHdoFormatHelper.formatRemainingDuration(0L));
        assertNull(DashboardHdoFormatHelper.formatRemainingDuration(-5_000L));
        assertEquals("< 1 min", DashboardHdoFormatHelper.formatRemainingDuration(30_000L));
        assertEquals("1 min", DashboardHdoFormatHelper.formatRemainingDuration(60_000L));
        assertEquals("45 min", DashboardHdoFormatHelper.formatRemainingDuration(45L * 60_000L));
        assertEquals("2 h", DashboardHdoFormatHelper.formatRemainingDuration(2L * 60L * 60_000L));
        assertEquals("2 h 15 min", DashboardHdoFormatHelper.formatRemainingDuration((2L * 60L + 15L) * 60_000L));
        assertEquals("1 d 2 h 5 min", DashboardHdoFormatHelper.formatRemainingDuration(((24L + 2L) * 60L + 5L) * 60_000L));
    }


    private static long november2026Millis(int day, int hour, int minute) {
        Calendar cal = Calendar.getInstance(PRAGUE_TZ);
        cal.clear();
        cal.set(2026, Calendar.NOVEMBER, day, hour, minute, 0);
        return cal.getTimeInMillis();
    }

}
