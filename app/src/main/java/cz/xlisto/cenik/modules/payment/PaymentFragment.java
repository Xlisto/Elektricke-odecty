package cz.xlisto.cenik.modules.payment;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

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

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import cz.xlisto.cenik.R;
import cz.xlisto.cenik.databaze.DataSubscriptionPointSource;
import cz.xlisto.cenik.format.DecimalFormatHelper;
import cz.xlisto.cenik.models.InvoiceListModel;
import cz.xlisto.cenik.models.PaymentModel;
import cz.xlisto.cenik.models.SubscriptionPointModel;
import cz.xlisto.cenik.modules.invoice.MySpinnerInvoiceListAdapter;
import cz.xlisto.cenik.shp.ShPSubscriptionPoint;
import cz.xlisto.cenik.utils.FragmentChange;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link PaymentFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class PaymentFragment extends Fragment {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ID_FAK = "id_fak";
    private static final String POSITION = "position";

    // TODO: Rename and change types of parameters
    private String table;
    private long idFak;
    private int position;
    private Button btnAddPayment;
    private RecyclerView rv;
    private TextView tvTotal, tvDiscount;
    private Spinner spinner;
    private long idSubscriptionPoint;
    private SubscriptionPointModel subscriptionPoint;
    private double discountPayment;
    private ArrayList<PaymentModel> payments;
    private ArrayList<InvoiceListModel> invoicesList;

    public PaymentFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param idFak Parameter 1.
     * @return A new instance of fragment PaymentFragment.
     */
    // TODO: Rename and change types and number of parameters
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
        btnAddPayment = view.findViewById(R.id.btnAddPayment);
        btnAddPayment.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FragmentChange.replace(getActivity(), PaymentAddFragment.newInstance(idFak, table), FragmentChange.Transaction.MOVE, true);
            }
        });

    }

    @Override
    public void onResume() {
        super.onResume();

        MySpinnerInvoiceListAdapter invoiceListAdapter = new MySpinnerInvoiceListAdapter(getActivity(), R.layout.item_own_simple_list, invoicesList);
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
        subscriptionPoint = dataSubscriptionPointSource.loadSubscriptionPoint(idSubscriptionPoint);
        if (subscriptionPoint == null)
            return;
        invoicesList = dataSubscriptionPointSource.loadInvoiceLists(subscriptionPoint);
        dataSubscriptionPointSource.close();
    }


    /**
     * Nastaví recyclerview se seznamem plateb
     */
    private void setRecyclerView() {
        PaymentAdapter paymentAdapter = new PaymentAdapter(payments, rv, table);
        paymentAdapter.setUpdateListener(new PaymentAdapter.InvoiceAdapterListener() {
            @Override
            public void onUpdateData() {
                onResume();
            }
        });
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
                    /*v.setAlpha(0.0f);
                    v.setTranslationY(((-1*v.getHeight()*20/100)*(i+1)));
                    v.setScaleY(1.05f);
                    v.setScaleX(1.05f);
                    v.setPivotX(0.5f);
                    v.setPivotY(0.5f);
                    v.animate().alpha(1.0f).translationY(0)
                            .scaleX(1).scaleY(1)
                            .setDuration(500)
                            .setStartDelay(i * 15*500/100)
                            .start();*/
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
        discountPayment = dataSubscriptionPointSource.sumDiscount(idFak, table);
        double total = dataSubscriptionPointSource.sumPayment(idFak, table);
        dataSubscriptionPointSource.close();

        PaymentModel.getDiscountDPHText(discountPayment, tvDiscount);
        tvTotal.setText("Součet: " + DecimalFormatHelper.df2.format(total));
    }
}