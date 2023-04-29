package cz.xlisto.cenik.modules.invoice;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import cz.xlisto.cenik.R;
import cz.xlisto.cenik.format.DecimalFormatHelper;
import cz.xlisto.cenik.models.InvoiceModel;
import cz.xlisto.cenik.models.SummaryInvoiceModel;
import cz.xlisto.cenik.ownview.ViewHelper;

/**
 * Xlisto 19.03.2023 19:05
 */
public class InvoiceDetailAdapter extends RecyclerView.Adapter<InvoiceDetailAdapter.MyViewHolder> {
    private static final String TAG = "InvoiceDetailAdapter";
    private ArrayList<SummaryInvoiceModel> items;
    private double totalPrice;
    private Listener listener;

    static class MyViewHolder extends RecyclerView.ViewHolder {
        TextView tvDate,tvTitle,tvAmount, tvPriceUnit,tvTotalPrice;
        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
        }

    }

    public InvoiceDetailAdapter(ArrayList<SummaryInvoiceModel> items) {
        this.items = items;
        this.totalPrice = 0;
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
        holder.tvDate.setText(ViewHelper.convertLongToTime(si.getDateOf())+" - "+ViewHelper.convertLongToTime(si.getDateTo()));
        holder.tvTitle.setText(si.getTitle().toString());
        holder.tvAmount.setText(DecimalFormatHelper.df3.format(si.getAmount())+" "+si.getUnit());
        holder.tvPriceUnit.setText(DecimalFormatHelper.df2.format(si.getUnitPrice())+" kč/"+si.getUnit());
        holder.tvTotalPrice.setText(DecimalFormatHelper.df2.format(si.getTotalPrice())+" kč");
        totalPrice += si.getTotalPrice();
        listener.getTotalPrice(totalPrice);
    }

    @Override
    public int getItemCount() {
        if (items == null)
            return 0;
        return items.size();
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    interface Listener{
        void getTotalPrice(double totalPrice) ;
    }

}
