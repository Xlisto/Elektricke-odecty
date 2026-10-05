package cz.xlisto.elektrodroid.modules.subscriptionpoint;

import android.content.res.Configuration;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.util.ArrayList;

import cz.xlisto.elektrodroid.R;
import cz.xlisto.elektrodroid.databaze.DataSubscriptionPointSource;
import cz.xlisto.elektrodroid.dialogs.OwnAlertDialog;
import cz.xlisto.elektrodroid.models.SubscriptionPointModel;

/**
 * Celoobrazovkový fragment pro zobrazení kompletní historie roční spotřeby a cen za všechna dostupná období.
 */
public class AnnualGraphFullscreenFragment extends Fragment {

    private static final String ARG_SUBSCRIPTION_POINT_ID = "argSubscriptionPointId";
    private static final String STATE_PORTRAIT_PROMPT_SHOWN = "portraitPromptShown";

    private long subscriptionPointId = 0L;
    private GraphYAxisView yAxisLeft;
    private GraphYAxisView yAxisRight;
    private GraphLegendView graphLegend;
    private GraphAnnualOverviewView graphAnnualOverview;
    private HorizontalScrollView horizontalScrollView;
    private TextView tvTitle;
    private boolean portraitPromptShown = false;

    public AnnualGraphFullscreenFragment() {
    }

    public static AnnualGraphFullscreenFragment newInstance(long subscriptionPointId) {
        AnnualGraphFullscreenFragment fragment = new AnnualGraphFullscreenFragment();
        Bundle args = new Bundle();
        args.putLong(ARG_SUBSCRIPTION_POINT_ID, subscriptionPointId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            portraitPromptShown = savedInstanceState.getBoolean(STATE_PORTRAIT_PROMPT_SHOWN, false);
        }
        if (getArguments() != null) {
            subscriptionPointId = getArguments().getLong(ARG_SUBSCRIPTION_POINT_ID, 0L);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_annual_graph_fullscreen, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tvTitle = view.findViewById(R.id.tvTitle);
        yAxisLeft = view.findViewById(R.id.yAxisLeft);
        yAxisRight = view.findViewById(R.id.yAxisRight);
        graphLegend = view.findViewById(R.id.graphLegend);
        horizontalScrollView = view.findViewById(R.id.horizontalScrollView);
        graphAnnualOverview = view.findViewById(R.id.graphAnnualOverview);

        if (yAxisLeft != null) {
            yAxisLeft.setAxisMode(GraphYAxisView.MODE_LEFT_CONSUMPTION);
            yAxisLeft.setDrawCardBackground(false);
        }
        if (yAxisRight != null) {
            yAxisRight.setAxisMode(GraphYAxisView.MODE_RIGHT_PRICE);
            yAxisRight.setDrawCardBackground(false);
        }
        if (graphAnnualOverview != null) {
            graphAnnualOverview.setShowYAxes(false);
            graphAnnualOverview.setShowLegend(false);
            graphAnnualOverview.setDrawCardBackground(false);
            graphAnnualOverview.setCustomMinSlotWidthDp(85f);
        }

        loadAndDisplayData();
    }

    @Override
    public void onResume() {
        super.onResume();
        checkPortraitOrientationPrompt();
    }


    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(STATE_PORTRAIT_PROMPT_SHOWN, portraitPromptShown);
    }


    /**
     * Zobrazí dialogové okno s výzvou k otočení zařízení do režimu na šířku (Landscape)
     * výhradně v případě, kdy se zařízení nachází v režimu na výšku (Portrait).
     */
    private void checkPortraitOrientationPrompt() {
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            return; // V režimu na šířku (Landscape) upozornění nezobrazujeme
        }

        if (!portraitPromptShown && getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            portraitPromptShown = true;
            OwnAlertDialog.showDialog(requireActivity(), getString(R.string.fullscreen_graph_title), getString(R.string.rotate_to_landscape_message));
        }
    }

    private void loadAndDisplayData() {
        if (subscriptionPointId <= 0 || graphAnnualOverview == null) {
            return;
        }

        DataSubscriptionPointSource source = new DataSubscriptionPointSource(requireContext());
        source.open();
        SubscriptionPointModel subscriptionPoint = source.loadSubscriptionPoint(subscriptionPointId);
        source.close();

        if (subscriptionPoint != null) {
            if (tvTitle != null) {
                String titleText = getString(R.string.fullscreen_graph_title) + " - " + subscriptionPoint.getName();
                tvTitle.setText(titleText);
            }

            AnnualOverviewDataBuilder builder = new AnnualOverviewDataBuilder(requireContext());
            ArrayList<AnnualYearData> allAnnualData = builder.buildAllAnnualData(subscriptionPoint);

            if (yAxisLeft != null) {
                yAxisLeft.updateFromData(allAnnualData);
            }
            if (yAxisRight != null) {
                yAxisRight.updateFromData(allAnnualData);
            }
            if (graphAnnualOverview != null) {
                graphAnnualOverview.setData(allAnnualData);
            }
            if (graphLegend != null && graphAnnualOverview != null) {
                graphLegend.setLegendData(graphAnnualOverview.isMWh(), graphAnnualOverview.isDualTariff());
            }

            if (horizontalScrollView != null) {
                horizontalScrollView.post(() -> horizontalScrollView.fullScroll(View.FOCUS_RIGHT));
            }
        }
    }
}
