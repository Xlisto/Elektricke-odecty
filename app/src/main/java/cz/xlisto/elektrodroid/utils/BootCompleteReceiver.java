package cz.xlisto.elektrodroid.utils;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import cz.xlisto.elektrodroid.modules.backup.PendingBackupUploadScheduler;
import cz.xlisto.elektrodroid.services.HdoNotice;
import cz.xlisto.elektrodroid.shp.ShPHdo;


/**
 * Posluchač restartu zařízení a aktualizace aplikace.
 * Po restartu zobrazí notifikaci s výzvou ke spuštění HDO služby.
 * Zároveň naplánuje odeslání čekajících záloh na Google Drive, pokud fronta není prázdná.
 * Xlisto 13.06.2023 9:21
 */
public class BootCompleteReceiver extends BroadcastReceiver {

    /**
     * Metoda volaná při přijetí broadcastu (restart zařízení nebo aktualizace aplikace).
     *
     * @param context Kontext aplikace
     * @param intent  Přijatý intent
     */
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent == null ? null : intent.getAction();
        if (Intent.ACTION_BOOT_COMPLETED.equals(action)) {
            ShPHdo shPHdo = new ShPHdo(context);
            if (shPHdo.get(ShPHdo.ARG_RUNNING_SERVICE, false)) {
                HdoNotice.showRestartNotice(context);
            }
        }

        // --- Čekající zálohy na Google Drive ---
        // Naplánuje upload přes WorkManager; worker počká na připojení k síti
        PendingBackupUploadScheduler.scheduleIfNeeded(context, false);
    }

}
