package cz.xlisto.odecty.modules.monthlyreading;

import android.os.Bundle;
import android.transition.TransitionManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;


import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import cz.xlisto.odecty.R;
import cz.xlisto.odecty.databaze.DataMonthlyReadingSource;
import cz.xlisto.odecty.models.MonthlyReadingModel;
import cz.xlisto.odecty.models.PaymentModel;
import cz.xlisto.odecty.models.SubscriptionPointModel;
import cz.xlisto.odecty.modules.backup.SaveDataToBackupFile;
import cz.xlisto.odecty.modules.invoice.WithOutInvoiceService;
import cz.xlisto.odecty.ownview.LabelEditText;
import cz.xlisto.odecty.modules.pricelist.PriceListFragment;
import cz.xlisto.odecty.models.PriceListModel;
import cz.xlisto.odecty.ownview.ViewHelper;
import cz.xlisto.odecty.shp.ShPAddEditMonthlyReading;
import cz.xlisto.odecty.shp.ShPInvoice;
import cz.xlisto.odecty.utils.FragmentChange;
import cz.xlisto.odecty.utils.Keyboard;
import cz.xlisto.odecty.utils.SubscriptionPoint;

import static cz.xlisto.odecty.ownview.OwnDatePicker.showDialog;
import static cz.xlisto.odecty.ownview.ViewHelper.parseCalendarFromString;
import static cz.xlisto.odecty.shp.ShPAddEditMonthlyReading.ARG_DATE_MONTHLY_READING;
import static cz.xlisto.odecty.shp.ShPAddEditMonthlyReading.ARG_SHOW_DESCRIPTION_MONTHLY_READING;
import static cz.xlisto.odecty.shp.ShPAddEditMonthlyReading.ARG_FIRST_READING_MONTHLY_READING;
import static cz.xlisto.odecty.shp.ShPAddEditMonthlyReading.ARG_NT_MONTHLY_READING;
import static cz.xlisto.odecty.shp.ShPAddEditMonthlyReading.ARG_OTHER_MONTHLY_READING;
import static cz.xlisto.odecty.shp.ShPAddEditMonthlyReading.ARG_PAYMENT_MONTHLY_READING;
import static cz.xlisto.odecty.shp.ShPAddEditMonthlyReading.ARG_VT_MONTHLY_READING;
import static cz.xlisto.odecty.utils.FragmentChange.Transaction.MOVE;


/**
 * Abstraktní třída pro přidání a editaci měsíčního odečtu.
 */
public abstract class MonthlyReadingAddEditFragmentAbstract extends Fragment {
    private final String TAG = "MonthlyReadingAddEditFragmentAbstract";
    Button btnBack, btnSave, btnDate, btnSelectPriceList;
    LabelEditText labVT, labNT, labPayment, labDescription, labOtherServices;
    CheckBox cbAddPayment, cbFirstReading, cbShowDescription, cbAddBackup;
    EditText etDatePayment;
    TextView tvContentAddPayment, tvResultDate;
    static PriceListModel selectedPriceList;
    SubscriptionPointModel subscriptionPoint;
    RelativeLayout rlRoot;
    ShPAddEditMonthlyReading shPAddEditMonthlyReading;
    long selectedIdPriceList = -1L;
    boolean isFirstLoad = true;
    private final String ARG_IS_FIRST_LOAD = "isFirstLoad";
    private final String ARG_DATE = "date";
    private final String ARG_VT = "vt";
    private final String ARG_NT = "nt";
    private final String ARG_PAYMENT = "payment";
    private final String ARG_DESCRIPTION = "description";
    private final String ARG_OTHER_SERVICE = "otherServices";
    private final String ARG_SHOW_DESCRIPTION = "showDescription";
    private final String ARG_SELECTED_ID_PRICE_LIST = "selectedIdPriceList";
    private final String ARG_BTN_PRICE_LIST = "btnPriceList";
    private static boolean restoreSharedPreferences = false;
    private boolean isShowFragment;
    int countMonthlyReading = 0;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_monthly_reading_add_edit, container, false);
    }


    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        requireActivity().invalidateOptionsMenu();
        shPAddEditMonthlyReading = new ShPAddEditMonthlyReading(requireActivity());

        isShowFragment = true;
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
        cbAddBackup = view.findViewById(R.id.cbAddBackup);
        cbShowDescription = view.findViewById(R.id.cbShowDescription);
        etDatePayment = view.findViewById(R.id.etDatePayment);
        tvContentAddPayment = view.findViewById(R.id.tvContentAddPayment);
        tvResultDate = view.findViewById(R.id.tvResultDate);
        rlRoot = view.findViewById(R.id.rlRoot);


        cbShowDescription.setChecked(shPAddEditMonthlyReading.get(ARG_SHOW_DESCRIPTION, false));
        cbFirstReading.setChecked(shPAddEditMonthlyReading.get(ARG_FIRST_READING_MONTHLY_READING, false));

        cbFirstReading.setOnCheckedChangeListener((buttonView, isChecked) -> hideItemsForFirstReading(isChecked));

        cbShowDescription.setOnCheckedChangeListener((buttonView, isChecked) -> {
            shPAddEditMonthlyReading.set(ARG_SHOW_DESCRIPTION, cbShowDescription.isChecked());
            setShowDescription();
        });


        btnDate.setOnClickListener(v -> showDialog(getActivity(), day -> {
            btnDate.setText(day);
            onResume();
        }, btnDate.getText().toString()));

        btnSelectPriceList.setOnClickListener(v -> {
            saveSharedPreferences();

            PriceListFragment priceListFragment = PriceListFragment.newInstance(true, selectedIdPriceList);

            FragmentChange.replace(requireActivity(), priceListFragment, MOVE, true);
        });

        btnBack.setOnClickListener(v -> {
            Keyboard.hide(requireActivity());
            getParentFragmentManager().popBackStack();
        });


        if (savedInstanceState != null) {
            isFirstLoad = savedInstanceState.getBoolean(ARG_IS_FIRST_LOAD);
            btnDate.setText(savedInstanceState.getString(ARG_DATE, ""));
            labVT.setDefaultText(savedInstanceState.getString(ARG_VT, "0"));
            labNT.setDefaultText(savedInstanceState.getString(ARG_NT, "0"));
            labPayment.setDefaultText(savedInstanceState.getString(ARG_PAYMENT, "0"));
            labDescription.setDefaultText(savedInstanceState.getString(ARG_DESCRIPTION, ""));
            labOtherServices.setDefaultText(savedInstanceState.getString(ARG_OTHER_SERVICE, ""));
            selectedIdPriceList = savedInstanceState.getLong(ARG_SELECTED_ID_PRICE_LIST);
            btnSelectPriceList.setText(savedInstanceState.getString(ARG_BTN_PRICE_LIST));
        }

        //zjistí počet záznamů v měsíčním odečtu
        DataMonthlyReadingSource dataMonthlyReadingSource = new DataMonthlyReadingSource(requireContext());
        dataMonthlyReadingSource.open();
        countMonthlyReading = dataMonthlyReadingSource.getCount(subscriptionPoint.getTableO());
        dataMonthlyReadingSource.close();
        hideItemsForFirstReading(countMonthlyReading == 0);
        if (countMonthlyReading == 0) {
            cbFirstReading.setVisibility(View.GONE);
        }

        hideItemsForFirstReading(cbFirstReading.isChecked());
    }


    @Override
    public void onResume() {
        super.onResume();

        if (isFirstLoad) {
            isFirstLoad = false;
        }

        if (restoreSharedPreferences)
            restoreSharedPreferences();

        setShowDescription();
        setShowAddPayment();
    }


    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(ARG_IS_FIRST_LOAD, isFirstLoad);
        outState.putLong(ARG_SELECTED_ID_PRICE_LIST, selectedIdPriceList);
        if (isShowFragment) {
            outState.putString(ARG_DATE, btnDate.getText().toString());
            outState.putString(ARG_VT, labVT.getText());
            outState.putString(ARG_NT, labNT.getText());
            outState.putString(ARG_PAYMENT, labPayment.getText());
            outState.putString(ARG_DESCRIPTION, labDescription.getText());
            outState.putString(ARG_OTHER_SERVICE, labOtherServices.getText());
            outState.putString(ARG_BTN_PRICE_LIST, btnSelectPriceList.getText().toString());
        }
    }


    /**
     * Sestaví objekt odběrného místa z údajů widgetů
     *
     * @return MonthlyReadingModel Objekt měsíčního odečtu
     */
    MonthlyReadingModel createMonthlyReading() {
        long date = parseCalendarFromString(btnDate.getText().toString()).getTimeInMillis();
        date -= ViewHelper.getOffsetTimeZones(date);
        return new MonthlyReadingModel(date,
                labVT.getDouble(), labNT.getDouble(), labPayment.getDouble(),
                labDescription.getText(), labOtherServices.getDouble(),
                selectedIdPriceList, cbFirstReading.isChecked());
    }


    /**
     * Sestaví objekt platby z údajů widgetů
     *
     * @return PaymentModel Objekt platby
     */
    PaymentModel createPayment(long date) {
        date -= ViewHelper.getOffsetTimeZones(date);
        return new PaymentModel(-1L, -1L, date, labPayment.getDouble(), 2);
    }


    /**
     * Uloží hodnoty widgetů do sharedprefences
     */
    void saveSharedPreferences() {
        shPAddEditMonthlyReading.set(ARG_VT_MONTHLY_READING, labVT.getText());
        shPAddEditMonthlyReading.set(ARG_NT_MONTHLY_READING, labNT.getText());
        shPAddEditMonthlyReading.set(ARG_DESCRIPTION, labDescription.getText());
        shPAddEditMonthlyReading.set(ARG_PAYMENT_MONTHLY_READING, labPayment.getText());
        shPAddEditMonthlyReading.set(ARG_OTHER_MONTHLY_READING, labOtherServices.getText());
        shPAddEditMonthlyReading.set(ARG_DATE_MONTHLY_READING, btnDate.getText().toString());
        //shPAddEditMonthlyReading.set(ARG_BTN_PRICE_LIST, btnSelectPriceList.getText().toString());
        restoreSharedPreferences = true;
    }


    /**
     * Obnoví hodnoty widgetů ze sharedprefences
     */
    void restoreSharedPreferences() {
        labVT.setDefaultText(shPAddEditMonthlyReading.get(ARG_VT_MONTHLY_READING, ""));
        labNT.setDefaultText(shPAddEditMonthlyReading.get(ARG_NT_MONTHLY_READING, ""));
        labDescription.setDefaultText(shPAddEditMonthlyReading.get(ARG_SHOW_DESCRIPTION_MONTHLY_READING, ""));
        labPayment.setDefaultText(shPAddEditMonthlyReading.get(ARG_PAYMENT_MONTHLY_READING, ""));
        labOtherServices.setDefaultText(shPAddEditMonthlyReading.get(ARG_OTHER_MONTHLY_READING, ""));
        btnDate.setText(shPAddEditMonthlyReading.get(ARG_DATE_MONTHLY_READING, ""));
        //btnSelectPriceList.setText(shPAddEditMonthlyReading.get(ARG_BTN_PRICE_LIST, ""));
        restoreSharedPreferences = false;
    }


    /**
     * Nastaví zaškrtnutí checkboxu "Přidat platbu" a zobrazí/skryje edittext pro zadání data platby
     */
    void setShowAddPayment() {
        TransitionManager.beginDelayedTransition(rlRoot);
        if (cbAddPayment.isChecked()) {
            etDatePayment.setVisibility(View.VISIBLE);
            tvContentAddPayment.setVisibility(View.VISIBLE);
            tvResultDate.setVisibility(View.VISIBLE);
        } else {
            etDatePayment.setVisibility(View.GONE);
            tvContentAddPayment.setVisibility(View.GONE);
            tvResultDate.setVisibility(View.GONE);
        }
    }


    /**
     * Nastaví zaškrtnutí checkboxu "Zobrazit popis" a zobrazí/skryje labeledity pro zadání poznámky a další doplňkové služby
     */
    void setShowDescription() {
        TransitionManager.beginDelayedTransition(rlRoot);
        if (cbShowDescription.isChecked()) {
            labDescription.setVisibility(View.VISIBLE);
            labOtherServices.setVisibility(View.VISIBLE);
        } else {
            labDescription.setVisibility(View.GONE);
            labOtherServices.setVisibility(View.GONE);
        }
    }


    /**
     * Upraví poslední záznam v období bez faktury, navazující na měsíční odečet. Vloží další záznam do faktury při výměně elektroměru.
     *
     * @param lastMonthlyReading - poslední záznam měsíčního odečtu podle data
     */
    void updateItemInvoice(MonthlyReadingModel lastMonthlyReading) {
        ShPInvoice shPInvoice = new ShPInvoice(requireActivity());
        if (shPInvoice.get(ShPInvoice.AUTO_GENERATE_INVOICE, true))
            WithOutInvoiceService.updateAllItemsInvoice(requireActivity(), subscriptionPoint.getTableTED(), subscriptionPoint.getTableFAK(), subscriptionPoint.getTableO());
        else
            WithOutInvoiceService.editLastItemInInvoice(requireActivity(), subscriptionPoint.getTableTED(), lastMonthlyReading);
    }


    /**
     * Vytvoří zálohu měsíčního odečtu
     */
    void backupMonthlyReading() {
        SaveDataToBackupFile.saveToZip(requireActivity(), null);
    }


    /**
     * Skryje/zobrazí widgety pro první odečet
     *
     * @param isChecked Zaškrtnutí checkboxu "První odečet"
     */
    void hideItemsForFirstReading(boolean isChecked) {
        labPayment.setEnabled(!isChecked);
        labOtherServices.setEnabled(!isChecked);
        cbAddPayment.setEnabled(!isChecked);
        if (isChecked)
            btnSelectPriceList.setVisibility(View.GONE);
        else
            btnSelectPriceList.setVisibility(View.VISIBLE);
        cbAddPayment.setChecked(false);
        shPAddEditMonthlyReading.set(ARG_FIRST_READING_MONTHLY_READING, cbFirstReading.isChecked());
    }


    @Override
    public void onDestroyView() {
        super.onDestroyView();
        isShowFragment = false;
    }
}
