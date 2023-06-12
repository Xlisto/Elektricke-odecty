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

        //načte odběrné místo
        DataSubscriptionPointSource dataSubscriptionPointSource = new DataSubscriptionPointSource(getActivity());
        dataSubscriptionPointSource.open();
        InvoiceModel invoice = dataSubscriptionPointSource.loadInvoice(id, table);
        dataSubscriptionPointSource.close();

        if (loadFromDatabase) {
            btnDateStart.setText(ViewHelper.convertLongToDate(invoice.getDateFrom()));
            btnDateEnd.setText(ViewHelper.convertLongToDate(invoice.getDateTo()));
            letVTStart.setDefaultText(DecimalFormatHelper.df2.format(invoice.getVtStart()));
            letNTStart.setDefaultText(DecimalFormatHelper.df2.format(invoice.getNtStart()));
            letVTEnd.setDefaultText(DecimalFormatHelper.df2.format(invoice.getVtEnd()));
            letNTEnd.setDefaultText(DecimalFormatHelper.df2.format(invoice.getNtEnd()));
            letOtherServices.setDefaultText(DecimalFormatHelper.df2.format(invoice.getOtherServices()));

            selectedIdPrice = invoice.getIdPriceList();
            selectedIdInvoice = invoice.getIdInvoice();
            loadFromDatabase = false;
        }

        //načte informace o ceníku použité pro tlačítko výběru ceníku

        DataPriceListSource dataPriceListSource = new DataPriceListSource(getActivity());
        dataPriceListSource.open();
        String priceListName = dataPriceListSource.readPrice(selectedIdPrice).getName();
        String priceListSazba = dataPriceListSource.readPrice(selectedIdPrice).getSazba();
        boolean priceListIsEmpty = dataPriceListSource.readPrice(selectedIdPrice).isEmpty();
        dataPriceListSource.close();

        btnSave.setEnabled(!priceListIsEmpty);

        btnSelectPriceList.setText(priceListName);

        //Skrytí NT údajů
        deactivateNT(priceListSazba.equals(D01) || priceListSazba.equals(D02));

        //zneaktivní tlačítka pokud je první nebo poslední záznam u záznamů bez faktury
        boolean first = WithOutInvoiceService.firstRecordInvoice(requireActivity(), -1L, id);
        boolean last = WithOutInvoiceService.lastRecordInvoice(requireActivity(), -1L, id);
        //zobrazení první záznamu - zneaktivnění vstupních polí pouze u
        if (first && invoice.getIdInvoice()==-1L) {
            letNTStart.setEnabled(false);
            letVTStart.setEnabled(false);
            btnDateStart.setEnabled(false);
        }

        //zobrazení posledního záznamu - zneaktivnění vstupních polí
        if (last && invoice.getIdInvoice()==-1L) {
            letNTEnd.setEnabled(false);
            letVTEnd.setEnabled(false);
            btnDateEnd.setEnabled(false);
        }

        btnSave.setOnClickListener(v -> {
            if (checkData())
                return;

            DataSubscriptionPointSource dataSubscriptionPointSource1 = new DataSubscriptionPointSource(getActivity());
            dataSubscriptionPointSource1.open();
            dataSubscriptionPointSource1.updateInvoice(id, table, createInvoice(id,selectedIdPrice));
            dataSubscriptionPointSource1.close();
            Keyboard.hide(requireActivity());
            WithOutInvoiceService.editFirstItemInInvoice(requireActivity());
            loadFromDatabase = true;
            getParentFragmentManager().popBackStack();
        });

        oldDateStart = btnDateStart.getText().toString();
        oldDateEnd = btnDateEnd.getText().toString();
    }
}
