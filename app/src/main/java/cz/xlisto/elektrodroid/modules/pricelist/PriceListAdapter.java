package cz.xlisto.elektrodroid.modules.pricelist;


import static cz.xlisto.elektrodroid.format.DecimalFormatHelper.df2;
import static cz.xlisto.elektrodroid.utils.FragmentChange.Transaction.MOVE;

import android.annotation.SuppressLint;
import android.content.Context;
import android.transition.TransitionManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

import cz.xlisto.elektrodroid.R;
import cz.xlisto.elektrodroid.databaze.DataPriceListSource;
import cz.xlisto.elektrodroid.databaze.DataSubscriptionPointSource;
import cz.xlisto.elektrodroid.dialogs.YesNoDialogFragment;
import cz.xlisto.elektrodroid.models.PriceListModel;
import cz.xlisto.elektrodroid.models.PriceListRegulBuilder;
import cz.xlisto.elektrodroid.models.SubscriptionPointModel;
import cz.xlisto.elektrodroid.ownview.ViewHelper;
import cz.xlisto.elektrodroid.shp.ShPPriceList;
import cz.xlisto.elektrodroid.utils.Calculation;
import cz.xlisto.elektrodroid.utils.FragmentChange;
import cz.xlisto.elektrodroid.utils.Round;


public class PriceListAdapter extends RecyclerView.Adapter<PriceListAdapter.MyViewHolder> {
    public static final String TAG = "PriceListAdapter";
    public final static String FLAG_DIALOG_FRAGMENT_DELETE_PRICE_LIST = "dialogFragmentDeletePriceList";
    private final OnClickItemListener onClickItemListener;
    private final boolean showSelectItem;
    private int selectedItem = -1;
    private long idSelectedPriceList;
    private final ArrayList<PriceListModel> items;
    private final SubscriptionPointModel subscriptionPoint;
    private int showButtons = -1;
    private final RecyclerView recyclerView;
    private final ShPPriceList shPPriceList;
    private final Context context;
    private long selectedItemId;
    private int selectedPosition;


    static class MyViewHolder extends RecyclerView.ViewHolder {
        RelativeLayout relativeLayout;
        TextView tvValidityDate, tvSeries, tvProduct, tvRate, tvFirma, tvPriceVT, tvPriceNT, tvPriceMonth,
                tvNote, tvPriceVTRegul, tvPriceNTRegul, tvPriceMonthRegul;
        RadioButton rbSelectItem;
        LinearLayout lnRegulPrice, lnButtons;
        Button btnEdit, btnDelete, btnDetail;

        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }


    //konstruktor
    public PriceListAdapter(ArrayList<PriceListModel> items, SubscriptionPointModel subscriptionPoint, boolean selectItem, long idSelectedPriceList,
                            OnClickItemListener onClickItemListener, RecyclerView recyclerView) {
        this.items = items;
        this.showSelectItem = selectItem;
        this.onClickItemListener = onClickItemListener;
        this.idSelectedPriceList = idSelectedPriceList;
        this.subscriptionPoint = subscriptionPoint;
        this.recyclerView = recyclerView;
        context = recyclerView.getContext();
        shPPriceList = new ShPPriceList(context);
    }


    // Vytvoření a inicializace objektu View z XML návrhu vzoru položky.
    // Příprava kontejneru, ve kterém budou zobrazena data jednotlivé položky
    @SuppressLint("MissingInflatedId")
    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_price_list, parent, false);
        MyViewHolder vh = new MyViewHolder(v);
        vh.relativeLayout = v.findViewById(R.id.item_price_list);
        vh.rbSelectItem = v.findViewById(R.id.rbPriceList);
        vh.tvValidityDate = v.findViewById(R.id.tvPlatnostCeniku);
        vh.tvSeries = v.findViewById(R.id.tvProduktovaRada);
        vh.tvProduct = v.findViewById(R.id.tvProdukt);
        vh.tvRate = v.findViewById(R.id.tvSazba);
        vh.tvFirma = v.findViewById(R.id.tvFirma);
        vh.tvPriceVT = v.findViewById(R.id.tvCenaVT);
        vh.tvPriceNT = v.findViewById(R.id.tvCenaNT);
        vh.tvPriceMonth = v.findViewById(R.id.tvPayment);
        vh.tvPriceVTRegul = v.findViewById(R.id.tvCenaVTRegul);
        vh.tvPriceNTRegul = v.findViewById(R.id.tvCenaNTRegul);
        vh.tvPriceMonthRegul = v.findViewById(R.id.tvCenaMesicRegul);
        vh.tvNote = v.findViewById(R.id.tvPoznamkaItemPriceList);
        vh.lnRegulPrice = v.findViewById(R.id.ln_item_price_list_regul);
        vh.lnButtons = v.findViewById(R.id.lnButtonsPriceListItem);
        vh.btnEdit = v.findViewById(R.id.btnEditPriceListItem);
        vh.btnDelete = v.findViewById(R.id.btnDeletePriceListItem);
        vh.btnDetail = v.findViewById(R.id.btnDetailPriceListItem);

        return vh;
    }


    // Naplnění kontejneru daty (kontejner vytvořen v přepsané metodě onCreateViewHolder())
    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, @SuppressLint("RecyclerView") int position) {
        final PriceListModel priceList = items.get(position);

        showButtons = shPPriceList.get(ShPPriceList.SHOW_BUTTONS_PRICE_LIST, -1);

        double[] ceny = Calculation.calculatePriceForPriceListDPH(priceList, subscriptionPoint);
        String cenaVT = "1kWh VT: " + (df2.format(Round.round(ceny[0]))) + " Kč";
        String cenaNT = "NT: " + (df2.format(Round.round(ceny[1]))) + " Kč";
        String cenaMesic = "Měsíc: " + (df2.format(Round.round(ceny[2]))) + " Kč s DPH";

        int year = ViewHelper.yearIntOfLong(priceList.getPlatnostOD());
        PriceListRegulBuilder priceListBuilder = new PriceListRegulBuilder(priceList, year);
        PriceListModel priceListRegul = priceListBuilder.getRegulPriceList();
        double[] cenyRegul = Calculation.calculatePriceForPriceListDPH(priceListRegul, subscriptionPoint);

        String cenaVTRegul = "1kWh VT: " + (df2.format(Round.round(cenyRegul[0]))) + " Kč";
        String cenaNTRegul = "NT: " + (df2.format(Round.round(cenyRegul[1]))) + " Kč";
        String cenaMesicRegul = "Měsíc: " + (df2.format(Round.round(cenyRegul[2]))) + " Kč s DPH";

        if (showSelectItem) {
            //zobrazený radiobutton pro výběr
            holder.rbSelectItem.setVisibility(View.VISIBLE);
            holder.relativeLayout.setTag(position);
            holder.relativeLayout.setOnClickListener(v -> {
                selectedItem = (int) v.getTag();
                idSelectedPriceList = priceList.getId();
                notifyDataSetChanged();
                onClickItemListener.setClickPriceListListener(priceList);
            });
            holder.rbSelectItem.setChecked(selectedItem == position);
            if (priceList.getId() == idSelectedPriceList) {
                holder.rbSelectItem.setChecked(true);
                onClickItemListener.setClickPriceListListener(priceList);
            }
        } else {
            //skrytý radiobutton pro výběr
            //nastaví zarovnání na kraj rodiče
            RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) holder.tvProduct.getLayoutParams();
            params.addRule(RelativeLayout.ALIGN_PARENT_START, 1);
            params.removeRule(RelativeLayout.END_OF);
            holder.tvProduct.setLayoutParams(params);
            holder.rbSelectItem.setVisibility(View.GONE);
            holder.relativeLayout.setOnClickListener(v -> {

                if (showButtons >= 0) {
                    RecyclerView.ViewHolder viewHolder = recyclerView.findViewHolderForAdapterPosition(showButtons);
                    if (viewHolder != null)
                        viewHolder.itemView.findViewById(R.id.lnButtonsPriceListItem).setVisibility(View.GONE);
                }

                TransitionManager.beginDelayedTransition(recyclerView);

                if (showButtons == position) {
                    showButtons = -1;
                    onClickItemListener.setClickPriceListListener(null);
                    setShowIdItem(-1);
                } else {
                    showButtons = position;
                    onClickItemListener.setClickPriceListListener(priceList);
                    setShowIdItem(priceList.getId());
                }

                setShowPositionItem(position);
                showButtons(holder, position);
            });

            holder.btnDetail.setOnClickListener(v -> {
                PriceListDetailFragment priceListDetailFragment = PriceListDetailFragment.newInstance(priceList.getId());
                FragmentChange.replace((FragmentActivity) context, priceListDetailFragment, MOVE, true);
            });

            holder.btnEdit.setOnClickListener(v -> {
                PriceListEditFragment priceListEditFragment = PriceListEditFragment.newInstance(priceList.getId());
                FragmentChange.replace((FragmentActivity) context, priceListEditFragment, MOVE, true);
            });

            holder.btnDelete.setOnClickListener(v -> {
                selectedItemId = priceList.getId();
                selectedPosition = position;
                YesNoDialogFragment yesNoDialogFragment = YesNoDialogFragment.newInstance(
                        "Chcete smazat ceník ?", FLAG_DIALOG_FRAGMENT_DELETE_PRICE_LIST,
                        priceList.getRada() + " "
                                + priceList.getProdukt() + ", \n" + priceList.getSazba() + ", \n"
                                + ViewHelper.convertLongToDate(priceList.getPlatnostOD()) + " - "
                                + ViewHelper.convertLongToDate(priceList.getPlatnostDO()));
                yesNoDialogFragment.show(((FragmentActivity) context).getSupportFragmentManager(), "yesNoDialogFragment");

            });

            showButtons(holder, position);
        }

        String distUzemi = context.getResources().getString(R.string.dist_uzemi,priceList.getFirma(), priceList.getDistribuce());
        holder.tvValidityDate.setText(context.getResources().getString(R.string.validity_price_list_date, ViewHelper.convertLongToDate(priceList.getPlatnostOD()),ViewHelper.convertLongToDate(priceList.getPlatnostDO())));
        holder.tvSeries.setText(priceList.getRada());
        holder.tvProduct.setText(priceList.getProdukt());
        holder.tvRate.setText(priceList.getSazba());
        holder.tvFirma.setText(distUzemi);
        holder.tvPriceVT.setText(cenaVT);
        holder.tvPriceNT.setText(cenaNT);
        holder.tvPriceMonth.setText(cenaMesic);

        if (priceListBuilder.isRegulPrice()) {
            holder.tvPriceVTRegul.setText(cenaVTRegul);
            holder.tvPriceNTRegul.setText(cenaNTRegul);
            holder.tvPriceMonthRegul.setText(cenaMesicRegul);
        }


        //poznámky jsou uloženy v třídě Notes a hledají se podle příslušných datumů
        if (priceListBuilder.isRegulPrice()) {
            //ceník obsahuje regulovanou cenu
            holder.tvNote.setText(priceListBuilder.getNotes(context));
            holder.tvNote.setVisibility(View.VISIBLE);
            holder.lnRegulPrice.setVisibility(View.VISIBLE);
        } else {
            holder.tvNote.setVisibility(View.GONE);
            holder.lnRegulPrice.setVisibility(View.GONE);
        }


        if (priceList.getCenaNT() == 0) {
            //jednotarifní sazby
            holder.tvPriceNT.setVisibility(View.GONE);
            holder.tvPriceNTRegul.setVisibility(View.GONE);
        } else {
            //dvoutarifní sazby
            holder.tvPriceNT.setVisibility(View.VISIBLE);
            holder.tvPriceNTRegul.setVisibility(View.VISIBLE);
        }

        holder.lnButtons.setOnClickListener(v -> {
            if (showButtons == position) {
                showButtons = -1;
            } else {
                showButtons = position;
            }
        });
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
     * Zobrazí tlačítka pro editaci a smazání ceníku
     * @param holder MyViewHolder
     * @param position int pozice položky ceníku
     */
    private void showButtons(MyViewHolder holder, int position) {
        setShowPositionItem(showButtons);
        if (position == showButtons) {
            holder.lnButtons.setVisibility(View.VISIBLE);
        } else {
            holder.lnButtons.setVisibility(View.GONE);
        }
    }


    /**
     * Smaže vybraný ceník
     */
    public void deleteItemPrice() {
        deleteItemPrice(selectedItemId, selectedPosition);
        setShowPositionItem(-1);
        setShowIdItem(-1);
    }


    /**
     * Skryje tlačítka pro editaci a smazání ceníku
     */
    public void setHideButtons() {
        setShowPositionItem(-1);
        setShowIdItem(-1);
    }


    /**
     * Uloží pozici položky ceníku, kde jsou zobrazeny tlačítka
     * Hodnota -1 skryje všechny tlačítka
     *
     * @param position int pozice položky ceníku, kde se mají zobrazit tlačítka
     */
    private void setShowPositionItem(int position) {
        shPPriceList.set(ShPPriceList.SHOW_BUTTONS_PRICE_LIST, position);
    }


    /**
     * Uloží id položky ceníku, kde se mají zobrazit tlačítka
     * Hodnota -1  - jsou skryty všechny tlačítka
     *
     * @param id long id položky ceníku, kde se mají zobrazit tlačítka
     */
    private void setShowIdItem(long id) {
        shPPriceList.set(ShPPriceList.SHOW_ID_ITEM_PRICE_LIST, id);
    }


    /**
     * Smaže vybraný ceník podle id
     *
     * @param itemId long id ceníku
     */
    private void deleteItemPrice(long itemId, int position) {
        DataSubscriptionPointSource dataSubscriptionPointSource = new DataSubscriptionPointSource(context);
        dataSubscriptionPointSource.open();
        //seznam odběrných míst
        ArrayList<SubscriptionPointModel> subscriptionPointModels = dataSubscriptionPointSource.loadSubscriptionPoints();
        int pricesTED = 0;
        int pricesFAK = 0;
        int pricesMON = 0;
        //iterace všemi odběrnými místy a hledání použitých ceníků
        for (SubscriptionPointModel subscriptionPointModel : subscriptionPointModels) {
            pricesTED += dataSubscriptionPointSource.countPriceItems(subscriptionPointModel.getTableTED(), itemId);
            pricesFAK += dataSubscriptionPointSource.countPriceItems(subscriptionPointModel.getTableFAK(), itemId);
            pricesMON += dataSubscriptionPointSource.countPriceItems(subscriptionPointModel.getTableO(), itemId);
        }
        dataSubscriptionPointSource.close();

        if (pricesTED > 0 || pricesFAK > 0 || pricesMON > 0) {
            showWarningDialog(pricesTED, pricesFAK, pricesMON, context);
            return;
        }

        DataPriceListSource dataPriceListSource = new DataPriceListSource(context);
        dataPriceListSource.open();
        dataPriceListSource.deletePriceList(itemId);

        dataPriceListSource.close();
        showButtons = -1;
        items.remove(position);
        notifyItemRemoved(position);
        notifyItemRangeChanged(position, getItemCount());
    }


    /**
     * Zobrazí dialog s upozorněním, že ceník nelze smazat, protože je použit v záznamech
     */
    private void showWarningDialog(int ted, int fak, int mon, Context context) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context, R.style.DialogTheme);
        builder.setTitle("Smazání ceníku");
        builder.setMessage("Ceník nelze smazat, protože je použit v těchto záznamech:\n\n" +
                "Období bez faktury " + ted + "x\n\nVe fakturách, " + fak + "x\n\nV měsíčních odečtech: " + mon + "x");
        builder.setPositiveButton("OK", (dialog, which) -> dialog.dismiss());
        builder.show();
    }


    public interface OnClickItemListener {
        void setClickPriceListListener(PriceListModel priceList);
    }
}
