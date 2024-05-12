package cz.xlisto.elektrodroid.dialogs;

import android.app.Dialog;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import cz.xlisto.elektrodroid.R;

/**
 * DialogFragment s tlačítky ANO/NE
 * Výsledek kliknutí je v OnDialogResult
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

    public YesNoDialogFragment() {
    }


    public static YesNoDialogFragment newInstance(String title, String flagResultDialogFragment) {
        YesNoDialogFragment yesNoDialogFragment = new YesNoDialogFragment();
        yesNoDialogFragment.title = title;
        yesNoDialogFragment.flagResultDialogFragment = flagResultDialogFragment;
        return yesNoDialogFragment;
    }


    public static YesNoDialogFragment newInstance(String title, String flagResultDialogFragment, String message) {
        YesNoDialogFragment yesNoDialogFragment = new YesNoDialogFragment();
        yesNoDialogFragment.title = title;
        yesNoDialogFragment.message = message;
        yesNoDialogFragment.flagResultDialogFragment = flagResultDialogFragment;
        return yesNoDialogFragment;
    }


    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            title = savedInstanceState.getString(TITLE);
            message = savedInstanceState.getString(MESSAGE);
            flagResultDialogFragment = savedInstanceState.getString(FLAG_RESULT_DIALOG_FRAGMENT);
        }
        builder = new AlertDialog.Builder(requireContext());
        builder.setTitle(title);
        builder.setMessage(message);
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


    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(TITLE, title);
        outState.putString(MESSAGE, message);
        outState.putString(FLAG_RESULT_DIALOG_FRAGMENT, flagResultDialogFragment);
    }
}
