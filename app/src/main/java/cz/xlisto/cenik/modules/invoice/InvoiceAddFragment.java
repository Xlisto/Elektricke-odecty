package cz.xlisto.cenik.modules.invoice;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import cz.xlisto.cenik.databaze.DataSubscriptionPointSource;
import cz.xlisto.cenik.utils.Keyboard;

/**
 * Xlisto 04.02.2023 11:43
 */
public class InvoiceAddFragment extends InvoiceAddEditAbstractFragment {
    private static final String TAG = "InvoiceAddFragment";

    public static InvoiceAddFragment newInstance(String table, long id_fak) {
        InvoiceAddFragment invoiceAddFragment = new InvoiceAddFragment();
        Bundle bundle = new Bundle();
        bundle.putString(TABLE,table);
        bundle.putLong(ID_FAK,id_fak);
        invoiceAddFragment.setArguments(bundle);
        return invoiceAddFragment;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(checkData())
                    return;

                DataSubscriptionPointSource dataSubscriptionPointSource = new DataSubscriptionPointSource(getActivity());
                dataSubscriptionPointSource.open();
                dataSubscriptionPointSource.insertInvoice(table, createInvoice());
                dataSubscriptionPointSource.close();
                WithOutInvoiceService.editFirstItemInInvoice(getActivity());
                Keyboard.hide(getActivity());
                getParentFragmentManager().popBackStack();
            }
        });
    }
}
