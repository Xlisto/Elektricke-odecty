package cz.xlisto.elektrodroid.modules.invoice;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import cz.xlisto.elektrodroid.R;
import cz.xlisto.elektrodroid.models.SummaryInvoiceModel;

/**
 * Xlisto 21.03.2023 21:04
 */
public class MySpinnerInvoiceDetailAdapter extends ArrayAdapter<SummaryInvoiceModel.Title> {
    private static final String TAG = "MySpinnerInvoiceDetailAdapter";
    private final SummaryInvoiceModel.Title[] titles;
    private Boolean hideNt;

    public MySpinnerInvoiceDetailAdapter(@NonNull Context context, int resource, SummaryInvoiceModel.Title[] objects) {
        super(context, resource, objects);
        this.titles = objects;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        return getCustomView(position, parent);
    }

    @Override
    public View getDropDownView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        return getCustomView(position, parent);
    }

    private View getCustomView(int position, @NonNull ViewGroup parent) {
        View view = LayoutInflater.from(getContext()).inflate(R.layout.spinner_invoice_detail, parent, false);
        TextView tvNumberInvoice = view.findViewById(R.id.tvInvoiceDetailName);
        tvNumberInvoice.setText(titles[position].toString());
        if (hideNt) {
            if (position == 1 || position == 5) {
                TextView tv = new TextView(getContext());
                tv.setVisibility(View.GONE);
                tv.setHeight(0);
                view = tv;
            }
        }
        return view;
    }

    @Nullable
    @Override
    public SummaryInvoiceModel.Title getItem(int position) {
        return super.getItem(position);
    }

    public void setHideNt(boolean hideNt) {
        this.hideNt = hideNt;
    }
}
