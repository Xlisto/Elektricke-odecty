package cz.xlisto.elektrodroid.modules.graphcolor;


import static cz.xlisto.elektrodroid.R.menu.menu_graph_color;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.util.ArrayList;

import cz.xlisto.elektrodroid.R;
import cz.xlisto.elektrodroid.databaze.DataGraphColor;
import cz.xlisto.elektrodroid.dialogs.SubscriptionPointDialogFragment;


/**
 * Fragment pro nastavení barev grafu
 * Xlisto 17.10.2023 21:13
 */
public class GraphColorFragment extends Fragment {
    private static final String TAG = "GraphColorFragment";
    private GraphColorView graphColorView;
    private String vtColor, ntColor;
    private ArrayList<String> colorsHistory = new ArrayList<>();


    public static GraphColorFragment newInstance() {
        return new GraphColorFragment();
    }


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        //posluchač na změnu odběrného místa
        requireActivity().getSupportFragmentManager().setFragmentResultListener(
                SubscriptionPointDialogFragment.FLAG_UPDATE_SUBSCRIPTION_POINT,
                this,
                (requestKey, result) -> {
                    loadHistoryColors();
                    loadColors();
                }
        );
    }


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_graph_color, container, false);
        Button btnSave = view.findViewById(R.id.btnSave);
        graphColorView = view.findViewById(R.id.graphColorView);

        btnSave.setOnClickListener(v -> {
            //uložení vybrané barvy do databáze pro potřeby grafu
            DataGraphColor dataGraphColor = new DataGraphColor(getContext());
            dataGraphColor.open();
            dataGraphColor.saveColors(graphColorView.getColors());
            dataGraphColor.close();

            //přidání barev do historie
            addAndSaveColorHistory(graphColorView.getColorVTHTML(), graphColorView.getColorNTHTML());

        });

        return view;
    }


    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);


        requireActivity().getSupportFragmentManager().setFragmentResultListener(
                GraphColorDialogFragment.RESULT_GRAPH_COLOR_DIALOG_FRAGMENT,
                this,
                (requestKey, result) -> {
                    vtColor = result.getString(GraphColorDialogFragment.ARG_VT_COLOR);
                    ntColor = result.getString(GraphColorDialogFragment.ARG_NT_COLOR);
                    graphColorView.setColorsHTML(vtColor, ntColor);
                }
        );
    }


    @Override
    public void onResume() {
        super.onResume();
        loadHistoryColors();
        loadColors();
    }


    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(menu_graph_color, menu);
    }


    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.menu_color) {
            GraphColorDialogFragment graphColorDialogFragment = GraphColorDialogFragment.newInstance(graphColorView.getColorVTHTML(), graphColorView.getColorNTHTML());
            graphColorDialogFragment.show(getParentFragmentManager(), GraphColorDialogFragment.TAG);
            return true;
        }
        return false;
    }


    /**
     * Přidá barvy do historie.
     *
     * @param vtColor barva VT
     * @param ntColor barva NT
     */
    private void addAndSaveColorHistory(String vtColor, String ntColor) {
        colorsHistory.add(vtColor + ";" + ntColor);
        colorsHistory.remove(0);
        //uloží historii barev
        DataGraphColor dataGraphColor = new DataGraphColor(getContext());
        dataGraphColor.open();
        dataGraphColor.saveColorsHistory(colorsHistory);
        dataGraphColor.close();
    }


    /**
     * Načte historii barev.
     */
    private void loadHistoryColors() {
        //načtení historie barev
        DataGraphColor dataGraphColor = new DataGraphColor(requireContext());
        dataGraphColor.open();
        colorsHistory = dataGraphColor.loadColorsHistory();
        dataGraphColor.close();
    }


    /**
     * Načte barvy z databáze.
     */
    private void loadColors() {
        DataGraphColor dataGraphColor = new DataGraphColor(requireContext());
        dataGraphColor.open();
        int[] colors = dataGraphColor.loadColors();
        dataGraphColor.close();
        //nastavení barev do GraphColorView
        graphColorView.setColors(colors);
        //nastavení aktuálních barev do proměnných
        vtColor = graphColorView.getColorVTHTML();
        ntColor = graphColorView.getColorNTHTML();
    }
}
