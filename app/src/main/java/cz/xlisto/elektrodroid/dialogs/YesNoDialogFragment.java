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
    public static final String FLAG_RESULT_DIALOG_FRAGMENT = "flagResultDialogFragment";
    public static final String RESULT = "result";

    protected String title = "";
    protected String message = "";
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
        YesNoDialogFragment yesNoDialogFragment = new YesNoDialogFragment();
        Bundle args = new Bundle();
        args.putString(TITLE, title);
        args.putString(MESSAGE, message);
        args.putString(FLAG_RESULT_DIALOG_FRAGMENT, flagResultDialogFragment);
        yesNoDialogFragment.setArguments(args);

        // Zachováno i v polích pro kompatibilitu se stávajícím kódem/subclassy.
        yesNoDialogFragment.title = title;
        yesNoDialogFragment.message = message;
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
            flagResultDialogFragment = getArguments().getString(FLAG_RESULT_DIALOG_FRAGMENT, flagResultDialogFragment);
        }

        if (savedInstanceState != null) {
            title = savedInstanceState.getString(TITLE, title);
            message = savedInstanceState.getString(MESSAGE, message);
            flagResultDialogFragment = savedInstanceState.getString(FLAG_RESULT_DIALOG_FRAGMENT, flagResultDialogFragment);
        }

        builder = new AlertDialog.Builder(requireContext(), R.style.DialogTheme);
        builder.setTitle(title);
        builder.setIcon(R.drawable.ic_warning_png);

        if (message != null && !message.trim().isEmpty()) {
            builder.setMessage(message);
        }

        builder.setPositiveButton(getResources().getString(R.string.ano), (dialog, which) -> {
            Bundle bundle = new Bundle();
            bundle.putBoolean(RESULT, true);
            getParentFragmentManager().setFragmentResult(flagResultDialogFragment, bundle);
        });
        builder.setNegativeButton(getResources().getString(R.string.ne), (dialog, which) -> {
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
        outState.putString(FLAG_RESULT_DIALOG_FRAGMENT, flagResultDialogFragment);
    }

}
