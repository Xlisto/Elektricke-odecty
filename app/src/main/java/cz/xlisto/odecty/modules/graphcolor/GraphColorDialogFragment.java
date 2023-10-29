package cz.xlisto.odecty.modules.graphcolor;

import android.app.AlertDialog;
import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import cz.xlisto.odecty.R;
import cz.xlisto.odecty.databaze.DataGraphColor;

/**
 * Dialogové okno pro nastavení barev grafu.
 * Xlisto 27.10.2023 19:48
 */
public class GraphColorDialogFragment extends DialogFragment {
    static final String TAG = "GraphColorDialogFragment";
    public static final String ARG_VT_COLOR = "vtColor";
    public static final String ARG_NT_COLOR = "ntColor";
    public static final String RESULT_GRAPH_COLOR_DIALOG_FRAGMENT = "resultGraphColorDialogFragment";
    private Drawable drawableEditText;
    private View vVT, vNT;
    private EditText etVT, etNT;
    private ArrayList<String> items = new ArrayList<>();


    public static GraphColorDialogFragment newInstance(String vtColor, String ntColor) {
        Bundle bundle = new Bundle();
        bundle.putString(ARG_VT_COLOR, vtColor);
        bundle.putString(ARG_NT_COLOR, ntColor);
        GraphColorDialogFragment graphColorDialogFragment = new GraphColorDialogFragment();
        graphColorDialogFragment.setArguments(bundle);
        return graphColorDialogFragment;
    }


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //načítám historii barev
        DataGraphColor dataGraphColor = new DataGraphColor(getContext());
        dataGraphColor.open();
        items = dataGraphColor.loadColorsHistory();
        dataGraphColor.close();
    }


    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        Bundle bundle = getArguments();
        assert bundle != null;
        String vtColor = bundle.getString(ARG_VT_COLOR);
        String ntColor = bundle.getString(ARG_NT_COLOR);

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        View view = getLayoutInflater().inflate(R.layout.dialog_fragment_graph_color, null);

        etVT = view.findViewById(R.id.etVTDialogFragment);
        etNT = view.findViewById(R.id.etNTDialogFragment);
        Button btnOK = view.findViewById(R.id.btnSave);
        Button btnCancel = view.findViewById(R.id.btnCancel);
        vVT = view.findViewById(R.id.vVT);
        vNT = view.findViewById(R.id.vNT);
        RecyclerView rv = view.findViewById(R.id.recycleViewColor);

        drawableEditText = etVT.getBackground();

        etVT.setText(vtColor);
        etNT.setText(ntColor);

        setViewsColors(vVT, vtColor);
        setViewsColors(vNT, ntColor);

        btnOK.setOnClickListener(v -> {
            Bundle b = new Bundle();
            b.putString(ARG_VT_COLOR, etVT.getText().toString());
            b.putString(ARG_NT_COLOR, etNT.getText().toString());
            getParentFragmentManager().setFragmentResult(GraphColorDialogFragment.RESULT_GRAPH_COLOR_DIALOG_FRAGMENT, b);
            dismiss();
        });
        btnCancel.setOnClickListener(v -> dismiss());

        etVT.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                isValidHtmlColor(etVT);
                if (isValidHtmlColor(etVT)) {
                    setViewsColors(vVT, etVT.getText().toString());
                }
            }
        });

        etNT.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                isValidHtmlColor(etNT);
                if (isValidHtmlColor(etNT)) {
                    setViewsColors(vNT, etNT.getText().toString());
                }
            }
        });


        GraphHistoryColorAdapter adapter = new GraphHistoryColorAdapter(this,items);
        rv.setAdapter(adapter);
        rv.setLayoutManager(new LinearLayoutManager(getContext()));

        builder.setView(view);
        builder.setTitle(R.string.set_color_html_title);
        return builder.create();
    }


    /**
     * Ověří, zda je vstupní řetězec platným kódem barvy v HTML formátu.
     *
     * @param et Kontrolovaný EditText
     * @return true pokud je vstupní řetězec platným kódem barvy v HTML formátu
     */
    private boolean isValidHtmlColor(EditText et) {
        String htmlColor = et.getText().toString(); // Získá text z EditText
        if (!htmlColor.startsWith("#")) {
            // Pokud nezačíná křížkem, přidá ho zpět
            htmlColor = "#" + htmlColor;
            et.setText(htmlColor); // Nastaví text s křížkem zpět do EditText
            et.setSelection(1); // Nastaví kurzor na konec
        }
        Pattern htmlColorPattern = Pattern.compile("^#([A-Fa-f0-9]{6})$");
        Matcher matcher = htmlColorPattern.matcher(htmlColor);

        if (matcher.matches()) {
            // Platný kód barvy
            et.setBackground(drawableEditText);
        } else {
            // Neplatný kód barvy
            et.setBackgroundColor(Color.RED);
        }
        return matcher.matches();
    }


    /**
     * Nastaví barvu pozadí zadaného view.
     *
     * @param v     View, kterému se nastaví barva pozadí
     * @param color Barva pozadí ve formátu HTML
     */
    private void setViewsColors(View v, String color) {
        v.setBackgroundColor(Color.parseColor(color));
    }


    /**
     * Nastaví barvy do EditTextů a View.
     * @param colors pole s barvami ve formátu HTML
     */
    public void setColors(String[] colors){
        vVT.setBackgroundColor(Color.parseColor(colors[0]));
        vNT.setBackgroundColor(Color.parseColor(colors[1]));
        etVT.setText(colors[0]);
        etNT.setText(colors[1]);
    }


}
