package cz.xlisto.elektrodroid.modules.pricelist.download;


import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import cz.xlisto.elektrodroid.R;


/**
 * Adaptér pro zobrazení seznamu dodavatelů a odkazů na jejich stránky s ceníky.
 */
public class SupplierDownloadAdapter extends RecyclerView.Adapter<SupplierDownloadAdapter.ViewHolder> {

    /**
     * Rozhraní pro obsluhu kliknutí na položku dodavatele.
     */
    public interface OnSupplierClickListener {

        /**
         * Volá se při kliknutí na dodavatele.
         *
         * @param supplier Vybraný model dodavatele
         */
        void onSupplierClick(SupplierDownloadModel supplier);

    }


    private final List<SupplierDownloadModel> items;
    private final OnSupplierClickListener listener;


    /**
     * Vytvoří novou instanci adaptéru.
     *
     * @param items    Seznam dodavatelů
     * @param listener Posluchač události kliknutí
     */
    public SupplierDownloadAdapter(List<SupplierDownloadModel> items, OnSupplierClickListener listener) {
        this.items = items;
        this.listener = listener;
    }


    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_supplier_download, parent, false);
        return new ViewHolder(view);
    }


    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        SupplierDownloadModel item = items.get(position);
        holder.tvSupplierName.setText(item.getName());
        holder.tvSupplierUrl.setText(item.getUrl());

        View.OnClickListener clickListener = v -> {
            if (listener != null) {
                listener.onSupplierClick(item);
            }
        };

        holder.btnOpenSupplierUrl.setOnClickListener(clickListener);
        holder.itemView.setOnClickListener(clickListener);
    }


    @Override
    public int getItemCount() {
        return items != null ? items.size() : 0;
    }


    /**
     * Držitel pohledů pro položku dodavatele v {@link RecyclerView}.
     */
    public static class ViewHolder extends RecyclerView.ViewHolder {

        TextView tvSupplierName;
        TextView tvSupplierUrl;
        Button btnOpenSupplierUrl;


        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvSupplierName = itemView.findViewById(R.id.tvSupplierName);
            tvSupplierUrl = itemView.findViewById(R.id.tvSupplierUrl);
            btnOpenSupplierUrl = itemView.findViewById(R.id.btnOpenSupplierUrl);
        }

    }

}
