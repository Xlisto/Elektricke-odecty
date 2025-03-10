package cz.xlisto.elektrodroid.modules.monthlyreading;


import static cz.xlisto.elektrodroid.format.DecimalFormatHelper.df2;
import static cz.xlisto.elektrodroid.shp.ShPMonthlyReading.ADD_BACKUP_EDT_READING;
import static cz.xlisto.elektrodroid.shp.ShPMonthlyReading.SEND_BACKUP_EDT_READING;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import cz.xlisto.elektrodroid.R;
import cz.xlisto.elektrodroid.databaze.DataMonthlyReadingSource;
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
    private static final String ARG_IS_FIRST_LOAD = "isFirstLoad";
    private static final String ARG_CHANGE_METER = "changeMeter";
    private MonthlyReadingModel monthlyReading;

    private String tableO;
    private long itemId;


    public MonthlyReadingEditFragment() {
        // Required empty public constructor
    }


    /**
     * Použijte tuto tovární metodu k vytvoření nové instance
     * tohoto fragmentu pomocí poskytnutých parametrů.
     *
     * @param tableO Jméno databáze měsíčních odečtů (O).
     * @param itemId Id odečtu v databázi.
     * @return Nová instance fragmentu MonthlyReadingEditFragment.
     */
    public static MonthlyReadingEditFragment newInstance(String tableO, long itemId, boolean isFirstLoad, boolean isChangeMeter) {
        MonthlyReadingEditFragment fragment = new MonthlyReadingEditFragment();
        Bundle args = new Bundle();
        args.putString(ARG_TABLE_O, tableO);
        args.putLong(ARG_ITEM_ID, itemId);
        args.putBoolean(ARG_IS_FIRST_LOAD, isFirstLoad);
        args.putBoolean(ARG_CHANGE_METER, isChangeMeter);
        fragment.setArguments(args);
        return fragment;
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            tableO = getArguments().getString(ARG_TABLE_O);
            itemId = getArguments().getLong(ARG_ITEM_ID, -1L);
            isFirstLoad = getArguments().getBoolean(ARG_IS_FIRST_LOAD, false);
            isChangeMeter = getArguments().getBoolean(ARG_CHANGE_METER, false);
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
            labOtherService.setDefaultText(df2.format(monthlyReading.getOtherServices()));
            cbChangeMeter.setChecked(monthlyReading.isChangeMeter());

            isChangeMeter = monthlyReading.isChangeMeter();

            loadPriceList(monthlyReading.getPriceListId());

            viewModel.setIsChangeMeter(isChangeMeter);
            viewModel.setIsFirstLoad(isFirstLoad);
            viewModel.setSelectedPriceList(priceListFromDatabase);
        }

        if (priceListFromDatabase != null)
            btnSelectPriceList.setText(priceListFromDatabase.getName());

        cbChangeMeter.setEnabled(!isFirstLoad);
        cbAddPayment.setVisibility(View.GONE);
        tvContentAddPayment.setVisibility(View.GONE);
        tvResultDate.setVisibility(View.GONE);
        etDatePayment.setVisibility(View.GONE);

        cbAddBackup.setOnCheckedChangeListener((buttonView, isChecked) -> {
            shPAddEditMonthlyReading.set(ADD_BACKUP_EDT_READING, cbAddBackup.isChecked());
            cbSendBackup.setEnabled(cbAddBackup.isChecked());
        });

        cbSendBackup.setOnCheckedChangeListener((buttonView, isChecked) -> shPAddEditMonthlyReading.set(SEND_BACKUP_EDT_READING, cbSendBackup.isChecked()));

        cbSendBackup.setEnabled(cbAddBackup.isChecked());

        btnSave.setOnClickListener(v -> {
            if (selectedPriceList.getId() > 0 || cbChangeMeter.isChecked() || isFirstLoad) {

                updateMonthlyReading(itemId);
                if (cbAddBackup.isChecked()) {
                    backupMonthlyReading();
                } else {
                    Keyboard.hide(requireActivity());
                    getParentFragmentManager().popBackStack();
                }
            } else {
                Toast.makeText(requireActivity(), getResources().getString(R.string.vyberteCenik), Toast.LENGTH_SHORT).show();
            }
        });

        cbAddBackup.setChecked(shPAddEditMonthlyReading.get(ADD_BACKUP_EDT_READING, false));
        cbSendBackup.setChecked(shPAddEditMonthlyReading.get(SEND_BACKUP_EDT_READING, false));

        //listener pro výběr ceníku
        getParentFragmentManager().setFragmentResultListener(PriceListFragment.FLAG_PRICE_LIST_FRAGMENT, this, (requestKey, result) -> {
            selectedPriceList = (PriceListModel) result.getSerializable(PriceListFragment.FLAG_RESULT_PRICE_LIST_FRAGMENT);

            if (selectedPriceList != null) {
                btnSelectPriceList.setText(selectedPriceList.getName());
                viewModel.setSelectedPriceList(selectedPriceList);
            }
        });
    }


    @Override
    public void onResume() {
        super.onResume();
    }


    /**
     * Načte z databáze objekt měsíčního odečtu.
     */
    private void loadMonthlyReading() {
        DataSubscriptionPointSource dataSubscriptionPointSource = new DataSubscriptionPointSource(requireActivity());
        dataSubscriptionPointSource.open();
        monthlyReading = dataSubscriptionPointSource.loadMonthlyReading(tableO, itemId, 0, Long.MAX_VALUE);
        dataSubscriptionPointSource.close();
    }


    /**
     * Uloží upravený měsíční odečet do databáze.
     *
     * @param itemId long id měsíčního odečtu v databázi.
     */
    private void updateMonthlyReading(long itemId) {
        MonthlyReadingModel newMonthlyReading = createMonthlyReading();
        newMonthlyReading.setId(itemId);

        DataMonthlyReadingSource dataMonthlyReadingSource = new DataMonthlyReadingSource(requireActivity());
        dataMonthlyReadingSource.open();
        dataMonthlyReadingSource.updateMonthlyReading(newMonthlyReading, itemId, tableO);
        MonthlyReadingModel lastMonthlyReading = dataMonthlyReadingSource.loadLastMonthlyReadingByDate(tableO);
        dataMonthlyReadingSource.close();
        //úprava posledního záznamu v období bez faktury
        updateItemInvoice(lastMonthlyReading);
    }

    /**
     * Metoda, která se volá, když je dostupné internetové připojení.
     */
    @Override
    public void onNetworkAvailable() {
        internetAvailable = true;
        setShowCbSendBackup();
    }

    /**
     * Metoda, která se volá, když je ztraceno internetové připojení.
     */
    @Override
    public void onNetworkLost() {
        internetAvailable = false;
        setShowCbSendBackup();
    }

}
