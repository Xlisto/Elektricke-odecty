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
    public static final String NUMBER_INVOICE = "number_invoice";
    public static final String ID_INVOICE = "id_invoice";
    public static final String ID_SUBSCRIPTIONPOINT = "id_subscriptionpoint";
    private RecyclerView rv;
    private long idSubscriptionPoint;


    public static InvoiceListFragment newInstance() {
        return new InvoiceListFragment();
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //https://developer.android.com/guide/fragments/communicate#fragment-result
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
            DataInvoiceSource dataInvoiceSource = new DataInvoiceSource(getActivity());
            dataInvoiceSource.open();
            dataInvoiceSource.insertInvoiceList(numberInvoice,idSubscriptionPoint);
            dataInvoiceSource.close();
            onResume();
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
           InvoiceListAddDialogFragment invoiceAddDialogFragment= InvoiceListAddDialogFragment.newInstance(idSubscriptionPoint);
           invoiceAddDialogFragment.show(requireActivity().getSupportFragmentManager(),TAG);
        });

        SubscriptionPointModel subscriptionPoint = SubscriptionPoint.load(requireActivity());
        if(subscriptionPoint == null){
            tvNoInvoice.setVisibility(View.VISIBLE);
            rv.setVisibility(View.INVISIBLE);
            addInvoice.setEnabled(false);
        }
    }


    @Override
    public void onResume() {
        super.onResume();
        DataSubscriptionPointSource dataSubscriptionPointSource = new DataSubscriptionPointSource(getActivity());
        dataSubscriptionPointSource.open();
        SubscriptionPointModel subscriptionPoint = dataSubscriptionPointSource.loadSubscriptionPoint(idSubscriptionPoint);
        dataSubscriptionPointSource.close();

        if(subscriptionPoint ==null)
            return;

        DataInvoiceSource dataInvoiceSource = new DataInvoiceSource(getActivity());
        dataInvoiceSource.open();
        ArrayList<InvoiceListModel> invoices = dataInvoiceSource.loadInvoiceLists(subscriptionPoint);
        dataInvoiceSource.close();
        InvoiceListAdapter invoiceAdapter = new InvoiceListAdapter(getContext(), invoices,rv);
        rv.setAdapter(invoiceAdapter);
        rv.setLayoutManager(new LinearLayoutManager(getContext()));
    }


    private void readIdSubscriptionPoint(){
        ShPSubscriptionPoint shPSubscriptionPoint = new ShPSubscriptionPoint(getActivity());
        idSubscriptionPoint = shPSubscriptionPoint.get(ShPSubscriptionPoint.ID_SUBSCRIPTION_POINT,-1L);
    }
}