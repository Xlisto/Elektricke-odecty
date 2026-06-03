package cz.xlisto.elektrodroid.modules.hdo;


import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.animation.ValueAnimator;
import android.view.animation.DecelerateInterpolator;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

import cz.xlisto.elektrodroid.R;
import cz.xlisto.elektrodroid.databaze.DataHdoSource;
import cz.xlisto.elektrodroid.dialogs.YesNoDialogFragment;
import cz.xlisto.elektrodroid.models.HdoModel;
import cz.xlisto.elektrodroid.models.SubscriptionPointModel;
import cz.xlisto.elektrodroid.utils.FragmentChange;
import cz.xlisto.elektrodroid.utils.SubscriptionPoint;

/**
 * Adapter pro seznam HDO časů.
 * <p>
 * Adapter podporuje dva režimy zobrazení:
 * <ul>
 *     <li><b>Interaktivní režim</b> ({@code clickables=true}) pro {@link HdoFragment}:
 *     zobrazuje checkboxy notifikací NT a umožňuje editaci/smazání záznamů.</li>
 *     <li><b>Náhledový režim</b> ({@code clickables=false}) pro {@link HdoSiteFragment}:
 *     skrývá notifikační checkboxy i editační akce a slouží pouze k náhledu stažených časů.</li>
 * </ul>
 * Obsahuje také animaci rozbalení/sbalení editačních tlačítek.
 */
public class HdoAdapter extends RecyclerView.Adapter<HdoAdapter.MyViewHolder> {
    private static final String TAG = "HdoAdapter";
    private final ArrayList<HdoModel> items;
    private final RecyclerView recyclerView;
    private int showButtons = -1;
    public static final String FLAG_HDO_ADAPTER_DELETE = "flagHdoAdapterDelete";
    private long selectedId;
    private int selectedPosition;
    private final boolean clickables;


    /**
     * ViewHolder pro jednu položku HDO seznamu.
     */
    public static class MyViewHolder extends RecyclerView.ViewHolder {
        TextView tvRele, tvDate, tvTime, tvDayMon, tvDayTue, tvDayWed, tvDayThu, tvDayFri, tvDaySat, tvDaySun;
        LinearLayout lnHdoOut, lnHdoContent, lnNotifyArea, lnHdoButtons, lnHdoDays;
        CheckBox cbNotifyStart, cbNotifyEnd;
        Button btnEdit, btnDelete;
        ValueAnimator buttonsAnimator;

        /**
         * Vytvoří držák view pro položku RecyclerView.
         *
         * @param itemView kořenový view položky
         */
        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }


    /**
     * Vytvoří adapter HDO položek.
     *
     * @param items       seznam HDO záznamů
     * @param recyclerView RecyclerView, ve kterém se seznam zobrazuje
     * @param clickables  {@code true} = interaktivní režim (editace, mazání, checkboxy notifikací),
     *                    {@code false} = náhledový režim (bez checkboxů a bez akcí)
     */
    public HdoAdapter(ArrayList<HdoModel> items, RecyclerView recyclerView, boolean clickables) {
        this.items = items;
        this.recyclerView = recyclerView;
        this.clickables = clickables;
    }


    /**
     * Vytvoří nový ViewHolder pro položku seznamu.
     */
    @NonNull
    @Override
    public HdoAdapter.MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_hdo, parent, false);
        MyViewHolder vh = new MyViewHolder(v);

        vh.lnHdoOut = v.findViewById(R.id.lnHdoOut);
        vh.lnHdoContent = v.findViewById(R.id.lnHdoContent);
        vh.lnNotifyArea = v.findViewById(R.id.lnNotifyArea);
        vh.lnHdoButtons = v.findViewById(R.id.lnButtonsHdo);
        vh.tvRele = v.findViewById(R.id.tvRele);
        vh.tvDate = v.findViewById(R.id.tvDateHdoSite);
        vh.tvTime = v.findViewById(R.id.tvTime);
        vh.btnEdit = v.findViewById(R.id.btnEditHdo);
        vh.btnDelete = v.findViewById(R.id.btnDeleteHdo);
        vh.lnHdoDays = v.findViewById(R.id.lnHdoDays);
        vh.tvDayMon = v.findViewById(R.id.tvDayMon);
        vh.tvDayTue = v.findViewById(R.id.tvDayTue);
        vh.tvDayWed = v.findViewById(R.id.tvDayWed);
        vh.tvDayThu = v.findViewById(R.id.tvDayThu);
        vh.tvDayFri = v.findViewById(R.id.tvDayFri);
        vh.tvDaySat = v.findViewById(R.id.tvDaySat);
        vh.tvDaySun = v.findViewById(R.id.tvDaySun);
        vh.cbNotifyStart = v.findViewById(R.id.cbNotifyStart);
        vh.cbNotifyEnd = v.findViewById(R.id.cbNotifyEnd);

        return vh;
    }


    /**
     * Naplní položku daty a nastaví UI podle režimu adapteru.
     * <p>
     * V interaktivním režimu nastaví checkboxy notifikací a jejich posluchače,
     * v náhledovém režimu skryje celou notifikační oblast.
     */
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

        if (item.getDistributionArea().equals(DistributionArea.PRE.toString())) {
            holder.tvDate.setVisibility(View.VISIBLE);
            holder.lnHdoDays.setVisibility(View.GONE);
            holder.tvDate.setText(item.getDateFrom());
        } else {
            holder.tvDate.setVisibility(View.GONE);
            holder.lnHdoDays.setVisibility(View.VISIBLE);
        }

        holder.tvTime.setText(holder.itemView.getContext().getResources().getString(R.string.time, item.getTimeFrom(), item.getTimeUntil()));

        holder.cbNotifyStart.setOnCheckedChangeListener(null);
        holder.cbNotifyEnd.setOnCheckedChangeListener(null);

        if (clickables) {
            holder.lnNotifyArea.setVisibility(View.VISIBLE);
            holder.cbNotifyStart.setChecked(item.getNotifyStart() == 1);
            holder.cbNotifyEnd.setChecked(item.getNotifyEnd() == 1);

            holder.cbNotifyStart.setOnCheckedChangeListener((buttonView, isChecked) -> {
                item.setNotifyStart(isChecked ? 1 : 0);
                updateNotifyFlags(item);
            });

            holder.cbNotifyEnd.setOnCheckedChangeListener((buttonView, isChecked) -> {
                item.setNotifyEnd(isChecked ? 1 : 0);
                updateNotifyFlags(item);
            });
        } else {
            // Rezim nahledu (HdoSiteFragment): notifikace se zde nenastavuji.
            holder.lnNotifyArea.setVisibility(View.GONE);
        }

        holder.lnHdoContent.setOnClickListener(v1 -> {
            if (!clickables) return;
            if (showButtons >= 0 && showButtons == position) {
                collapseButtons(holder);
                showButtons = -1;
            } else {
                if (showButtons >= 0) {
                    RecyclerView.ViewHolder viewHolder = recyclerView.findViewHolderForAdapterPosition(showButtons);
                    if (viewHolder instanceof MyViewHolder) {
                        collapseButtons((MyViewHolder) viewHolder);
                    } else if (viewHolder != null) {
                        viewHolder.itemView.findViewById(R.id.lnButtonsHdo).setVisibility(View.GONE);
                    }
                }
                showButtons = position;
                expandButtons(holder);
            }
        });

        holder.btnEdit.setOnClickListener(v -> {
            HdoModel hdoModel = items.get(position);
            selectedId = hdoModel.getId();
            selectedPosition = position;
            HdoEditFragment hdoEditFragment = HdoEditFragment.newInstance(hdoModel);
            FragmentChange.replace(((FragmentActivity) v.getContext()), hdoEditFragment, FragmentChange.Transaction.MOVE, true);
        });

        holder.btnDelete.setOnClickListener(v -> {
            HdoModel hdoModel = items.get(position);
            selectedId = hdoModel.getId();
            selectedPosition = position;
            YesNoDialogFragment.newInstance("Odstranit záznam HDO", FLAG_HDO_ADAPTER_DELETE).show(((FragmentActivity) v.getContext()).getSupportFragmentManager(), TAG);
        });
        bindButtonsState(holder, position);


    }


    /**
     * Vrátí počet položek v adapteru.
     */
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
    private void bindButtonsState(MyViewHolder holder, int position) {
        cancelButtonsAnimator(holder);
        ViewGroup.LayoutParams params = holder.lnHdoButtons.getLayoutParams();
        if (showButtons == position) {
            params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
            holder.lnHdoButtons.setLayoutParams(params);
            holder.lnHdoButtons.setVisibility(View.VISIBLE);
            holder.lnHdoButtons.setAlpha(1f);
        } else {
            params.height = 0;
            holder.lnHdoButtons.setLayoutParams(params);
            holder.lnHdoButtons.setVisibility(View.GONE);
            holder.lnHdoButtons.setAlpha(0f);
        }
    }


    /**
     * Rozbalí sekci tlačítek dvoufázově: nejdřív výška, pak fade-in.
     */
    private void expandButtons(MyViewHolder holder) {
        cancelButtonsAnimator(holder);
        final LinearLayout buttons = holder.lnHdoButtons;
        buttons.setVisibility(View.VISIBLE);
        buttons.setAlpha(0f);

        ViewGroup.LayoutParams params = buttons.getLayoutParams();
        int targetHeight = measureViewHeight(buttons, holder.itemView.getWidth());
        if (targetHeight <= 0) {
            buttons.setVisibility(View.VISIBLE);
            buttons.setAlpha(1f);
            params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
            buttons.setLayoutParams(params);
            return;
        }

        params.height = 0;
        buttons.setLayoutParams(params);

        holder.buttonsAnimator = ValueAnimator.ofInt(0, targetHeight);
        holder.buttonsAnimator.setDuration(220L);
        holder.buttonsAnimator.setInterpolator(new DecelerateInterpolator());
        holder.buttonsAnimator.addUpdateListener(animation -> {
            params.height = (int) animation.getAnimatedValue();
            buttons.setLayoutParams(params);
        });
        holder.buttonsAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
                buttons.setLayoutParams(params);
                startButtonsFadeIn(holder, buttons);
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                holder.buttonsAnimator = null;
            }
        });
        holder.buttonsAnimator.start();
    }


    /**
     * Spustí zobrazení tlačítek z transparentního stavu.
     */
    private void startButtonsFadeIn(MyViewHolder holder, LinearLayout buttons) {
        holder.buttonsAnimator = null;
        holder.buttonsAnimator = ValueAnimator.ofFloat(0f, 1f);
        holder.buttonsAnimator.setDuration(120L);
        holder.buttonsAnimator.setInterpolator(new DecelerateInterpolator());
        holder.buttonsAnimator.addUpdateListener(animation -> buttons.setAlpha((Float) animation.getAnimatedValue()));
        holder.buttonsAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                buttons.setAlpha(1f);
                holder.buttonsAnimator = null;
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                holder.buttonsAnimator = null;
            }
        });
        holder.buttonsAnimator.start();
    }


    /**
     * Sbalí sekci tlačítek dvoufázově: nejdřív fade-out, pak výška.
     */
    private void collapseButtons(MyViewHolder holder) {
        cancelButtonsAnimator(holder);
        final LinearLayout buttons = holder.lnHdoButtons;
        int currentHeight = buttons.getHeight();
        if (currentHeight <= 0) {
            buttons.setVisibility(View.GONE);
            ViewGroup.LayoutParams params = buttons.getLayoutParams();
            params.height = 0;
            buttons.setLayoutParams(params);
            buttons.setAlpha(0f);
            return;
        }

        holder.buttonsAnimator = ValueAnimator.ofFloat(1f, 0f);
        holder.buttonsAnimator.setDuration(110L);
        holder.buttonsAnimator.setInterpolator(new DecelerateInterpolator());
        holder.buttonsAnimator.addUpdateListener(animation -> buttons.setAlpha((Float) animation.getAnimatedValue()));
        holder.buttonsAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                buttons.setAlpha(0f);
                holder.buttonsAnimator = null;
                collapseButtonsHeight(holder, buttons);
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                holder.buttonsAnimator = null;
            }
        });
        holder.buttonsAnimator.start();
    }


    /**
     * Sbalí výšku sekce tlačítek na nulu.
     */
    private void collapseButtonsHeight(MyViewHolder holder, LinearLayout buttons) {
        final ViewGroup.LayoutParams params = buttons.getLayoutParams();
        int currentHeight = buttons.getHeight();
        if (currentHeight <= 0) {
            buttons.setVisibility(View.GONE);
            params.height = 0;
            buttons.setLayoutParams(params);
            return;
        }

        holder.buttonsAnimator = ValueAnimator.ofInt(currentHeight, 0);
        holder.buttonsAnimator.setDuration(180L);
        holder.buttonsAnimator.setInterpolator(new DecelerateInterpolator());
        holder.buttonsAnimator.addUpdateListener(animation -> {
            params.height = (int) animation.getAnimatedValue();
            buttons.setLayoutParams(params);
        });
        holder.buttonsAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                buttons.setVisibility(View.GONE);
                params.height = 0;
                buttons.setLayoutParams(params);
                holder.buttonsAnimator = null;
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                holder.buttonsAnimator = null;
            }
        });
        holder.buttonsAnimator.start();
    }


    /**
     * Zruší běžící animaci tlačítek pro konkrétní ViewHolder.
     */
    private void cancelButtonsAnimator(MyViewHolder holder) {
        if (holder.buttonsAnimator != null) {
            holder.buttonsAnimator.cancel();
            holder.buttonsAnimator = null;
        }
    }


    /**
     * Změří cílovou výšku sekce tlačítek pro animaci rozbalení.
     */
    private int measureViewHeight(LinearLayout view, int parentWidth) {
        int width = parentWidth;
        if (width <= 0) {
            width = recyclerView.getWidth();
        }
        if (width <= 0) {
            width = recyclerView.getMeasuredWidth();
        }
        if (width <= 0) {
            width = view.getWidth();
        }
        if (width <= 0) {
            return 0;
        }

        int widthSpec = View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY);
        int heightSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
        view.measure(widthSpec, heightSpec);
        return view.getMeasuredHeight();
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


    /**
     * Uloží stav notifikačních voleb HDO záznamu do databáze.
     *
     * @param hdoModel upravený HDO model
     */
    private void updateNotifyFlags(HdoModel hdoModel) {
        SubscriptionPointModel subscriptionPointModel = SubscriptionPoint.load(recyclerView.getContext());
        if (subscriptionPointModel == null) {
            return;
        }
        DataHdoSource dataHdoSource = new DataHdoSource(recyclerView.getContext());
        dataHdoSource.updateHdo(hdoModel, subscriptionPointModel.getTableHDO());
    }
}
