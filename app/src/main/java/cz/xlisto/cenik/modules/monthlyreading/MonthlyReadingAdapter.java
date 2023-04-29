package cz.xlisto.cenik.modules.monthlyreading;

import android.annotation.SuppressLint;
import android.content.Context;
import android.text.Html;
import android.transition.TransitionManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Calendar;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.RecyclerView;
import cz.xlisto.cenik.R;
import cz.xlisto.cenik.databaze.DataPriceListSource;
import cz.xlisto.cenik.databaze.DataSubscriptionPointSource;
import cz.xlisto.cenik.dialogs.YesNoDialogFragment;
import cz.xlisto.cenik.format.DecimalFormatHelper;
import cz.xlisto.cenik.models.MonthlyReadingModel;
import cz.xlisto.cenik.models.PriceListModel;
import cz.xlisto.cenik.models.PriceListRegulBuilder;
import cz.xlisto.cenik.models.SubscriptionPointModel;
import cz.xlisto.cenik.modules.invoice.WithOutInvoiceService;
import cz.xlisto.cenik.ownview.ViewHelper;
import cz.xlisto.cenik.utils.Calculation;
import cz.xlisto.cenik.utils.DifferenceDate;
import cz.xlisto.cenik.utils.FragmentChange;

import static cz.xlisto.cenik.utils.FragmentChange.Transaction.MOVE;

public class MonthlyReadingAdapter extends RecyclerView.Adapter<MonthlyReadingAdapter.MyViewHolder> {
    private final String TAG = getClass().getName() + " ";
    private OnClickItemListener onClickItemListener;
    private OnLongClickItemListener onLongClickItemListener;
    private Context context;
    private ArrayList<MonthlyReadingModel> items;
    private boolean simplyView, showRegulPrice;
    private SubscriptionPointModel subscriptionPoint;
    private RecyclerView recyclerView;
    private int showButtons = -1;

    static class MyViewHolder extends RecyclerView.ViewHolder {
        RelativeLayout rootRelativeLayout, rl3;
        LinearLayout lnButtons;
        TextView tvDate, tvVt, tvNt, tvPayment, tvDescription, tvPriceList, tvNtDif, tvVtDif, tvVtPrice, tvNtPrice, tvPozePrice, tvMonth,
                tvMonthPrice, tvTotalPrice, tvDifferentPrice, tvPaymentDescription, tvNtDescription, tvNextServicesDescription, tvNextServicesPrice, tvAlertRegulPrice;
        ImageView ivIconResult, ivWarning;
        Button btnEdit, btnDelete;

        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }

    //konstruktor
    public MonthlyReadingAdapter(Context context, ArrayList<MonthlyReadingModel> items, SubscriptionPointModel subscriptionPoint, boolean simplyView, boolean showRegulPrice, RecyclerView recyclerView, OnClickItemListener onClickItemListener, OnLongClickItemListener onLongClickItemListener) {
        this.onClickItemListener = onClickItemListener;
        this.onLongClickItemListener = onLongClickItemListener;
        this.context = context;
        this.items = items;
        this.simplyView = simplyView;
        this.subscriptionPoint = subscriptionPoint;
        this.showRegulPrice = showRegulPrice;
        this.recyclerView = recyclerView;
    }

    // Vytvoření a inicializace objektu View z XML návrhu vzoru položky.
    // Příprava kontejneru, ve kterém budou zobrazena data jednotlivé položky
    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_monthly_reading, parent, false);
        MyViewHolder vh = new MyViewHolder(v);
        vh.rootRelativeLayout = v.findViewById(R.id.rlItemMonthlyReading);
        vh.lnButtons = v.findViewById(R.id.lnButtonsMonthlyItem);
        vh.rl3 = v.findViewById(R.id.rl3);
        vh.tvDate = v.findViewById(R.id.tvDate);
        vh.tvPriceList = v.findViewById(R.id.tvTarif);
        vh.tvVt = v.findViewById(R.id.tvVT);
        vh.tvNt = v.findViewById(R.id.tvNT);
        vh.tvVtDif = v.findViewById(R.id.tvVTrozdil);
        vh.tvNtDif = v.findViewById(R.id.tvNTrozdil);
        vh.tvNtDescription = v.findViewById(R.id.tvNTDescription);
        vh.tvPozePrice = v.findViewById(R.id.tvPozePrice);
        vh.tvPayment = v.findViewById(R.id.tvPaymentPrice);
        vh.tvPaymentDescription = v.findViewById(R.id.tvPaymentDescription);
        vh.tvVtPrice = v.findViewById(R.id.tvItemVTPrice);
        vh.tvNtPrice = v.findViewById(R.id.tvItemNTPrice);
        vh.tvMonth = v.findViewById(R.id.tvDays);
        vh.tvMonthPrice = v.findViewById(R.id.tvPriceDays);
        vh.tvTotalPrice = v.findViewById(R.id.tvTotalSum);
        vh.tvDifferentPrice = v.findViewById(R.id.tvDifferentPrice);
        vh.ivIconResult = v.findViewById(R.id.imageViewMonthlyReading);
        vh.ivWarning = v.findViewById(R.id.imageViewWarningMonthlyReading);
        vh.tvNextServicesDescription = v.findViewById(R.id.tvNextServicesDescription);
        vh.tvNextServicesPrice = v.findViewById(R.id.tvNextServicesPrice);
        vh.tvAlertRegulPrice = v.findViewById(R.id.tvAlertRegulPrice);
        vh.btnEdit = v.findViewById(R.id.btnEditMonthlyItem);
        vh.btnDelete = v.findViewById(R.id.btnDeleteMonthlyItem);
        //vh.tvDescription = v.findViewById(R.id.tvPo);
        return vh;
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        final MonthlyReadingModel monthlyReading = items.get(position);
        MonthlyReadingModel monthlyReadingPrevious;


        DataPriceListSource dataPriceListSource = new DataPriceListSource(context);
        dataPriceListSource.open();
        PriceListModel priceList = dataPriceListSource.readPrice(items.get(position).getPriceListId());
        dataPriceListSource.close();


        holder.tvDate.setText(ViewHelper.convertLongToTime(monthlyReading.getDate()));

        if (priceList != null) {
            holder.tvPriceList.setText(priceList.getName());
        } else
            holder.tvPriceList.setText(context.getResources().getString(R.string.vyberteCenik));
        holder.tvVt.setText(DecimalFormatHelper.df2.format(monthlyReading.getVt()));
        holder.tvNt.setText(DecimalFormatHelper.df2.format(monthlyReading.getNt()));
        holder.tvPayment.setText(DecimalFormatHelper.df2.format(monthlyReading.getPayment()) + " Kč");
        holder.tvAlertRegulPrice.setVisibility(View.GONE);

        //výpočet
        if ((position + 1) < items.size()) {
            String discoutDescription = " Kč s DPH";
            double vtDif = monthlyReading.getVt() - items.get(position + 1).getVt();
            double ntDif = monthlyReading.getNt() - items.get(position + 1).getNt();
            double[] prices = new double[]{0, 0, 0, 0};
            double month = 0;
            double monthPrice = 0;
            double total = 0;
            double different = 0;
            monthlyReadingPrevious = items.get(position + 1);
            month = Calculation.differentMonth(ViewHelper.convertLongToTime(items.get(position + 1).getDate()), holder.tvDate.getText().toString(), DifferenceDate.TypeDate.MONTH);
            if (priceList != null) {
                holder.ivWarning.setVisibility(View.GONE);
                if (showRegulPrice) {
                    Calendar calendar = Calendar.getInstance();
                    calendar.setTimeInMillis(monthlyReading.getDate());
                    int[] date = ViewHelper.parseIntsFromCalendar(calendar);

                    //odečtení jednoho měsíce/ protože zobrazuji data za měsíc zpět
                    date[1]--;//odečítám jeden měsíc
                    if (date[1] < 0) {//pokud je měsíc menší než 0(leden)
                        date[2]--;//odečítám jeden rok
                        date[1] = 11;//měsíc nastavuji na prosinec (11)
                    }
                    PriceListRegulBuilder priceListRegulBuilder = new PriceListRegulBuilder(priceList, date[2], date[1], date[0]);
                    priceList = priceListRegulBuilder.getRegulPriceList();

                    boolean overDateRegulPrice = isOverDateRegulPrice(priceListRegulBuilder, monthlyReading, monthlyReadingPrevious);

                    if (overDateRegulPrice) {
                        holder.ivWarning.setVisibility(View.VISIBLE);
                        holder.tvAlertRegulPrice.setVisibility(View.VISIBLE);
                    }


                    /*if (!priceListRegulBuilder.isRegulPrice()) {
                        holder.ivWarning.setVisibility(View.GONE);
                        holder.tvAlertRegulPrice.setVisibility(View.GONE);
                    }*/

                }
                prices = Calculation.calculatePriceWithoutPozeKwh(priceList, subscriptionPoint);
                monthPrice = month * prices[2];
                total = monthPrice + (prices[0] * vtDif) + (prices[1] * ntDif) + prices[3] * (vtDif + ntDif) + items.get(position).getOtherServices();
                double discout = monthlyReading.getDiscountDPH();
                if (discout > 0)
                    discoutDescription = " Kč se slevou DPH";
                total = total + (total * priceList.getDph() / 100) - discout;
                different = monthlyReading.getPayment() - total;
            }
            String color = "red";
            holder.rootRelativeLayout.setBackground(context.getResources().getDrawable(R.drawable.shape_montly_reading_no));
            int imageResource = R.mipmap.ic_ne;
            if (different >= 0) {
                color = "#008000";
                holder.rootRelativeLayout.setBackground(context.getResources().getDrawable(R.drawable.shape_montly_reading_yes));
                imageResource = R.mipmap.ic_ano;
            }

            holder.ivIconResult.setImageResource(imageResource);
            holder.tvVtDif.setText(DecimalFormatHelper.df2.format(vtDif) + " kWh");
            holder.tvNtDif.setText(DecimalFormatHelper.df2.format(ntDif) + " kWh");
            holder.tvVtPrice.setText(DecimalFormatHelper.df2.format(prices[0] * vtDif) + " Kč");
            holder.tvNtPrice.setText(DecimalFormatHelper.df2.format(prices[1] * ntDif) + " Kč");
            holder.tvPozePrice.setText(DecimalFormatHelper.df2.format(prices[3] * (vtDif + ntDif)) + " Kč");
            holder.tvNextServicesPrice.setText(DecimalFormatHelper.df2.format(items.get(position).getOtherServices()) + " Kč");
            holder.tvMonth.setText(Html.fromHtml("Počet měsíců: <b>" + (DecimalFormatHelper.df3.format(month))));
            holder.tvMonthPrice.setText(DecimalFormatHelper.df2.format(monthPrice) + " Kč");
            holder.tvTotalPrice.setText(Html.fromHtml("Celkem: <b>" + DecimalFormatHelper.df2.format(total) + "</b>" + discoutDescription));
            holder.tvDifferentPrice.setText(Html.fromHtml("Rozdíl: <b><font color=\"" + color + "\">" + DecimalFormatHelper.df2.format(different) + "</b> Kč"));
        }

        holder.rootRelativeLayout.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                v.showContextMenu();
                return true;
            }
        });

        holder.rootRelativeLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (showButtons >= 0) {
                    RecyclerView.ViewHolder viewHolder = recyclerView.findViewHolderForAdapterPosition(showButtons);
                    if (viewHolder != null)
                        viewHolder.itemView.findViewById(R.id.lnButtonsMonthlyItem).setVisibility(View.GONE);
                }

                TransitionManager.beginDelayedTransition(recyclerView);
                if (showButtons >= 0 && showButtons == position) {
                    showButtons = -1;
                } else {
                    showButtons = position;
                }
                showButtons(holder, position);
            }
        });

        showButtons(holder, position);

        holder.btnEdit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MonthlyReadingEditFragment monthlyReadingEditFragment = MonthlyReadingEditFragment.newInstance(
                        subscriptionPoint.getTableO(),
                        subscriptionPoint.getTablePLATBY(),
                        monthlyReading.getId());
                FragmentChange.replace((FragmentActivity) context, monthlyReadingEditFragment, MOVE, true);
            }
        });

        holder.btnDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                YesNoDialogFragment yesNoDialogFragment = YesNoDialogFragment.newInstance(
                        new YesNoDialogFragment.OnDialogResult() {
                            @Override
                            public void onResult(boolean b) {
                                if (b) {
                                    deleteMonthlyReading(monthlyReading.getId(),position);
                                }
                            }
                        }, context.getResources().getString(R.string.smazat_mesicni_odecet));
                yesNoDialogFragment.show(((FragmentActivity) context).getSupportFragmentManager(), TAG);
            }
        });

        if (monthlyReading.isFirst()) {
            holder.tvPriceList.setVisibility(View.GONE);
            holder.tvPayment.setVisibility(View.GONE);
            holder.rl3.setVisibility(View.GONE);
            holder.tvVtPrice.setVisibility(View.GONE);
            holder.tvNtPrice.setVisibility(View.GONE);
            holder.tvVtDif.setVisibility(View.GONE);
            holder.tvNtDif.setVisibility(View.GONE);
            holder.tvMonthPrice.setVisibility(View.GONE);
            holder.ivIconResult.setVisibility(View.GONE);
            holder.tvMonth.setText("První odečet. \nPoužití při prvním záznamu nebo výměně elektroměru.");
            holder.rootRelativeLayout.setBackground(context.getResources().getDrawable(R.drawable.shape_monthly_reading_gray));
            holder.ivWarning.setVisibility(View.GONE);
            if (simplyView) {

                holder.tvMonth.setVisibility(View.GONE);
            } else {

                holder.tvMonth.setVisibility(View.VISIBLE);
            }

        } else {
            holder.tvPriceList.setVisibility(View.VISIBLE);
            holder.tvPayment.setVisibility(View.VISIBLE);
            holder.rl3.setVisibility(View.VISIBLE);
            holder.tvVtPrice.setVisibility(View.VISIBLE);
            holder.tvVtDif.setVisibility(View.VISIBLE);
            holder.tvMonthPrice.setVisibility(View.VISIBLE);
            holder.ivIconResult.setVisibility(View.VISIBLE);
            if (priceList != null) {
                //skrytí NT sazby u jednotarifních sazeb
                if (priceList.getCenaNT() == 0 && priceList.getDistNT() == 0) {
                    holder.tvNtDescription.setVisibility(View.GONE);
                    holder.tvNt.setVisibility(View.GONE);
                    holder.tvNtDif.setVisibility(View.GONE);
                    holder.tvNtPrice.setVisibility(View.GONE);
                } else {
                    holder.tvNtDescription.setVisibility(View.VISIBLE);
                    holder.tvNt.setVisibility(View.VISIBLE);
                    holder.tvNtDif.setVisibility(View.VISIBLE);
                    holder.tvNtPrice.setVisibility(View.VISIBLE);
                }
            }
            if (simplyView) {
                holder.tvPayment.setVisibility(View.GONE);
                holder.tvPaymentDescription.setVisibility(View.GONE);
                holder.ivIconResult.setVisibility(View.GONE);
                holder.tvPriceList.setVisibility(View.GONE);

                RelativeLayout.LayoutParams paramsTvDays = (RelativeLayout.LayoutParams) holder.tvMonth.getLayoutParams();
                paramsTvDays.removeRule(RelativeLayout.ALIGN_PARENT_TOP);
                paramsTvDays.addRule(RelativeLayout.BELOW, R.id.tvAlertRegulPrice);
                holder.tvMonth.setLayoutParams(paramsTvDays);

            } else {
                holder.tvPayment.setVisibility(View.VISIBLE);
                holder.tvPaymentDescription.setVisibility(View.VISIBLE);
                holder.ivIconResult.setVisibility(View.VISIBLE);
                holder.tvPriceList.setVisibility(View.VISIBLE);

                RelativeLayout.LayoutParams paramsTvDays = (RelativeLayout.LayoutParams) holder.tvMonth.getLayoutParams();
                paramsTvDays.addRule(RelativeLayout.BELOW, R.id.imageViewMonthlyReading);
                holder.tvMonth.setLayoutParams(paramsTvDays);

            }

            if (items.get(position).getOtherServices() > 0) {
                holder.tvNextServicesDescription.setVisibility(View.VISIBLE);
                holder.tvNextServicesPrice.setVisibility(View.VISIBLE);
            } else {
                holder.tvNextServicesDescription.setVisibility(View.GONE);
                holder.tvNextServicesPrice.setVisibility(View.GONE);
            }
        }


    }

    @Override
    public int getItemCount() {
        if (items == null) return 0;
        return items.size();
    }


    @Override
    public long getItemId(int position) {
        long itemId = -1L;
        try {
            itemId = items.get(position).getId();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return itemId;
    }

    /**
     * Zkontroluje období, zdali neobsahuje současně regulovanou a neregulovanou cenu
     *
     * @return
     */
    private boolean isOverDateRegulPrice(PriceListRegulBuilder priceListRegulBuilder, MonthlyReadingModel monthlyReading, MonthlyReadingModel monthlyReadingPrevious) {
        long startRegulPrice = priceListRegulBuilder.getDateStart();
        long endRegulPrice = priceListRegulBuilder.getDateEnd();
        long dateStartMonthlyReading = monthlyReadingPrevious.getDate();
        long dateEndMonthlyReading = monthlyReading.getDate() - (24 * 60 * 60 * 1000);
        //Začátek regulace musí být větší  než začátek odečtu a zároveň začátek regulace menší nebo roven než konec odečtu
        //nebo
        //Konec regulace musí být větší či rovno než začátek měsíčního odečtu a zároveň konec regulace musí být menší než konce měsíčního odečtu
        if (
                ((startRegulPrice > dateStartMonthlyReading) && (startRegulPrice <= dateEndMonthlyReading))
                        || ((endRegulPrice >= dateStartMonthlyReading) && (endRegulPrice < dateEndMonthlyReading))
        ) {
            return true;
        } else {
            return false;
        }
    }

    private void showButtons(MyViewHolder holder, int position) {

        if (showButtons == position) {
            holder.lnButtons.setVisibility(View.VISIBLE);
        } else {
            holder.lnButtons.setVisibility(View.GONE);

        }
    }

    /**
     * Nastaví boolena pro zobtazení/skrytí rozšířených dat
     *
     * @param b
     */
    public void showSimpleView(boolean b) {
        Log.w("TAG", "Simply view " + b);
        this.simplyView = b;
        notifyDataSetChanged();
    }

    /**
     * Nastaví boolean pro zobrazení/skrytí regulovaných cen
     *
     * @param b
     */
    public void setShowRegulPrice(boolean b) {
        this.showRegulPrice = b;
        notifyDataSetChanged();
    }

    private void deleteMonthlyReading(long itemId, int position) {
        DataSubscriptionPointSource dataSubscriptionPointSource = new DataSubscriptionPointSource(context);
        dataSubscriptionPointSource.open();
        dataSubscriptionPointSource.deleteMonthlyReading(itemId, subscriptionPoint.getTableO());
        MonthlyReadingModel lastMonthlyReading = dataSubscriptionPointSource.lastMonthlyReadingByDate(subscriptionPoint.getTableO());
        dataSubscriptionPointSource.close();
        //upraví poslední záznam bez faktury podle posledního měsíčního záznamu
        showButtons = -1;
        items.remove(position);
        notifyItemRemoved(position);
        notifyItemRangeChanged(position-1, getItemCount());
        WithOutInvoiceService.editLastItemInInvoice(context, subscriptionPoint.getTableTED(), lastMonthlyReading);
    }

    public void setOnClickItemListener(OnClickItemListener onClickItemListener) {
        this.onClickItemListener = onClickItemListener;
    }

    public interface OnClickItemListener {
        void setClickItemListener(long id);

        //void setClickMonthlyReadingListener(MonthlyReadingModel monthlyReading);
    }

    public interface OnLongClickItemListener {
        void setLongClickItemListener(long id);
    }



}
