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
 * Jednoduchý upozorňovací dialog s titulkem, zprávou a tlačítkem potvrzení.
 * Dialog lze použít přímo přes statické metody {@code showDialog(...)} a volitelně
 * poslouchat jeho zavření přes {@link OnDialogDismissListener}.
 */
public class OwnAlertDialog extends DialogFragment {

    private static final String ARG_TITLE = "title";
    private static final String ARG_MESSAGE = "message";
    private static final String ARG_POSITIVE_TEXT = "positiveText";
    private OnDialogDismissListener dismissListener;


    /**
     * Vytvoří novou instanci OwnAlertDialog s daným názvem a zprávou.
     *
     * @param title   Název dialogu
     * @param message Zpráva dialogu
     * @return Nová instance OwnAlertDialog
     */
    public static OwnAlertDialog newInstance(String title, String message) {
        return newInstance(title, message, null);
    }


    /**
     * Vytvoří novou instanci OwnAlertDialog s daným názvem, zprávou a volitelným
     * textem potvrzovacího tlačítka.
     *
     * @param title        Název dialogu
     * @param message      Zpráva dialogu
     * @param positiveText text kladného tlačítka, nebo {@code null} pro výchozí hodnotu
     * @return Nová instance OwnAlertDialog
     */
    public static OwnAlertDialog newInstance(String title, String message, @Nullable String positiveText) {
        OwnAlertDialog fragment = new OwnAlertDialog();
        Bundle args = new Bundle();
        args.putString(ARG_TITLE, title);
        args.putString(ARG_MESSAGE, message);
        args.putString(ARG_POSITIVE_TEXT, positiveText);
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
        String positiveText = null;
        if (getArguments() != null) {
            title = getArguments().getString(ARG_TITLE);
            message = getArguments().getString(ARG_MESSAGE);
            positiveText = getArguments().getString(ARG_POSITIVE_TEXT);
        }

        String positiveButtonText = (positiveText == null || positiveText.trim().isEmpty())
                ? getString(android.R.string.yes)
                : positiveText;

        return new AlertDialog.Builder(requireContext(), R.style.DialogTheme)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(positiveButtonText, (dialogInterface, which) -> {
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

        /**
         * Volá se při zavření dialogu.
         */
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
        showDialog(activity, title, message, null, null);
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
        showDialog(activity, title, message, null, listener);
    }


    /**
     * Zobrazí dialogové okno s volitelným vlastním textem potvrzovacího tlačítka.
     *
     * @param activity      Aktivita, ve které se dialog zobrazí
     * @param title         Název dialogu
     * @param message       Zpráva dialogu
     * @param positiveText  text kladného tlačítka, nebo {@code null} pro výchozí hodnotu
     * @param listener      Posluchač události zavření dialogu
     */
    public static void showDialog(FragmentActivity activity,
                                  String title,
                                  String message,
                                  @Nullable String positiveText,
                                  OnDialogDismissListener listener) {
        OwnAlertDialog dialog = OwnAlertDialog.newInstance(title, message, positiveText);
        dialog.setOnDialogDismissListener(listener);
        dialog.show(activity.getSupportFragmentManager(), "OwnAlertDialog");
    }

    /**
     * Lifecycle callback po zobrazení dialogu.
     * Aplikuje jednotné barvy tlačítek.
     */
    @Override
    public void onStart() {
        super.onStart();
        DialogButtonColorHelper.apply(this);
    }

}
