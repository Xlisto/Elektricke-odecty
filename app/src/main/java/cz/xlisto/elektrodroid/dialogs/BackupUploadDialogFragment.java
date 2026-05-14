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

import cz.xlisto.elektrodroid.R;


/**
 * Dialog pro zobrazení průběhu nahrávání záloh na Google Drive.
 * Umožňuje průběžně aktualizovat stav nahrávání, zobrazit finální výsledek
 * a zachovat stav i při obnově instance (např. po rotaci obrazovky).
 */
public class BackupUploadDialogFragment extends DialogFragment {

    public static final String TAG = "BackupUploadDialogFragment";
    private static final String ARG_TOTAL_COUNT = "argTotalCount";
    private static final String STATE_TOTAL_COUNT = "stateTotalCount";
    private static final String STATE_UPLOADED_COUNT = "stateUploadedCount";
    private static final String STATE_FINISHED = "stateFinished";
    private static final String STATE_SUCCESS = "stateSuccess";
    private static final String STATE_CUSTOM_MESSAGE = "stateCustomMessage";

    private ProgressBar progressBar;
    private TextView tvStatus;
    private int totalCount;
    private int uploadedCount;
    private boolean finished;
    private boolean success;
    private String customMessage;

    /**
     * Vytvoří novou instanci dialogu.
     *
     * @param totalCount celkový počet souborů plánovaných k nahrání
     * @return instance dialogu se vstupními argumenty
     */
    public static BackupUploadDialogFragment newInstance(int totalCount) {
        Bundle args = new Bundle();
        args.putInt(ARG_TOTAL_COUNT, totalCount);
        BackupUploadDialogFragment fragment = new BackupUploadDialogFragment();
        fragment.setArguments(args);
        return fragment;
    }

    /**
     * Sestaví dialog a obnoví jeho stav z argumentů nebo saved state.
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
            uploadedCount = savedInstanceState.getInt(STATE_UPLOADED_COUNT, uploadedCount);
            finished = savedInstanceState.getBoolean(STATE_FINISHED, finished);
            success = savedInstanceState.getBoolean(STATE_SUCCESS, success);
            customMessage = savedInstanceState.getString(STATE_CUSTOM_MESSAGE, customMessage);
        } else if (getArguments() != null) {
            totalCount = getArguments().getInt(ARG_TOTAL_COUNT, 0);
        }

        setCancelable(false);
        AlertDialog dialog = new AlertDialog.Builder(requireContext(), R.style.DialogTheme)
                .setTitle(R.string.upload_selected_backups_title)
                .setView(view)
                .setPositiveButton(R.string.ok, (dialogInterface, which) -> dismissAllowingStateLoss())
                .create();

        applyState();
        return dialog;
    }

    /**
     * Lifecycle callback po zobrazení dialogu.
     * Aplikuje barvy tlačítek a jejich enabled stav.
     */
    @Override
    public void onStart() {
        super.onStart();
        DialogButtonColorHelper.apply(this);
        updatePositiveButtonState();
    }

    /**
     * Aktualizuje průběžný stav nahrávání.
     *
     * @param uploadedCount počet již nahraných souborů
     * @param totalCount    celkový počet souborů
     */
    public void showProgress(int uploadedCount, int totalCount) {
        this.uploadedCount = uploadedCount;
        this.totalCount = totalCount;
        this.finished = false;
        this.success = false;
        this.customMessage = null;
        applyState();
    }

    /**
     * Zobrazí finální výsledek nahrávání.
     *
     * @param success       {@code true}, pokud operace skončila úspěchem
     * @param uploadedCount počet nahraných souborů
     * @param totalCount    celkový počet souborů
     */
    public void showResult(boolean success, int uploadedCount, int totalCount) {
        this.finished = true;
        this.success = success;
        this.uploadedCount = uploadedCount;
        this.totalCount = totalCount;
        this.customMessage = null;
        applyState();
    }

    /**
     * Zobrazí chybový výsledek s vlastním textem.
     *
     * @param message text chyby zobrazený uživateli
     */
    public void showFailure(String message) {
        this.finished = true;
        this.success = false;
        this.customMessage = message;
        applyState();
    }

    /**
     * Uloží aktuální stav dialogu pro obnovu po změně konfigurace.
     *
     * @param outState výstupní bundle pro persistenci stavu
     */
    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(STATE_TOTAL_COUNT, totalCount);
        outState.putInt(STATE_UPLOADED_COUNT, uploadedCount);
        outState.putBoolean(STATE_FINISHED, finished);
        outState.putBoolean(STATE_SUCCESS, success);
        outState.putString(STATE_CUSTOM_MESSAGE, customMessage);
    }

    /**
     * Aplikuje interní stav na UI prvky (progress bar, text stavu, tlačítko OK).
     */
    private void applyState() {
        if (progressBar != null) {
            if (finished) {
                progressBar.setVisibility(View.GONE);
            } else {
                progressBar.setVisibility(View.VISIBLE);
                progressBar.setIndeterminate(false);
                progressBar.setMax(Math.max(totalCount, 1));
                progressBar.setProgress(Math.min(uploadedCount, Math.max(totalCount, 1)));
            }
        }

        if (tvStatus != null) {
            if (finished) {
                if (customMessage != null && !customMessage.isEmpty()) {
                    tvStatus.setText(customMessage);
                } else if (success) {
                    tvStatus.setText(getString(R.string.uploaded_selected_backups, uploadedCount, totalCount));
                } else if (uploadedCount > 0) {
                    tvStatus.setText(getString(R.string.partially_uploaded_selected_backups, uploadedCount, totalCount));
                } else {
                    tvStatus.setText(getString(R.string.not_uploaded_selected_backups, uploadedCount, totalCount));
                }
            } else {
                tvStatus.setText(getString(R.string.upload_selected_backups_in_progress, uploadedCount, totalCount));
            }
        }

        updatePositiveButtonState();
    }

    /**
     * Aktualizuje aktivaci tlačítka potvrzení podle toho, zda je operace dokončená.
     */
    private void updatePositiveButtonState() {
        if (!(getDialog() instanceof AlertDialog alertDialog))
            return;

        android.widget.Button positiveButton = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE);
        if (positiveButton != null)
            positiveButton.setEnabled(finished);

        DialogButtonColorHelper.apply(this);
    }
}
