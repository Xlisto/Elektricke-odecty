package cz.xlisto.odecty.modules.payment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.ArrayList;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import cz.xlisto.odecty.R;
import cz.xlisto.odecty.databaze.DataSubscriptionPointSource;
import cz.xlisto.odecty.dialogs.YesNoDialogFragment;
import cz.xlisto.odecty.format.DecimalFormatHelper;
import cz.xlisto.odecty.models.InvoiceListModel;
import cz.xlisto.odecty.models.PaymentModel;
import cz.xlisto.odecty.models.SubscriptionPointModel;
import cz.xlisto.odecty.modules.invoice.MySpinnerInvoiceListAdapter;
import cz.xlisto.odecty.shp.ShPSubscriptionPoint;
import cz.xlisto.odecty.utils.FragmentChange;

/**
 * Fragment zobrazující zálohové platby
 */
public class PaymentFragment extends Fragment {
    private static final String ID_FAK = "idFak";
    private static final String POSITION = "position";
    private String table;
    private long idFak;
    private int position;
    private RecyclerView rv;
    private TextView tvTotal, tvDiscount;
    private Spinner spinner;
    private long idSubscriptionPoint;
    private ArrayList<PaymentModel> payments;
    private ArrayList<InvoiceListModel> invoicesList;
    private PaymentAdapter paymentAdapter;


    public static PaymentFragment newInstance(long idFak, int position) {
        PaymentFragment fragment = new PaymentFragment();
        Bundle args = new Bundle();
        args.putLong(ID_FAK, idFak);
        args.putInt(POSITION, position);
        fragment.setArguments(args);
        return fragment;
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ShPSubscriptionPoint shPSubscriptionPoint = new ShPSubscriptionPoint(getActivity());
        idSubscriptionPoint = shPSubscriptionPoint.get(ShPSubscriptionPoint.ID_SUBSCRIPTION_POINT, -1L);
        DataSubscriptionPointSource dataSubscriptionPointSource = new DataSubscriptionPointSource(getActivity());
        dataSubscriptionPointSource.open();
        SubscriptionPointModel subscriptionPoint = dataSubscriptionPointSource.loadSubscriptionPoint(idSubscriptionPoint);
        table = subscriptionPoint.getTablePLATBY();
        if (getArguments() != null) {
            idFak = getArguments().getLong(ID_FAK);
            position = getArguments().getInt(POSITION);
        }
        if (savedInstanceState != null) {
            idFak = savedInstanceState.getLong(ID_FAK);
            position = savedInstanceState.getInt(POSITION);
        }
        loadPayments();
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_payment, container, false);
    }


    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        rv = view.findViewById(R.id.recycleViewPayment);
        tvTotal = view.findViewById(R.id.tvTotal);
        tvDiscount = view.findViewById(R.id.tvDiscountFragment);
        spinner = view.findViewById(R.id.spPayment);
        Button btnAddPayment = view.findViewById(R.id.btnAddPayment);
        btnAddPayment.setOnClickListener(v -> FragmentChange.replace(requireActivity(), PaymentAddFragment.newInstance(idFak, table), FragmentChange.Transaction.MOVE, true));

        // Posluchač dialogového okna na smazání platby
        requireActivity().getSupportFragmentManager().setFragmentResultListener(PaymentAdapter.FLAG_PAYMENT_ADAPTER_DELETE, this, (requestKey, result) -> {
            if (result.getBoolean(YesNoDialogFragment.RESULT))
                paymentAdapter.deleteItem();
        });
    }


    @Override
    public void onResume() {
        super.onResume();

        MySpinnerInvoiceListAdapter invoiceListAdapter = new MySpinnerInvoiceListAdapter(requireActivity(), R.layout.item_own_simple_list, invoicesList);
        spinner.setAdapter(invoiceListAdapter);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                PaymentFragment.this.position = position;
                idFak = invoicesList.get(position).getIdFak();
                loadPayments();
                setTotal();
                //setRecyclerView();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        spinner.setSelection(position);
        setTotal();
        setRecyclerView();
    }


    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putLong(ID_FAK, idFak);
        outState.putInt(POSITION, position);
    }


    @Override
    public void onPause() {
        super.onPause();
        rv.setAdapter(null);
    }


    /**
     * Načte seznam plateb a seznam faktur
     */
    private void loadPayments() {
        DataSubscriptionPointSource dataSubscriptionPointSource = new DataSubscriptionPointSource(getContext());
        dataSubscriptionPointSource.open();
        payments = dataSubscriptionPointSource.loadPayments(idFak, table);
        SubscriptionPointModel subscriptionPoint = dataSubscriptionPointSource.loadSubscriptionPoint(idSubscriptionPoint);
        if (subscriptionPoint == null)
            return;
        invoicesList = dataSubscriptionPointSource.loadInvoiceLists(subscriptionPoint);
        dataSubscriptionPointSource.close();
    }


    /**
     * Nastaví recyclerview se seznamem plateb
     */
    private void setRecyclerView() {
        paymentAdapter = new PaymentAdapter(payments, rv, table);
        rv.setAdapter(paymentAdapter);
        rv.setLayoutManager(new LinearLayoutManager(getContext()));
        rv.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                rv.getViewTreeObserver().removeOnPreDrawListener(this);

                for (int i = 0; i < rv.getChildCount(); i++) {
                    View v = rv.getChildAt(i);
                    Animation animation = AnimationUtils.loadAnimation(getContext(), R.anim.item_animation_fall_down);
                    v.startAnimation(animation);
                }
                return true;
            }
        });
    }


    /**
     * Nastaví formátovaný celkový součet na TextView
     */
    private void setTotal() {
        DataSubscriptionPointSource dataSubscriptionPointSource = new DataSubscriptionPointSource(getContext());
        dataSubscriptionPointSource.open();
        double discountPayment = dataSubscriptionPointSource.sumDiscount(idFak, table);
        double total = dataSubscriptionPointSource.sumPayment(idFak, table);
        dataSubscriptionPointSource.close();

        PaymentModel.getDiscountDPHText(discountPayment, tvDiscount);
        tvTotal.setText("Součet: " + DecimalFormatHelper.df2.format(total));
    }
}