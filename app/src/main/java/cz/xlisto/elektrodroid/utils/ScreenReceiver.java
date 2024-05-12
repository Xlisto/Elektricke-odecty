package cz.xlisto.elektrodroid.utils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import cz.xlisto.elektrodroid.services.HdoService;

/**
 * Posluchač zapnutí/vypnutí obrazovky obrazovky
 * Xlisto 07.06.2023 11:07
 */
public class ScreenReceiver extends BroadcastReceiver {
    private static final String TAG = "ScreenReceiver";
    public static boolean wasScreenOn = true;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
            wasScreenOn = false;
        } else if (intent.getAction().equals(Intent.ACTION_SCREEN_ON)) {
            wasScreenOn = true;
        }

        Intent i = new Intent(context, HdoService.class);
        i.putExtra("screen_state", wasScreenOn);
        context.startService(i);
    }
}
