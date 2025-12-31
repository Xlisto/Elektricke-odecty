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

import cz.xlisto.elektrodroid.R;
import cz.xlisto.elektrodroid.databaze.DataPriceListSource;
import cz.xlisto.elektrodroid.dialogs.OwnAlertDialog;
import cz.xlisto.elektrodroid.models.PriceListModel;
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
 * - validace a aktualizace záznamu přes {@link #updatePriceList(long)}
 * <p>
 * Poznámky implementace:
 * - používá {@code DataPriceListSource} pro čtení a zápis dat
 * - spinnery jsou nastaveny asynchronně v {@code setSpinners(PriceListModel)}
 * - zachází s kontrolou datových podmínek a skrytím volitelných polí
 *
 * @see PriceListAddEditAbstract
 * @see #newInstance(long)
 */
public class PriceListEditFragment extends PriceListAddEditAbstract {

    private final static String TAG = "PriceListEditFragment";
    private final static String IS_FIRST_LOAD = "isFirstLoad";
    private PriceListModel priceListModel;
    private static final String ARG_ID = "id";


    public PriceListEditFragment() {
        // Required empty public constructor
    }


    /**
     * Použijte tuto tovární metodu k vytvoření nové instance
     * tohoto fragmentu pomocí poskytnutých parametrů.
     *
     * @return Nová instance fragmentu PriceListEditFragment.
     */
    public static PriceListEditFragment newInstance(long id) {
        PriceListEditFragment fragment = new PriceListEditFragment();
        Bundle args = new Bundle();
        args.putLong(ARG_ID, id);
        fragment.setArguments(args);
        return fragment;
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            itemId = getArguments().getLong(ARG_ID);
        }
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        if (savedInstanceState != null)
            isFirstLoad = savedInstanceState.getBoolean(IS_FIRST_LOAD);
        return inflater.inflate(R.layout.fragment_price_list_add_edit, container, false);
    }


    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (savedInstanceState == null) {
            loadItemPrice(itemId);
            setItemPrice();
        }
        btnSave.setOnClickListener(v -> {
            if (updatePriceList(itemId) > 0) {
                Keyboard.hide(requireActivity());
                getParentFragmentManager().popBackStack();
            }
        });
    }


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
     * - Nakonec volá {@link #setSpinners(PriceListModel)} pro nastavení spinnerů.
     * <p>
     * Vedlejší efekty:
     * - Mění stav UI komponent; metoda předpokládá, že fragment je připojen a všechna view jsou inicializována.
     *
     * @throws NullPointerException pokud {@code priceListModel} nebo některé widgety jsou {@code null}
     */
    private void setItemPrice() {
        btnFrom.setText(ViewHelper.convertLongToDate(priceListModel.getPlatnostOD()));
        btnUntil.setText(ViewHelper.convertLongToDate(priceListModel.getPlatnostDO()));
        ivRada.setDefaultText(priceListModel.getRada());
        ivProdukt.setDefaultText(priceListModel.getProdukt());
        ivDodavatel.setDefaultText(priceListModel.getFirma());
        ivVT.setDefaultText(df2.format(priceListModel.getCenaVT()));
        ivNT.setDefaultText(df2.format(priceListModel.getCenaNT()));
        ivPlat.setDefaultText(df2.format(priceListModel.getMesicniPlat()));
        ivVT1.setDefaultText(df2.format(priceListModel.getDistVT()));
        ivNT1.setDefaultText(df2.format(priceListModel.getDistNT()));
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
        ivOTE.setDefaultText(df2.format(priceListModel.getOte()));
        ivCinnostOperatora.setDefaultText(df2.format(priceListModel.getCinnost()));
        ivOZE.setDefaultText(df2.format(priceListModel.getOze()));
        ivPOZE1.setDefaultText(df2.format(priceListModel.getPoze1()));
        ivPOZE2.setDefaultText(df2.format(priceListModel.getPoze2()));
        ivSystemSluzby.setDefaultText(df2.format(priceListModel.getSystemSluzby()));
        ivDan.setDefaultText(df2.format(priceListModel.getDan()));
        ivDPH.setDefaultText(df2.format(priceListModel.getDph()));
        if (priceListModel.getJ10() == 0 && priceListModel.getJ11() == 0
                && priceListModel.getJ12() == 0 && priceListModel.getJ13() == 0
                && priceListModel.getJ14() == 0) {
            switchJistic.setChecked(false);
            hideItemView();
        } else {
            switchJistic.setChecked(true);
            hideItemView();
        }
        setSpinners(priceListModel);
    }


    /**
     * Nastaví spinnery distribučního území a sazby podle hodnot v předaném
     * {@code PriceListModel} s drobným zpožděním, aby měly adaptéry čas na inicializaci.
     * <p>
     * Popis:
     * - Spouští operaci na hlavním vlákně pomocí {@link android.os.Handler} s {@link android.os.Looper#getMainLooper()}
     * a {@code postDelayed(...)} (prodleva 1300 ms).
     * - Před manipulací s UI ověřuje {@link #isAdded()} a kontroluje, že {@code priceListModel}
     * i cílové spinnery nejsou {@code null}, aby se zabránilo {@link NullPointerException}
     * nebo {@code IllegalStateException}.
     * - Načítá pole z resources: {@code R.array.distribucni_uzemi} a {@code R.array.sazby}
     * a pro nastavení výběru volá pomocnou metodu {@link #compare(String[], android.widget.Spinner, String)}.
     * <p>
     * Poznámky:
     * - Všechny UI operace probíhají na hlavním vlákně.
     * - Prodleva je záměrná, aby bylo jisté, že adaptéry spinnerů jsou inicializované.
     * - Metoda neprovádí I/O v pozadí; načítání resources je rychlé, ale kontroluje životní cyklus fragmentu.
     *
     * @param priceListModel model ceníku obsahující hodnoty pro nastavení spinnerů; pokud je {@code null}, metoda nic neprovádí
     */
    private void setSpinners(PriceListModel priceListModel) {
        Handler handler = new Handler(android.os.Looper.getMainLooper());
        final Runnable r = () -> {
            if (!isAdded())
                return; // ochrana proti IllegalStateException když fragment už není připojen

            String[] distribucniUzemi = getResources().getStringArray(R.array.distribucni_uzemi);
            String[] sazby = getResources().getStringArray(R.array.sazby);

            compare(distribucniUzemi, spDistribucniUzemi, priceListModel.getDistribuce());
            compare(sazby, spSazba, priceListModel.getSazba());
        };
        handler.postDelayed(r, 1300);
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
        if (spDistribucniUzemi.getSelectedItem().toString().equals(arrayDistUzemi[0]) || spSazba.getSelectedItem().toString().equals(arraySazba[0])) {
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
