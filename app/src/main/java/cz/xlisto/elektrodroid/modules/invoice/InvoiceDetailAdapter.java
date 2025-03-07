package cz.xlisto.elektrodroid.modules.invoice;


import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

import cz.xlisto.elektrodroid.R;
import cz.xlisto.elektrodroid.models.SummaryInvoiceModel;
import cz.xlisto.elektrodroid.ownview.ViewHelper;

/**
 * Xlisto 19.03.2023 19:05
 */
public class InvoiceDetailAdapter extends RecyclerView.Adapter<InvoiceDetailAdapter.MyViewHolder> {
    private static final String TAG = "InvoiceDetailAdapter";
    private final ArrayList<SummaryInvoiceModel> items;
    private final Context context;

    static class MyViewHolder extends RecyclerView.ViewHolder {
        TextView tvDate,tvTitle,tvAmount, tvPriceUnit,tvTotalPrice;
        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
        }

    }

    public InvoiceDetailAdapter(Context context,ArrayList<SummaryInvoiceModel> items) {
        this.items = items;
        this.context = context;
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_invoice_detail, parent, false);
        MyViewHolder vh = new MyViewHolder(v);
        vh.tvDate = v.findViewById(R.id.tvDateInvoiceDetail);
        vh.tvTitle = v.findViewById(R.id.tvTitle);
        vh.tvAmount = v.findViewById(R.id.tvAmount);
        vh.tvPriceUnit = v.findViewById(R.id.tvPriceUnit);
        vh.tvTotalPrice = v.findViewById(R.id.tvTotalPrice);
        return vh;
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        SummaryInvoiceModel si = items.get(position);
        holder.tvDate.setText(context.getResources().getString(R.string.string_dash_string,ViewHelper.convertLongToDate(si.getDateOf()),ViewHelper.convertLongToDate(si.getDateTo())));
        holder.tvTitle.setText(si.getTitle().toString());
        holder.tvAmount.setText(context.getResources().getString(R.string.text_amount,si.getAmount(),si.getUnit()));
        holder.tvPriceUnit.setText(context.getResources().getString(R.string.text_price_unit,si.getUnitPrice(),si.getUnit()));
        holder.tvTotalPrice.setText(context.getResources().getString(R.string.float_price,si.getTotalPrice()));
    }

    @Override
    public int getItemCount() {
        if (items == null)
            return 0;
        return items.size();
    }
}
