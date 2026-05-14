package cz.xlisto.elektrodroid.dialogs;


import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import java.util.ArrayList;

import cz.xlisto.elektrodroid.R;


/**
 * Dialog pro výběr HDO kódu, skupiny a kategorie.
 * <p>
 * Podporuje standardní i výjimečnou oblast. Výsledek je předán přes
 * Fragment Result API pod klíčem zadaným při vytvoření instance.
 */
public class SelectHdoCategoryDialogFragment extends DialogFragment {
    public static final String ARG_TITLE = "title";
    public static final String ARG_CODE = "code";
    public static final String ARG_GROUP = "group";
    public static final String ARG_CATEGORY = "category";
    private static final String ARG_CODES = "codes";
    private static final String ARG_CODES_EXCEPTION = "codesException";
    private static final String ARG_GROUPS_LIST = "groupsList";
    private static final String ARG_GROUP_EXCEPTION_LIST = "groupExceptionList";
    public static final String ARG_AREA = "area";
    public static final String ARG_EXCEPTION_AREA = "exceptionArea";
    public static final String ARG_IS_EXCEPTION = "isExceptionArea";
    public static final String ARG_POSITION = "position";
    public static final String RESULT = "result";
    public static final String FLAG_RESULT_DIALOG_FRAGMENT = "flagResultDialogFragment";

    private String title = "";
    private String area = "";
    private ArrayList<String> codes, codesException;
    private ArrayList<String[]> groupsList, groupExceptionList;
    private Spinner spinner;
    @SuppressLint("UseSwitchCompatOrMaterialCode")
    private Switch swExceptionArea;
    private String exceptionArea;
    private String flagResultDialogFragment;
    private int selectedPosition;
    private boolean isExceptionArea = false;


    /**
     * Vytvoří novou instanci dialogu s daty pro obě varianty oblasti.
     *
     * @param title                    titulek dialogu
     * @param codes                    seznam kódů pro standardní oblast
     * @param codesException           seznam kódů pro výjimečnou oblast
     * @param groupsList               dvojice skupina/kategorie pro standardní oblast
     * @param groupExceptionList       dvojice skupina/kategorie pro výjimečnou oblast
     * @param area                     název standardní oblasti
     * @param exceptionArea            název výjimečné oblasti
     * @param flagResultDialogFragment klíč pro odeslání výsledku
     * @return připravená instance dialogu
     */
    public static SelectHdoCategoryDialogFragment newInstance(String title, ArrayList<String> codes, ArrayList<String> codesException, ArrayList<String[]> groupsList, ArrayList<String[]> groupExceptionList, String area, String exceptionArea, String flagResultDialogFragment) {
        SelectHdoCategoryDialogFragment selectHdoCategoryDialogFragment = new SelectHdoCategoryDialogFragment();
        selectHdoCategoryDialogFragment.title = title;
        selectHdoCategoryDialogFragment.codes = codes;
        selectHdoCategoryDialogFragment.groupsList = groupsList;
        selectHdoCategoryDialogFragment.codesException = codesException;
        selectHdoCategoryDialogFragment.groupExceptionList = groupExceptionList;
        selectHdoCategoryDialogFragment.exceptionArea = exceptionArea;
        selectHdoCategoryDialogFragment.area = area;
        selectHdoCategoryDialogFragment.flagResultDialogFragment = flagResultDialogFragment;
        return selectHdoCategoryDialogFragment;
    }


    /**
     * Sestaví dialog, obnoví stav po rotaci a nastaví předání vybraných dat.
     *
     * @param savedInstanceState uložený stav dialogu, může být {@code null}
     * @return vytvořený dialog
     */
    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext(), R.style.DialogTheme);
        builder.setTitle(title);
        View view = View.inflate(requireContext(), R.layout.dialog_select_hdo_category, null);
        spinner = view.findViewById(R.id.spCodes);
        swExceptionArea = view.findViewById(R.id.swExceptionArea);
        TextView tvExceptionArea = view.findViewById(R.id.tvExceptionArea);
        TextView tvExceptionAreaList = view.findViewById(R.id.tvExceptionAreaList);

        if (savedInstanceState != null) {
            builder.setTitle(savedInstanceState.getString(ARG_TITLE));
            codes = savedInstanceState.getStringArrayList(ARG_CODES);
            groupsList = decodeGroupList(savedInstanceState.getStringArrayList(ARG_GROUPS_LIST));
            codesException = savedInstanceState.getStringArrayList(ARG_CODES_EXCEPTION);
            groupExceptionList = decodeGroupList(savedInstanceState.getStringArrayList(ARG_GROUP_EXCEPTION_LIST));
            exceptionArea = savedInstanceState.getString(ARG_EXCEPTION_AREA);
            area = savedInstanceState.getString(ARG_AREA);
            isExceptionArea = savedInstanceState.getBoolean(ARG_IS_EXCEPTION);
            swExceptionArea.setChecked(isExceptionArea);
            flagResultDialogFragment = savedInstanceState.getString(FLAG_RESULT_DIALOG_FRAGMENT);
        }

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedPosition = position;
            }


            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        builder.setView(view);
        builder.setPositiveButton(requireContext().getResources().getString(R.string.ok), (dialog, which) -> {
            Bundle bundle = new Bundle();
            bundle.putBoolean(RESULT, true);
            bundle.putInt(ARG_POSITION, selectedPosition);
            bundle.putString(ARG_AREA, area);
            if (!isExceptionArea) {
                bundle.putString(ARG_CODE, codesException.get(selectedPosition));
                bundle.putString(ARG_GROUP, groupExceptionList.get(selectedPosition)[0]);
                bundle.putString(ARG_CATEGORY, groupExceptionList.get(selectedPosition)[1]);
            } else {
                bundle.putString(ARG_CODE, codes.get(selectedPosition));
                bundle.putString(ARG_GROUP, groupsList.get(selectedPosition)[0]);
                bundle.putString(ARG_CATEGORY, groupsList.get(selectedPosition)[1]);
            }
            requireActivity().getSupportFragmentManager().setFragmentResult(flagResultDialogFragment, bundle);
        });
        builder.setNegativeButton(requireContext().getResources().getString(R.string.zrusit), (dialog, which) -> {
            Bundle bundle = new Bundle();
            bundle.putBoolean(RESULT, false);
            requireActivity().getSupportFragmentManager().setFragmentResult(flagResultDialogFragment, bundle);
        });

        swExceptionArea.setOnCheckedChangeListener((buttonView, isChecked) -> {
            isExceptionArea = isChecked;
            setAdapter(isExceptionArea ? codes : codesException);
        });
        isExceptionArea = swExceptionArea.isChecked();

        if (exceptionArea == null || exceptionArea.isEmpty() || codesException.isEmpty()) {
            tvExceptionAreaList.setVisibility(View.GONE);
            tvExceptionArea.setVisibility(View.GONE);
            swExceptionArea.setVisibility(View.GONE);
            swExceptionArea.setChecked(true);
        }

        tvExceptionAreaList.setText(exceptionArea);
        setAdapter(isExceptionArea ? codes : codesException);

        return builder.create();
    }

    /**
     * Lifecycle callback po zobrazení dialogu.
     * Aplikuje jednotné barvy tlačítek.
     */
    @Override
    public void onStart() {
        super.onStart();
        DialogButtonColorHelper.apply(this);
    }

    /**
     * Uloží aktuální stav dialogu pro obnovu po změně konfigurace.
     *
     * @param outState výstupní bundle pro persistenci stavu
     */
    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(ARG_TITLE, title);
        outState.putStringArrayList(ARG_CODES, codes);
        outState.putStringArrayList(ARG_CODES_EXCEPTION, codesException);
        outState.putStringArrayList(ARG_GROUPS_LIST, encodeGroupList(groupsList));
        outState.putStringArrayList(ARG_GROUP_EXCEPTION_LIST, encodeGroupList(groupExceptionList));
        outState.putBoolean(ARG_IS_EXCEPTION, isExceptionArea);
        outState.putString(ARG_AREA, area);
        outState.putString(ARG_EXCEPTION_AREA, exceptionArea);
        outState.putString(FLAG_RESULT_DIALOG_FRAGMENT, flagResultDialogFragment);
        outState.putBoolean(ARG_POSITION, swExceptionArea.isChecked());
    }


    /**
     * Nastaví data spinneru.
     *
     * @param list seznam položek pro zobrazení
     */
    private void setAdapter(ArrayList<String> list) {
        spinner.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, list));
    }

    /**
     * Zakóduje seznam dvojic skupina/kategorie do StringArrayList pro uložení do Bundle.
     *
     * @param source seznam dvojic [skupina, kategorie]
     * @return zakódovaný seznam řetězců
     */
    private ArrayList<String> encodeGroupList(ArrayList<String[]> source) {
        if (source == null)
            return null;

        ArrayList<String> encoded = new ArrayList<>(source.size());
        for (String[] item : source) {
            if (item == null || item.length < 2) {
                encoded.add("\u0001");
            } else {
                encoded.add((item[0] == null ? "" : item[0]) + "\u0001" + (item[1] == null ? "" : item[1]));
            }
        }
        return encoded;
    }

    /**
     * Dekóduje seznam dvojic skupina/kategorie uložený jako StringArrayList.
     *
     * @param source zakódovaný seznam řetězců
     * @return seznam dvojic [skupina, kategorie]
     */
    private ArrayList<String[]> decodeGroupList(ArrayList<String> source) {
        if (source == null)
            return null;

        ArrayList<String[]> decoded = new ArrayList<>(source.size());
        for (String item : source) {
            if (item == null) {
                decoded.add(new String[]{"", ""});
                continue;
            }

            String[] parts = item.split("\u0001", 2);
            if (parts.length < 2) {
                decoded.add(new String[]{parts[0], ""});
            } else {
                decoded.add(parts);
            }
        }
        return decoded;
    }
}
