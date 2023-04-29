package cz.xlisto.cenik.modules.payment;

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
import cz.xlisto.cenik.R;
import cz.xlisto.cenik.databaze.DataSubscriptionPointSource;
import cz.xlisto.cenik.dialogs.YesNoDialogFragment;
import cz.xlisto.cenik.format.DecimalFormatHelper;
import cz.xlisto.cenik.models.PaymentModel;
import cz.xlisto.cenik.ownview.ViewHelper;
import cz.xlisto.cenik.utils.FragmentChange;

/**
 * Xlisto 17.02.2023 20:57
 */
public class PaymentAdapter extends RecyclerView.Adapter<PaymentAdapter.MyViewHolder> {
    private static final String TAG = "PaymentAdapter";
    private ArrayList<PaymentModel> items;
    private RecyclerView recyclerView;
    private Context context;
    private String table;
    private InvoiceAdapterListener reloadData;

    class MyViewHolder extends RecyclerView.ViewHolder {
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
        vh.lnButtons = v.findViewById(R.id.lnButtonsBackup);
        vh.tvDate = v.findViewById(R.id.tvCislo0);
        vh.tvPayment = v.findViewById(R.id.tvPlatba);
        vh.tvType = v.findViewById(R.id.tvDruhPlatby);
        vh.tvDescription = v.findViewById(R.id.tvDescriptionPayment);
        vh.btnEdit = v.findViewById(R.id.btnEditPayment);
        vh.btnDelete = v.findViewById(R.id.btnDeletePayment);

        vh.rlPaymentOut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TransitionManager.beginDelayedTransition(recyclerView);
                if (vh.lnButtons.getVisibility() == View.GONE)
                    vh.lnButtons.setVisibility(View.VISIBLE);
                else
                    vh.lnButtons.setVisibility(View.GONE);
            }
        });
        context = parent.getContext();

        return vh;
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        PaymentModel payment = items.get(position);
        holder.tvDate.setText(ViewHelper.convertLongToTime(payment.getDate()));
        holder.tvPayment.setText(DecimalFormatHelper.df2.format(payment.getPayment()) + " kč");
        holder.tvType.setText("" + payment.getTypePaymentString());
        PaymentModel.getDiscountDPHText(payment.getDiscountDPH(),holder.tvDescription);
        holder.btnEdit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FragmentChange.replace((FragmentActivity) context, PaymentEditFragment.newInstance(payment.getIdFak(), payment.getId(), table), FragmentChange.Transaction.MOVE, true);
            }
        });
        holder.btnDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                YesNoDialogFragment.newInstance(new YesNoDialogFragment.OnDialogResult() {
                    @Override
                    public void onResult(boolean b) {
                        deleteItem(payment.getId(), table);
                    }
                }, "Odstranit záznam platby").show(((FragmentActivity) context).getSupportFragmentManager(), TAG);
            }
        });
    }

    @Override
    public int getItemCount() {
        if (items == null)
            return 0;
        return items.size();
    }

    private void deleteItem(long idPayment, String table) {
        DataSubscriptionPointSource dataSubscriptionPointSource = new DataSubscriptionPointSource(context);
        dataSubscriptionPointSource.open();
        dataSubscriptionPointSource.deletePayment(idPayment, table);
        dataSubscriptionPointSource.close();
        reloadData.onUpdateData();
    }

    public void setUpdateListener(InvoiceAdapterListener listener) {
        this.reloadData = listener;
    }

    public interface InvoiceAdapterListener {
        void onUpdateData();
    }

}
