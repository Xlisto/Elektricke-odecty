package cz.xlisto.cenik.modules.monthlyreading;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import cz.xlisto.cenik.MainActivity;
import cz.xlisto.cenik.R;
import cz.xlisto.cenik.models.MonthlyReadingModel;
import cz.xlisto.cenik.models.SubscriptionPointModel;
import cz.xlisto.cenik.modules.invoice.WithOutInvoiceService;
import cz.xlisto.cenik.ownview.LabelEditText;
import cz.xlisto.cenik.modules.pricelist.PriceListFragment;
import cz.xlisto.cenik.models.PriceListModel;
import cz.xlisto.cenik.shp.ShPAddEditMonthlyReading;
import cz.xlisto.cenik.utils.FragmentChange;
import cz.xlisto.cenik.utils.SubscriptionPoint;

import static cz.xlisto.cenik.ownview.OwnDatePicker.showDialog;
import static cz.xlisto.cenik.ownview.ViewHelper.parseCalendarFromString;
import static cz.xlisto.cenik.shp.ShPAddEditMonthlyReading.ADD_PAYMENT_MONTHLY_READING;
import static cz.xlisto.cenik.shp.ShPAddEditMonthlyReading.DATE_MONTHLY_READING;
import static cz.xlisto.cenik.shp.ShPAddEditMonthlyReading.DESCRIPTION_MONTHLY_READING;
import static cz.xlisto.cenik.shp.ShPAddEditMonthlyReading.FIRS_READING_MONTHLY_READING;
import static cz.xlisto.cenik.shp.ShPAddEditMonthlyReading.NT_MONTHLY_READING;
import static cz.xlisto.cenik.shp.ShPAddEditMonthlyReading.OTHER_MONTHLY_READING;
import static cz.xlisto.cenik.shp.ShPAddEditMonthlyReading.PAYMENT_MONTHLY_READING;
import static cz.xlisto.cenik.shp.ShPAddEditMonthlyReading.VT_MONTHLY_READING;
import static cz.xlisto.cenik.utils.FragmentChange.Transaction.MOVE;

public abstract class MonthlyReadingAddEditFragmentAbstract extends Fragment {
    private final String TAG = getClass().getName() + " ";
    Button btnBack, btnSave, btnDate, btnSelectPriceList;
    LabelEditText labVT, labNT, labPayment,labDescription, labOtherServices;
    CheckBox cbAddPayment, cbFirstReading;
    PriceListModel selectedPriceList;
    SubscriptionPointModel subscriptionPoint;
    ShPAddEditMonthlyReading shPAddEditMonthlyReading;
    boolean isFirstLoad = true;
    private final String IS_FIRST_LOAD = "isFirstLoad";
    private final String DATE = "date";
    private final String VT = "vt";
    private final String NT = "nt";
    private final String PAYMENT = "payment";
    private final String DESCRIPTION = "description";
    private final String OTHER_SERVICE = "other_services";
    private final String ADD_PAYMENT = "add_payment";
    private final String FIRST_READING = "first_reading";

    private final String SELECTED_ID_PRICELIST = "selectedIdPriceList";
    private final String BTN_PRICELIST = "btn_pricelist";

    private static boolean restoreSharedPreferences = false;
    long selectedIdPriceList = -1L;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_monthly_reading_add_edit, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        shPAddEditMonthlyReading = new ShPAddEditMonthlyReading(getActivity());
        subscriptionPoint = SubscriptionPoint.load(getActivity());
        btnSave = view.findViewById(R.id.btnSaveMonthlyReading);
        btnBack = view.findViewById(R.id.btnBackMonthlyReading);
        btnDate = view.findViewById(R.id.btnDate);
        btnSelectPriceList = view.findViewById(R.id.btnSelectPriceList);
        labVT = view.findViewById(R.id.labVT);
        labNT = view.findViewById(R.id.labNT);
        labPayment = view.findViewById(R.id.labPayment);
        labDescription = view.findViewById(R.id.labDescription);
        labOtherServices = view.findViewById(R.id.labOtherServices);
        cbAddPayment = view.findViewById(R.id.cbAddPayment);
        cbFirstReading = view.findViewById(R.id.cbFirstReading);

        cbFirstReading.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    labPayment.setEnabled(!isChecked);
                    labOtherServices.setEnabled(!isChecked);
                    cbAddPayment.setEnabled(!isChecked);
                    if(isChecked)
                        btnSelectPriceList.setVisibility(View.GONE);
                    else
                        btnSelectPriceList.setVisibility(View.VISIBLE);
            }
        });

        btnDate.setOnClickListener(v -> showDialog(getActivity(), day -> {
            btnDate.setText(day);
            onResume();
        }, btnDate.getText().toString()));

        btnSelectPriceList.setOnClickListener(v -> {
            saveSharedPreferences();
            restoreSharedPreferences = true;

            PriceListFragment priceListFragment = PriceListFragment.newInstance(true, selectedIdPriceList);
            priceListFragment.setOnSelectedPriceListListener(new PriceListFragment.OnSelectedPriceList() {
                @Override
                public void getOnSelectedItemPriceList(PriceListModel priceList) {
                    selectedPriceList = priceList;
                    selectedIdPriceList = selectedPriceList.getId();
                }
            });
            
            FragmentChange.replace(getActivity(), priceListFragment,MOVE, true);
        });

        btnBack.setOnClickListener(v -> getParentFragmentManager().popBackStack());
        if (savedInstanceState != null) {
            isFirstLoad = savedInstanceState.getBoolean(IS_FIRST_LOAD);
            btnDate.setText(savedInstanceState.getString(DATE, ""));
            labVT.setDefaultText(savedInstanceState.getString(VT, "0"));
            labNT.setDefaultText(savedInstanceState.getString(NT, "0"));
            labPayment.setDefaultText(savedInstanceState.getString(PAYMENT, "0"));
            labDescription.setDefaultText(savedInstanceState.getString(DESCRIPTION, ""));
            labOtherServices.setDefaultText(savedInstanceState.getString(OTHER_SERVICE, ""));
            cbAddPayment.setChecked(savedInstanceState.getBoolean(ADD_PAYMENT, false));
            cbFirstReading.setChecked(savedInstanceState.getBoolean(FIRST_READING, false));
            selectedIdPriceList = savedInstanceState.getLong(SELECTED_ID_PRICELIST);
            btnSelectPriceList.setText(savedInstanceState.getString(BTN_PRICELIST));
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        if (selectedPriceList != null) {
            btnSelectPriceList.setText(selectedPriceList.getName());
            selectedIdPriceList = selectedPriceList.getId();
        }
        if (isFirstLoad) {
            isFirstLoad = false;
        }

        if(restoreSharedPreferences)
            restoreSharedPreferences();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(IS_FIRST_LOAD, isFirstLoad);
        outState.putString(DATE, btnDate.getText().toString());
        outState.putString(VT, labVT.getText());
        outState.putString(NT, labNT.getText());
        outState.putString(PAYMENT, labPayment.getText());
        outState.putString(DESCRIPTION, labDescription.getText());
        outState.putString(OTHER_SERVICE, labOtherServices.getText());
        outState.putBoolean(ADD_PAYMENT, cbAddPayment.isChecked());
        outState.putBoolean(FIRST_READING, cbFirstReading.isChecked());
        outState.putLong(SELECTED_ID_PRICELIST, selectedIdPriceList);
        outState.putString(BTN_PRICELIST, btnSelectPriceList.getText().toString());
    }

    /**
     * Sestaví objekt odběrného místa z údajů widgetů
     * @return
     */
    MonthlyReadingModel createMonthlyReading(){
        long date = parseCalendarFromString(btnDate.getText().toString()).getTimeInMillis();
        MonthlyReadingModel monthlyReading = new MonthlyReadingModel(date,
                labVT.getDouble(),labNT.getDouble(), labPayment.getDouble(),
                labDescription.getText(),labOtherServices.getDouble(),
                selectedIdPriceList,cbFirstReading.isChecked());
        return monthlyReading;
    }

    /**
     * Uloží hodnoty widgetů do sharedprefences
     */
    void saveSharedPreferences(){
        shPAddEditMonthlyReading.set(VT_MONTHLY_READING,labVT.getText());
        shPAddEditMonthlyReading.set(NT_MONTHLY_READING,labNT.getText());
        shPAddEditMonthlyReading.set(DESCRIPTION_MONTHLY_READING,labDescription.getText());
        shPAddEditMonthlyReading.set(PAYMENT_MONTHLY_READING,labPayment.getText());
        shPAddEditMonthlyReading.set(OTHER_MONTHLY_READING,labOtherServices.getText());
        shPAddEditMonthlyReading.set(ADD_PAYMENT_MONTHLY_READING,cbAddPayment.isChecked());
        shPAddEditMonthlyReading.set(FIRS_READING_MONTHLY_READING,cbFirstReading.isChecked());
        shPAddEditMonthlyReading.set(DATE_MONTHLY_READING,btnDate.getText().toString());
        restoreSharedPreferences = true;
    }

    /**
     * Obnoví hodnoty widgetů ze sharedprefences
     */
    void restoreSharedPreferences(){
        labVT.setDefaultText(shPAddEditMonthlyReading.get(VT_MONTHLY_READING,""));
        labNT.setDefaultText(shPAddEditMonthlyReading.get(NT_MONTHLY_READING,""));
        labDescription.setDefaultText(shPAddEditMonthlyReading.get(DESCRIPTION_MONTHLY_READING,""));
        labPayment.setDefaultText(shPAddEditMonthlyReading.get(PAYMENT_MONTHLY_READING,""));
        labOtherServices.setDefaultText(shPAddEditMonthlyReading.get(OTHER_MONTHLY_READING,""));
        cbAddPayment.setChecked(shPAddEditMonthlyReading.get(ADD_PAYMENT_MONTHLY_READING,false));
        cbFirstReading.setChecked(shPAddEditMonthlyReading.get(FIRS_READING_MONTHLY_READING,false));
        btnDate.setText(shPAddEditMonthlyReading.get(DATE_MONTHLY_READING,""));
        restoreSharedPreferences = false;
    }

    /**
     * Upraví poslední záznam v období bez faktury, navazující na měsíční odečet
     * @param monthlyReading
     */
    void updateLastItemInvoice(){
        WithOutInvoiceService.editLastItemInInvoice(getActivity(), subscriptionPoint.getTableTED(), createMonthlyReading());
    }
}
