package cz.xlisto.elektrodroid.dialogs;

import android.app.Dialog;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import java.util.Locale;

import cz.xlisto.elektrodroid.R;

/**
 * Dialog pro zobrazení průběhu mazání vybraných souborů z Google Drive.
 */
public class GoogleDriveDeleteProgressDialogFragment extends DialogFragment {

    public static final String TAG = "GoogleDriveDeleteProgressDialogFragment";
    private static final String ARG_TOTAL_COUNT = "argTotalCount";
    private static final String STATE_TOTAL_COUNT = "stateTotalCount";
    private static final String STATE_DELETED_COUNT = "stateDeletedCount";

    private ProgressBar progressBar;
    private TextView tvStatus;
    private int totalCount;
    private int deletedCount;

    /**
     * Vytvoří novou instanci dialogu.
     *
     * @param totalCount počet souborů, které se budou mazat
     * @return instance dialogu
     */
    public static GoogleDriveDeleteProgressDialogFragment newInstance(int totalCount) {
        Bundle args = new Bundle();
        args.putInt(ARG_TOTAL_COUNT, totalCount);
        GoogleDriveDeleteProgressDialogFragment fragment = new GoogleDriveDeleteProgressDialogFragment();
        fragment.setArguments(args);
        return fragment;
    }

    /**
     * Sestaví dialog s progress barem pro mazání souborů.
     *
     * @param savedInstanceState uložený stav dialogu, může být {@code null}
     * @return vytvořený dialog
     */
    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        View view = View.inflate(requireContext(), R.layout.dialog_backup_upload, null);
        progressBar = view.findViewById(R.id.pbBackupUpload);
        tvStatus = view.findViewById(R.id.tvBackupUploadStatus);

        if (savedInstanceState != null) {
            totalCount = savedInstanceState.getInt(STATE_TOTAL_COUNT, totalCount);
            deletedCount = savedInstanceState.getInt(STATE_DELETED_COUNT, deletedCount);
        } else if (getArguments() != null) {
            totalCount = getArguments().getInt(ARG_TOTAL_COUNT, 0);
        }

        setCancelable(false);
        AlertDialog dialog = new AlertDialog.Builder(requireContext(), R.style.DialogTheme)
                .setTitle("Mazání z Google Drive")
                .setView(view)
                .create();

        applyState();
        return dialog;
    }

    /**
     * Uloží stav dialogu pro obnovu po změně konfigurace.
     *
     * @param outState výstupní bundle pro persistenci stavu
     */
    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(STATE_TOTAL_COUNT, totalCount);
        outState.putInt(STATE_DELETED_COUNT, deletedCount);
    }

    /**
     * Aktualizuje průběh mazání.
     *
     * @param deletedCount počet již smazaných souborů
     * @param totalCount celkový počet souborů
     */
    public void showProgress(int deletedCount, int totalCount) {
        this.deletedCount = deletedCount;
        this.totalCount = totalCount;
        applyState();
    }

    private void applyState() {
        if (progressBar != null) {
            progressBar.setVisibility(View.VISIBLE);
            progressBar.setIndeterminate(false);
            progressBar.setMax(Math.max(totalCount, 1));
            progressBar.setProgress(Math.min(deletedCount, Math.max(totalCount, 1)));
        }

        if (tvStatus != null) {
            tvStatus.setText(String.format(Locale.getDefault(), "Mažu vybrané soubory na Google Drive (%d/%d)", deletedCount, totalCount));
        }
    }
}



