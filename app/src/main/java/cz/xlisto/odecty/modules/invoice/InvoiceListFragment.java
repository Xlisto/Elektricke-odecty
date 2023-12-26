package cz.xlisto.odecty.modules.invoice;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import java.util.ArrayList;

import androidx.fragment.app.FragmentResultListener;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import cz.xlisto.odecty.R;
import cz.xlisto.odecty.databaze.DataInvoiceSource;
import cz.xlisto.odecty.databaze.DataSubscriptionPointSource;
import cz.xlisto.odecty.models.InvoiceListModel;
import cz.xlisto.odecty.models.SubscriptionPointModel;
import cz.xlisto.odecty.shp.ShPSubscriptionPoint;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link InvoiceListFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class InvoiceListFragment extends Fragment {
    private final String TAG = "InvoiceFragment";

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    public static final String INVOICE_NUMBER_EDIT_LISTENER = "invoice_number_edit_listener";
    public static final String INVOICE_NUMBER_ADD_LISTENER = "invoice_number_add_listener";
    public static final String NUMBER_INVOICE = "number_invoice";
    public static final String ID_INVOICE = "id_invoice";
    public static final String ID_SUBSCRIPTIONPOINT = "id_subscriptionpoint";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    private Button addInvoice;
    private RecyclerView rv;
    private long idSubscriptionPoint;
    private SubscriptionPointModel subscriptionPoint;

    public InvoiceListFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment InvoiceFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static InvoiceListFragment newInstance(String param1, String param2) {
        InvoiceListFragment fragment = new InvoiceListFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
        //https://developer.android.com/guide/fragments/communicate#fragment-result
        getParentFragmentManager().setFragmentResultListener(INVOICE_NUMBER_EDIT_LISTENER, this, new FragmentResultListener() {
            @Override
            public void onFragmentResult(@NonNull String requestKey, @NonNull Bundle bundle) {
                String numberInvoice = bundle.getString(NUMBER_INVOICE);
                long idInvoice = bundle.getLong(ID_INVOICE);
                DataInvoiceSource dataInvoiceSource = new DataInvoiceSource(requireActivity());
                dataInvoiceSource.open();
                dataInvoiceSource.updateInvoiceList(numberInvoice, idInvoice);
                dataInvoiceSource.close();
                onResume();
            }
        });
        getParentFragmentManager().setFragmentResultListener(INVOICE_NUMBER_ADD_LISTENER, this, new FragmentResultListener() {
            @Override
            public void onFragmentResult(@NonNull String requestKey, @NonNull Bundle bundle) {
                String numberInvoice = bundle.getString(NUMBER_INVOICE);
                long idSubscriptionPoint = bundle.getLong(ID_SUBSCRIPTIONPOINT);
                DataInvoiceSource dataInvoiceSource = new DataInvoiceSource(getActivity());
                dataInvoiceSource.open();
                dataInvoiceSource.insertInvoiceList(numberInvoice,idSubscriptionPoint);
                dataInvoiceSource.close();
                onResume();
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
        addInvoice = view.findViewById(R.id.btnAddPayment);
        rv = view.findViewById(R.id.recycleViewInvoiceList);



        addInvoice.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
               InvoiceListAddDialogFragment invoiceAddDialogFragment= InvoiceListAddDialogFragment.newInstance(idSubscriptionPoint);
               invoiceAddDialogFragment.show(getActivity().getSupportFragmentManager(),TAG);
            }
        });

    }

    @Override
    public void onResume() {
        super.onResume();
        DataSubscriptionPointSource dataSubscriptionPointSource = new DataSubscriptionPointSource(getActivity());
        dataSubscriptionPointSource.open();
        subscriptionPoint = dataSubscriptionPointSource.loadSubscriptionPoint(idSubscriptionPoint);
        dataSubscriptionPointSource.close();

        if(subscriptionPoint==null)
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