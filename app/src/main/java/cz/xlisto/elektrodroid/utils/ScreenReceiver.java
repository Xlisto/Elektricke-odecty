package cz.xlisto.elektrodroid.utils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Posluchač zapnutí/vypnutí obrazovky obrazovky
 * Xlisto 07.06.2023 11:07
 */
public class ScreenReceiver extends BroadcastReceiver {
    public static boolean wasScreenOn = true;

    /**
     * Přijatá zpráva o změně stavu obrazovky.
     * <p>
     * Aktualizuje globální příznak `wasScreenOn` na základě přijaté akce.
     * Podporuje akce {@link Intent#ACTION_SCREEN_OFF} a {@link Intent#ACTION_SCREEN_ON}.
     * <p>
     * Poznámka: HDO služba se nespouští z tohoto broadcast receiveru,
     * protože Android 36 neumožňuje spouštění foreground služeb v těchto případech.
     * Služba se spustí pouze, když je aplikace otevřená.
     *
     * @param context kontext aplikace
     * @param intent  přijatý intent obsahující akci změny stavu obrazovky
     */
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent != null ? intent.getAction() : null;
        if (Intent.ACTION_SCREEN_OFF.equals(action)) {
            wasScreenOn = false;
        } else if (Intent.ACTION_SCREEN_ON.equals(action)) {
            wasScreenOn = true;
        }
    }
}
