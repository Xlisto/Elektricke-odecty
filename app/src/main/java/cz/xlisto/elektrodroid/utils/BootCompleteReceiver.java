package cz.xlisto.elektrodroid.utils;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import cz.xlisto.elektrodroid.modules.backup.PendingBackupUploadScheduler;
import cz.xlisto.elektrodroid.services.HdoAlarmScheduler;


/**
 * Posluchač restartu zařízení a aktualizace aplikace.
 * Naplánuje odeslání čekajících záloh na Google Drive, pokud fronta není prázdná.
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

        if (Intent.ACTION_BOOT_COMPLETED.equals(action)
                || Intent.ACTION_MY_PACKAGE_REPLACED.equals(action)) {
            HdoAlarmScheduler.rescheduleAll(context);
        }

        // --- Čekající zálohy na Google Drive ---
        // Naplánuje upload přes WorkManager; worker počká na připojení k síti
        PendingBackupUploadScheduler.scheduleIfNeeded(context, false);
    }

}
