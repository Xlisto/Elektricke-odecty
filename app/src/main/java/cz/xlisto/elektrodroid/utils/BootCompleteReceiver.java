package cz.xlisto.elektrodroid.utils;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import cz.xlisto.elektrodroid.modules.backup.PendingBackupUploadScheduler;
import cz.xlisto.elektrodroid.services.HdoData;
import cz.xlisto.elektrodroid.services.HdoService;
import cz.xlisto.elektrodroid.shp.ShPHdo;


/**
 * Posluchač restartu zařízení a aktualizace aplikace.
 * Pokud je HDO služba povolena uživatelem, spustí ji po rebootu a načte data k aktuálnímu odběrnému místu.
 * Zároveň naplánuje odeslání čekajících záloh na Google Drive, pokud fronta není prázdná.
 * Xlisto 13.06.2023 9:21
 */
public class BootCompleteReceiver extends BroadcastReceiver {

    /**
     * Metoda volaná při přijetí broadcastu (restart zařízení nebo aktualizace aplikace).
     * <p>
     * BOOT_COMPLETED je explicitně vyjmuto z omezení spouštění foreground služeb na pozadí
     * (viz Android dokumentace Background execution limits), takže {@code startForegroundService}
     * lze použít na API 26+ bez omezení.
     *
     * @param context Kontext aplikace
     * @param intent  Přijatý intent
     */
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent == null ? null : intent.getAction();
        if (Intent.ACTION_BOOT_COMPLETED.equals(action)
                || Intent.ACTION_MY_PACKAGE_REPLACED.equals(action)) {

            // --- HDO sledovací služba ---
            ShPHdo shPHdo = new ShPHdo(context);
            if (shPHdo.get(ShPHdo.ARG_RUNNING_SERVICE, false)) {
                HdoData.loadHdoData(context);
                Intent i = new Intent(context, HdoService.class);
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                    // Android 7.1 a nižší
                    context.startService(i);
                } else {
                    // Android 8+ – BOOT_COMPLETED / MY_PACKAGE_REPLACED jsou exempt,
                    // startForegroundService je povoleno i na Android 12+
                    context.startForegroundService(i);
                }
            }

            // --- Čekající zálohy na Google Drive ---
            // Naplánuje upload přes WorkManager; worker počká na připojení k síti
            PendingBackupUploadScheduler.scheduleIfNeeded(context, false);
        }
    }

}
