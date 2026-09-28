package cz.xlisto.elektrodroid.modules.hdo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;

import org.junit.Test;

import cz.xlisto.elektrodroid.models.HdoModel;

public class HdoGraphicGroupModelTest {

    @Test
    public void testAllDaysModelGroupedAsAllWeek() {
        // TUV model active on all 7 days (PO, ÚT, ST, ČT, PÁ, SO, NE)
        HdoModel tuvModelAllDays = new HdoModel(1, "Relé3-TUV", "01.09.2026", "25.10.2026", "01:00", "05:00", 1, 1, 1, 1, 1, 1, 1, "CEZ");

        ArrayList<HdoModel> items = new ArrayList<>();
        items.add(tuvModelAllDays);

        ArrayList<HdoGraphicGroupModel> groups = HdoGraphicGroupModel.buildGraphicGroups(items);

        assertEquals("Should create 1 group for All Week", 1, groups.size());

        HdoGraphicGroupModel allWeekGroup = groups.get(0);
        assertEquals("Title should be Celý týden", "Celý týden", allWeekGroup.title());
        assertTrue("Group should contain TUV model", allWeekGroup.models().contains(tuvModelAllDays));
    }

    @Test
    public void testExplicitHolidayGroupForPRE() {
        // PRE explicit holiday model
        HdoModel preHolidayModel = new HdoModel(1, "485 - odblokování NT", "29.01.2026", "12.02.2026", "00:00", "02:40", 0, 0, 0, 0, 0, 0, 0, "PRE");
        preHolidayModel.setSv(1);

        ArrayList<HdoModel> items = new ArrayList<>();
        items.add(preHolidayModel);

        ArrayList<HdoGraphicGroupModel> groups = HdoGraphicGroupModel.buildGraphicGroups(items);

        assertEquals("Should create 1 group for Svátek", 1, groups.size());
        assertEquals("Title should be Svátek", "Svátek", groups.get(0).title());
        assertEquals("Group type should be HOLIDAY", HdoGraphicGroupModel.GroupType.HOLIDAY, groups.get(0).groupType());
    }
}
