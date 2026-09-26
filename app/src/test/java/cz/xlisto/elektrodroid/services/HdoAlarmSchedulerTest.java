package cz.xlisto.elektrodroid.services;


import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.junit.Test;

import cz.xlisto.elektrodroid.models.HdoModel;


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

}
