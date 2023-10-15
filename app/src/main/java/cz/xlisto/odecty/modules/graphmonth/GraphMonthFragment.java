package cz.xlisto.odecty.modules.graphmonth;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import cz.xlisto.odecty.R;
import cz.xlisto.odecty.databaze.DataGraphMonth;
import cz.xlisto.odecty.databaze.DataSettingsSource;

/**
 * Xlisto 20.08.2023 21:53
 */
public class GraphMonthFragment extends Fragment {
    private static final String TAG = "GraphMonthFragment";
    private final String ARG_IS_SHOW_PERIOD = "isShowPeriod";
    private final String ARG_IS_SHOW_VT = "isShowVT";
    private final String ARG_IS_SHOW_NT = "isShowNT";
    private final String ARG_TYPE_GRAPH = "isShowLineGraph";
    private final String ARG_COMPARE_MONTH = "compareMonth";
    private GraphMonthView graphMonthView;
    private ImageButton btnShowVT, btnShowNT, btnChangePeriod, btnTypeGraph, btnLeft, btnRight;
    private boolean showVT = true;
    private boolean showNT = true;
    private int showPeriod = 1; // 0 - month, 1 - year, 2 - moth compare
    private int compareMonth = 0;
    private boolean showTypeGraph = true;


    public static GraphMonthFragment newInstance() {
        return new GraphMonthFragment();
    }


    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_graph_month, container, false);
        graphMonthView = view.findViewById(R.id.graphMonthView);
        btnShowVT = view.findViewById(R.id.imgBtnVT);
        btnShowNT = view.findViewById(R.id.imgBtnNT);
        btnChangePeriod = view.findViewById(R.id.imgBtnPeriod);
        btnTypeGraph = view.findViewById(R.id.imgBtnType);
        btnLeft = view.findViewById(R.id.imgBtnLeft);
        btnRight = view.findViewById(R.id.imgBtnRight);


        btnShowVT.setOnClickListener(v -> {
            showVT = !showVT;
            if (!showNT && !showVT) {
                showNT = true;
            }
            graphMonthView.setShowVT(showVT);
            graphMonthView.setShowNT(showNT);
            setImageIconNT();
            setImageIconVT();
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
        });

        btnChangePeriod.setOnClickListener(v -> {
            showPeriod++;
            if (showPeriod > 2) showPeriod = 0;
            graphMonthView.changePeriod(showPeriod);
            setImageIconPeriod();
            setImageIconZoom();
        });

        btnTypeGraph.setOnClickListener(v -> {
            showTypeGraph = !showTypeGraph;
            graphMonthView.setTypeShowGraph(showTypeGraph);
            setImageIconTypeGraph();
        });

        btnLeft.setOnClickListener(v -> {
            if (showPeriod == 2) {
                compareMonth--;
                if (compareMonth < 0) compareMonth = 11;
                graphMonthView.setCompareMonth(compareMonth);
            } else {
                graphMonthView.setCofDown();
            }
        });

        btnRight.setOnClickListener(v -> {
            if (showPeriod == 2) {
                compareMonth++;
                if (compareMonth > 11) compareMonth = 0;
                graphMonthView.setCompareMonth(compareMonth);
            } else {
                graphMonthView.setCofUp();
            }
        });

        if (savedInstanceState != null) {
            showPeriod = savedInstanceState.getInt(ARG_IS_SHOW_PERIOD);
            showVT = savedInstanceState.getBoolean(ARG_IS_SHOW_VT);
            showNT = savedInstanceState.getBoolean(ARG_IS_SHOW_NT);
            showTypeGraph = savedInstanceState.getBoolean(ARG_TYPE_GRAPH);
            compareMonth = savedInstanceState.getInt(ARG_COMPARE_MONTH);
            graphMonthView.changePeriod(showPeriod);
            graphMonthView.setShowVT(showVT);
            graphMonthView.setShowNT(showNT);
            graphMonthView.setTypeShowGraph(showTypeGraph);
            setImageIconPeriod();
            setImageIconVT();
            setImageIconNT();
            setImageIconTypeGraph();
            setImageIconZoom();
        }
        return view;
    }


    @Override
    public void onResume() {
        super.onResume();
        DataGraphMonth dataGraphMonth = new DataGraphMonth(getContext());
        ConsuptionContainer consuptionContainer = dataGraphMonth.loadConsuptions();
        graphMonthView.setConsuption(consuptionContainer);

        DataSettingsSource dataSettingsSource = new DataSettingsSource(getContext());
        dataSettingsSource.open();
        int[] graphColors = dataSettingsSource.loadColorVTNT();
        dataSettingsSource.close();
        graphMonthView.setColors(graphColors[0], graphColors[1]);

        setImageIconZoom();
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
     * Nastaví ikonu podle zobrazení období
     * 0 - měsíc; 1 - rok; 2 - meziměsíční porovnání
     */
    private void setImageIconPeriod() {
        switch (showPeriod) {
            case 0:
                btnChangePeriod.setImageResource(R.mipmap.ic_graph_month);
                break;
            case 1:
                btnChangePeriod.setImageResource(R.mipmap.ic_graph_year);
                break;
            case 2:
                btnChangePeriod.setImageResource(R.mipmap.ic_graph_month_compare);
                break;
        }
    }


    /**
     * Nastaví ikonu podle zobrazení VT
     */
    private void setImageIconVT() {
        if (showVT) {
            btnShowVT.setImageResource(R.mipmap.ic_graph_vt_off);
        } else {
            btnShowVT.setImageResource(R.mipmap.ic_graph_vt_on);
        }
    }


    /**
     * Nastaví ikonu podle zobrazení NT
     */
    private void setImageIconNT() {
        if (showNT) {
            btnShowNT.setImageResource(R.mipmap.ic_graph_nt_off);
        } else {
            btnShowNT.setImageResource(R.mipmap.ic_graph_nt_on);
        }
    }


    /**
     * Nastaví ikonu podle typu grafu
     * sloupcový nebo čárový
     */
    private void setImageIconTypeGraph() {
        if (showTypeGraph) {
            btnTypeGraph.setImageResource(R.mipmap.ic_graph_column);
        } else {
            btnTypeGraph.setImageResource(R.mipmap.ic_graph_line);
        }
    }


    private void setImageIconZoom() {
        if (showPeriod == 2) {
            btnLeft.setImageResource(R.mipmap.ic_graph_month_left_gray);
            btnRight.setImageResource(R.mipmap.ic_graph_month_right_gray);
        }
        else {
            btnLeft.setImageResource(R.mipmap.ic_graph_zoom_minus);
            btnRight.setImageResource(R.mipmap.ic_graph_zoom_plus);
        }
    }
}
