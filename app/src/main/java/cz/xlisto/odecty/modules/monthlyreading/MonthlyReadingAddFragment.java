package cz.xlisto.odecty.modules.monthlyreading;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import cz.xlisto.odecty.R;
import cz.xlisto.odecty.databaze.DataSubscriptionPointSource;
import cz.xlisto.odecty.ownview.ViewHelper;
import cz.xlisto.odecty.utils.Keyboard;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Toast;

import java.util.Calendar;

import static cz.xlisto.odecty.shp.ShPAddEditMonthlyReading.ARG_SHOW_ADD_PAYMENT_MONTHLY_READING;
import static cz.xlisto.odecty.shp.ShPMonthlyReading.ADD_BACKUP_NEW_READING;

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
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param tableO        Jméno databáze měsíčních odečtů (O).
     * @param tablePayments Jméno databáze plateb (PLATBY).
     * @return A new instance of fragment MonthlyReadingAddFragment.
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
        if (isFirstLoad)
            btnDate.setText(ViewHelper.getTodayDate());

        btnSave.setOnClickListener(v -> {
            if (selectedIdPriceList > 0 || cbFirstReading.isChecked()) {
                DataSubscriptionPointSource dataSubscriptionPointSource = new DataSubscriptionPointSource(getContext());
                dataSubscriptionPointSource.open();
                dataSubscriptionPointSource.insertMonthlyReading(tableO, createMonthlyReading());
                //přidat platbu: true; první odečet: false
                if (cbAddPayment.isChecked() && !cbFirstReading.isChecked()) {
                    dataSubscriptionPointSource.insertPayment(tablePayments, createPayment(datePayment));
                }
                dataSubscriptionPointSource.close();
                updateLastItemInvoice();
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