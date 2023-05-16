package cz.xlisto.odecty.modules.payment;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.DatePicker;

import cz.xlisto.odecty.R;
import cz.xlisto.odecty.models.PaymentModel;
import cz.xlisto.odecty.ownview.LabelEditText;
import cz.xlisto.odecty.ownview.ViewHelper;
import cz.xlisto.odecty.utils.Keyboard;

/**
 * Abstraktní třída pro přidání a editaci platby.
 */
public abstract class PaymentAddEditFragmentAbstract extends Fragment {
    private static final String TAG = "PaymentAddEditFragmentAbstract";
    public static final String FLAG_RESULT_PAYMENT_FRAGMENT = "flagResultPaymentFragment";
    static final String ID_FAK = "id_fak";
    static final String ID_PAYMENT = "id_payment";
    static final String TABLE = "table";
    static final String LAB_PAYMENT = "lab_payment";
    static final String CH_PAYMENT = "payment";
    static final String CH_SUPPLEMENT = "supplement";
    static final String CH_DISCOUNT = "discount";
    static final String DP_DAY = "dp_day";
    static final String DP_MONTH = "dp_month";
    static final String DP_YEAR = "dp_year";
    long idFak, idPayment;
    String table;
    Button btnSave, btnBack;
    LabelEditText labPayment;
    CheckBox chPayment, chSupplement, chDiscount;
    DatePicker dp;
    PaymentModel payment;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            idFak = getArguments().getLong(ID_FAK,-2L);
            table = getArguments().getString(TABLE);
            idPayment = getArguments().getLong(ID_PAYMENT,-1L);
        }
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_payment_add_edit_abstract, container, false);
    }


    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        labPayment = view.findViewById(R.id.labPayment);
        chPayment = view.findViewById(R.id.cbPayment);
        chSupplement = view.findViewById(R.id.cbSupplement);
        chDiscount = view.findViewById(R.id.cbDiscount);
        btnSave = view.findViewById(R.id.btnSavePayment);
        btnBack = view.findViewById(R.id.btnBackPayment);
        dp = view.findViewById(R.id.dpPayment);

        btnBack.setOnClickListener(v -> {
            Keyboard.hide(requireActivity());
            closeFragment();
        });

        btnSave.setOnClickListener(v -> {
            Keyboard.hide(requireActivity());
            save();
        });

        chPayment.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                unCheck();
                chPayment.setChecked(true);
            }
        });

        chSupplement.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                unCheck();
                chSupplement.setChecked(true);
            }
        });

        chDiscount.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                unCheck();
                chDiscount.setChecked(true);
            }
        });

        if (savedInstanceState != null) {
            labPayment.setDefaultText(savedInstanceState.getString(LAB_PAYMENT));
            chPayment.setChecked(savedInstanceState.getBoolean(CH_PAYMENT));
            chSupplement.setChecked(savedInstanceState.getBoolean(CH_SUPPLEMENT));
            chDiscount.setChecked(savedInstanceState.getBoolean(CH_DISCOUNT));
            int year = savedInstanceState.getInt(DP_YEAR);
            int month = savedInstanceState.getInt(DP_MONTH);
            int day = savedInstanceState.getInt(DP_DAY);
            dp.updateDate(year, month, day);
        }
    }


    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(LAB_PAYMENT, labPayment.getText());
        outState.putBoolean(CH_PAYMENT, chPayment.isChecked());
        outState.putBoolean(CH_SUPPLEMENT, chSupplement.isChecked());
        outState.putBoolean(CH_DISCOUNT, chDiscount.isChecked());
        outState.putInt(DP_DAY, dp.getDayOfMonth());
        outState.putInt(DP_MONTH, dp.getMonth());
        outState.putInt(DP_YEAR, dp.getYear());
    }


    private void unCheck() {
        chPayment.setChecked(false);
        chSupplement.setChecked(false);
        chDiscount.setChecked(false);
    }


    void save() {
        long date = ViewHelper.parseCalendarFromString(""+dp.getDayOfMonth()+"."+(dp.getMonth()+1)+"."+dp.getYear()).getTimeInMillis();
        int typePayment = getTypePayment();
        //TODO: doplnit typ platby
        payment = new PaymentModel(idPayment,idFak,date, labPayment.getDouble(), typePayment);

        getParentFragmentManager().setFragmentResult(FLAG_RESULT_PAYMENT_FRAGMENT, new Bundle());
    }


    void closeFragment() {
        getParentFragmentManager().popBackStack();
    }

    /**
     * Druh platby
     *
     * @return 0 - platba, 1 - doplatek, 3 - sleva
     */
    int getTypePayment() {
        if (chSupplement.isChecked())
            return 1;
        if (chDiscount.isChecked())
            return 3;
        return 0;
    }
}