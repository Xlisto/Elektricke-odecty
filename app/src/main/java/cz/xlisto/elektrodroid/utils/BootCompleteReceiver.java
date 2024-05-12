package cz.xlisto.elektrodroid.utils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import cz.xlisto.elektrodroid.services.HdoData;
import cz.xlisto.elektrodroid.services.HdoService;
import cz.xlisto.elektrodroid.shp.ShPHdo;


/**
 * Posluchač restartu zařízení, pokud je služba povolena uživatelem, spustí ji po rebootu a načte data k atuálnímu odběrnému místu
 * Xlisto 13.06.2023 9:21
 */
public class BootCompleteReceiver extends BroadcastReceiver {
    private static final String TAG = "BootCompleteReceiver";


    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            ShPHdo shPHdo = new ShPHdo(context);
            if (shPHdo.get(ShPHdo.ARG_RUNNING_SERVICE, false)) {
                HdoData.loadHdoData(context);
                Intent i = new Intent(context, HdoService.class);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(i);
                } else {
                    context.startService(i);
                }
            }
        }

    }
}
