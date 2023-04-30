package cz.xlisto.odecty.modules.payment;

import android.os.Bundle;
import android.util.Log;

import cz.xlisto.odecty.databaze.DataSubscriptionPointSource;

/**
 * Xlisto 15.02.2023 20:31
 */
public class PaymentAddFragment extends PaymentAddEditFragmentAbstract{
    private static final String TAG = "PaymentAddFragment";

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param idFak Parameter 1.
     * @return A new instance of fragment PaymentAddEditFragmentAbstract.
     */
    // TODO: Rename and change types and number of parameters
    public static PaymentAddFragment newInstance(long idFak, String table) {
        PaymentAddFragment fragment = new PaymentAddFragment();
        Bundle args = new Bundle();
        args.putLong(ID_FAK, idFak);
        args.putString(TABLE, table);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    void save() {
        super.save();

        Log.w(TAG,"payment add "+payment.getPayment());
        Log.w(TAG,table);
        Log.w(TAG,payment.toString());

        DataSubscriptionPointSource dataSubscriptionPointSource = new DataSubscriptionPointSource(getContext());
        dataSubscriptionPointSource.open();
        dataSubscriptionPointSource.insertPayment(table,payment);
        dataSubscriptionPointSource.close();
        closeFragment();
    }
}
