package cz.xlisto.elektrodroid.modules.pricelist.pdf;


import android.content.Context;
import android.content.res.Resources;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.color.MaterialColors;

import java.util.List;

import cz.xlisto.elektrodroid.R;


/**
 * Adaptér pro obrazovku {@link SupportedPriceListsFragment} zobrazující úvodní informační panel,
 * rozbalovací upozornění na zkrácené ceníky a abecední seznam karet podporovaných dodavatelů.
 */
public class SupportedPriceListsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_INTRO = 0;
    private static final int VIEW_TYPE_WARNING = 1;
    private static final int VIEW_TYPE_SUPPLIER = 2;
    private static final int HEADER_COUNT = 2;

    private final List<SupportedPriceList> items;
    private final OnPreviewClickListener onPreviewClickListener;
    private final int spanCount;
    @Nullable
    private final String highlightedSupplierId;
    private boolean warningExpanded;


    /**
     * Posluchač klepnutí na náhled podporovaného ceníku.
     */
    public interface OnPreviewClickListener {

        /**
         * Voláno po klepnutí na náhledový obrázek konkrétního dodavatele.
         *
         * @param item Vybraná položka {@link SupportedPriceList}
         */
        void onPreviewClick(@NonNull SupportedPriceList item);

    }


    /**
     * Vytvoří instanci adaptéru pro seznam podporovaných ceníků.
     *
     * @param items                  Seřazený seznam podporovaných ceníků
     * @param spanCount              Aktuální počet sloupců mřížky (pro výpočet cílové velikosti náhledů)
     * @param highlightedSupplierId  Volitelný identifikátor dodavatele pro vizuální zvýraznění karty
     * @param warningExpanded        Výchozí stav rozbalení podrobného seznamu v upozornění
     * @param onPreviewClickListener Posluchač klepnutí na náhled pro otevření detailu
     */
    public SupportedPriceListsAdapter(
            @NonNull List<SupportedPriceList> items,
            int spanCount,
            @Nullable String highlightedSupplierId,
            boolean warningExpanded,
            @NonNull OnPreviewClickListener onPreviewClickListener
    ) {
        this.items = items;
        this.spanCount = Math.max(1, spanCount);
        this.highlightedSupplierId = highlightedSupplierId;
        this.warningExpanded = warningExpanded;
        this.onPreviewClickListener = onPreviewClickListener;
    }


    /**
     * Zjistí, zda zadaná pozice odpovídá hlavičkové položce (úvod nebo upozornění),
     * která má v mřížce zabrat celou šířku.
     *
     * @param position Pozice v adaptéru
     * @return {@code true} pro hlavičkové položky, jinak {@code false}
     */
    public boolean isHeaderPosition(int position) {
        return position < HEADER_COUNT;
    }


    /**
     * Vrátí aktuální stav rozbalení detailního výčtu v panelu upozornění.
     *
     * @return {@code true}, pokud je výčet rozbalen
     */
    public boolean isWarningExpanded() {
        return warningExpanded;
    }


    /**
     * Vyhledá pozici první karty odpovídající zadanému identifikátoru dodavatele.
     *
     * @param supplierId Identifikátor dodavatele (např. {@code "eon"})
     * @return Pozice v adaptéru nebo {@link RecyclerView#NO_POSITION}, pokud nebyl nalezen
     */
    public int findAdapterPositionBySupplierId(@Nullable String supplierId) {
        if (supplierId == null || supplierId.isEmpty()) {
            return RecyclerView.NO_POSITION;
        }
        for (int i = 0; i < items.size(); i++) {
            if (supplierId.equals(items.get(i).supplierId())) {
                return HEADER_COUNT + i;
            }
        }
        return RecyclerView.NO_POSITION;
    }


    @Override
    public int getItemViewType(int position) {
        if (position == 0) {
            return VIEW_TYPE_INTRO;
        }
        if (position == 1) {
            return VIEW_TYPE_WARNING;
        }
        return VIEW_TYPE_SUPPLIER;
    }


    @Override
    public int getItemCount() {
        return HEADER_COUNT + items.size();
    }


    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == VIEW_TYPE_INTRO) {
            View view = inflater.inflate(R.layout.item_supported_price_list_intro, parent, false);
            return new IntroViewHolder(view);
        }
        if (viewType == VIEW_TYPE_WARNING) {
            View view = inflater.inflate(R.layout.item_supported_price_list_warning, parent, false);
            return new WarningViewHolder(view);
        }
        View view = inflater.inflate(R.layout.item_supported_price_list, parent, false);
        return new SupplierViewHolder(view);
    }


    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof WarningViewHolder warningHolder) {
            warningHolder.bind(warningExpanded, () -> {
                warningExpanded = !warningExpanded;
                notifyItemChanged(1);
            });
        } else if (holder instanceof SupplierViewHolder supplierHolder) {
            SupportedPriceList item = items.get(position - HEADER_COUNT);
            boolean highlighted = highlightedSupplierId != null
                    && highlightedSupplierId.equals(item.supplierId());
            supplierHolder.bind(item, highlighted, spanCount, onPreviewClickListener);
        }
    }


    @Override
    public void onViewRecycled(@NonNull RecyclerView.ViewHolder holder) {
        super.onViewRecycled(holder);
        if (holder instanceof SupplierViewHolder supplierHolder) {
            AssetPreviewImageLoader.getInstance().clearTarget(supplierHolder.ivPreview);
        }
    }


    /**
     * ViewHolder pro úvodní informační kartu.
     */
    private static final class IntroViewHolder extends RecyclerView.ViewHolder {

        IntroViewHolder(@NonNull View itemView) {
            super(itemView);
        }

    }


    /**
     * ViewHolder pro rozbalovací upozornění na zkrácené ceníky.
     */
    private static final class WarningViewHolder extends RecyclerView.ViewHolder {

        private final View layoutToggle;
        private final ImageView ivExpandIcon;
        private final View layoutDetails;


        WarningViewHolder(@NonNull View itemView) {
            super(itemView);
            layoutToggle = itemView.findViewById(R.id.layoutWarningToggle);
            ivExpandIcon = itemView.findViewById(R.id.ivWarningExpandIcon);
            layoutDetails = itemView.findViewById(R.id.layoutWarningDetails);
        }


        void bind(boolean expanded, @NonNull Runnable onToggle) {
            layoutDetails.setVisibility(expanded ? View.VISIBLE : View.GONE);
            ivExpandIcon.setImageResource(expanded
                    ? R.drawable.ic_expand_less_24
                    : R.drawable.ic_expand_more_24);

            Context context = itemView.getContext();
            String toggleLabel = context.getString(R.string.supported_price_lists_warning_toggle);
            String stateDesc = context.getString(expanded
                    ? R.string.supported_price_lists_warning_toggle_collapse_cd
                    : R.string.supported_price_lists_warning_toggle_expand_cd);
            layoutToggle.setContentDescription(toggleLabel);
            ViewCompat.setStateDescription(layoutToggle, stateDesc);
            layoutToggle.setOnClickListener(v -> onToggle.run());
        }

    }


    /**
     * ViewHolder pro kartu jednotlivého podporovaného dodavatele s náhledem PDF dokumentu.
     */
    private static final class SupplierViewHolder extends RecyclerView.ViewHolder {

        private final MaterialCardView cardSupplier;
        private final TextView tvSupplierName;
        private final TextView tvSamplePages;
        private final View cardPreviewContainer;
        private final ImageView ivPreview;
        private final View progressPreview;
        private final TextView tvPreviewError;
        private final View tvZoomBadge;
        private final TextView tvDescription;
        private final TextView tvSupplierWarning;


        SupplierViewHolder(@NonNull View itemView) {
            super(itemView);
            cardSupplier = itemView.findViewById(R.id.cardSupplier);
            tvSupplierName = itemView.findViewById(R.id.tvSupplierName);
            tvSamplePages = itemView.findViewById(R.id.tvSamplePages);
            cardPreviewContainer = itemView.findViewById(R.id.cardPreviewContainer);
            ivPreview = itemView.findViewById(R.id.ivPreview);
            progressPreview = itemView.findViewById(R.id.progressPreview);
            tvPreviewError = itemView.findViewById(R.id.tvPreviewError);
            tvZoomBadge = itemView.findViewById(R.id.tvZoomBadge);
            tvDescription = itemView.findViewById(R.id.tvDescription);
            tvSupplierWarning = itemView.findViewById(R.id.tvSupplierWarning);
        }


        void bind(
                @NonNull SupportedPriceList item,
                boolean highlighted,
                int spanCount,
                @NonNull OnPreviewClickListener onPreviewClickListener
        ) {
            Context context = itemView.getContext();
            Resources res = context.getResources();
            float density = res.getDisplayMetrics().density;

            String supplierName = context.getString(item.supplierNameResId());
            String pagesText = res.getQuantityString(
                    R.plurals.supported_price_list_sample_pages,
                    item.samplePageCount(),
                    item.samplePageCount()
            );

            tvSupplierName.setText(supplierName);
            tvSamplePages.setText(pagesText);
            tvDescription.setText(item.descriptionResId());

            if (item.hasWarning()) {
                tvSupplierWarning.setVisibility(View.VISIBLE);
                tvSupplierWarning.setText(item.warningResId());
            } else {
                tvSupplierWarning.setVisibility(View.GONE);
            }

            if (highlighted) {
                int primaryColor = MaterialColors.getColor(
                        cardSupplier,
                        androidx.appcompat.R.attr.colorPrimary,
                        0
                );
                cardSupplier.setStrokeColor(primaryColor);
                cardSupplier.setStrokeWidth(Math.round(2f * density));
            } else {
                int outlineColor = MaterialColors.getColor(
                        cardSupplier,
                        com.google.android.material.R.attr.colorOutlineVariant,
                        0
                );
                cardSupplier.setStrokeColor(outlineColor);
                cardSupplier.setStrokeWidth(Math.round(density));
            }

            cardPreviewContainer.setContentDescription(
                    context.getString(R.string.supported_price_lists_preview_cd, supplierName, pagesText)
            );
            cardPreviewContainer.setOnClickListener(v -> onPreviewClickListener.onPreviewClick(item));

            int screenWidthPx = res.getDisplayMetrics().widthPixels;
            int horizontalMarginsPx = Math.round(48f * density);
            int targetWidthPx = Math.max(240, (screenWidthPx - horizontalMarginsPx) / Math.max(1, spanCount));
            int targetHeightPx = Math.round(260f * density);

            AssetPreviewImageLoader.getInstance().loadThumbnail(
                    context,
                    item.previewAssetPath(),
                    ivPreview,
                    targetWidthPx,
                    targetHeightPx,
                    new AssetPreviewImageLoader.LoadCallback() {
                        @Override
                        public void onLoading() {
                            progressPreview.setVisibility(View.VISIBLE);
                            tvPreviewError.setVisibility(View.GONE);
                            tvZoomBadge.setVisibility(View.GONE);
                        }


                        @Override
                        public void onSuccess() {
                            progressPreview.setVisibility(View.GONE);
                            tvPreviewError.setVisibility(View.GONE);
                            tvZoomBadge.setVisibility(View.VISIBLE);
                        }


                        @Override
                        public void onError() {
                            progressPreview.setVisibility(View.GONE);
                            tvPreviewError.setVisibility(View.VISIBLE);
                            tvZoomBadge.setVisibility(View.GONE);
                        }
                    }
            );
        }

    }

}
