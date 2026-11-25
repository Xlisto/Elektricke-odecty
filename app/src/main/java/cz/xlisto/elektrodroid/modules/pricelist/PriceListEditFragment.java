package cz.xlisto.elektrodroid.modules.pricelist;


import static cz.xlisto.elektrodroid.format.DecimalFormatHelper.df2;

import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Spinner;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import cz.xlisto.elektrodroid.R;
import cz.xlisto.elektrodroid.databaze.DataPriceListSource;
import cz.xlisto.elektrodroid.dialogs.OwnAlertDialog;
import cz.xlisto.elektrodroid.models.PriceListModel;
import cz.xlisto.elektrodroid.ownview.LabelEditText;
import cz.xlisto.elektrodroid.ownview.ViewHelper;
import cz.xlisto.elektrodroid.utils.Keyboard;


/**
 * Fragment pro úpravu existujícího ceníku.
 * <p>
 * Rozšiřuje {@code PriceListAddEditAbstract} a poskytuje načtení záznamu podle
 * předaného id, naplnění vstupních polí hodnotami ceníku a uložení změn zpět do databáze.
 * <p>
 * Hlavní chování:
 * - vytvoření instance pomocí {@link #newInstance(long)}
 * - načtení modelu z databáze v {@code onCreate/onViewCreated}
 * - naplnění UI hodnotami modelu (datum, ceny, sazby, distribuční území)
 * - zvýraznění regulovaných položek, které se liší od oficiálních cen v aplikaci
 * - validace a aktualizace záznamu přes {@link #updatePriceList(long)}
 * <p>
 * Poznámky implementace:
 * - používá {@code DataPriceListSource} pro čtení a zápis dat
 * - spinnery jsou nastaveny asynchronně v {@code setSpinners(PriceListModel)} bez přepsání uložených regulovaných cen
 * - zachází s kontrolou datových podmínek a skrytím volitelných polí
 *
 * @see PriceListAddEditAbstract
 * @see #newInstance(long)
 */
public class PriceListEditFragment extends PriceListAddEditAbstract {

    private final static String IS_FIRST_LOAD = "isFirstLoad";
    private PriceListModel priceListModel;
    private static final String ARG_ID = "id";
    private boolean allowSpinnerAutoReload = false;


    /**
     * Vytvoří novou instanci fragmentu pro úpravu ceníku se zadaným identifikátorem.
     *
     * @param id Identifikátor upravovaného ceníku v databázi
     * @return Nová instance fragmentu PriceListEditFragment.
     */
    public static PriceListEditFragment newInstance(long id) {
        PriceListEditFragment fragment = new PriceListEditFragment();
        Bundle args = new Bundle();
        args.putLong(ARG_ID, id);
        fragment.setArguments(args);
        return fragment;
    }


    /**
     * Načte předaný identifikátor ceníku z argumentů fragmentu při jeho vytvoření.
     *
     * @param savedInstanceState Uložený stav fragmentu při opětném vytvoření
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            itemId = getArguments().getLong(ARG_ID);
        }
    }


    /**
     * Vytvoří a obnoví zobrazení fragmentu a načte příznak prvního načtení ze savedInstanceState.
     *
     * @param inflater           Objekt {@link LayoutInflater} pro nafouknutí rozvržení
     * @param container          Rodičovské zobrazení {@link ViewGroup}
     * @param savedInstanceState Uložený stav fragmentu při opětném vytvoření
     * @return Nafouknuté zobrazení fragmentu
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        if (savedInstanceState != null)
            isFirstLoad = savedInstanceState.getBoolean(IS_FIRST_LOAD);
        return inflater.inflate(R.layout.fragment_price_list_add_edit, container, false);
    }


    /**
     * Inicializuje komponenty UI, načte ceník z databáze a nastaví posluchač pro tlačítko uložení.
     *
     * @param view               Kořenové zobrazení fragmentu
     * @param savedInstanceState Uložený stav fragmentu při opětném vytvoření
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        allowSpinnerAutoReload = false;
        if (savedInstanceState == null) {
            loadItemPrice(itemId);
            setItemPrice();
        } else {
            new Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                if (isAdded()) {
                    allowSpinnerAutoReload = true;
                    highlightDifferingRegulatedFields(createPriceListWithCurrentSpinners());
                }
            }, 1350);
        }
        btnSave.setOnClickListener(v -> {
            if (updatePriceList(itemId) > 0) {
                Keyboard.hide(requireActivity());
                getParentFragmentManager().popBackStack();
            }
        });
    }


    @Override
    protected void onSpinnerSelectionChanged() {
        if (allowSpinnerAutoReload) {
            setRegulPrice();
        }
    }


    /**
     * Při obnovení fragmentu zajistí při prvním načtení inicializaci adaptéru distribučního území,
     * nastavení výběru spinnerů a kontrolu automatického přepnutí distributora.
     */
    @Override
    public void onResume() {
        super.onResume();
        if (isFirstLoad) {
            isFirstLoad = false;
            year = getYearBtnStart();
            setDistribucniUzemiAdapter();
            setSpinners(priceListModel);
        }
        changeDistributionSpinner();
    }


    /**
     * Uloží stav fragmentu pro zachování příznaku prvního načtení při změně konfigurace.
     *
     * @param outState {@link Bundle} pro uložení stavu
     */
    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(IS_FIRST_LOAD, isFirstLoad);
    }


    /**
     * Načte stávající údaje ceníku a přiřadí do příslušných widgetů
     *
     * @param id long id ceníku
     */
    private void loadItemPrice(long id) {
        DataPriceListSource dataPriceListSource = new DataPriceListSource(getActivity());
        dataPriceListSource.open();
        priceListModel = dataPriceListSource.readPrice(id);
        dataPriceListSource.close();
    }


    /**
     * Naplní UI widgety hodnotami z aktuálního {@code PriceListModel}.
     * <p>
     * Implementační body:
     * - Převádí datumové hodnoty pomocí {@link cz.xlisto.elektrodroid.ownview.ViewHelper#convertLongToDate(long)}.
     * - Formátuje číselné hodnoty pomocí {@code df2} a nastavuje je do příslušných input view.
     * - Nastavuje textová pole (řada, produkt, dodavatel) a hodnoty sazeb/distancí.
     * - Podle hodnot J10..J14 upraví stav {@code switchJistic} a zavolá {@link #hideItemView()}.
     * - Zvýrazní regulované položky, které se liší od referenčních cen v aplikaci.
     *
     * @throws NullPointerException pokud {@code priceListModel} nebo některé widgety jsou {@code null}
     */
    private void setItemPrice() {
        btnFrom.setText(ViewHelper.convertLongToDate(priceListModel.getValidFrom()));
        btnUntil.setText(ViewHelper.convertLongToDate(priceListModel.getValidTo()));
        year = getYearBtnStart();
        updateValidityRangeButtonText();
        ivSeries.setDefaultText(priceListModel.getSeries());
        ivProduct.setDefaultText(priceListModel.getProduct());
        ivCompany.setDefaultText(priceListModel.getCompany());
        ivVt.setDefaultText(df2.format(priceListModel.getPriceVT()));
        ivNt.setDefaultText(df2.format(priceListModel.getPriceNT()));
        ivPayment.setDefaultText(df2.format(priceListModel.getMonthlyPayment()));
        ivVtRegulated.setDefaultText(df2.format(priceListModel.getDistVT()));
        ivNtRegulated.setDefaultText(df2.format(priceListModel.getDistNT()));
        ivJ0.setDefaultText(df2.format(priceListModel.getJ0()));
        ivJ1.setDefaultText(df2.format(priceListModel.getJ1()));
        ivJ2.setDefaultText(df2.format(priceListModel.getJ2()));
        ivJ3.setDefaultText(df2.format(priceListModel.getJ3()));
        ivJ4.setDefaultText(df2.format(priceListModel.getJ4()));
        ivJ5.setDefaultText(df2.format(priceListModel.getJ5()));
        ivJ6.setDefaultText(df2.format(priceListModel.getJ6()));
        ivJ7.setDefaultText(df2.format(priceListModel.getJ7()));
        ivJ8.setDefaultText(df2.format(priceListModel.getJ8()));
        ivJ9.setDefaultText(df2.format(priceListModel.getJ9()));
        ivJ10.setDefaultText(df2.format(priceListModel.getJ10()));
        ivJ11.setDefaultText(df2.format(priceListModel.getJ11()));
        ivJ12.setDefaultText(df2.format(priceListModel.getJ12()));
        ivJ13.setDefaultText(df2.format(priceListModel.getJ13()));
        ivJ14.setDefaultText(df2.format(priceListModel.getJ14()));
        ivOte.setDefaultText(df2.format(priceListModel.getOte()));
        ivMarketOperatorActivity.setDefaultText(df2.format(priceListModel.getActivity()));
        ivOze.setDefaultText(df2.format(priceListModel.getOze()));
        ivPoze1.setDefaultText(df2.format(priceListModel.getPoze1()));
        ivPoze2.setDefaultText(df2.format(priceListModel.getPoze2()));
        ivSystemServices.setDefaultText(df2.format(priceListModel.getSystemServices()));
        ivTax.setDefaultText(df2.format(priceListModel.getTax()));
        ivVat.setDefaultText(df2.format(priceListModel.getVat()));
        if (priceListModel.getJ10() == 0 && priceListModel.getJ11() == 0
                && priceListModel.getJ12() == 0 && priceListModel.getJ13() == 0
                && priceListModel.getJ14() == 0) {
            switchBreaker.setChecked(false);
            hideItemView();
        } else {
            switchBreaker.setChecked(true);
            hideItemView();
        }
        changeTitleOperatorTrhu();
        highlightDifferingRegulatedFields(priceListModel);
    }


    /**
     * Nastaví spinnery distribučního území a sazby podle hodnot v předaném
     * {@code PriceListModel} s drobným zpožděním, aby měly adaptéry čas na inicializaci.
     * Po nastavení výběru povolí automatické přenačtení regulovaných cen při budoucí ruční změně spinnerů.
     *
     * @param priceListModel model ceníku obsahující hodnoty pro nastavení spinnerů; pokud je {@code null}, metoda nic neprovádí
     */
    private void setSpinners(PriceListModel priceListModel) {
        if (priceListModel == null) {
            return;
        }
        Handler handler = new Handler(android.os.Looper.getMainLooper());
        final Runnable r = () -> {
            if (!isAdded())
                return; // ochrana proti IllegalStateException když fragment už není připojen

            int year = priceListModel.getValidityYear();
            String[] distribucniUzemi = PriceListAddEditAbstract.getDistAreaArrayForYear(year);
            String[] sazby = getResources().getStringArray(R.array.sazby);

            compare(distribucniUzemi, spDistArea, priceListModel.getDistribution());
            compare(sazby, spRate, priceListModel.getRate());
            handler.postDelayed(() -> {
                if (isAdded()) {
                    allowSpinnerAutoReload = true;
                }
            }, 150);
        };
        handler.postDelayed(r, 1300);
    }


    /**
     * Porovná na pozadí uložené regulované položky ceníku s oficiálními regulovanými cenami v aplikaci
     * a odlišná pole v UI vizuálně zvýrazní.
     *
     * @param model model ceníku k porovnání s referenčními cenami
     */
    private void highlightDifferingRegulatedFields(@Nullable PriceListModel model) {
        if (model == null || getContext() == null) {
            return;
        }
        android.content.Context appContext = requireContext().getApplicationContext();
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler mainHandler = new Handler(android.os.Looper.getMainLooper());

        executor.execute(() -> {
            try {
                PriceListModel ref = PriceListOriginHelper.getReferenceRegulatedPriceList(appContext, model);
                mainHandler.post(() -> {
                    if (!isAdded()) return;
                    boolean hasValidReference = ref != null
                            && !(ref.getDistVT() == 0.0 && ref.getJ0() == 0.0)
                            && ref.getVat() > 0.0;

                    LabelEditText[] fields = new LabelEditText[]{
                            ivVtRegulated, ivNtRegulated,
                            ivJ0, ivJ1, ivJ2, ivJ3, ivJ4, ivJ5, ivJ6, ivJ7, ivJ8, ivJ9, ivJ10, ivJ11, ivJ12, ivJ13, ivJ14,
                            ivSystemServices, ivMarketOperatorActivity, ivPoze1, ivPoze2, ivOze, ivOte, ivTax, ivVat
                    };

                    if (!hasValidReference) {
                        for (LabelEditText field : fields) {
                            field.setChangedBackgroundEditText(R.drawable.error_edittext_background);
                            field.setAllowChangeBackgroundColor(true);
                            field.setHighlightChangedBackground(false);
                        }
                        return;
                    }

                    double[] actualValues = new double[]{
                            model.getDistVT(), model.getDistNT(),
                            model.getJ0(), model.getJ1(), model.getJ2(), model.getJ3(), model.getJ4(),
                            model.getJ5(), model.getJ6(), model.getJ7(), model.getJ8(), model.getJ9(),
                            model.getJ10(), model.getJ11(), model.getJ12(), model.getJ13(), model.getJ14(),
                            model.getSystemServices(), model.getActivity(), model.getPoze1(), model.getPoze2(),
                            model.getOze(), model.getOte(), model.getTax(), model.getVat()
                    };
                    double[] refValues = new double[]{
                            ref.getDistVT(), ref.getDistNT(),
                            ref.getJ0(), ref.getJ1(), ref.getJ2(), ref.getJ3(), ref.getJ4(),
                            ref.getJ5(), ref.getJ6(), ref.getJ7(), ref.getJ8(), ref.getJ9(),
                            ref.getJ10(), ref.getJ11(), ref.getJ12(), ref.getJ13(), ref.getJ14(),
                            ref.getSystemServices(), ref.getActivity(), ref.getPoze1(), ref.getPoze2(),
                            ref.getOze(), ref.getOte(), ref.getTax(), ref.getVat()
                    };

                    for (int i = 0; i < fields.length; i++) {
                        boolean differs = !PriceListOriginHelper.nearlyEqual(actualValues[i], refValues[i]);
                        fields[i].setChangedBackgroundEditText(R.drawable.error_edittext_background);
                        fields[i].setAllowChangeBackgroundColor(true);
                        fields[i].setHighlightChangedBackground(differs);
                    }
                });
            } finally {
                executor.shutdown();
            }
        });
    }


    /**
     * Sestaví {@link PriceListModel} z aktuálních hodnot v UI po obnovení stavu, pokud jsou spinnery připravené.
     *
     * @return model ceníku s hodnotami z UI, nebo {@code null} pokud spinnery ještě nemají vybrané položky
     */
    @Nullable
    private PriceListModel createPriceListWithCurrentSpinners() {
        if (spDistArea.getSelectedItem() == null || spRate.getSelectedItem() == null) {
            return null;
        }
        return createPriceList();
    }


    /**
     * Porovná pole řetězců se zadaným hledaným řetězcem a při první shodě nastaví
     * odpovídající položku na předaném `Spinneru`.
     *
     * @param strings      pole řetězců (možnosti spinneru)
     * @param sp           spinner, jehož výběr bude nastaven
     * @param searchString hledaný řetězec
     */
    private void compare(String[] strings, Spinner sp, String searchString) {
        for (int i = 0; i < strings.length; i++) {
            String s = strings[i];
            if (s.equals(searchString)) {
                sp.setSelection(i);
            }
        }
    }


    /**
     * Aktualizuje ceník se zadaným id.
     * <p>
     * Provádí validaci povinných polí (vybrané distribuční území a sazba) a kontrolu datových podmínek.
     * Pokud není vybráno distribuční území nebo sazba, zobrazí varovný dialog a nic neuloží.
     * Pokud kontrola dat (checkDateConditions) zjistí chybu, aktualizace se neprovede.
     * <p>
     * V případě úspěšné validace vytvoří instance DataPriceListSource, otevře ji, provede aktualizaci
     * pomocí createPriceList(), uzavře zdroj a vrátí id aktualizovaného záznamu.
     *
     * @param itemId id ceníku, který se má upravit
     * @return id aktualizovaného záznamu z databáze, nebo 0L pokud aktualizace nebyla provedena kvůli nevalidním vstupům
     */
    private long updatePriceList(long itemId) {
        if (spDistArea.getSelectedItem().toString().equals(arrayDistArea[0]) || spRate.getSelectedItem().toString().equals(arrayRate[0])) {
            //pokud není vybráno distribuční uzemí nebo sazba k uložení nedojde, nemělo by k tomu nikdy dojít
            OwnAlertDialog.showDialog(requireActivity(), getString(R.string.alert_title), getString(R.string.alert_message_select_area));
            return 0L;
        }

        if (checkDateConditions())
            return 0L;
        DataPriceListSource dataPriceListSource = new DataPriceListSource(requireActivity());
        dataPriceListSource.open();
        long id = dataPriceListSource.updatePriceList(createPriceList(), itemId);
        dataPriceListSource.close();
        return id;
    }

}
