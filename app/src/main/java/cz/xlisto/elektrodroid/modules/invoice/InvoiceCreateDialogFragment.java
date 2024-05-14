package cz.xlisto.elektrodroid.modules.invoice;

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import cz.xlisto.elektrodroid.R;


/**
 * Dialogové oknovo pro nastavení čísla nové faktury
 * Xlisto 18.02.2024 8:15
 */
public class InvoiceCreateDialogFragment extends DialogFragment {
    public static final String TAG = "InvoiceCreateDialogFragment";
    public static final String RESULT_CREATE_DIALOG_FRAGMENT = "resultCreateDialogFragment";
    public static final String NUMBER = "number";
    public static final String RESULT = "result";


    public static InvoiceCreateDialogFragment newInstance() {
        return new InvoiceCreateDialogFragment();
    }


    public void onCreate() {

    }


    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext(), R.style.DialogTheme);
        View dialogView = getLayoutInflater().inflate(R.layout.fragment_create_new_invoice, null);
        EditText etNumber = dialogView.findViewById(R.id.etNumber);
        Button btnCancel = dialogView.findViewById(R.id.btnCancelInvoice);
        Button btnCreate = dialogView.findViewById(R.id.btnCreateInvoice);

        btnCancel.setOnClickListener(v -> dismiss());
        btnCreate.setOnClickListener(v -> {
            Bundle bundle = new Bundle();
            bundle.putString(NUMBER, etNumber.getText().toString());
            bundle.putBoolean(RESULT, true);
            getParentFragmentManager().setFragmentResult(RESULT_CREATE_DIALOG_FRAGMENT, bundle);
            dismiss();
        });

        builder.setView(dialogView);
        builder.setTitle(R.string.create_new_invoice);
        return builder.create();
    }
}
