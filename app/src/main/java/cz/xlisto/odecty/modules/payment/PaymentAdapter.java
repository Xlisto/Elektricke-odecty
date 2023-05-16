package cz.xlisto.odecty.modules.payment;

import android.annotation.SuppressLint;
import android.content.Context;
import android.transition.TransitionManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.ArrayList;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.RecyclerView;
import cz.xlisto.odecty.R;
import cz.xlisto.odecty.databaze.DataSubscriptionPointSource;
import cz.xlisto.odecty.dialogs.YesNoDialogFragment;
import cz.xlisto.odecty.format.DecimalFormatHelper;
import cz.xlisto.odecty.models.PaymentModel;
import cz.xlisto.odecty.ownview.ViewHelper;
import cz.xlisto.odecty.utils.FragmentChange;

/**
 * Xlisto 17.02.2023 20:57
 */
public class PaymentAdapter extends RecyclerView.Adapter<PaymentAdapter.MyViewHolder> {
    private static final String TAG = "PaymentAdapter";
    public static final String FLAG_PAYMENT_ADAPTER_DELETE = "flagPaymentAdapterDelete";
    private static int showButtons = -1;
    private static String selectedTable;
    private static long selectedId;
    private static int selectedPosition;
    private final ArrayList<PaymentModel> items;
    private final RecyclerView recyclerView;
    private Context context;
    private final String table;


    static class MyViewHolder extends RecyclerView.ViewHolder {
        TextView tvDate, tvPayment, tvType, tvDescription;
        LinearLayout lnButtons;
        RelativeLayout rlPaymentOut;
        Button btnEdit, btnDelete;

        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }


    public PaymentAdapter(ArrayList<PaymentModel> items, RecyclerView recyclerView, String table) {
        this.items = items;
        this.recyclerView = recyclerView;
        this.table = table;
    }


    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_payment, parent, false);
        MyViewHolder vh = new MyViewHolder(v);
        vh.rlPaymentOut = v.findViewById(R.id.rlPaymentOut);
        vh.lnButtons = v.findViewById(R.id.lnButtonsPayment);
        vh.tvDate = v.findViewById(R.id.tvCislo0);
        vh.tvPayment = v.findViewById(R.id.tvPlatba);
        vh.tvType = v.findViewById(R.id.tvDruhPlatby);
        vh.tvDescription = v.findViewById(R.id.tvDescriptionPayment);
        vh.btnEdit = v.findViewById(R.id.btnEditPayment);
        vh.btnDelete = v.findViewById(R.id.btnDeletePayment);


        context = parent.getContext();

        return vh;
    }


    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, @SuppressLint("RecyclerView") int position) {
        PaymentModel payment = items.get(position);
        holder.tvDate.setText(ViewHelper.convertLongToTime(payment.getDate()));
        holder.tvPayment.setText(DecimalFormatHelper.df2.format(payment.getPayment()) + " kč");
        holder.tvType.setText("" + payment.getTypePaymentString());
        PaymentModel.getDiscountDPHText(payment.getDiscountDPH(), holder.tvDescription);

        holder.rlPaymentOut.setOnClickListener(v1 -> {
            if (showButtons >= 0) {
                RecyclerView.ViewHolder viewHolder = recyclerView.findViewHolderForAdapterPosition(showButtons);
                if (viewHolder != null)
                    viewHolder.itemView.findViewById(R.id.lnButtonsPayment).setVisibility(View.GONE);
            }

            TransitionManager.beginDelayedTransition(recyclerView);
            if (showButtons >= 0 && showButtons == position) {
                showButtons = -1;
            } else {
                showButtons = position;
            }
            showButtons(holder, position);
        });

        holder.btnEdit.setOnClickListener(v -> FragmentChange.replace((FragmentActivity) context, PaymentEditFragment.newInstance(payment.getIdFak(), payment.getId(), table), FragmentChange.Transaction.MOVE, true));

        holder.btnDelete.setOnClickListener(v -> {
            selectedId = payment.getId();
            selectedPosition = position;
            selectedTable = table;
            YesNoDialogFragment.newInstance("Odstranit záznam platby", FLAG_PAYMENT_ADAPTER_DELETE).show(((FragmentActivity) context).getSupportFragmentManager(), TAG);
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
     * Odstraní položku z databáze a z recycleru
     */
    public void deleteItem() {
        deleteItem(selectedId, selectedPosition, selectedTable);
    }


    /**
     * Odstraní položku z databáze a z recycleru
     */
    private void deleteItem(long idPayment, int position, String table) {
        DataSubscriptionPointSource dataSubscriptionPointSource = new DataSubscriptionPointSource(context);
        dataSubscriptionPointSource.open();
        dataSubscriptionPointSource.deletePayment(idPayment, table);
        dataSubscriptionPointSource.close();
        items.remove(position);
        notifyItemRemoved(position);
        notifyItemChanged(position, items.size());
    }


    /**
     * Skryje/zobrazí tlačítka pro smazání a editaci
     *
     * @param holder   MyViewHolder
     * @param position pozice
     */
    private void showButtons(PaymentAdapter.MyViewHolder holder, int position) {

        if (showButtons == position)
            holder.lnButtons.setVisibility(View.VISIBLE);
        else
            holder.lnButtons.setVisibility(View.GONE);
    }

    public static void resetShowButtons() {
        showButtons = -1;
    }
}
