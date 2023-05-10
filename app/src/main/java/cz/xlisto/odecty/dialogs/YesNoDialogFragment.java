package cz.xlisto.odecty.dialogs;

import android.app.Dialog;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import cz.xlisto.odecty.R;

/**
 * DialogFragment s tlačítky ANO/NE
 * Výsledek kliknutí je v OnDialogResult
 */
public class YesNoDialogFragment extends DialogFragment {
    public static final String TAG = "YesNoDialogFragment";
    OnDialogResult onDialogResult;
    private String title = "";
    private String message = "";


    public static YesNoDialogFragment newInstance(OnDialogResult onDialogResult, String title) {
        YesNoDialogFragment yesNoDialogFragment = new YesNoDialogFragment();
        yesNoDialogFragment.title = title;
        yesNoDialogFragment.onDialogResult = onDialogResult;
        return yesNoDialogFragment;
    }


    public static YesNoDialogFragment newInstance(OnDialogResult onDialogResult,String title, String message) {
        YesNoDialogFragment yesNoDialogFragment = new YesNoDialogFragment();
        yesNoDialogFragment.title = title;
        yesNoDialogFragment.message = message;
        yesNoDialogFragment.onDialogResult = onDialogResult;
        return yesNoDialogFragment;
    }


    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle(title);
        builder.setMessage(message);
        builder.setPositiveButton(getResources().getString(R.string.ano), (dialog, which) -> onDialogResult.onResult(true));
        builder.setNegativeButton(getResources().getString(R.string.ne), (dialog, which) -> onDialogResult.onResult(false));
        return builder.create();
    }


    public interface OnDialogResult {
        void onResult(boolean b);
    }
}
