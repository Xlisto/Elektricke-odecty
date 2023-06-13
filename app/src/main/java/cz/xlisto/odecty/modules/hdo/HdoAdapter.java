package cz.xlisto.odecty.modules.hdo;

import android.annotation.SuppressLint;
import android.transition.TransitionManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.RecyclerView;
import cz.xlisto.odecty.R;
import cz.xlisto.odecty.databaze.DataHdoSource;
import cz.xlisto.odecty.dialogs.YesNoDialogFragment;
import cz.xlisto.odecty.models.HdoModel;
import cz.xlisto.odecty.models.SubscriptionPointModel;
import cz.xlisto.odecty.utils.FragmentChange;
import cz.xlisto.odecty.utils.SubscriptionPoint;

/**
 * Xlisto 26.05.2023 18:38
 */
public class HdoAdapter extends RecyclerView.Adapter<HdoAdapter.MyViewHolder> {
    private static final String TAG = "HdoAdapter";
    private final ArrayList<HdoModel> items;
    private final RecyclerView recyclerView;
    private static int showButtons = -1;
    public static final String FLAG_HDO_ADAPTER_DELETE = "flagHdoAdapterDelete";
    private static long selectedId;
    private static int selectedPosition;


    public static class MyViewHolder extends RecyclerView.ViewHolder {
        TextView tvRele, tvDay, tvTime, tvDayMon, tvDayTue, tvDayWed, tvDayThu, tvDayFri, tvDaySat, tvDaySun;
        LinearLayout lnHdoOut, lnHdoButtons;
        Button btnEdit, btnDelete;

        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }


    public HdoAdapter(ArrayList<HdoModel> items, RecyclerView recyclerView) {
        this.items = items;
        this.recyclerView = recyclerView;
    }


    @NonNull
    @Override
    public HdoAdapter.MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_hdo, parent, false);
        MyViewHolder vh = new MyViewHolder(v);

        vh.lnHdoOut = v.findViewById(R.id.lnHdoOut);
        vh.lnHdoButtons = v.findViewById(R.id.lnButtonsHdo);
        vh.tvRele = v.findViewById(R.id.tvRele);
        vh.tvDay = v.findViewById(R.id.tvDay);
        vh.tvTime = v.findViewById(R.id.tvTime);
        vh.btnEdit = v.findViewById(R.id.btnEditHdo);
        vh.btnDelete = v.findViewById(R.id.btnDeleteHdo);
        vh.tvDayMon = v.findViewById(R.id.tvDayMon);
        vh.tvDayTue = v.findViewById(R.id.tvDayTue);
        vh.tvDayWed = v.findViewById(R.id.tvDayWed);
        vh.tvDayThu = v.findViewById(R.id.tvDayThu);
        vh.tvDayFri = v.findViewById(R.id.tvDayFri);
        vh.tvDaySat = v.findViewById(R.id.tvDaySat);
        vh.tvDaySun = v.findViewById(R.id.tvDaySun);

        return vh;
    }


    @Override
    public void onBindViewHolder(@NonNull HdoAdapter.MyViewHolder holder, @SuppressLint("RecyclerView") int position) {
        HdoModel item = items.get(position);

        if ((item.getMon() == 0)) {
            holder.tvDayMon.setVisibility(View.INVISIBLE);
        } else {
            holder.tvDayMon.setVisibility(View.VISIBLE);
        }
        if ((item.getTue() == 0)) {
            holder.tvDayTue.setVisibility(View.INVISIBLE);
        } else {
            holder.tvDayTue.setVisibility(View.VISIBLE);
        }
        if ((item.getWed() == 0)) {
            holder.tvDayWed.setVisibility(View.INVISIBLE);
        } else {
            holder.tvDayWed.setVisibility(View.VISIBLE);
        }
        if ((item.getThu() == 0)) {
            holder.tvDayThu.setVisibility(View.INVISIBLE);
        } else {
            holder.tvDayThu.setVisibility(View.VISIBLE);
        }
        if ((item.getFri() == 0)) {
            holder.tvDayFri.setVisibility(View.INVISIBLE);
        } else {
            holder.tvDayFri.setVisibility(View.VISIBLE);
        }
        if ((item.getSat() == 0)) {
            holder.tvDaySat.setVisibility(View.INVISIBLE);
        } else {
            holder.tvDaySat.setVisibility(View.VISIBLE);
        }
        if ((item.getSun() == 0)) {
            holder.tvDaySun.setVisibility(View.INVISIBLE);
        } else {
            holder.tvDaySun.setVisibility(View.VISIBLE);
        }


        holder.tvRele.setText(item.getRele());
        holder.tvDay.setVisibility(View.GONE);
        holder.tvTime.setText("Čas: "+item.getTimeFrom()+" - "+item.getTimeUntil());

        holder.lnHdoOut.setOnClickListener(v1 -> {
            if (showButtons >= 0) {
                RecyclerView.ViewHolder viewHolder = recyclerView.findViewHolderForAdapterPosition(showButtons);
                if (viewHolder != null)
                    viewHolder.itemView.findViewById(R.id.lnButtonsHdo).setVisibility(View.GONE);
            }

            TransitionManager.beginDelayedTransition(recyclerView);
            if (showButtons >= 0 && showButtons == position) {
                showButtons = -1;
            } else {
                showButtons = position;
            }
            showButtons(holder, position);
        });

        holder.btnEdit.setOnClickListener(v -> {
            HdoModel hdoModel = items.get(position);
            selectedId = hdoModel.getId();
            selectedPosition = position;
            HdoEditFragment hdoEditFragment = HdoEditFragment.newInstance(hdoModel);
            FragmentChange.replace(((FragmentActivity) v.getContext()), hdoEditFragment, FragmentChange.Transaction.MOVE,true);
        });

        holder.btnDelete.setOnClickListener(v -> {
            HdoModel hdoModel = items.get(position);
            selectedId = hdoModel.getId();
            selectedPosition = position;
            YesNoDialogFragment.newInstance("Odstranit záznam HDO", FLAG_HDO_ADAPTER_DELETE).show(((FragmentActivity) v.getContext()).getSupportFragmentManager(), TAG);
        });
        showButtons(holder, position);
    }


    @Override
    public int getItemCount() {
        if (items == null)
            return 0;
        return items.size();
    }


    /**
     * Skryje/zobrazí tlačítka pro smazání a editaci
     *
     * @param holder   MyViewHolder
     * @param position pozice
     */
    private void showButtons(MyViewHolder holder, int position) {

        if (showButtons == position)
            holder.lnHdoButtons.setVisibility(View.VISIBLE);
        else
            holder.lnHdoButtons.setVisibility(View.GONE);
    }


    /**
     * Odstraní záznam HDO z databáze a z recycler view
     */
    public void deleteItem() {
        deleteHdo();
    }


    /**
     * Odstraní záznam HDO z databáze a z recycler view
     */
    private void deleteHdo() {
        SubscriptionPointModel subscriptionPointModel = SubscriptionPoint.load(recyclerView.getContext());
        DataHdoSource dataHdoSource = new DataHdoSource(recyclerView.getContext());
        dataHdoSource.open();
        assert subscriptionPointModel != null;
        dataHdoSource.deleteHdo(selectedId, subscriptionPointModel.getTableHDO());
        dataHdoSource.close();
        items.remove(selectedPosition);
        notifyItemRemoved(selectedPosition);
        notifyItemRangeChanged(selectedPosition, items.size());
        selectedPosition = -1;
        showButtons = -1;
    }
}
