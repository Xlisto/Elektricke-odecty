package cz.xlisto.elektrodroid.modules.invoice;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;


/**
 * Fragment pro přidání nového záznamu ve faktuře
 * Xlisto 04.02.2023 11:43
 */
public class InvoiceAddFragment extends InvoiceAddEditAbstractFragment {
    private static final String TAG = "InvoiceAddFragment";


    public static InvoiceAddFragment newInstance(String table, long id_fak) {
        InvoiceAddFragment invoiceAddFragment = new InvoiceAddFragment();
        Bundle bundle = new Bundle();
        bundle.putString(TABLE, table);
        bundle.putLong(ID_FAK, id_fak);
        invoiceAddFragment.setArguments(bundle);
        return invoiceAddFragment;
    }


    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        btnSave.setOnClickListener(v -> saveData(TypeSave.ADD));
    }
}
