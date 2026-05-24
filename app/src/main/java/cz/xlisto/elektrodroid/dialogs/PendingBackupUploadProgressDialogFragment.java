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
 * DialogFragment pro informační zobrazení průběhu odesílání čekajících záloh na Google Drive.
 *
 * <p>Dialog nepoužívá procentuální průběh, protože pending upload worker neposkytuje
 * detailní jednotlivé kroky. Zobrazuje pouze neurčitý progress bar a text stavu,
 * aby měl uživatel jasnou informaci, že nahrávání právě probíhá.</p>
 */
public class PendingBackupUploadProgressDialogFragment extends DialogFragment {

    public static final String TAG = "PendingBackupUploadProgressDialogFragment";
    private static final String ARG_MESSAGE = "argMessage";


    /**
     * Vytvoří novou instanci dialogu s vlastním textem.
     *
     * @param message text zobrazený pod progress barem
     * @return instance dialogu
     */
    public static PendingBackupUploadProgressDialogFragment newInstance(@NonNull String message) {
        PendingBackupUploadProgressDialogFragment fragment = new PendingBackupUploadProgressDialogFragment();
        Bundle args = new Bundle();
        args.putString(ARG_MESSAGE, message);
        fragment.setArguments(args);
        return fragment;
    }


    /**
     * Vytvoří a vrátí instanci dialogu.
     *
     * <p>Nafoukne sdílený layout s progress barem, přepne progress do indeterminate
     * režimu a nastaví text předaný přes argumenty (nebo výchozí fallback string).
     * Dialog je záměrně neuzavíratelný, dokud jej kód explicitně neschová.</p>
     *
     * @param savedInstanceState uložený stav dialogu, může být {@code null}
     * @return nakonfigurovaný dialog pro zobrazení pending uploadu
     */
    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        View view = View.inflate(requireContext(), R.layout.dialog_backup_upload, null);
        ProgressBar progressBar = view.findViewById(R.id.pbBackupUpload);
        TextView tvStatus = view.findViewById(R.id.tvBackupUploadStatus);

        progressBar.setIndeterminate(true);

        String message = getString(R.string.pending_upload_in_progress);
        if (getArguments() != null) {
            String argMessage = getArguments().getString(ARG_MESSAGE, message);
            if (argMessage != null && !argMessage.trim().isEmpty())
                message = argMessage;
        }
        tvStatus.setText(message);

        setCancelable(false);
        return new AlertDialog.Builder(requireContext(), R.style.DialogTheme)
                .setTitle(R.string.upload_selected_backups_title)
                .setView(view)
                .create();
    }


    /**
     * Lifecycle callback po zobrazení dialogu.
     *
     * <p>Aplikuje jednotné barevné styly tlačítek podle helperu používaného v celé aplikaci.</p>
     */
    @Override
    public void onStart() {
        super.onStart();
        DialogButtonColorHelper.apply(this);
    }
}

