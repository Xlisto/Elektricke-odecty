package cz.xlisto.elektrodroid.modules.graphmonth;


import android.content.res.ColorStateList;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.TooltipCompat;
import androidx.fragment.app.Fragment;

import cz.xlisto.elektrodroid.R;
import cz.xlisto.elektrodroid.databaze.DataGraphMonth;
import cz.xlisto.elektrodroid.databaze.DataSettingsSource;
import cz.xlisto.elektrodroid.dialogs.SubscriptionPointDialogFragment;
import cz.xlisto.elektrodroid.shp.ShPGraphMonth;


/**
 * Xlisto 20.08.2023 21:53
 */
public class GraphMonthFragment extends Fragment {

    private final String ARG_IS_SHOW_PERIOD = "isShowPeriod";
    private final String ARG_IS_SHOW_VT = "isShowVT";
    private final String ARG_IS_SHOW_NT = "isShowNT";
    private final String ARG_TYPE_GRAPH = "isShowLineGraph";
    private final String ARG_COMPARE_MONTH = "compareMonth";

    private GraphMonthView graphMonthView;
    private View btnShowVT, btnShowNT, btnChangePeriod, btnTypeGraph, btnLeft, btnRight;
    private View containerVT, containerNT, containerPeriod, containerType, containerLeft, containerRight;
    private ImageView imgShowVT, imgShowNT, imgChangePeriod, imgTypeGraph, imgLeft, imgRight;
    private TextView tvLabelLeft, tvLabelRight;

    private boolean showVT = true;
    private boolean showNT = true;
    private int showPeriod = 1; // 0 - month, 1 - year, 2 - month compare
    private int compareMonth = 0;
    private boolean showTypeGraph = true;
    private ShPGraphMonth shPGraphMonth;
    private ConsuptionContainer consuptionContainer;


    public static GraphMonthFragment newInstance() {
        return new GraphMonthFragment();
    }


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //posluchač změny odběrného místa
        requireActivity().getSupportFragmentManager().setFragmentResultListener(SubscriptionPointDialogFragment.FLAG_UPDATE_SUBSCRIPTION_POINT,
                this,
                (requestKey, result) -> {
                    consuptionContainer = new DataGraphMonth(requireContext()).loadConsuptions();
                    graphMonthView.setConsuption(consuptionContainer);
                    compareMonth = checkEmptyMonths(compareMonth, false);
                    graphMonthView.setCompareMonth(compareMonth);
                });
    }


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_graph_month, container, false);
        graphMonthView = view.findViewById(R.id.graphMonthView);

        btnTypeGraph = view.findViewById(R.id.btnTypeGraph);
        btnChangePeriod = view.findViewById(R.id.btnPeriod);
        btnShowVT = view.findViewById(R.id.btnVT);
        btnShowNT = view.findViewById(R.id.btnNT);
        btnLeft = view.findViewById(R.id.btnLeft);
        btnRight = view.findViewById(R.id.btnRight);

        containerType = view.findViewById(R.id.containerBtnType);
        containerPeriod = view.findViewById(R.id.containerBtnPeriod);
        containerVT = view.findViewById(R.id.containerBtnVT);
        containerNT = view.findViewById(R.id.containerBtnNT);
        containerLeft = view.findViewById(R.id.containerBtnLeft);
        containerRight = view.findViewById(R.id.containerBtnRight);

        imgTypeGraph = view.findViewById(R.id.imgBtnType);
        imgChangePeriod = view.findViewById(R.id.imgBtnPeriod);
        imgShowVT = view.findViewById(R.id.imgBtnVT);
        imgShowNT = view.findViewById(R.id.imgBtnNT);
        imgLeft = view.findViewById(R.id.imgBtnLeft);
        imgRight = view.findViewById(R.id.imgBtnRight);

        tvLabelLeft = view.findViewById(R.id.tvLabelLeft);
        tvLabelRight = view.findViewById(R.id.tvLabelRight);

        shPGraphMonth = new ShPGraphMonth(requireContext());

        btnShowVT.setOnClickListener(v -> {
            showVT = !showVT;
            if (!showNT && !showVT) {
                showNT = true;
            }
            graphMonthView.setShowVT(showVT);
            graphMonthView.setShowNT(showNT);
            setImageIconNT();
            setImageIconVT();
            shPGraphMonth.set(ShPGraphMonth.ARG_IS_SHOW_VT, showVT);
            shPGraphMonth.set(ShPGraphMonth.ARG_IS_SHOW_NT, showNT);
        });

        btnShowNT.setOnClickListener(v -> {
            showNT = !showNT;
            if (!showNT && !showVT) {
                showVT = true;
            }
            graphMonthView.setShowVT(showVT);
            graphMonthView.setShowNT(showNT);
            setImageIconNT();
            setImageIconVT();
            shPGraphMonth.set(ShPGraphMonth.ARG_IS_SHOW_VT, showVT);
            shPGraphMonth.set(ShPGraphMonth.ARG_IS_SHOW_NT, showNT);
        });

        btnChangePeriod.setOnClickListener(v -> {
            showPeriod++;
            if (showPeriod > 2) showPeriod = 0;
            graphMonthView.changePeriod(showPeriod);
            setImageIconPeriod();
            setImageIconZoom();
            shPGraphMonth.set(ShPGraphMonth.ARG_IS_SHOW_PERIOD, showPeriod);
        });

        btnTypeGraph.setOnClickListener(v -> {
            showTypeGraph = !showTypeGraph;
            graphMonthView.setTypeShowGraph(showTypeGraph);
            setImageIconTypeGraph();
            shPGraphMonth.set(ShPGraphMonth.ARG_TYPE_GRAPH, showTypeGraph);
        });

        btnLeft.setOnClickListener(v -> {
            if (showPeriod == 2) {
                compareMonth--;
                if (compareMonth < 0) compareMonth = 11;

                compareMonth = checkEmptyMonths(compareMonth, true);

                graphMonthView.setCompareMonth(compareMonth);
                shPGraphMonth.set(ShPGraphMonth.ARG_COMPARE_MONTH, compareMonth);
            } else {
                graphMonthView.setCofDown();
            }
        });

        btnRight.setOnClickListener(v -> {
            if (showPeriod == 2) {
                compareMonth++;
                if (compareMonth > 11) compareMonth = 0;

                compareMonth = checkEmptyMonths(compareMonth, false);

                graphMonthView.setCompareMonth(compareMonth);
                shPGraphMonth.set(ARG_COMPARE_MONTH, compareMonth);
            } else {
                graphMonthView.setCofUp();
            }
        });

        if (savedInstanceState != null) {
            showPeriod = savedInstanceState.getInt(ShPGraphMonth.ARG_IS_SHOW_PERIOD);
            showVT = savedInstanceState.getBoolean(ShPGraphMonth.ARG_IS_SHOW_VT);
            showNT = savedInstanceState.getBoolean(ShPGraphMonth.ARG_IS_SHOW_NT);
            showTypeGraph = savedInstanceState.getBoolean(ShPGraphMonth.ARG_TYPE_GRAPH);
            compareMonth = savedInstanceState.getInt(ShPGraphMonth.ARG_COMPARE_MONTH);
            setGraphMonthView();
        }
        setupTooltips();
        return view;
    }


    @Override
    public void onResume() {
        super.onResume();
        requireActivity().invalidateOptionsMenu();

        DataGraphMonth dataGraphMonth = new DataGraphMonth(requireContext());
        consuptionContainer = dataGraphMonth.loadConsuptions();

        graphMonthView.setConsuption(consuptionContainer);

        DataSettingsSource dataSettingsSource = new DataSettingsSource(requireContext());
        dataSettingsSource.open();
        int[] graphColors = dataSettingsSource.loadColorVTNT();
        dataSettingsSource.close();
        graphMonthView.setColors(graphColors[0], graphColors[1]);

        setImageIconZoom();

        showPeriod = shPGraphMonth.get(ARG_IS_SHOW_PERIOD, 1);
        showTypeGraph = shPGraphMonth.get(ARG_TYPE_GRAPH, true);
        showVT = shPGraphMonth.get(ARG_IS_SHOW_VT, true);
        showNT = shPGraphMonth.get(ARG_IS_SHOW_NT, true);
        compareMonth = shPGraphMonth.get(ARG_COMPARE_MONTH, 1);

        compareMonth = checkEmptyMonths(compareMonth, true);

        graphMonthView.setCompareMonth(compareMonth);
        setGraphMonthView();
    }


    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(ARG_IS_SHOW_PERIOD, showPeriod);
        outState.putBoolean(ARG_IS_SHOW_VT, showVT);
        outState.putBoolean(ARG_IS_SHOW_NT, showNT);
        outState.putBoolean(ARG_TYPE_GRAPH, showTypeGraph);
        outState.putInt(ARG_COMPARE_MONTH, compareMonth);
    }


    /**
     * Nastaví parametry GraphView
     */
    private void setGraphMonthView() {
        graphMonthView.changePeriod(showPeriod);
        graphMonthView.setShowVT(showVT);
        graphMonthView.setShowNT(showNT);
        graphMonthView.setTypeShowGraph(showTypeGraph);
        graphMonthView.setCompareMonth(compareMonth);
        setImageIconPeriod();
        setImageIconVT();
        setImageIconNT();
        setImageIconTypeGraph();
        setImageIconZoom();
    }


    /**
     * Vrátí barvu z aktuálního tématu podle zadaného atributu
     */
    private int getThemeColor(int attrRes) {
        TypedValue typedValue = new TypedValue();
        requireContext().getTheme().resolveAttribute(attrRes, typedValue, true);
        return typedValue.data;
    }


    /**
     * Nastaví ikonu podle zobrazení období
     * 0 - měsíc; 1 - rok; 2 - meziměsíční porovnání
     */
    private void setImageIconPeriod() {
        switch (showPeriod) {
            case 0:
                imgChangePeriod.setImageResource(R.drawable.ic_graph_month);
                break;
            case 1:
                imgChangePeriod.setImageResource(R.drawable.ic_graph_year);
                break;
            case 2:
                imgChangePeriod.setImageResource(R.drawable.ic_graph_month_compare);
                break;
        }
        containerPeriod.setBackgroundResource(R.drawable.bg_graph_button_active);
        imgChangePeriod.setImageTintList(ColorStateList.valueOf(getThemeColor(com.google.android.material.R.attr.colorOnPrimaryContainer)));
    }


    /**
     * Nastaví ikonu podle zobrazení VT
     */
    private void setImageIconVT() {
        if (showVT) {
            imgShowVT.setImageResource(R.drawable.ic_graph_vt_on);
            containerVT.setBackgroundResource(R.drawable.bg_graph_button_active);
            imgShowVT.setImageTintList(ColorStateList.valueOf(getThemeColor(com.google.android.material.R.attr.colorOnPrimaryContainer)));
        } else {
            imgShowVT.setImageResource(R.drawable.ic_graph_vt_off);
            containerVT.setBackgroundResource(R.drawable.bg_graph_button_inactive);
            imgShowVT.setImageTintList(ColorStateList.valueOf(getThemeColor(com.google.android.material.R.attr.colorOnSurfaceVariant)));
        }
    }


    /**
     * Nastaví ikonu podle zobrazení NT
     */
    private void setImageIconNT() {
        if (showNT) {
            imgShowNT.setImageResource(R.drawable.ic_graph_nt_on);
            containerNT.setBackgroundResource(R.drawable.bg_graph_button_active);
            imgShowNT.setImageTintList(ColorStateList.valueOf(getThemeColor(com.google.android.material.R.attr.colorOnPrimaryContainer)));
        } else {
            imgShowNT.setImageResource(R.drawable.ic_graph_nt_off);
            containerNT.setBackgroundResource(R.drawable.bg_graph_button_inactive);
            imgShowNT.setImageTintList(ColorStateList.valueOf(getThemeColor(com.google.android.material.R.attr.colorOnSurfaceVariant)));
        }
    }


    /**
     * Nastaví ikonu podle typu grafu
     * sloupcový nebo čárový
     */
    private void setImageIconTypeGraph() {
        if (showTypeGraph) {
            imgTypeGraph.setImageResource(R.drawable.ic_graph_column);
        } else {
            imgTypeGraph.setImageResource(R.drawable.ic_graph_line);
        }
        containerType.setBackgroundResource(R.drawable.bg_graph_button_active);
        imgTypeGraph.setImageTintList(ColorStateList.valueOf(getThemeColor(com.google.android.material.R.attr.colorOnPrimaryContainer)));
    }


    /**
     * Nastaví ikonu podle druhu zobrazení grafu. Buď zoom nebo posun.
     */
    private void setImageIconZoom() {
        if (showPeriod == 2) {
            imgLeft.setImageResource(R.drawable.ic_graph_month_left);
            imgRight.setImageResource(R.drawable.ic_graph_month_right);
            if (tvLabelLeft != null) tvLabelLeft.setText(R.string.graph_btn_prev_month);
            if (tvLabelRight != null) tvLabelRight.setText(R.string.graph_btn_next_month);
        } else {
            imgLeft.setImageResource(R.drawable.ic_graph_zoom_minus);
            imgRight.setImageResource(R.drawable.ic_graph_zoom_plus);
            if (tvLabelLeft != null) tvLabelLeft.setText(R.string.graph_btn_zoom_out);
            if (tvLabelRight != null) tvLabelRight.setText(R.string.graph_btn_zoom_in);
        }
        containerLeft.setBackgroundResource(R.drawable.bg_graph_button_active);
        imgLeft.setImageTintList(ColorStateList.valueOf(getThemeColor(com.google.android.material.R.attr.colorOnPrimaryContainer)));
        containerRight.setBackgroundResource(R.drawable.bg_graph_button_active);
        imgRight.setImageTintList(ColorStateList.valueOf(getThemeColor(com.google.android.material.R.attr.colorOnPrimaryContainer)));
        updateZoomTooltips();
    }


    /**
     * Nastaví nápovědu (Tooltip / Toast fallback pro Samsung One UI) pro zadané tlačítko.
     */
    private void setButtonTooltip(View view, String tooltipText) {
        if (view == null || tooltipText == null) return;
        TooltipCompat.setTooltipText(view, tooltipText);
        view.setContentDescription(tooltipText);
        view.setOnLongClickListener(v -> {
            Toast.makeText(v.getContext(), tooltipText, Toast.LENGTH_SHORT).show();
            return true;
        });
    }


    /**
     * Nastaví nápovědy (Tooltips) pro navigační tlačítka grafu.
     */
    private void setupTooltips() {
        setButtonTooltip(btnTypeGraph, getString(R.string.description_btn_type));
        setButtonTooltip(btnChangePeriod, getString(R.string.description_btn_period));
        setButtonTooltip(btnShowVT, getString(R.string.description_btn_vt));
        setButtonTooltip(btnShowNT, getString(R.string.description_btn_nt));
        updateZoomTooltips();
    }


    /**
     * Aktualizuje text nápovědy u tlačítek lupy / posunu podle zvoleného období.
     */
    private void updateZoomTooltips() {
        if (showPeriod == 2) {
            setButtonTooltip(btnLeft, getString(R.string.description_btn_previous_month));
            setButtonTooltip(btnRight, getString(R.string.description_btn_next_month));
        } else {
            setButtonTooltip(btnLeft, getString(R.string.description_btn_zoom_in));
            setButtonTooltip(btnRight, getString(R.string.description_btn_zoom_out));
        }
    }


    /**
     * Zkontroluje, zda jsou všechny měsíce prázdné
     *
     * @param compareMonth měsíc, který se má porovnávat
     * @param back         zda se má porovnávat zpět nebo dopředu
     * @return následující měsíc, který není prázdný
     */
    private int checkEmptyMonths(int compareMonth, boolean back) {
        if (consuptionContainer == null || consuptionContainer.monthsConsuptionsArray().isEmpty())
            return 0;
        boolean allEmpty = true;
        for (int i = 0; i < consuptionContainer.monthsConsuptionsArray().size(); i++) {
            if (!consuptionContainer.monthsConsuptionsArray().get(i).isEmpty()) {
                allEmpty = false;
                break;
            }
        }

        if (!allEmpty) {
            int attempts = 0;
            int size = consuptionContainer.monthsConsuptionsArray().size();
            while (attempts < size && (compareMonth < 0 || compareMonth >= size || consuptionContainer.monthsConsuptionsArray().get(compareMonth).isEmpty())) {
                if (back) {
                    compareMonth--;
                    if (compareMonth < 0) compareMonth = size - 1;
                } else {
                    compareMonth++;
                    if (compareMonth >= size) compareMonth = 0;
                }
                attempts++;
            }
        }
        if (compareMonth < 0 || compareMonth >= consuptionContainer.monthsConsuptionsArray().size()) {
            return 0;
        }
        return compareMonth;
    }

}
