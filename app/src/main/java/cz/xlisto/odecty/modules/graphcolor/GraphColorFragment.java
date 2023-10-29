package cz.xlisto.odecty.modules.graphcolor;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import java.util.ArrayList;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import cz.xlisto.odecty.R;
import cz.xlisto.odecty.databaze.DataGraphColor;

import static cz.xlisto.odecty.R.menu.menu_graph_color;

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
        //načtení historie barev
        DataGraphColor dataGraphColor = new DataGraphColor(getContext());
        dataGraphColor.open();
        colorsHistory = dataGraphColor.loadColorsHistory();
        dataGraphColor.close();
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
        DataGraphColor dataGraphColor = new DataGraphColor(getContext());
        dataGraphColor.open();
        int[] colors = dataGraphColor.loadColors();
        dataGraphColor.close();
        //nastavení barev do GraphColorView
        graphColorView.setColors(colors);
        //nastavení aktuálních barev do proměnných
        vtColor = graphColorView.getColorVTHTML();
        ntColor = graphColorView.getColorNTHTML();

        requireActivity().getSupportFragmentManager().setFragmentResultListener(
                GraphColorDialogFragment.RESULT_GRAPH_COLOR_DIALOG_FRAGMENT,
                this,
                (requestKey, result) -> {
                    Log.w(TAG, "onViewCreated: " + result.getString(GraphColorDialogFragment.ARG_VT_COLOR));
                    vtColor = result.getString(GraphColorDialogFragment.ARG_VT_COLOR);
                    ntColor = result.getString(GraphColorDialogFragment.ARG_NT_COLOR);
                    graphColorView.setColorsHTML(vtColor, ntColor);
                }
        );
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
        Log.w(TAG, "addAndSaveColorHistory: " + vtColor + ";" + ntColor);
        colorsHistory.add(vtColor + ";" + ntColor);
        colorsHistory.remove(0);
        //uloží historii barev
        DataGraphColor dataGraphColor = new DataGraphColor(getContext());
        dataGraphColor.open();
        dataGraphColor.saveColorsHistory(colorsHistory);
        dataGraphColor.close();
    }
}
