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


    @Test
    public void findNextTrigger_monthlyUpcomingInCurrentMonth() {
        Calendar now = Calendar.getInstance();
        now.clear();
        now.set(2026, Calendar.MAY, 5, 10, 0, 0); // May 5, 2026 10:00

        long trigger = MonthlyReadingReminderScheduler.findNextTrigger(0, 15, 0, 8, 0, now.getTimeInMillis());

        Calendar triggerCal = Calendar.getInstance();
        triggerCal.setTimeInMillis(trigger);

        assertEquals(2026, triggerCal.get(Calendar.YEAR));
        assertEquals(Calendar.MAY, triggerCal.get(Calendar.MONTH));
        assertEquals(15, triggerCal.get(Calendar.DAY_OF_MONTH));
        assertEquals(8, triggerCal.get(Calendar.HOUR_OF_DAY));
        assertEquals(0, triggerCal.get(Calendar.MINUTE));
    }


    @Test
    public void findNextTrigger_monthlyPassedInCurrentMonthMovesToNextMonth() {
        Calendar now = Calendar.getInstance();
        now.clear();
        now.set(2026, Calendar.MAY, 20, 10, 0, 0); // May 20, 2026 10:00

        long trigger = MonthlyReadingReminderScheduler.findNextTrigger(0, 15, 0, 8, 0, now.getTimeInMillis());

        Calendar triggerCal = Calendar.getInstance();
        triggerCal.setTimeInMillis(trigger);

        assertEquals(2026, triggerCal.get(Calendar.YEAR));
        assertEquals(Calendar.JUNE, triggerCal.get(Calendar.MONTH));
        assertEquals(15, triggerCal.get(Calendar.DAY_OF_MONTH));
        assertEquals(8, triggerCal.get(Calendar.HOUR_OF_DAY));
        assertEquals(0, triggerCal.get(Calendar.MINUTE));
    }


    @Test
    public void findNextTrigger_weeklyUpcomingInCurrentWeek() {
        Calendar now = Calendar.getInstance();
        now.clear();
        now.set(2026, Calendar.MAY, 11, 10, 0, 0); // Monday May 11, 2026 10:00

        // Wednesday is index 2
        long trigger = MonthlyReadingReminderScheduler.findNextTrigger(MonthlyReadingReminderScheduler.FREQUENCY_WEEKLY, 1, 2, 8, 0, now.getTimeInMillis());

        Calendar triggerCal = Calendar.getInstance();
        triggerCal.setTimeInMillis(trigger);

        assertEquals(2026, triggerCal.get(Calendar.YEAR));
        assertEquals(Calendar.MAY, triggerCal.get(Calendar.MONTH));
        assertEquals(13, triggerCal.get(Calendar.DAY_OF_MONTH)); // Wednesday May 13
        assertEquals(8, triggerCal.get(Calendar.HOUR_OF_DAY));
        assertEquals(0, triggerCal.get(Calendar.MINUTE));
    }

}

