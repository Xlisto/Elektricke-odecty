package cz.xlisto.elektrodroid.dialogs;


import android.app.Dialog;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import cz.xlisto.elektrodroid.R;


/**
 * Třída YesNoDialogFragment představuje dialogové okno s tlačítky ANO/NE.
 * Výsledek kliknutí je předán prostřednictvím rozhraní OnDialogResult.
 */
public class YesNoDialogFragment extends DialogFragment {

    public static final String TAG = "YesNoDialogFragment";
    private static final String TITLE = "title";
    private static final String MESSAGE = "message";
    private static final String POSITIVE_TEXT = "positiveText";
    private static final String NEGATIVE_TEXT = "negativeText";
    public static final String FLAG_RESULT_DIALOG_FRAGMENT = "flagResultDialogFragment";
    public static final String RESULT = "result";

    protected String title = "";
    protected String message = "";
    protected String positiveText = "";
    protected String negativeText = "";
    protected String flagResultDialogFragment;
    protected AlertDialog.Builder builder;


    /**
     * Výchozí konstruktor pro YesNoDialogFragment.
     */
    public YesNoDialogFragment() {
    }


    /**
     * Vytvoří novou instanci YesNoDialogFragment se zadaným názvem a příznakem.
     *
     * @param title                    Název dialogu.
     * @param flagResultDialogFragment Příznak pro identifikaci výsledku dialogu.
     * @return Nová instance YesNoDialogFragment.
     */
    public static YesNoDialogFragment newInstance(String title, String flagResultDialogFragment) {
        return newInstance(title, flagResultDialogFragment, null);
    }


    /**
     * Vytvoří novou instanci YesNoDialogFragment se zadaným názvem, příznakem a zprávou.
     *
     * @param title                    Název dialogu.
     * @param flagResultDialogFragment Příznak pro identifikaci výsledku dialogu.
     * @param message                  Zpráva dialogu.
     * @return Nová instance YesNoDialogFragment.
     */
    public static YesNoDialogFragment newInstance(String title, String flagResultDialogFragment, String message) {
        return newInstance(title, flagResultDialogFragment, message, null, null);
    }


    /**
     * Vytvoří novou instanci YesNoDialogFragment s volitelným vlastním textem tlačítek.
     *
     * @param title                    Název dialogu.
     * @param flagResultDialogFragment Příznak pro identifikaci výsledku dialogu.
     * @param message                  Zpráva dialogu.
     * @param positiveText             vlastní text pro kladné tlačítko, nebo {@code null} pro výchozí ANO
     * @param negativeText             vlastní text pro záporné tlačítko, nebo {@code null} pro výchozí NE
     * @return Nová instance YesNoDialogFragment.
     */
    public static YesNoDialogFragment newInstance(String title,
                                                  String flagResultDialogFragment,
                                                  String message,
                                                  String positiveText,
                                                  String negativeText) {
        YesNoDialogFragment yesNoDialogFragment = new YesNoDialogFragment();
        Bundle args = new Bundle();
        args.putString(TITLE, title);
        args.putString(MESSAGE, message);
        args.putString(POSITIVE_TEXT, positiveText);
        args.putString(NEGATIVE_TEXT, negativeText);
        args.putString(FLAG_RESULT_DIALOG_FRAGMENT, flagResultDialogFragment);
        yesNoDialogFragment.setArguments(args);

        // Zachováno i v polích pro kompatibilitu se stávajícím kódem/subclassy.
        yesNoDialogFragment.title = title;
        yesNoDialogFragment.message = message;
        yesNoDialogFragment.positiveText = positiveText;
        yesNoDialogFragment.negativeText = negativeText;
        yesNoDialogFragment.flagResultDialogFragment = flagResultDialogFragment;
        return yesNoDialogFragment;
    }

    /**
     * Sestaví dialog ANO/NE a nastaví vrácení výsledku přes Fragment Result API.
     *
     * @param savedInstanceState uložený stav dialogu, může být {@code null}
     * @return vytvořený dialog
     */
    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        if (getArguments() != null) {
            title = getArguments().getString(TITLE, title);
            message = getArguments().getString(MESSAGE, message);
            positiveText = getArguments().getString(POSITIVE_TEXT, positiveText);
            negativeText = getArguments().getString(NEGATIVE_TEXT, negativeText);
            flagResultDialogFragment = getArguments().getString(FLAG_RESULT_DIALOG_FRAGMENT, flagResultDialogFragment);
        }

        if (savedInstanceState != null) {
            title = savedInstanceState.getString(TITLE, title);
            message = savedInstanceState.getString(MESSAGE, message);
            positiveText = savedInstanceState.getString(POSITIVE_TEXT, positiveText);
            negativeText = savedInstanceState.getString(NEGATIVE_TEXT, negativeText);
            flagResultDialogFragment = savedInstanceState.getString(FLAG_RESULT_DIALOG_FRAGMENT, flagResultDialogFragment);
        }

        builder = new AlertDialog.Builder(requireContext(), R.style.DialogTheme);
        builder.setTitle(title);
        builder.setIcon(R.drawable.ic_warning_png);

        if (message != null && !message.trim().isEmpty()) {
            builder.setMessage(message);
        }

        String positiveButtonText = (positiveText != null && !positiveText.trim().isEmpty())
                ? positiveText
                : getResources().getString(R.string.ano);
        String negativeButtonText = (negativeText != null && !negativeText.trim().isEmpty())
                ? negativeText
                : getResources().getString(R.string.ne);

        builder.setPositiveButton(positiveButtonText, (dialog, which) -> {
            Bundle bundle = new Bundle();
            bundle.putBoolean(RESULT, true);
            getParentFragmentManager().setFragmentResult(flagResultDialogFragment, bundle);
        });
        builder.setNegativeButton(negativeButtonText, (dialog, which) -> {
            Bundle bundle = new Bundle();
            bundle.putBoolean(RESULT, false);
            getParentFragmentManager().setFragmentResult(flagResultDialogFragment, bundle);
        });
        return builder.create();
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

    /**
     * Uloží aktuální stav dialogu pro obnovu po změně konfigurace.
     *
     * @param outState výstupní bundle pro persistenci stavu
     */
    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(TITLE, title);
        outState.putString(MESSAGE, message);
        outState.putString(POSITIVE_TEXT, positiveText);
        outState.putString(NEGATIVE_TEXT, negativeText);
        outState.putString(FLAG_RESULT_DIALOG_FRAGMENT, flagResultDialogFragment);
    }

}
