package cz.xlisto.elektrodroid.dialogs;


import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.DialogFragment;

import cz.xlisto.elektrodroid.R;
import cz.xlisto.elektrodroid.shp.ShPConnections;


/**
 * DialogFragment pro povolení mobilního připojení.
 */
public class SelectConnectionDialogFragment extends DialogFragment {

    public static final String TAG = "SelectConnectionDialogFragment";


    /**
     * Vytvoří novou instanci SelectConnectionDialogFragment.
     *
     * @return Nová instance SelectConnectionDialogFragment.
     */
    public static SelectConnectionDialogFragment newInstance() {
        return new SelectConnectionDialogFragment();
    }


    /**
     * Vytvoří dialog pro výběr připojení.
     *
     * @param savedInstanceState Uložený stav instance.
     * @return Vytvořený dialog.
     */
    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        ShPConnections shPConnections = new ShPConnections(requireContext());
        View view = View.inflate(requireContext(), R.layout.dialog_select_connection, null);
        TextView tvDescription = view.findViewById(R.id.tvDescriptionConnections);
        SwitchCompat switchConnection = view.findViewById(R.id.switchAllowMobileConnection);
        switchConnection.setChecked(shPConnections.get(ShPConnections.ALLOW_MOBILE_CONNECTION, true));
        switchConnection.setOnCheckedChangeListener((buttonView, isChecked) -> setMessageDescription(switchConnection, tvDescription));

        setMessageDescription(switchConnection, tvDescription);

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext(), R.style.DialogTheme);
        builder.setTitle(getResources().getString(R.string.select_connection_titla));
        builder.setView(view);
        builder.setPositiveButton(getResources().getString(R.string.ok), (dialog, which) -> shPConnections.set(ShPConnections.ALLOW_MOBILE_CONNECTION, switchConnection.isChecked()));
        builder.setNegativeButton(getResources().getString(R.string.zrusit), null);
        return builder.create();
    }


    /**
     * Nastaví popis zprávy na základě stavu přepínače.
     *
     * @param sw Přepínač pro povolení mobilního připojení.
     * @param tv TextView pro zobrazení popisu.
     */
    private void setMessageDescription(SwitchCompat sw, TextView tv) {
        sw.setOnCheckedChangeListener((buttonView, isChecked) -> tv.setText(getMessageDescription(sw)));
        tv.setText(getMessageDescription(sw));
    }


    /**
     * Vrátí popis zprávy na základě stavu přepínače.
     *
     * @param sw Přepínač pro povolení mobilního připojení.
     * @return Popis zprávy.
     */
    private String getMessageDescription(SwitchCompat sw) {
        return sw.isChecked() ? getResources().getString(R.string.mobile_data_on) : getResources().getString(R.string.mobile_data_off);
    }

}
