package cz.xlisto.elektrodroid.dialogs;


import android.app.Dialog;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import cz.xlisto.elektrodroid.R;
import cz.xlisto.elektrodroid.modules.hdo.HdoSiteFragment;
import cz.xlisto.elektrodroid.shp.ShPHdo;
import cz.xlisto.elektrodroid.utils.FragmentChange;


/**
 * Třída HdoOldReminderDialogFragment představuje dialogové okno,
 * které upozorní uživatele na vypršelou platnost časů HDO při spuštění aplikace.
 * <p>
 * Umožňuje uživateli vybrat:
 * - Stáhnout hned (otevře HdoSiteFragment)
 * - Odložit upozornění o 1 den
 * - Nikdy nezobrazovat
 * <p>
 * Xlisto 20.02.2025
 */
public class HdoOldReminderDialogFragment extends DialogFragment {

    private static final String ARG_PLACE_NAME = "placeName";


    /**
     * Vytvoří novou instanci dialogu pro staré HDO.
     *
     * @param placeName Název odběrného místa
     * @return Nová instance HdoOldReminderDialogFragment
     */
    public static HdoOldReminderDialogFragment newInstance(String placeName) {
        HdoOldReminderDialogFragment fragment = new HdoOldReminderDialogFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PLACE_NAME, placeName);
        fragment.setArguments(args);
        return fragment;
    }


    /**
     * Vytvoří a vrátí dialogové okno s možnostmi akce pro staré HDO.
     *
     * @param savedInstanceState Uložený stav
     * @return Vytvořený AlertDialog
     */
    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        String placeName = getArguments() != null ? getArguments().getString(ARG_PLACE_NAME, "") : "";
        String message = placeName.isEmpty()
                ? getString(R.string.hdo_old_reminder_message)
                : getString(R.string.hdo_old_reminder_message_place, placeName);

        ShPHdo shPHdo = new ShPHdo(requireContext());

        return new AlertDialog.Builder(requireContext(), R.style.DialogTheme)
                .setTitle(R.string.hdo_old_reminder_title)
                .setMessage(message)
                .setIcon(R.drawable.ic_warning_png)
                .setPositiveButton(R.string.hdo_download_now, (dialog, which) -> {
                    // Stáhnout hned -> otevřít HdoSiteFragment
                    if (getActivity() != null) {
                        HdoSiteFragment hdoSite = HdoSiteFragment.newInstance();
                        FragmentChange.replace(getActivity(), hdoSite, FragmentChange.Transaction.MOVE, true);
                    }
                })
                .setNeutralButton(R.string.hdo_snooze_1_day, (dialog, which) -> {
                    // Odložit o 1 den (+ 24 hodin v milisekundách)
                    long snoozeUntil = System.currentTimeMillis() + 24L * 60L * 60L * 1000L;
                    shPHdo.set(ShPHdo.HDO_OLD_REMINDER_SNOOZE_UNTIL, snoozeUntil);
                })
                .setNegativeButton(R.string.hdo_never_show, (dialog, which) -> {
                    // Nikdy nezobrazovat -> nastavit flag true
                    shPHdo.set(ShPHdo.HDO_OLD_REMINDER_DISABLED, true);
                })
                .create();
    }


    /**
     * Lifecycle callback po zobrazení dialogu.
     * Aplikuje jednotné barvy tlačítek odpovídající ostatním dialogům v aplikaci.
     */
    @Override
    public void onStart() {
        super.onStart();
        DialogButtonColorHelper.apply(this);
    }

}
