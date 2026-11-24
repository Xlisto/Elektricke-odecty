package cz.xlisto.elektrodroid;


import android.app.Application;

import com.google.android.material.color.DynamicColors;
import com.google.android.material.color.DynamicColorsOptions;

import cz.xlisto.elektrodroid.shp.ShPSettings;


/**
 * Hlavní třída aplikace zodpovědná za globální inicializaci při startu procesu.
 * <p>
 * Registruje podporu dynamických barev Material You ({@link DynamicColors}) pro všechny aktivity
 * na zařízeních s Androidem 12 a novějším, pokud je volba povolena v {@link ShPSettings}.
 */
public class ElektroDroidApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        DynamicColorsOptions options = new DynamicColorsOptions.Builder()
                .setPrecondition((activity, theme) -> new ShPSettings(activity).isUseDynamicColors())
                .build();
        DynamicColors.applyToActivitiesIfAvailable(this, options);
    }

}
