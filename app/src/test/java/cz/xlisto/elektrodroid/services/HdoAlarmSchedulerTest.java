package cz.xlisto.elektrodroid.services;


import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

import cz.xlisto.elektrodroid.models.HdoModel;


/**
 * Jednotkové testy pro {@link HdoAlarmScheduler} – plynulé navazování přes půlnoc,
 * vyhodnocení neaktuálních/prošlých HDO rozvrhů a výběr aktuálně platné sezóny.
 */
public class HdoAlarmSchedulerTest {

    @Test
    public void testMidnightContiguousExtension() {
        // Models for C45d,C46d-relé1-TAR:
        // Model 1: Mon-Fri 00:00 - 06:15 (mon=1, tue=1, wed=1, thu=1, fri=1, sat=0, sun=0)
        // Model 2: Sat-Sun 22:30 - 0:00 (mon=0, tue=0, wed=0, thu=0, fri=0, sat=1, sun=1)
        HdoModel modelMonFri = new HdoModel(1, "C45d,C46d-relé1-TAR", "01.09.2026", "25.10.2026", "00:00", "06:15", 1, 1, 1, 1, 1, 0, 0, "CEZ");
        HdoModel modelSatSun = new HdoModel(2, "C45d,C46d-relé1-TAR", "01.09.2026", "25.10.2026", "22:30", "0:00", 0, 0, 0, 0, 0, 1, 1, "CEZ");

        List<HdoModel> models = new ArrayList<>();
        models.add(modelMonFri);
        models.add(modelSatSun);

        // Sunday 04.10.2026 at 23:24:43
        Calendar sundayNight = Calendar.getInstance();
        sundayNight.set(2026, Calendar.OCTOBER, 4, 23, 24, 43);
        sundayNight.set(Calendar.MILLISECOND, 0);

        long nowMillis = sundayNight.getTimeInMillis();
        long timeShift = 0;

        long endTrigger = HdoAlarmScheduler.findNextTriggerForModels(models, HdoAlarmScheduler.TYPE_END, nowMillis, timeShift);

        // Expected end: Monday 05.10.2026 at 06:15
        Calendar expectedEnd = Calendar.getInstance();
        expectedEnd.set(2026, Calendar.OCTOBER, 5, 6, 15, 0);
        expectedEnd.set(Calendar.MILLISECOND, 0);

        assertEquals("End trigger should extend seamlessly to Monday 06:15", expectedEnd.getTimeInMillis(), endTrigger);
    }


    @Test
    public void testOutdatedSeasonalHdoUsesWeeklyFallbackInsteadOfJumpingMonthsAhead() {
        // Neaktuální sezónní HDO bez roku (01.04.–31.08.), ale dnes je 23. 11. 2026 (128 dní před 1. 4.)
        HdoModel outdatedSummer = new HdoModel(1, "TAR", "01.04.", "31.08.", "01:30", "06:00", 1, 1, 1, 1, 1, 1, 1, "EGD");
        List<HdoModel> models = Collections.singletonList(outdatedSummer);

        // Pondělí 23. 11. 2026 22:42
        Calendar now = Calendar.getInstance();
        now.set(2026, Calendar.NOVEMBER, 23, 22, 42, 0);
        now.set(Calendar.MILLISECOND, 0);

        long nextStart = HdoAlarmScheduler.findNextTriggerForModels(models, HdoAlarmScheduler.TYPE_START, now.getTimeInMillis(), 0L);

        // Očekává se úterý 24. 11. 2026 v 01:30 (za ~2 h 48 min), nikoliv za 128 dní
        Calendar expected = Calendar.getInstance();
        expected.set(2026, Calendar.NOVEMBER, 24, 1, 30, 0);
        expected.set(Calendar.MILLISECOND, 0);

        assertEquals(expected.getTimeInMillis(), nextStart);
    }


    @Test
    public void testExpiredYearSpecificHdoUsesWeeklyFallback() {
        // Prošlý HDO kód z předchozího roku (01.09.2025–25.10.2025)
        HdoModel expiredModel = new HdoModel(1, "TAR", "01.09.2025", "25.10.2025", "22:00", "06:00", 1, 1, 1, 1, 1, 1, 1, "CEZ");
        List<HdoModel> models = Collections.singletonList(expiredModel);

        // Pondělí 23. 11. 2026 20:00
        Calendar now = Calendar.getInstance();
        now.set(2026, Calendar.NOVEMBER, 23, 20, 0, 0);
        now.set(Calendar.MILLISECOND, 0);

        long nextStart = HdoAlarmScheduler.findNextTriggerForModels(models, HdoAlarmScheduler.TYPE_START, now.getTimeInMillis(), 0L);

        Calendar expected = Calendar.getInstance();
        expected.set(2026, Calendar.NOVEMBER, 23, 22, 0, 0);
        expected.set(Calendar.MILLISECOND, 0);

        assertEquals(expected.getTimeInMillis(), nextStart);
    }


    @Test
    public void testValidCurrentSeasonPreferredOverFutureSeason() {
        // Tabulka obsahuje jak aktuální zimní rozvrh (22:00), tak budoucí letní rozvrh (20:00)
        HdoModel winterValid = new HdoModel(1, "TAR", "01.11.", "31.03.", "22:00", "06:00", 1, 1, 1, 1, 1, 1, 1, "EGD");
        HdoModel summerFuture = new HdoModel(2, "TAR", "01.04.", "31.10.", "20:00", "04:00", 1, 1, 1, 1, 1, 1, 1, "EGD");
        List<HdoModel> models = new ArrayList<>();
        models.add(winterValid);
        models.add(summerFuture);

        // Pondělí 23. 11. 2026 19:00 -> musí vybrat zimní 22:00, ne letní 20:00
        Calendar now = Calendar.getInstance();
        now.set(2026, Calendar.NOVEMBER, 23, 19, 0, 0);
        now.set(Calendar.MILLISECOND, 0);

        long nextStart = HdoAlarmScheduler.findNextTriggerForModels(models, HdoAlarmScheduler.TYPE_START, now.getTimeInMillis(), 0L);

        Calendar expected = Calendar.getInstance();
        expected.set(2026, Calendar.NOVEMBER, 23, 22, 0, 0);
        expected.set(Calendar.MILLISECOND, 0);

        assertEquals(expected.getTimeInMillis(), nextStart);
    }


    @Test
    public void testOvernightIntervalEndAfterMidnightReturnsSameMorning() {
        HdoModel overnight = new HdoModel(1, "TAR", "01.11.2026", "31.03.2027", "22:00", "06:00", 1, 1, 1, 1, 1, 1, 1, "CEZ");
        List<HdoModel> models = Collections.singletonList(overnight);

        // Úterý 24. 11. 2026 01:30 -> aktuálně probíhá noční interval ze včera, konec musí být dnes v 06:00
        Calendar now = Calendar.getInstance();
        now.set(2026, Calendar.NOVEMBER, 24, 1, 30, 0);
        now.set(Calendar.MILLISECOND, 0);

        long nextEnd = HdoAlarmScheduler.findNextTriggerForModels(models, HdoAlarmScheduler.TYPE_END, now.getTimeInMillis(), 0L);

        Calendar expected = Calendar.getInstance();
        expected.set(2026, Calendar.NOVEMBER, 24, 6, 0, 0);
        expected.set(Calendar.MILLISECOND, 0);

        assertEquals(expected.getTimeInMillis(), nextEnd);
    }


    @Test
    public void testModelWithNoEnabledDaysReturnsInvalid() {
        HdoModel invalid = new HdoModel(1, "TAR", "01.01.2020", "01.01.2020", "01:30", "06:00", 0, 0, 0, 0, 0, 0, 0, "CEZ");
        List<HdoModel> models = Collections.singletonList(invalid);

        Calendar now = Calendar.getInstance();
        now.set(2026, Calendar.NOVEMBER, 23, 12, 0, 0);
        now.set(Calendar.MILLISECOND, 0);

        long nextStart = HdoAlarmScheduler.findNextTriggerForModels(models, HdoAlarmScheduler.TYPE_START, now.getTimeInMillis(), 0L);
        assertEquals(-1L, nextStart);
    }

}
