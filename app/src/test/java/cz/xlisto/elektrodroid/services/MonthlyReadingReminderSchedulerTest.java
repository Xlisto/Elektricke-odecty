package cz.xlisto.elektrodroid.services;


import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.util.Calendar;


public class MonthlyReadingReminderSchedulerTest {

    @Test
    public void resolveTargetDayOfMonth_capsDayToMonthEnd() {
        assertEquals(28, MonthlyReadingReminderScheduler.resolveTargetDayOfMonth(31, 28));
        assertEquals(30, MonthlyReadingReminderScheduler.resolveTargetDayOfMonth(31, 30));
        assertEquals(1, MonthlyReadingReminderScheduler.resolveTargetDayOfMonth(0, 31));
    }


    @Test
    public void createMonthlyCandidate_usesLastDayOfFebruaryWhenNeeded() {
        Calendar calendar = Calendar.getInstance();
        calendar.clear();
        calendar.set(2026, Calendar.FEBRUARY, 1, 0, 0, 0);

        Calendar candidate = MonthlyReadingReminderScheduler.createMonthlyCandidate(calendar, 31, 8, 15);

        assertEquals(2026, candidate.get(Calendar.YEAR));
        assertEquals(Calendar.FEBRUARY, candidate.get(Calendar.MONTH));
        assertEquals(28, candidate.get(Calendar.DAY_OF_MONTH));
        assertEquals(8, candidate.get(Calendar.HOUR_OF_DAY));
        assertEquals(15, candidate.get(Calendar.MINUTE));
    }

}

