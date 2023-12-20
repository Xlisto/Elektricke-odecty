package cz.xlisto.odecty.modules.invoice;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Toast;


import java.util.Calendar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import cz.xlisto.odecty.R;
import cz.xlisto.odecty.models.InvoiceModel;
import cz.xlisto.odecty.models.PriceListModel;
import cz.xlisto.odecty.modules.pricelist.PriceListFragment;
import cz.xlisto.odecty.ownview.LabelEditText;
import cz.xlisto.odecty.ownview.OwnDatePicker;
import cz.xlisto.odecty.ownview.ViewHelper;
import cz.xlisto.odecty.shp.ShPAddEditInvoice;
import cz.xlisto.odecty.utils.FragmentChange;
import cz.xlisto.odecty.utils.Keyboard;

import static cz.xlisto.odecty.utils.FragmentChange.Transaction.MOVE;

/**
 * Abstraktní třída pro třídy přidání/úpravy položek faktury.
 * Xlisto 04.02.2023 11:58
 */
public abstract class InvoiceAddEditAbstractFragment extends Fragment {
    private static final String TAG = "InvoiceAddEditAbstractFragment";
    static final String TABLE = "table";
    static final String ID_FAK = "idFak";
    static final String ID = "id";
    static final String BTN_DATE_OF = "btnDate1";
    static final String BTN_DATE_TO = "btnDate2";
    static final String VT_START = "vtStart";
    static final String NT_START = "ntStart";
    static final String VT_END = "vtEnd";
    static final String NT_END = "ntEnd";
    static final String OTHER_SERVICES = "other";
    static final String SELECTED_ID_PRICE = "selectedIdPrice";
    static final String SELECTED_ID_INVOICE = "selectedIdInvoice";
    static final String IS_CHANGED_ELECTROMETER = "isChangedElectrometer";
    static boolean loadFromDatabase = true;
    private PriceListModel selectedPrice;
    long selectedIdPrice = -1L;
    long selectedIdInvoice = -1L;
    long id = -1L;
    Button btnDateStart, btnDateEnd, btnSelectPriceList, btnBack, btnSave;
    LabelEditText letVTStart, letVTEnd, letNTStart, letNTEnd, letOtherServices;
    String numberInvoice, table;
    CheckBox chIsChangedElectricMeter;
    String oldDateStart = "1.1.2023";
    String oldDateEnd = "31.12.2023";


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            table = getArguments().getString(TABLE);
            selectedIdInvoice = getArguments().getLong(ID_FAK);
            id = getArguments().getLong(ID);
        }
        Calendar calendar = Calendar.getInstance();
        oldDateStart = "1.1."+calendar.get(Calendar.YEAR);
        oldDateEnd = "31.12."+calendar.get(Calendar.YEAR);
    }


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_invoice_add_edit, container, false);
    }


    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        btnDateStart = view.findViewById(R.id.btnDateStart);
        btnDateEnd = view.findViewById(R.id.btnDateEnd);
        btnSelectPriceList = view.findViewById(R.id.btnSelectPriceList);
        btnSave = view.findViewById(R.id.btnSaveInvoice);
        btnBack = view.findViewById(R.id.btnBackInvoice);
        letVTStart = view.findViewById(R.id.letVTStart);
        letVTEnd = view.findViewById(R.id.letVTEnd);
        letNTStart = view.findViewById(R.id.letNTStart);
        letNTEnd = view.findViewById(R.id.letNTEnd);
        letOtherServices = view.findViewById(R.id.letOtherServices);
        chIsChangedElectricMeter = view.findViewById(R.id.cbIsChangedElectrometer);

        btnDateStart.setOnClickListener(v -> OwnDatePicker.showDialog(getActivity(), date -> btnDateStart.setText(date), oldDateStart));

        btnDateEnd.setOnClickListener(v -> OwnDatePicker.showDialog(getActivity(), date -> btnDateEnd.setText(date), oldDateEnd));

        btnSelectPriceList.setOnClickListener(v -> {
            saveSharedPreferences();
            PriceListFragment priceListFragment = PriceListFragment.newInstance(true, selectedIdPrice);
            priceListFragment.setOnSelectedPriceListListener(priceList -> {
                if(priceList == null) {
                    Toast.makeText(getActivity(), "Cena nebyla vybrána", Toast.LENGTH_SHORT).show();
                    return;
                }
                selectedPrice = priceList;
                selectedIdPrice = selectedPrice.getId();
                deactivateNT(selectedPrice.getSazba().equals(InvoiceAbstract.D01) || selectedPrice.getSazba().equals(InvoiceAbstract.D02));

            });
            Keyboard.hide(requireActivity());
            FragmentChange.replace(requireActivity(), priceListFragment, MOVE, true);
        });

        btnBack.setOnClickListener(v -> getParentFragmentManager().popBackStack());


        loadSharedPreferences();

        if (savedInstanceState != null) {
            btnDateStart.setText(savedInstanceState.getString(BTN_DATE_OF));
            btnDateEnd.setText(savedInstanceState.getString(BTN_DATE_TO));
            letVTStart.setDefaultText(savedInstanceState.getString(VT_START));
            letNTStart.setDefaultText(savedInstanceState.getString(NT_START));
            letVTEnd.setDefaultText(savedInstanceState.getString(VT_END));
            letNTEnd.setDefaultText(savedInstanceState.getString(NT_END));
            letOtherServices.setDefaultText(savedInstanceState.getString(OTHER_SERVICES));
            selectedIdPrice = savedInstanceState.getLong(SELECTED_ID_PRICE, -1L);
            selectedIdInvoice = savedInstanceState.getLong(SELECTED_ID_INVOICE, -1L);
            chIsChangedElectricMeter.setChecked(savedInstanceState.getBoolean(IS_CHANGED_ELECTROMETER, false));
        }
    }


    @Override
    public void onResume() {
        super.onResume();
        if (selectedPrice != null) {
            selectedIdPrice = selectedPrice.getId();
            deactivateNT(selectedPrice.getSazba().equals(InvoiceAbstract.D01) || selectedPrice.getSazba().equals(InvoiceAbstract.D02));
            btnSelectPriceList.setText(selectedPrice.getName());
        }
    }


    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(BTN_DATE_OF, btnDateStart.getText().toString());
        outState.putString(BTN_DATE_TO, btnDateEnd.getText().toString());
        outState.putString(VT_START, letVTStart.getText());
        outState.putString(NT_START, letNTStart.getText());
        outState.putString(VT_END, letVTEnd.getText());
        outState.putString(NT_END, letNTEnd.getText());
        outState.putString(OTHER_SERVICES, letOtherServices.getText());
        outState.putLong(SELECTED_ID_PRICE, selectedIdPrice);
        outState.putLong(SELECTED_ID_INVOICE, selectedIdInvoice);
        outState.putBoolean(IS_CHANGED_ELECTROMETER, chIsChangedElectricMeter.isChecked());
    }

    @Override
    public void onStop() {
        super.onStop();
        Keyboard.hide(requireActivity());
        loadFromDatabase = true;
    }



    /**
     * Vytvoří objekt InvoiceModel - záznam faktury
     * @return InvoiceModel - jeden záznam faktury
     */
    InvoiceModel createInvoice() {
        ShPAddEditInvoice shPAddEditInvoice = new ShPAddEditInvoice(requireContext());
        selectedIdPrice = shPAddEditInvoice.get(ShPAddEditInvoice.SELECTED_ID_PRICE, -1L);
        long start = ViewHelper.parseCalendarFromString(btnDateStart.getText().toString()).getTimeInMillis();
        long end = ViewHelper.parseCalendarFromString(btnDateEnd.getText().toString()).getTimeInMillis();
        start -=  ViewHelper.getOffsetTimeZones(start);
        end -= ViewHelper.getOffsetTimeZones(end);
        return new InvoiceModel(start, end,
                letVTStart.getDouble(), letVTEnd.getDouble(),
                letNTStart.getDouble(), letNTEnd.getDouble(),
                selectedIdInvoice, selectedIdPrice,
                letOtherServices.getDouble(), numberInvoice,
                chIsChangedElectricMeter.isChecked()
        );
    }


    /**
     * Vytvoří objekt InvoiceModel - záznam faktury. Nastaví id a idPriceList - používá se při editaci
     * @param id long id faktury
     * @param idPriceList long id ceníku
     * @return InvoiceModel - jeden záznam faktury
     */
    InvoiceModel createInvoice(long id, long idPriceList) {
        InvoiceModel invoice = createInvoice();
        invoice.setId(id);
        invoice.setIdPriceList(idPriceList);
        return invoice;
    }


    void saveSharedPreferences() {
        ShPAddEditInvoice shPAddEditInvoice = new ShPAddEditInvoice(getContext());
        shPAddEditInvoice.set(ShPAddEditInvoice.LOAD_PREFERENCES, true);
        shPAddEditInvoice.set(ShPAddEditInvoice.BTN_DATE_OF, btnDateStart.getText().toString());
        shPAddEditInvoice.set(ShPAddEditInvoice.BTN_DATE_TO, btnDateEnd.getText().toString());
        shPAddEditInvoice.set(ShPAddEditInvoice.VT_START, letVTStart.getText());
        shPAddEditInvoice.set(ShPAddEditInvoice.NT_START, letNTStart.getText());
        shPAddEditInvoice.set(ShPAddEditInvoice.VT_END, letVTEnd.getText());
        shPAddEditInvoice.set(ShPAddEditInvoice.NT_END, letNTEnd.getText());
        shPAddEditInvoice.set(ShPAddEditInvoice.OTHER_SERVICES, letOtherServices.getText());
        shPAddEditInvoice.set(ShPAddEditInvoice.SELECTED_ID_PRICE, selectedIdPrice);
        shPAddEditInvoice.set(ShPAddEditInvoice.SELECTED_ID_INVOICE, selectedIdInvoice);
        shPAddEditInvoice.set(ShPAddEditInvoice.TABLE, table);
        shPAddEditInvoice.set(ShPAddEditInvoice.ID, id);
    }


    /**
     * Načte uložené hodnoty z SharedPreferences a nastaví je do odpovídajících polí.
     * Pokud bylo v SharedPreferences nastaveno načtení hodnot, provede se načtení a následné nastavení hodnot do polí.
     */
    void loadSharedPreferences() {
        ShPAddEditInvoice shPAddEditInvoice = new ShPAddEditInvoice(getContext());
        boolean loadPreferences = shPAddEditInvoice.get(ShPAddEditInvoice.LOAD_PREFERENCES, false);
        if (loadPreferences) {
            btnDateStart.setText(shPAddEditInvoice.get(ShPAddEditInvoice.BTN_DATE_OF, "Datum"));
            btnDateEnd.setText(shPAddEditInvoice.get(ShPAddEditInvoice.BTN_DATE_TO, "Datum"));
            letVTStart.setDefaultText(shPAddEditInvoice.get(ShPAddEditInvoice.VT_START, "0"));
            letNTStart.setDefaultText(shPAddEditInvoice.get(ShPAddEditInvoice.NT_START, "0"));
            letVTEnd.setDefaultText(shPAddEditInvoice.get(ShPAddEditInvoice.VT_END, "0"));
            letNTEnd.setDefaultText(shPAddEditInvoice.get(ShPAddEditInvoice.NT_END, "0"));
            letOtherServices.setDefaultText(shPAddEditInvoice.get(ShPAddEditInvoice.OTHER_SERVICES, "0"));
            selectedIdPrice = shPAddEditInvoice.get(ShPAddEditInvoice.SELECTED_ID_PRICE, -1L);
            selectedIdInvoice = shPAddEditInvoice.get(ShPAddEditInvoice.SELECTED_ID_INVOICE, -1L);
            id = shPAddEditInvoice.get(ShPAddEditInvoice.ID, -1L);
            table = shPAddEditInvoice.get(ShPAddEditInvoice.TABLE, "");
            shPAddEditInvoice.set(ShPAddEditInvoice.LOAD_PREFERENCES, false);
        }
    }


    /**
     * Kontroluje, zda jsou všechny potřebné údaje vyplněny pro vytvoření nového záznamu o tarifu.
     * Pokud některý údaj chybí, zobrazí se upozornění a funkce vrátí hodnotu true.
     * V opačném případě vrátí funkce hodnotu false.
     *
     * @return True, pokud nějaký údaj chybí, jinak false.
     */
    boolean checkData() {
        if (selectedIdInvoice == -2L) {
            Toast.makeText(getActivity(), "Není vybrána faktura", Toast.LENGTH_SHORT).show();
            return true;
        }
        if (selectedIdPrice == -1L) {
            Toast.makeText(getActivity(), "Není vybrán ceník", Toast.LENGTH_SHORT).show();
            return true;
        }

        if (btnDateStart.getText().toString().equals("Datum")) {
            Toast.makeText(getActivity(), "Není vybrán počáteční datum", Toast.LENGTH_SHORT).show();
            return true;
        }
        if (btnDateEnd.getText().toString().equals("Datum")) {
            Toast.makeText(getActivity(), "Není vybrán konečný datum", Toast.LENGTH_SHORT).show();
            return true;
        }
        return false;
    }


    /**
     * Deaktivuje nebo aktivuje prvky v adapterovém zobrazení položky, které slouží k zadání informací o nočním tarifu.
     *
     * @param deactivate Pokud je true, prvky budou deaktivovány, jinak budou aktivovány
     */
    void deactivateNT(boolean deactivate) {
        letNTStart.setEnabled(!deactivate);
        letNTEnd.setEnabled(!deactivate);
        if (deactivate) {
            letNTStart.setHintText("0");
            letNTEnd.setHintText("0");
            letNTStart.setDefaultText("");
            letNTEnd.setDefaultText("");
        }
    }
}
