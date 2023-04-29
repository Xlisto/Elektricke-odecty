package cz.xlisto.cenik.modules.payment;

import android.os.Bundle;
import android.util.Log;
import android.view.View;

import java.util.Calendar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import cz.xlisto.cenik.databaze.DataSubscriptionPointSource;
import cz.xlisto.cenik.format.DecimalFormatHelper;
import cz.xlisto.cenik.models.PaymentModel;

/**
 * Xlisto 17.02.2023 23:03
 */
public class PaymentEditFragment extends PaymentAddEditFragmentAbstract {
    private static final String TAG = "PaymentEditFragment";
    private final String LOAD_DATABASE = "load_database";
    private boolean loadDatabase;

    // TODO: Rename and change types and number of parameters
    public static PaymentEditFragment newInstance(long idFak,long idPayment, String table) {
        PaymentEditFragment fragment = new PaymentEditFragment();
        Bundle args = new Bundle();
        args.putLong(ID_PAYMENT, idPayment);
        args.putLong(ID_FAK, idFak);
        args.putString(TABLE, table);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        loadDatabase = true;
        DataSubscriptionPointSource dataSubscriptionPointSource = new DataSubscriptionPointSource(getContext());
        dataSubscriptionPointSource.open();
        payment = dataSubscriptionPointSource.loadPayment(idPayment, table);
        dataSubscriptionPointSource.close();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (savedInstanceState != null)
            loadDatabase = savedInstanceState.getBoolean(LOAD_DATABASE);
        if (loadDatabase) {
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(payment.getDate());
            labPayment.setDefaultText("" + DecimalFormatHelper.df2.format(payment.getPayment()));
            dp.updateDate(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
            int typePayment = payment.getTypePayment();
            if (typePayment == 0) chPayment.setChecked(true);
            if (typePayment == 1) chSupplement.setChecked(true);
            if (typePayment == 3) chDiscount.setChecked(true);
            loadDatabase = false;
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(LOAD_DATABASE, loadDatabase);
    }

    @Override
    void save() {
        super.save();
        Log.w(TAG,"payment edit "+payment.getPayment());
        DataSubscriptionPointSource dataSubscriptionPointSource = new DataSubscriptionPointSource(getContext());
        dataSubscriptionPointSource.open();
        dataSubscriptionPointSource.updatePayment(idPayment,table,payment);
        dataSubscriptionPointSource.close();
        closeFragment();
    }
}
