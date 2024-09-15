package cz.xlisto.elektrodroid.modules.pricelist;


import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static cz.xlisto.elektrodroid.format.DecimalFormatHelper.df2;
import static cz.xlisto.elektrodroid.ownview.OwnDatePicker.showDialog;

import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.Fragment;

import java.util.Calendar;

import cz.xlisto.elektrodroid.R;
import cz.xlisto.elektrodroid.databaze.DataPriceListSource;
import cz.xlisto.elektrodroid.dialogs.OwnAlertDialog;
import cz.xlisto.elektrodroid.dialogs.YesNoDialogFragment;
import cz.xlisto.elektrodroid.models.PriceListModel;
import cz.xlisto.elektrodroid.ownview.LabelEditText;
import cz.xlisto.elektrodroid.ownview.ViewHelper;
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

    String[] arrayDistUzemi;
    String[] arraySazba;
    int year, lastYear, selectionDistUzemi, selectionSazba;
    boolean isFirstLoad = true;
    boolean closedDialog = false;
    long itemId;

    Button btnFrom, btnUntil, btnBack, btnSave, btnReloadRegulPriceList;
    LabelEditText ivRada, ivProdukt, ivDodavatel, ivVT, ivNT, ivPlat, ivVT1, ivNT1;
    LabelEditText ivJ0, ivJ1, ivJ2, ivJ3, ivJ4, ivJ5, ivJ6, ivJ7, ivJ8, ivJ9, ivJ10, ivJ11, ivJ12, ivJ13, ivJ14;
    LabelEditText ivOTE, ivCinnostOperatora, ivOZE, ivPOZE1, ivPOZE2, ivSystemSluzby, ivDan, ivDPH;
    SwitchCompat switchJistic;
    Spinner spSazba, spDistribucniUzemi;
    TextView tvNoPriceListDescription;
    MySpinnerDistributorsAdapter adapterDistUzemi, adapterSazba;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
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

        btnFrom.setOnClickListener(v -> showDialog(getActivity(), day -> {
            closedDialog = true;
            btnFrom.setText(day);
            hideItemView();
            lastYear = year;
            getYearBtnStart();
            onResume();
            setRegulPrice();
            showBtnReloadRegulPriceList();
        }, btnFrom.getText().toString()));

        btnUntil.setOnClickListener(v -> showDialog(getActivity(), day -> {
            btnUntil.setText(day);
            setRegulPrice();
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
        getYearBtnStart();
        hideItemView();

        //nastavení adaptéru, výběr první položky, která reprezentuje nápovědu
        setSazbaAdapter();
        setDistribucniUzemiAdapter();
        showBtnReloadRegulPriceList();

        /*
         * Nastaví posluchače pro výsledek dialogu "Provoz nesíťové infrastruktury" - změny ceníku od 1.7.2024.
         * <p>
         * Tato metoda nastaví posluchače pro výsledek dialogu s klíčem `FLAG_RESULT_DIALOG_PROVOZ_NESITOVE_INFRASTRUKTURY`.
         * Pokud je výsledek dialogu kladný, vytvoří dva objekty `PriceListModel` s daty platnosti
         * a nastaví cenu činnosti na 9.24 pro druhý ceník. Poté uloží nebo aktualizuje ceníky
         * podle typu fragmentu (`PriceListAddFragment` nebo `PriceListEditFragment`).
         */
        requireActivity().getSupportFragmentManager().setFragmentResultListener(FLAG_RESULT_DIALOG_PROVOZ_NESITOVE_INFRASTRUKTURY, this, (requestKey, result) -> {
            if (result.getBoolean(YesNoDialogFragment.RESULT)) {
                Calendar calendarLastJune = createCalendar(5, 30);
                Calendar calendarFirstJuly = createCalendar(6, 1);

                PriceListModel priceListModelFirst = createPriceListWithDates(calendarLastJune.getTimeInMillis(), null);
                PriceListModel priceListModelSecond = createPriceListWithDates(null, calendarFirstJuly.getTimeInMillis());
                priceListModelSecond.setCinnost(9.24);

                if (this.getClass().equals(PriceListAddFragment.class)) {
                    saveNewPriceLists(priceListModelFirst, priceListModelSecond);
                } else if (this.getClass().equals(PriceListEditFragment.class)) {
                    updateAndSavePriceLists(priceListModelFirst, priceListModelSecond);
                }
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
     * <p>
     * Používá `Handler` k opožděnému spuštění kódu, který načte pole sazeb
     * z prostředků a nastaví adaptér pro spinner `spSazba`.
     */
    void setSazbaAdapter() {
        Handler handler = new Handler();
        final Runnable r = () -> {
            arraySazba = getResources().getStringArray(R.array.sazby);
            adapterSazba = new MySpinnerDistributorsAdapter(requireContext(), R.layout.spinner_view, arraySazba, year);
            spSazba.setAdapter(adapterSazba);
            spSazba.setSelection(selectionSazba, true);
        };
        handler.postDelayed(r, 1050);

    }


    /**
     * Nastaví adaptér pro spinner distribučního území.
     * <p>
     * Používá `Handler` k opožděnému spuštění kódu, který načte pole distribučních území
     * z prostředků a nastaví adaptér pro spinner `spDistribucniUzemi`.
     */
    void setDistribucniUzemiAdapter() {
        Handler handler = new Handler();
        final Runnable r = () -> {
            arrayDistUzemi = getResources().getStringArray(R.array.distribucni_uzemi);
            adapterDistUzemi = new MySpinnerDistributorsAdapter(requireContext(), R.layout.spinner_view, arrayDistUzemi, year);
            spDistribucniUzemi.setAdapter(adapterDistUzemi);
            spDistribucniUzemi.setSelection(selectionDistUzemi, true);
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
        if (spDistribucniUzemi.getSelectedItem().toString().equals("PRE")
                || spDistribucniUzemi.getSelectedItem().toString().equals("ČEZ")
                || spDistribucniUzemi.getSelectedItemPosition() == 0
        ) return;

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
     * Získá rok z data vybraného v tlačítku "Platnost od".
     *
     * @return int rok vybraný z data v tlačítku "Platnost od"
     */
    int getYearBtnStart() {
        Calendar calendar = ViewHelper.parseCalendarFromString(btnFrom.getText().toString());
        year = calendar.get(Calendar.YEAR);
        return year;
    }


    /**
     * Získá long z data vybraného v tlačítku "Platnost od".
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
     * Sestaví objekt ceníku z údajů widgetů.
     * <p>
     * Metoda vytvoří a vrátí instanci `PriceListModel` naplněnou daty z různých widgetů
     * uživatelského rozhraní, jako jsou textová pole a spinnery.
     *
     * @return PriceListModel objekt ceníku
     */
    PriceListModel createPriceList() {
        Calendar calendar = Calendar.getInstance();
        long dateCreated = calendar.getTimeInMillis();
        String email = "";
        String autor = "";
        long validityFrom = ViewHelper.parseCalendarFromString(btnFrom.getText().toString()).getTimeInMillis();
        long validityUntil = ViewHelper.parseCalendarFromString(btnUntil.getText().toString()).getTimeInMillis();

        return new PriceListModel(-1L, ivRada.getText(), ivProdukt.getText(), ivDodavatel.getText(),
                ivVT.getDouble(), ivNT.getDouble(), ivPlat.getDouble(), ivDan.getDouble(), spSazba.getSelectedItem().toString(), ivVT1.getDouble(),
                ivNT1.getDouble(), ivJ0.getDouble(), ivJ1.getDouble(), ivJ2.getDouble(), ivJ3.getDouble(), ivJ4.getDouble(),
                ivJ5.getDouble(), ivJ6.getDouble(), ivJ7.getDouble(), ivJ8.getDouble(), ivJ9.getDouble(), ivJ10.getDouble(),
                ivJ11.getDouble(), ivJ12.getDouble(), ivJ13.getDouble(), ivJ14.getDouble(), ivSystemSluzby.getDouble(),
                ivCinnostOperatora.getDouble(), ivPOZE1.getDouble(), ivPOZE2.getDouble(), ivOZE.getDouble(), ivOTE.getDouble(),
                validityFrom, validityUntil, ivDPH.getDouble(), spDistribucniUzemi.getSelectedItem().toString(), autor, dateCreated, email);
    }


    /**
     * Nastaví adaptér pro spinner sazby distribuce.
     * <p>
     * Používá `Handler` k opožděnému spuštění kódu, který načte pole sazeb
     * z prostředků a nastaví adaptér pro spinner `spSazba`.
     */
    void setRegulPrice() {
        if (year < 2021)
            return;
        ReadRawJSON readRawJSON = new ReadRawJSON(getActivity());
        Handler handler = new Handler();
        final Runnable r = () -> {

            PriceListModel priceListModel = readRawJSON.read(year, spDistribucniUzemi.getSelectedItem().toString(),
                    spSazba.getSelectedItem().toString());
            double[] regulPrice = new double[]{priceListModel.getDistVT(), priceListModel.getDistNT(),
                    priceListModel.getJ0(), priceListModel.getJ1(), priceListModel.getJ2(), priceListModel.getJ3(),
                    priceListModel.getJ4(), priceListModel.getJ5(), priceListModel.getJ6(), priceListModel.getJ7(),
                    priceListModel.getJ8(), priceListModel.getJ9(), priceListModel.getJ10(), priceListModel.getJ11(),
                    priceListModel.getJ12(), priceListModel.getJ13(), priceListModel.getJ14(), priceListModel.getSystemSluzby(),
                    priceListModel.getCinnost(), priceListModel.getPoze1(), priceListModel.getPoze2(), priceListModel.getDan(),
                    priceListModel.getDph()
            };
            LabelEditText[] labelEditTexts = new LabelEditText[]{ivVT1, ivNT1,
                    ivJ0, ivJ1, ivJ2, ivJ3,
                    ivJ4, ivJ5, ivJ6, ivJ7,
                    ivJ8, ivJ9, ivJ10, ivJ11,
                    ivJ12, ivJ13, ivJ14, ivSystemSluzby,
                    ivCinnostOperatora, ivPOZE1, ivPOZE2, ivDan,
                    ivDPH};

            for (int i = 0; i < regulPrice.length; i++) {
                labelEditTexts[i].setAllowChangeBackgroundColor(false);
                labelEditTexts[i].setChangedBackgroundEditText(R.drawable.error_edittext_background);
                labelEditTexts[i].setDefaultText(df2.format(regulPrice[i]));
                labelEditTexts[i].setAllowChangeBackgroundColor(true);
            }

            switchJistic.setChecked((priceListModel.getJ10() != 0) || (priceListModel.getJ11() != 0) ||
                    (priceListModel.getJ12() != 0) || (priceListModel.getJ12() != 0));

            hideItemView();

            //změní činnost operátora trhu na provoz nesíťové infrastruktury - od 1.7.2024
            changeTitleOperatorTrhu();
            chanagePriceOperatorTrhu();
        };
        handler.postDelayed(r, 1000);
    }


    /**
     * Zobrazí nebo skryje tlačítko pro načtení regulovaných cen.
     * <p>
     * Na základě roku vybraného v tlačítku "Platnost od" zobrazí nebo skryje
     * tlačítko `btnReloadRegulPriceList` a popis `tvNoPriceListDescription`.
     * Pokud je rok menší než 2021 nebo větší než 2024, tlačítko se skryje a popis se zobrazí.
     * Jinak se tlačítko zobrazí a popis se skryje.
     */
    void showBtnReloadRegulPriceList() {
        String startDate = btnFrom.getText().toString();
        int year = ViewHelper.parseCalendarFromString(startDate).get(Calendar.YEAR);
        if (year < 2021 || year > 2024) {
            btnReloadRegulPriceList.setVisibility(View.GONE);
            tvNoPriceListDescription.setVisibility(View.VISIBLE);
        } else {
            btnReloadRegulPriceList.setVisibility(View.VISIBLE);
            tvNoPriceListDescription.setVisibility(View.GONE);
        }
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

        boolean isValid = isValid(fromDate, untilDate);

        if (!isValid) {
            String title = requireContext().getString(R.string.alert_title);
            String message = requireContext().getString(R.string.alert_message_provoz_nesitove_infrastruktury);

            if (!requireActivity().isFinishing()) {
                //OwnAlertDialog.showDialog(requireActivity(), title, message);
                YesNoDialogFragment yesNoDialogFragment = YesNoDialogFragment.newInstance(title, FLAG_RESULT_DIALOG_PROVOZ_NESITOVE_INFRASTRUKTURY, message);
                yesNoDialogFragment.show(requireActivity().getSupportFragmentManager(), TAG);
            }

        }

        if (untilDate.getTimeInMillis() < fromDate.getTimeInMillis()) {
            OwnAlertDialog.showDialog(requireActivity(), requireContext().getString(R.string.alert_title), requireContext().getString(R.string.alert_message_older_date));
            return true;
        }
        return !isValid;
    }


    /**
     * Zkontroluje, zda jsou data platná podle zadaných podmínek.
     *
     * @param fromDate  Počáteční datum
     * @param untilDate Konečné datum
     * @return true, pokud jsou data platná, jinak false
     */
    private static boolean isValid(Calendar fromDate, Calendar untilDate) {
        //1.1.2024
        Calendar startOf2024 = Calendar.getInstance();
        startOf2024.set(2024, Calendar.JANUARY, 1, 0, 0, 0);
        startOf2024.set(Calendar.MILLISECOND, 0);

        //1.7.2024
        Calendar startOfJuly2024 = Calendar.getInstance();
        startOfJuly2024.set(2024, Calendar.JULY, 1, 0, 0, 0);
        startOfJuly2024.set(Calendar.MILLISECOND, 0);

        //31.12.2024
        Calendar endOf2024 = Calendar.getInstance();
        endOf2024.set(2024, Calendar.DECEMBER, 31, 23, 59, 59);
        endOf2024.set(Calendar.MILLISECOND, 0);

        boolean isValid = true;

        if (fromDate.getTimeInMillis() >= startOf2024.getTimeInMillis() && fromDate.getTimeInMillis() < startOfJuly2024.getTimeInMillis()) {
            if (untilDate.getTimeInMillis() >= startOfJuly2024.getTimeInMillis()) {
                isValid = false;
            }
        } else if (fromDate.getTimeInMillis() >= startOfJuly2024.getTimeInMillis() && fromDate.getTimeInMillis() <= endOf2024.getTimeInMillis()) {
            if (untilDate.getTimeInMillis() > endOf2024.getTimeInMillis()) {
                isValid = false;
            }
        }
        return isValid;
    }


    /**
     * Vytvoří a vrátí instanci kalendáře s nastaveným rokem, měsícem a dnem.
     * <p>
     * Tato metoda vytvoří objekt `Calendar` a nastaví jeho rok, měsíc a den
     * podle zadaných parametrů. Čas je nastaven na začátek dne (00:00:00.000).
     *
     * @param month Měsíc, který má být nastaven (0-11, kde 0 je leden a 11 je prosinec)
     * @param day   Den, který má být nastaven
     * @return Calendar objekt s nastaveným rokem, měsícem a dnem
     */
    private Calendar createCalendar(int month, int day) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(2024, month, day, 0, 0, 0);
        calendar.set(Calendar.MILLISECOND, 0);
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
    private PriceListModel createPriceListWithDates(Long platnostDO, Long platnostOD) {
        PriceListModel priceListModel = createPriceList();
        if (platnostDO != null) {
            priceListModel.setPlatnostDO(platnostDO);
        }
        if (platnostOD != null) {
            priceListModel.setPlatnostOD(platnostOD);
        }
        return priceListModel;
    }


    /**
     * Uloží nové ceníky do databáze.
     * <p>
     * Tato metoda otevře zdroj dat `DataPriceListSource` a vloží dva nové ceníky
     * do databáze. Po dokončení operací zdroj dat uzavře. Pokud jsou obě operace
     * úspěšné (tj. obě ID jsou větší než 0), metoda vrátí fragment o jeden krok
     * zpět v zásobníku fragmentů.
     *
     * @param priceListModelFirst  První ceník, který bude vložen
     * @param priceListModelSecond Druhý ceník, který bude vložen
     */
    private void saveNewPriceLists(PriceListModel priceListModelFirst, PriceListModel priceListModelSecond) {
        DataPriceListSource dataPriceListSource = new DataPriceListSource(requireActivity());
        dataPriceListSource.open();
        long idFirst = dataPriceListSource.insertPriceList(priceListModelFirst);
        long idSecond = dataPriceListSource.insertPriceList(priceListModelSecond);
        dataPriceListSource.close();
        if (idFirst > 0 && idSecond > 0) {
            getParentFragmentManager().popBackStack();
        }
    }


    /**
     * Aktualizuje první ceník a vloží druhý ceník do databáze.
     * <p>
     * Tato metoda nejprve nastaví ID prvního ceníku na hodnotu `itemId`.
     * Poté otevře zdroj dat `DataPriceListSource` a provede aktualizaci prvního ceníku
     * a vložení druhého ceníku do databáze. Po dokončení operací zdroj dat uzavře.
     * Pokud jsou obě operace úspěšné (tj. obě ID jsou větší než 0), metoda vrátí
     * fragment o jeden krok zpět v zásobníku fragmentů.
     *
     * @param priceListModelFirst  První ceník, který bude aktualizován
     * @param priceListModelSecond Druhý ceník, který bude vložen
     */
    private void updateAndSavePriceLists(PriceListModel priceListModelFirst, PriceListModel priceListModelSecond) {
        priceListModelFirst.setId(itemId);
        DataPriceListSource dataPriceListSource = new DataPriceListSource(requireActivity());
        dataPriceListSource.open();
        long idFirst = dataPriceListSource.updatePriceList(priceListModelFirst, itemId);
        long idSecond = dataPriceListSource.insertPriceList(priceListModelSecond);
        dataPriceListSource.close();
        if (idFirst > 0 && idSecond > 0) {
            getParentFragmentManager().popBackStack();
        }
    }

}
