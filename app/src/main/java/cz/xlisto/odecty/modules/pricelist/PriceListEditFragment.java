package cz.xlisto.odecty.modules.pricelist;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import cz.xlisto.odecty.R;
import cz.xlisto.odecty.databaze.DataPriceListSource;
import cz.xlisto.odecty.models.PriceListModel;
import cz.xlisto.odecty.ownview.ViewHelper;
import cz.xlisto.odecty.utils.Keyboard;

import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Spinner;

import static cz.xlisto.odecty.format.DecimalFormatHelper.df2;


/**
 * Fragment pro úpravu ceníku.
 */
public class PriceListEditFragment extends PriceListAddEditAbstract {
    private final static String TAG = "PriceListEditFragment";
    private final static String IS_FIRST_LOAD = "isFirstLoad";
    private PriceListModel priceListModel;
    private static final String ARG_ID = "id";
    private long itemId;

    public PriceListEditFragment() {
        // Required empty public constructor
    }


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
            getYearBtnStart();
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
     * Nastaví spinnery distribuční uzemí a sazba podle nalezeného údaje v ceníku
     *
     * @param priceListModel PriceListModel ceníku
     */
    private void setSpinners(PriceListModel priceListModel) {
        Handler handler = new Handler();
        final Runnable r = () -> {
            String[] distribucniUzemi = getResources().getStringArray(R.array.distribucni_uzemi);
            String[] sazby = getResources().getStringArray(R.array.sazby);

            compare(distribucniUzemi, spDistribucniUzemi, priceListModel.getDistribuce());
            compare(sazby, spSazba, priceListModel.getSazba());
        };
        handler.postDelayed(r, 1300);
    }


    /**
     * Porovná pole stringu načtený ze spinneru s hledaným stringem. Při shodě nastaví položku na spinneru
     *
     * @param strings       pole stringů
     * @param sp            spinner, pro který se nastaví nalezená položka
     * @param searchString  hledaný string
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
     * Upraví ceník vybraný podle itemId
     *
     * @param itemId long id ceníku
     */
    private long updatePriceList(long itemId) {
        if (spDistribucniUzemi.getSelectedItem().toString().equals(arrayDistUzemi[0]) || spSazba.getSelectedItem().toString().equals(arraySazba[0])) {
            //pokud není vybráno distribuční uzemí nebo sazba k uložení nedojde
            return 0L;
        }
        DataPriceListSource dataPriceListSource = new DataPriceListSource(getActivity());
        dataPriceListSource.open();
        long id = dataPriceListSource.updatePriceList(createPriceList(), itemId);
        dataPriceListSource.close();
        return id;
    }
}
