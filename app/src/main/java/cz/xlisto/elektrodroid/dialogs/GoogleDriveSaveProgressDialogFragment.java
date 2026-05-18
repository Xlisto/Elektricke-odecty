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
 * Dialog pro zobrazení průběhu ukládání vybraných souborů z Google Drive do lokální zálohy.
 */
public class GoogleDriveSaveProgressDialogFragment extends DialogFragment {

    public static final String TAG = "GoogleDriveSaveProgressDialogFragment";
    private static final String ARG_TOTAL_COUNT = "argTotalCount";
    private static final String STATE_TOTAL_COUNT = "stateTotalCount";
    private static final String STATE_SAVED_COUNT = "stateSavedCount";

    private ProgressBar progressBar;
    private TextView tvStatus;
    private int totalCount;
    private int savedCount;

    public static GoogleDriveSaveProgressDialogFragment newInstance(int totalCount) {
        Bundle args = new Bundle();
        args.putInt(ARG_TOTAL_COUNT, totalCount);
        GoogleDriveSaveProgressDialogFragment fragment = new GoogleDriveSaveProgressDialogFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        View view = View.inflate(requireContext(), R.layout.dialog_backup_upload, null);
        progressBar = view.findViewById(R.id.pbBackupUpload);
        tvStatus = view.findViewById(R.id.tvBackupUploadStatus);

        if (savedInstanceState != null) {
            totalCount = savedInstanceState.getInt(STATE_TOTAL_COUNT, totalCount);
            savedCount = savedInstanceState.getInt(STATE_SAVED_COUNT, savedCount);
        } else if (getArguments() != null) {
            totalCount = getArguments().getInt(ARG_TOTAL_COUNT, 0);
        }

        setCancelable(false);
        AlertDialog dialog = new AlertDialog.Builder(requireContext(), R.style.DialogTheme)
                .setTitle(R.string.save_selected_google_drive_files_title)
                .setView(view)
                .create();

        applyState();
        return dialog;
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(STATE_TOTAL_COUNT, totalCount);
        outState.putInt(STATE_SAVED_COUNT, savedCount);
    }

    public void showProgress(int savedCount, int totalCount) {
        this.savedCount = savedCount;
        this.totalCount = totalCount;
        applyState();
    }

    private void applyState() {
        if (progressBar != null) {
            progressBar.setVisibility(View.VISIBLE);
            progressBar.setIndeterminate(false);
            progressBar.setMax(Math.max(totalCount, 1));
            progressBar.setProgress(Math.min(savedCount, Math.max(totalCount, 1)));
        }

        if (tvStatus != null) {
            tvStatus.setText(String.format(Locale.getDefault(), "%s (%d/%d)",
                    getString(R.string.save_selected_google_drive_files_in_progress), savedCount, totalCount));
        }
    }
}


