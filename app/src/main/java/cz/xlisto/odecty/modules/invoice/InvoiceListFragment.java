package cz.xlisto.odecty.modules.invoice;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import java.util.ArrayList;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import cz.xlisto.odecty.R;
import cz.xlisto.odecty.databaze.DataInvoiceSource;
import cz.xlisto.odecty.databaze.DataSubscriptionPointSource;
import cz.xlisto.odecty.dialogs.SubscriptionPointDialogFragment;
import cz.xlisto.odecty.dialogs.YesNoDialogFragment;
import cz.xlisto.odecty.models.InvoiceListModel;
import cz.xlisto.odecty.models.SubscriptionPointModel;
import cz.xlisto.odecty.shp.ShPSubscriptionPoint;
import cz.xlisto.odecty.utils.SubscriptionPoint;


/**
 * A simple {@link Fragment} subclass.
 * Use the {@link InvoiceListFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class InvoiceListFragment extends Fragment {
    private final String TAG = "InvoiceFragment";
    public static final String INVOICE_NUMBER_EDIT_LISTENER = "invoice_number_edit_listener";
    public static final String INVOICE_NUMBER_ADD_LISTENER = "invoice_number_add_listener";
    public static final String INVOICE_DELETE_LISTENER = "deleteInvoice";
    public static final String NUMBER_INVOICE = "number_invoice";
    public static final String ID_INVOICE = "id_invoice";
    public static final String ID_SUBSCRIPTIONPOINT = "id_subscriptionpoint";
    private RecyclerView rv;
    private long idSubscriptionPoint;
    private InvoiceListAdapter invoiceAdapter;
    private String tablePay;
    private String tableFak;


    public static InvoiceListFragment newInstance() {
        return new InvoiceListFragment();
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //https://developer.android.com/guide/fragments/communicate#fragment-result
        //posluchač pro změnu čísla faktury
        getParentFragmentManager().setFragmentResultListener(INVOICE_NUMBER_EDIT_LISTENER, this, (requestKey, bundle) -> {
            String numberInvoice = bundle.getString(NUMBER_INVOICE);
            long idInvoice = bundle.getLong(ID_INVOICE);
            DataInvoiceSource dataInvoiceSource = new DataInvoiceSource(requireActivity());
            dataInvoiceSource.open();
            dataInvoiceSource.updateInvoiceList(numberInvoice, idInvoice);
            dataInvoiceSource.close();
            onResume();
        });


        getParentFragmentManager().setFragmentResultListener(INVOICE_NUMBER_ADD_LISTENER, this, (requestKey, bundle) -> {
            String numberInvoice = bundle.getString(NUMBER_INVOICE);
            long idSubscriptionPoint = bundle.getLong(ID_SUBSCRIPTIONPOINT);
            DataInvoiceSource dataInvoiceSource = new DataInvoiceSource(requireActivity());
            dataInvoiceSource.open();
            dataInvoiceSource.insertInvoiceList(numberInvoice, idSubscriptionPoint);
            dataInvoiceSource.close();
            onResume();
        });

        //posluchač změny odběrného místa
        getParentFragmentManager().setFragmentResultListener(SubscriptionPointDialogFragment.FLAG_UPDATE_SUBSCRIPTION_POINT, this, (requestKey, bundle) -> {
            readIdSubscriptionPoint();
            onResume();
        });

        //posluchač pro smazání faktury
        getParentFragmentManager().setFragmentResultListener(INVOICE_DELETE_LISTENER, this, (requestKey, bundle) -> {
            boolean b = bundle.getBoolean(YesNoDialogFragment.RESULT);
            long idFak = invoiceAdapter.getSelectedIdFak();

            if (b) {
                DataSubscriptionPointSource dataSubscriptionPointSource = new DataSubscriptionPointSource(requireContext());
                dataSubscriptionPointSource.open();
                dataSubscriptionPointSource.deletePayments(idFak, tablePay);
                dataSubscriptionPointSource.close();

                DataInvoiceSource dataInvoiceSource = new DataInvoiceSource(requireContext());
                dataInvoiceSource.open();
                dataInvoiceSource.deleteInvoiceList(tableFak, idFak);
                dataInvoiceSource.close();

                invoiceAdapter.removeItem();
            }

        });
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_invoice_list, container, false);
    }


    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        readIdSubscriptionPoint();
        Button addInvoice = view.findViewById(R.id.btnAddPayment);
        rv = view.findViewById(R.id.recycleViewInvoiceList);
        TextView tvNoInvoice = view.findViewById(R.id.tvAlertInvoiceList);

        addInvoice.setOnClickListener(v -> {
            InvoiceListAddDialogFragment invoiceAddDialogFragment = InvoiceListAddDialogFragment.newInstance(idSubscriptionPoint);
            invoiceAddDialogFragment.show(requireActivity().getSupportFragmentManager(), TAG);
        });

        SubscriptionPointModel subscriptionPoint = SubscriptionPoint.load(requireActivity());
        if (subscriptionPoint == null) {
            tvNoInvoice.setVisibility(View.VISIBLE);
            rv.setVisibility(View.INVISIBLE);
            addInvoice.setEnabled(false);
        }
    }


    @Override
    public void onResume() {
        super.onResume();
        DataSubscriptionPointSource dataSubscriptionPointSource = new DataSubscriptionPointSource(requireActivity());
        dataSubscriptionPointSource.open();
        SubscriptionPointModel subscriptionPoint = dataSubscriptionPointSource.loadSubscriptionPoint(idSubscriptionPoint);
        tablePay = subscriptionPoint.getTablePLATBY();
        tableFak = subscriptionPoint.getTableFAK();
        dataSubscriptionPointSource.close();

        DataInvoiceSource dataInvoiceSource = new DataInvoiceSource(requireContext());
        dataInvoiceSource.open();
        ArrayList<InvoiceListModel> invoices = dataInvoiceSource.loadInvoiceLists(subscriptionPoint);
        dataInvoiceSource.close();

        invoiceAdapter = new InvoiceListAdapter(getContext(), invoices, rv);
        rv.setAdapter(invoiceAdapter);
        rv.setLayoutManager(new LinearLayoutManager(getContext()));
    }


    private void readIdSubscriptionPoint() {
        ShPSubscriptionPoint shPSubscriptionPoint = new ShPSubscriptionPoint(requireActivity());
        idSubscriptionPoint = shPSubscriptionPoint.get(ShPSubscriptionPoint.ID_SUBSCRIPTION_POINT, -1L);
    }
}