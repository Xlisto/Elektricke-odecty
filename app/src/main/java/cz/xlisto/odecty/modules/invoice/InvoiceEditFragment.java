package cz.xlisto.odecty.modules.invoice;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import cz.xlisto.odecty.databaze.DataPriceListSource;
import cz.xlisto.odecty.databaze.DataSubscriptionPointSource;
import cz.xlisto.odecty.format.DecimalFormatHelper;
import cz.xlisto.odecty.models.InvoiceModel;
import cz.xlisto.odecty.ownview.ViewHelper;
import cz.xlisto.odecty.utils.Keyboard;

import static cz.xlisto.odecty.modules.invoice.InvoiceAbstract.D01;
import static cz.xlisto.odecty.modules.invoice.InvoiceAbstract.D02;

/**
 * Fragment pro editaci faktury
 * Xlisto 04.02.2023 21:20
 */
public class InvoiceEditFragment extends InvoiceAddEditAbstractFragment {
    private static final String TAG = "InvoiceEditFragment";


    public static InvoiceEditFragment newInstance(String table, long id) {
        InvoiceEditFragment invoiceEditFragment = new InvoiceEditFragment();
        Bundle bundle = new Bundle();
        bundle.putString(TABLE, table);
        bundle.putLong(ID, id);
        invoiceEditFragment.setArguments(bundle);
        return invoiceEditFragment;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        DataSubscriptionPointSource dataSubscriptionPointSource = new DataSubscriptionPointSource(getActivity());
        dataSubscriptionPointSource.open();
        InvoiceModel invoice = dataSubscriptionPointSource.loadInvoice(id, table);
        dataSubscriptionPointSource.close();

        selectedIdPrice = invoice.getIdPriceList();
        selectedIdInvoice = invoice.getIdInvoice();
        DataPriceListSource dataPriceListSource = new DataPriceListSource(getActivity());
        dataPriceListSource.open();
        String priceListName = dataPriceListSource.readPrice(selectedIdPrice).getName();
        String priceListSazba = dataPriceListSource.readPrice(selectedIdPrice).getSazba();
        dataPriceListSource.close();

        if (loadFromDatabase) {
            btnDateStart.setText(ViewHelper.convertLongToTime(invoice.getDateFrom()));
            btnDateEnd.setText(ViewHelper.convertLongToTime(invoice.getDateTo()));
            letVTStart.setDefaultText(DecimalFormatHelper.df2.format(invoice.getVtStart()));
            letNTStart.setDefaultText(DecimalFormatHelper.df2.format(invoice.getNtStart()));
            letVTEnd.setDefaultText(DecimalFormatHelper.df2.format(invoice.getVtEnd()));
            letNTEnd.setDefaultText(DecimalFormatHelper.df2.format(invoice.getNtEnd()));
            letOtherServices.setDefaultText(DecimalFormatHelper.df2.format(invoice.getOtherServices()));
            loadFromDatabase = false;

        }
        btnSelectPriceList.setText(priceListName);
        deactivateNT(priceListSazba.equals(D01) || priceListSazba.equals(D02));

        //zneaktivní tlačítka pokud je první nebo poslední záznam
        boolean first = WithOutInvoiceService.firstRecordInvoice(getActivity(), -1L, id);
        boolean last = WithOutInvoiceService.lastRecordInvoice(getActivity(), -1L, id);

        if (first) {
            letNTStart.setEnabled(false);
            letVTStart.setEnabled(false);
            btnDateStart.setEnabled(false);
        }

        if (last) {
            letNTEnd.setEnabled(false);
            letVTEnd.setEnabled(false);
            btnDateEnd.setEnabled(false);
        }

        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (checkData())
                    return;

                DataSubscriptionPointSource dataSubscriptionPointSource = new DataSubscriptionPointSource(getActivity());
                dataSubscriptionPointSource.open();
                dataSubscriptionPointSource.updateInvoice(id, table, createInvoice(id,selectedIdPrice));
                dataSubscriptionPointSource.close();
                Keyboard.hide(getActivity());
                WithOutInvoiceService.editFirstItemInInvoice(getActivity());
                loadFromDatabase = true;
                getParentFragmentManager().popBackStack();
            }
        });
        oldDateStart = btnDateStart.getText().toString();
        oldDateEnd = btnDateEnd.getText().toString();
    }

}
