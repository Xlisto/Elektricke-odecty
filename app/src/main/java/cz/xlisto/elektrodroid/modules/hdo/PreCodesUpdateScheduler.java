package cz.xlisto.elektrodroid.modules.hdo;


import android.content.Context;

import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import java.util.concurrent.TimeUnit;


/**
 * Třída pro plánování periodické aktualizace kódů povelů PRE (1x týdně na Wi-Fi).
 */
public class PreCodesUpdateScheduler {

    public static final String UNIQUE_WORK_NAME_PRE_CODES_UPDATE = "PreCodesUpdateWork";


    private PreCodesUpdateScheduler() {
    }


    /**
     * Naplánuje periodickou úlohu aktualizace kódů PRE (jednou za 7 dní, pouze na unmetered/Wi-Fi síti).
     *
     * @param context kontext aplikace
     */
    public static void schedulePreCodesUpdate(Context context) {
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.UNMETERED)
                .build();

        PeriodicWorkRequest workRequest = new PeriodicWorkRequest.Builder(
                PreCodesUpdateWorker.class,
                7, TimeUnit.DAYS
        )
                .setConstraints(constraints)
                .build();

        WorkManager.getInstance(context.getApplicationContext()).enqueueUniquePeriodicWork(
                UNIQUE_WORK_NAME_PRE_CODES_UPDATE,
                ExistingPeriodicWorkPolicy.KEEP,
                workRequest
        );
    }

}
