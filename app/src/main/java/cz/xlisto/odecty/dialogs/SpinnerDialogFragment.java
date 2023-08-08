package cz.xlisto.odecty.dialogs;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;

import java.util.ArrayList;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import cz.xlisto.odecty.R;

/**
 * Xlisto 11.07.2023 5:31
 */
public class SpinnerDialogFragment extends DialogFragment {
    private static final String TAG = "SpinnerDialogFragment";
    public static final String ARG_CODE = "code";
    public static final String ARG_GROUP = "group";
    public static final String ARG_CATEGORY = "category";
    private static final String ARG_CODES = "codes";
    private static final String ARG_CODES_EXCEPTION = "codesException";
    private static final String ARG_GROUPS_LIST = "groupsList";
    private static final String ARG_GROUP_EXCEPTION_LIST = "groupExceptionList";
    public static final String ARG_AREA = "area";
    public static final String ARG_IS_EXCEPTION = "isExceptionArea";
    public static final String ARG_POSITION = "position";
    public static final String RESULT = "result";

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

    public static SpinnerDialogFragment newInstance(String title, ArrayList<String> codes, ArrayList<String> codesException, ArrayList<String[]> groupsList, ArrayList<String[]> groupExceptionList, String area, String exceptionArea, String flagResultDialogFragment) {
        SpinnerDialogFragment spinnerDialogFragment = new SpinnerDialogFragment();
        spinnerDialogFragment.title = title;
        spinnerDialogFragment.codes = codes;
        spinnerDialogFragment.groupsList = groupsList;
        spinnerDialogFragment.groupExceptionList = groupExceptionList;
        spinnerDialogFragment.codesException = codesException;
        spinnerDialogFragment.exceptionArea = exceptionArea;
        spinnerDialogFragment.area = area;
        spinnerDialogFragment.flagResultDialogFragment = flagResultDialogFragment;
        return spinnerDialogFragment;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            codes = (ArrayList<String>) savedInstanceState.getSerializable(ARG_CODES);
            codesException = (ArrayList<String>) savedInstanceState.getSerializable(ARG_CODES_EXCEPTION);
            groupsList = (ArrayList<String[]>) savedInstanceState.getSerializable(ARG_GROUPS_LIST);
            groupExceptionList = (ArrayList<String[]>) savedInstanceState.getSerializable(ARG_GROUP_EXCEPTION_LIST);
            area = savedInstanceState.getString(ARG_AREA);
            isExceptionArea = savedInstanceState.getBoolean(ARG_IS_EXCEPTION);
            swExceptionArea.setChecked(isExceptionArea);
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle(title);
        View view = View.inflate(requireContext(), R.layout.dialog_spinner, null);
        spinner = view.findViewById(R.id.spCodes);
        swExceptionArea = view.findViewById(R.id.swExceptionArea);
        TextView tvExceptionArea = view.findViewById(R.id.tvExceptionArea);
        TextView tvExceptionAreaList = view.findViewById(R.id.tvExceptionAreaList);
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
        builder.setPositiveButton("OK", (dialog, which) -> {
            Bundle bundle = new Bundle();
            bundle.putBoolean(RESULT, true);
            bundle.putInt(ARG_POSITION, selectedPosition);
            bundle.putString(ARG_AREA, area);
            if(!isExceptionArea) {
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
        builder.setNegativeButton("Zrušit", (dialog, which) -> {
            Bundle bundle = new Bundle();
            bundle.putBoolean(RESULT, false);
            requireActivity().getSupportFragmentManager().setFragmentResult(flagResultDialogFragment, bundle);
        });

        swExceptionArea.setOnCheckedChangeListener((buttonView, isChecked) -> {
            isExceptionArea = isChecked;
            setAdapter(isExceptionArea ? codes : codesException);
        });
        isExceptionArea = swExceptionArea.isChecked();

        if (exceptionArea == null || exceptionArea.isEmpty() || codesException.size()==0) {
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
        outState.putSerializable(ARG_CODES, codes);
        outState.putSerializable(ARG_CODES_EXCEPTION, codesException);
        outState.putSerializable(ARG_GROUPS_LIST, groupsList);
        outState.putSerializable(ARG_GROUP_EXCEPTION_LIST, groupExceptionList);
        outState.putBoolean(ARG_IS_EXCEPTION, isExceptionArea);
        outState.putString(ARG_AREA, area);
    }

    private void setAdapter(ArrayList<String> list) {
        spinner.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, list));
    }
}
