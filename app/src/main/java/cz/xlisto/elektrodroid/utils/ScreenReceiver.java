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

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent != null ? intent.getAction() : null;
        if (Intent.ACTION_SCREEN_OFF.equals(action)) {
            wasScreenOn = false;
        } else if (Intent.ACTION_SCREEN_ON.equals(action)) {
            wasScreenOn = true;
        }

        // POZNÁMKA: Nespouštíme HDO službu z broadcast receiveru,
        // protože Android 36 neumožňuje spustit foreground služby s určitými typy.
        // Služba se spustí pouze když je aplikace otevřená.
        // Intent i = new Intent(context, HdoService.class);
        // i.putExtra("screen_state", wasScreenOn);
        // i.putExtra("should_be_foreground", false);
        // context.startService(i);
    }
}
