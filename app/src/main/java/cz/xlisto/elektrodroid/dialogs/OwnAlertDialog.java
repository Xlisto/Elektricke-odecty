package cz.xlisto.elektrodroid.dialogs;


import android.content.Context;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.FragmentActivity;

import cz.xlisto.elektrodroid.R;


/**
 * Třída OwnAlertDialog slouží k zobrazení vlastního dialogového okna s možností zachycení události zavření dialogu.
 * Xlisto 25.06.2023 9:41
 */
public class OwnAlertDialog {

    private static final String TAG = "AlertDialog";


    /**
     * Zobrazí dialogové okno s daným titulem a zprávou.
     *
     * @param context Kontext, ve kterém se dialog zobrazí.
     * @param title   Titul dialogového okna.
     * @param message Zpráva dialogového okna.
     */
    public static void show(Context context, String title, String message) {
        show(context, title, message, null);
    }


    /**
     * Zobrazí dialogové okno s daným titulem, zprávou a listenerem pro zachycení události zavření dialogu.
     *
     * @param context         Kontext, ve kterém se dialog zobrazí.
     * @param title           Titul dialogového okna.
     * @param message         Zpráva dialogového okna.
     * @param dismissListener Listener pro zachycení události zavření dialogu.
     */
    public static void show(Context context, String title, String message, OnDialogDismissListener dismissListener) {
        ((FragmentActivity) context).runOnUiThread(() -> {
            AlertDialog dialog = new AlertDialog.Builder(context, R.style.DialogTheme)
                    .setTitle(title)
                    .setMessage(message)
                    .setPositiveButton(android.R.string.yes, (dialogInterface, which) -> {
                        // Positive button action
                    })
                    .setIcon(R.drawable.ic_warning_png)
                    .create();

            dialog.setOnDismissListener(dialogInterface -> {
                if (dismissListener != null) {
                    dismissListener.onDialogDismissed();
                }
            });

            dialog.show();
        });
    }


    /**
     * Rozhraní pro zachycení události zavření dialogu.
     */
    public interface OnDialogDismissListener {

        /**
         * Metoda, která se zavolá při zavření dialogu.
         */
        void onDialogDismissed();

    }

}
