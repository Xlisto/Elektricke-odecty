package cz.xlisto.odecty.modules.payment;


import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;

import cz.xlisto.odecty.R;
import cz.xlisto.odecty.databaze.DataInvoiceSource;
import cz.xlisto.odecty.databaze.DataSubscriptionPointSource;
import cz.xlisto.odecty.dialogs.SettingsViewDialogFragment;
import cz.xlisto.odecty.dialogs.YesNoDialogFragment;
import cz.xlisto.odecty.models.PaymentModel;
import cz.xlisto.odecty.models.SubscriptionPointModel;
import cz.xlisto.odecty.shp.ShPSubscriptionPoint;
import cz.xlisto.odecty.utils.FragmentChange;
import cz.xlisto.odecty.utils.UIHelper;


/**
 * Fragment zobrazující zálohové platby
 */
public class PaymentFragment extends Fragment {

    private static final String TAG = "PaymentFragment";
    private static final String ID_FAK = "idFak";
    private static final String POSITION = "position";
    private String table;
    private long idFak;
    private int position;
    private RecyclerView rv;
    private TextView tvTotal, tvDiscount;
    private long idSubscriptionPoint;
    private ArrayList<PaymentModel> payments;
    private PaymentAdapter paymentAdapter;
    private Button btnAddPayment;
    private FloatingActionButton fab;


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
        idSubscriptionPoint = shPSubscriptionPoint.get(ShPSubscriptionPoint.ID_SUBSCRIPTION_POINT_LONG, -1L);
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
        return inflater.inflate(R.layout.fragment_payment, container, false);
    }


    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        rv = view.findViewById(R.id.recycleViewPayment);
        tvTotal = view.findViewById(R.id.tvTotal);
        tvDiscount = view.findViewById(R.id.tvDiscountFragment);
        btnAddPayment = view.findViewById(R.id.btnAddPayment);
        fab = view.findViewById(R.id.fab);
        btnAddPayment.setOnClickListener(v -> showAddPaymentDialog());
        fab.setOnClickListener(v -> showAddPaymentDialog());
        // Posluchač dialogového okna na smazání platby
        requireActivity().getSupportFragmentManager().setFragmentResultListener(PaymentAdapter.FLAG_PAYMENT_ADAPTER_DELETE, this, (requestKey, result) -> {
            if (result.getBoolean(YesNoDialogFragment.RESULT)) {
                paymentAdapter.deleteItem();
                setTotal();
            }
        });
        // Posluchač PaymentEditFragment na změnu platby
        requireActivity().getSupportFragmentManager().setFragmentResultListener(PaymentAddEditFragmentAbstract.FLAG_RESULT_PAYMENT_FRAGMENT, this,
                (requestKey, result) -> {
                    loadPayments();
                    setTotal();
                    setRecyclerView();
                });
        //Posluchač PaymentChangeInvoiceDialogFragment na změnu faktury
        requireActivity().getSupportFragmentManager().setFragmentResultListener(PaymentChangeInvoiceDialogFragment.FLAG_PAYMENT_ADAPTER_CHANGE_INVOICE, this,
                (requestKey, result) -> {
                    long selectedIdInvoiceNew = result.getLong(PaymentChangeInvoiceDialogFragment.ARG_SELECTED_ID_INVOICE);
                    long idPayment = result.getLong(PaymentChangeInvoiceDialogFragment.ARG_ID_PAYMENT);
                    long previousIdInvoice = result.getLong(PaymentChangeInvoiceDialogFragment.ARG_PREVIOUS_ID_INVOICE);
                    boolean resultChangeInvoice = result.getBoolean(PaymentChangeInvoiceDialogFragment.ARG_RESULT);
                    if (resultChangeInvoice) {
                        DataSubscriptionPointSource dataSubscriptionPointSource = new DataSubscriptionPointSource(requireActivity());
                        dataSubscriptionPointSource.open();
                        dataSubscriptionPointSource.changeInvoicePayment(selectedIdInvoiceNew, table, idPayment);
                        dataSubscriptionPointSource.close();
                    }
                    loadPayments();
                    setTotal();
                    if (selectedIdInvoiceNew != previousIdInvoice) {
                        //paymentAdapter.notifyItemRemoved(paymentAdapter.getSelectedPosition());
                        paymentAdapter.deleteItem();
                        PaymentAdapter.resetShowButtons();
                    }
                });
        //posluchač zavření dialogová okna nastavení
        requireActivity().getSupportFragmentManager().setFragmentResultListener(SettingsViewDialogFragment.FLAG_UPDATE_SETTINGS, this,
                (requestKey, bundle) ->
                        UIHelper.showButtons(btnAddPayment, fab, requireActivity())
        );
    }


    @Override
    public void onResume() {
        super.onResume();
        setTotal();
        setRecyclerView();
        UIHelper.showButtons(btnAddPayment, fab, requireActivity());
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
        DataSubscriptionPointSource dataSubscriptionPointSource = new DataSubscriptionPointSource(requireActivity());
        dataSubscriptionPointSource.open();
        payments = dataSubscriptionPointSource.loadPayments(idFak, table);
        SubscriptionPointModel subscriptionPoint = dataSubscriptionPointSource.loadSubscriptionPoint(idSubscriptionPoint);
        if (subscriptionPoint == null)
            return;
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
        DataInvoiceSource dataInvoiceSource = new DataInvoiceSource(getContext());
        dataInvoiceSource.open();
        double discountPayment = dataInvoiceSource.sumDiscountWithTax(idFak, table);
        double total = dataInvoiceSource.sumPayment(idFak, table);
        dataInvoiceSource.close();
        Log.w(TAG, "setTotal: " + discountPayment + " " + total);
        PaymentModel.getDiscountDPHText(discountPayment, tvDiscount);
        tvTotal.setText(getResources().getString(R.string.sum, total));
    }


    /**
     * Zobrazí dialogové okno pro přidání platby
     */
    private void showAddPaymentDialog() {
        FragmentChange.replace(requireActivity(), PaymentAddFragment.newInstance(idFak, table), FragmentChange.Transaction.MOVE, true);
    }

}