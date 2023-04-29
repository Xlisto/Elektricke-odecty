package cz.xlisto.cenik.modules.invoice;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import cz.xlisto.cenik.R;
import cz.xlisto.cenik.databaze.DataPriceListSource;
import cz.xlisto.cenik.databaze.DataSubscriptionPointSource;
import cz.xlisto.cenik.models.InvoiceModel;
import cz.xlisto.cenik.models.PriceListModel;
import cz.xlisto.cenik.modules.pricelist.PriceListFragment;
import cz.xlisto.cenik.ownview.LabelEditText;
import cz.xlisto.cenik.ownview.OwnDatePicker;
import cz.xlisto.cenik.ownview.ViewHelper;
import cz.xlisto.cenik.shp.ShPAddEditInvoice;
import cz.xlisto.cenik.utils.FragmentChange;
import cz.xlisto.cenik.utils.Keyboard;

import static cz.xlisto.cenik.utils.FragmentChange.Transaction.MOVE;

/**
 * Xlisto 04.02.2023 11:58
 */
public abstract class InvoiceAddEditAbstractFragment extends Fragment {
    private static final String TAG = "InvoiceAddEditAbstractFragment";
    static final String TABLE = "table";
    static final String ID_FAK = "id_fak";
    static final String ID = "id";
    static final String BTNDATE_OF = "btnDate1";
    static final String BTNDATE_TO = "btnDate2";
    static final String VT_START = "vt_start";
    static final String NT_START = "nt_start";
    static final String VT_END = "vt_end";
    static final String NT_END = "nt_end";
    static final String OTHER_SERVICES = "other";
    static final String SELECTED_ID_PRICE = "selectedIdPrice";
    static final String SELECTED_ID_INVOICE = "selectedIdInvoice";
    Button btnDateStart, btnDateEnd, btnSelectPriceList, btnBack, btnSave;
    private PriceListModel selectedPrice;
    LabelEditText letVTStart, letVTEnd, letNTStart, letNTEnd, letOtherServices;
    long selectedIdPrice = -1L;
    long selectedIdInvoice = -1L;
    long id = -1L;
    String numberInvoice, table;
    String oldDateStart = "1.1.2023";
    String oldDateEnd = "31.12.2023";
    static boolean loadFromDatabase = true;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            table = getArguments().getString(TABLE);
            selectedIdInvoice = getArguments().getLong(ID_FAK);
            id = getArguments().getLong(ID);
        }
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

        btnDateStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                OwnDatePicker.showDialog(getActivity(), new OwnDatePicker.OnDateListener() {
                    @Override
                    public void getDate(String date) {
                        btnDateStart.setText(date);
                    }
                }, oldDateStart);
            }
        });

        btnDateEnd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                OwnDatePicker.showDialog(getActivity(), new OwnDatePicker.OnDateListener() {
                    @Override
                    public void getDate(String date) {
                        btnDateEnd.setText(date);
                    }
                }, oldDateEnd);
            }
        });

        btnSelectPriceList.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveSharedPreferences();
                PriceListFragment priceListFragment = PriceListFragment.newInstance(true, selectedIdPrice);
                priceListFragment.setOnSelectedPriceListListener(new PriceListFragment.OnSelectedPriceList() {
                    @Override
                    public void getOnSelectedItemPriceList(PriceListModel priceList) {
                        selectedPrice = priceList;
                        selectedIdPrice = selectedPrice.getId();
                        deactivateNT(selectedPrice.getSazba().equals("D 01d") || selectedPrice.getSazba().equals("D 02d"));

                    }
                });
                Keyboard.hide(getActivity());
                FragmentChange.replace(getActivity(), priceListFragment, MOVE, true);
            }
        });

        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Keyboard.hide(getActivity());
                loadFromDatabase = true;
                getParentFragmentManager().popBackStack();
            }
        });

        loadSharedPreferences();

        if (savedInstanceState != null) {
            btnDateStart.setText(savedInstanceState.getString(BTNDATE_OF));
            btnDateEnd.setText(savedInstanceState.getString(BTNDATE_TO));
            letVTStart.setDefaultText(savedInstanceState.getString(VT_START));
            letNTStart.setDefaultText(savedInstanceState.getString(NT_START));
            letVTEnd.setDefaultText(savedInstanceState.getString(VT_END));
            letNTEnd.setDefaultText(savedInstanceState.getString(NT_END));
            letOtherServices.setDefaultText(savedInstanceState.getString(OTHER_SERVICES));
            selectedIdPrice = savedInstanceState.getLong(SELECTED_ID_PRICE, -1L);
            selectedIdInvoice = savedInstanceState.getLong(SELECTED_ID_INVOICE, -1L);

        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (selectedPrice != null) {
            selectedIdPrice = selectedPrice.getId();
            deactivateNT(selectedPrice.getSazba().equals("D 01d") || selectedPrice.getSazba().equals("D 02d"));
            btnSelectPriceList.setText(selectedPrice.getName());
        }

    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(BTNDATE_OF, btnDateStart.getText().toString());
        outState.putString(BTNDATE_TO, btnDateEnd.getText().toString());
        outState.putString(VT_START, letVTStart.getText());
        outState.putString(NT_START, letNTStart.getText());
        outState.putString(VT_END, letVTEnd.getText());
        outState.putString(NT_END, letNTEnd.getText());
        outState.putString(OTHER_SERVICES, letOtherServices.getText());
        outState.putLong(SELECTED_ID_PRICE, selectedIdPrice);
        outState.putLong(SELECTED_ID_INVOICE, selectedIdInvoice);
    }

    InvoiceModel createInvoice() {
        ShPAddEditInvoice shPAddEditInvoice = new ShPAddEditInvoice(getContext());
        selectedIdPrice = shPAddEditInvoice.get(ShPAddEditInvoice.SELECTED_ID_PRICE, -1L);
        long start = ViewHelper.parseCalendarFromString(btnDateStart.getText().toString()).getTimeInMillis();
        long end = ViewHelper.parseCalendarFromString(btnDateEnd.getText().toString()).getTimeInMillis();
        return new InvoiceModel(start, end,
                letVTStart.getDouble(), letVTEnd.getDouble(),
                letNTStart.getDouble(), letNTEnd.getDouble(),
                selectedIdInvoice, selectedIdPrice,
                letOtherServices.getDouble(), numberInvoice
        );
    }

    InvoiceModel createInvoice(long id) {
        InvoiceModel invoice = createInvoice();
        invoice.setId(id);
        return invoice;
    }

    void saveSharedPreferences() {
        ShPAddEditInvoice shPAddEditInvoice = new ShPAddEditInvoice(getContext());
        shPAddEditInvoice.set(ShPAddEditInvoice.LOAD_PREFERENCES, true);
        shPAddEditInvoice.set(ShPAddEditInvoice.BTNDATE_OF, btnDateStart.getText().toString());
        shPAddEditInvoice.set(ShPAddEditInvoice.BTNDATE_TO, btnDateEnd.getText().toString());
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
            btnDateStart.setText(shPAddEditInvoice.get(ShPAddEditInvoice.BTNDATE_OF, "Datum"));
            btnDateEnd.setText(shPAddEditInvoice.get(ShPAddEditInvoice.BTNDATE_TO, "Datum"));
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
