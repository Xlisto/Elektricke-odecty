package cz.xlisto.elektrodroid.dialogs;


import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

import cz.xlisto.elektrodroid.R;
import cz.xlisto.elektrodroid.models.PriceListModel;

/**
 * Adapter pro dialog se seznamem ceníků a checkboxy.
 * Umožňuje uživateli označit více sazeb, které mají být následně zpracované.
 */
public class YesNoDialogRecyclerViewAdapter extends RecyclerView.Adapter<YesNoDialogRecyclerViewAdapter.MyViewHolder> {
    private final ArrayList<PriceListModel> priceLists;


    /**
     * ViewHolder pro jednu položku seznamu.
     */
    public static class MyViewHolder extends RecyclerView.ViewHolder {
        CheckBox cbTitle;

        /**
         * Vytvoří holder nad položkou layoutu.
         *
         * @param itemView kořenový view položky
         */
        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
        }

    }


    /**
     * Vytvoří adapter nad seznamem ceníků.
     *
     * @param priceLists seznam ceníků k zobrazení
     */
    public YesNoDialogRecyclerViewAdapter(ArrayList<PriceListModel> priceLists) {
        this.priceLists = priceLists;
    }


    /**
     * Vytvoří nový view holder položky.
     */
    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = View.inflate(parent.getContext(), R.layout.item_yes_no_dialog_recycler_view, null);
        MyViewHolder vh = new MyViewHolder(view);
        vh.cbTitle = view.findViewById(R.id.cbTitle);
        return vh;
    }


    /**
     * Naváže data ceníku na konkrétní položku seznamu.
     */
    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        PriceListModel priceList = priceLists.get(position);
        holder.cbTitle.setText(holder.itemView.getContext().getString(R.string.price_list_item_label, priceList.getSazba(), priceList.getProdukt()));
        holder.cbTitle.setOnCheckedChangeListener((buttonView, isChecked) -> priceList.setChecked(isChecked));
        holder.cbTitle.setChecked(priceList.isChecked());
    }


    /**
     * Vrací počet položek seznamu.
     *
     * @return počet ceníků
     */
    @Override
    public int getItemCount() {
        if (priceLists == null)
            return 0;
        return priceLists.size();
    }



}
