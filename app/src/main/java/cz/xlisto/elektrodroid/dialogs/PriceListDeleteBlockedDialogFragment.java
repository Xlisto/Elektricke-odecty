package cz.xlisto.elektrodroid.dialogs;


import android.app.Dialog;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import cz.xlisto.elektrodroid.R;


/**
 * Dialog informující o tom, že ceník nelze smazat, protože je stále používán
 * v dalších částech aplikace.
 *
 * <p>Počty použití jsou zobrazeny přehledně ve třech řádcích podobně jako v tabulce:</p>
 * <ul>
 *     <li>v nezfakturovaném období,</li>
 *     <li>ve fakturách,</li>
 *     <li>v měsíčních odečtech.</li>
 * </ul>
 */
public class PriceListDeleteBlockedDialogFragment extends DialogFragment {

    private static final String ARG_TED = "argTed";
    private static final String ARG_FAK = "argFak";
    private static final String ARG_MON = "argMon";


    /**
     * Vytvoří novou instanci dialogu se souhrnem počtů použití ceníku.
     *
     * @param ted počet použití v nezfakturovaném období
     * @param fak počet použití ve fakturách
     * @param mon počet použití v měsíčních odečtech
     * @return nová instance dialogu
     */
    public static PriceListDeleteBlockedDialogFragment newInstance(int ted, int fak, int mon) {
        PriceListDeleteBlockedDialogFragment fragment = new PriceListDeleteBlockedDialogFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_TED, ted);
        args.putInt(ARG_FAK, fak);
        args.putInt(ARG_MON, mon);
        fragment.setArguments(args);
        return fragment;
    }


    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        View view = getLayoutInflater().inflate(R.layout.dialog_pricelist_delete_blocked, null);

        int ted = 0;
        int fak = 0;
        int mon = 0;
        if (getArguments() != null) {
            ted = getArguments().getInt(ARG_TED, 0);
            fak = getArguments().getInt(ARG_FAK, 0);
            mon = getArguments().getInt(ARG_MON, 0);
        }

        TextView tvTed = view.findViewById(R.id.tvPriceListBlockedTedCount);
        TextView tvFak = view.findViewById(R.id.tvPriceListBlockedFakCount);
        TextView tvMon = view.findViewById(R.id.tvPriceListBlockedMonCount);

        tvTed.setText(String.valueOf(ted));
        tvFak.setText(String.valueOf(fak));
        tvMon.setText(String.valueOf(mon));

        return new AlertDialog.Builder(requireContext(), R.style.DialogTheme)
                .setTitle(R.string.deleting_pricelist)
                .setIcon(R.drawable.ic_warning_png)
                .setView(view)
                .setPositiveButton(R.string.ok, (dialog, which) -> dialog.dismiss())
                .create();
    }


    /**
     * Po zobrazení dialogu sjednotí barvy tlačítek s ostatními dialogy aplikace.
     */
    @Override
    public void onStart() {
        super.onStart();
        DialogButtonColorHelper.apply(this);
    }
}

