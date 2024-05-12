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
 * Xlisto 09.12.2023 20:02
 */
public class YesNoDialogRecyclerViewAdapter extends RecyclerView.Adapter<YesNoDialogRecyclerViewAdapter.MyViewHolder> {
    private static final String TAG = "YesNoDialogRecyclerViewAdapter";
    private final ArrayList<PriceListModel> priceLists;


    static class MyViewHolder extends RecyclerView.ViewHolder {
        CheckBox cbTitle;

        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
        }

    }


    public YesNoDialogRecyclerViewAdapter(ArrayList<PriceListModel> priceLists) {
        this.priceLists = priceLists;
    }


    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = View.inflate(parent.getContext(), R.layout.item_yes_no_dialog_recycler_view, null);
        MyViewHolder vh = new MyViewHolder(view);
        vh.cbTitle = view.findViewById(R.id.cbTitle);
        return vh;
    }


    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        PriceListModel priceList = priceLists.get(position);
        holder.cbTitle.setText(priceList.getSazba()+", "+priceList.getProdukt());
        holder.cbTitle.setOnCheckedChangeListener((buttonView, isChecked) -> priceList.setChecked(isChecked));
        holder.cbTitle.setChecked(priceList.isChecked());
    }


    @Override
    public int getItemCount() {
        if (priceLists == null)
            return 0;
        return priceLists.size();
    }


    /**
     * Vrátí seznam zaškrtnutých ceníků
     * @return seznam zaškrtnutých ceníků
     */
    public ArrayList<PriceListModel> getCheckedPriceLists() {
        ArrayList<PriceListModel> checkedPriceLists = new ArrayList<>();
        for (PriceListModel priceList : priceLists) {
            if (priceList.isChecked())
                checkedPriceLists.add(priceList);
        }
        return checkedPriceLists;
    }

}
