package cz.xlisto.elektrodroid.modules.pricelist.pdf;


import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import cz.xlisto.elektrodroid.R;
import cz.xlisto.elektrodroid.format.DecimalFormatHelper;
import cz.xlisto.elektrodroid.models.PriceListModel;


/**
 * Adaptér pro zobrazení a výběr načtených distribučních sazeb z PDF ceníku v {@link RecyclerView}.
 * <p>
 * Spravuje seznam položek {@link PriceListModel} a uchovává stav zaškrtnutí jednotlivých sazeb,
 * které si uživatel zvolil k uložení do databáze.
 */
public class PdfRateSelectionAdapter extends RecyclerView.Adapter<PdfRateSelectionAdapter.ViewHolder> {

    private final List<PriceListModel> items;
    private final Set<Integer> selectedIndices;


    /**
     * Vytvoří novou instanci adaptéru se seznamem ceníkových položek.
     * <p>
     * Ve výchozím stavu označí všechny předané sazby jako zaškrtnuté.
     *
     * @param items Seznam načtených ceníkových sazeb z PDF
     */
    public PdfRateSelectionAdapter(List<PriceListModel> items) {
        this.items = items != null ? items : new ArrayList<>();
        this.selectedIndices = new HashSet<>();
        for (int i = 0; i < this.items.size(); i++) {
            selectedIndices.add(i);
        }
    }


    /**
     * Vytvoří a nafoukne nový držitel pohledu s rozvržením položky výběru sazby.
     *
     * @param parent   Rodičovská {@link ViewGroup}
     * @param viewType Typ pohledu
     * @return Nová instance {@link ViewHolder}
     */
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_pdf_rate_selection, parent, false);
        return new ViewHolder(view);
    }


    /**
     * Propojí data ceníkové sazby na dané pozici s prvnky v držiteli pohledu.
     *
     * @param holder   Držitel pohledu {@link ViewHolder}
     * @param position Pozice položky v seznamu
     */
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        PriceListModel item = items.get(position);
        holder.cbSelectRate.setOnCheckedChangeListener(null);
        holder.cbSelectRate.setChecked(selectedIndices.contains(position));

        String title = item.getSazba() + " - " + item.getRada();
        holder.tvRateName.setText(title);

        String info = String.format(Locale.US, "VT: %s Kč | NT: %s Kč | Plat: %s Kč/měs",
                DecimalFormatHelper.df2.format(item.getCenaVT()),
                DecimalFormatHelper.df2.format(item.getCenaNT()),
                DecimalFormatHelper.df2.format(item.getMesicniPlat()));
        holder.tvPricesInfo.setText(info);

        holder.cbSelectRate.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                selectedIndices.add(holder.getBindingAdapterPosition());
            } else {
                selectedIndices.remove(holder.getBindingAdapterPosition());
            }
        });

        holder.itemView.setOnClickListener(v -> {
            boolean nextState = !holder.cbSelectRate.isChecked();
            holder.cbSelectRate.setChecked(nextState);
        });
    }


    /**
     * Vrátí celkový počet ceníkových položek v seznamu.
     *
     * @return Počet položek v seznamu
     */
    @Override
    public int getItemCount() {
        return items.size();
    }


    /**
     * Označí všechny ceníkové sazby v seznamu jako zaškrtnuté a aktualizuje zobrazení položek.
     */
    public void selectAll() {
        selectedIndices.clear();
        for (int i = 0; i < items.size(); i++) {
            selectedIndices.add(i);
        }
        if (!items.isEmpty()) {
            notifyItemRangeChanged(0, items.size());
        }
    }


    /**
     * Zruší označení všech ceníkových sazeb v seznamu a aktualizuje zobrazení položek.
     */
    public void deselectAll() {
        selectedIndices.clear();
        if (!items.isEmpty()) {
            notifyItemRangeChanged(0, items.size());
        }
    }


    /**
     * Vrátí seznam ceníkových položek {@link PriceListModel}, které uživatel označil zaškrtnutím.
     *
     * @return Seznam vybraných ceníkových sazeb
     */
    public List<PriceListModel> getSelectedItems() {
        List<PriceListModel> selected = new ArrayList<>();
        for (int index : selectedIndices) {
            if (index >= 0 && index < items.size()) {
                selected.add(items.get(index));
            }
        }
        return selected;
    }


    /**
     * Držitel pohledů (ViewHolder) pro jednotlivé položky výběru ceníkové sazby v {@link RecyclerView}.
     */
    public static class ViewHolder extends RecyclerView.ViewHolder {

        CheckBox cbSelectRate;
        TextView tvRateName;
        TextView tvPricesInfo;


        /**
         * Vytvoří novou instanci držitele pohledu a vyhledá UI prvky položky.
         *
         * @param itemView Pohled položky seznamu
         */
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            cbSelectRate = itemView.findViewById(R.id.cbSelectRate);
            tvRateName = itemView.findViewById(R.id.tvRateName);
            tvPricesInfo = itemView.findViewById(R.id.tvPricesInfo);
        }

    }

}
