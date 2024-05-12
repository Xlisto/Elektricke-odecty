package cz.xlisto.elektrodroid.modules.monthlyreading;


import static cz.xlisto.elektrodroid.format.DecimalFormatHelper.df2;
import static cz.xlisto.elektrodroid.shp.ShPMonthlyReading.ADD_BACKUP_EDT_READING;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import cz.xlisto.elektrodroid.R;
import cz.xlisto.elektrodroid.databaze.DataMonthlyReadingSource;
import cz.xlisto.elektrodroid.databaze.DataPriceListSource;
import cz.xlisto.elektrodroid.databaze.DataSubscriptionPointSource;
import cz.xlisto.elektrodroid.models.MonthlyReadingModel;
import cz.xlisto.elektrodroid.models.PriceListModel;
import cz.xlisto.elektrodroid.modules.pricelist.PriceListFragment;
import cz.xlisto.elektrodroid.ownview.ViewHelper;
import cz.xlisto.elektrodroid.utils.Keyboard;


/**
 * Fragment pro editaci měsíčního odečtu.
 */
public class MonthlyReadingEditFragment extends MonthlyReadingAddEditFragmentAbstract {
    private final String TAG = "MonthlyReadingEditFragment";
    private static final String ARG_TABLE_O = "table_O";
    private static final String ARG_ITEM_ID = "item_id";
    private static final String IS_FIRST_LOAD = "isFirstLoad";
    private MonthlyReadingModel monthlyReading;
    private PriceListModel priceList;
    private String tableO;
    private long itemId;


    public MonthlyReadingEditFragment() {
        // Required empty public constructor
    }


    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param tableO Jméno databáze měsíčních odečtů (O).
     * @param itemId Id odečtu v databázi.
     * @return A new instance of fragment MonthlyReadingEditFragment.
     */
    public static MonthlyReadingEditFragment newInstance(String tableO, long itemId) {
        MonthlyReadingEditFragment fragment = new MonthlyReadingEditFragment();
        Bundle args = new Bundle();
        args.putString(ARG_TABLE_O, tableO);
        args.putLong(ARG_ITEM_ID, itemId);
        fragment.setArguments(args);
        return fragment;
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            tableO = getArguments().getString(ARG_TABLE_O);
            itemId = getArguments().getLong(ARG_ITEM_ID, -1L);
        }
    }


    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (savedInstanceState == null) {
            loadMonthlyReading();

            btnDate.setText(ViewHelper.convertLongToDate(monthlyReading.getDate()));
            labVT.setDefaultText(df2.format(monthlyReading.getVt()));
            labNT.setDefaultText(df2.format(monthlyReading.getNt()));
            labPayment.setDefaultText(df2.format(monthlyReading.getPayment()));
            labDescription.setDefaultText(monthlyReading.getDescription());
            labOtherServices.setDefaultText(df2.format(monthlyReading.getOtherServices()));
            cbFirstReading.setChecked(monthlyReading.isFirst());
            selectedIdPriceList = monthlyReading.getPriceListId();

            loadPriceList();
            if (priceList != null)
                btnSelectPriceList.setText(priceList.getName());
        }

        cbAddPayment.setVisibility(View.GONE);
        tvContentAddPayment.setVisibility(View.GONE);
        tvResultDate.setVisibility(View.GONE);
        etDatePayment.setVisibility(View.GONE);

        cbAddBackup.setOnCheckedChangeListener((buttonView, isChecked) -> shPAddEditMonthlyReading.set(ADD_BACKUP_EDT_READING, cbAddBackup.isChecked()));


        btnSave.setOnClickListener(v -> {
            if (priceList != null || cbFirstReading.isChecked()) {
                updateMonthlyReading(itemId);
                if (cbAddBackup.isChecked()) {
                    backupMonthlyReading();
                }
                Keyboard.hide(requireActivity());
                getParentFragmentManager().popBackStack();
            } else {
                Toast.makeText(getActivity(), getResources().getString(R.string.vyberteCenik), Toast.LENGTH_SHORT).show();
            }
        });

        cbAddBackup.setChecked(shPAddEditMonthlyReading.get(ADD_BACKUP_EDT_READING, false));

        //listener pro výběr ceníku
        getParentFragmentManager().setFragmentResultListener(PriceListFragment.FLAG_PRICE_LIST_FRAGMENT, this, (requestKey, result) -> {
            selectedPriceList = (PriceListModel) result.getSerializable(PriceListFragment.FLAG_RESULT_PRICE_LIST_FRAGMENT);

            if (selectedPriceList != null) {
                selectedIdPriceList = selectedPriceList.getId();
                btnSelectPriceList.setText(selectedPriceList.getName());
            }
        });
    }


    @Override
    public void onResume() {
        super.onResume();
        loadPriceList();
        if (countMonthlyReading == 1) {//pokud je první záznam v měsíčním odečtu
            btnSelectPriceList.setVisibility(View.GONE);
            cbFirstReading.setVisibility(View.GONE);
            labPayment.setEnabled(false);
        } else if (cbFirstReading.isChecked()) {//pokud je výměna elektroměru
            btnSelectPriceList.setVisibility(View.GONE);
        } else //ostatní záznamy, pokud není nastaven ceník
            btnSave.setEnabled(!priceList.isEmpty());
        //nastavení textu tlačítka pr výběr ceníku; zejména pro nastavení při návratu z ceníku
        btnSelectPriceList.setText(priceList.getName());
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
        monthlyReading = dataSubscriptionPointSource.loadMonthlyReading(tableO, itemId, 0, Long.MAX_VALUE);
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
     * Uloží upravený měsíční odečet do databáze.
     *
     * @param itemId long id měsíčního odečtu v databázi.
     */
    private void updateMonthlyReading(long itemId) {
        MonthlyReadingModel newMonthlyReading = createMonthlyReading();
        newMonthlyReading.setId(itemId);

        DataSubscriptionPointSource dataSubscriptionPointSource = new DataSubscriptionPointSource(getActivity());
        dataSubscriptionPointSource.open();
        dataSubscriptionPointSource.updateMonthlyReading(newMonthlyReading, itemId, tableO);
        dataSubscriptionPointSource.close();

        DataMonthlyReadingSource dataMonthlyReadingSource = new DataMonthlyReadingSource(getActivity());
        dataMonthlyReadingSource.open();
        MonthlyReadingModel lastMonthlyReading = dataMonthlyReadingSource.loadLastMonthlyReadingByDate(tableO);
        dataMonthlyReadingSource.close();
        //kontrola, zda-li je upravovaný měsíční odečet posledním odečtem
        //if (lastMonthlyReading.getId() == itemId) {
        //reakce, pokud se jedná o první záznam právě vloženého měsíčního odečtu
        //úprava posledního záznamu v období bez faktury
        updateItemInvoice(lastMonthlyReading);
        //}
    }
}
