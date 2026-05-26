package cz.xlisto.elektrodroid.utils;


import android.app.Activity;

import java.util.Objects;

import cz.xlisto.elektrodroid.MainActivity;


/**
 * Pomocné utility pro práci s hlavní aktivitou aplikace.
 * <p>
 * Obsahuje metody pro bezpečnou aktualizaci UI prvků závislých
 * na aktuálně vybraném odběrném místě.
 */
public class MainActivityHelper {

    /**
     * Aktualizuje podtitulek toolbaru podle aktivního odběrného místa.
     *
     * @param activity aktuální aktivita
     */
    public static void updateToolbarAndLoadData(Activity activity) {
        if (activity instanceof MainActivity mainActivity) {
            mainActivity.setToolbarSubtitle(Objects.requireNonNull(SubscriptionPoint.load(activity)).getName());
        }
    }

}
