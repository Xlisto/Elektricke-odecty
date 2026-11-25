package cz.xlisto.elektrodroid.modules.pricelist.pdf;


import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import java.util.List;

import cz.xlisto.elektrodroid.MainActivity;
import cz.xlisto.elektrodroid.R;
import cz.xlisto.elektrodroid.ui.OnPaddingChangeListener;


/**
 * Samostatná obrazovka nápovědy „Podporované ceníky“ pro automatický import PDF ceníků elektřiny.
 * <p>
 * Zobrazuje úvodní vysvětlení, rozbalovací upozornění na zkrácené ceníky a abecední přehled
 * podporovaných dodavatelů včetně kompletních náhledů jejich PDF ceníků s možností zvětšení.
 * Umožňuje také přímé spuštění výběru PDF souboru přes existující mechanismus v rodičovské obrazovce.
 */
public class SupportedPriceListsFragment extends Fragment implements OnPaddingChangeListener {

    /**
     * Tag fragmentu pro identifikaci ve {@code FragmentManager}.
     */
    public static final String TAG = "SupportedPriceListsFragment";

    /**
     * Klíč výsledku pro vyvolání výběru PDF ceníku v {@code PriceListFragment}.
     */
    public static final String REQUEST_KEY_SELECT_PDF = "SupportedPriceListsFragment_SELECT_PDF";

    private static final String ARG_HIGHLIGHT_SUPPLIER_ID = "argHighlightSupplierId";
    private static final String STATE_WARNING_EXPANDED = "stateWarningExpanded";
    private static final String STATE_HAS_AUTO_SCROLLED = "stateHasAutoScrolled";
    private static final String STATE_PREVIOUS_TOOLBAR_TITLE = "statePreviousToolbarTitle";
    private static final int TWO_COLUMN_MIN_SCREEN_WIDTH_DP = 600;

    @Nullable
    private String highlightSupplierId;
    private boolean warningExpanded = false;
    private boolean hasAutoScrolled = false;
    @Nullable
    private String previousToolbarTitle;

    @Nullable
    private RecyclerView rvSupportedPriceLists;
    @Nullable
    private SupportedPriceListsAdapter adapter;


    /**
     * Vyžadovaný prázdný konstruktor fragmentu.
     */
    public SupportedPriceListsFragment() {
    }


    /**
     * Vytvoří novou instanci fragmentu bez předvybraného dodavatele.
     *
     * @return Nová instance {@link SupportedPriceListsFragment}
     */
    @NonNull
    public static SupportedPriceListsFragment newInstance() {
        return newInstance(null);
    }


    /**
     * Vytvoří novou instanci fragmentu s volitelným automatickým posunutím a zvýrazněním dodavatele.
     *
     * @param highlightSupplierId Identifikátor dodavatele (např. {@link PdfParserFactory#SUPPLIER_ID_EON}),
     *                            nebo {@code null} pro zobrazení seznamu od začátku
     * @return Nová instance {@link SupportedPriceListsFragment}
     */
    @NonNull
    public static SupportedPriceListsFragment newInstance(@Nullable String highlightSupplierId) {
        SupportedPriceListsFragment fragment = new SupportedPriceListsFragment();
        if (highlightSupplierId != null && !highlightSupplierId.isEmpty()) {
            Bundle args = new Bundle();
            args.putString(ARG_HIGHLIGHT_SUPPLIER_ID, highlightSupplierId);
            fragment.setArguments(args);
        }
        return fragment;
    }


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            highlightSupplierId = getArguments().getString(ARG_HIGHLIGHT_SUPPLIER_ID);
        }
        if (savedInstanceState != null) {
            warningExpanded = savedInstanceState.getBoolean(STATE_WARNING_EXPANDED, false);
            hasAutoScrolled = savedInstanceState.getBoolean(STATE_HAS_AUTO_SCROLLED, false);
            previousToolbarTitle = savedInstanceState.getString(STATE_PREVIOUS_TOOLBAR_TITLE);
        }
    }


    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        return inflater.inflate(R.layout.fragment_supported_price_lists, container, false);
    }


    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (previousToolbarTitle == null && getActivity() instanceof MainActivity mainAct) {
            CharSequence currentTitle = mainAct.getToolbarTitle();
            if (currentTitle != null) {
                previousToolbarTitle = currentTitle.toString();
            }
        }

        RecyclerView recyclerView = view.findViewById(R.id.rvSupportedPriceLists);
        rvSupportedPriceLists = recyclerView;
        MaterialButton btnSelectPdf = view.findViewById(R.id.btnSelectPdfFromSupported);

        int spanCount = computeColumnCount();
        List<SupportedPriceList> supportedItems = PdfParserFactory.getSupportedPriceLists(requireContext());

        adapter = new SupportedPriceListsAdapter(
                supportedItems,
                spanCount,
                highlightSupplierId,
                warningExpanded,
                this::openPreviewDetail
        );

        GridLayoutManager layoutManager = new GridLayoutManager(requireContext(), spanCount);
        layoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                return (adapter != null && adapter.isHeaderPosition(position)) ? spanCount : 1;
            }
        });

        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(adapter);

        if (!hasAutoScrolled && highlightSupplierId != null) {
            int targetPos = adapter.findAdapterPositionBySupplierId(highlightSupplierId);
            if (targetPos != RecyclerView.NO_POSITION) {
                hasAutoScrolled = true;
                recyclerView.post(() -> {
                    if (isAdded() && rvSupportedPriceLists != null) {
                        layoutManager.scrollToPositionWithOffset(targetPos, 0);
                    }
                });
            }
        }

        btnSelectPdf.setOnClickListener(v -> {
            getParentFragmentManager().setFragmentResult(REQUEST_KEY_SELECT_PDF, new Bundle());
            getParentFragmentManager().popBackStack();
        });

        updateViewPadding();
    }


    @Override
    public void onResume() {
        super.onResume();
        if (getActivity() instanceof MainActivity mainAct) {
            mainAct.setToolbarTitle(getString(R.string.supported_price_lists));
        }
        updateViewPadding();
    }


    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        boolean currentExpanded = adapter != null ? adapter.isWarningExpanded() : warningExpanded;
        outState.putBoolean(STATE_WARNING_EXPANDED, currentExpanded);
        outState.putBoolean(STATE_HAS_AUTO_SCROLLED, hasAutoScrolled);
        if (previousToolbarTitle != null) {
            outState.putString(STATE_PREVIOUS_TOOLBAR_TITLE, previousToolbarTitle);
        }
    }


    @Override
    public void onDestroyView() {
        if (adapter != null) {
            warningExpanded = adapter.isWarningExpanded();
        }
        if (rvSupportedPriceLists != null) {
            rvSupportedPriceLists.setAdapter(null);
            rvSupportedPriceLists = null;
        }
        adapter = null;

        if (isRemoving() && previousToolbarTitle != null && getActivity() instanceof MainActivity mainAct) {
            mainAct.setToolbarTitle(previousToolbarTitle);
        }
        super.onDestroyView();
    }


    /**
     * Aktualizuje vnitřní odsazení kořenového pohledu podle výšky AppBaru, boční/spodní navigace
     * a systémových insetů v režimu edge-to-edge.
     */
    public void updateViewPadding() {
        View rootView = getView();
        if (!isAdded() || rootView == null) {
            return;
        }
        if (getActivity() instanceof MainActivity mainAct) {
            float density = getResources().getDisplayMetrics().density;
            int topPaddingPx = mainAct.getTopBarTotalHeight();
            int bottomPaddingPx = mainAct.isNavRail()
                    ? mainAct.getEffectiveNavBarInsetBottom()
                    : mainAct.getBottomPanelMarginPx();
            int leftPaddingPx = mainAct.getLeftNavTotalWidth();
            int rightPaddingPx = mainAct.getEffectiveNavBarInsetRight();

            rootView.setAlpha(1.0f);
            rootView.setPadding(leftPaddingPx, topPaddingPx, rightPaddingPx, bottomPaddingPx);

            if (rvSupportedPriceLists != null) {
                int horizontalPx = Math.round(4f * density);
                int verticalPx = Math.round(6f * density);
                rvSupportedPriceLists.setClipToPadding(false);
                rvSupportedPriceLists.setPadding(horizontalPx, verticalPx, horizontalPx, verticalPx);
            }
        }
    }


    @Override
    public void onPaddingChanged() {
        updateViewPadding();
    }


    /**
     * Určí vhodný počet sloupců seznamu dodavatelů s ohledem na šířku obrazovky
     * tak, aby v režimu na výšku na telefonu zůstal přehledný jeden sloupec
     * a na širších obrazovkách / tabletech se využilo dvousloupcové rozvržení.
     *
     * @return Počet sloupců (1 nebo 2)
     */
    private int computeColumnCount() {
        int screenWidthDp = getResources().getConfiguration().screenWidthDp;
        return screenWidthDp >= TWO_COLUMN_MIN_SCREEN_WIDTH_DP ? 2 : 1;
    }


    /**
     * Otevře celoobrazovkový detailní prohlížeč náhledu vybraného dodavatele.
     *
     * @param item Vybraná položka {@link SupportedPriceList}
     */
    private void openPreviewDetail(@NonNull SupportedPriceList item) {
        if (!isAdded()) {
            return;
        }
        if (getChildFragmentManager().findFragmentByTag(PriceListPreviewDialogFragment.TAG) != null) {
            return;
        }
        String supplierName = getString(item.supplierNameResId());
        PriceListPreviewDialogFragment dialog = PriceListPreviewDialogFragment.newInstance(
                supplierName,
                item.previewAssetPath(),
                item.samplePageCount()
        );
        dialog.show(getChildFragmentManager(), PriceListPreviewDialogFragment.TAG);
    }

}
