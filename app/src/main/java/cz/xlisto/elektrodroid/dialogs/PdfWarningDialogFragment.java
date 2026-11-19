package cz.xlisto.elektrodroid.dialogs;


import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.Calendar;

import cz.xlisto.elektrodroid.R;
import cz.xlisto.elektrodroid.shp.ShPPriceList;


/**
 * Dialogové okno s upozorněním na strojové čtení PDF ceníku.
 * Obsahuje checkbox pro pozdržení zobrazování o 1 měsíc.
 */
public class PdfWarningDialogFragment extends DialogFragment {

    public static final String TAG = "PdfWarningDialogFragment";
    public static final String REQUEST_KEY = "PdfWarningDialogFragment_REQUEST_KEY";
    public static final String RESULT_CONFIRMED = "RESULT_CONFIRMED";


    public static PdfWarningDialogFragment newInstance() {
        return new PdfWarningDialogFragment();
    }


    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        View view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_pdf_warning, null);
        CheckBox cbSnooze = view.findViewById(R.id.cbSnoozePdfWarning);

        AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext(), R.style.DialogTheme)
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

        return dialog;
    }


    @Override
    public void onStart() {
        super.onStart();
        DialogButtonColorHelper.apply(this);
    }

}
