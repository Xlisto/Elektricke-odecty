package cz.xlisto.odecty.modules.payment;

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.ArrayList;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import cz.xlisto.odecty.R;
import cz.xlisto.odecty.models.InvoiceListModel;
import cz.xlisto.odecty.ownview.ViewHelper;


/**
 * Adaptér pro recyclerview dialogového okna pro změnu faktury u platby
 * Xlisto 20.02.2024 16:56
 */
public class PaymentChangeInvoiceAdapter extends RecyclerView.Adapter<PaymentChangeInvoiceAdapter.MyViewHolder> {
    private static final String TAG = "PaymentChangeInvoiceAdapter";
    private final ArrayList<InvoiceListModel> items;
    private long selectedId;
    private int selectedPosition = -1;

    static class MyViewHolder extends RecyclerView.ViewHolder {
        TextView tvDate, tvNumberInvoice;
        RadioButton rbSelect;
        RelativeLayout rlRoot;


        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }


    public PaymentChangeInvoiceAdapter(ArrayList<InvoiceListModel> items, long selectedId) {
        this.items = items;
        this.selectedId = selectedId;
    }


    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_change_payment_invoice, parent, false);
        PaymentChangeInvoiceAdapter.MyViewHolder vh = new PaymentChangeInvoiceAdapter.MyViewHolder(view);
        vh.tvDate = view.findViewById(R.id.tvDateInvoicePayment);
        vh.tvNumberInvoice = view.findViewById(R.id.tvNumberInvoicePayment);
        vh.rbSelect = view.findViewById(R.id.rbSelectedInvoicePayment);
        vh.rlRoot = view.findViewById(R.id.rlRoot);
        return vh;
    }


    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, @SuppressLint("RecyclerView") int position) {
        InvoiceListModel item = items.get(position);
        if (item.getIdFak() == selectedId)
            selectedPosition = position;

        holder.tvDate.setText(holder.rlRoot.getContext().getString(R.string.string_dash_string,
                ViewHelper.convertLongToDate(item.getMinDate()), ViewHelper.convertLongToDate(item.getMaxDate())));
        holder.tvNumberInvoice.setText(item.getNumberInvoice());
        holder.rbSelect.setChecked(position == selectedPosition);
        holder.rlRoot.setOnClickListener(v -> {
            selectedPosition = position;
            selectedId = item.getIdFak();
            notifyItemRangeChanged(0, items.size());
        });
    }


    @Override
    public int getItemCount() {
        if (items == null)
            return 0;
        return items.size();
    }


    public long getSelectedId() {
        return selectedId;
    }


    public int getSelectedPosition() {
        return selectedPosition;
    }
}
