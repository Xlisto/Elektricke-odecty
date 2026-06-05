package cz.xlisto.elektrodroid.modules.invoice;


import static cz.xlisto.elektrodroid.modules.invoice.InvoiceListFragment.INVOICE_DELETE_LISTENER;
import static cz.xlisto.elektrodroid.utils.FragmentChange.Transaction.MOVE;

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

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Calendar;

import cz.xlisto.elektrodroid.R;
import cz.xlisto.elektrodroid.databaze.DataSubscriptionPointSource;
import cz.xlisto.elektrodroid.dialogs.InvoiceListEditDialogFragment;
import cz.xlisto.elektrodroid.dialogs.YesNoDialogFragment;
import cz.xlisto.elektrodroid.format.DecimalFormatHelper;
import cz.xlisto.elektrodroid.models.InvoiceListModel;
import cz.xlisto.elektrodroid.models.SubscriptionPointModel;
import cz.xlisto.elektrodroid.modules.payment.PaymentAdapter;
import cz.xlisto.elektrodroid.ownview.ViewHelper;
import cz.xlisto.elektrodroid.shp.ShPInvoiceList;
import cz.xlisto.elektrodroid.shp.ShPSubscriptionPoint;
import cz.xlisto.elektrodroid.utils.Calculation;
import cz.xlisto.elektrodroid.utils.DifferenceDate;
import cz.xlisto.elektrodroid.utils.FragmentChange;


/**
 * Adaptér pro zobrazení seznamu faktur v RecyclerView.
 * Zobrazuje základní informace o každé faktuře (číslo, datum, stav plateb a odečtů)
 * a umožňuje editaci, zobrazení detailů a smazání faktury.
 * Xlisto 01.02.2023 19:28
 */
public class InvoiceListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final String TAG = "InvoiceListAdapter";
    private final Context context;
    private final ArrayList<InvoiceListModel> items;
    private final RecyclerView recyclerView;
    private String tableFak, tableNow, tablePay, tableRead;
    private ColorStateList originalTextViewColors;
    private int showButtons = -1;
    private ShPInvoiceList shPInvoiceList;
    private long selectedIdFak;


    /**
     * ViewHolder pro zobrazení jedné položky v seznamu faktur.
     * Obsahuje reference na všechny Views v položce.
     */
    private static final class MyViewHolder extends RecyclerView.ViewHolder {

        RelativeLayout relativeLayout;
        LinearLayout lnButtons1, lnButtons2;
        TextView tvNumberInvoice, tvDateOf, tvDateTo, tvDateDifferent, tvPayments, tvReads, tvNumberInvoiceDescription;
        TextView tvVTmin, tvVTmax, tvNTmin, tvNTmax, tvVTDescription, tvNTDescription, tvInvoiceListVTDash, tvInvoiceListNTDash;
        Button btnNumberInvoice, btnShowInvoice, btnPayments, btnDeleteInvoice;
        ImageView imgAlert;

        long id;


        /**
         * Konstruktor ViewHolderu.
         *
         * @param itemView kořenový View položky
         */
        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
        }

    }


    /**
     * Konstruktor adaptéru.
     *
     * @param context      kontext aplikace
     * @param items        seznam faktur
     * @param recyclerView RecyclerView, ve kterém bude adaptér použit
     */
    public InvoiceListAdapter(Context context, ArrayList<InvoiceListModel> items, RecyclerView recyclerView) {
        this.context = context;
        this.items = items;
        this.recyclerView = recyclerView;
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
        vh.tvVTDescription = v.findViewById(R.id.tvInvoiceListVTDescription);
        vh.tvNTDescription = v.findViewById(R.id.tvInvoiceListNTDescription);
        vh.tvInvoiceListVTDash = v.findViewById(R.id.tvInvoiceListVTDash);
        vh.tvInvoiceListNTDash = v.findViewById(R.id.tvInvoiceListNTDash);
        originalTextViewColors = vh.tvDateOf.getTextColors();

        shPInvoiceList = new ShPInvoiceList(context);
        showButtons = shPInvoiceList.get(ShPInvoiceList.SHOW_BUTTONS_INVOICE_LIST, -1);

        return vh;
    }


    /**
     * Naváže data ze seznamu na ViewHolder na dané pozici.
     *
     * @param holder   ViewHolder, který se má naplnit daty
     * @param position pozice položky v seznamu
     */
    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, @SuppressLint("RecyclerView") int position) {
        MyViewHolder viewHolder = (MyViewHolder) holder;
        final InvoiceListModel invoice = items.get(position);

        viewHolder.id = invoice.getIdFak();
        loadTableName();
        viewHolder.relativeLayout.setOnClickListener(v -> {
            if (showButtons >= 0) {
                RecyclerView.ViewHolder selectedViewHolder = recyclerView.findViewHolderForAdapterPosition(showButtons);
                if (selectedViewHolder != null) {
                    selectedViewHolder.itemView.findViewById(R.id.lnButtonsInvoiceList1).setVisibility(View.GONE);
                    selectedViewHolder.itemView.findViewById(R.id.lnButtonsInvoiceList2).setVisibility(View.GONE);
                }
            }

            TransitionManager.beginDelayedTransition(recyclerView);
            if (showButtons == position)
                showButtons = -1;
            else
                showButtons = position;
            shPInvoiceList.set(ShPInvoiceList.SHOW_BUTTONS_INVOICE_LIST, showButtons);
            showButtons(viewHolder, position);
        });

        //skrytí popisu čísla faktury
        if (position == 0)
            viewHolder.tvNumberInvoiceDescription.setVisibility(View.GONE);
        else
            viewHolder.tvNumberInvoiceDescription.setVisibility(View.VISIBLE);

        viewHolder.btnNumberInvoice.setOnClickListener(v -> {
            InvoiceListEditDialogFragment invoiceEditDialogFragment = InvoiceListEditDialogFragment.newInstance(viewHolder.id, viewHolder.tvNumberInvoice.getText().toString());
            invoiceEditDialogFragment.show(((FragmentActivity) context).getSupportFragmentManager(), TAG);
        });

        viewHolder.btnPayments.setOnClickListener(v -> {
            loadTableName();
            InvoiceAdapter.resetShowButtons();
            PaymentAdapter.resetShowButtons();

            InvoiceTabFragment invoiceTabFragment = InvoiceTabFragment.newInstance(tableFak, tableNow, tablePay, tableRead, viewHolder.id, viewHolder.getBindingAdapterPosition(), MyViewPagerAdapter.TypeTabs.PAYMENT);
            FragmentChange.replace((FragmentActivity) context, invoiceTabFragment, MOVE, true);
        });

        viewHolder.btnShowInvoice.setOnClickListener(v -> {
            loadTableName();
            InvoiceAdapter.resetShowButtons();
            PaymentAdapter.resetShowButtons();

            InvoiceTabFragment invoiceTabFragment = InvoiceTabFragment.newInstance(tableFak, tableNow, tablePay, tableRead, viewHolder.id, viewHolder.getBindingAdapterPosition(), MyViewPagerAdapter.TypeTabs.INVOICE);
            FragmentChange.replace((FragmentActivity) context, invoiceTabFragment, MOVE, true);
        });

        viewHolder.btnDeleteInvoice.setOnClickListener(v -> {
            YesNoDialogFragment yesNoDialogFragment = YesNoDialogFragment.newInstance(context.getResources().getString(R.string.alert_delete_invoice), INVOICE_DELETE_LISTENER);
            yesNoDialogFragment.show(((FragmentActivity) context).getSupportFragmentManager(), TAG);

        });

        long minDate = invoice.getMinDate();
        long maxDate = invoice.getMaxDate();
        long payments = invoice.getPayments();
        long reads = invoice.getReads();

        String startDate = ViewHelper.convertLongToDate(minDate);
        String endDate = ViewHelper.convertLongToDate(maxDate);
        double differentDate = Calculation.differentMonth(startDate, endDate, DifferenceDate.TypeDate.INVOICE);
        viewHolder.tvDateOf.setText(startDate);
        viewHolder.tvDateTo.setText(endDate);
        viewHolder.tvDateDifferent.setText(context.getResources().getString(R.string.double_in_brackets, differentDate));

        if (minDate == 0 || maxDate == 0) {
            viewHolder.tvDateOf.setText(context.getResources().getString(R.string.no_date));
            viewHolder.tvDateTo.setText(context.getResources().getString(R.string.no_date));
            viewHolder.tvDateDifferent.setText("");
        }

        if (payments > 0)
            viewHolder.tvPayments.setText(context.getResources().getString(R.string.advances, payments));

        if (reads > 0)
            viewHolder.tvReads.setText(context.getResources().getString(R.string.records, reads));
        else {
            viewHolder.tvReads.setText(context.getResources().getString(R.string.no_records));
            viewHolder.btnShowInvoice.setText(context.getResources().getString(R.string.add_records));
        }
        viewHolder.tvNumberInvoice.setText(invoice.getNumberInvoice());
        viewHolder.lnButtons1.setVisibility(View.GONE);
        viewHolder.tvVTmin.setText(DecimalFormatHelper.df2.format(invoice.getMinVT()));
        viewHolder.tvVTmax.setText(DecimalFormatHelper.df2.format(invoice.getMaxVT()));
        viewHolder.tvNTmin.setText(DecimalFormatHelper.df2.format(invoice.getMinNT()));
        viewHolder.tvNTmax.setText(DecimalFormatHelper.df2.format(invoice.getMaxNT()));

        showButtons(viewHolder, position);

        viewHolder.btnNumberInvoice.setEnabled(invoice.getIdFak() != -1);
        viewHolder.btnDeleteInvoice.setEnabled(invoice.getIdFak() != -1);

        checkDate(position, viewHolder);
    }


    /**
     * Vrátí počet položek v seznamu faktur.
     *
     * @return počet faktur, nebo 0 pokud je seznam prázdný
     */
    @Override
    public int getItemCount() {
        if (items == null)
            return 0;
        return items.size();
    }


    /**
     * Zkontroluje návaznost dat (datumy, hodnoty VT/NT) mezi sousedními fakturami
     * a vizuálně zvýrazní případné nesrovnalosti.
     *
     * @param position pozice aktuální položky v seznamu
     * @param holder   ViewHolder aktuální položky
     */
    private void checkDate(int position, MyViewHolder holder) {
        InvoiceListModel nextInvoiceList, lastInvoiceList, invoiceList;
        String dateOf, dateTo, prevDate, nextDate;
        long prevDateLong, nextDateLong;
        double vtMin, ntMin, vtMax, ntMax, prevVt, prevNt, nextVt, nextNt;

        invoiceList = items.get(position);
        dateOf = ViewHelper.convertLongToDate(invoiceList.getMinDate());
        dateTo = ViewHelper.convertLongToDate(invoiceList.getMaxDate());
        prevDateLong = invoiceList.getMinDate();
        nextDateLong = invoiceList.getMaxDate();
        vtMin = invoiceList.getMinVT();
        vtMax = invoiceList.getMaxVT();
        ntMin = invoiceList.getMinNT();
        ntMax = invoiceList.getMaxNT();

        if (position > 0) {
            nextInvoiceList = items.get(position - 1);
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(nextInvoiceList.getMinDate());
            calendar.add(Calendar.DATE, -1);
            //nextDate = ViewHelper.convertLongToDate(nextInvoiceList.getMinDate() - (23 * 60 * 60 * 1000));//odečítám pouze 23 hodin - kvůli přechodu letního/zimního času
            nextDate = ViewHelper.convertLongToDate(calendar.getTimeInMillis());
            nextDateLong = calendar.getTimeInMillis();
            nextVt = nextInvoiceList.getMinVT();
            nextNt = nextInvoiceList.getMinNT();
        } else {
            nextDate = dateTo;
            nextVt = vtMax;
            nextNt = ntMax;
        }
        if (position < items.size() - 1) {
            lastInvoiceList = items.get(position + 1);
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(lastInvoiceList.getMaxDate());
            calendar.add(Calendar.DATE, 1);
            //prevDate = ViewHelper.convertLongToDate(lastInvoiceList.getMaxDate() + (25 * 60 * 60 * 1000));//přičítám 25 hodin - kvůli přechodu letního/zimního času
            prevDate = ViewHelper.convertLongToDate(calendar.getTimeInMillis());
            prevDateLong = calendar.getTimeInMillis();
            prevVt = lastInvoiceList.getMaxVT();
            prevNt = lastInvoiceList.getMaxNT();
        } else {
            prevDate = dateOf;
            prevVt = vtMin;
            prevNt = ntMin;
        }

        setTextAlertColor(holder.tvDateTo, dateTo.equals(nextDate));
        setTextAlertColor(holder.tvDateOf, dateOf.equals(prevDate));
        setTextAlertColor(holder.tvDateTo, nextDateLong >= prevDateLong);
        setTextAlertColor(holder.tvDateOf, nextDateLong >= prevDateLong);
        setTextAlertColor(holder.tvVTmin, vtMin == prevVt);
        setTextAlertColor(holder.tvVTmax, vtMax == nextVt);
        setTextAlertColor(holder.tvNTmin, ntMin == prevNt);
        setTextAlertColor(holder.tvNTmax, ntMax == nextNt);
        //zobrazení ikony alertu
        if (dateOf.equals(prevDate) && dateTo.equals(nextDate) &&
                vtMin == prevVt && vtMax == nextVt && ntMin == prevNt && ntMax == nextNt &&
                nextDateLong >= prevDateLong) {
            holder.imgAlert.setVisibility(View.GONE);
            //zobrazení ikony alertu, když je datum na 0
            if (dateOf.equals("01.01.1970") || dateTo.equals("01.01.1970")) {
                holder.imgAlert.setVisibility(View.VISIBLE);
                holder.tvDateOf.setText(context.getResources().getString(R.string.no_date));
                holder.tvDateTo.setText(context.getResources().getString(R.string.no_date));
                holder.tvDateDifferent.setText("");
                holder.tvVTmin.setVisibility(View.GONE);
                holder.tvVTmax.setVisibility(View.GONE);
                holder.tvNTmin.setVisibility(View.GONE);
                holder.tvNTmax.setVisibility(View.GONE);
                holder.tvVTDescription.setVisibility(View.GONE);
                holder.tvNTDescription.setVisibility(View.GONE);
                holder.tvInvoiceListVTDash.setVisibility(View.GONE);
                holder.tvInvoiceListNTDash.setVisibility(View.GONE);
            } else
                holder.imgAlert.setVisibility(View.GONE);
        } else {
            holder.imgAlert.setVisibility(View.VISIBLE);
            Toast.makeText(context, "Zvýrazněné záznamy na sebe nenavazují", Toast.LENGTH_LONG).show();
        }

    }


    /**
     * Nastaví barvu textu daného TextView – původní nebo výstražnou červenou.
     *
     * @param tv TextView, jehož barva se mění
     * @param b1 {@code true} = barva v pořádku (původní), {@code false} = výstražná barva
     */
    private void setTextAlertColor(TextView tv, boolean b1) {
        if (b1) {
            tv.setTextColor(originalTextViewColors);
        } else {
            tv.setTextColor(ContextCompat.getColor(context, R.color.color_red_alert));
        }
    }


    /**
     * Načte názvy tabulek pro aktuálně vybrané odběrné místo ze SharedPreferences a databáze.
     */
    private void loadTableName() {
        ShPSubscriptionPoint shPSubscriptionPoint = new ShPSubscriptionPoint(context);
        long idSubscriptionPoint = shPSubscriptionPoint.get(ShPSubscriptionPoint.ID_SUBSCRIPTION_POINT_LONG, -1L);
        if (idSubscriptionPoint == -1L) return;

        DataSubscriptionPointSource dataSubscriptionPointSource = new DataSubscriptionPointSource(context);
        dataSubscriptionPointSource.open();
        SubscriptionPointModel subscriptionPoint = dataSubscriptionPointSource.loadSubscriptionPoint(idSubscriptionPoint);
        tableFak = subscriptionPoint.getTableFAK();
        tableNow = subscriptionPoint.getTableTED();
        tablePay = subscriptionPoint.getTablePLATBY();
        tableRead = subscriptionPoint.getTableO();
        dataSubscriptionPointSource.close();
    }


    /**
     * Zobrazí nebo skryje akční tlačítka pro danou položku faktury.
     *
     * @param holder   ViewHolder položky
     * @param position pozice položky v seznamu
     */
    private void showButtons(MyViewHolder holder, int position) {
        if (position == showButtons) {
            holder.lnButtons1.setVisibility(View.VISIBLE);
            holder.lnButtons2.setVisibility(View.VISIBLE);
            selectedIdFak = holder.id;
        } else {
            holder.lnButtons1.setVisibility(View.GONE);
            holder.lnButtons2.setVisibility(View.GONE);
        }
    }


    /**
     * Vrátí id vybrané faktury
     *
     * @return id faktury
     */
    public long getSelectedIdFak() {
        return selectedIdFak;
    }


    /**
     * Odebere aktuálně vybranou položku faktury ze seznamu a upozorní adaptér na změnu.
     * Resetuje výběr a zobrazení tlačítek.
     */
    public void removeItem() {
        items.remove(showButtons);
        notifyItemRemoved(showButtons);
        selectedIdFak = -1;
        showButtons = -1;
        notifyItemChanged(0, getItemCount());
    }

}
