package cz.xlisto.odecty.modules.payment;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import java.util.ArrayList;
import java.util.Objects;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import cz.xlisto.odecty.R;
import cz.xlisto.odecty.databaze.DataInvoiceSource;
import cz.xlisto.odecty.models.InvoiceListModel;
import cz.xlisto.odecty.models.SubscriptionPointModel;
import cz.xlisto.odecty.utils.SubscriptionPoint;


/**
 * Dialogové okno pro změnu faktury u platby
 * Xlisto 20.02.2024 15:01
 */
public class PaymentChangeInvoiceDialogFragment extends DialogFragment {
    private static final String TAG = "PaymentChangeInvoiceDialogFragment";
    public static final String ARG_SELECTED_ID_INVOICE = "selectedIdInvoice";
    public static final String ARG_ID_PAYMENT = "idPayment";
    public static final String ARG_PREVIOUS_ID_INVOICE = "previousIdInvoice";
    public static final String ARG_SELECTED_POSITION = "position";
    public static final String ARG_RESULT = "result";
    public static final String FLAG_PAYMENT_ADAPTER_CHANGE_INVOICE = "flagPaymentAdapterChangeInvoice";
    private ArrayList<InvoiceListModel> invoices;
    private long selectedIdInvoice;
    private long idPayment, idInvoicePrevious;
    private int selectedPosition;
    private RecyclerView recyclerView;
    private boolean result = false;


    public static PaymentChangeInvoiceDialogFragment newInstance(long selectedIdInvoice, long idPayment) {
        PaymentChangeInvoiceDialogFragment fragment = new PaymentChangeInvoiceDialogFragment();
        Bundle args = new Bundle();
        args.putLong(ARG_SELECTED_ID_INVOICE, selectedIdInvoice);
        args.putLong(ARG_ID_PAYMENT, idPayment);
        args.putLong(ARG_PREVIOUS_ID_INVOICE, selectedIdInvoice);
        args.putInt(ARG_SELECTED_POSITION, -1);
        fragment.setArguments(args);
        return fragment;
    }


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            selectedIdInvoice = getArguments().getLong(ARG_SELECTED_ID_INVOICE);
            idPayment = getArguments().getLong(ARG_ID_PAYMENT);
            idInvoicePrevious = getArguments().getLong(ARG_PREVIOUS_ID_INVOICE);
            selectedPosition = getArguments().getInt(ARG_SELECTED_POSITION);
        }

        SubscriptionPointModel subscriptionPoint = SubscriptionPoint.load(requireContext());

        DataInvoiceSource dataInvoiceSource = new DataInvoiceSource(requireContext());

        if(subscriptionPoint == null) return;

        dataInvoiceSource.open();
        invoices = dataInvoiceSource.loadInvoiceLists(subscriptionPoint);
        dataInvoiceSource.close();
    }


    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putLong(ARG_SELECTED_ID_INVOICE, selectedIdInvoice);
        outState.putLong(ARG_ID_PAYMENT, idPayment);
        outState.putLong(ARG_PREVIOUS_ID_INVOICE, idInvoicePrevious);
        outState.putInt(ARG_SELECTED_POSITION, selectedPosition);
    }


    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        View view = View.inflate(requireContext(), R.layout.dialog_payment_change_invoice, null);
        recyclerView = view.findViewById(R.id.rvInvoiceList);
        recyclerView.setAdapter(new PaymentChangeInvoiceAdapter(invoices, selectedIdInvoice));
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(recyclerView.getContext(), LinearLayoutManager.VERTICAL);
        recyclerView.addItemDecoration(dividerItemDecoration);
        builder.setView(view);
        builder.setTitle(getResources().getString(R.string.change_invoice_dialog_title));

        Button btnChangeInvoice = view.findViewById(R.id.btnOk);
        Button btnCancel = view.findViewById(R.id.btnCancel);
        btnChangeInvoice.setOnClickListener(v -> {
            result = true;
            dismiss();
        });
        btnCancel.setOnClickListener(v -> dismiss());
        return builder.create();
    }


    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
        super.onDismiss(dialog);
        selectedIdInvoice = ((PaymentChangeInvoiceAdapter) Objects.requireNonNull(recyclerView.getAdapter())).getSelectedId();
        selectedPosition = ((PaymentChangeInvoiceAdapter) Objects.requireNonNull(recyclerView.getAdapter())).getSelectedPosition();
        Bundle bundle = new Bundle();
        bundle.putLong(ARG_SELECTED_ID_INVOICE, selectedIdInvoice);
        bundle.putLong(ARG_ID_PAYMENT, idPayment);
        bundle.putLong(ARG_PREVIOUS_ID_INVOICE, idInvoicePrevious);
        bundle.putInt(ARG_SELECTED_POSITION, selectedPosition);
        bundle.putBoolean(ARG_RESULT, result);
        getParentFragmentManager().setFragmentResult(FLAG_PAYMENT_ADAPTER_CHANGE_INVOICE, bundle);
    }
}
