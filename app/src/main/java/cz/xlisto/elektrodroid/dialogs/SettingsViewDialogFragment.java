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
 * Dialogové okno pro změnu preferencí zobrazení aplikace.
 * <p>
 * Uživatel může zapnout/vypnout FAB, spodní navigaci a levou navigaci.
 * Po potvrzení se změny uloží do {@link ShPSettings} a odešlou se výsledky
 * pro aktualizaci fragmentů i aktivity.
 */
public class SettingsViewDialogFragment extends DialogFragment {
    public static final String TAG = "SettingsViewDialogFragment";
    public static final String FLAG_UPDATE_SETTINGS_FOR_FRAGMENT = "SettingsViewDialogFragment1";
    public static final String FLAG_UPDATE_SETTINGS_FOR_ACTIVITY = "SettingsViewDialogFragment2";

    /**
     * Vytvoří novou instanci dialogu nastavení zobrazení.
     *
     * @return instance fragmentu
     */
    public static SettingsViewDialogFragment newInstance() {
        return new SettingsViewDialogFragment();
    }


    /**
     * Sestaví dialog s přepínači a uloží hodnoty po potvrzení.
     *
     * @param savedInstanceState uložený stav dialogu, může být {@code null}
     * @return vytvořený dialog
     */
    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        View view = View.inflate(requireContext(), R.layout.dialog_settings_view, null);
        SwitchCompat switchShowFab = view.findViewById(R.id.switchShowFab);
        SwitchCompat switchShowBottomNavigation = view.findViewById(R.id.switchShowBottomNavigation);
        SwitchCompat switchShowLeftNavigation = view.findViewById(R.id.switchShowLeftNavigation);
        ShPSettings shPSettings = new ShPSettings(requireContext());
        switchShowFab.setChecked(shPSettings.get(ShPSettings.SHOW_FAB, true));
        switchShowBottomNavigation.setChecked(shPSettings.get(ShPSettings.SHOW_BOTTOM_NAVIGATION, true));
        switchShowLeftNavigation.setChecked(shPSettings.get(ShPSettings.SHOW_LEFT_NAVIGATION, true));

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext(), R.style.DialogTheme);
        builder.setTitle(getResources().getString(R.string.settings_view));
        builder.setView(view);
        builder.setPositiveButton(getResources().getString(R.string.ok), (dialog, which) -> {
            shPSettings.set(ShPSettings.SHOW_FAB, switchShowFab.isChecked());
            shPSettings.set(ShPSettings.SHOW_BOTTOM_NAVIGATION, switchShowBottomNavigation.isChecked());
            shPSettings.set(ShPSettings.SHOW_LEFT_NAVIGATION, switchShowLeftNavigation.isChecked());
            getParentFragmentManager().setFragmentResult(FLAG_UPDATE_SETTINGS_FOR_FRAGMENT, new Bundle());
            getParentFragmentManager().setFragmentResult(FLAG_UPDATE_SETTINGS_FOR_ACTIVITY, new Bundle());
        });

        return builder.create();
    }

    /**
     * Lifecycle callback po zobrazení dialogu.
     * Aplikuje sjednocené barvy tlačítek.
     */
    @Override
    public void onStart() {
        super.onStart();
        DialogButtonColorHelper.apply(this);
    }
}
