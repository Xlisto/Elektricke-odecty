package cz.xlisto.elektrodroid.modules.dashboard;


import androidx.annotation.NonNull;


/**
 * Konfigurace jedné sekce (widgetu) na přehledové obrazovce.
 * Uchovává typ sekce, její viditelnost, pořadí a sloupec v režimu na šířku (landscape).
 */
public class DashboardSectionConfig implements Comparable<DashboardSectionConfig> {

    private final DashboardSectionType type;
    private final boolean visible;
    private final int order;
    private final int landscapeColumn;


    public DashboardSectionConfig(@NonNull DashboardSectionType type, boolean visible, int order, int landscapeColumn) {
        this.type = type;
        this.visible = visible;
        this.order = order;
        this.landscapeColumn = landscapeColumn;
    }


    @NonNull
    public DashboardSectionType getType() {
        return type;
    }


    public boolean isVisible() {
        return visible;
    }


    public int getLandscapeColumn() {
        return landscapeColumn;
    }


    @Override
    public int compareTo(@NonNull DashboardSectionConfig other) {
        return Integer.compare(this.order, other.order);
    }

}
