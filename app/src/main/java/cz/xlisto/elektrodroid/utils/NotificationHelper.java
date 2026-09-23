package cz.xlisto.elektrodroid.utils;


import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.view.View;

import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.snackbar.Snackbar;

import cz.xlisto.elektrodroid.R;


public class NotificationHelper {

    /**
     * Vrátí true, pokud má aplikace povolené notifikace.
     */
    public static boolean isNotificationPermissionGranted(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                    == PackageManager.PERMISSION_GRANTED;
        }
        return NotificationManagerCompat.from(context).areNotificationsEnabled();
    }


    /**
     * Zobrazí Snackbar s varováním, že notifikace jsou vypnuté, a s tlačítkem pro jejich povolení.
     *
     * @param view    root view fragmentu/aktivity
     * @param message zpráva, která se zobrazí v upozornění
     */
    public static void showNotificationWarningSnackbar(View view, String message) {
        if (view == null) return;
        Snackbar.make(view, message, Snackbar.LENGTH_LONG)
                .setAction(R.string.enable, v -> openNotificationSettings(view.getContext()))
                .show();
    }


    /**
     * Otevře systémové nastavení notifikací pro aplikaci.
     */
    public static void openNotificationSettings(Context context) {
        Intent intent;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            intent = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                    .putExtra(Settings.EXTRA_APP_PACKAGE, context.getPackageName());
        } else {
            intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    .setData(Uri.fromParts("package", context.getPackageName(), null));
        }
        context.startActivity(intent);
    }

}
