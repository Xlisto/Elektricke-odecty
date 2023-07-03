package cz.xlisto.odecty.dialogs;

import android.content.Context;
import android.content.DialogInterface;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.FragmentActivity;

/**
 * Xlisto 25.06.2023 9:41
 */
public class OwnAlertDialog {
    private static final String TAG = "AlertDialog";

    public static void show(Context context, String title, String message) {
        ((FragmentActivity)context).runOnUiThread(new Runnable() {
            public void run() {
                new AlertDialog.Builder(context)
                        .setTitle(title)
                        .setMessage(message)

                        // Specifying a listener allows you to take an action before dismissing the dialog.
                        // The dialog is automatically dismissed when a dialog button is clicked.
                        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                // Continue with delete operation
                            }
                        })

                        // A null listener allows the button to dismiss the dialog and take no further action.

                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .create()
                        .show();
            }
        });

    }
}
