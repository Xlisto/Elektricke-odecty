package cz.xlisto.elektrodroid.dialogs;


import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.DialogFragment;

import cz.xlisto.elektrodroid.R;
import cz.xlisto.elektrodroid.shp.ShPSettings;


/**
 * dialogové okno pro zobrazení nastavení zobrazení
 */
public class SettingsViewDialogFragment extends DialogFragment {
    public static final String TAG = "SettingsViewDialogFragment";
    public static final String FLAG_UPDATE_SETTINGS = "SettingsViewDialogFragment";

    public static SettingsViewDialogFragment newInstance() {
        return new SettingsViewDialogFragment();
    }


    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        View view = View.inflate(requireContext(), R.layout.dialog_settings_view, null);
        SwitchCompat switchShowFab = view.findViewById(R.id.switchShowFab);
        ShPSettings shPSettings = new ShPSettings(requireContext());
        switchShowFab.setChecked(shPSettings.get(ShPSettings.SHOW_FAB, true));

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle(getResources().getString(R.string.settings_view));
        builder.setView(view);
        builder.setPositiveButton(getResources().getString(R.string.ok), (dialog, which) -> {
            shPSettings.set(ShPSettings.SHOW_FAB, switchShowFab.isChecked());
            getParentFragmentManager().setFragmentResult(FLAG_UPDATE_SETTINGS, new Bundle());
        });
        return builder.create();
    }
}
