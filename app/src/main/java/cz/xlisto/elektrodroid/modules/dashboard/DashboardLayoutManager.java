package cz.xlisto.elektrodroid.modules.dashboard;


import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import cz.xlisto.elektrodroid.shp.ShPDashBoard;


/**
 * Správce rozložení sekcí (widgetů) na hlavní přehledové obrazovce.
 * Umožňuje načítat, ukládat a aplikovat pořadí, viditelnost a rozdělení do sloupců (v landscape režimu),
 * což připravuje architekturu pro budoucí uživatelské přizpůsobení rozložení hlavní obrazovky.
 */
public class DashboardLayoutManager {

    private final ShPDashBoard shPDashBoard;


    public DashboardLayoutManager(@NonNull Context context) {
        this.shPDashBoard = new ShPDashBoard(context);
    }


    /**
     * Načte konfiguraci sekcí z SharedPreferences nebo vrátí výchozí rozložení.
     */
    @NonNull
    public List<DashboardSectionConfig> loadConfig() {
        String raw = shPDashBoard.get(ShPDashBoard.DASHBOARD_LAYOUT_CONFIG, "");
        Map<DashboardSectionType, DashboardSectionConfig> map = new EnumMap<>(DashboardSectionType.class);

        if (raw != null && !raw.trim().isEmpty()) {
            String[] entries = raw.split(";");
            for (String entry : entries) {
                String[] parts = entry.split(":");
                if (parts.length >= 4) {
                    DashboardSectionType type = DashboardSectionType.fromId(parts[0]);
                    if (type != null) {
                        boolean visible = "1".equals(parts[1]) || Boolean.parseBoolean(parts[1]);
                        int order = parseIntSafe(parts[2], type.getDefaultOrder());
                        int col = parseIntSafe(parts[3], type.getDefaultLandscapeColumn());
                        map.put(type, new DashboardSectionConfig(type, visible, order, col));
                    }
                }
            }
        }

        for (DashboardSectionType type : DashboardSectionType.values()) {
            if (!map.containsKey(type)) {
                map.put(type, new DashboardSectionConfig(
                        type,
                        true,
                        type.getDefaultOrder(),
                        type.getDefaultLandscapeColumn()
                ));
            }
        }

        List<DashboardSectionConfig> result = new ArrayList<>(map.values());
        Collections.sort(result);
        return result;
    }


    /**
     * Aplikuje pořadí a viditelnost sekcí do kontejneru (jednoho sloupce v portrait nebo dvou sloupců v landscape).
     */
    public void applyLayout(@NonNull Map<DashboardSectionType, View> sectionViews,
                            @Nullable ViewGroup portraitContainer,
                            @Nullable ViewGroup landscapeLeftContainer,
                            @Nullable ViewGroup landscapeRightContainer) {
        List<DashboardSectionConfig> configs = loadConfig();

        for (View view : sectionViews.values()) {
            if (view != null && view.getParent() instanceof ViewGroup) {
                ((ViewGroup) view.getParent()).removeView(view);
            }
        }

        boolean isLandscape = landscapeLeftContainer != null && landscapeRightContainer != null;

        for (DashboardSectionConfig config : configs) {
            View sectionView = sectionViews.get(config.getType());
            if (sectionView == null) {
                continue;
            }
            sectionView.setVisibility(config.isVisible() ? View.VISIBLE : View.GONE);
            if (isLandscape) {
                if (config.getLandscapeColumn() == 1) {
                    landscapeRightContainer.addView(sectionView);
                } else {
                    landscapeLeftContainer.addView(sectionView);
                }
            } else if (portraitContainer != null) {
                portraitContainer.addView(sectionView);
            }
        }
    }


    private int parseIntSafe(String value, int fallback) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

}
