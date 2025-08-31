package cz.xlisto.elektrodroid.dialogs;


import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentActivity;

import cz.xlisto.elektrodroid.R;


/**
 * Třída OwnAlertDialog, která rozšiřuje DialogFragment pro zobrazení vlastního dialogového okna.
 */
public class OwnAlertDialog extends DialogFragment {

    private static final String ARG_TITLE = "title";
    private static final String ARG_MESSAGE = "message";
    private OnDialogDismissListener dismissListener;


    /**
     * Vytvoří novou instanci OwnAlertDialog s daným názvem a zprávou.
     *
     * @param title   Název dialogu
     * @param message Zpráva dialogu
     * @return Nová instance OwnAlertDialog
     */
    public static OwnAlertDialog newInstance(String title, String message) {
        OwnAlertDialog fragment = new OwnAlertDialog();
        Bundle args = new Bundle();
        args.putString(ARG_TITLE, title);
        args.putString(ARG_MESSAGE, message);
        fragment.setArguments(args);
        return fragment;
    }


    /**
     * Vytvoří a vrátí dialogové okno.
     *
     * @param savedInstanceState Stav uložený při předchozím vytvoření
     * @return Vytvořený dialog
     */
    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {

        String title = null;
        String message = null;
        if (getArguments() != null) {
            title = getArguments().getString(ARG_TITLE);
            message = getArguments().getString(ARG_MESSAGE);
        }

        return new AlertDialog.Builder(requireContext())
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(android.R.string.yes, (dialogInterface, which) -> {
                    // Positive button action
                })
                .setIcon(R.drawable.ic_warning_png)
                .create();
    }


    /**
     * Metoda volaná při zavření dialogu.
     *
     * @param dialog Dialogové rozhraní
     */
    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
        super.onDismiss(dialog);
        if (dismissListener != null) {
            dismissListener.onDialogDismissed();
        }
    }


    /**
     * Nastaví posluchače pro událost zavření dialogu.
     *
     * @param listener Posluchač události zavření dialogu
     */
    public void setOnDialogDismissListener(OnDialogDismissListener listener) {
        this.dismissListener = listener;
    }


    /**
     * Rozhraní pro posluchače události zavření dialogu.
     */
    public interface OnDialogDismissListener {

        void onDialogDismissed();

    }


    /**
     * Zobrazí dialogové okno.
     *
     * @param activity Aktivita, ve které se dialog zobrazí
     * @param title    Název dialogu
     * @param message  Zpráva dialogu
     */
    public static void showDialog(FragmentActivity activity, String title, String message) {
        showDialog(activity, title, message, null);
    }


    /**
     * Zobrazí dialogové okno s posluchačem události zavření.
     *
     * @param activity Aktivita, ve které se dialog zobrazí
     * @param title    Název dialogu
     * @param message  Zpráva dialogu
     * @param listener Posluchač události zavření dialogu
     */
    public static void showDialog(FragmentActivity activity, String title, String message, OnDialogDismissListener listener) {
        OwnAlertDialog dialog = OwnAlertDialog.newInstance(title, message);
        dialog.setOnDialogDismissListener(listener);
        dialog.show(activity.getSupportFragmentManager(), "OwnAlertDialog");
    }

}