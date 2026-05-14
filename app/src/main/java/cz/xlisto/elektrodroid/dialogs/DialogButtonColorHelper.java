package cz.xlisto.elektrodroid.dialogs;

import android.content.Context;
import android.widget.Button;

import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;

import cz.xlisto.elektrodroid.R;

/**
 * Sdílená utilita pro sjednocení barvy textu tlačítek v dialogových oknech.
 */
public final class DialogButtonColorHelper {

    /**
     * Privátní konstruktor utilitní třídy.
     */
    private DialogButtonColorHelper() {
    }

    /**
     * Aplikuje barvy tlačítek na dialog získaný z {@link DialogFragment}.
     *
     * @param fragment dialogový fragment, jehož tlačítka se mají obarvit
     */
    public static void apply(DialogFragment fragment) {
        if (fragment == null || fragment.getContext() == null)
            return;
        if (!(fragment.getDialog() instanceof AlertDialog))
            return;

        apply((AlertDialog) fragment.getDialog(), fragment.requireContext());
    }

    /**
     * Aplikuje barvy tlačítek pro konkrétní {@link AlertDialog}.
     *
     * @param alertDialog dialog, jehož tlačítka se mají obarvit
     * @param context     kontext pro načtení barev z resources
     */
    public static void apply(AlertDialog alertDialog, Context context) {
        int enabledColor = ContextCompat.getColor(context, R.color.dialog_button_text);
        int disabledColor = ContextCompat.getColor(context, R.color.dialog_button_text_disabled);

        applyToButton(alertDialog.getButton(AlertDialog.BUTTON_POSITIVE), enabledColor, disabledColor);
        applyToButton(alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE), enabledColor, disabledColor);
        applyToButton(alertDialog.getButton(AlertDialog.BUTTON_NEUTRAL), enabledColor, disabledColor);
    }

    /**
     * Nastaví barvu textu tlačítka podle jeho aktuálního stavu enabled/disabled.
     *
     * @param button        cílové tlačítko
     * @param enabledColor  barva při povoleném stavu
     * @param disabledColor barva při zakázaném stavu
     */
    public static void applyToButton(Button button, int enabledColor, int disabledColor) {
        if (button == null)
            return;
        button.setTextColor(button.isEnabled() ? enabledColor : disabledColor);
    }
}

