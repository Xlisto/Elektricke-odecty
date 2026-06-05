package cz.xlisto.elektrodroid.dialogs;


import android.app.Dialog;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import cz.xlisto.elektrodroid.R;
import cz.xlisto.elektrodroid.modules.invoice.InvoiceListFragment;


/**
 * DialogFragment pro přidání nové faktury do seznamu faktur.
 * Uživatel zadá číslo faktury a potvrdí přidání. Výsledek je předán přes FragmentResult.
 * Xlisto 30.01.2023 20:09
 */
public class InvoiceListAddDialogFragment extends InvoiceListAddEditFragmentAbsctract {

    /**
     * Vytvoří novou instanci dialogu pro přidání faktury.
     *
     * @param idSubscriptionPoint ID odběrného místa, ke kterému bude faktura přiřazena
     * @return nová instance InvoiceListAddDialogFragment
     */
    public static InvoiceListAddDialogFragment newInstance(long idSubscriptionPoint) {
        InvoiceListAddDialogFragment invoiceAddDialogFragment = new InvoiceListAddDialogFragment();
        Bundle bundle = new Bundle();
        bundle.putLong(ID_SUBSCRIPTION_POINT, idSubscriptionPoint);
        invoiceAddDialogFragment.setArguments(bundle);
        return invoiceAddDialogFragment;
    }

    /** Požadovaný prázdný veřejný konstruktor. */
    public InvoiceListAddDialogFragment() {
    }

    /**
     * Vytvoří a vrátí dialog pro přidání nové faktury.
     *
     * @param savedInstanceState uložený stav instance (může být null)
     * @return sestavený AlertDialog
     */
    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        super.onCreateDialog(savedInstanceState);
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext(), R.style.DialogTheme);
        View dialogView = getLayoutInflater().inflate(R.layout.fragment_invoice_list_add_edit, null);

        letNumberInvoice = dialogView.findViewById(R.id.letNumberInvoice);

        builder.setView(dialogView);
        builder.setTitle("Přidat novou fakturu");
        builder.setPositiveButton(R.string.ok, (dialog, which) -> {
            Bundle bundle = new Bundle();
            bundle.putString(InvoiceListFragment.NUMBER_INVOICE, letNumberInvoice.getText());
            bundle.putLong(InvoiceListFragment.ID_SUBSCRIPTIONPOINT, idSubsriptionPoint);
            getParentFragmentManager().setFragmentResult(InvoiceListFragment.INVOICE_NUMBER_ADD_LISTENER, bundle);
        });
        builder.setNegativeButton(R.string.zrusit, null);

        if (savedInstanceState != null) {
            idSubsriptionPoint = savedInstanceState.getLong(ID_SUBSCRIPTION_POINT);
            letNumberInvoice.setDefaultText(savedInstanceState.getString(NUMBER_INVOICE));
        }

        return builder.create();
    }

}
