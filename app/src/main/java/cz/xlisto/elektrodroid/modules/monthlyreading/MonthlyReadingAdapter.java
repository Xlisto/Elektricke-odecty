package cz.xlisto.elektrodroid.modules.monthlyreading;


import static cz.xlisto.elektrodroid.utils.FragmentChange.Transaction.MOVE;

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

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

import cz.xlisto.elektrodroid.R;
import cz.xlisto.elektrodroid.databaze.DataMonthlyReadingSource;
import cz.xlisto.elektrodroid.databaze.DataPriceListSource;
import cz.xlisto.elektrodroid.dialogs.YesNoDialogFragment;
import cz.xlisto.elektrodroid.format.DecimalFormatHelper;
import cz.xlisto.elektrodroid.models.MonthlyReadingModel;
import cz.xlisto.elektrodroid.models.PriceListModel;
import cz.xlisto.elektrodroid.models.PriceListRegulBuilder;
import cz.xlisto.elektrodroid.models.SubscriptionPointModel;
import cz.xlisto.elektrodroid.modules.invoice.WithOutInvoiceService;
import cz.xlisto.elektrodroid.ownview.ViewHelper;
import cz.xlisto.elektrodroid.utils.Calculation;
import cz.xlisto.elektrodroid.utils.DetectScreenMode;
import cz.xlisto.elektrodroid.utils.DifferenceDate;
import cz.xlisto.elektrodroid.utils.FragmentChange;
import cz.xlisto.elektrodroid.utils.TextSizeAdjuster;


/**
 * Adapter pro zobrazení měsíčních odečtů v RecyclerView.
 * Tento adapter spravuje zobrazení a interakce s jednotlivými položkami měsíčních odečtů.
 */
public class MonthlyReadingAdapter extends RecyclerView.Adapter<MonthlyReadingAdapter.MyViewHolder> {

    /**
     * Tag pro logování v `MonthlyReadingAdapter`.
     */
    private static final String TAG = "MonthlyReadingAdapter";
    /**
     * Konstantní hodnota pro označení akce smazání měsíčního odečtu.
     */
    public static final String FLAG_DELETE_MONTHLY_READING = "flagDeleteMonthlyReading";
    /**
     * Index aktuálně zobrazených tlačítek.
     * -1 znamená, že žádná tlačítka nejsou zobrazena.
     */
    private static int showButtons = -1;
    /**
     * Seznam měsíčních odečtů.
     */
    private final ArrayList<MonthlyReadingModel> items;
    /**
     * Boolean hodnota pro zobrazení jednoduchého pohledu.
     */
    private boolean simplyView;
    /**
     * Boolean hodnota pro zobrazení regulovaných cen.
     */
    private boolean showRegulPrice;
    /**
     * Model odběrného místa.
     */
    private final SubscriptionPointModel subscriptionPoint;
    /**
     * RecyclerView, ve kterém budou položky zobrazeny.
     */
    private final RecyclerView recyclerView;
    /**
     * ID vybraného měsíčního odečtu.
     */
    private long selectedMonthlyReadingId;
    /**
     * Pozice vybrané položky.
     */
    private int selectedPosition;
    /**
     * Listener pro kliknutí na položku.
     */
    private OnClickItemListener onClickItemListener;


    /**
     * ViewHolder pro položky v RecyclerView.
     * Tato třída obsahuje odkazy na zobrazené prvky v jednotlivých položkách měsíčních odečtů.
     */
    static class MyViewHolder extends RecyclerView.ViewHolder {

        RelativeLayout rootRelativeLayout, rl2, rl3;
        LinearLayout lnButtons;
        TextView tvDate, tvVt, tvNt, tvPayment, tvPriceList, tvNtDif, tvVtDif, tvVtPrice, tvNtPrice, tvPozePrice, tvMonth, tvDateDetail,
                tvMonthPrice, tvTotalPrice, tvDifferentPrice, tvPaymentDescription, tvNextServicesDescription, tvNextServicesPrice, tvAlertRegulPrice,
                tvVtDescription, tvNtDescription, tvDescription;
        ImageView ivIconResult, ivWarning;
        Button btnEdit, btnDetail, btnDelete;


        /**
         * Konstruktor ViewHolderu.
         *
         * @param itemView View položky, která bude zobrazena v RecyclerView.
         */
        public MyViewHolder(@NonNull View itemView) {

            super(itemView);
        }

    }


    /**
     * Konstruktor pro `MonthlyReadingAdapter`.
     *
     * @param items             Seznam měsíčních odečtů.
     * @param subscriptionPoint Model odběrného místa.
     * @param simplyView        Boolean hodnota pro zobrazení jednoduchého pohledu.
     * @param showRegulPrice    Boolean hodnota pro zobrazení regulovaných cen.
     * @param recyclerView      RecyclerView, ve kterém budou položky zobrazeny.
     */
    public MonthlyReadingAdapter(ArrayList<MonthlyReadingModel> items, SubscriptionPointModel subscriptionPoint, boolean simplyView, boolean showRegulPrice, RecyclerView recyclerView) {

        this.items = items;
        this.simplyView = simplyView;
        this.subscriptionPoint = subscriptionPoint;
        this.showRegulPrice = showRegulPrice;
        this.recyclerView = recyclerView;
    }


    /**
     * Vytvoří a inicializuje objekt View z XML návrhu vzoru položky.
     * Příprava kontejneru, ve kterém budou zobrazena data jednotlivé položky.
     *
     * @param parent   ViewGroup, do kterého bude nový View přidán po jeho vytvoření.
     * @param viewType Typ nového View.
     * @return MyViewHolder obsahující nový View.
     */
    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_monthly_reading, parent, false);
        MyViewHolder vh = new MyViewHolder(v);
        vh.rootRelativeLayout = v.findViewById(R.id.rlItemMonthlyReading);
        vh.lnButtons = v.findViewById(R.id.lnButtonsMonthlyItem);
        vh.rl2 = v.findViewById(R.id.rl2);
        vh.rl3 = v.findViewById(R.id.rl3);
        vh.tvDate = v.findViewById(R.id.tvDate);
        vh.tvDateDetail = v.findViewById(R.id.tvDateDetail);
        vh.tvPriceList = v.findViewById(R.id.tvTarif);
        vh.tvVt = v.findViewById(R.id.tvVT);
        vh.tvNt = v.findViewById(R.id.tvNT);
        vh.tvVtDif = v.findViewById(R.id.tvVtRozdil);
        vh.tvNtDif = v.findViewById(R.id.tvNtRozdil);
        vh.tvVtDescription = v.findViewById(R.id.tvVTDescription);
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
        vh.btnDetail = v.findViewById(R.id.btnDetailMonthlyItem);
        vh.btnDelete = v.findViewById(R.id.btnDeleteMonthlyItem);
        vh.tvDescription = v.findViewById(R.id.tvDescription);
        return vh;
    }


    /**
     * Nastaví data pro zobrazení v jednotlivých položkách RecyclerView.
     * Tato metoda je volána při každém zobrazení položky v RecyclerView.
     *
     * @param holder   ViewHolder, který bude aktualizován daty.
     * @param position Pozice položky v adapteru.
     */
    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, @SuppressLint("RecyclerView") int position) {

        final MonthlyReadingModel monthlyReading = items.get(position);
        MonthlyReadingModel monthlyReadingPrevious;
        Context context = holder.rootRelativeLayout.getContext();

        DataPriceListSource dataPriceListSource = new DataPriceListSource(context);
        dataPriceListSource.open();
        PriceListModel priceList = dataPriceListSource.readPrice(items.get(position).getPriceListId());
        dataPriceListSource.close();

        if (priceList != null) {
            holder.tvPriceList.setText(priceList.getName());
        } else
            holder.tvPriceList.setText(context.getResources().getString(R.string.vyberteCenik));

        holder.tvDate.setText(ViewHelper.convertLongToDate(monthlyReading.getDate()));
        holder.tvVt.setText(DecimalFormatHelper.df2.format(monthlyReading.getVt()));
        holder.tvNt.setText(DecimalFormatHelper.df2.format(monthlyReading.getNt()));
        holder.tvPayment.setText(context.getResources().getString(R.string.float_price, monthlyReading.getPayment()));
        holder.tvAlertRegulPrice.setVisibility(View.GONE);

        //výpočet
        if ((position + 1) < items.size()) {
            String differenceDescription = context.getResources().getString(R.string.kc_with_tax);
            double vtDiff = monthlyReading.getVt() - items.get(position + 1).getVt();
            double ntDiff = monthlyReading.getNt() - items.get(position + 1).getNt();
            double[] prices = new double[]{0, 0, 0, 0};
            double month;
            double monthPrice = 0;
            double total = 0;
            double different = 0;
            monthlyReadingPrevious = items.get(position + 1);
            month = Calculation.differentMonth(ViewHelper.convertLongToDate(items.get(position + 1).getDate()), holder.tvDate.getText().toString(), DifferenceDate.TypeDate.MONTH);
            if (priceList != null) {
                holder.ivWarning.setVisibility(View.INVISIBLE);
                boolean overDateRegulPrice = false;
                Calendar calendarEnd = Calendar.getInstance();
                calendarEnd.setTimeInMillis(items.get(position).getDate());
                calendarEnd.add(Calendar.DAY_OF_MONTH, -1);
                long dateStart = items.get(position + 1).getDate();
                long dateEnd = calendarEnd.getTimeInMillis();
                holder.tvDateDetail.setText(context.getResources().getString(R.string.period, ViewHelper.convertLongToDate(dateStart), ViewHelper.convertLongToDate(dateEnd)));

                if (showRegulPrice) {
                    PriceListRegulBuilder priceListRegulBuilder = new PriceListRegulBuilder(priceList, monthlyReadingPrevious);
                    priceList = priceListRegulBuilder.getRegulPriceList();

                    overDateRegulPrice = isOverDateRegulPrice(priceListRegulBuilder, monthlyReading, monthlyReadingPrevious);
                }

                boolean overDatePriceList = priceList.getPlatnostOD() <= dateStart && priceList.getPlatnostDO() >= dateEnd;

                if (overDateRegulPrice || !overDatePriceList) {
                    holder.ivWarning.setVisibility(View.VISIBLE);
                    holder.tvAlertRegulPrice.setVisibility(View.VISIBLE);
                    if (!overDatePriceList) {
                        holder.tvAlertRegulPrice.setText(context.getResources().getString(R.string.alert_date_price_list));
                        holder.tvAlertRegulPrice.setTextColor(holder.tvAlertRegulPrice.getContext().getResources().getColor(R.color.color_red_alert));
                    }
                } else {
                    holder.ivWarning.setVisibility(View.INVISIBLE);
                    holder.tvAlertRegulPrice.setVisibility(View.GONE);
                }

                prices = Calculation.calculatePriceWithoutPozeKwh(priceList, subscriptionPoint);//vt, nt, stPlat, poze
                monthPrice = month * prices[2];
                total = monthPrice + (prices[0] * vtDiff) + (prices[1] * ntDiff) + prices[3] * (vtDiff + ntDiff) + items.get(position).getOtherServices();
                double difference = monthlyReading.getDifferenceDPH();
                if (difference > 0)
                    differenceDescription = context.getResources().getString(R.string.kc_with_discount);
                total = total + (total * priceList.getDph() / 100) - difference;
                different = monthlyReading.getPayment() - total;
            }
            String color = "red";
            holder.rootRelativeLayout.setBackground(ResourcesCompat.getDrawable(context.getResources(), R.drawable.shape_montly_reading_no, null));
            holder.tvDateDetail.setTextColor(ContextCompat.getColor(holder.tvDateDetail.getContext(), R.color.colorOnRedContainer));
            holder.tvDate.setTextColor(ContextCompat.getColor(holder.tvDateDetail.getContext(),R.color.colorOnRedContainer));
            int imageResource = R.drawable.ic_ne_smile;
            if (different >= 0) {
                color = "#008000";
                holder.rootRelativeLayout.setBackground(ResourcesCompat.getDrawable(context.getResources(), R.drawable.shape_monthly_reading_yes, null));
                holder.tvDateDetail.setTextColor(ContextCompat.getColor(holder.tvDateDetail.getContext(), R.color.colorOnGreenContainer));
                holder.tvDate.setTextColor(ContextCompat.getColor(holder.tvDateDetail.getContext(),R.color.colorOnGreenContainer));
                imageResource = R.drawable.ic_ano_smile;
            }

            holder.ivIconResult.setImageResource(imageResource);
            holder.tvVtDif.setText(context.getResources().getString(R.string.consuption, DecimalFormatHelper.df2.format(vtDiff)));
            holder.tvNtDif.setText(context.getResources().getString(R.string.consuption, DecimalFormatHelper.df2.format(ntDiff)));
            holder.tvVtPrice.setText(context.getResources().getString(R.string.string_price, DecimalFormatHelper.df2.format(prices[0] * vtDiff)));
            holder.tvNtPrice.setText(context.getResources().getString(R.string.string_price, DecimalFormatHelper.df2.format(prices[1] * ntDiff)));
            holder.tvPozePrice.setText(context.getResources().getString(R.string.string_price, DecimalFormatHelper.df2.format(prices[3] * (vtDiff + ntDiff))));
            holder.tvNextServicesPrice.setText(context.getResources().getString(R.string.string_price, DecimalFormatHelper.df2.format(items.get(position).getOtherServices())));
            holder.tvMonth.setText(Html.fromHtml(context.getResources().getString(R.string.count_months_html, (DecimalFormatHelper.df3.format(month)))));
            holder.tvMonthPrice.setText(context.getResources().getString(R.string.string_price, DecimalFormatHelper.df2.format(monthPrice)));
            holder.tvTotalPrice.setText(Html.fromHtml(context.getResources().getString(R.string.total_price_html, DecimalFormatHelper.df2.format(total), differenceDescription)));
            holder.tvDifferentPrice.setText(Html.fromHtml(context.getResources().getString(R.string.total_different_html, color, DecimalFormatHelper.df2.format(different))));

            List<TextView> textViewsVt = Arrays.asList(holder.tvVtDescription, holder.tvVt, holder.tvVtDif, holder.tvVtPrice);
            List<TextView> textViewsNt = Arrays.asList(holder.tvNtDescription, holder.tvNt, holder.tvNtDif, holder.tvNtPrice);

            TextSizeAdjuster.adjustTextSize(holder.rl2, textViewsVt, context);
            TextSizeAdjuster.adjustTextSize(holder.rl2, textViewsNt, context);

        } else {
            monthlyReadingPrevious = null;
        }

        holder.rootRelativeLayout.setOnClickListener(v -> {
            if (showButtons >= 0) {
                RecyclerView.ViewHolder viewHolder = recyclerView.findViewHolderForAdapterPosition(showButtons);
                if (viewHolder != null)
                    viewHolder.itemView.findViewById(R.id.lnButtonsMonthlyItem).setVisibility(View.GONE);
            }

            TransitionManager.beginDelayedTransition(recyclerView);
            if (showButtons >= 0 && showButtons == position) {
                showButtons = -1;
                if (onClickItemListener != null)
                    onClickItemListener.setClickPriceListListener(-1L, -1L);
            } else {
                showButtons = position;
                if (onClickItemListener != null && monthlyReadingPrevious != null)
                    onClickItemListener.setClickPriceListListener(monthlyReading.getId(), monthlyReadingPrevious.getId());
            }
            showButtons(holder, position);

        });

        holder.btnEdit.setOnClickListener(v -> {
            MonthlyReadingEditFragment monthlyReadingEditFragment = MonthlyReadingEditFragment.newInstance(
                    subscriptionPoint.getTableO(),
                    monthlyReading.getId(),
                    isFirst(position),
                    isChangeMeter(monthlyReading));
            FragmentChange.replace((FragmentActivity) context, monthlyReadingEditFragment, MOVE, true);
        });

        holder.btnDetail.setOnClickListener(v -> {
            if (monthlyReadingPrevious != null) {
                MonthlyReadingDetailFragment monthlyReadingDetailFragment = MonthlyReadingDetailFragment.newInstance(monthlyReading.getId(), monthlyReadingPrevious.getId(), showRegulPrice);
                FragmentChange.replace((FragmentActivity) context, monthlyReadingDetailFragment, MOVE, true);
            }
        });

        if (DetectScreenMode.isLandscape(context))
            holder.btnDetail.setVisibility(View.GONE);

        holder.btnDelete.setOnClickListener(v -> {
            selectedMonthlyReadingId = monthlyReading.getId();
            selectedPosition = position;
            YesNoDialogFragment yesNoDialogFragment = YesNoDialogFragment.newInstance(context.getResources().getString(R.string.smazat_mesicni_odecet), FLAG_DELETE_MONTHLY_READING);
            yesNoDialogFragment.show(((FragmentActivity) context).getSupportFragmentManager(), TAG);
        });

        if (isFirst(position) || isChangeMeter(monthlyReading)) {
            holder.tvPriceList.setVisibility(View.GONE);
            holder.tvPayment.setVisibility(View.GONE);
            holder.rl3.setVisibility(View.GONE);
            holder.tvVtPrice.setVisibility(View.GONE);
            holder.tvNtPrice.setVisibility(View.GONE);
            holder.tvVtDif.setVisibility(View.GONE);
            holder.tvNtDif.setVisibility(View.GONE);
            holder.tvMonthPrice.setVisibility(View.GONE);
            holder.ivIconResult.setVisibility(View.GONE);
            holder.tvAlertRegulPrice.setVisibility(View.GONE);
            holder.tvMonth.setText(context.getResources().getString(R.string.first_reading));
            holder.rootRelativeLayout.setBackground(ResourcesCompat.getDrawable(context.getResources(), R.drawable.shape_monthly_reading_gray, null));
            holder.tvDate.setTextColor(ContextCompat.getColor(holder.tvDateDetail.getContext(),R.color.md_theme_onSurface));
            holder.tvDateDetail.setVisibility(View.GONE);
            holder.btnDetail.setVisibility(View.GONE);
            holder.ivWarning.setVisibility(View.INVISIBLE);
            if (simplyView) {
                holder.tvMonth.setVisibility(View.GONE);
            } else {
                holder.tvMonth.setVisibility(View.VISIBLE);
            }

        } else {

            //poslední položka v seznamu
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

                RelativeLayout.LayoutParams paramsTvMonthPrice = (RelativeLayout.LayoutParams) holder.tvMonthPrice.getLayoutParams();
                paramsTvMonthPrice.removeRule(RelativeLayout.START_OF);
                paramsTvMonthPrice.addRule(RelativeLayout.ALIGN_PARENT_END, R.id.tvDays);
                holder.tvMonthPrice.setLayoutParams(paramsTvMonthPrice);

            } else {
                holder.tvPayment.setVisibility(View.VISIBLE);
                holder.tvPaymentDescription.setVisibility(View.VISIBLE);
                holder.ivIconResult.setVisibility(View.VISIBLE);
                holder.tvPriceList.setVisibility(View.VISIBLE);

                RelativeLayout.LayoutParams paramsTvMonthPrice = (RelativeLayout.LayoutParams) holder.tvMonthPrice.getLayoutParams();
                paramsTvMonthPrice.addRule(RelativeLayout.START_OF, R.id.imageViewMonthlyReading);
                paramsTvMonthPrice.removeRule(RelativeLayout.ALIGN_PARENT_END);
                holder.tvMonthPrice.setLayoutParams(paramsTvMonthPrice);
            }

            if (items.get(position).getOtherServices() > 0) {
                holder.tvNextServicesDescription.setVisibility(View.VISIBLE);
                holder.tvNextServicesPrice.setVisibility(View.VISIBLE);
            } else {
                holder.tvNextServicesDescription.setVisibility(View.GONE);
                holder.tvNextServicesPrice.setVisibility(View.GONE);
            }

            if (monthlyReading.getDescription() != null) {
                if (monthlyReading.getDescription().isEmpty()) {
                    holder.tvDescription.setVisibility(View.GONE);
                } else {
                    holder.tvDescription.setVisibility(View.VISIBLE);
                    holder.tvDescription.setText(monthlyReading.getDescription());
                }
            }

        }

        showButtons(holder, position);
    }


    /**
     * Vrátí počet položek v adapteru.
     *
     * @return Počet položek v seznamu.
     */
    @Override
    public int getItemCount() {

        if (items == null) return 0;
        return items.size();
    }


    /**
     * Vrátí ID položky na zadané pozici.
     *
     * @param position Pozice položky v adapteru.
     * @return ID položky na zadané pozici.
     */
    @Override
    public long getItemId(int position) {

        long itemId = -1L;
        try {
            itemId = items.get(position).getId();
        } catch (Exception e) {
            Log.e(TAG, "getItemId: " + e.getMessage());
        }

        return itemId;
    }


    /**
     * Zkontroluje období, zda-li neobsahuje současně regulovanou a neregulovanou cenu.
     *
     * @param priceListRegulBuilder  Objekt PriceListRegulBuilder obsahující informace o regulovaných cenách.
     * @param monthlyReading         Aktuální měsíční odečet.
     * @param monthlyReadingPrevious Předchozí měsíční odečet.
     * @return boolean true - obsahuje, false - neobsahuje.
     */
    private boolean isOverDateRegulPrice(PriceListRegulBuilder priceListRegulBuilder, MonthlyReadingModel monthlyReading, MonthlyReadingModel monthlyReadingPrevious) {

        long startRegulPrice = priceListRegulBuilder.getDateStart();
        long endRegulPrice = priceListRegulBuilder.getDateEnd();
        long dateStartMonthlyReading = monthlyReadingPrevious.getDate();
        long dateEndMonthlyReading = monthlyReading.getDate() - (24 * 60 * 60 * 1000);
        //Začátek regulace musí být větší  než začátek odečtu a zároveň začátek regulace menší nebo roven než konec odečtu
        //nebo
        //Konec regulace musí být větší či rovno než začátek měsíčního odečtu a zároveň konec regulace musí být menší než konce měsíčního odečtu
        return ((startRegulPrice > dateStartMonthlyReading) && (startRegulPrice <= dateEndMonthlyReading))
                || ((endRegulPrice >= dateStartMonthlyReading) && (endRegulPrice < dateEndMonthlyReading));
    }


    /**
     * Skryje nebo zobrazí tlačítka pro smazání a editaci na základě aktuální pozice.
     *
     * @param holder   MyViewHolder, který obsahuje odkazy na zobrazené prvky v jednotlivých položkách měsíčních odečtů.
     * @param position Pozice položky v adapteru.
     */
    private void showButtons(MyViewHolder holder, int position) {

        if (showButtons == position)
            holder.lnButtons.setVisibility(View.VISIBLE);
        else
            holder.lnButtons.setVisibility(View.GONE);
    }


    /**
     * Nastaví boolean pro zobrazení/skrytí rozšířených dat.
     *
     * @param b Boolean hodnota pro zobrazení jednoduchého pohledu.
     */
    public void showSimpleView(boolean b) {

        this.simplyView = b;
        notifyItemRangeChanged(0, getItemCount());
    }


    /**
     * Nastaví boolean pro zobrazení/skrytí regulovaných cen.
     *
     * @param b Boolean hodnota pro zobrazení regulovaných cen.
     */
    public void setShowRegulPrice(boolean b) {

        this.showRegulPrice = b;
        notifyItemRangeChanged(0, getItemCount());
    }


    /**
     * Smaže záznam měsíčního odečtu.
     *
     * @param context Kontext aplikace.
     */
    public void deleteMonthlyReading(Context context) {

        deleteMonthlyReading(selectedMonthlyReadingId, selectedPosition, context);
    }


    /**
     * Smaže záznam měsíčního odečtu.
     *
     * @param itemId   ID záznamu, který má být smazán.
     * @param position Pozice záznamu v adapteru.
     * @param context  Kontext aplikace.
     */
    private void deleteMonthlyReading(long itemId, int position, Context context) {
        if (items == null || items.isEmpty()) {
            Log.e(TAG, "deleteMonthlyReading: Pokus o odstranění z prázdného seznamu");
            return;
        }

        DataMonthlyReadingSource dataMonthlyReadingSource = new DataMonthlyReadingSource(context);
        dataMonthlyReadingSource.open();
        dataMonthlyReadingSource.deleteMonthlyReading(itemId, subscriptionPoint.getTableO());
        dataMonthlyReadingSource.close();

        // odebere položku z adapter a přepočítá pozice, vynuluje showButtons jež ukazuje na rozbalenou položku
        showButtons = -1;
        items.remove(position);
        notifyItemRemoved(position);
        notifyItemRangeChanged(Math.max(position - 1, 0), getItemCount());

        /*
        / Smaže záznam v období bez faktury pokud je vázaný k prvnímu odečtu nebo výměně elektroměru
        / WithOutInvoiceService.deleteItemInInvoiceByIdMonthlyReading(context, subscriptionPoint.getTableTED(), itemId);
        / upraví poslední záznam bez faktury podle posledního měsíčního záznamu
        */
        if (items.isEmpty()) {
            WithOutInvoiceService.deleteAllItemsInvoiceTableTED(context);
        } else {
            WithOutInvoiceService.updateAllItemsInvoice(context, subscriptionPoint.getTableTED(), subscriptionPoint.getTableFAK(), subscriptionPoint.getTableO());
        }
    }


    /**
     * Zkontroluje, zda je měsíční odečet první.
     *
     * @param position Pozice aktuálního měsíčního odečtu v seznamu.
     * @return boolean true, pokud je měsíční odečet první nebo poslední, jinak false.
     */
    private boolean isFirst(int position) {
        return (position == items.size() - 1);
    }


    /**
     * Zkontroluje, zda je měsíční odečet veden jako výměna elektroměru.
     *
     * @param monthlyReading Aktuální měsíční odečet.
     * @return boolean true, pokud je měsíční odečet první nebo poslední, jinak false.
     */
    private boolean isChangeMeter(MonthlyReadingModel monthlyReading) {
        return monthlyReading.isChangeMeter();
    }


    /**
     * Nastaví listener pro kliknutí na položku.
     *
     * @param onClickItemListener OnClickItemListener, který bude nastaven.
     */
    public void setOnClickItemListener(OnClickItemListener onClickItemListener) {

        this.onClickItemListener = onClickItemListener;
    }


    /**
     * Rozhraní pro listener kliknutí na položku.
     * Obsahuje metodu pro nastavení listeneru pro kliknutí na cenový seznam.
     */
    public interface OnClickItemListener {

        /**
         * Nastaví listener pro kliknutí na cenový seznam.
         *
         * @param idCurrentlyReading ID aktuálního odečtu.
         * @param idPreviousReading  ID předchozího odečtu.
         */
        void setClickPriceListListener(long idCurrentlyReading, long idPreviousReading);

    }

}
