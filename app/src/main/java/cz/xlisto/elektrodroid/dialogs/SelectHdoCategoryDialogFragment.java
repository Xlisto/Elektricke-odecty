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
 * Dialogové okno se spinnerem pro výběr kódu
 * Xlisto 11.07.2023 5:31
 */
public class SelectHdoCategoryDialogFragment extends DialogFragment {
    private static final String TAG = "SpinnerDialogFragment";
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


    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle(title);
        View view = View.inflate(requireContext(), R.layout.dialog_select_hdo_category, null);
        spinner = view.findViewById(R.id.spCodes);
        swExceptionArea = view.findViewById(R.id.swExceptionArea);
        TextView tvExceptionArea = view.findViewById(R.id.tvExceptionArea);
        TextView tvExceptionAreaList = view.findViewById(R.id.tvExceptionAreaList);

        if (savedInstanceState != null) {
            builder.setTitle(savedInstanceState.getString(ARG_TITLE));
            codes = (ArrayList<String>) savedInstanceState.getSerializable(ARG_CODES);
            groupsList = (ArrayList<String[]>) savedInstanceState.getSerializable(ARG_GROUPS_LIST);
            codesException = (ArrayList<String>) savedInstanceState.getSerializable(ARG_CODES_EXCEPTION);
            groupExceptionList = (ArrayList<String[]>) savedInstanceState.getSerializable(ARG_GROUP_EXCEPTION_LIST);
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


    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(ARG_TITLE, title);
        outState.putSerializable(ARG_CODES, codes);
        outState.putSerializable(ARG_CODES_EXCEPTION, codesException);
        outState.putSerializable(ARG_GROUPS_LIST, groupsList);
        outState.putSerializable(ARG_GROUP_EXCEPTION_LIST, groupExceptionList);
        outState.putBoolean(ARG_IS_EXCEPTION, isExceptionArea);
        outState.putString(ARG_AREA, area);
        outState.putString(ARG_EXCEPTION_AREA, exceptionArea);
        outState.putString(FLAG_RESULT_DIALOG_FRAGMENT, flagResultDialogFragment);
        outState.putBoolean(ARG_POSITION, swExceptionArea.isChecked());
    }


    private void setAdapter(ArrayList<String> list) {
        spinner.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, list));
    }
}
