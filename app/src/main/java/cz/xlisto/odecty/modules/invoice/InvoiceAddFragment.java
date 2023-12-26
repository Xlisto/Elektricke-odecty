package cz.xlisto.odecty.modules.invoice;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import cz.xlisto.odecty.R;
import cz.xlisto.odecty.databaze.DataInvoiceSource;
import cz.xlisto.odecty.dialogs.OwnAlertDialog;
import cz.xlisto.odecty.models.InvoiceModel;
import cz.xlisto.odecty.ownview.ViewHelper;
import cz.xlisto.odecty.utils.Keyboard;

/**
 * Fragment pro přidání nového záznamu ve faktuře
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
        btnSave.setOnClickListener(v -> {
            if(checkData())
                return;

            InvoiceModel createdInvoice = createInvoice();

            //kontrola zda datum nepřekročí první záznam v období bez faktury
            if(WithOutInvoiceService.checkDateFirstItemInvoice(requireActivity(),createdInvoice)){
                DataInvoiceSource dataInvoiceSource = new DataInvoiceSource(getActivity());
                dataInvoiceSource.open();
                dataInvoiceSource.insertInvoice(table, createdInvoice);
                dataInvoiceSource.close();

                WithOutInvoiceService.editFirstItemInInvoice(requireActivity());
                Keyboard.hide(requireActivity());
                getParentFragmentManager().popBackStack();
            } else {
                OwnAlertDialog.show(requireActivity(), requireContext().getResources().getString(R.string.error),
                        requireContext().getResources().getString(R.string.date_is_not_correct, ViewHelper.convertLongToDate(createdInvoice.getDateTo())));
            }



        });
    }
}
