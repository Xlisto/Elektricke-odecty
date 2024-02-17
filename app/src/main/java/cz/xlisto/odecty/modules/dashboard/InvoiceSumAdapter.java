package cz.xlisto.odecty.modules.dashboard;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import cz.xlisto.odecty.R;
import cz.xlisto.odecty.models.InvoiceSumModel;
import cz.xlisto.odecty.ownview.ViewHelper;
import cz.xlisto.odecty.utils.DifferenceDate;

import static cz.xlisto.odecty.utils.Calculation.differentMonth;


/**
 * Adaptér pro zobrazení souhrnu faktur
 * Xlisto 26.12.2023 21:30
 */
public class InvoiceSumAdapter extends RecyclerView.Adapter<InvoiceSumAdapter.MyViewHolder> {
    private static final String TAG = "InvoiceSumAdapter";
    private final Context context;
    private final ArrayList<InvoiceSumModel> items;
    private final int colorVT, colorNT;
    double max;
    private final boolean isShowTotal;


    static class MyViewHolder extends RecyclerView.ViewHolder {
        TextView tvSum, tvDate, tvVtConsuption, tvNtConsuption;
        GraphConsuptionInvoiceGraphView graphConsuptionInvoiceGraphViewVT, graphConsuptionInvoiceGraphViewNT;

        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }


    public InvoiceSumAdapter(Context context, ArrayList<InvoiceSumModel> items, int[] colorVTNT, double max, boolean isShowTotal) {
        this.context = context;
        this.items = items;
        this.colorVT = colorVTNT[0];
        this.colorNT = colorVTNT[1];
        this.max = max;
        this.isShowTotal = isShowTotal;
    }


    @NonNull
    @Override
    public InvoiceSumAdapter.MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_invoice_sum, parent, false);
        MyViewHolder vh = new MyViewHolder(v);
        vh.tvSum = v.findViewById(R.id.tvSumInvoceNumber);
        vh.tvDate = v.findViewById(R.id.tvSumInvoceDate);
        vh.tvVtConsuption = v.findViewById(R.id.tvVTConsuption);
        vh.tvNtConsuption = v.findViewById(R.id.tvNTConsuption);
        vh.graphConsuptionInvoiceGraphViewVT = v.findViewById(R.id.graphDashBoardViewVT);
        vh.graphConsuptionInvoiceGraphViewNT = v.findViewById(R.id.graphDashBoardViewNT);
        return vh;
    }


    @Override
    public void onBindViewHolder(@NonNull InvoiceSumAdapter.MyViewHolder holder, int position) {
        InvoiceSumModel is = items.get(position);
        holder.tvSum.setText(context.getResources().getString(R.string.number_sum_invoce, is.getNumber()));
        if(is.getNumber() == -1)
            holder.tvSum.setText(context.getResources().getString(R.string.number_sum_no_invoce));
        holder.tvDate.setText(context.getResources().getString(R.string.date_sum_invoce,
                ViewHelper.convertLongToDate(is.getDateStart()), ViewHelper.convertLongToDate(is.getDateEnd())));

        double dif = differentMonth(is.getDateStart(),is.getDateEnd(), DifferenceDate.TypeDate.INVOICE);//počet měsíců
        double averageVT = is.getTotalVT()/dif;
        double averageNT = is.getTotalNT()/dif;
        double averageTotal = is.getTotal()/dif;

        holder.graphConsuptionInvoiceGraphViewVT.setConsuption(is.getTotalVT());
        holder.graphConsuptionInvoiceGraphViewVT.setConsuptionMax(max);
        holder.graphConsuptionInvoiceGraphViewVT.setColorGraph(colorVT);
        holder.graphConsuptionInvoiceGraphViewVT.setConsuption(context.getResources().getString(R.string.vt_text_consuption,is.getTotalVT(),averageVT));
        holder.tvVtConsuption.setText(context.getResources().getString(R.string.vt_consuption, is.getTotalVT()));

        if (isShowTotal) {
            holder.graphConsuptionInvoiceGraphViewVT.setConsuption(is.getTotal());
            holder.graphConsuptionInvoiceGraphViewVT.setConsuption(context.getResources().getString(R.string.total_text_consuption,is.getTotal(),averageTotal));
            holder.tvVtConsuption.setText(context.getResources().getString(R.string.total_consuption, is.getTotal()));
            holder.graphConsuptionInvoiceGraphViewNT.setVisibility(View.GONE);
            holder.tvNtConsuption.setVisibility(View.GONE);
        }

        if(!isShowTotal) {
            holder.graphConsuptionInvoiceGraphViewNT.setConsuption(is.getTotalNT());
            holder.graphConsuptionInvoiceGraphViewNT.setConsuptionMax(max);
            holder.graphConsuptionInvoiceGraphViewNT.setColorGraph(colorNT);
            holder.graphConsuptionInvoiceGraphViewNT.setConsuption(context.getResources().getString(R.string.nt_text_consuption,is.getTotalNT(),averageNT));
            holder.tvNtConsuption.setText(context.getResources().getString(R.string.nt_consuption, is.getTotalNT()));
        }
    }


    @Override
    public int getItemCount() {
        if (items == null)
            return 0;
        return items.size();
    }
}
