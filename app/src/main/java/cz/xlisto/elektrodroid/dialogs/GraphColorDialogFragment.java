package cz.xlisto.elektrodroid.dialogs;


import android.app.Dialog;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cz.xlisto.elektrodroid.R;
import cz.xlisto.elektrodroid.databaze.DataGraphColor;
import cz.xlisto.elektrodroid.modules.graphcolor.GraphHistoryColorAdapter;


/**
 * Dialog pro úpravu barev grafu (VT/NT) pomocí HTML hex formátu v Material 3 provedení.
 *
 * <p>Fragment zobrazuje vstupní pole s TextInputLayout pro barvy VT a NT, průběžný náhled,
 * validaci formátu {@code #RRGGBB} a historii dříve použitých barev.
 * Po potvrzení vrací vybrané hodnoty přes Fragment Result API pod klíčem
 * {@link #RESULT_GRAPH_COLOR_DIALOG_FRAGMENT}.</p>
 *
 * Xlisto 27.10.2023 19:48
 */
public class GraphColorDialogFragment extends DialogFragment {

    public static final String TAG = "GraphColorDialogFragment";
    public static final String ARG_VT_COLOR = "vtColor";
    public static final String ARG_NT_COLOR = "ntColor";
    public static final String RESULT_GRAPH_COLOR_DIALOG_FRAGMENT = "resultGraphColorDialogFragment";
    private View vVT, vNT;
    private EditText etVT, etNT;
    private TextInputLayout tilVT, tilNT;
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

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext());
        View view = getLayoutInflater().inflate(R.layout.dialog_fragment_graph_color, null);

        etVT = view.findViewById(R.id.etVTDialogFragment);
        etNT = view.findViewById(R.id.etNTDialogFragment);
        tilVT = view.findViewById(R.id.tilVT);
        tilNT = view.findViewById(R.id.tilNT);
        vVT = view.findViewById(R.id.vVT);
        vNT = view.findViewById(R.id.vNT);
        RecyclerView rv = view.findViewById(R.id.recycleViewColor);

        etVT.setText(vtColor);
        etNT.setText(ntColor);

        setViewsColors(vVT, vtColor);
        setViewsColors(vNT, ntColor);

        etVT.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }


            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }


            @Override
            public void afterTextChanged(Editable s) {
                if (isValidHtmlColor(etVT, tilVT)) {
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
                if (isValidHtmlColor(etNT, tilNT)) {
                    setViewsColors(vNT, etNT.getText().toString());
                }
            }
        });

        GraphHistoryColorAdapter adapter = new GraphHistoryColorAdapter(this, items);
        rv.setAdapter(adapter);
        rv.setLayoutManager(new LinearLayoutManager(getContext()));

        builder.setView(view);
        builder.setTitle(R.string.set_color_html_title);
        builder.setPositiveButton(R.string.set, (dialog, which) -> {
            Bundle b = new Bundle();
            b.putString(ARG_VT_COLOR, etVT.getText().toString());
            b.putString(ARG_NT_COLOR, etNT.getText().toString());
            getParentFragmentManager().setFragmentResult(GraphColorDialogFragment.RESULT_GRAPH_COLOR_DIALOG_FRAGMENT, b);
        });
        builder.setNegativeButton(R.string.zrusit, (dialog, which) -> dialog.dismiss());

        return builder.create();
    }


    private boolean isValidHtmlColor(EditText et, TextInputLayout til) {
        String htmlColor = et.getText().toString();
        if (!htmlColor.startsWith("#")) {
            htmlColor = "#" + htmlColor;
            et.setText(htmlColor);
            et.setSelection(htmlColor.length());
        }
        Pattern htmlColorPattern = Pattern.compile("^#([A-Fa-f0-9]{6})$");
        Matcher matcher = htmlColorPattern.matcher(htmlColor);

        if (matcher.matches()) {
            til.setError(null);
            til.setErrorEnabled(false);
            return true;
        } else {
            til.setError("Neznámý HEX kód (#RRGGBB)");
            return false;
        }
    }


    private void setViewsColors(View v, String color) {
        try {
            v.setBackgroundColor(Color.parseColor(color));
        } catch (Exception ignored) {
        }
    }


    public void setColors(String[] colors) {
        if (colors != null && colors.length >= 2) {
            setViewsColors(vVT, colors[0]);
            setViewsColors(vNT, colors[1]);
            etVT.setText(colors[0]);
            etNT.setText(colors[1]);
        }
    }


    @Override
    public void onStart() {
        super.onStart();
        DialogButtonColorHelper.apply(this);
    }

}
