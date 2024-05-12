package cz.xlisto.elektrodroid.dialogs;

import android.content.Context;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.FragmentActivity;

/**
 * Xlisto 25.06.2023 9:41
 */
public class OwnAlertDialog {
    private static final String TAG = "AlertDialog";

    public static void show(Context context, String title, String message) {
        ((FragmentActivity)context).runOnUiThread(() -> new AlertDialog.Builder(context)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(android.R.string.yes, (dialog, which) -> {

                })
                .setIcon(android.R.drawable.ic_dialog_alert)
                .create()
                .show());
    }
}
