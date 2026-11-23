package cz.xlisto.elektrodroid.dialogs;


import android.app.Dialog;
import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.Calendar;

import cz.xlisto.elektrodroid.R;
import cz.xlisto.elektrodroid.shp.ShPPriceList;


/**
 * Dialogové okno s upozorněním na strojové čtení PDF ceníku.
 * Obsahuje checkbox pro pozdržení zobrazování o 1 měsíc a předává výsledek
 * potvrzení rodičovskému fragmentu přes {@code FragmentManager}.
 */
public class PdfWarningDialogFragment extends DialogFragment {

    /**
     * Tag pro identifikaci dialogového fragmentu ve {@code FragmentManager}.
     */
    public static final String TAG = "PdfWarningDialogFragment";
    /** Klíč požadavku pro předání výsledku dialogu přes {@code FragmentResultListener}. */
    public static final String REQUEST_KEY = "PdfWarningDialogFragment_REQUEST_KEY";
    /** Klíč boolean hodnoty v {@link Bundle} určující, zda uživatel pokračování potvrdil. */
    public static final String RESULT_CONFIRMED = "RESULT_CONFIRMED";


    /**
     * Vytvoří novou instanci dialogu s upozorněním na import PDF ceníku.
     *
     * @return nová instance {@link PdfWarningDialogFragment}
     */
    public static PdfWarningDialogFragment newInstance() {
        return new PdfWarningDialogFragment();
    }


    /**
     * Sestaví dialogové okno s volbou odložení upozornění o 1 měsíc.
     * Při potvrzení se zaškrtnutým checkboxem uloží čas příštího zobrazení do {@link ShPPriceList}
     * pod klíčem {@link ShPPriceList#PDF_WARNING_DEFERRED_UNTIL} a odešle výsledek
     * ({@code true} při potvrzení, {@code false} při zrušení) pod klíčem {@link #REQUEST_KEY}.
     *
     * @param savedInstanceState uložený stav fragmentu při obnově, nebo {@code null}
     * @return nakonfigurovaná instance {@link Dialog}
     */
    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        View view = getLayoutInflater().inflate(R.layout.dialog_pdf_warning, null);
        CheckBox cbSnooze = view.findViewById(R.id.cbSnoozePdfWarning);

        return new MaterialAlertDialogBuilder(requireContext(), R.style.DialogTheme)
                .setTitle(R.string.pdf_import_warning_title)
                .setIcon(R.drawable.ic_warning_png)
                .setView(view)
                .setPositiveButton(R.string.continue_text, (dialogInterface, which) -> {
                    if (cbSnooze.isChecked()) {
                        Calendar cal = Calendar.getInstance();
                        cal.add(Calendar.MONTH, 1);
                        ShPPriceList shp = new ShPPriceList(requireContext());
                        shp.set(ShPPriceList.PDF_WARNING_DEFERRED_UNTIL, cal.getTimeInMillis());
                    }
                    Bundle result = new Bundle();
                    result.putBoolean(RESULT_CONFIRMED, true);
                    getParentFragmentManager().setFragmentResult(REQUEST_KEY, result);
                })
                .setNegativeButton(R.string.zrusit, (dialogInterface, which) -> {
                    Bundle result = new Bundle();
                    result.putBoolean(RESULT_CONFIRMED, false);
                    getParentFragmentManager().setFragmentResult(REQUEST_KEY, result);
                })
                .create();
    }


    /**
     * Při zobrazení dialogu aplikuje barevné stylování tlačítek pomocí {@link DialogButtonColorHelper}.
     */
    @Override
    public void onStart() {
        super.onStart();
        DialogButtonColorHelper.apply(this);
    }

}
