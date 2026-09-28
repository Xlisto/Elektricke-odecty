package cz.xlisto.elektrodroid.modules.hdo;


import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import cz.xlisto.elektrodroid.models.HdoModel;

/**
 * Model reprezentující skupinu HDO časů pro grafické zobrazení (např. Všední dny, Víkend, Svátek).
 * Xlisto 30.01.2026
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
     * Sestaví český název skupiny podle aktivních dní
     */
    public static String buildGroupTitle(boolean mon, boolean tue, boolean wed, boolean thu, boolean fri, boolean sat, boolean sun, boolean sv) {
        if (sv) {
            return "Svátek";
        }
        if (mon && tue && wed && thu && fri && sat && sun) {
            return "Celý týden";
        }
        if (mon && tue && wed && thu && fri && !sat && !sun) {
            return "Všední dny";
        }
        if (!mon && !tue && !wed && !thu && !fri && sat && sun) {
            return "Víkend";
        }
        if (mon && tue && wed && thu && !fri && !sat && !sun) {
            return "Pondělí - Čtvrtek";
        }

        ArrayList<String> dayNames = new ArrayList<>();
        if (mon) dayNames.add("Po");
        if (tue) dayNames.add("Út");
        if (wed) dayNames.add("St");
        if (thu) dayNames.add("Čt");
        if (fri) dayNames.add("Pá");
        if (sat) dayNames.add("So");
        if (sun) dayNames.add("Ne");

        if (dayNames.size() == 1) {
            return switch (dayNames.get(0)) {
                case "Po" -> "Pondělí";
                case "Út" -> "Úterý";
                case "St" -> "Středa";
                case "Čt" -> "Čtvrtek";
                case "Pá" -> "Pátek";
                case "So" -> "Sobota";
                case "Ne" -> "Neděle";
                default -> dayNames.get(0);
            };
        }

        return String.join(", ", dayNames);
    }

    /**
     * Shlukne seznam HDO časů do skupin podle jejich reálného denního rozvrhu bez duplicitních zmatených karet.
     */
    public static ArrayList<HdoGraphicGroupModel> buildGraphicGroups(ArrayList<HdoModel> items) {
        ArrayList<HdoGraphicGroupModel> groups = new ArrayList<>();
        if (items == null || items.isEmpty()) return groups;

        ArrayList<HdoModel> holidayModels = new ArrayList<>();
        ArrayList<HdoModel> nonHolidayModels = new ArrayList<>();

        for (HdoModel item : items) {
            if (item.getSv() == 1 || (item.getDateFrom() != null && item.getDateFrom().equalsIgnoreCase("SVÁTEK"))) {
                holidayModels.add(item);
            } else {
                nonHolidayModels.add(item);
            }
        }

        // Zjištění uníkátních denních vzorů pro ne-sváteční modely
        Set<String> distinctPatterns = getStrings(nonHolidayModels);

        List<String> refinedPatterns = refinePatterns(distinctPatterns);

        // Pro každý vyladěný denní vzor shromáždíme všechny modely platné pro tyto dny
        for (String pattern : refinedPatterns) {
            String[] parts = pattern.split(",");
            boolean mon = "1".equals(parts[0]);
            boolean tue = "1".equals(parts[1]);
            boolean wed = "1".equals(parts[2]);
            boolean thu = "1".equals(parts[3]);
            boolean fri = "1".equals(parts[4]);
            boolean sat = "1".equals(parts[5]);
            boolean sun = "1".equals(parts[6]);

            ArrayList<HdoModel> groupModels = new ArrayList<>();
            for (HdoModel item : nonHolidayModels) {
                boolean matches = (mon && item.getMon() == 1) ||
                        (tue && item.getTue() == 1) ||
                        (wed && item.getWed() == 1) ||
                        (thu && item.getThu() == 1) ||
                        (fri && item.getFri() == 1) ||
                        (sat && item.getSat() == 1) ||
                        (sun && item.getSun() == 1);
                if (matches && !containsModel(groupModels, item)) {
                    groupModels.add(item);
                }
            }

            if (!groupModels.isEmpty()) {
                String title = buildGroupTitle(mon, tue, wed, thu, fri, sat, sun, false);
                GroupType groupType = (sat || sun) ? GroupType.WEEKEND : GroupType.WEEKDAYS;

                groups.add(new HdoGraphicGroupModel(
                        groupType,
                        title,
                        groupModels,
                        mon, tue, wed, thu, fri, sat, sun, false
                ));
            }
        }

        // Přidání skupiny pro Svátek
        if (!holidayModels.isEmpty()) {
            groups.add(new HdoGraphicGroupModel(
                    GroupType.HOLIDAY,
                    "Svátek",
                    holidayModels,
                    false, false, false, false, false, false, false, true
            ));
        }

        return groups;
    }


    @NonNull
    private static Set<String> getStrings(ArrayList<HdoModel> nonHolidayModels) {
        Set<String> distinctPatterns = new LinkedHashSet<>();
        for (HdoModel item : nonHolidayModels) {
            boolean mon = item.getMon() == 1;
            boolean tue = item.getTue() == 1;
            boolean wed = item.getWed() == 1;
            boolean thu = item.getThu() == 1;
            boolean fri = item.getFri() == 1;
            boolean sat = item.getSat() == 1;
            boolean sun = item.getSun() == 1;

            String pattern = (mon ? "1" : "0") + "," + (tue ? "1" : "0") + "," + (wed ? "1" : "0") + "," +
                    (thu ? "1" : "0") + "," + (fri ? "1" : "0") + "," + (sat ? "1" : "0") + "," + (sun ? "1" : "0");
            distinctPatterns.add(pattern);
        }
        return distinctPatterns;
    }


    /**
     * Odstraní obecnější vzory (např. Po-Pá), pokud v seznamu existují konkrétnější pod-vzory (např. Po-Čt a Pá).
     */
    private static List<String> refinePatterns(Set<String> patterns) {
        List<String> list = new ArrayList<>(patterns);
        List<String> result = new ArrayList<>();

        for (String p : list) {
            String[] parts = p.split(",");
            boolean mon = "1".equals(parts[0]);
            boolean tue = "1".equals(parts[1]);
            boolean wed = "1".equals(parts[2]);
            boolean thu = "1".equals(parts[3]);
            boolean fri = "1".equals(parts[4]);
            boolean sat = "1".equals(parts[5]);
            boolean sun = "1".equals(parts[6]);

            boolean hasStrictSubPattern = false;
            for (String other : list) {
                if (other.equals(p)) continue;
                String[] oParts = other.split(",");
                boolean oMon = "1".equals(oParts[0]);
                boolean oTue = "1".equals(oParts[1]);
                boolean oWed = "1".equals(oParts[2]);
                boolean oThu = "1".equals(oParts[3]);
                boolean oFri = "1".equals(oParts[4]);
                boolean oSat = "1".equals(oParts[5]);
                boolean oSun = "1".equals(oParts[6]);

                boolean isSubset = (!oMon || mon) && (!oTue || tue) && (!oWed || wed) &&
                        (!oThu || thu) && (!oFri || fri) && (!oSat || sat) && (!oSun || sun);
                if (isSubset) {
                    hasStrictSubPattern = true;
                    break;
                }
            }

            if (!hasStrictSubPattern) {
                result.add(p);
            }
        }

        return result.isEmpty() ? new ArrayList<>(patterns) : result;
    }


    private static boolean containsModel(ArrayList<HdoModel> list, HdoModel model) {
        if (list == null || model == null) return false;
        for (HdoModel m : list) {
            if (m == model) return true;
            if (m.getId() > 0 && m.getId() == model.getId()) return true;
            if (m.equals(model)) return true;
        }
        return false;
    }
}
