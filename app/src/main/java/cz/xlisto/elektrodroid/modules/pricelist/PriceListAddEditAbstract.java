package cz.xlisto.elektrodroid.modules.pricelist;


import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static java.util.Calendar.*;
import static cz.xlisto.elektrodroid.format.DecimalFormatHelper.df2;
import static cz.xlisto.elektrodroid.format.SimpleDateFormatHelper.dateFormat;
import static cz.xlisto.elektrodroid.ownview.OwnDatePicker.showDialog;

import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.Fragment;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;

import cz.xlisto.elektrodroid.R;
import cz.xlisto.elektrodroid.dialogs.OwnAlertDialog;
import cz.xlisto.elektrodroid.dialogs.YesNoDialogFragment;
import cz.xlisto.elektrodroid.models.PriceListModel;
import cz.xlisto.elektrodroid.ownview.LabelEditText;
import cz.xlisto.elektrodroid.ownview.ViewHelper;
import cz.xlisto.elektrodroid.utils.DateUtil;
import cz.xlisto.elektrodroid.utils.Keyboard;
import cz.xlisto.elektrodroid.utils.ReadRawJSON;


/**
 * Abstraktní třída `PriceListAddEditAbstract` rozšiřující `Fragment`.
 * <p>
 * Tato třída poskytuje základní funkcionalitu pro přidávání a úpravu ceníků
 * v aplikaci. Obsahuje metody pro nastavení adaptérů, načítání regulovaných cen,
 * a manipulaci s různými widgety uživatelského rozhraní.
 */
public abstract class PriceListAddEditAbstract extends Fragment {

    public static String TAG = "PriceListAddEditAbstract";
    static final String BTN_FROM = "btnFrom";
    static final String BTN_UNTIL = "btnUntil";
    static final String RADA = "ivRada";
    static final String PRODUKT = "ivProdukt";
    static final String DODAVATEL = "ivDodavatel";
    static final String DISTRIBUCNI_UZEMI = "spDistribucniUzemi";
    static final String SAZBA_DISTRIBUCE = "spSazbaDistribuce";
    static final String VT_NEREGUL = "ivVT";
    static final String NT_NEREGUL = "ivNT";
    static final String MESICNI_PLAT = "ivMesicniPlat";
    static final String VT_REGUL = "ivVT1";
    static final String NT_REGUL = "ivNT1";
    static final String JISTIC0 = "ivJistic0";
    static final String JISTIC1 = "ivJistic1";
    static final String JISTIC2 = "ivJistic2";
    static final String JISTIC3 = "ivJistic3";
    static final String JISTIC4 = "ivJistic4";
    static final String JISTIC5 = "ivJistic5";
    static final String JISTIC6 = "ivJistic6";
    static final String JISTIC7 = "ivJistic7";
    static final String JISTIC8 = "ivJistic8";
    static final String JISTIC9 = "ivJistic9";
    static final String JISTIC10 = "ivJistic10";
    static final String JISTIC11 = "ivJistic11";
    static final String JISTIC12 = "ivJistic12";
    static final String JISTIC13 = "ivJistic13";
    static final String JISTIC14 = "ivJistic14";
    static final String OTE = "ivOTE";
    static final String CINNOST_OPERATORA = "ivCinnostOperatora";
    static final String OZE = "ivOZE";
    static final String POZE1 = "ivPOZE1";
    static final String POZE2 = "ivPOZE2";
    static final String SYSTEMOVE_SLUZBY = "ivSystemoveSluzby";
    static final String DAN = "ivDan";
    static final String DPH = "ivDPH";
    static final String JISTIC = "swJistic";
    static final String LAST_YEAR = "lastYear";
    static final String FLAG_RESULT_DIALOG_PROVOZ_NESITOVE_INFRASTRUKTURY = "flagResultDialogProvozNesitoveInfrastruktury";
    static final String FLAG_RESULT_DIALOG_PROVOZ_NESITOVE_INFRASTRUKTURY_2025 = "flagResultDialogProvozNesitoveInfrastruktury2025";
    private static final int DEF_MIN_YEAR = 2021;
    private static final int DEF_MAX_YEAR = 2026;

    String[] arrayDistUzemi;
    String[] arraySazba;
    int year, lastYear, selectionDistUzemi, selectionSazba;
    boolean isFirstLoad = true;
    boolean closedDialog = false;
    Long itemId;

    Button btnFrom, btnUntil, btnBack, btnSave, btnReloadRegulPriceList;
    LabelEditText ivRada, ivProdukt, ivDodavatel, ivVT, ivNT, ivPlat, ivVT1, ivNT1;
    LabelEditText ivJ0, ivJ1, ivJ2, ivJ3, ivJ4, ivJ5, ivJ6, ivJ7, ivJ8, ivJ9, ivJ10, ivJ11, ivJ12, ivJ13, ivJ14;
    LabelEditText ivOTE, ivCinnostOperatora, ivOZE, ivPOZE1, ivPOZE2, ivSystemSluzby, ivDan, ivDPH;
    SwitchCompat switchJistic;
    Spinner spSazba, spDistribucniUzemi;
    TextView tvNoPriceListDescription, tvNoPriceListTitle;
    MySpinnerDistributorsAdapter adapterDistUzemi, adapterSazba;
    RelativeLayout rlNoPriceList;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_price_list_add_edit, container, false);
    }


    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        btnFrom = view.findViewById(R.id.btnPlatnostOD);
        btnUntil = view.findViewById(R.id.btnPlatnostDO);
        btnBack = view.findViewById(R.id.btnZpet);
        btnSave = view.findViewById(R.id.btnUloz);
        btnReloadRegulPriceList = view.findViewById(R.id.btnReloadRegulPriceList);
        ivRada = view.findViewById(R.id.ivRada);
        ivProdukt = view.findViewById(R.id.ivProdukt);
        ivDodavatel = view.findViewById(R.id.ivDodavatel);
        ivVT = view.findViewById(R.id.ivVT);//neregul
        ivNT = view.findViewById(R.id.ivNT);
        ivPlat = view.findViewById(R.id.ivPlat);
        ivVT1 = view.findViewById(R.id.ivVT1);//regul
        ivNT1 = view.findViewById(R.id.ivNT1);
        ivJ0 = view.findViewById(R.id.ivJ0);
        ivJ1 = view.findViewById(R.id.ivJ1);
        ivJ2 = view.findViewById(R.id.ivJ2);
        ivJ3 = view.findViewById(R.id.ivJ3);
        ivJ4 = view.findViewById(R.id.ivJ4);
        ivJ5 = view.findViewById(R.id.ivJ5);
        ivJ6 = view.findViewById(R.id.ivJ6);
        ivJ7 = view.findViewById(R.id.ivJ7);
        ivJ8 = view.findViewById(R.id.ivJ8);
        ivJ9 = view.findViewById(R.id.ivJ9);
        ivJ10 = view.findViewById(R.id.ivJ10);
        ivJ11 = view.findViewById(R.id.ivJ11);
        ivJ12 = view.findViewById(R.id.ivJ12);
        ivJ13 = view.findViewById(R.id.ivJ13);
        ivJ14 = view.findViewById(R.id.ivJ14);
        ivOTE = view.findViewById(R.id.ivOTE);
        ivCinnostOperatora = view.findViewById(R.id.ivCinnostOperatoraTrhu);
        ivOZE = view.findViewById(R.id.ivOZE);
        ivPOZE1 = view.findViewById(R.id.ivPOZE1);
        ivPOZE2 = view.findViewById(R.id.ivPOZE2);
        ivSystemSluzby = view.findViewById(R.id.ivSystemoveSluzby);
        ivDan = view.findViewById(R.id.ivDan);
        ivDPH = view.findViewById(R.id.ivDPH);
        switchJistic = view.findViewById(R.id.swJistic);
        spDistribucniUzemi = view.findViewById(R.id.spDistribucniUzemiSeznam);
        spSazba = view.findViewById(R.id.spSazbaSeznam);
        tvNoPriceListDescription = view.findViewById(R.id.tvNoPriceListDescription);
        tvNoPriceListTitle = view.findViewById(R.id.tvNoPriceListTitle);
        rlNoPriceList = view.findViewById(R.id.rlNoPriceList);

        PriceListViewModel viewModel = new androidx.lifecycle.ViewModelProvider(requireActivity()).get(PriceListViewModel.class);
        viewModel.getSaveResultEvent().observe(getViewLifecycleOwner(), event -> {
            Boolean success = event == null ? null : event.getContentIfNotHandled();
            if (Boolean.TRUE.equals(success)) {
                getParentFragmentManager().popBackStack();
            }
        });

        btnFrom.setOnClickListener(v -> showDialog(getActivity(), day -> {
            closedDialog = true;
            btnFrom.setText(day);
            hideItemView();
            lastYear = year;
            year = getYearBtnStart();
            onResume();
            setRegulPrice();
            evaluateRegulatedPriceAvailability();
        }, btnFrom.getText().toString()));

        btnUntil.setOnClickListener(v -> showDialog(getActivity(), day -> {
            btnUntil.setText(day);
            setRegulPrice();
            evaluateRegulatedPriceAvailability();
        }, btnUntil.getText().toString()));

        switchJistic.setOnClickListener(v -> hideItemView());

        btnBack.setOnClickListener(v -> {
            Keyboard.hide(requireActivity());
            //vrácení o fragment zpět
            getParentFragmentManager().popBackStack();
        });

        selectionSazba = 0;
        selectionDistUzemi = 0;

        spSazba.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                setRegulPrice();
            }


            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        spDistribucniUzemi.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                setRegulPrice();
            }


            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        btnReloadRegulPriceList.setOnClickListener(v -> setRegulPrice());

        if (savedInstanceState != null) {
            //Restore the fragment's state here
            btnFrom.setText(savedInstanceState.getString(BTN_FROM));
            btnUntil.setText(savedInstanceState.getString(BTN_UNTIL));
            ivRada.setDefaultText(savedInstanceState.getString(RADA));
            ivProdukt.setDefaultText(savedInstanceState.getString(PRODUKT));
            ivDodavatel.setDefaultText(savedInstanceState.getString(DODAVATEL));
            selectionDistUzemi = savedInstanceState.getInt(DISTRIBUCNI_UZEMI, 0);
            selectionSazba = savedInstanceState.getInt(SAZBA_DISTRIBUCE, 0);
            ivVT.setDefaultText(savedInstanceState.getString(VT_NEREGUL));
            ivNT.setDefaultText(savedInstanceState.getString(NT_NEREGUL));
            ivPlat.setDefaultText(savedInstanceState.getString(MESICNI_PLAT));
            ivVT1.setDefaultText(savedInstanceState.getString(VT_REGUL));
            ivNT1.setDefaultText(savedInstanceState.getString(NT_REGUL));
            ivJ0.setDefaultText(savedInstanceState.getString(JISTIC0));
            ivJ1.setDefaultText(savedInstanceState.getString(JISTIC1));
            ivJ2.setDefaultText(savedInstanceState.getString(JISTIC2));
            ivJ3.setDefaultText(savedInstanceState.getString(JISTIC3));
            ivJ4.setDefaultText(savedInstanceState.getString(JISTIC4));
            ivJ5.setDefaultText(savedInstanceState.getString(JISTIC5));
            ivJ6.setDefaultText(savedInstanceState.getString(JISTIC6));
            ivJ7.setDefaultText(savedInstanceState.getString(JISTIC7));
            ivJ8.setDefaultText(savedInstanceState.getString(JISTIC8));
            ivJ9.setDefaultText(savedInstanceState.getString(JISTIC9));
            ivJ10.setDefaultText(savedInstanceState.getString(JISTIC10));
            ivJ11.setDefaultText(savedInstanceState.getString(JISTIC11));
            ivJ12.setDefaultText(savedInstanceState.getString(JISTIC12));
            ivJ13.setDefaultText(savedInstanceState.getString(JISTIC13));
            ivJ14.setDefaultText(savedInstanceState.getString(JISTIC14));
            ivOTE.setDefaultText(savedInstanceState.getString(OTE));
            ivCinnostOperatora.setDefaultText(savedInstanceState.getString(CINNOST_OPERATORA));
            ivOZE.setDefaultText(savedInstanceState.getString(OZE));
            ivPOZE1.setDefaultText(savedInstanceState.getString(POZE1));
            ivPOZE2.setDefaultText(savedInstanceState.getString(POZE2));
            ivSystemSluzby.setDefaultText(savedInstanceState.getString(SYSTEMOVE_SLUZBY));
            ivDan.setDefaultText(savedInstanceState.getString(DAN));
            ivDPH.setDefaultText(savedInstanceState.getString(DPH));
            switchJistic.setChecked(savedInstanceState.getBoolean(JISTIC));
            lastYear = savedInstanceState.getInt(LAST_YEAR);
            isFirstLoad = false;

        }
        year = getYearBtnStart();
        hideItemView();

        //nastavení adaptéru, výběr první položky, která reprezentuje nápovědu
        setSazbaAdapter();
        setDistribucniUzemiAdapter();

        /*
         * Listener pro výsledek dialogu `FLAG_RESULT_DIALOG_PROVOZ_NESITOVE_INFRASTRUKTURY`.
         *
         * <p>Chování:
         * - Po kladné odpovědi vytvoří dva `PriceListModel` s platností před a od začátku nového režimu (1.7.2024):
         *   - první s platností do 30.6.2024,
         *   - druhý s platností od 1.7.2024.
         * - Používá `createCalendar(year, month, day)` (parametr `month` je 0-based, např. 6 = červenec).
         *   Vytvořený `Calendar` používá výchozí časové pásmo zařízení (`Calendar.getInstance()`).
         * - U druhého ceníku nastaví pole `cinnost` na 9.24.
         * - Pokud je aktuální fragment instancí `PriceListAddFragment`, vloží oba nové záznamy;
         *   pokud je `PriceListEditFragment`, aktualizuje první a vloží druhý.
         */
        requireActivity().getSupportFragmentManager().setFragmentResultListener(FLAG_RESULT_DIALOG_PROVOZ_NESITOVE_INFRASTRUKTURY, this, (requestKey, result) -> {
            if (result.getBoolean(YesNoDialogFragment.RESULT)) {
                Calendar calendarLastJune = createCalendar(2024, 5, 30);
                Calendar calendarFirstJuly = createCalendar(2024, 6, 1);

                PriceListModel priceListModelFirst = createPriceListWithDates(calendarLastJune, null);
                PriceListModel priceListModelSecond = createPriceListWithDates(null, calendarFirstJuly);
                priceListModelSecond.setCinnost(9.24);

                 viewModel.preparePriceLists(priceListModelFirst, priceListModelSecond, this.getClass().equals(PriceListAddFragment.class), itemId);

            }
        });

        /*
         * Listener pro výsledek dialogu `FLAG_RESULT_DIALOG_PROVOZ_NESITOVE_INFRASTRUKTURY_2025`.
         *
         * <p>Chování:
         * - Po kladné odpovědi vytvoří dva `PriceListModel` s platností těsně před a od začátku nového režimu.
         * - Používá `createCalendar(year, month, day)` (pole `month` je 0-based, tedy 8 = září),
         *   vytvořený kalendář používá výchozí časové pásmo zařízení (Calendar.getInstance()).
         * - U druhého ceníku nastaví pole `cinnost` na 12.45.
         * - Pokud je aktuální fragment instancí `PriceListAddFragment`, vloží oba nové záznamy,
         *   pokud je `PriceListEditFragment`, aktualizuje první a vloží druhý.
         */
        requireActivity().getSupportFragmentManager().setFragmentResultListener(FLAG_RESULT_DIALOG_PROVOZ_NESITOVE_INFRASTRUKTURY_2025, this, (requestKey, result) -> {
            if (result.getBoolean(YesNoDialogFragment.RESULT)) {
                Calendar calendarLastAugust = createCalendar(2025, 7, 31);
                Calendar calendarFirstSeptember = createCalendar(2025, 8, 1);

                PriceListModel priceListModelFirst = createPriceListWithDates(calendarLastAugust, null);
                PriceListModel priceListModelSecond = createPriceListWithDates(null, calendarFirstSeptember);
                priceListModelSecond.setCinnost(12.45);

                viewModel.preparePriceLists(priceListModelFirst, priceListModelSecond, this.getClass().equals(PriceListAddFragment.class), itemId);
            }
        });
    }


    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(BTN_FROM, btnFrom.getText().toString());
        outState.putString(BTN_UNTIL, btnUntil.getText().toString());
        outState.putString(RADA, ivRada.getText());
        outState.putString(PRODUKT, ivProdukt.getText());
        outState.putString(DODAVATEL, ivDodavatel.getText());
        outState.putInt(SAZBA_DISTRIBUCE, spSazba.getSelectedItemPosition());
        outState.putInt(DISTRIBUCNI_UZEMI, spDistribucniUzemi.getSelectedItemPosition());
        outState.putString(VT_NEREGUL, ivVT.getText());
        outState.putString(NT_NEREGUL, ivNT.getText());
        outState.putString(MESICNI_PLAT, ivPlat.getText());
        outState.putString(VT_REGUL, ivVT1.getText());
        outState.putString(NT_REGUL, ivNT1.getText());
        outState.putString(JISTIC0, ivJ0.getText());
        outState.putString(JISTIC1, ivJ1.getText());
        outState.putString(JISTIC2, ivJ2.getText());
        outState.putString(JISTIC3, ivJ3.getText());
        outState.putString(JISTIC4, ivJ4.getText());
        outState.putString(JISTIC5, ivJ5.getText());
        outState.putString(JISTIC6, ivJ6.getText());
        outState.putString(JISTIC7, ivJ7.getText());
        outState.putString(JISTIC8, ivJ8.getText());
        outState.putString(JISTIC9, ivJ9.getText());
        outState.putString(JISTIC10, ivJ10.getText());
        outState.putString(JISTIC11, ivJ11.getText());
        outState.putString(JISTIC12, ivJ12.getText());
        outState.putString(JISTIC13, ivJ13.getText());
        outState.putString(JISTIC14, ivJ14.getText());
        outState.putString(OTE, ivOTE.getText());
        outState.putString(CINNOST_OPERATORA, ivCinnostOperatora.getText());
        outState.putString(OZE, ivOZE.getText());
        outState.putString(POZE1, ivPOZE1.getText());
        outState.putString(POZE2, ivPOZE2.getText());
        outState.putString(SYSTEMOVE_SLUZBY, ivSystemSluzby.getText());
        outState.putString(DAN, ivDan.getText());
        outState.putString(DPH, ivDPH.getText());
        outState.putBoolean(JISTIC, switchJistic.isChecked());

        outState.putInt(LAST_YEAR, year);
    }


    /**
     * Nastaví adaptér pro spinner sazby distribuce.
     *
     * <p>
     * Provádí bezpečné nastavení dat a adaptéru na hlavním vlákně:
     * - použije {@link android.os.Handler} s {@link android.os.Looper#getMainLooper()} a vykoná úlohu se zpožděním (1050 ms),
     * - před manipulací s UI ověří {@link #isAdded()} aby se zabránilo {@code IllegalStateException} pokud už fragment není připojen,
     * - načte pole sazeb z resources (`R.array.sazby`), vytvoří nový {@code MySpinnerDistributorsAdapter}
     * a nastaví ho do `spSazba`,
     * - obnoví předchozí výběr pomocí `selectionSazba`.
     * <p>
     * Poznámky:
     * - Všechny UI operace probíhají na hlavním vlákně.
     * - Zpoždění slouží k tomu, aby se adaptér nastavil až po dokončení jiných inicializací UI v onViewCreated.
     * - Metoda nijak nemění stav fragmentu mimo nastavení adaptéru a neprovádí I/O v pozadí.
     */
    void setSazbaAdapter() {
        // zajistit, že runnable poběží na hlavním vlákně a zkontrolovat, jestli je fragment připojen
        Handler handler = new Handler(android.os.Looper.getMainLooper());
        final Runnable r = () -> {
            if (!isAdded())
                return; // ochrana proti IllegalStateException když fragment už není připojen

            arraySazba = getResources().getStringArray(R.array.sazby);
            adapterSazba = new MySpinnerDistributorsAdapter(requireContext(), R.layout.spinner_view, arraySazba, year);
            spSazba.setAdapter(adapterSazba);
            spSazba.setSelection(selectionSazba, true);
        };
        handler.postDelayed(r, 1050);
    }


    /**
     * Nastaví adaptér pro spinner distribučního území.
     *
     * <p>
     * Provádí bezpečné nastavení dat a adaptéru na hlavním vlákně:
     * - použije {@link android.os.Handler} s {@link android.os.Looper#getMainLooper()} a vykoná úlohu se zpožděním (1140 ms),
     * - před manipulací s UI ověří {@link #isAdded()} aby se zabránilo {@code IllegalStateException} pokud už fragment není připojen,
     * - načte pole distribučních území z resources (`R.array.distribucni_uzemi`), vytvoří nový {@code MySpinnerDistributorsAdapter}
     * a nastaví ho do `spDistribucniUzemi`,
     * - obnoví předchozí výběr pomocí `selectionDistUzemi`,
     * - po nastavení adaptéru zavolá {@link #evaluateRegulatedPriceAvailability()} pro aktualizaci stavu UI závislého na dostupnosti regulovaných cen.
     * <p>
     * Poznámky:
     * - Všechny UI operace probíhají na hlavním vlákně.
     * - Zpoždění slouží k tomu, aby se adaptér nastavil až po dokončení jiných inicializací UI v onViewCreated.
     * - Metoda nijak neprovádí I/O v pozadí (načítání resources je rychlé) a dbá na bezpečnost vůči životnímu cyklu fragmentu.
     */
    void setDistribucniUzemiAdapter() {
        // zajistit, že runnable poběží na hlavním vlákně a zkontrolovat, jestli je fragment připojen
        Handler handler = new Handler(android.os.Looper.getMainLooper());
        final Runnable r = () -> {
            if (!isAdded())
                return; // ochrana proti IllegalStateException když fragment už není připojen

            arrayDistUzemi = getResources().getStringArray(R.array.distribucni_uzemi);
            adapterDistUzemi = new MySpinnerDistributorsAdapter(requireContext(), R.layout.spinner_view, arrayDistUzemi, year);
            spDistribucniUzemi.setAdapter(adapterDistUzemi);
            spDistribucniUzemi.setSelection(selectionDistUzemi, true);
            evaluateRegulatedPriceAvailability();
        };
        handler.postDelayed(r, 1140);
    }


    /**
     * Změni spinner podle data na E.ON nebo EG.D. Položek ČEZ a PRE se netýká.
     * <p>
     * Pokud je vybraný index 0, PRE nebo ČEZ, metoda nic nemění.
     * Pokud je rok 2021 a vyšší a vybraný string položky neodpovídá EG.D,
     * vybere položku s indexem 3 (EG.D).
     * Pokud je rok 2020 a nižší a vybraný string položky neodpovídá E.ON,
     * vybere položku s indexem 2 (E.ON).
     */
    void changeDistributionSpinner() {
        //nic neměnit, pokud je vybraný index 0, PRE nebo ČEZ
        if (spDistribucniUzemi.getSelectedItem().toString().equals("PRE") || spDistribucniUzemi.getSelectedItem().toString().equals("ČEZ") || spDistribucniUzemi.getSelectedItemPosition() == 0)
            return;

        //po předchozí  podmínce se zde dostanou jen E.ON a EG.D
        //pokud je rok 2021 a vyšší. Vybraný string položky neodpovídá EG.D, tak se vybere položka s indexem 3. To je EG.D
        //pokud je rok 2020 a nižší. Vybraný string položky neodpovídá E.ON, tak se vybere položka s indexem 2. To je E.ON
        if (year >= 2021) {
            if (!spDistribucniUzemi.getSelectedItem().toString().equals("EG.D")) {
                setDistribucniUzemiAdapter();
                selectionDistUzemi = 3;
            }
        } else {
            if (!spDistribucniUzemi.getSelectedItem().toString().equals("E.ON")) {
                setDistribucniUzemiAdapter();
                selectionDistUzemi = 2;
            }
        }
    }


    /**
     * Vrátí rok extrahovaný z data zobrazeného v tlačítku "Platnost od".
     * <p>
     * Metoda:
     * - přečte text z tlačítka `btnFrom`,
     * - pomocí `ViewHelper.parseCalendarFromString` vytvoří objekt `Calendar`,
     * - vrátí rok získaný z pomocné třídy `DateUtil`.
     * <p>
     * Metoda pouze čte hodnotu z UI a neprovádí žádné vedlejší efekty (neukládá rok do polí třídy).
     *
     * @return rok vybraný v tlačítku "Platnost od" jako `int`
     */
    int getYearBtnStart() {
        Calendar calendar = ViewHelper.parseCalendarFromString(btnFrom.getText().toString());
        return new DateUtil(calendar).getYear();
    }


    /**
     * Vrátí čas v milisekundách extrahovaný z data zobrazeného v tlačítku "Platnost od".
     * <p>
     * Metoda:
     * - přečte text z tlačítka `btnFrom`,
     * - pomocí `ViewHelper.parseCalendarFromString` vytvoří objekt `Calendar`,
     * - vrátí hodnotu `calendar.getTimeInMillis()`.
     * <p>
     * Metoda pouze čte hodnotu z UI a neprovádí žádné vedlejší efekty (neukládá stav).
     *
     * @return čas v milisekundách odpovídající datu z `btnFrom`
     */
    long getLongBtnStart() {
        Calendar calendar = ViewHelper.parseCalendarFromString(btnFrom.getText().toString());
        return calendar.getTimeInMillis();
    }


    /**
     * Zobrazí nebo skryje doplňkové hodnoty jističů u tarifu D57d.
     * <p>
     * Pokud je přepínač jističů zapnutý, zobrazí více hodnot jističů a skryje některé další.
     * Pokud je přepínač jističů vypnutý, zobrazí méně hodnot jističů a skryje některé další.
     * <p>
     * Také upraví viditelnost některých dalších hodnot na základě roku.
     */
    void hideItemView() {
        //Zobrazí/skryje doplňkové hodnoty jističů u tarifu D57d
        if (switchJistic.isChecked()) {
            switchJistic.setText(R.string.zobrazeno_vice);
            ivJ10.setVisibility(VISIBLE);
            ivJ11.setVisibility(VISIBLE);
            ivJ12.setVisibility(VISIBLE);
            ivJ13.setVisibility(VISIBLE);
            ivJ14.setVisibility(VISIBLE);
            ivJ8.setVisibility(GONE);
        } else {
            switchJistic.setText(R.string.zobrazeno_mene);
            ivJ10.setVisibility(GONE);
            ivJ11.setVisibility(GONE);
            ivJ12.setVisibility(GONE);
            ivJ13.setVisibility(GONE);
            ivJ14.setVisibility(GONE);
            ivJ8.setVisibility(VISIBLE);
        }

        if (year >= 2016) {//když je větší než toto etDatum (nad 2016 rok)
            ivOZE.setVisibility(View.GONE);
            ivOTE.setVisibility(View.GONE);
            ivCinnostOperatora.setVisibility(View.VISIBLE);
            ivPOZE1.setVisibility(View.VISIBLE);
            ivPOZE2.setVisibility(View.VISIBLE);
        } else {
            ivOZE.setVisibility(View.VISIBLE);
            ivOTE.setVisibility(View.VISIBLE);
            ivCinnostOperatora.setVisibility(View.GONE);
            ivPOZE1.setVisibility(View.GONE);
            ivPOZE2.setVisibility(View.GONE);
        }
    }


    /**
     * Vytvoří a vrátí instanci {@link PriceListModel} naplněnou hodnotami z aktuálních UI komponent.
     * <p>
     * Metoda:
     * - získá čas vytvoření z aktuálního systémového kalendáře,
     * - načte platnosti (platnost od / platnost do) z tlačítek {@code btnFrom} a {@code btnUntil},
     * - přečte textová a číselná pole z jednotlivých {@code LabelEditText} a výběry ze spinnerů,
     * - sestaví a vrátí nový {@code PriceListModel} s id nastaveným na {@code -1L}.
     * <p>
     * Poznámky:
     * - Metoda neprovádí rozsáhlou validaci vstupů; volající by měl zajistit, že UI prvky jsou inicializované
     * (např. metoda volána po {@code onViewCreated}) a že hodnoty jsou platné.
     * - Pokud je potřeba validační logika nebo ošetření chyb, přidejte ji před nebo po volání této metody.
     *
     * @return nový {@code PriceListModel} obsahující data aktuálně zobrazená v UI
     */
    PriceListModel createPriceList() {
        Calendar calendar = getInstance();
        long dateCreated = calendar.getTimeInMillis();
        String email = "";
        String autor = "";
        long validityFrom = ViewHelper.parseCalendarFromString(btnFrom.getText().toString()).getTimeInMillis();
        long validityUntil = ViewHelper.parseCalendarFromString(btnUntil.getText().toString()).getTimeInMillis();

        return new PriceListModel(-1L, ivRada.getText(), ivProdukt.getText(), ivDodavatel.getText(), ivVT.getDouble(), ivNT.getDouble(), ivPlat.getDouble(), ivDan.getDouble(), spSazba.getSelectedItem().toString(), ivVT1.getDouble(), ivNT1.getDouble(), ivJ0.getDouble(), ivJ1.getDouble(), ivJ2.getDouble(), ivJ3.getDouble(), ivJ4.getDouble(), ivJ5.getDouble(), ivJ6.getDouble(), ivJ7.getDouble(), ivJ8.getDouble(), ivJ9.getDouble(), ivJ10.getDouble(), ivJ11.getDouble(), ivJ12.getDouble(), ivJ13.getDouble(), ivJ14.getDouble(), ivSystemSluzby.getDouble(), ivCinnostOperatora.getDouble(), ivPOZE1.getDouble(), ivPOZE2.getDouble(), ivOZE.getDouble(), ivOTE.getDouble(), validityFrom, validityUntil, ivDPH.getDouble(), spDistribucniUzemi.getSelectedItem().toString(), autor, dateCreated, email);
    }


    /**
     * Načte regulované ceny asynchronně a aplikuje je do UI.
     * <p>
     * Chování:
     * - Neprovádí žádné IO na UI vlákně: čtení proběhne v pozadí pomocí
     * single-thread {@link java.util.concurrent.ExecutorService}.
     * - Celá úloha je zabalena v {@code try-catch-finally}: při výjimce se zaznamená
     * chybová hláška a na UI se zobrazí informace (pokud je fragment připojen).
     * - Výsledná aktualizace widgetů probíhá přes {@code mainHandler.post(...)} na hlavním vlákně.
     * - Před manipulací s UI se kontroluje {@code isAdded()}.
     * - V {@code finally} je vždy zavoláno {@code executor.shutdown()}.
     * <p>
     * Poznámky:
     * - Metoda předpokládá, že volající rozhoduje o tom, kdy je vhodné ji zavolat
     * (např. podle viditelnosti tlačítka pro načtení). ReadRawJSON zodpovídá za detailní
     * zpracování/parsing dat.
     */

    void setRegulPrice() {
        if (year < DEF_MIN_YEAR) return;

        ReadRawJSON readRawJSON = new ReadRawJSON(getActivity());
        java.util.concurrent.ExecutorService executor = java.util.concurrent.Executors.newSingleThreadExecutor();
        android.os.Handler mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());

        executor.execute(() -> {
            try {
                Calendar startCal = ViewHelper.parseCalendarFromString(btnFrom.getText().toString());
                Calendar endCal = ViewHelper.parseCalendarFromString(btnUntil.getText().toString());

                PriceListModel priceListModel = readRawJSON.read(startCal, endCal, spDistribucniUzemi.getSelectedItem().toString(), spSazba.getSelectedItem().toString());



                double[] regulPrice = new double[]{priceListModel.getDistVT(), priceListModel.getDistNT(), priceListModel.getJ0(), priceListModel.getJ1(), priceListModel.getJ2(), priceListModel.getJ3(), priceListModel.getJ4(), priceListModel.getJ5(), priceListModel.getJ6(), priceListModel.getJ7(), priceListModel.getJ8(), priceListModel.getJ9(), priceListModel.getJ10(), priceListModel.getJ11(), priceListModel.getJ12(), priceListModel.getJ13(), priceListModel.getJ14(), priceListModel.getSystemSluzby(), priceListModel.getCinnost(), priceListModel.getPoze1(), priceListModel.getPoze2(), priceListModel.getDan(), priceListModel.getDph()};

                LabelEditText[] labelEditTexts = new LabelEditText[]{ivVT1, ivNT1, ivJ0, ivJ1, ivJ2, ivJ3, ivJ4, ivJ5, ivJ6, ivJ7, ivJ8, ivJ9, ivJ10, ivJ11, ivJ12, ivJ13, ivJ14, ivSystemSluzby, ivCinnostOperatora, ivPOZE1, ivPOZE2, ivDan, ivDPH};

                mainHandler.post(() -> {
                    if (!isAdded()) return;
                    for (int i = 0; i < regulPrice.length; i++) {
                        labelEditTexts[i].setAllowChangeBackgroundColor(false);
                        labelEditTexts[i].setChangedBackgroundEditText(R.drawable.error_edittext_background);
                        labelEditTexts[i].setDefaultText(df2.format(regulPrice[i]));
                        labelEditTexts[i].setAllowChangeBackgroundColor(true);
                    }

                    switchJistic.setChecked((priceListModel.getJ10() != 0) || (priceListModel.getJ11() != 0) || (priceListModel.getJ12() != 0) || (priceListModel.getJ13() != 0));

                    hideItemView();
                    changeTitleOperatorTrhu();
                    chanagePriceOperatorTrhu();
                });
            } catch (Exception e) {
                Log.e(TAG, "Chyba při načítání regulovaných cen: " + e.getMessage(), e);
                mainHandler.post(() -> {
                    if (!isAdded()) return;
                    // Volitelně: informovat uživatele nebo vyčistit pole
                    tvNoPriceListDescription.setVisibility(View.VISIBLE);
                    btnReloadRegulPriceList.setVisibility(View.GONE);
                });
            } finally {
                executor.shutdown();
            }
        });
    }


    /**
     * Aktualizuje viditelnost a stav UI související s načítáním regulovaných cen
     * na základě dat z tlačítek `btnFrom` a `btnUntil`.
     * <p>
     * Chování:
     * - Parsuje datumy z `btnFrom` a `btnUntil` pomocí `ViewHelper.parseCalendarFromString`.
     * - Spouští vlákno na pozadí (nové `Thread`) které volá `getMinMaxYearFromRaw(selectedStart, selectedEnd)`
     * pro načtení minimálního/ maximálního roku a seznamu platných intervalů z `res/raw/ostatni.json`.
     * - Po dokončení práce na pozadí přepne do UI vlákna pomocí `requireActivity().runOnUiThread(...)`
     * a provede následující kontroly a akce:
     * - aktivuje/deaktivuje `btnSave` podle výsledků validací,
     * - kontroluje, zda je vybraný rok v rozsahu `minYear..maxYear`,
     * - kontroluje pořadí a překrytí datumů (start <= end a intervaly pokrývají zvolený rozsah),
     * - sestaví text s dostupnými intervaly pro zobrazení v `tvNoPriceListDescription`,
     * - přepne viditelnost `btnReloadRegulPriceList`, `tvNoPriceListDescription` a `rlNoPriceList`,
     * - aktualizuje nadpis `tvNoPriceListTitle` podle toho, zda platí "každý rok" nebo "během roku".
     * <p>
     * Robustnost:
     * - Metoda neprovádí IO na hlavním vlákně; všechny IO a parsing jsou v pozadí.
     * - Závisí na tom, že `getMinMaxYearFromRaw` vrací fallback hodnoty `DEF_MIN_YEAR` a `DEF_MAX_YEAR`
     * v případě chyby při načítání/parsing JSONu.
     * - Metoda nijak nevyhazuje výjimky volajícímu; příp. chyby zpracovává interně a upravuje UI.
     * <p>
     * Poznámky k vylepšení:
     * - Pro lepší kontrolu životního cyklu a zrušení úloh lze místo `new Thread` použít `ExecutorService`
     * nebo jiný mechanismus správy asynchronních úloh.
     */
    void evaluateRegulatedPriceAvailability() {
        String startDate = btnFrom.getText().toString();
        String endDate = btnUntil.getText().toString();
        StringBuilder errorTitleBuilder = new StringBuilder();
        StringBuilder errorMessageBuilder = new StringBuilder();

        final Calendar selectedStart = ViewHelper.parseCalendarFromString(startDate);
        final Calendar selectedEnd = ViewHelper.parseCalendarFromString(endDate);
        final int selectedStartYear = selectedStart.get(YEAR);
        final int selectedEndYear = selectedEnd.get(YEAR);

        // kontola rozsahů platnosti
        new Thread(() -> {
            ValidityDateContainer getMinMaxYearFromRaw = getMinMaxYearFromRaw(selectedStart, selectedEnd);
            int minYear = getMinMaxYearFromRaw.getMinYear();
            int maxYear = getMinMaxYearFromRaw.getMaxYear();
            ArrayList<Calendar> dates = getMinMaxYearFromRaw.getDates();

            requireActivity().runOnUiThread(() -> {
                // kontrola platnosti, které nejsou uvedeny
                btnSave.setEnabled(true);
                if (selectedStartYear < selectedEndYear) {
                    // konečný rok je menší než počáteční
                    errorTitleBuilder.append(getString(R.string.start_year_is_smaller_title));
                    errorMessageBuilder.append(getString(R.string.start_year_is_smaller_message));
                } else if (selectedStartYear < minYear || selectedStartYear > maxYear) {
                    // zadaný rok je mimo rozsah ceníků
                    errorTitleBuilder.append(getString(R.string.no_price_list));
                } else if (selectedEnd.getTimeInMillis() < selectedStart.getTimeInMillis()) {
                    // konečný rok je větší než počáteční
                    errorTitleBuilder.append(getString(R.string.end_year_is_smaller));
                    btnSave.setEnabled(false);
                } else {
                    // kontrola platnosti měsíců v ostatní
                    boolean found = false;
                    StringBuilder datesMessageBuilder = new StringBuilder();
                    for (int i = 0; i < dates.size(); i += 2) {
                        Calendar cal1 = dates.get(i);
                        Calendar cal2 = dates.get(i + 1);
                        if (cal1.get(YEAR) != cal2.get(YEAR)) {
                            // pokud roky nesouhlasí, kontrola se ukončí
                            found = true;
                            break;
                        } else if (cal1.get(YEAR) == selectedStartYear) {
                            // pokud jsou roky stejné, vytvoří se rozsah platných datumů
                            datesMessageBuilder.append(dateFormat.format(cal1.getTime())).append(" - ").append(dateFormat.format(cal2.getTime()));
                            datesMessageBuilder.append('\n');
                        }
                    }
                    String datesMessage = datesMessageBuilder.toString().trim();

                    // kontrola průniku, pokud některý datumový rozsah vyhovuje rozsahu načtený v ceníku, cyklus se ukončí nastavením proměnné found na true
                    for (int i = 0; i < dates.size(); i += 2) {
                        Calendar calendar1 = dates.get(i);
                        Calendar calendar2 = dates.get(i + 1);

                        if (calendar1.getTimeInMillis() <= selectedStart.getTimeInMillis() && calendar2.getTimeInMillis() >= selectedEnd.getTimeInMillis()) {
                            found = true;
                            break;
                        }
                    }
                    //pokud je found false, nastaví se obsah chybového hlášení a připojí se datumový rozpis
                    if (!found) {
                        tvNoPriceListTitle.setText(R.string.alert_regulated_prices_title_during_the_year);
                        errorTitleBuilder.append(getString(R.string.alert_regulated_prices_title_during_the_year, selectedStartYear));
                        errorMessageBuilder.append(getString(R.string.alert_regulated_prices_text)).append(datesMessage);
                    }
                }

                // zobrazení chybového hlášení, pokud něco obsahuje a zneaktivnění tlačítka pro uložení.
                if (errorTitleBuilder.length() == 0) {
                    btnReloadRegulPriceList.setVisibility(View.VISIBLE);
                    tvNoPriceListDescription.setVisibility(View.GONE);
                    rlNoPriceList.setVisibility(GONE);
                } else {
                    tvNoPriceListTitle.setText(errorTitleBuilder);
                    tvNoPriceListDescription.setText(errorMessageBuilder);
                    btnReloadRegulPriceList.setVisibility(View.GONE);
                    rlNoPriceList.setVisibility(VISIBLE);
                }
                if (errorMessageBuilder.length() == 0)
                    tvNoPriceListDescription.setVisibility(View.GONE);
                else tvNoPriceListDescription.setVisibility(View.VISIBLE);
            });
        }).start();
    }


    /**
     * Načte a zpracuje soubor `res/raw/ostatni.json` a vrátí minimální a maximální rok
     * spolu se seznamem platných intervalů (počátek/konec) pro regulované ceny.
     * <p>
     * Chování:
     * - Parsuje JSON a z každého záznamu přečte hodnotu `rok` pro výpočet `minYear` a `maxYear`.
     * - Pole `ostatni` v záznamu může být objekt nebo pole:
     * - Pokud je objekt, metoda vytvoří interval 1.1.<rok> - 31.12.<rok>.
     * - Pokud je pole, pro každý prvek přečte `od` a `do` (očekávaný formát den.měsíc nebo podobný)
     * a vytvoří odpovídající intervaly pro rok zvolených dat (`selectedStart`/`selectedEnd`).
     * - Vytvářené `Calendar` objekty mají explicitně nastavené časové složky (začátek dne / konec dne).
     * <p>
     * Robustnost:
     * - Metoda zachytává výjimky interně; v případě chyby (např. nepřístupný nebo nevalidní JSON)
     * vrací fallback hodnoty `DEF_MIN_YEAR` a `DEF_MAX_YEAR` a prázdný nebo částečně naplněný seznam datumů.
     * - Nevyhazuje výjimky volajícímu; volající získá vždy instanci `ValidityDateContainer`.
     *
     * @param selectedStart Počáteční vybrané datum (použito pro sestavení konkrétních intervalů).
     * @param selectedEnd   Konečné vybrané datum (použito pro sestavení konkrétních intervalů).
     * @return ValidityDateContainer obsahující:
     * - minYear: nejmenší nalezený rok (nebo fallback),
     * - maxYear: největší nalezený rok (nebo fallback),
     * - dates: seznam `Calendar` párů (každá dvojice = začátek a konec platného intervalu).
     */
    private ValidityDateContainer getMinMaxYearFromRaw(Calendar selectedStart, Calendar selectedEnd) {
        int minYear = Integer.MAX_VALUE;
        int maxYear = Integer.MIN_VALUE;
        Calendar validStart = getInstance();
        Calendar validEnd = getInstance();
        ArrayList<Calendar> dates = new ArrayList<>();

        try (java.io.InputStream is = getResources().openRawResource(R.raw.ostatni); java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(is))) {

            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line);

            org.json.JSONArray arr = new org.json.JSONArray(sb.toString());
            for (int i = 0; i < arr.length(); i++) {
                org.json.JSONObject obj = arr.getJSONObject(i);
                if (obj.has("rok")) {
                    int r = obj.getInt("rok");
                    if (r < minYear) minYear = r;
                    if (r > maxYear) maxYear = r;
                }
                Object ostatni = obj.get("ostatni");
                int year = obj.getInt("rok");
                // pokud je nastaven jen celý rok
                if (ostatni instanceof JSONObject) {
                    // nastavení na 1.1. vybraného roku, čas = 00:00:00.000
                    validStart.set(selectedStart.get(YEAR), JANUARY, 1, 0, 0, 0);
                    validStart.set(MILLISECOND, 0);
                    validEnd.set(selectedEnd.get(YEAR), DECEMBER, 31, 0, 0, 0);
                    validEnd.set(MILLISECOND, 0);
                    Calendar parsedStart = getInstance();
                    parsedStart.set(year, JANUARY, 1, 0, 0, 0);
                    parsedStart.set(MILLISECOND, 0);

                    Calendar parsedEnd = getInstance();
                    parsedEnd.set(year, DECEMBER, 31, 23, 59, 59);
                    parsedEnd.set(MILLISECOND, 0);

                    dates.add(parsedStart);
                    dates.add(parsedEnd);
                }
                // pokud je rok rozdělen na několik dalších úseků
                if (ostatni instanceof JSONArray) {
                    JSONArray ostatniArray = (JSONArray) ostatni;
                    for (int j = 0; j < ostatniArray.length(); j++) {
                        JSONObject ostatniObj = ostatniArray.getJSONObject(j);
                        String odRaw = ostatniObj.optString("od", "");
                        String doRaw = ostatniObj.optString("do", "");

                        int odDay = 1, odMonth = 0;   // default 1.1.
                        int doDay = 31, doMonth = 11; // default 31.12.

                        try {
                            String[] odParts = odRaw.replaceAll("\\s", "").split("\\D+");
                            if (odParts.length >= 2) {
                                odDay = Integer.parseInt(odParts[0]);
                                odMonth = Integer.parseInt(odParts[1]) - 1; // Calendar: 0-based
                            }
                        } catch (Exception ex) {
                            Log.w(TAG, "Nelze parsovat 'od': " + odRaw + " - použito výchozí 1.1", ex);
                        }

                        try {
                            String[] doParts = doRaw.replaceAll("\\s", "").split("\\D+");
                            if (doParts.length >= 2) {
                                doDay = Integer.parseInt(doParts[0]);
                                doMonth = Integer.parseInt(doParts[1]) - 1;
                            }
                        } catch (Exception ex) {
                            Log.w(TAG, "Nelze parsovat 'do': " + doRaw + " - použito výchozí 31.12", ex);
                        }

                        Calendar parsedStart = getInstance();
                        parsedStart.set(year, odMonth, odDay, 0, 0, 0);
                        parsedStart.set(MILLISECOND, 0);

                        Calendar parsedEnd = getInstance();
                        parsedEnd.set(year, doMonth, doDay, 23, 59, 59);
                        parsedEnd.set(MILLISECOND, 0);

                        dates.add(parsedStart);
                        dates.add(parsedEnd);
                    }
                }

            }
        } catch (Exception e) {
            // fallback pokud se nepodaří načíst JSON
            minYear = DEF_MIN_YEAR;
            maxYear = DEF_MAX_YEAR;
        }

        if (minYear == Integer.MAX_VALUE || maxYear == Integer.MIN_VALUE) {
            minYear = DEF_MIN_YEAR;
            maxYear = DEF_MAX_YEAR;
        }
        return new ValidityDateContainer(minYear, maxYear, dates);
    }


    /**
     * Změní název pole "Činnost operátora trhu" na "Provoz nesíťové infrastruktury"
     * pokud je datum v tlačítku "Platnost od" větší nebo rovno 1.7.2024.
     */
    void changeTitleOperatorTrhu() {
        if (isAdded()) {
            if (getLongBtnStart() >= 1719784800000L) {//1.7.2024
                ivCinnostOperatora.setLabel(getString(R.string.provoz_nesitove_infrastruktury));
            } else {
                ivCinnostOperatora.setLabel(getString(R.string.cinnost_operatora_trhu));
            }
        }
    }


    /**
     * Změní cenu pole "Činnost operátora trhu" na pevnou hodnotu 9.24,
     * pokud je datum v tlačítku "Platnost od" větší nebo rovno 1.7.2024 a rok je 2024.
     * <p>
     * Pokud jsou splněny podmínky, nastaví cenu a zakáže změnu barvy pozadí.
     */
    void chanagePriceOperatorTrhu() {
        if (isAdded() && getLongBtnStart() >= 1719784800000L && year == 2024) {//1.7.2024
            ivCinnostOperatora.setDefaultText("9.24");
            ivCinnostOperatora.setAllowChangeBackgroundColor(false);
        }
    }


    //TODO: kontrola ceníku 2024 a 2025
    /**
     * Zkontroluje datumy nastavené na btnFrom a btnUntil.
     * Pokud btnFrom je mezi 1.1.2024 a 30.6.2024, btnUntil musí být také do 30.6.2024.
     * Pokud btnFrom je od 1.7.2024, btnUntil musí být maximálně do 31.12.2024.
     * Pokud btnUntil je menší než btnFrom, zobrazí dialogové okno s výstrahou.
     * Pokud podmínky nejsou splněny, zobrazí dialogové okno s výstrahou.
     *
     * @return true, pokud jsou datumy neplatné a zobrazí dialog s výstrahou, jinak false.
     */
    boolean checkDateConditions() {
        Calendar fromDate = ViewHelper.parseCalendarFromString(btnFrom.getText().toString());
        Calendar untilDate = ViewHelper.parseCalendarFromString(btnUntil.getText().toString());

        boolean isValid2024 = isValid(2024, 1, JULY, fromDate, untilDate);
        boolean isValid2025 = isValid(2025, 1, SEPTEMBER, fromDate, untilDate);

        if (!isValid2024) {
            String title = requireContext().getString(R.string.alert_title);
            String message = requireContext().getString(R.string.alert_message_provoz_nesitove_infrastruktury);

            if (!requireActivity().isFinishing()) {
                YesNoDialogFragment yesNoDialogFragment = YesNoDialogFragment.newInstance(title, FLAG_RESULT_DIALOG_PROVOZ_NESITOVE_INFRASTRUKTURY, message);
                yesNoDialogFragment.show(requireActivity().getSupportFragmentManager(), TAG);
            }

        } else if (!isValid2025) {
            String title = requireContext().getString(R.string.alert_title);
            String message = requireContext().getString(R.string.alert_message_provoz_nesitove_infrastruktury_new_price);

            if (!requireActivity().isFinishing()) {
                YesNoDialogFragment yesNoDialogFragment = YesNoDialogFragment.newInstance(title, FLAG_RESULT_DIALOG_PROVOZ_NESITOVE_INFRASTRUKTURY_2025, message);
                yesNoDialogFragment.show(requireActivity().getSupportFragmentManager(), TAG);
            }

        }

        if (untilDate.getTimeInMillis() < fromDate.getTimeInMillis()) {
            OwnAlertDialog.showDialog(requireActivity(), requireContext().getString(R.string.alert_title), requireContext().getString(R.string.alert_message_older_date));
            return true;
        }
        return (!isValid2024 || !isValid2025);
    }


    /**
     * Zkontroluje platnost intervalu [fromDate, untilDate] vůči pravidlům pro konkrétní rok.
     * <p>
     * Pravidla:
     * - Referenční rok je určen parametrem `year`. Celý rok je od 1.1. 00:00:00 do 31.12. 23:59:59.
     * - Nový ceník začíná v dni/měsíci zadaném parametry `newDay` a `newMonth` (měsíc 0-11) v tomtéž roce.
     * - Pokud `fromDate` leží v intervalu [1.1., startNewPrice) pak `untilDate` musí být před `startNewPrice`
     * (tj. `untilDate.getTimeInMillis() < startNewPrice.getTimeInMillis()`).
     * - Pokud `fromDate` leží v intervalu [startNewPrice, 31.12.] pak `untilDate` nesmí překročit konec roku
     * (tj. `untilDate.getTimeInMillis() <= end.getTimeInMillis()`).
     * <p>
     * Poznámky:
     * - Porovnání probíhá na úrovni milisekund.
     * - Hranice jsou explicitně nastaveny: začátky dne na 00:00:00.000, konec dne na 23:59:59.000.
     *
     * @param year      cílový rok pro validaci
     * @param newDay    den začátku nového ceníku (1-31)
     * @param newMonth  měsíc začátku nového ceníku (0-11)
     * @param fromDate  počáteční datum intervalu (včetně času)
     * @param untilDate konečné datum intervalu (včetně času)
     * @return true pokud interval splňuje pravidla pro zadaný rok, jinak false
     */
    private static boolean isValid(int year, int newDay, int newMonth, Calendar fromDate, Calendar untilDate) {
        //začátek platnosti ceníku
        Calendar start = getInstance();
        start.set(year, JANUARY, 1, 0, 0, 0);
        start.set(MILLISECOND, 0);

        //Datum platnosti nového ceníku
        Calendar startNewPrice = getInstance();
        startNewPrice.set(year, newMonth, newDay, 0, 0, 0);
        startNewPrice.set(MILLISECOND, 0);

        //konec platnosti ceníku
        Calendar end = getInstance();
        end.set(year, DECEMBER, 31, 23, 59, 59);
        end.set(MILLISECOND, 0);

        boolean isValid = true;

        if (fromDate.getTimeInMillis() >= start.getTimeInMillis() && fromDate.getTimeInMillis() < startNewPrice.getTimeInMillis()) {
            if (untilDate.getTimeInMillis() >= startNewPrice.getTimeInMillis()) {
                isValid = false;
            }
        } else if (fromDate.getTimeInMillis() >= startNewPrice.getTimeInMillis() && fromDate.getTimeInMillis() <= end.getTimeInMillis()) {
            if (untilDate.getTimeInMillis() > end.getTimeInMillis()) {
                isValid = false;
            }
        }
        return isValid;
    }


    /**
     * Vytvoří a vrátí novou instanci {@code Calendar} se zadaným rokem, měsícem a dnem.
     *
     * <p>Metoda:
     * - vytvoří nový {@code Calendar} pomocí {@code Calendar.getInstance()} (používá výchozí časové pásmo),
     * - nastaví rok, měsíc a den podle parametrů,
     * - nastaví čas na začátek dne (00:00:00.000),
     * - vrací plně inicializovaný objekt {@code Calendar} (nová instance, nesdílí stav).
     * <p>
     * Poznámky:
     * - Parametr {@code month} je 0-based (0 = leden, 11 = prosinec).
     * - Parametr {@code day} je 1-based (1..31 podle měsíce).
     * - Metoda nemění žádný stav třídy a je bezpečná pro použití z více míst (vrací novou instanci).
     *
     * @param year  rok, který se má nastavit
     * @param month měsíc (0-11)
     * @param day   den v měsíci (1-31)
     * @return nový {@code Calendar} nastavený na zadané datum na začátku dne (00:00:00.000)
     */
    private Calendar createCalendar(int year, int month, int day) {
        Calendar calendar = getInstance();
        calendar.set(year, month, day, 0, 0, 0);
        calendar.set(MILLISECOND, 0);
        return calendar;
    }


    /**
     * Vytvoří a vrátí instanci `PriceListModel` s nastavenými daty platnosti.
     * <p>
     * Tato metoda vytvoří objekt ceníku pomocí metody `createPriceList()`
     * a nastaví data platnosti podle zadaných parametrů `platnostDO` a `platnostOD`.
     *
     * @param platnostDO Datum platnosti do (v milisekundách), může být null
     * @param platnostOD Datum platnosti od (v milisekundách), může být null
     * @return PriceListModel objekt ceníku s nastavenými daty platnosti
     */
    private PriceListModel createPriceListWithDates(Calendar platnostDO, Calendar platnostOD) {
        PriceListModel priceListModel = createPriceList();
        if (platnostDO != null) {
            priceListModel.setPlatnostDO(platnostDO.getTimeInMillis());
        }
        if (platnostOD != null) {
            priceListModel.setPlatnostOD(platnostOD.getTimeInMillis());
        }

        return priceListModel;
    }


    /**
     * Pomocná statická třída pro operace související s regulovanými cenami (parsování dat, tvorba intervalů, validace).
     *
     * <p>Vlastnosti:
     * - Třída je pouze s&nbsp;statickými metodami a neobsahuje stav; je bezpečná pro použití z více vláken.
     * - Konstruktor je privátní, aby se zabránilo vytváření instancí.
     *
     * <p>Chování metod:
     * - Veřejné metody by měly být čisté (pure) — vrací výsledky bez vedlejších efektů.
     * - Validace vstupů a chybové stavy dokumentovat u jednotlivých metod; preferuje se vracení {@code Optional}
     * nebo jasné fallback hodnoty místo vyhazování Runtime výjimek.
     *
     * <p>Použití:
     * - Volat statické metody přímo: {@code RegulatedPriceUtils.parseCalendar(...)}.
     *
     * <p>Poznámky:
     * - Pokud metoda provádí IO nebo časově náročnou práci, spouštět ji v pozadí (ExecutorService nebo jiný mechanismus),
     * aby nedocházelo k blokování UI vlákna (Android specifika).
     *
     * @see java.util.Optional
     * @since 1.0
     */
    private static class ValidityDateContainer {

        private final int minYear;
        private final int maxYear;
        private final ArrayList<Calendar> dates;


        ValidityDateContainer(int minYear, int maxYear, ArrayList<Calendar> dates) {
            this.minYear = minYear;
            this.maxYear = maxYear;
            this.dates = dates;
        }


        public int getMinYear() {
            return minYear;
        }


        public int getMaxYear() {
            return maxYear;
        }


        public ArrayList<Calendar> getDates() {
            return dates;
        }

    }

}
