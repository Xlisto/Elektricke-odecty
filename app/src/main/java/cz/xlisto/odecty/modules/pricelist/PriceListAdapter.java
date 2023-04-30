package cz.xlisto.odecty.modules.pricelist;

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

import java.util.ArrayList;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.RecyclerView;
import cz.xlisto.odecty.databaze.DataPriceListSource;
import cz.xlisto.odecty.databaze.DataSubscriptionPointSource;
import cz.xlisto.odecty.dialogs.YesNoDialogFragment;
import cz.xlisto.odecty.models.PriceListRegulBuilder;
import cz.xlisto.odecty.models.SubscriptionPointModel;
import cz.xlisto.odecty.shp.ShPPriceList;
import cz.xlisto.odecty.utils.Calculation;
import cz.xlisto.odecty.R;
import cz.xlisto.odecty.models.PriceListModel;
import cz.xlisto.odecty.ownview.ViewHelper;
import cz.xlisto.odecty.utils.FragmentChange;
import cz.xlisto.odecty.utils.Round;

import static cz.xlisto.odecty.format.DecimalFormatHelper.df2;
import static cz.xlisto.odecty.utils.FragmentChange.Transaction.MOVE;


public class PriceListAdapter extends RecyclerView.Adapter<PriceListAdapter.MyViewHolder> {
    private static final String TAG = "PriceListAdapter";
    private final OnClickItemListener onClickItemListener;
    private final OnLongClickItemListener onLongClickItemListener;
    private final boolean showSelectItem;
    private int selectedItem = -1;
    private long idSelectedPriceList = -1;
    private Context context;
    private final ArrayList<PriceListModel> items;
    private final SubscriptionPointModel subscriptionPoint;
    private int showButtons = -1;
    private RecyclerView recyclerView;
    private ShPPriceList shPPriceList;

    static class MyViewHolder extends RecyclerView.ViewHolder {
        RelativeLayout relativeLayout;
        TextView tvOd, tvDo, tvRada, tvProdukt, tvSazba, tvFirma, tvCenaVT, tvCenaNT, tvCenaMesic,
                tvPoznamka, tvCenaVTRegul, tvCenaNTRegul, tvCenaMesicRegul;
        RadioButton rbSelectItem;
        LinearLayout lnRegulPrice, lnButtons;
        Button btnEdit, btnDelete, btnDetail;

        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }

    //konstruktor
    public PriceListAdapter(Context context, ArrayList<PriceListModel> items, SubscriptionPointModel subscriptionPoint, boolean selectItem, long idSelectedPriceList,
                            OnClickItemListener onClickItemListener, OnLongClickItemListener onLongClickItemListener, RecyclerView recyclerView) {
        this.context = context;
        this.items = items;
        this.showSelectItem = selectItem;
        this.onClickItemListener = onClickItemListener;
        this.onLongClickItemListener = onLongClickItemListener;
        this.idSelectedPriceList = idSelectedPriceList;
        this.subscriptionPoint = subscriptionPoint;
        this.recyclerView = recyclerView;
    }

    // Vytvoření a inicializace objektu View z XML návrhu vzoru položky.
    // Příprava kontejneru, ve kterém budou zobrazena data jednotlivé položky
    @SuppressLint("MissingInflatedId")
    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Získání objektu View položky seznamu z jejího XML návrhu
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_price_list, parent, false);
        MyViewHolder vh = new MyViewHolder(v);
        vh.relativeLayout = v.findViewById(R.id.item_price_list);
        // Získání referencí na jednotlivé komponenty zobrazené seznam
        vh.rbSelectItem = v.findViewById(R.id.rbPriceList);
        vh.tvOd = v.findViewById(R.id.tvPlatnostZacatek);
        vh.tvDo = v.findViewById(R.id.tvPlatnostKonec);
        vh.tvRada = v.findViewById(R.id.tvProduktovaRada);
        vh.tvProdukt = v.findViewById(R.id.tvProdukt);
        vh.tvSazba = v.findViewById(R.id.tvSazba);
        vh.tvFirma = v.findViewById(R.id.tvFirma);
        vh.tvCenaVT = v.findViewById(R.id.tvCenaVT);
        vh.tvCenaNT = v.findViewById(R.id.tvCenaNT);
        vh.tvCenaMesic = v.findViewById(R.id.tvPayment);
        vh.tvCenaVTRegul = v.findViewById(R.id.tvCenaVTRegul);
        vh.tvCenaNTRegul = v.findViewById(R.id.tvCenaNTRegul);
        vh.tvCenaMesicRegul = v.findViewById(R.id.tvCenaMesicRegul);
        vh.tvPoznamka = v.findViewById(R.id.tvPoznamkaItemPriceList);
        vh.lnRegulPrice = v.findViewById(R.id.ln_item_price_list_regul);
        vh.lnButtons = v.findViewById(R.id.lnButtonsPriceListItem);
        vh.btnEdit = v.findViewById(R.id.btnEditPriceListItem);
        vh.btnDelete = v.findViewById(R.id.btnDeletePriceListItem);
        vh.btnDetail = v.findViewById(R.id.btnDetailPriceListItem);

        shPPriceList = new ShPPriceList(context);
        showButtons = shPPriceList.get(ShPPriceList.SHOW_BUTTONS_PRICE_LIST, -1);
        return vh;
    }

    // Naplnění kontejneru daty (kontejner vytvořen v přepsané metodě onCreateViewHolder())
    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, @SuppressLint("RecyclerView") int position) {
        final PriceListModel priceList = items.get(position);
        double[] ceny = Calculation.calculatePriceForPriceListDPH(priceList, subscriptionPoint);
        String cenaVT = "1kWh VT: " + (df2.format(Round.round(ceny[0]))) + " Kč";
        String cenaNT = "NT: " + (df2.format(Round.round(ceny[1]))) + " Kč";
        String cenaMesic = "Měsíc: " + (df2.format(Round.round(ceny[2]))) + " Kč s DPH";

        int year = ViewHelper.yearIntOfLong(priceList.getPlatnostOD());
        int month = 12;
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
            holder.relativeLayout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    selectedItem = (int) v.getTag();
                    idSelectedPriceList = priceList.getId();
                    notifyDataSetChanged();
                    onClickItemListener.setClickPriceListListener(priceList);
                }
            });
            holder.rbSelectItem.setChecked(selectedItem == position);
            if (priceList.getId() == idSelectedPriceList) {
                holder.rbSelectItem.setChecked(true);
                onClickItemListener.setClickPriceListListener(priceList);
            }
        } else {
            //skrytý radiobutton pro výběr
            //nastaví zarovnání na kraj rodiče
            RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) holder.tvProdukt.getLayoutParams();
            params.addRule(RelativeLayout.ALIGN_PARENT_START, 1);
            params.removeRule(RelativeLayout.END_OF);
            holder.tvProdukt.setLayoutParams(params);
            holder.rbSelectItem.setVisibility(View.GONE);
            holder.relativeLayout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    if (showButtons >= 0) {
                        RecyclerView.ViewHolder viewHolder = recyclerView.findViewHolderForAdapterPosition(showButtons);
                        if (viewHolder != null)
                            viewHolder.itemView.findViewById(R.id.lnButtonsPriceListItem).setVisibility(View.GONE);
                    }

                    TransitionManager.beginDelayedTransition(recyclerView);

                    if (showButtons == position)
                        showButtons = -1;
                    else
                        showButtons = position;

                    shPPriceList.set(ShPPriceList.SHOW_BUTTONS_PRICE_LIST, position);
                    showButtons(holder, position);
                }
            });

            holder.btnDetail.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    PriceListDetailFragment priceListDetailFragment = PriceListDetailFragment.newInstance(priceList.getId());
                    FragmentChange.replace((FragmentActivity) context, priceListDetailFragment, MOVE, true);
                }
            });

            holder.btnEdit.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    PriceListEditFragment priceListEditFragment = PriceListEditFragment.newInstance(priceList.getId());
                    FragmentChange.replace((FragmentActivity) context, priceListEditFragment, MOVE, true);
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
                                        deleteItemPrice(priceList.getId(), position);
                                    }
                                }
                            }, "Opravdu chcete smazat ceník ?", priceList.getRada() + " "
                                    + priceList.getProdukt() + ", \n" + priceList.getSazba() + ", \n"
                                    + ViewHelper.convertLongToTime(priceList.getPlatnostOD()) + " - "
                                    + ViewHelper.convertLongToTime(priceList.getPlatnostDO()));
                    yesNoDialogFragment.show(((FragmentActivity) context).getSupportFragmentManager(), "yesNoDialogFragment");

                }
            });

            showButtons(holder, position);
        }

        holder.tvOd.setText("" + ViewHelper.convertLongToTime(priceList.getPlatnostOD()));
        holder.tvDo.setText("" + ViewHelper.convertLongToTime(priceList.getPlatnostDO()));
        holder.tvRada.setText("" + priceList.getRada());
        holder.tvProdukt.setText("" + priceList.getProdukt());
        holder.tvSazba.setText("" + priceList.getSazba());
        holder.tvFirma.setText("" + priceList.getFirma() + "\nDist. území: " + priceList.getDistribuce());
        holder.tvCenaVT.setText(cenaVT);
        holder.tvCenaNT.setText(cenaNT);
        holder.tvCenaMesic.setText(cenaMesic);

        if (priceListBuilder.isRegulPrice()) {
            holder.tvCenaVTRegul.setText(cenaVTRegul);
            holder.tvCenaNTRegul.setText(cenaNTRegul);
            holder.tvCenaMesicRegul.setText(cenaMesicRegul);
        }


        //poznámky jsou uloženy v třídě Notes a hledají se podle příslušných datumů
        if (priceListBuilder.isRegulPrice()) {
            //ceník obsahuje regulovanou cenu
            //holder.tvPoznamka.setText(context.getResources().getString(R.string.poznamka_info));
            holder.tvPoznamka.setText(priceListBuilder.getNotes(context));
            holder.tvPoznamka.setVisibility(View.VISIBLE);
            holder.lnRegulPrice.setVisibility(View.VISIBLE);
        } else {
            holder.tvPoznamka.setVisibility(View.GONE);
            holder.lnRegulPrice.setVisibility(View.GONE);
        }


        if (priceList.getCenaNT() == 0) {
            //jednotarifní sazby
            holder.tvCenaNT.setVisibility(View.GONE);
            holder.tvCenaNTRegul.setVisibility(View.GONE);
        } else {
            //dvoutarifní sazby
            holder.tvCenaNT.setVisibility(View.VISIBLE);
            holder.tvCenaNTRegul.setVisibility(View.VISIBLE);
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

    private void showButtons(MyViewHolder holder, int position) {
        shPPriceList.set(ShPPriceList.SHOW_BUTTONS_PRICE_LIST, showButtons);
        if (position == showButtons) {
            holder.lnButtons.setVisibility(View.VISIBLE);
        } else {
            holder.lnButtons.setVisibility(View.GONE);
        }
    }

    /**
     * Smaže vybraný ceník podle id
     *
     * @param itemId
     */
    private void deleteItemPrice(long itemId, int position) {
        DataSubscriptionPointSource dataSubscriptionPointSource = new DataSubscriptionPointSource(context);
        dataSubscriptionPointSource.open();
        int pricesTED = dataSubscriptionPointSource.countPriceItems(subscriptionPoint.getTableTED(),itemId);
        int pricesFAK = dataSubscriptionPointSource.countPriceItems(subscriptionPoint.getTableFAK(),itemId);
        int pricesMON = dataSubscriptionPointSource.countPriceItems(subscriptionPoint.getTableO(),itemId);
        dataSubscriptionPointSource.close();
        if(pricesTED > 0 || pricesFAK > 0 || pricesMON > 0) {
            showWarningDialog(pricesTED, pricesFAK, pricesMON);
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
    private void showWarningDialog(int ted, int fak, int mon) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Smazání ceníku");
        builder.setMessage("Ceník nelze smazat, protože je použit v těchto záznamech:\n\n" +
                "Období bez faktury " + ted + "x\n\nVe fakturách, " + fak + "x\n\nV měsíčních odečtech: " + mon + "x");
        builder.setPositiveButton("OK", (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    public interface OnClickItemListener {
        void setClickPriceListListener(PriceListModel priceList);
    }

    public interface OnLongClickItemListener {
        void setLongClickItemListener(long id);
    }


}
