package cz.xlisto.elektrodroid;


import android.app.Application;
import android.content.res.Configuration;

import androidx.annotation.NonNull;

import com.google.android.material.color.DynamicColors;
import com.google.android.material.color.DynamicColorsOptions;

import cz.xlisto.elektrodroid.modules.widgets.WidgetUpdateManager;
import cz.xlisto.elektrodroid.shp.ShPFilter;
import cz.xlisto.elektrodroid.shp.ShPSettings;


/**
 * Hlavní třída aplikace zodpovědná za globální inicializaci při startu procesu.
 * <p>
 * Registruje podporu dynamických barev Material You ({@link DynamicColors}) pro všechny aktivity
 * na zařízeních s Androidem 12 a novějším, pokud je volba povolena v {@link ShPSettings},
 * a při změně systémové konfigurace (tmavý/světlý režim, tapeta) obnoví bitmapové prvky widgetů.
 */
public class ElektroDroidApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        ShPFilter.migrateLegacyFilters(this);
        DynamicColorsOptions options = new DynamicColorsOptions.Builder()
                .setPrecondition((activity, theme) -> new ShPSettings(activity).isUseDynamicColors())
                .build();
        DynamicColors.applyToActivitiesIfAvailable(this, options);
    }


    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        WidgetUpdateManager.notifyThemeChanged(this);
    }

}
