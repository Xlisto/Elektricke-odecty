package cz.xlisto.elektrodroid.dialogs;


import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import cz.xlisto.elektrodroid.R;


/**
 * DialogFragment pro vytváření nebo přejmenování složek.
 * <p>
 * Tato třída poskytuje dialogové rozhraní pro uživatele k vytvoření nové složky nebo přejmenování stávající.
 * Zpracovává uživatelský vstup a vrací výsledek volajícímu fragmentu.
 */
public class FolderDialog extends DialogFragment {

    public static final String TAG = "FolderDialog";
    public static final String FLAG_RESULT_DIALOG_FRAGMENT = "flagResultDialogFragment";
    public static final String FLAG_FOLDER_NAME = "folderName";
    public static final String RESULT = "result";
    private String flagResultDialogFragment;
    private String oldFolderName;


    /**
     * Vytvoří novou instanci FolderDialog se zadaným příznakem a starým názvem složky.
     * <p>
     * Tato metoda inicializuje novou instanci FolderDialog a nastaví argumenty pro dialog.
     *
     * @param flagResultDialogFragment Příznak označující výsledek dialogového fragmentu.
     * @param oldName                  Název složky, která má být přejmenována.
     * @return Nová instance FolderDialog se zadanými argumenty.
     */
    public static FolderDialog newInstance(String flagResultDialogFragment, String oldName) {
        FolderDialog newDialogFragment = newInstance(flagResultDialogFragment);
        Bundle args = newDialogFragment.getArguments();
        if (args != null) {
            args.putString(FLAG_FOLDER_NAME, oldName);
        }
        newDialogFragment.setArguments(args);
        return newDialogFragment;
    }


    /**
     * Vytvoří novou instanci FolderDialog se zadaným příznakem.
     * <p>
     * Tato metoda inicializuje novou instanci FolderDialog a nastaví argumenty pro dialog.
     *
     * @param flagResultDialogFragment Příznak označující výsledek dialogového fragmentu.
     * @return Nová instance FolderDialog se zadanými argumenty.
     */
    public static FolderDialog newInstance(String flagResultDialogFragment) {
        Bundle args = new Bundle();
        args.putString(FLAG_RESULT_DIALOG_FRAGMENT, flagResultDialogFragment);
        FolderDialog newDialogFragment = new FolderDialog();
        newDialogFragment.setArguments(args);
        return newDialogFragment;
    }


    /**
     * Vytvoří a vrátí dialog pro vytvoření nebo přejmenování složky.
     * <p>
     * Tato metoda nastaví zobrazení dialogu, načte argumenty a nastaví příslušné texty a tlačítka
     * podle toho, zda se jedná o vytvoření nové složky nebo přejmenování stávající složky.
     *
     * @param savedInstanceState Stav uložený v instanci Bundle.
     * @return Dialog pro vytvoření nebo přejmenování složky.
     */
    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        View view = View.inflate(requireContext(), R.layout.dialog_new_folder, null);

        if (getArguments() != null) {
            flagResultDialogFragment = getArguments().getString(FLAG_RESULT_DIALOG_FRAGMENT);
            oldFolderName = getArguments().getString(FLAG_FOLDER_NAME);
        }

        Log.w(TAG, "onCreateDialog: " + oldFolderName);

        int idTitle = R.string.create_new_folder;
        int idBtnOK = R.string.create;
        if (oldFolderName != null) {
            EditText etFolderName = view.findViewById(R.id.etNewFolder);
            etFolderName.setText(oldFolderName);
            idTitle = R.string.rename_folder;
            idBtnOK = R.string.rename;
        }

        return new AlertDialog.Builder(requireContext())
                .setTitle(idTitle)
                .setView(view)
                .setPositiveButton(idBtnOK, (dialog, which) -> {
                    EditText etFolderName = view.findViewById(R.id.etNewFolder);
                    String folderName = etFolderName.getText().toString();
                    if (!folderName.isEmpty()) {
                        Bundle bundle = new Bundle();
                        bundle.putBoolean(RESULT, true);
                        bundle.putString(FLAG_FOLDER_NAME, folderName);
                        requireActivity().getSupportFragmentManager().setFragmentResult(flagResultDialogFragment, bundle);
                        //googleDriveService.createFolder(folderName);
                    }
                })
                .setNegativeButton(R.string.zrusit, null)
                .create();
    }

}
