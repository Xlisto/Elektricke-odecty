package cz.xlisto.elektrodroid.modules.monthlyreading;


import static cz.xlisto.elektrodroid.shp.ShPAddEditMonthlyReading.ARG_FIRST_READING_MONTHLY_READING;
import static cz.xlisto.elektrodroid.shp.ShPAddEditMonthlyReading.ARG_LAST_ID_SELECTED_PRICE_LIST;
import static cz.xlisto.elektrodroid.shp.ShPAddEditMonthlyReading.ARG_SHOW_ADD_PAYMENT_MONTHLY_READING;
import static cz.xlisto.elektrodroid.shp.ShPMonthlyReading.ADD_BACKUP_NEW_READING;
import static cz.xlisto.elektrodroid.shp.ShPMonthlyReading.SEND_BACKUP_NEW_READING;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Calendar;
import java.util.logging.Logger;

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

        long idPriceList = shPAddEditMonthlyReading.get(ARG_LAST_ID_SELECTED_PRICE_LIST, 0L);
        if (idPriceList > 0) {
            loadPriceList(idPriceList);
            viewModel.setSelectedPriceList(priceListFromDatabase);
        }

        btnSave.setOnClickListener(v -> {
            if (selectedPriceList.getId() > 0 || cbChangeMeter.isChecked() || getCountMonthlyReading() == 0) {
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
                if (cbAddPayment.isChecked() && !cbChangeMeter.isChecked() && countMonthlyReading != 0) {
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
                } else {
                    Keyboard.hide(requireActivity());
                    getParentFragmentManager().popBackStack();
                }
            } else {
                Toast.makeText(requireActivity(), getResources().getString(R.string.vyberteCenik), Toast.LENGTH_SHORT).show();
            }
        });

        cbAddPayment.setOnCheckedChangeListener((buttonView, isChecked) -> {
            shPAddEditMonthlyReading.set(ARG_SHOW_ADD_PAYMENT_MONTHLY_READING, cbAddPayment.isChecked());
            setShowAddPayment();
        });

        cbAddBackup.setOnCheckedChangeListener((buttonView, isChecked) -> {
            shPAddEditMonthlyReading.set(ADD_BACKUP_NEW_READING, cbAddBackup.isChecked());
            cbSendBackup.setEnabled(cbAddBackup.isChecked());
        });
        cbSendBackup.setOnCheckedChangeListener((buttonView, isChecked) -> shPAddEditMonthlyReading.set(SEND_BACKUP_NEW_READING, cbSendBackup.isChecked()));
        cbSendBackup.setEnabled(cbAddBackup.isChecked());

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
        cbSendBackup.setChecked(shPAddEditMonthlyReading.get(SEND_BACKUP_NEW_READING, false));
        setDatePayment();

        //skrytí chceckboxů pro označení první záznamu (výměny elektroměru) a tlačítka pro výběr ceníku
        if (getCountMonthlyReading() == 0) {
            viewModel.setIsChangeMeter(true);
            cbChangeMeter.setVisibility(View.GONE);
            cbChangeMeter.setChecked(false);
        }

        if (savedInstanceState == null && getCountMonthlyReading() != 0) {
            cbChangeMeter.setChecked(shPAddEditMonthlyReading.get(ARG_FIRST_READING_MONTHLY_READING, false));
        }

        //listener pro výběr ceníku
        getParentFragmentManager().setFragmentResultListener(PriceListFragment.FLAG_PRICE_LIST_FRAGMENT, this, (requestKey, result) -> {
            selectedPriceList = (PriceListModel) result.getSerializable(PriceListFragment.FLAG_RESULT_PRICE_LIST_FRAGMENT);
            if (selectedPriceList != null) {
                btnSelectPriceList.setText(selectedPriceList.getName());
                viewModel.setSelectedPriceList(selectedPriceList);
                shPAddEditMonthlyReading.set(ARG_LAST_ID_SELECTED_PRICE_LIST, selectedPriceList.getId());
            } else {
                btnSelectPriceList.setText(getResources().getString(R.string.vyberCenik));
            }
        });
    }


    @Override
    public void onResume() {
        super.onResume();
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
            try {
                int dayOfMonth = Integer.parseInt(date);
                calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
            } catch (Exception e) {
                Logger.getLogger(TAG).warning("Nepodařilo se převést datum platby na číslo.");
            }

            String sb = getResources().getString(R.string.result_date) +
                    ViewHelper.getSimpleDateFormat().format(calendar.getTime());
            tvResultDate.setText(sb);
            datePayment = calendar.getTimeInMillis();
        }
    }


    /**
     * Metoda, která se volá při dostupnosti síťového připojení.
     * <p>
     * Tato metoda nastaví příznak `internetAvailable` na `true` a aktualizuje zobrazení
     * checkboxu pro odeslání zálohy.
     */
    @Override
    public void onNetworkAvailable() {
        internetAvailable = true;
        setShowCbSendBackup();
    }


    /**
     * Metoda, která se volá při ztrátě síťového připojení.
     * <p>
     * Tato metoda nastaví příznak `internetAvailable` na `false` a aktualizuje zobrazení
     * checkboxu pro odeslání zálohy.
     */
    @Override
    public void onNetworkLost() {
        internetAvailable = false;
        setShowCbSendBackup();
    }

}