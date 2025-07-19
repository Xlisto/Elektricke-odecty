package cz.xlisto.elektrodroid.utils;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import cz.xlisto.elektrodroid.services.HdoData;
import cz.xlisto.elektrodroid.services.HdoService;
import cz.xlisto.elektrodroid.shp.ShPHdo;


/**
 * Posluchač restartu zařízení, pokud je služba povolena uživatelem, spustí ji po rebootu a načte data k atuálnímu odběrnému místu
 * Xlisto 13.06.2023 9:21
 */
public class BootCompleteReceiver extends BroadcastReceiver {

    private static final String TAG = "BootCompleteReceiver";


    /**
     * Metoda volaná při přijetí broadcastu (např. po restartu zařízení).
     * Pokud je služba povolena uživatelem, spustí ji a načte data k aktuálnímu odběrnému místu.
     *
     * @param context Kontext aplikace
     * @param intent  Přijatý intent (např. ACTION_BOOT_COMPLETED)
     */
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            ShPHdo shPHdo = new ShPHdo(context);
            if (shPHdo.get(ShPHdo.ARG_RUNNING_SERVICE, false)) {
                HdoData.loadHdoData(context);
                Intent i = new Intent(context, HdoService.class);
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.N_MR1) {
                    // Android 7.1 a nižší
                    context.startService(i);
                } else if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.R) {
                    // Android 8 až 11
                    context.startForegroundService(i);
                } else {
                    // Android 12 a vyšší - zde použij WorkManager nebo jiný mechanismus
                    OneTimeWorkRequest workRequest = new OneTimeWorkRequest.Builder(HdoWorker.class).build();
                    WorkManager.getInstance(context).enqueue(workRequest);
                }
            }
        }

    }

}
