package cz.xlisto.cenik.modules.monthlyreading;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import cz.xlisto.cenik.R;
import cz.xlisto.cenik.databaze.DataSubscriptionPointSource;
import cz.xlisto.cenik.ownview.ViewHelper;

import android.util.Log;
import android.view.View;
import android.widget.Toast;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link MonthlyReadingAddFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class MonthlyReadingAddFragment extends MonthlyReadingAddEditFragmentAbstract {
    private final String TAG = getClass().getName() + " ";

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_TABLE_O = "table_O";
    private static final String ARG_TABLE_PLATBY = "table_PLATBY";

    // TODO: Rename and change types of parameters
    private String tableO;
    private String tablePlatby;

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
    // TODO: Rename and change types and number of parameters
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
            tablePlatby = getArguments().getString(ARG_TABLE_PLATBY);
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (isFirstLoad)
            btnDate.setText(ViewHelper.getTodayDate());

        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
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
            }
        });

    }
}