package cz.xlisto.elektrodroid.modules.invoice;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import cz.xlisto.elektrodroid.ownview.LabelEditText;

/**
 * Xlisto 30.01.2023 19:59
 */
public abstract class InvoiceListAddEditFragmentAbsctract extends DialogFragment {
    private static final String TAG = "InvoiceAddEditFragmentAbsctract";
    //static CloseDialogWithPositiveButtonListener closeDialogWithPositiveButtonListener;
    static final String ID_SUBSCRIPTION_POINT = "idSubscriptionPoint";
    static final String ID_INVOICE = "idInvoice";
    static final String NUMBER_INVOICE = "number_invoice";
    long idSubsriptionPoint;
    long idInvoice;
    String numberInvoice;
    LabelEditText letNumberInvoice;




    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle bundle=getArguments();
        idSubsriptionPoint = bundle.getLong(ID_SUBSCRIPTION_POINT);
        idInvoice = bundle.getLong(ID_INVOICE);
        numberInvoice = bundle.getString(NUMBER_INVOICE,"");


    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putLong(ID_SUBSCRIPTION_POINT,idSubsriptionPoint);
        outState.putLong(ID_INVOICE,idInvoice);
        outState.putString(NUMBER_INVOICE, letNumberInvoice.getText());
    }


}
