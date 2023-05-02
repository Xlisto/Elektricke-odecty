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
import java.util.Calendar;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.RecyclerView;
import cz.xlisto.odecty.R;
import cz.xlisto.odecty.databaze.DataPriceListSource;
import cz.xlisto.odecty.databaze.DataSubscriptionPointSource;
import cz.xlisto.odecty.dialogs.YesNoDialogFragment;
import cz.xlisto.odecty.format.DecimalFormatHelper;
import cz.xlisto.odecty.models.InvoiceModel;
import cz.xlisto.odecty.models.PozeModel;
import cz.xlisto.odecty.models.PriceListModel;
import cz.xlisto.odecty.models.PriceListRegulBuilder;
import cz.xlisto.odecty.models.SubscriptionPointModel;
import cz.xlisto.odecty.ownview.ViewHelper;
import cz.xlisto.odecty.utils.Calculation;
import cz.xlisto.odecty.utils.DifferenceDate;
import cz.xlisto.odecty.utils.FragmentChange;

/**
 * Adaptér pro zobrazení záznamu faktur
 * Xlisto 04.02.2023 20:03
 */
public class InvoiceAdapter extends RecyclerView.Adapter<InvoiceAdapter.MyViewHolder> {
    private static final String TAG = "InvoiceAdapter";
    private final Context context;
    private final ArrayList<InvoiceModel> items;
    private final String table;
    private InvoiceAdapterListener reloadData;
    private final SubscriptionPointModel subScriptionPoint;
    private final RecyclerView recyclerView;
    private final PozeModel.TypePoze typePoze;
    private ColorStateList originalTextViewColors;
    private boolean showNT = true;
    private PriceListModel priceList;

    private int showButtons = -1;


    static class MyViewHolder extends RecyclerView.ViewHolder {
        TextView vtStart, vtEnd, vTDif, ntStart, ntEnd, ntDif, dateStart, dateEnd, dateDifference, vtPrice, ntPrice, tvPayment, tvPOZE, tvPriceTotal, tvPriceTotalDPH, tvOtherServices, tvOtherServicesDescription, tvAlert;
        TextView tvNtDescription, tvNtDash;
        Button btnEdit, btnDelete, btnCut, btnJoin;
        RelativeLayout itemInvoice;
        LinearLayout lnButtons, lnButtons2;

        ImageView imgAlert;


        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }

    public InvoiceAdapter(Context context, ArrayList<InvoiceModel> items, String table, SubscriptionPointModel subscriptionPoint, PozeModel.TypePoze typePoze, RecyclerView recyclerView) {
        this.context = context;
        this.items = items;
        this.table = table;
        this.subScriptionPoint = subscriptionPoint;
        this.recyclerView = recyclerView;
        this.typePoze = typePoze;
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_invoice, parent, false);
        InvoiceAdapter.MyViewHolder vh = new InvoiceAdapter.MyViewHolder(v);
        vh.itemInvoice = v.findViewById(R.id.item_invoice);
        vh.lnButtons = v.findViewById(R.id.lnButtonsInvoiceItem);
        vh.lnButtons2 = v.findViewById(R.id.lnButtonsInvoiceItem2);
        vh.btnEdit = v.findViewById(R.id.btnEditInvoiceItem);
        vh.btnDelete = v.findViewById(R.id.btnDeleteInvoiceItem);
        vh.btnCut = v.findViewById(R.id.btnCutInvoiceItem);
        vh.btnJoin = v.findViewById(R.id.btnJoinInvoiceItem);
        vh.dateStart = v.findViewById(R.id.tvDateInvoiceOf);
        vh.dateEnd = v.findViewById(R.id.tvDateInvoiceTo);
        vh.dateDifference = v.findViewById(R.id.tvDateInvoiceDifference);
        vh.vtStart = v.findViewById(R.id.tvVtStart);
        vh.ntStart = v.findViewById(R.id.tvNtStart);
        vh.vtEnd = v.findViewById(R.id.tvVtEnd);
        vh.ntEnd = v.findViewById(R.id.tvNtEnd);
        vh.vTDif = v.findViewById(R.id.tvVtDifferent);
        vh.ntDif = v.findViewById(R.id.tvNtDifferent);
        vh.tvNtDescription = v.findViewById(R.id.tvNtDescription);
        vh.tvNtDash = v.findViewById(R.id.tvNtDash);
        vh.vtPrice = v.findViewById(R.id.tvVTPrice);
        vh.ntPrice = v.findViewById(R.id.tvNTPrice);
        vh.tvPayment = v.findViewById(R.id.tvPayment);
        vh.tvPOZE = v.findViewById(R.id.tvPricePoze);
        vh.tvPriceTotal = v.findViewById(R.id.tvPriceTotal);
        vh.tvPriceTotalDPH = v.findViewById(R.id.tvPriceTotalDPH);
        vh.tvOtherServices = v.findViewById(R.id.tvOtherServices);
        vh.tvOtherServicesDescription = v.findViewById(R.id.tvOtherServicesDescription);
        vh.imgAlert = v.findViewById(R.id.imageViewWarningInvoice);
        vh.tvAlert = v.findViewById(R.id.tvAlertInvoice);
        originalTextViewColors = vh.vtStart.getTextColors();
        return vh;
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, @SuppressLint("RecyclerView") int position) {
        InvoiceModel invoice = items.get(position);

        DataPriceListSource dataPriceListSource = new DataPriceListSource(context);
        dataPriceListSource.open();
        priceList = dataPriceListSource.readPrice(invoice.getIdPriceList());
        dataPriceListSource.close();

        hideNt(holder, priceList.getSazba().equals(InvoiceAbstract.D01) || priceList.getSazba().equals(InvoiceAbstract.D02));

        //nastavení datumu odečtu
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(invoice.getDateFrom());
        int[] date = ViewHelper.parseIntsFromCalendar(calendar);

        //nastavení regulovaného ceníku
        PriceListRegulBuilder priceListRegulBuilder = new PriceListRegulBuilder(priceList, date[2], date[1], date[0]);
        priceList = priceListRegulBuilder.getRegulPriceList();

        checkDate(position, holder, priceListRegulBuilder);


        String dateStart = ViewHelper.convertLongToTime(invoice.getDateFrom());
        String dateEnd = ViewHelper.convertLongToTime(invoice.getDateTo());
        double differentDate = Calculation.differentMonth(dateStart, dateEnd, DifferenceDate.TypeDate.INVOICE);
        holder.dateStart.setText(dateStart);
        holder.dateEnd.setText(dateEnd);
        holder.dateDifference.setText("(" + differentDate + ")");

        holder.vtStart.setText(DecimalFormatHelper.df2.format(invoice.getVtStart()));
        holder.ntStart.setText(DecimalFormatHelper.df2.format(invoice.getNtStart()));
        holder.vtEnd.setText(DecimalFormatHelper.df2.format(invoice.getVtEnd()));
        holder.ntEnd.setText(DecimalFormatHelper.df2.format(invoice.getNtEnd()));
        double vtDif = invoice.getVtEnd() - invoice.getVtStart();
        double ntDif = invoice.getNtEnd() - invoice.getNtStart();
        double[] price = Calculation.calculatePriceWithoutPozeKwh(priceList, subScriptionPoint);
        double vtTotal = price[0] * vtDif;
        double ntTotal = price[1] * ntDif;
        double paymentTotal = price[2] * differentDate;
        double poze = Calculation.getPozeByType(priceList, subScriptionPoint.getCountPhaze(), subScriptionPoint.getPhaze(), (vtDif + ntDif) / 1000, differentDate, typePoze);
        double otherServices = differentDate * invoice.getOtherServices();

        holder.vTDif.setText("Sp.: " + DecimalFormatHelper.df2.format(vtDif) + " kWh");
        holder.ntDif.setText("Sp.: " + DecimalFormatHelper.df2.format(ntDif) + " kWh");
        holder.vtPrice.setText("" + DecimalFormatHelper.df2.format(vtTotal) + " kč");
        holder.ntPrice.setText("" + DecimalFormatHelper.df2.format(ntTotal) + " kč");
        holder.tvPayment.setText("Stálý plat: " + DecimalFormatHelper.df2.format(paymentTotal) + "kč");
        holder.tvPOZE.setText("POZE (OZE): " + DecimalFormatHelper.df2.format(poze) + " kč");
        holder.tvOtherServices.setText(" " + DecimalFormatHelper.df2.format(otherServices) + " kč");

        double total = vtTotal + ntTotal + paymentTotal + poze + otherServices;
        double totalDPH = total + (total * priceList.getDph() / 100);
        holder.tvPriceTotal.setText("Celkem: " + DecimalFormatHelper.df2.format(total) + " kč bez DPH");
        holder.tvPriceTotalDPH.setText(DecimalFormatHelper.df2.format(totalDPH) + " kč s DPH");


        holder.itemInvoice.setOnClickListener(v -> {
            if (showButtons >= 0) {
                RecyclerView.ViewHolder viewHolder = recyclerView.findViewHolderForAdapterPosition(showButtons);
                if (viewHolder != null) {
                    viewHolder.itemView.findViewById(R.id.lnButtonsInvoiceItem).setVisibility(View.GONE);
                    viewHolder.itemView.findViewById(R.id.lnButtonsInvoiceItem2).setVisibility(View.GONE);
                }
            }
            if (showButtons == position)
                showButtons = -1;
            else
                showButtons = position;

            showButtons(holder, invoice, position);
        });

        holder.btnEdit.setOnClickListener(v -> {
            InvoiceEditFragment invoiceEditFragment = InvoiceEditFragment.newInstance(table, invoice.getId());
            FragmentChange.replace((FragmentActivity) context, invoiceEditFragment, FragmentChange.Transaction.MOVE, true);
        });

        holder.btnCut.setOnClickListener(v -> {
            InvoiceCutDialogFragment invoiceCutDialogFragment = InvoiceCutDialogFragment.newInstance(invoice.getDateFrom(), invoice.getDateTo(),
                    invoice.getVtStart(), invoice.getVtEnd(), invoice.getNtStart(), invoice.getNtEnd(), showNT, priceList.getId(), invoice.getId(), invoice.getOtherServices(), table);
            invoiceCutDialogFragment.setOnCutListener(b -> {
                if (b) {
                    notifyDataSetChanged();
                    reloadData.onUpdateData();
                }

            });
            invoiceCutDialogFragment.show(((FragmentActivity) context).getSupportFragmentManager(), TAG);
        });

        holder.btnDelete.setOnClickListener(v -> YesNoDialogFragment.newInstance(b -> {
            if (b)
                deleteItem(invoice.getId(), position);
        }, "Smazat záznam z faktury").show(((FragmentActivity) context).getSupportFragmentManager(), TAG));

        holder.lnButtons.setVisibility(View.GONE);
        holder.lnButtons2.setVisibility(View.GONE);
        if (invoice.getIdInvoice() == -1L) {
            holder.btnDelete.setVisibility(View.GONE);
        }

        if (otherServices == 0) {
            holder.tvOtherServicesDescription.setVisibility(View.GONE);
            holder.tvOtherServices.setVisibility(View.GONE);
        } else {
            holder.tvOtherServicesDescription.setVisibility(View.VISIBLE);
            holder.tvOtherServices.setVisibility(View.VISIBLE);
        }

        showButtons(holder, invoice, position);
    }

    @Override
    public int getItemCount() {
        if (items == null)
            return 0;
        return items.size();
    }

    private void showButtons(MyViewHolder holder, InvoiceModel invoice, int position) {
        TransitionManager.beginDelayedTransition(recyclerView);
        if (showButtons == position) {
            holder.lnButtons.setVisibility(View.VISIBLE);
            //skrytí rozdělovacích tlačítek pro jiné faktury než aktuální období bez faktury
            if (invoice.getIdInvoice() == -1L) {
                holder.lnButtons2.setVisibility(View.VISIBLE);
            }
        } else {
            holder.lnButtons.setVisibility(View.GONE);
            holder.lnButtons2.setVisibility(View.GONE);
        }
    }

    /**
     * Smaže položku s daným ID z databáze a aktualizuje adaptér a posluchače dat.
     *
     * @param id ID položky, kterou chceme smazat z databáze
     */
    private void deleteItem(long id,int position) {
        DataSubscriptionPointSource dataSubscriptionPointSource = new DataSubscriptionPointSource(context);
        dataSubscriptionPointSource.open();
        dataSubscriptionPointSource.deleteInvoice(id, table);
        dataSubscriptionPointSource.close();
        showButtons = -1;
        items.remove(position);
        notifyItemRemoved(position);
        notifyItemRangeChanged(position-1, getItemCount());
        WithOutInvoiceService.editFirstItemInInvoice(context);
    }

    /**
     * Kontroluje data a nastavuje zvýraznění a ikonu alertu v každé položce seznamu faktur.
     * Funkce nejdříve načte údaje o faktuře, datumy, začáteční a koncové hodnoty pro VT a NT.
     * Poté zkontroluje, zda je aktuální položka první nebo poslední v seznamu faktur, a načte údaje
     * o předchozí a následující položce. Pokud předchozí nebo následující faktura neexistuje,
     * funkce nastaví datum a hodnoty VT a NT na aktuální fakturu. Funkce pak zvýrazní datum
     * a hodnoty VT a NT, pokud nejsou stejné jako předchozí nebo následující faktura, a zviditělní
     * ikonu alertu..
     *
     * @param position aktuální pozice záznamu
     * @param holder   aktuální záznam
     */
    private void checkDate(int position, MyViewHolder holder, PriceListRegulBuilder
            priceListRegulBuilder) {
        InvoiceModel invoice, prevInvoice, nextInvoice;
        String dateOf, dateTo, prevDate, nextDate;
        double vtStart, ntStart, vtEnd, ntEnd, prevVt, prevNt, nextVt, nextNt;
        invoice = items.get(position);

        dateTo = ViewHelper.convertLongToTime(invoice.getDateTo());
        dateOf = ViewHelper.convertLongToTime(invoice.getDateFrom());
        vtStart = invoice.getVtStart();
        ntStart = invoice.getNtStart();
        vtEnd = invoice.getVtEnd();
        ntEnd = invoice.getNtEnd();
        if (position > 0) {
            nextInvoice = items.get(position - 1);
            nextDate = ViewHelper.convertLongToTime(nextInvoice.getDateFrom() - (23 * 60 * 60 * 1000));//odečítám pouze 23 hodin - kvůli přechodu letního/zimního času
            nextVt = nextInvoice.getVtStart();
            nextNt = nextInvoice.getNtStart();
            //Log.w(TAG, "Invoice date 2: " + ViewHelper.convertLongToTime(invoice.getDateTo()) + " " + ViewHelper.convertLongToTime(nextInvoice.getDateOf() - (24 * 60 * 60 * 1000)));
        } else {
            nextDate = dateTo;
            nextVt = vtEnd;
            nextNt = ntEnd;
            //Log.w(TAG, "Invoice date  :" + prevDate + " " + nextDate);
        }
        if (position < items.size() - 1) {
            prevInvoice = items.get(position + 1);
            prevDate = ViewHelper.convertLongToTime(prevInvoice.getDateTo() + (25 * 60 * 60 * 1000));//přičítám 25 hodin - kvůli přechodu letního/zimného času
            prevVt = prevInvoice.getVtEnd();
            prevNt = prevInvoice.getNtEnd();
            //Log.w(TAG, "Invoice date 1: " + ViewHelper.convertLongToTime(invoice.getDateOf()) + " " + ViewHelper.convertLongToTime(prevInvoice.getDateTo() + (24 * 60 * 60 * 1000)));
        } else {
            prevDate = dateOf;
            prevVt = vtStart;
            prevNt = ntStart;
        }
        //zvýrazenění datumu
        setTextAlertColor(holder.dateStart, dateOf.equals(prevDate));
        setTextAlertColor(holder.dateEnd, dateTo.equals(nextDate));
        setTextAlertColor(holder.vtStart, vtStart == prevVt);
        setTextAlertColor(holder.vtEnd, vtEnd == nextVt);
        setTextAlertColor(holder.ntStart, ntStart == prevNt);
        setTextAlertColor(holder.ntEnd, ntEnd == nextNt);
        //Log.w(TAG, "Invoice vt: " + vtStart + "=" + prevVt + " ; " + vtEnd + "=" + nextVt);

        //zobrezení ikony alertu
        if (dateOf.equals(prevDate) && dateTo.equals(nextDate) && vtStart == prevVt && vtEnd == nextVt && ntStart == prevNt && ntEnd == nextNt && !isOverDateRegulPrice(priceListRegulBuilder, invoice)) {
            holder.imgAlert.setVisibility(View.GONE);
        } else {
            holder.imgAlert.setVisibility(View.VISIBLE);
            Toast.makeText(context, "Zvýrazněné záznamy na sebe nenavazují", Toast.LENGTH_LONG).show();
        }
        //zobrezení varovného textu
        if (!isOverDateRegulPrice(priceListRegulBuilder, invoice))
            holder.tvAlert.setVisibility(View.GONE);
        else {
            holder.tvAlert.setVisibility(View.VISIBLE);
            holder.tvAlert.setTextColor(context.getResources().getColor(R.color.color_no));
        }
    }

    /**
     * Zkontroluje období odečtu s obdobím termínů úlev. Pokud se překrývají vrátí true
     *
     * @param priceListRegulBuilder Ceník s regulovanými cenami
     * @param invoice              Faktura
     * @return true pokud se období překrývají
     */
    private boolean isOverDateRegulPrice(PriceListRegulBuilder
                                                 priceListRegulBuilder, InvoiceModel invoice) {
        long startRegulPrice = priceListRegulBuilder.getDateStart();
        long endRegulPrice = priceListRegulBuilder.getDateEnd();
        long dateStartMonthlyReading = invoice.getDateFrom();
        long dateEndMonthlyReading = invoice.getDateTo();
        //Začátek regulace musí být větší  než začátek odečtu a zároveň začátek regulace menší nebo roven než konec odečtu
        //nebo
        //Konec regulace musí být větší či rovno než začátek měsíčního odečtu a zároveň konec regulace musí být menší než konce měsíčního odečtu
        return ((startRegulPrice > dateStartMonthlyReading) && (startRegulPrice <= dateEndMonthlyReading))
                || ((endRegulPrice >= dateStartMonthlyReading) && (endRegulPrice < dateEndMonthlyReading));
    }

    /**
     * Nastaví barvu textového pole pro upozornění na určitou akci.
     *
     * @param tv TextView, u kterého chceme změnit barvu
     * @param b1 Pokud je true, barva textu bude původní. Pokud je false, barva textu bude nastavena na barvu pro upozornění.
     */
    private void setTextAlertColor(TextView tv, boolean b1) {
        if (b1) {
            tv.setTextColor(originalTextViewColors);
        } else {
            tv.setTextColor(context.getResources().getColor(R.color.color_no));
        }
    }

    /**
     * Nastaví posluchače pro aktualizaci dat v adaptéru pro faktury.
     *
     * @param listener Instance třídy InvoiceAdapterListener, která bude sloužit jako posluchač pro aktualizaci dat
     */
    public void setUpdateListener(InvoiceAdapterListener listener) {
        this.reloadData = listener;
    }


    /**
     * Skryje nebo zobrazí prvky v adapterovém zobrazení položky, které obsahují informace o noční tarifů.
     *
     * @param holder Instance třídy MyViewHolder, která obsahuje odkazy na jednotlivé prvky v adapterovém zobrazení položky
     * @param show   Pokud je true, prvky budou skryty, jinak budou zobrazeny
     */
    private void hideNt(MyViewHolder holder, boolean show) {
        if (!show) {
            holder.tvNtDescription.setVisibility(View.VISIBLE);
            holder.ntStart.setVisibility(View.VISIBLE);
            holder.tvNtDash.setVisibility(View.VISIBLE);
            holder.ntEnd.setVisibility(View.VISIBLE);
            holder.ntPrice.setVisibility(View.VISIBLE);
            holder.ntDif.setVisibility(View.VISIBLE);
        } else {
            holder.tvNtDescription.setVisibility(View.GONE);
            holder.ntStart.setVisibility(View.GONE);
            holder.tvNtDash.setVisibility(View.GONE);
            holder.ntEnd.setVisibility(View.GONE);
            holder.ntPrice.setVisibility(View.GONE);
            holder.ntDif.setVisibility(View.GONE);
        }
    }

    public interface InvoiceAdapterListener {
        void onUpdateData();
    }

}
