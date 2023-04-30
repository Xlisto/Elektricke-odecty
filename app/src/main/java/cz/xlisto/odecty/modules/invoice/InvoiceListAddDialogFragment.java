package cz.xlisto.odecty.modules.invoice;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import cz.xlisto.odecty.R;

/**
 * Xlisto 30.01.2023 20:09
 */
public class InvoiceListAddDialogFragment extends InvoiceListAddEditFragmentAbsctract {
    private static final String TAG = "InvoiceAddDialogFragment";


    public static InvoiceListAddDialogFragment newInstance(long idSubscriptionPoint) {
        InvoiceListAddDialogFragment invoiceAddDialogFragment = new InvoiceListAddDialogFragment();
        Bundle bundle = new Bundle();
        bundle.putLong(ID_SUBSCRIPTION_POINT, idSubscriptionPoint);
        invoiceAddDialogFragment.setArguments(bundle);
        return invoiceAddDialogFragment;
    }

    public InvoiceListAddDialogFragment() {
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        super.onCreateDialog(savedInstanceState);
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        View dialogView = getLayoutInflater().inflate(R.layout.fragment_invoice_list_add_edit, null);

        letNumberInvoice = dialogView.findViewById(R.id.letNumberInvoice);

        builder.setView(dialogView);
        builder.setTitle("Přidat novou fakturu");
        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Bundle bundle = new Bundle();
                bundle.putString(InvoiceListFragment.NUMBER_INVOICE,letNumberInvoice.getText());
                bundle.putLong(InvoiceListFragment.ID_SUBSCRIPTIONPOINT,idSubsriptionPoint);
                getParentFragmentManager().setFragmentResult(InvoiceListFragment.INVOICE_NUMBER_ADD_LISTENER,bundle);
            }
        });
        builder.setNegativeButton(R.string.zrusit, null);

        if (savedInstanceState != null) {
            idSubsriptionPoint = savedInstanceState.getLong(ID_SUBSCRIPTION_POINT);
            letNumberInvoice.setDefaultText(savedInstanceState.getString(NUMBER_INVOICE));
        }

        return builder.create();
    }
}
