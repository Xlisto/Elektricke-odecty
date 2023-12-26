package cz.xlisto.odecty.modules.invoice;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.ColorStateList;
import android.transition.TransitionManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.RecyclerView;
import cz.xlisto.odecty.R;
import cz.xlisto.odecty.databaze.DataSubscriptionPointSource;
import cz.xlisto.odecty.format.DecimalFormatHelper;
import cz.xlisto.odecty.models.InvoiceListModel;
import cz.xlisto.odecty.models.SubscriptionPointModel;
import cz.xlisto.odecty.modules.payment.PaymentAdapter;
import cz.xlisto.odecty.ownview.ViewHelper;
import cz.xlisto.odecty.shp.ShPInvoiceList;
import cz.xlisto.odecty.shp.ShPSubscriptionPoint;
import cz.xlisto.odecty.utils.Calculation;
import cz.xlisto.odecty.utils.DifferenceDate;
import cz.xlisto.odecty.utils.FragmentChange;

import static cz.xlisto.odecty.utils.FragmentChange.Transaction.MOVE;

/**
 * Seznam faktur
 * Xlisto 01.02.2023 19:28
 */
public class InvoiceListAdapter extends RecyclerView.Adapter<InvoiceListAdapter.MyViewHolder> {
    private static final String TAG = "InvoiceListAdapter";
    private final Context context;
    private final ArrayList<InvoiceListModel> items;
    private final RecyclerView recyclerView;
    private String tableFak, tableNow, tablePay;
    private ColorStateList originalTextViewColors;
    private int showButtons = -1;
    private ShPInvoiceList shPInvoiceList;


    static class MyViewHolder extends RecyclerView.ViewHolder {

        RelativeLayout relativeLayout;
        LinearLayout lnButtons1, lnButtons2;
        TextView tvNumberInvoice, tvDateOf, tvDateTo, tvDateDifferent, tvPayments, tvReads, tvNumberInvoiceDescription;
        TextView tvVTmin, tvVTmax, tvNTmin, tvNTmax;
        Button btnNumberInvoice, btnShowInvoice, btnPayments, btnDeleteInvoice;
        ImageView imgAlert;

        long id;

        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
        }

    }


    public InvoiceListAdapter(Context context, ArrayList<InvoiceListModel> items, RecyclerView recyclerView) {
        this.context = context;
        this.items = items;
        this.recyclerView = recyclerView;
    }


    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_invoice_list, parent, false);
        MyViewHolder vh = new MyViewHolder(v);
        vh.relativeLayout = v.findViewById(R.id.item_invoice_list);
        vh.tvDateOf = v.findViewById(R.id.tvDateInvoiceListOf);
        vh.tvDateTo = v.findViewById(R.id.tvDateInvoiceListTo);
        vh.tvDateDifferent = v.findViewById(R.id.tvDateInvoiceListDifferent);
        vh.lnButtons1 = v.findViewById(R.id.lnButtonsInvoiceList1);
        vh.lnButtons2 = v.findViewById(R.id.lnButtonsInvoiceList2);
        vh.btnNumberInvoice = v.findViewById(R.id.btnEditInvoiceList);
        vh.tvNumberInvoiceDescription = v.findViewById(R.id.tvNumberInvoiceListDescription);
        vh.btnShowInvoice = v.findViewById(R.id.btnShowInvoiceList);
        vh.btnPayments = v.findViewById(R.id.btnShowPaymentList);
        vh.btnDeleteInvoice = v.findViewById(R.id.btnDeleteInvoiceList);
        vh.tvNumberInvoice = v.findViewById(R.id.tvNumberInvoiceList);
        vh.tvPayments = v.findViewById(R.id.tvPayments);
        vh.tvReads = v.findViewById(R.id.tvReads);
        vh.imgAlert = v.findViewById(R.id.imageViewWarningInvoiceList);
        vh.tvVTmin = v.findViewById(R.id.tvInvoiceListVTMin);
        vh.tvVTmax = v.findViewById(R.id.tvInvoiceListVTMax);
        vh.tvNTmin = v.findViewById(R.id.tvInvoiceListNTMin);
        vh.tvNTmax = v.findViewById(R.id.tvInvoiceListNTMax);
        originalTextViewColors = vh.tvDateOf.getTextColors();

        shPInvoiceList = new ShPInvoiceList(context);
        showButtons = shPInvoiceList.get(ShPInvoiceList.SHOW_BUTTONS_INVOICE_LIST, -1);

        return vh;
    }


    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, @SuppressLint("RecyclerView") int position) {
        final InvoiceListModel invoice = items.get(position);
        checkDate(position, holder);
        holder.id = invoice.getIdFak();
        loadTableName();
        holder.relativeLayout.setOnClickListener(v -> {
            if (showButtons >= 0) {
                RecyclerView.ViewHolder viewHolder = recyclerView.findViewHolderForAdapterPosition(showButtons);
                if (viewHolder != null) {
                    viewHolder.itemView.findViewById(R.id.lnButtonsInvoiceList1).setVisibility(View.GONE);
                    viewHolder.itemView.findViewById(R.id.lnButtonsInvoiceList2).setVisibility(View.GONE);
                }
            }

            TransitionManager.beginDelayedTransition(recyclerView);
            if (showButtons == position)
                showButtons = -1;
            else
                showButtons = position;
            shPInvoiceList.set(ShPInvoiceList.SHOW_BUTTONS_INVOICE_LIST, showButtons);
            showButtons(holder, position);
        });

        //skrytí popisu čísla faktury
        if (position == 0)
            holder.tvNumberInvoiceDescription.setVisibility(View.GONE);
        else
            holder.tvNumberInvoiceDescription.setVisibility(View.VISIBLE);

        holder.btnNumberInvoice.setOnClickListener(v -> {
            InvoiceListEditDialogFragment invoiceEditDialogFragment = InvoiceListEditDialogFragment.newInstance(holder.id, holder.tvNumberInvoice.getText().toString());
            invoiceEditDialogFragment.show(((FragmentActivity) context).getSupportFragmentManager(), TAG);
        });


        holder.btnPayments.setOnClickListener(v -> {
            loadTableName();
            InvoiceAdapter.resetShowButtons();
            PaymentAdapter.resetShowButtons();

            InvoiceTabFragment invoiceTabFragment = InvoiceTabFragment.newInstance(tableFak, tableNow, tablePay, holder.id, holder.getBindingAdapterPosition(), MyViewPagerAdapter.TypeTabs.PAYMENT);
            FragmentChange.replace((FragmentActivity) context, invoiceTabFragment, MOVE, true);
        });


        holder.btnShowInvoice.setOnClickListener(v -> {
            loadTableName();
            InvoiceAdapter.resetShowButtons();
            PaymentAdapter.resetShowButtons();

            InvoiceTabFragment invoiceTabFragment = InvoiceTabFragment.newInstance(tableFak, tableNow, tablePay, holder.id, holder.getBindingAdapterPosition(), MyViewPagerAdapter.TypeTabs.INVOICE);
            FragmentChange.replace((FragmentActivity) context, invoiceTabFragment, MOVE, true);
        });
        long minDate = invoice.getMinDate();
        long maxDate = invoice.getMaxDate();
        long payments = invoice.getPayments();
        long reads = invoice.getReads();


        String startDate = ViewHelper.convertLongToDate(minDate);
        String endDate = ViewHelper.convertLongToDate(maxDate);
        double differentDate = Calculation.differentMonth(startDate, endDate, DifferenceDate.TypeDate.INVOICE);
        holder.tvDateOf.setText(startDate);
        holder.tvDateTo.setText(endDate);
        holder.tvDateDifferent.setText(context.getResources().getString(R.string.double_in_brackets, differentDate));

        if (payments > 0)
            holder.tvPayments.setText(context.getResources().getString(R.string.advances, payments));

        if (reads > 0)
            holder.tvReads.setText(context.getResources().getString(R.string.records, reads));
        else {
            holder.tvReads.setText(context.getResources().getString(R.string.no_records));
            holder.btnShowInvoice.setText(context.getResources().getString(R.string.add_records));
        }
        holder.tvNumberInvoice.setText(invoice.getNumberInvoice());
        holder.lnButtons1.setVisibility(View.GONE);
        holder.tvVTmin.setText(DecimalFormatHelper.df2.format(invoice.getMinVT()));
        holder.tvVTmax.setText(DecimalFormatHelper.df2.format(invoice.getMaxVT()));
        holder.tvNTmin.setText(DecimalFormatHelper.df2.format(invoice.getMinNT()));
        holder.tvNTmax.setText(DecimalFormatHelper.df2.format(invoice.getMaxNT()));

        showButtons(holder, position);

        if(invoice.getIdFak() == -1)
            holder.btnNumberInvoice.setEnabled(false);
        else
            holder.btnNumberInvoice.setEnabled(true);
    }


    @Override
    public int getItemCount() {
        if (items == null)
            return 0;
        return items.size();
    }


    private void checkDate(int position, MyViewHolder holder) {
        InvoiceListModel nextInvoiceList, lastInvoiceList, invoiceList;
        String dateOf, dateTo, prevDate, nextDate;
        double vtMin, ntMin, vtMax, ntMax, prevVt, prevNt, nextVt, nextNt;

        invoiceList = items.get(position);
        dateOf = ViewHelper.convertLongToDate(invoiceList.getMinDate());
        dateTo = ViewHelper.convertLongToDate(invoiceList.getMaxDate());
        vtMin = invoiceList.getMinVT();
        vtMax = invoiceList.getMaxVT();
        ntMin = invoiceList.getMinNT();
        ntMax = invoiceList.getMaxNT();

        if (position > 0) {
            nextInvoiceList = items.get(position - 1);
            nextDate = ViewHelper.convertLongToDate(nextInvoiceList.getMinDate() - (23 * 60 * 60 * 1000));//odečítám pouze 23 hodin - kvůli přechodu letního/zimního času
            nextVt = nextInvoiceList.getMinVT();
            nextNt = nextInvoiceList.getMinNT();
        } else {
            nextDate = dateTo;
            nextVt = vtMax;
            nextNt = ntMax;
        }
        if (position < items.size() - 1) {
            lastInvoiceList = items.get(position + 1);
            prevDate = ViewHelper.convertLongToDate(lastInvoiceList.getMaxDate() + (25 * 60 * 60 * 1000));//přičítám 25 hodin - kvůli přechodu letního/zimního času
            prevVt = lastInvoiceList.getMaxVT();
            prevNt = lastInvoiceList.getMaxNT();
        } else {
            prevDate = dateOf;
            prevVt = vtMin;
            prevNt = ntMin;
        }

        setTextAlertColor(holder.tvDateTo, dateTo.equals(nextDate));
        setTextAlertColor(holder.tvDateOf, dateOf.equals(prevDate));
        setTextAlertColor(holder.tvVTmin, vtMin == prevVt);
        setTextAlertColor(holder.tvVTmax, vtMax == nextVt);
        setTextAlertColor(holder.tvNTmin, ntMin == prevNt);
        setTextAlertColor(holder.tvNTmax, ntMax == nextNt);

        //zobrazení ikony alertu
        if (dateOf.equals(prevDate) && dateTo.equals(nextDate) && vtMin == prevVt && vtMax == nextVt && ntMin == prevNt && ntMax == nextNt)
            holder.imgAlert.setVisibility(View.GONE);
        else {
            holder.imgAlert.setVisibility(View.VISIBLE);
            Toast.makeText(context, "Zvýrazněné záznamy na sebe nenavazují", Toast.LENGTH_LONG).show();
        }

        //zobrazení ikony alertu, když je datum na 0
        if (dateOf.equals("01.01.1970") || dateTo.equals("01.01.1970"))
            holder.imgAlert.setVisibility(View.VISIBLE);
        else
            holder.imgAlert.setVisibility(View.GONE);
    }


    private void setTextAlertColor(TextView tv, boolean b1) {
        if (b1) {
            tv.setTextColor(originalTextViewColors);
        } else {
            tv.setTextColor(context.getResources().getColor(R.color.color_red_alert));
        }
    }


    private void loadTableName() {
        ShPSubscriptionPoint shPSubscriptionPoint = new ShPSubscriptionPoint(context);
        long idSubscriptionPoint = shPSubscriptionPoint.get(ShPSubscriptionPoint.ID_SUBSCRIPTION_POINT, -1L);
        if (idSubscriptionPoint == -1L) return;

        DataSubscriptionPointSource dataSubscriptionPointSource = new DataSubscriptionPointSource(context);
        dataSubscriptionPointSource.open();
        SubscriptionPointModel subscriptionPoint = dataSubscriptionPointSource.loadSubscriptionPoint(idSubscriptionPoint);
        tableFak = subscriptionPoint.getTableFAK();
        tableNow = subscriptionPoint.getTableTED();
        tablePay = subscriptionPoint.getTablePLATBY();
        dataSubscriptionPointSource.close();
    }


    private void showButtons(MyViewHolder holder, int position) {
        if (position == showButtons) {
            holder.lnButtons1.setVisibility(View.VISIBLE);
            holder.lnButtons2.setVisibility(View.VISIBLE);
        } else {
            holder.lnButtons1.setVisibility(View.GONE);
            holder.lnButtons2.setVisibility(View.GONE);
        }
    }

}
