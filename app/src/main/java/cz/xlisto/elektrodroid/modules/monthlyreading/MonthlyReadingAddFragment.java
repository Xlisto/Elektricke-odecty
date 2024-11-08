package cz.xlisto.elektrodroid.modules.monthlyreading;


import static cz.xlisto.elektrodroid.shp.ShPAddEditMonthlyReading.ARG_SHOW_ADD_PAYMENT_MONTHLY_READING;
import static cz.xlisto.elektrodroid.shp.ShPMonthlyReading.ADD_BACKUP_NEW_READING;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Calendar;

import cz.xlisto.elektrodroid.R;
import cz.xlisto.elektrodroid.databaze.DataMonthlyReadingSource;
import cz.xlisto.elektrodroid.databaze.DataSubscriptionPointSource;
import cz.xlisto.elektrodroid.models.MonthlyReadingModel;
import cz.xlisto.elektrodroid.models.PriceListModel;
import cz.xlisto.elektrodroid.modules.pricelist.PriceListFragment;
import cz.xlisto.elektrodroid.ownview.ViewHelper;
import cz.xlisto.elektrodroid.utils.Keyboard;


/**
 * Fragment pro přidání měsíčního odečtu.
 */
public class MonthlyReadingAddFragment extends MonthlyReadingAddEditFragmentAbstract {
    private final String TAG = "MonthlyReadingAddFragment";
    private static final String ARG_TABLE_O = "table_O";
    private static final String ARG_TABLE_PAYMENT = "table_PLATBY";
    private final String ARG_DATE_PAYMENT = "datePayment";
    private String tableO, tablePayments;
    private long datePayment = 0L;


    public MonthlyReadingAddFragment() {
        // Required empty public constructor
    }


    /**
     * Použijte tuto tovární metodu k vytvoření nové instance
     * tohoto fragmentu pomocí poskytnutých parametrů.
     *
     * @param tableO        Jméno databáze měsíčních odečtů (O).
     * @param tablePayments Jméno databáze plateb (PLATBY).
     * @return Nová instance fragmentu MonthlyReadingAddFragment.
     */
    public static MonthlyReadingAddFragment newInstance(String tableO, String tablePayments) {
        MonthlyReadingAddFragment fragment = new MonthlyReadingAddFragment();
        Bundle args = new Bundle();
        args.putString(ARG_TABLE_O, tableO);
        args.putString(ARG_TABLE_PAYMENT, tablePayments);
        fragment.setArguments(args);
        return fragment;
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            tableO = getArguments().getString(ARG_TABLE_O);
            tablePayments = getArguments().getString(ARG_TABLE_PAYMENT);
        }
    }


    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        requireActivity().invalidateOptionsMenu();
        if (isFirstLoad)
            btnDate.setText(ViewHelper.getTodayDate());

        btnSave.setOnClickListener(v -> {
            if (selectedIdPriceList > 0 || cbFirstReading.isChecked() || countMonthlyReading == 0) {
                //načítám poslední měsíční odečet
                DataMonthlyReadingSource dataMonthlyReadingSource = new DataMonthlyReadingSource(getContext());
                dataMonthlyReadingSource.open();

                MonthlyReadingModel newMonthlyReading = createMonthlyReading();
                //získává id nově uloženého měsíčního záznamu
                long id = dataMonthlyReadingSource.insertMonthlyReading(tableO, newMonthlyReading);
                //nastavuji id záznamu
                newMonthlyReading.setId(id);

                MonthlyReadingModel lastMonthlyReading = dataMonthlyReadingSource.loadLastMonthlyReadingByDate(tableO);
                dataMonthlyReadingSource.close();

                //přidat platbu: true; první odečet: false
                if (cbAddPayment.isChecked() && !cbFirstReading.isChecked() && countMonthlyReading != 0) {
                    DataSubscriptionPointSource dataSubscriptionPointSource = new DataSubscriptionPointSource(requireContext());
                    dataSubscriptionPointSource.open();
                    dataSubscriptionPointSource.insertPayment(tablePayments, createPayment(datePayment));
                    dataSubscriptionPointSource.close();
                }

                //reakce, pokud se jedná o první záznam právě vloženého měsíčního odečtu
                //úprava posledního záznamu v období bez faktury
                updateItemInvoice(lastMonthlyReading);


                if (cbAddBackup.isChecked()) {
                    backupMonthlyReading();
                }
                Keyboard.hide(requireActivity());
                getParentFragmentManager().popBackStack();
            } else {
                Toast.makeText(getActivity(), getResources().getString(R.string.vyberteCenik), Toast.LENGTH_SHORT).show();
            }
        });

        cbAddPayment.setOnCheckedChangeListener((buttonView, isChecked) -> {
            shPAddEditMonthlyReading.set(ARG_SHOW_ADD_PAYMENT_MONTHLY_READING, cbAddPayment.isChecked());
            setShowAddPayment();
        });

        cbAddBackup.setOnCheckedChangeListener((buttonView, isChecked) -> shPAddEditMonthlyReading.set(ADD_BACKUP_NEW_READING, cbAddBackup.isChecked()));

        etDatePayment.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                setDatePayment();
            }


            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                setDatePayment();
            }


            @Override
            public void afterTextChanged(Editable s) {
                setDatePayment();
            }
        });
        etDatePayment.setText(shPAddEditMonthlyReading.get(ARG_DATE_PAYMENT, ""));
        cbAddPayment.setChecked(shPAddEditMonthlyReading.get(ARG_SHOW_ADD_PAYMENT_MONTHLY_READING, false));
        cbAddBackup.setChecked(shPAddEditMonthlyReading.get(ADD_BACKUP_NEW_READING, false));
        setDatePayment();

        //listener pro výběr ceníku
        getParentFragmentManager().setFragmentResultListener(PriceListFragment.FLAG_PRICE_LIST_FRAGMENT, this, (requestKey, result) -> {
            selectedPriceList = (PriceListModel) result.getSerializable(PriceListFragment.FLAG_RESULT_PRICE_LIST_FRAGMENT);
            if (selectedPriceList != null) {
                selectedIdPriceList = selectedPriceList.getId();
                btnSelectPriceList.setText(selectedPriceList.getName());
            } else {
                selectedIdPriceList = -1L;
                btnSelectPriceList.setText(getResources().getString(R.string.vyberCenik));
            }
        });
    }


    @Override
    public void onResume() {
        super.onResume();
        if (selectedPriceList != null) {
            btnSelectPriceList.setText(selectedPriceList.getName());
            selectedIdPriceList = selectedPriceList.getId();
        }
    }


    /**
     * Doplní platné datum podle zadaného data platby do tvResultDate
     */
    void setDatePayment() {
        if (cbAddPayment.isChecked()) {
            String date = etDatePayment.getText().toString();
            shPAddEditMonthlyReading.set(ARG_DATE_PAYMENT, date);
            if (date.isEmpty()) {
                tvResultDate.setText("");
                return;
            }
            Calendar calendar = Calendar.getInstance();
            calendar.set(Calendar.DAY_OF_MONTH, Integer.parseInt(date));
            String sb = getResources().getString(R.string.result_date) +
                    ViewHelper.getSimpleDateFormat().format(calendar.getTime());
            tvResultDate.setText(sb);
            datePayment = calendar.getTimeInMillis();
        }
    }
}