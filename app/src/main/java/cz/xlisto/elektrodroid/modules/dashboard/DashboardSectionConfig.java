package cz.xlisto.elektrodroid.modules.dashboard;


import androidx.annotation.NonNull;


/**
 * Konfigurace jedné sekce (widgetu) na přehledové obrazovce.
 * Uchovává typ sekce, její viditelnost, pořadí a sloupec v režimu na šířku (landscape).
 */
public class DashboardSectionConfig implements Comparable<DashboardSectionConfig> {

    private final DashboardSectionType type;
    private boolean visible;
    private int order;
    private int landscapeColumn;


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


    public void setVisible(boolean visible) {
        this.visible = visible;
    }


    public int getOrder() {
        return order;
    }


    public void setOrder(int order) {
        this.order = order;
    }


    public int getLandscapeColumn() {
        return landscapeColumn;
    }


    public void setLandscapeColumn(int landscapeColumn) {
        this.landscapeColumn = landscapeColumn;
    }


    @Override
    public int compareTo(@NonNull DashboardSectionConfig other) {
        return Integer.compare(this.order, other.order);
    }

}
