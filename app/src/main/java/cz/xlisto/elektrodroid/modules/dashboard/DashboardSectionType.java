package cz.xlisto.elektrodroid.modules.dashboard;


/**
 * Výčet sekcí (widgetů) na nové přehledové obrazovce.
 * Slouží pro modulární skládání obrazovky a budoucí uživatelské přizpůsobení pořadí a viditelnosti.
 */
public enum DashboardSectionType {
    CURRENT_PRICE("current_price", 0, 0),
    QUICK_ACTIONS("quick_actions", 1, 0),
    PERIOD_CONSUMPTION("period_consumption", 2, 0),
    CONSUMPTION_OVERVIEW("consumption_overview", 3, 1),
    BILLING_AND_INVOICES("billing_and_invoices", 4, 1);

    private final String id;
    private final int defaultOrder;
    private final int defaultLandscapeColumn;


    DashboardSectionType(String id, int defaultOrder, int defaultLandscapeColumn) {
        this.id = id;
        this.defaultOrder = defaultOrder;
        this.defaultLandscapeColumn = defaultLandscapeColumn;
    }


    public String getId() {
        return id;
    }


    public int getDefaultOrder() {
        return defaultOrder;
    }


    public int getDefaultLandscapeColumn() {
        return defaultLandscapeColumn;
    }


    public static DashboardSectionType fromId(String id) {
        for (DashboardSectionType type : values()) {
            if (type.id.equals(id)) {
                return type;
            }
        }
        return null;
    }
}
