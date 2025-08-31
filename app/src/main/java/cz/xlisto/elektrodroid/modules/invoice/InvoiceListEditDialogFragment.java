package cz.xlisto.elektrodroid.modules.invoice;


import android.app.Dialog;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import cz.xlisto.elektrodroid.R;

/**
 * Xlisto 01.02.2023 20:45
 */
public class InvoiceListEditDialogFragment extends InvoiceListAddEditFragmentAbsctract {
    private static final String TAG = "InvoiceEditDialogFragment";
    //static CloseDialogWithPositiveButtonListenerEdit closeDialogWithPositiveButtonListenerEdit;

    public static InvoiceListEditDialogFragment newInstance(long idInvoice, String numberInvoice) {
        InvoiceListEditDialogFragment invoiceEditDialogFragment = new InvoiceListEditDialogFragment();
        Bundle bundle = new Bundle();
        bundle.putLong(ID_INVOICE, idInvoice);
        bundle.putString(NUMBER_INVOICE, numberInvoice);
        invoiceEditDialogFragment.setArguments(bundle);
        return invoiceEditDialogFragment;
    }

    public InvoiceListEditDialogFragment() {
    }

    /*@Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        Log.w(TAG,"attach ");
        try {
            closeDialogWithPositiveButtonListenerEdit = (CloseDialogWithPositiveButtonListenerEdit) context;
        } catch(ClassCastException e) {
            //throw new ClassCastException(context.toString() + "must implement");
        }
    }*/

    /*@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            closeDialogWithPositiveButtonListenerEdit = (CloseDialogWithPositiveButtonListenerEdit) getParentFragment();
        } catch (ClassCastException e) {
            throw new ClassCastException("Calling fragment must implement Callback interface");
        }
    }*/



    /*public interface CloseDialogWithPositiveButtonListenerEdit {
        void onCloseDialogWithPositiveButton(String numberInvoice, long idSubscriptionPoint);
    }*/

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        super.onCreateDialog(savedInstanceState);
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        View dialogView = getLayoutInflater().inflate(R.layout.fragment_invoice_list_add_edit, null);

        letNumberInvoice = dialogView.findViewById(R.id.letNumberInvoice);
        if (!numberInvoice.isEmpty())
            letNumberInvoice.setDefaultText(numberInvoice);

        builder.setView(dialogView);
        builder.setTitle("Upravit novou fakturu");
        builder.setPositiveButton(R.string.ok, (dialog, which) -> {
            Bundle bundle = new Bundle();
            bundle.putString(InvoiceListFragment.NUMBER_INVOICE, letNumberInvoice.getText());
            bundle.putLong(InvoiceListFragment.ID_INVOICE, idInvoice);
            getParentFragmentManager().setFragmentResult(InvoiceListFragment.INVOICE_NUMBER_EDIT_LISTENER, bundle);
        });
        builder.setNegativeButton(R.string.zrusit, null);

        if (savedInstanceState != null) {
            idSubsriptionPoint = savedInstanceState.getLong(ID_INVOICE);
            letNumberInvoice.setDefaultText(savedInstanceState.getString(NUMBER_INVOICE));
        }

        return builder.create();
    }
}
