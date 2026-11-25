package cz.xlisto.elektrodroid.modules.pricelist;


import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import cz.xlisto.elektrodroid.MainActivity;
import cz.xlisto.elektrodroid.R;
import cz.xlisto.elektrodroid.databaze.DataPriceListSource;
import cz.xlisto.elektrodroid.format.DecimalFormatHelper;
import cz.xlisto.elektrodroid.models.PriceListModel;
import cz.xlisto.elektrodroid.models.PriceListRegulBuilder;
import cz.xlisto.elektrodroid.modules.pricelist.pdf.PdfParserUtils;
import cz.xlisto.elektrodroid.ownview.LabelPriceDetail;
import cz.xlisto.elektrodroid.ownview.ViewHelper;
import cz.xlisto.elektrodroid.ui.OnPaddingChangeListener;


/**
 * Fragment pro zobrazení detailů ceníku elektřiny.
 * <p>
 * Přehledně zobrazuje všechny složky ceníku včetně obchodní dodávky (VT, NT, stálý plat),
 * distribuční sazby a jističů, regulovaných poplatků (systémové služby, OTE, POZE),
 * daní (daň z elektřiny, DPH) a aplikovatelných cenových stropů a poznámek.
 */
public class PriceListDetailFragment extends Fragment implements OnPaddingChangeListener {

    /**
     * Klíč příznaku výsledku pro mezigrafickou komunikaci mezi fragmenty.
     */
    public static final String PRICE_LIST_DETAIL_FRAGMENT = "price_list_detail_fragment";

    /**
     * Klíč předávaného ID ceníku ve výsledkovém balíčku.
     */
    public static final String PRICE_LIST_ID = "price_list_id";

    private static final String PRICE_ID = "price_list_id";
    private static final String SHOW_IN_FRAGMENT = "show_in_fragment";

    private LabelPriceDetail ldnDate;
    private LabelPriceDetail ldnSeries;
    private LabelPriceDetail ldnProduct;
    private LabelPriceDetail ldnRate;
    private LabelPriceDetail ldnCompany;
    private LabelPriceDetail ldnArea;
    private LabelPriceDetail ldnVtSupply;
    private LabelPriceDetail ldnNtSupply;
    private LabelPriceDetail ldnMonthlyPayment;
    private LabelPriceDetail ldnVtDistribution;
    private LabelPriceDetail ldnNtDistribution;
    private LabelPriceDetail ldnJ0;
    private LabelPriceDetail ldnJ1;
    private LabelPriceDetail ldnJ2;
    private LabelPriceDetail ldnJ3;
    private LabelPriceDetail ldnJ4;
    private LabelPriceDetail ldnJ5;
    private LabelPriceDetail ldnJ6;
    private LabelPriceDetail ldnJ7;
    private LabelPriceDetail ldnJ8;
    private LabelPriceDetail ldnJ9;
    private LabelPriceDetail ldnJ10;
    private LabelPriceDetail ldnJ11;
    private LabelPriceDetail ldnJ12;
    private LabelPriceDetail ldnJ13;
    private LabelPriceDetail ldnJ14;
    private LabelPriceDetail ldnSystemServices;
    private LabelPriceDetail ldnOte;
    private LabelPriceDetail ldnPoze1;
    private LabelPriceDetail ldnPoze2;
    private LabelPriceDetail ldnTax;
    private LabelPriceDetail ldnVat;

    private TextView tvNote;
    private TextView tvNoteVt;
    private TextView tvNoteNt;
    private TextView tvNotePayment;
    private TextView tvNotePoze1;
    private TextView tvNotePoze2;
    private TextView tvNoteOte;
    private View containerDetailOriginBadge;
    private ImageView ivDetailOriginIcon;
    private TextView tvDetailOriginTitle;
    private TextView tvDetailOriginDescription;
    private TextView tvDetailOriginDiff;
    private LinearLayout containerBreakers;

    private long priceId = -1L;
    private boolean showInFragment = false;


    /**
     * Vytvoří novou instanci fragmentu pro zobrazení detailu ceníku.
     *
     * @param priceListId ID ceníku v databázi
     * @return Nová instance {@link PriceListDetailFragment}
     */
    public static PriceListDetailFragment newInstance(long priceListId) {
        return newInstance(priceListId, false);
    }


    /**
     * Vytvoří novou instanci fragmentu s možností přizpůsobení rozvržení pro vnoření do jiného fragmentu.
     *
     * @param priceListId    ID ceníku v databázi
     * @param showInFragment {@code true} pokud se fragment zobrazuje vnořený (např. v režimu na šířku), jinak {@code false}
     * @return Nová instance {@link PriceListDetailFragment}
     */
    public static PriceListDetailFragment newInstance(long priceListId, boolean showInFragment) {
        PriceListDetailFragment fragment = new PriceListDetailFragment();
        Bundle args = new Bundle();
        args.putLong(PRICE_ID, priceListId);
        args.putBoolean(SHOW_IN_FRAGMENT, showInFragment);
        fragment.setArguments(args);
        return fragment;
    }


    /**
     * Načte předané argumenty fragmentu (ID ceníku a příznak vnořeného zobrazení).
     */
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            priceId = getArguments().getLong(PRICE_ID, -1L);
            showInFragment = getArguments().getBoolean(SHOW_IN_FRAGMENT, false);
        }
    }


    /**
     * Vytvoří a vrátí strom pohledů fragmentu (v závislosti na orientaci obrazovky a vnoření),
     * inicializuje komponenty UI a zaregistruje posluchač výsledků pro zobrazení zvoleného ceníku.
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_price_list_detail, container, false);

        containerDetailOriginBadge = v.findViewById(R.id.containerDetailOriginBadge);
        ivDetailOriginIcon = v.findViewById(R.id.ivDetailOriginIcon);
        tvDetailOriginTitle = v.findViewById(R.id.tvDetailOriginTitle);
        tvDetailOriginDescription = v.findViewById(R.id.tvDetailOriginDescription);
        tvDetailOriginDiff = v.findViewById(R.id.tvDetailOriginDiff);

        ldnDate = v.findViewById(R.id.ldnPlatnost);
        ldnSeries = v.findViewById(R.id.ldnRada);
        ldnProduct = v.findViewById(R.id.ldnProdukt);
        ldnRate = v.findViewById(R.id.ldnSazba);
        ldnCompany = v.findViewById(R.id.ldnDodavatel);
        ldnArea = v.findViewById(R.id.ldnUzemi);
        tvNote = v.findViewById(R.id.tvPoznamkaDetail);

        ldnVtSupply = v.findViewById(R.id.ldn_VT_dodavka);
        ldnNtSupply = v.findViewById(R.id.ldn_NT_dodavka);
        ldnMonthlyPayment = v.findViewById(R.id.ldn_staly_plat);
        tvNoteVt = v.findViewById(R.id.tvPoznamkaVT);
        tvNoteNt = v.findViewById(R.id.tvPoznamkaNT);
        tvNotePayment = v.findViewById(R.id.tvPoznamkaPlat);
        tvNotePoze1 = v.findViewById(R.id.tvPoznamkaPOZE1);
        tvNotePoze2 = v.findViewById(R.id.tvPoznamkaPOZE2);
        tvNoteOte = v.findViewById(R.id.tvPoznamkaOTE);

        ldnVtDistribution = v.findViewById(R.id.ldn_VT_distribuce);
        ldnNtDistribution = v.findViewById(R.id.ldn_NT_distribuce);

        ldnJ0 = v.findViewById(R.id.ldn_J0);
        ldnJ1 = v.findViewById(R.id.ldn_J1);
        ldnJ2 = v.findViewById(R.id.ldn_J2);
        ldnJ3 = v.findViewById(R.id.ldn_J3);
        ldnJ4 = v.findViewById(R.id.ldn_J4);
        ldnJ5 = v.findViewById(R.id.ldn_J5);
        ldnJ6 = v.findViewById(R.id.ldn_J6);
        ldnJ7 = v.findViewById(R.id.ldn_J7);
        ldnJ8 = v.findViewById(R.id.ldn_J8);
        ldnJ9 = v.findViewById(R.id.ldn_J9);
        ldnJ10 = v.findViewById(R.id.ldn_J10);
        ldnJ11 = v.findViewById(R.id.ldn_J11);
        ldnJ12 = v.findViewById(R.id.ldn_J12);
        ldnJ13 = v.findViewById(R.id.ldn_J13);
        ldnJ14 = v.findViewById(R.id.ldn_J14);

        containerBreakers = v.findViewById(R.id.containerJistice);

        ldnSystemServices = v.findViewById(R.id.ldn_systemove_sluzby);
        ldnOte = v.findViewById(R.id.ldn_OTE);
        ldnPoze1 = v.findViewById(R.id.ldn_POZE1);
        ldnPoze2 = v.findViewById(R.id.ldn_POZE2);

        ldnTax = v.findViewById(R.id.ldn_dan);
        ldnVat = v.findViewById(R.id.ldn_DPH);

        loadPrice(priceId);

        getParentFragmentManager().setFragmentResultListener(
                PRICE_LIST_DETAIL_FRAGMENT,
                getViewLifecycleOwner(),
                (requestKey, result) -> {
                    long newPriceId = result.getLong(PRICE_LIST_ID);
                    loadPrice(newPriceId);
                }
        );

        return v;
    }


    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        updateViewPadding();
    }


    @Override
    public void onResume() {
        super.onResume();
        updateViewPadding();
    }


    public void updateViewPadding() {
        if (!isAdded()) return;
        if (!(getActivity() instanceof MainActivity act)) return;
        float density = getResources().getDisplayMetrics().density;

        int topPaddingPx = act.getTopBarTotalHeight() + (int) (6 * density);
        int bottomPaddingPx = act.isNavRail()
                ? act.getEffectiveNavBarInsetBottom() + (int) (16 * density)
                : act.getEffectiveNavBarInsetBottom() + (int) (112 * density);

        boolean isRightColumn = showInFragment || (act.isNavRail() && getView() != null && getView().getParent() instanceof ViewGroup parent && parent.getId() == R.id.fl_price_list_right);
        int leftPaddingPx = isRightColumn
                ? (int) (2 * density)
                : act.getLeftNavTotalWidth() + (int) (2 * density);

        int rightPaddingPx = (int) (5 * density);

        View v = getView();
        if (v == null) return;
        v.setAlpha(1.0f);
        if (v instanceof ViewGroup vg) {
            vg.setClipToPadding(false);
            vg.setPadding(
                    leftPaddingPx,
                    topPaddingPx,
                    rightPaddingPx,
                    bottomPaddingPx
            );
        }
    }


    @Override
    public void onPaddingChanged() {
        updateViewPadding();
    }


    /**
     * Načte a zobrazí detaily ceníku na základě zadaného ID ceníku.
     *
     * @param priceId ID ceníku k načtení z databáze
     */
    public void loadPrice(long priceId) {
        if (priceId <= 0) return;
        this.priceId = priceId;

        DataPriceListSource dataPriceListSource = new DataPriceListSource(requireActivity());
        dataPriceListSource.open();
        PriceListModel priceList = dataPriceListSource.readPrice(priceId);
        dataPriceListSource.close();

        if (priceList == null) return;

        PriceListOriginHelper.OriginType originType = PriceListOriginHelper.classify(requireContext(), priceList);
        PriceListOriginHelper.bindBadge(
                requireContext(),
                originType,
                containerDetailOriginBadge,
                ivDetailOriginIcon,
                tvDetailOriginTitle,
                false
        );
        int textColor = ContextCompat.getColor(
                requireContext(),
                originType.getTextColorRes()
        );
        tvDetailOriginDescription.setText(originType.getDetailLabelRes());
        tvDetailOriginDescription.setTextColor(textColor);

        if (originType == PriceListOriginHelper.OriginType.CUSTOM_REGULATED) {
            List<String> diffs = PriceListOriginHelper.getDifferingRegulatedFields(requireContext(), priceList);
            if (!diffs.isEmpty()) {
                tvDetailOriginDiff.setText(getString(
                        R.string.pricelist_origin_custom_regul_diff_prefix,
                        String.join(", ", diffs)
                ));
                tvDetailOriginDiff.setTextColor(textColor);
                tvDetailOriginDiff.setVisibility(View.VISIBLE);
            } else {
                tvDetailOriginDiff.setVisibility(View.GONE);
            }
        } else {
            tvDetailOriginDiff.setVisibility(View.GONE);
        }

        long dateStart = priceList.getValidFrom();
        long dateEnd = priceList.getValidTo();

        ldnDate.setPrice(ViewHelper.convertLongToDate(dateStart) + " - " + ViewHelper.convertLongToDate(dateEnd));

        ldnSeries.setPrice(priceList.getSeries());
        ldnProduct.setPrice(priceList.getProduct());
        ldnRate.setPrice(priceList.getRate());
        ldnCompany.setPrice(priceList.getCompany());
        ldnArea.setPrice(PdfParserUtils.getFullDistribuceName(priceList.getDistribution()));

        ldnVtSupply.setPrice(DecimalFormatHelper.df2.format(priceList.getPriceVT()));
        ldnNtSupply.setPrice(DecimalFormatHelper.df2.format(priceList.getPriceNT()));
        ldnMonthlyPayment.setPrice(DecimalFormatHelper.df2.format(priceList.getMonthlyPayment()));

        ldnVtDistribution.setPrice(DecimalFormatHelper.df2.format(priceList.getDistVT()));
        ldnNtDistribution.setPrice(DecimalFormatHelper.df2.format(priceList.getDistNT()));

        List<BreakerEntry> allBreakers = createAllBreakers(priceList);

        List<LabelPriceDetail> activeViews = new ArrayList<>();
        for (BreakerEntry entry : allBreakers) {
            if (entry.value() != 0.0) {
                entry.view().setPrice(DecimalFormatHelper.df2.format(entry.value()));
                activeViews.add(entry.view());
            }
        }

        if (containerBreakers != null) {
            setupBreakersGrid(containerBreakers, activeViews);
        }

        ldnSystemServices.setPrice(DecimalFormatHelper.df2.format(priceList.getSystemServices()));
        ldnOte.setPrice(DecimalFormatHelper.df2.format(priceList.getActivity()));
        ldnPoze1.setPrice(DecimalFormatHelper.df2.format(priceList.getPoze1()));
        ldnPoze2.setPrice(DecimalFormatHelper.df2.format(priceList.getPoze2()));

        ldnTax.setPrice(DecimalFormatHelper.df2.format(priceList.getTax()));
        ldnVat.setPrice(DecimalFormatHelper.df2.format(priceList.getVat()));

        Calendar calendar = ViewHelper.parseCalendarFromString(ldnDate.getPrice());
        int year = calendar.get(Calendar.YEAR);
        if (year < 2016) {
            ldnOte.setPrice(DecimalFormatHelper.df2.format(priceList.getOte()));
            ldnOte.setLabel(getString(R.string.cinnost_ote));
            ldnOte.setItem(getString(R.string.kc_MWh));
            ldnPoze2.setPrice(DecimalFormatHelper.df2.format(priceList.getOze()));
            ldnPoze2.setLabel(getString(R.string.podpora_vykupu_el_z_oze_kvet_a_dz));
            ldnPoze1.setVisibility(View.GONE);
        }

        // Přenastavení ceníku pro rok 2024 (1719784800000 = 1.7.2024)
        if (dateStart >= 1719784800000L) {
            ldnOte.setLabel(getString(R.string.provoz_nesitove_infrastruktury));
            ldnOte.setItem(getString(R.string.kc_om));
        }

        long transition2024 = 1719784800000L; // 1.7.2024
        long transition2025 = 1756677600000L; // 1.9.2025
        if (year == 2024) {
            if (dateStart < transition2024 && dateEnd >= transition2024) {
                tvNoteOte.setText(getText(R.string.provoz_nesitove_infrastruktury_poznamka));
                tvNoteOte.setVisibility(View.VISIBLE);
            } else {
                tvNoteOte.setVisibility(View.GONE);
            }
        } else if (year == 2025) {
            if (dateStart < transition2025 && dateEnd >= transition2025) {
                tvNoteOte.setText(getText(R.string.nova_cena_infrastruktua_poznamka));
                tvNoteOte.setVisibility(View.VISIBLE);
            } else {
                tvNoteOte.setVisibility(View.GONE);
            }
        } else {
            tvNoteOte.setVisibility(View.GONE);
        }

        // Poznámky a cenové stropy
        PriceListRegulBuilder priceListRegulBuilder = new PriceListRegulBuilder(priceList, year);
        String note = priceListRegulBuilder.getNotes(requireActivity());
        String maxVT = priceListRegulBuilder.getMaxVT();
        String maxNT = priceListRegulBuilder.getMaxNT();
        String maxPayment = priceListRegulBuilder.getMaxPayment();
        String maxPOZE1 = priceListRegulBuilder.getMaxPOZE();

        if (priceListRegulBuilder.isRegulPrice()) {
            tvNote.setText(note);
            tvNote.setVisibility(View.VISIBLE);

            // Zobrazení stropů přímo pod příslušným sloupcem (VT, NT, Měsíční plat)
            setNote(tvNoteVt, maxVT);
            setNote(tvNoteNt, maxNT);
            if (priceList.getDistNT() == 0.0) {
                tvNoteNt.setVisibility(View.GONE);
            }
            setNote(tvNotePayment, maxPayment);

            // Zrušení POZE se zobrazuje pouze jednou pod sekcí Ostatní služby
            setNote(tvNotePoze1, maxPOZE1);
            tvNotePoze2.setVisibility(View.GONE);
        } else {
            tvNote.setVisibility(View.GONE);
            tvNoteVt.setVisibility(View.GONE);
            tvNoteNt.setVisibility(View.GONE);
            tvNotePayment.setVisibility(View.GONE);
            tvNotePoze1.setVisibility(View.GONE);
            tvNotePoze2.setVisibility(View.GONE);
        }
    }


    @NonNull
    private List<BreakerEntry> createAllBreakers(PriceListModel priceList) {
        List<BreakerEntry> allBreakers = new ArrayList<>();
        allBreakers.add(new BreakerEntry(ldnJ0, priceList.getJ0()));
        allBreakers.add(new BreakerEntry(ldnJ1, priceList.getJ1()));
        allBreakers.add(new BreakerEntry(ldnJ2, priceList.getJ2()));
        allBreakers.add(new BreakerEntry(ldnJ3, priceList.getJ3()));
        allBreakers.add(new BreakerEntry(ldnJ4, priceList.getJ4()));
        allBreakers.add(new BreakerEntry(ldnJ5, priceList.getJ5()));
        allBreakers.add(new BreakerEntry(ldnJ6, priceList.getJ6()));
        allBreakers.add(new BreakerEntry(ldnJ7, priceList.getJ7()));
        allBreakers.add(new BreakerEntry(ldnJ10, priceList.getJ10()));
        allBreakers.add(new BreakerEntry(ldnJ11, priceList.getJ11()));
        allBreakers.add(new BreakerEntry(ldnJ12, priceList.getJ12()));
        allBreakers.add(new BreakerEntry(ldnJ13, priceList.getJ13()));
        allBreakers.add(new BreakerEntry(ldnJ14, priceList.getJ14()));
        allBreakers.add(new BreakerEntry(ldnJ8, priceList.getJ8()));
        allBreakers.add(new BreakerEntry(ldnJ9, priceList.getJ9()));
        return allBreakers;
    }


    /**
     * Nastaví text poznámky do zadaného {@link TextView} a upraví jeho viditelnost podle toho, zda je text neprázdný.
     *
     * @param tv Komponenta {@link TextView} pro zobrazení poznámky
     * @param s  Text poznámky k nastavení (nebo {@code null}/prázdný pro skrytí)
     */
    private void setNote(TextView tv, String s) {
        if (s != null && !s.isEmpty()) {
            tv.setVisibility(View.VISIBLE);
            tv.setText(s);
        } else {
            tv.setVisibility(View.GONE);
        }
    }


    /**
     * Dynamicky uspořádá seznam aktivních prvků jističů do dvou sloupců bez prázdných mezer.
     *
     * @param container    Kontejnerové rozvržení {@link LinearLayout} pro vložení řádků jističů
     * @param visibleItems Seznam aktivních komponent {@link LabelPriceDetail} s nenulovou cenou jističe
     */
    private void setupBreakersGrid(LinearLayout container, List<LabelPriceDetail> visibleItems) {
        container.removeAllViews();
        android.content.Context context = container.getContext();
        float density = context.getResources().getDisplayMetrics().density;
        int margin2dp = (int) (2 * density);

        for (int i = 0; i < visibleItems.size(); i += 2) {
            LinearLayout rowLayout = new LinearLayout(context);
            LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            rowLp.topMargin = margin2dp;
            rowLp.bottomMargin = margin2dp;
            rowLayout.setLayoutParams(rowLp);
            rowLayout.setOrientation(LinearLayout.HORIZONTAL);

            LabelPriceDetail item1 = visibleItems.get(i);
            if (item1.getParent() instanceof ViewGroup parent) {
                parent.removeView(item1);
            }
            LinearLayout.LayoutParams itemLp1 = new LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f
            );
            item1.setLayoutParams(itemLp1);
            item1.setVisibility(View.VISIBLE);
            rowLayout.addView(item1);

            if (i + 1 < visibleItems.size()) {
                LabelPriceDetail item2 = visibleItems.get(i + 1);
                if (item2.getParent() instanceof ViewGroup parent) {
                    parent.removeView(item2);
                }
                LinearLayout.LayoutParams itemLp2 = new LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1f
                );
                item2.setLayoutParams(itemLp2);
                item2.setVisibility(View.VISIBLE);
                rowLayout.addView(item2);
            } else {
                View emptyView = new View(context);
                emptyView.setLayoutParams(new LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1f
                ));
                rowLayout.addView(emptyView);
            }

            container.addView(rowLayout);
        }
    }


    private record BreakerEntry(LabelPriceDetail view, double value) {

    }

}
