package cz.xlisto.elektrodroid.modules.hdo;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;

import org.junit.Test;

import cz.xlisto.elektrodroid.models.HdoModel;


public class HdoGraphicGroupModelTest {

    @Test
    public void testAllDaysModelIncludedInBothWeekdayAndWeekendGroups() {
        // TUV model active on all 7 days (PO, ÚT, ST, ČT, PÁ, SO, NE)
        HdoModel tuvModelAllDays = new HdoModel(1, "Relé3-TUV", "01.09.2026", "25.10.2026", "01:00", "05:00", 1, 1, 1, 1, 1, 1, 1, "CEZ");

        ArrayList<HdoModel> items = new ArrayList<>();
        items.add(tuvModelAllDays);

        ArrayList<HdoGraphicGroupModel> groups = HdoGraphicGroupModel.buildGraphicGroups(items);

        assertEquals("Should create 3 groups (Weekdays, Weekend, and Svátek fallback)", 3, groups.size());

        HdoGraphicGroupModel weekdaysGroup = groups.get(0);
        assertEquals("First group should be Weekdays", HdoGraphicGroupModel.GroupType.WEEKDAYS, weekdaysGroup.groupType());
        assertTrue("Weekdays group should contain TUV model", weekdaysGroup.models().contains(tuvModelAllDays));

        HdoGraphicGroupModel weekendGroup = groups.get(1);
        assertEquals("Second group should be Weekend", HdoGraphicGroupModel.GroupType.WEEKEND, weekendGroup.groupType());
        assertTrue("Weekend group should ALSO contain TUV model", weekendGroup.models().contains(tuvModelAllDays));

        HdoGraphicGroupModel holidayGroup = groups.get(2);
        assertEquals("Third group should be Holiday", HdoGraphicGroupModel.GroupType.HOLIDAY, holidayGroup.groupType());
        assertTrue("Holiday group should ALSO contain TUV model", holidayGroup.models().contains(tuvModelAllDays));
    }


    @Test
    public void testHolidayFallbackToSundayScheduleWhenNoExplicitHolidayModel() {
        // EGD / CEZ models without explicit sv=1
        HdoModel egdSundayModel = new HdoModel(1, "Relé-TAR", "01.09.2026", "25.10.2026", "00:00", "09:15", 0, 0, 0, 0, 0, 0, 1, "EGD");

        ArrayList<HdoModel> items = new ArrayList<>();
        items.add(egdSundayModel);

        ArrayList<HdoGraphicGroupModel> groups = HdoGraphicGroupModel.buildGraphicGroups(items);

        boolean hasHolidayGroup = false;
        for (HdoGraphicGroupModel group : groups) {
            if (group.groupType() == HdoGraphicGroupModel.GroupType.HOLIDAY) {
                hasHolidayGroup = true;
                assertTrue("Holiday group should contain Sunday model for EGD/CEZ", group.models().contains(egdSundayModel));
            }
        }
        assertTrue("Should create Svátek group for EGD/CEZ using Sunday schedule", hasHolidayGroup);
    }

}
