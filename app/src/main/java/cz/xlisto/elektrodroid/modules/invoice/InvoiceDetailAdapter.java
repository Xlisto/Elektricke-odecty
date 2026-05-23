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
 * Adaptér pro zobrazení detailních složek ceny faktury v RecyclerView.
 * Každá položka zobrazuje datum, název složky, množství, jednotkovou cenu a celkovou cenu.
 * Xlisto 19.03.2023 19:05
 */
public class InvoiceDetailAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private final ArrayList<SummaryInvoiceModel> items;
    private final Context context;

    /**
     * ViewHolder pro zobrazení jedné řádky detailu faktury.
     */
    private static final class MyViewHolder extends RecyclerView.ViewHolder {
        TextView tvDate, tvTitle, tvAmount, tvPriceUnit, tvTotalPrice;

        /**
         * Konstruktor ViewHolderu.
         *
         * @param itemView kořenový View položky
         */
        MyViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }

    /**
     * Konstruktor adaptéru.
     *
     * @param context kontext aplikace
     * @param items   seznam položek detailu faktury
     */
    public InvoiceDetailAdapter(Context context, ArrayList<SummaryInvoiceModel> items) {
        this.items = items;
        this.context = context;
    }

    /**
     * Vytvoří nový ViewHolder a inicializuje jeho Views.
     *
     * @param parent   rodičovský ViewGroup
     * @param viewType typ pohledu (nepoužíván)
     * @return nový inicializovaný ViewHolder
     */
    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_invoice_detail, parent, false);
        MyViewHolder vh = new MyViewHolder(v);
        vh.tvDate = v.findViewById(R.id.tvDateInvoiceDetail);
        vh.tvTitle = v.findViewById(R.id.tvTitle);
        vh.tvAmount = v.findViewById(R.id.tvAmount);
        vh.tvPriceUnit = v.findViewById(R.id.tvPriceUnit);
        vh.tvTotalPrice = v.findViewById(R.id.tvTotalPrice);
        return vh;
    }

    /**
     * Naváže data ze seznamu na ViewHolder na dané pozici.
     *
     * @param holder   ViewHolder, který se má naplnit daty
     * @param position pozice položky v seznamu
     */
    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        MyViewHolder viewHolder = (MyViewHolder) holder;
        SummaryInvoiceModel si = items.get(position);
        viewHolder.tvDate.setText(context.getResources().getString(R.string.string_dash_string, ViewHelper.convertLongToDate(si.getDateOf()), ViewHelper.convertLongToDate(si.getDateTo())));
        viewHolder.tvTitle.setText(si.getTitle().toString());
        viewHolder.tvAmount.setText(context.getResources().getString(R.string.text_amount, si.getAmount(), si.getUnit()));
        viewHolder.tvPriceUnit.setText(context.getResources().getString(R.string.text_price_unit, si.getUnitPrice(), si.getUnit()));
        viewHolder.tvTotalPrice.setText(context.getResources().getString(R.string.float_price, si.getTotalPrice()));
    }

    /**
     * Vrátí počet položek v seznamu.
     *
     * @return počet položek, nebo 0 pokud je seznam prázdný
     */
    @Override
    public int getItemCount() {
        if (items == null)
            return 0;
        return items.size();
    }
}
