package cz.xlisto.elektrodroid.modules.pricelist;


import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import cz.xlisto.elektrodroid.R;
import cz.xlisto.elektrodroid.databaze.DataPriceListSource;
import cz.xlisto.elektrodroid.dialogs.OwnAlertDialog;
import cz.xlisto.elektrodroid.shp.ShPAddPriceList;
import cz.xlisto.elektrodroid.utils.Keyboard;


/**
 * Fragment pro vytvoření nového ceníku
 * Jednoduchý {@link Fragment} podtřída.
 * Použijte tovární metodu {@link PriceListAddFragment#newInstance}
 * k vytvoření instance tohoto fragmentu.
 */
public class PriceListAddFragment extends PriceListAddEditAbstract {
    private final static String TAG = PriceListAddFragment.class.getSimpleName();
    private ShPAddPriceList shPAddPriceList;


    public PriceListAddFragment() {
        // Required empty public constructor
    }


    /**
     * Použijte tuto tovární metodu k vytvoření nové instance
     * tohoto fragmentu pomocí poskytnutých parametrů.
     *
     * @return Nová instance fragmentu PriceListAddFragment.
     */
    public static PriceListAddFragment newInstance() {
        return new PriceListAddFragment();
    }


    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        shPAddPriceList = new ShPAddPriceList(requireActivity());
        btnSave.setOnClickListener(v -> {
            setPreference();
            savePriceList();
        });
        if (savedInstanceState == null) {
            setRegulPrice();
            getPreference();
        }
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
        btnBack.setOnClickListener(v -> {
            setPreference();
            Keyboard.hide(requireActivity());
            getParentFragmentManager().popBackStack();
        });//vrácení o fragment zpět
    }


    @Override
    public void onResume() {
        super.onResume();
        changeDistributionSpinner();

        if (closedDialog) {
            setRegulPrice();
            closedDialog = false;
        }
    }


    /**
     * Uloží nový ceník do databáze.
     * <p>
     * Pokud není vybráno distribuční území nebo sazba, k uložení nedojde.
     * Po úspěšném uložení se fragment vrátí o jeden krok zpět.
     */
    private void savePriceList() {
        if (spDistribucniUzemi.getSelectedItem().toString().equals(arrayDistUzemi[0]) || spSazba.getSelectedItem().toString().equals(arraySazba[0])) {
            //pokud není vybráno distribuční uzemí nebo sazba k uložení nedojde
            OwnAlertDialog.showDialog(requireActivity(), getString(R.string.alert_title), getString(R.string.alert_message_select_area));
            return;
        }
        //kontrola platnosti datumů a zobrazení dialogového dotazu na rozdělění ceníku
        if(checkDateConditions())
            return;
        DataPriceListSource dataPriceListSource = new DataPriceListSource(requireActivity());
        dataPriceListSource.open();
        long id = dataPriceListSource.insertPriceList(createPriceList());
        dataPriceListSource.close();
        //-1 - chyba
        //kladné číslo - položka vytvořena
        if (id > 0)
            getParentFragmentManager().popBackStack();
    }


    /**
     * Uloží hodnoty tlačítek platností, textu dodavatele, rady a position spinneru distribučního území
     */
    private void setPreference() {
        shPAddPriceList.set(ShPAddPriceList.PLATNOST_OD, btnFrom.getText().toString());
        shPAddPriceList.set(ShPAddPriceList.PLATNOST_DO, btnUntil.getText().toString());
        shPAddPriceList.set(ShPAddPriceList.DODAVATEL, ivDodavatel.getText());
        shPAddPriceList.set(ShPAddPriceList.PRODUKT, ivProdukt.getText());
        shPAddPriceList.set(ShPAddPriceList.RADA, ivRada.getText());
        shPAddPriceList.set(ShPAddPriceList.DIST_UZEMI, spDistribucniUzemi.getSelectedItemPosition());
        shPAddPriceList.set(ShPAddPriceList.SAZBA, spSazba.getSelectedItemPosition());
    }


    /**
     * Načte hodnoty tlačítek platností, textu dodavatele, rady a position spinneru distribučního území
     */
    private void getPreference() {
        btnFrom.setText(shPAddPriceList.get(ShPAddPriceList.PLATNOST_OD, "01.01." + getYearBtnStart()));
        btnUntil.setText(shPAddPriceList.get(ShPAddPriceList.PLATNOST_DO, "31.12." + getYearBtnStart()));
        ivDodavatel.setDefaultText(shPAddPriceList.get(ShPAddPriceList.DODAVATEL, ""));
        ivProdukt.setDefaultText(shPAddPriceList.get(ShPAddPriceList.PRODUKT, ""));
        ivRada.setDefaultText(shPAddPriceList.get(ShPAddPriceList.RADA, ""));
    }
}