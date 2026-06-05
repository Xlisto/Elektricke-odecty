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
 * DialogFragment pro úpravu čísla existující faktury v seznamu faktur.
 * Uživatel může změnit číslo faktury a výsledek je předán přes FragmentResult API.
 * Xlisto 01.02.2023 20:45
 */
public class InvoiceListEditDialogFragment extends InvoiceListAddEditFragmentAbsctract {

    /**
     * Vytvoří novou instanci dialogu pro úpravu čísla faktury.
     *
     * @param idInvoice     ID faktury, která se má upravit
     * @param numberInvoice aktuální číslo faktury
     * @return nová instance InvoiceListEditDialogFragment
     */
    public static InvoiceListEditDialogFragment newInstance(long idInvoice, String numberInvoice) {
        InvoiceListEditDialogFragment invoiceEditDialogFragment = new InvoiceListEditDialogFragment();
        Bundle bundle = new Bundle();
        bundle.putLong(ID_INVOICE, idInvoice);
        bundle.putString(NUMBER_INVOICE, numberInvoice);
        invoiceEditDialogFragment.setArguments(bundle);
        return invoiceEditDialogFragment;
    }

    /** Požadovaný prázdný veřejný konstruktor. */
    public InvoiceListEditDialogFragment() {
    }

    /**
     * Vytvoří a vrátí dialog pro úpravu čísla faktury.
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
        if (!numberInvoice.isEmpty()) {
            letNumberInvoice.setDefaultText(numberInvoice);
        }

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
            idInvoice = savedInstanceState.getLong(ID_INVOICE);
            letNumberInvoice.setDefaultText(savedInstanceState.getString(NUMBER_INVOICE));
        }

        return builder.create();
    }
}
