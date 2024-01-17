package cz.xlisto.odecty.utils;

import android.app.Activity;

import java.util.Objects;

import cz.xlisto.odecty.MainActivity;
import cz.xlisto.odecty.services.HdoData;


/**
 * Xlisto 17.01.2024 18:50
 */
public class MainActivityHelper {
    private static final String TAG = "MainActivityHelper";


    /**
     * Aktualizuje toolbar a načte data
     */
    public static void updateToolbarAndLoadData(Activity activity) {
        if (activity instanceof MainActivity) {
            MainActivity mainActivity = (MainActivity) activity;
            mainActivity.setToolbarSubtitle(Objects.requireNonNull(SubscriptionPoint.load(activity)).getName());
            HdoData.loadHdoData(activity);
        }
    }
}
