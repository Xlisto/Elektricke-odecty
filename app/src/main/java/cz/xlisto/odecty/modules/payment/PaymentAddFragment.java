package cz.xlisto.odecty.modules.payment;

import android.os.Bundle;

import cz.xlisto.odecty.databaze.DataSubscriptionPointSource;

/**
 * Abstraktní třída fragmentu pro přidání/editaci platby
 * Xlisto 15.02.2023 20:31
 */
public class PaymentAddFragment extends PaymentAddEditFragmentAbstract{
    private static final String TAG = "PaymentAddFragment";


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
        DataSubscriptionPointSource dataSubscriptionPointSource = new DataSubscriptionPointSource(getContext());
        dataSubscriptionPointSource.open();
        dataSubscriptionPointSource.insertPayment(table,payment);
        dataSubscriptionPointSource.close();
        closeFragment();
    }
}
