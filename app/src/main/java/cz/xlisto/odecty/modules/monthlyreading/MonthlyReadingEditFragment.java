package cz.xlisto.odecty.modules.monthlyreading;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import cz.xlisto.odecty.R;
import cz.xlisto.odecty.databaze.DataPriceListSource;
import cz.xlisto.odecty.databaze.DataSubscriptionPointSource;
import cz.xlisto.odecty.models.MonthlyReadingModel;
import cz.xlisto.odecty.models.PriceListModel;
import cz.xlisto.odecty.ownview.ViewHelper;

import static cz.xlisto.odecty.format.DecimalFormatHelper.*;

/**
 * Fragment pro editaci měsíčního odečtu.
 */
public class MonthlyReadingEditFragment extends MonthlyReadingAddEditFragmentAbstract {
    private final String TAG = "MonthlyReadingEditFragment";
    private static final String ARG_TABLE_O = "table_O";
    private static final String ARG_TABLE_PLATBY = "table_PLATBY";
    private static final String ARG_ITEM_ID = "item_id";
    private static final String IS_FIRST_LOAD = "isFirstLoad";
    private MonthlyReadingModel monthlyReading;
    private PriceListModel priceList;
    private String tableO;
    private String tablePlatby;
    private long itemId;


    public MonthlyReadingEditFragment() {
        // Required empty public constructor
    }


    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param tableO      Jméno databáze měsíčních odečtů (O).
     * @param tablePlatby Jméno databáze plateb (PLATBY).
     * @param itemId      Id odečtu v databázi.
     * @return A new instance of fragment MonthlyReadingEditFragment.
     */
    public static MonthlyReadingEditFragment newInstance(String tableO, String tablePlatby, long itemId) {
        MonthlyReadingEditFragment fragment = new MonthlyReadingEditFragment();
        Bundle args = new Bundle();
        args.putString(ARG_TABLE_O, tableO);
        args.putString(ARG_TABLE_PLATBY, tablePlatby);
        args.putLong(ARG_ITEM_ID, itemId);
        fragment.setArguments(args);
        return fragment;
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            tableO = getArguments().getString(ARG_TABLE_O);
            tablePlatby = getArguments().getString(ARG_TABLE_PLATBY);
            itemId = getArguments().getLong(ARG_ITEM_ID, -1L);
        }
    }


    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (savedInstanceState == null) {
            loadMonthlyReading();

            btnDate.setText(ViewHelper.convertLongToTime(monthlyReading.getDate()));
            labVT.setDefaultText(df1.format(monthlyReading.getVt()));
            labNT.setDefaultText(df1.format(monthlyReading.getNt()));
            labPayment.setDefaultText(df2.format(monthlyReading.getPayment()));
            labDescription.setDefaultText(monthlyReading.getDescription());
            labOtherServices.setDefaultText(df2.format(monthlyReading.getOtherServices()));
            cbFirstReading.setChecked(monthlyReading.isFirst());
            selectedIdPriceList = monthlyReading.getPriceListId();

            loadPriceList();
            if (priceList != null)
                btnSelectPriceList.setText(priceList.getName());
        }



        btnSave.setOnClickListener(v -> {
            if (priceList != null || cbFirstReading.isChecked()) {
                updateMonthlyReading(itemId);
                getParentFragmentManager().popBackStack();
            } else {
                Toast.makeText(getActivity(), getResources().getString(R.string.vyberteCenik), Toast.LENGTH_SHORT).show();
            }
        });

    }


    @Override
    public void onResume() {
        super.onResume();
        loadPriceList();
        btnSave.setEnabled(!priceList.isEmpty());
    }


    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(IS_FIRST_LOAD, true);
    }


    /**
     * Načte z databáze objekt měsíčního odečtu.
     */
    private void loadMonthlyReading() {
        DataSubscriptionPointSource dataSubscriptionPointSource = new DataSubscriptionPointSource(getActivity());
        dataSubscriptionPointSource.open();
        monthlyReading = dataSubscriptionPointSource.loadMonthlyReading(tableO, itemId);
        dataSubscriptionPointSource.close();
    }


    /**
     * Načte s databáze objekt ceníku.
     */
    private void loadPriceList() {
        DataPriceListSource dataPriceListSource = new DataPriceListSource(getActivity());
        dataPriceListSource.open();
        priceList = dataPriceListSource.readPrice(selectedIdPriceList);
        dataPriceListSource.close();
    }


    /**
     * Uloží úpravený měsíční odečet do databáze.
     * @param itemId long id měsíčního odečtu v databázi.
     */
    private void updateMonthlyReading(long itemId) {
        DataSubscriptionPointSource dataSubscriptionPointSource = new DataSubscriptionPointSource(getActivity());
        dataSubscriptionPointSource.open();
        dataSubscriptionPointSource.updateMonthlyReading(createMonthlyReading(), itemId, tableO);
        dataSubscriptionPointSource.close();
        updateLastItemInvoice();
    }
}
