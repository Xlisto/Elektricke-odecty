package cz.xlisto.elektrodroid.modules.graphcolor;


import static cz.xlisto.elektrodroid.R.menu.menu_graph_color;
import static cz.xlisto.elektrodroid.utils.ColorHelper.colorToHtml;
import static cz.xlisto.elektrodroid.utils.ColorHelper.htmlToColor;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.MenuHost;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;

import cz.xlisto.elektrodroid.R;
import cz.xlisto.elektrodroid.databaze.DataGraphColor;
import cz.xlisto.elektrodroid.dialogs.GraphColorDialogFragment;
import cz.xlisto.elektrodroid.dialogs.SubscriptionPointDialogFragment;


/**
 * Fragment pro nastavení barev grafu VT/NT s moderním Material 3 rozhraním.
 * Zajišťuje načtení barev, uchování neuložených změn při rotaci obrazovky,
 * práci s historií, rychlými vzorníky, vizuálními výřezy témat v mřížce 5x2 a uložení vybraných hodnot.
 * Xlisto 17.10.2023 21:13
 */
public class GraphColorFragment extends Fragment {

    private static final String KEY_VT_COLOR = "key_vt_color";
    private static final String KEY_NT_COLOR = "key_nt_color";

    private GraphColorView graphColorView;
    private Chip chipVTColor, chipNTColor;
    private Chip chipFineTuningVT, chipFineTuningNT;
    private LinearLayout llSwatchesVT, llSwatchesNT;
    private View previewBarVT1, previewBarVT2, previewLegendVT;
    private View previewBarNT1, previewBarNT2, previewLegendNT;
    private LinearLayout llPresetsLight, llPresetsDark;
    private final ArrayList<MaterialCardView> allPresetCards = new ArrayList<>();
    private String vtColor, ntColor;
    private ArrayList<String> colorsHistory = new ArrayList<>();

    // Rychlé vzorníky barev pro VT a NT
    private static final String[] SWATCHES_VT = new String[]{
            "#E53935", "#FB8B24", "#FFB300", "#D81B60", "#8E24AA",
            "#FF6E40", "#B71C1C", "#F57C00", "#FFD600", "#4A148C"
    };
    private static final String[] SWATCHES_NT = new String[]{
            "#06D6A0", "#1B9AAA", "#00A6FB", "#00BCD4", "#4CAF50",
            "#80CBC4", "#00E676", "#00E5FF", "#1565C0", "#2E7D32"
    };

    // 10 témat pro světlé prostředí (5x2 grid)
    private static final String[][] PRESETS_LIGHT = new String[][]{
            {"#2E7D32;#512DA8"},
            {"#1565C0;#C62828"},
            {"#00695C;#EF6C00"},
            {"#00838F;#283593"},
            {"#880E4F;#0D47A1"},
            {"#212121;#B71C1C"},
            {"#FF8F00;#1B5E20"},
            {"#3F51B5;#E64A19"},
            {"#4A148C;#F57F17"},
            {"#37474F;#AD1457"}
    };

    // 10 témat pro tmavé prostředí (5x2 grid)
    private static final String[][] PRESETS_DARK = new String[][]{
            {"#00E676;#B388FF"},
            {"#00E5FF;#FF6E40"},
            {"#FFEA00;#00B0FF"},
            {"#40C4FF;#FF4081"},
            {"#FF9100;#AEEA00"},
            {"#FF1744;#1DE9B6"},
            {"#FFD700;#E040FB"},
            {"#76FF03;#FF3D00"},
            {"#D500F9;#00E5FF"},
            {"#64FFDA;#FF1744"}
    };


    public static GraphColorFragment newInstance() {
        return new GraphColorFragment();
    }


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Obnovení stavu neuložených barev při rotaci obrazovky
        if (savedInstanceState != null) {
            vtColor = savedInstanceState.getString(KEY_VT_COLOR);
            ntColor = savedInstanceState.getString(KEY_NT_COLOR);
        }

        // posluchač na změnu odběrného místa
        requireActivity().getSupportFragmentManager().setFragmentResultListener(
                SubscriptionPointDialogFragment.FLAG_UPDATE_SUBSCRIPTION_POINT,
                this,
                (requestKey, result) -> {
                    loadHistoryColors();
                    loadColorsFromDb();
                }
        );
    }


    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (graphColorView != null) {
            outState.putString(KEY_VT_COLOR, graphColorView.getColorVTHTML());
            outState.putString(KEY_NT_COLOR, graphColorView.getColorNTHTML());
        } else if (vtColor != null && ntColor != null) {
            outState.putString(KEY_VT_COLOR, vtColor);
            outState.putString(KEY_NT_COLOR, ntColor);
        }
    }


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_graph_color, container, false);

        graphColorView = view.findViewById(R.id.graphColorView);
        chipVTColor = view.findViewById(R.id.chipVTColor);
        chipNTColor = view.findViewById(R.id.chipNTColor);
        chipFineTuningVT = view.findViewById(R.id.chipFineTuningVT);
        chipFineTuningNT = view.findViewById(R.id.chipFineTuningNT);

        llSwatchesVT = view.findViewById(R.id.llSwatchesVT);
        llSwatchesNT = view.findViewById(R.id.llSwatchesNT);

        previewBarVT1 = view.findViewById(R.id.previewBarVT1);
        previewBarVT2 = view.findViewById(R.id.previewBarVT2);
        previewLegendVT = view.findViewById(R.id.previewLegendVT);

        previewBarNT1 = view.findViewById(R.id.previewBarNT1);
        previewBarNT2 = view.findViewById(R.id.previewBarNT2);
        previewLegendNT = view.findViewById(R.id.previewLegendNT);

        llPresetsLight = view.findViewById(R.id.llPresetsLight);
        llPresetsDark = view.findViewById(R.id.llPresetsDark);

        MaterialButton btnSave = view.findViewById(R.id.btnSave);

        // Sledování změn v GraphColorView pro živou aktualizaci
        graphColorView.setOnColorChangeListener(this::updateUIColors);

        // Kliknutí na chipy otevře dialog pro přímé zadání HEX
        chipVTColor.setOnClickListener(v -> openColorDialog());
        chipNTColor.setOnClickListener(v -> openColorDialog());
        chipFineTuningVT.setOnClickListener(v -> openColorDialog());
        chipFineTuningNT.setOnClickListener(v -> openColorDialog());

        // Tlačítko uložení
        btnSave.setOnClickListener(v -> {
            DataGraphColor dataGraphColor = new DataGraphColor(requireContext());
            dataGraphColor.open();
            dataGraphColor.saveColors(graphColorView.getColors());
            dataGraphColor.close();

            addAndSaveColorHistory(graphColorView.getColorVTHTML(), graphColorView.getColorNTHTML());

            Snackbar.make(view, R.string.graph_color_saved, Snackbar.LENGTH_SHORT).show();
        });

        setupSwatches();
        setupPresets();

        return view;
    }


    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        MenuHost menuHost = requireActivity();
        menuHost.addMenuProvider(new MenuProvider() {
            @Override
            public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
                menuInflater.inflate(menu_graph_color, menu);
            }


            @Override
            public boolean onMenuItemSelected(@NonNull MenuItem item) {
                if (item.getItemId() == R.id.menu_color) {
                    openColorDialog();
                    return true;
                }
                return false;
            }
        }, getViewLifecycleOwner(), Lifecycle.State.RESUMED);

        requireActivity().getSupportFragmentManager().setFragmentResultListener(
                GraphColorDialogFragment.RESULT_GRAPH_COLOR_DIALOG_FRAGMENT,
                this,
                (requestKey, result) -> {
                    vtColor = result.getString(GraphColorDialogFragment.ARG_VT_COLOR);
                    ntColor = result.getString(GraphColorDialogFragment.ARG_NT_COLOR);
                    if (vtColor != null && ntColor != null) {
                        graphColorView.setColorsHTML(vtColor, ntColor);
                    }
                }
        );
    }


    @Override
    public void onResume() {
        super.onResume();
        loadHistoryColors();
        if (vtColor != null && ntColor != null) {
            // Aplikujeme obnovené/neuložené barvy z paměti po rotaci
            graphColorView.setColorsHTML(vtColor, ntColor);
            updateUIColors(graphColorView.getColorVT(), graphColorView.getColorNT());
        } else {
            // První načtení z databáze
            loadColorsFromDb();
        }
    }


    private void openColorDialog() {
        GraphColorDialogFragment dialog = GraphColorDialogFragment.newInstance(
                graphColorView.getColorVTHTML(),
                graphColorView.getColorNTHTML()
        );
        dialog.show(getParentFragmentManager(), GraphColorDialogFragment.TAG);
    }


    /**
     * Vytvoří kruhové vzorníky barev (Swatches) pro rychlý výběr VT a NT barev.
     */
    private void setupSwatches() {
        llSwatchesVT.removeAllViews();
        llSwatchesNT.removeAllViews();

        int density = (int) getResources().getDisplayMetrics().density;
        int size = 32 * density;
        int margin = 4 * density;

        for (String hex : SWATCHES_VT) {
            View swatch = createSwatchView(hex, size, margin);
            swatch.setOnClickListener(v -> graphColorView.setColorsHTML(hex, graphColorView.getColorNTHTML()));
            llSwatchesVT.addView(swatch);
        }

        for (String hex : SWATCHES_NT) {
            View swatch = createSwatchView(hex, size, margin);
            swatch.setOnClickListener(v -> graphColorView.setColorsHTML(graphColorView.getColorVTHTML(), hex));
            llSwatchesNT.addView(swatch);
        }
    }


    private View createSwatchView(String hexColor, int size, int margin) {
        View view = new View(requireContext());
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(size, size);
        params.setMargins(margin, margin, margin, margin);
        view.setLayoutParams(params);

        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.OVAL);
        drawable.setColor(htmlToColor(hexColor));
        drawable.setStroke((int) (1.5 * getResources().getDisplayMetrics().density), Color.WHITE);
        view.setBackground(drawable);
        view.setElevation(2 * getResources().getDisplayMetrics().density);

        return view;
    }


    /**
     * Vytvoří výřezy barevných dvojic témat v mřížce 5x2 pro světlé i tmavé prostředí.
     */
    private void setupPresets() {
        llPresetsLight.removeAllViews();
        llPresetsDark.removeAllViews();
        allPresetCards.clear();

        populatePresetGrid(llPresetsLight, PRESETS_LIGHT);
        populatePresetGrid(llPresetsDark, PRESETS_DARK);
    }


    private void populatePresetGrid(LinearLayout parentContainer, String[][] presets) {
        parentContainer.removeAllViews();
        int density = (int) getResources().getDisplayMetrics().density;
        int primaryColor = Color.GRAY;
        TypedValue typedValue = new TypedValue();
        if (requireContext().getTheme().resolveAttribute(android.R.attr.colorPrimary, typedValue, true)) {
            primaryColor = typedValue.data;
        }

        final int activeColor = primaryColor;

        int totalItems = presets.length;
        int numCols = 5;
        int numRows = (int) Math.ceil((double) totalItems / numCols);

        for (int row = 0; row < numRows; row++) {
            LinearLayout rowLayout = new LinearLayout(requireContext());
            rowLayout.setOrientation(LinearLayout.HORIZONTAL);
            rowLayout.setWeightSum(numCols);
            LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            if (row > 0) {
                rowParams.topMargin = 4 * density;
            }
            rowLayout.setLayoutParams(rowParams);

            for (int col = 0; col < numCols; col++) {
                int index = row * numCols + col;
                if (index >= totalItems) {
                    View emptyView = new View(requireContext());
                    LinearLayout.LayoutParams emptyParams = new LinearLayout.LayoutParams(
                            0,
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            1.0f
                    );
                    emptyView.setLayoutParams(emptyParams);
                    rowLayout.addView(emptyView);
                    continue;
                }

                String[] colors = presets[index][0].split(";");
                String vtHex = colors[0];
                String ntHex = colors[1];

                MaterialCardView card = new MaterialCardView(requireContext(), null, com.google.android.material.R.attr.materialCardViewOutlinedStyle);
                LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1.0f
                );
                cardParams.setMargins(2 * density, 2 * density, 2 * density, 2 * density);
                card.setLayoutParams(cardParams);
                card.setRadius(14 * density);
                card.setCardElevation(0);
                card.setClickable(true);
                card.setFocusable(true);

                LinearLayout innerLayout = new LinearLayout(requireContext());
                innerLayout.setOrientation(LinearLayout.HORIZONTAL);
                innerLayout.setPadding(4 * density, 6 * density, 4 * density, 6 * density);
                innerLayout.setGravity(Gravity.CENTER);

                // Kolečko VT barvy
                View vtCircle = new View(requireContext());
                LinearLayout.LayoutParams circleParams1 = new LinearLayout.LayoutParams(18 * density, 18 * density);
                vtCircle.setLayoutParams(circleParams1);
                GradientDrawable vtDrawable = new GradientDrawable();
                vtDrawable.setShape(GradientDrawable.OVAL);
                vtDrawable.setColor(htmlToColor(vtHex));
                vtDrawable.setStroke((int) (1.2f * density), Color.WHITE);
                vtCircle.setBackground(vtDrawable);
                vtCircle.setElevation(1.5f * density);

                // Kolečko NT barvy
                View ntCircle = new View(requireContext());
                LinearLayout.LayoutParams circleParams2 = new LinearLayout.LayoutParams(18 * density, 18 * density);
                circleParams2.setMarginStart(3 * density);
                ntCircle.setLayoutParams(circleParams2);
                GradientDrawable ntDrawable = new GradientDrawable();
                ntDrawable.setShape(GradientDrawable.OVAL);
                ntDrawable.setColor(htmlToColor(ntHex));
                ntDrawable.setStroke((int) (1.2f * density), Color.WHITE);
                ntCircle.setBackground(ntDrawable);
                ntCircle.setElevation(1.5f * density);

                innerLayout.addView(vtCircle);
                innerLayout.addView(ntCircle);
                card.addView(innerLayout);

                allPresetCards.add(card);

                card.setOnClickListener(v -> {
                    for (MaterialCardView c : allPresetCards) {
                        c.setStrokeWidth(density);
                        c.setStrokeColor(Color.TRANSPARENT);
                    }
                    card.setStrokeColor(activeColor);
                    card.setStrokeWidth((int) (2.5f * density));

                    graphColorView.setColorsHTML(vtHex, ntHex);
                });

                rowLayout.addView(card);
            }

            parentContainer.addView(rowLayout);
        }
    }


    /**
     * Aktualizuje veškeré UI prvky podle vybrané barvy VT a NT.
     */
    private void updateUIColors(int colorVT, int colorNT) {
        vtColor = colorToHtml(colorVT);
        ntColor = colorToHtml(colorNT);

        // Chip VT
        chipVTColor.setText(vtColor.toUpperCase());
        chipVTColor.setChipIconTint(ColorStateList.valueOf(colorVT));

        // Chip NT
        chipNTColor.setText(ntColor.toUpperCase());
        chipNTColor.setChipIconTint(ColorStateList.valueOf(colorNT));

        // Chipy v hlavičce karty Jemné doladění barev
        chipFineTuningVT.setText(getString(R.string.graph_color_chip_vt, vtColor.toUpperCase()));
        chipFineTuningVT.setChipIconTint(ColorStateList.valueOf(colorVT));

        chipFineTuningNT.setText(getString(R.string.graph_color_chip_nt, ntColor.toUpperCase()));
        chipFineTuningNT.setChipIconTint(ColorStateList.valueOf(colorNT));

        // Náhledové sloupečky v grafu
        setCornerRadius(previewBarVT1, colorVT, 8);
        setCornerRadius(previewBarVT2, colorVT, 8);
        setCornerRadius(previewLegendVT, colorVT, 4);

        setCornerRadius(previewBarNT1, colorNT, 8);
        setCornerRadius(previewBarNT2, colorNT, 8);
        setCornerRadius(previewLegendNT, colorNT, 4);
    }


    private void setCornerRadius(View view, int color, float dp) {
        float px = dp * getResources().getDisplayMetrics().density;
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.RECTANGLE);
        drawable.setCornerRadii(new float[]{px, px, px, px, 0, 0, 0, 0});
        drawable.setColor(color);
        view.setBackground(drawable);
    }


    private void addAndSaveColorHistory(String vtColor, String ntColor) {
        colorsHistory.add(vtColor + ";" + ntColor);
        if (colorsHistory.size() > 5) {
            colorsHistory.remove(0);
        }
        DataGraphColor dataGraphColor = new DataGraphColor(getContext());
        dataGraphColor.open();
        dataGraphColor.saveColorsHistory(colorsHistory);
        dataGraphColor.close();
    }


    private void loadHistoryColors() {
        DataGraphColor dataGraphColor = new DataGraphColor(requireContext());
        dataGraphColor.open();
        colorsHistory = dataGraphColor.loadColorsHistory();
        dataGraphColor.close();
    }


    private void loadColorsFromDb() {
        DataGraphColor dataGraphColor = new DataGraphColor(requireContext());
        dataGraphColor.open();
        int[] colors = dataGraphColor.loadColors();
        dataGraphColor.close();

        graphColorView.setColors(colors);
        vtColor = graphColorView.getColorVTHTML();
        ntColor = graphColorView.getColorNTHTML();

        updateUIColors(colors[0], colors[1]);
    }

}
