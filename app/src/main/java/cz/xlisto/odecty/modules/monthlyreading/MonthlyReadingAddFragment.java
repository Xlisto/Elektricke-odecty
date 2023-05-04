package cz.xlisto.odecty.modules.monthlyreading;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import cz.xlisto.odecty.R;
import cz.xlisto.odecty.databaze.DataSubscriptionPointSource;
import cz.xlisto.odecty.ownview.ViewHelper;

import android.view.View;
import android.widget.Toast;

/**
 * Fragment pro přidání měsíčního odečtu.
 */
public class MonthlyReadingAddFragment extends MonthlyReadingAddEditFragmentAbstract {
    private final String TAG = "MonthlyReadingAddFragment";
    private static final String ARG_TABLE_O = "table_O";
    private static final String ARG_TABLE_PLATBY = "table_PLATBY";

    private String tableO;


    public MonthlyReadingAddFragment() {
        // Required empty public constructor
    }


    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param tableO      Jméno databáze měsíčních odečtů (O).
     * @param tablePlatby Jméno databáze plateb (PLATBY).
     * @return A new instance of fragment MonthlyReadingAddFragment.
     */
    public static MonthlyReadingAddFragment newInstance(String tableO, String tablePlatby) {
        MonthlyReadingAddFragment fragment = new MonthlyReadingAddFragment();
        Bundle args = new Bundle();
        args.putString(ARG_TABLE_O, tableO);
        args.putString(ARG_TABLE_PLATBY, tablePlatby);
        fragment.setArguments(args);
        return fragment;
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            tableO = getArguments().getString(ARG_TABLE_O);
        }
    }


    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (isFirstLoad)
            btnDate.setText(ViewHelper.getTodayDate());

        btnSave.setOnClickListener(v -> {
            if (selectedIdPriceList > 0 || cbFirstReading.isChecked()) {
                DataSubscriptionPointSource dataSubscriptionPointSource = new DataSubscriptionPointSource(getContext());
                dataSubscriptionPointSource.open();
                dataSubscriptionPointSource.insertMonthlyReading(createMonthlyReading(), tableO);
                dataSubscriptionPointSource.close();
                updateLastItemInvoice();
                getParentFragmentManager().popBackStack();
            } else {
                Toast.makeText(getActivity(), getResources().getString(R.string.vyberteCenik), Toast.LENGTH_SHORT).show();
            }
        });
    }
}