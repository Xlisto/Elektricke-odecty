package cz.xlisto.elektrodroid.modules.hdo;


import java.util.ArrayList;

import cz.xlisto.elektrodroid.models.HdoModel;


/**
 * Model reprezentující skupinu HDO časů pro grafické zobrazení (např. Všední dny, Víkend, Svátek).
 */
public record HdoGraphicGroupModel(GroupType groupType, String title, ArrayList<HdoModel> models,
                                   boolean mon, boolean tue, boolean wed, boolean thu, boolean fri,
                                   boolean sat, boolean sun, boolean sv) {

    public enum GroupType {
        WEEKDAYS,
        WEEKEND,
        HOLIDAY,
        OTHER
    }


    /**
     * Shlukne seznam HDO časů do skupin podle dní (Všední dny, Víkend, Svátek)
     */
    public static ArrayList<HdoGraphicGroupModel> buildGraphicGroups(ArrayList<HdoModel> items) {
        ArrayList<HdoGraphicGroupModel> groups = new ArrayList<>();
        if (items == null || items.isEmpty()) return groups;

        ArrayList<HdoModel> weekdayModels = new ArrayList<>();
        ArrayList<HdoModel> weekendModels = new ArrayList<>();
        ArrayList<HdoModel> holidayModels = new ArrayList<>();
        ArrayList<HdoModel> otherModels = new ArrayList<>();

        boolean mon = false, tue = false, wed = false, thu = false, fri = false, sat = false, sun = false;

        for (HdoModel item : items) {
            boolean isHoliday = item.getSv() == 1 || (item.getDateFrom() != null && item.getDateFrom().equalsIgnoreCase("SVÁTEK"));
            boolean isWeekday = item.getMon() == 1 || item.getTue() == 1 || item.getWed() == 1 || item.getThu() == 1 || item.getFri() == 1;
            boolean isWeekend = item.getSat() == 1 || item.getSun() == 1;

            if (isHoliday) {
                holidayModels.add(item);
            }
            if (isWeekday) {
                weekdayModels.add(item);
                if (item.getMon() == 1) mon = true;
                if (item.getTue() == 1) tue = true;
                if (item.getWed() == 1) wed = true;
                if (item.getThu() == 1) thu = true;
                if (item.getFri() == 1) fri = true;
            }
            if (isWeekend) {
                weekendModels.add(item);
                if (item.getSat() == 1) sat = true;
                if (item.getSun() == 1) sun = true;
            }
            if (!isHoliday && !isWeekday && !isWeekend) {
                otherModels.add(item);
            }
        }

        // Pokud pro svátek neexistuje explicitní model (např. u EGD / CEZ), použijí se nedělní modely (sun == 1)
        if (holidayModels.isEmpty()) {
            for (HdoModel item : items) {
                if (item.getSun() == 1) {
                    holidayModels.add(item);
                }
            }
        }

        if (!weekdayModels.isEmpty()) {
            groups.add(new HdoGraphicGroupModel(
                    GroupType.WEEKDAYS,
                    "Všední dny",
                    weekdayModels,
                    mon, tue, wed, thu, fri, false, false, false
            ));
        }

        if (!weekendModels.isEmpty()) {
            groups.add(new HdoGraphicGroupModel(
                    GroupType.WEEKEND,
                    "Víkend",
                    weekendModels,
                    false, false, false, false, false, sat, sun, false
            ));
        }

        if (!holidayModels.isEmpty()) {
            groups.add(new HdoGraphicGroupModel(
                    GroupType.HOLIDAY,
                    "Svátek",
                    holidayModels,
                    false, false, false, false, false, false, false, true
            ));
        }

        if (!otherModels.isEmpty()) {
            groups.add(new HdoGraphicGroupModel(
                    GroupType.OTHER,
                    "Ostatní",
                    otherModels,
                    false, false, false, false, false, false, false, false
            ));
        }

        return groups;
    }

}
