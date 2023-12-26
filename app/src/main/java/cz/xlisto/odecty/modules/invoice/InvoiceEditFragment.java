package cz.xlisto.odecty.modules.invoice;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import cz.xlisto.odecty.R;
import cz.xlisto.odecty.databaze.DataInvoiceSource;
import cz.xlisto.odecty.databaze.DataPriceListSource;
import cz.xlisto.odecty.dialogs.OwnAlertDialog;
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
        DataInvoiceSource dataInvoiceSource = new DataInvoiceSource(getActivity());
        dataInvoiceSource.open();
        InvoiceModel invoice = dataInvoiceSource.loadInvoice(id, table);
        dataInvoiceSource.close();

        if (loadFromDatabase) {
            btnDateStart.setText(ViewHelper.convertLongToDate(invoice.getDateFrom()));
            btnDateEnd.setText(ViewHelper.convertLongToDate(invoice.getDateTo()));
            letVTStart.setDefaultText(DecimalFormatHelper.df2.format(invoice.getVtStart()));
            letNTStart.setDefaultText(DecimalFormatHelper.df2.format(invoice.getNtStart()));
            letVTEnd.setDefaultText(DecimalFormatHelper.df2.format(invoice.getVtEnd()));
            letNTEnd.setDefaultText(DecimalFormatHelper.df2.format(invoice.getNtEnd()));
            letOtherServices.setDefaultText(DecimalFormatHelper.df2.format(invoice.getOtherServices()));
            chIsChangedElectricMeter.setChecked(invoice.isChangedElectricMeter());

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

            InvoiceModel createdInvoice = createInvoice(id,selectedIdPrice);

            //kontrola zda datum nepřekročí první záznam v období bez faktury
            if(WithOutInvoiceService.checkDateFirstItemInvoice(requireActivity(),createdInvoice)) {
                DataInvoiceSource dataInvoiceSource1 = new DataInvoiceSource(requireActivity());
                dataInvoiceSource1.open();
                dataInvoiceSource1.updateInvoice(id, table, createdInvoice);
                dataInvoiceSource1.close();

                Keyboard.hide(requireActivity());

                WithOutInvoiceService.editFirstItemInInvoice(requireActivity());
                loadFromDatabase = true;
                getParentFragmentManager().popBackStack();
            } else {
                OwnAlertDialog.show(requireActivity(), requireContext().getResources().getString(R.string.error),
                        requireContext().getResources().getString(R.string.date_is_not_correct,ViewHelper.convertLongToDate(createdInvoice.getDateTo())));
            }
        });

        oldDateStart = btnDateStart.getText().toString();
        oldDateEnd = btnDateEnd.getText().toString();
    }
}
