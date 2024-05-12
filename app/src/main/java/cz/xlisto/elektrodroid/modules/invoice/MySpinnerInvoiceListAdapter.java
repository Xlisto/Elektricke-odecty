package cz.xlisto.elektrodroid.modules.invoice;


import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;

import cz.xlisto.elektrodroid.R;
import cz.xlisto.elektrodroid.models.InvoiceListModel;
import cz.xlisto.elektrodroid.ownview.ViewHelper;

/**
 * Xlisto 23.02.2023 20:48
 */
public class MySpinnerInvoiceListAdapter extends ArrayAdapter<InvoiceListModel> {
    private static final String TAG = "MySpinnerInvoiceListAdapter";
    private int resource;
    private ArrayList<InvoiceListModel> invoicesList;

    public MySpinnerInvoiceListAdapter(@NonNull Context context, int resource, @NonNull ArrayList<InvoiceListModel> objects) {
        super(context, resource, objects);
        this.resource = resource;
        this.invoicesList = objects;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        return getCustomView(position, convertView, parent);
    }

    @Override
    public View getDropDownView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        return getCustomView(position, convertView, parent);
    }

    private View getCustomView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        View view = LayoutInflater.from(getContext()).inflate(R.layout.spinner_invoice_list, parent, false);

        InvoiceListModel invoiceList = invoicesList.get(position);
        String minDate = ViewHelper.convertLongToDate(invoiceList.getMinDate());
        String maxDate = ViewHelper.convertLongToDate(invoiceList.getMaxDate());
        TextView tvNumberInvoice = view.findViewById(R.id.tvNumberInvoice);
        TextView tvDateInvoice = view.findViewById(R.id.tvDateInvoiceList);
        tvNumberInvoice.setText(invoiceList.getNumberInvoice());
        tvDateInvoice.setText(" (" + minDate + " - " + maxDate + ")");
        if (invoiceList.getMinDate() == 0 && invoiceList.getMaxDate() == 0) {
            tvDateInvoice.setText("");
        }
        return view;
    }
}
