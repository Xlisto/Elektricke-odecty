package cz.xlisto.elektrodroid.modules.dashboard;


import androidx.annotation.StringRes;

import cz.xlisto.elektrodroid.R;


/**
 * Výčet sekcí (widgetů) na nové přehledové obrazovce.
 * Slouží pro modulární skládání obrazovky a budoucí uživatelské přizpůsobení pořadí a viditelnosti.
 */
public enum DashboardSectionType {
    CURRENT_PRICE("current_price", R.string.dashboard_current_price_title, 0, 0),
    QUICK_ACTIONS("quick_actions", R.string.dashboard_quick_monthly_readings, 1, 0),
    PERIOD_CONSUMPTION("period_consumption", R.string.dashboard_period_consumption_title, 2, 0),
    CONSUMPTION_OVERVIEW("consumption_overview", R.string.dashboard_consumption_overview_title, 3, 1),
    BILLING_AND_INVOICES("billing_and_invoices", R.string.dashboard_billing_title, 4, 1);

    private final String id;
    @StringRes
    private final int titleRes;
    private final int defaultOrder;
    private final int defaultLandscapeColumn;


    DashboardSectionType(String id, @StringRes int titleRes, int defaultOrder, int defaultLandscapeColumn) {
        this.id = id;
        this.titleRes = titleRes;
        this.defaultOrder = defaultOrder;
        this.defaultLandscapeColumn = defaultLandscapeColumn;
    }


    public String getId() {
        return id;
    }


    @StringRes
    public int getTitleRes() {
        return titleRes;
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
