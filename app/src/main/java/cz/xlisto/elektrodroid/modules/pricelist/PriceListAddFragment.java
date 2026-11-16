package cz.xlisto.elektrodroid.modules.pricelist;


import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import cz.xlisto.elektrodroid.R;
import cz.xlisto.elektrodroid.databaze.DataPriceListSource;
import cz.xlisto.elektrodroid.dialogs.OwnAlertDialog;
import cz.xlisto.elektrodroid.shp.ShPAddPriceList;
import cz.xlisto.elektrodroid.utils.Keyboard;


/**
 * Fragment pro vytvoření nového ceníku.
 * <p>
 * Rozšiřuje {@link PriceListAddEditAbstract} a zajišťuje inicializaci UI formuláře,
 * ukládání dočasných preferencí uživatele a vložení nového záznamu do databáze.
 * <p>
 * Hlavní chování:
 * - vytvoření instance pomocí {@link #newInstance()}
 * - nastavení posluchačů pro uložení (btnSave) a návrat (btnBack)
 * - persistování výběrů a textů přes {@link ShPAddPriceList}
 * - validace dat a vložení záznamu přes {@link DataPriceListSource#insertPriceList}
 * - aktualizace spinnerů a výpočet regulované ceny (metoda {@link #setRegulPrice()})
 *
 * @see PriceListAddEditAbstract
 * @see DataPriceListSource
 * @see ShPAddPriceList
 */
public class PriceListAddFragment extends PriceListAddEditAbstract {

    private ShPAddPriceList shPAddPriceList;


    /**
     * Vytvoří novou instanci {@link PriceListAddFragment}.
     */
    public PriceListAddFragment() {
        // Required empty public constructor
    }


    /**
     * Vytvoří novou instanci fragmentu {@link PriceListAddFragment}.
     *
     * @return nová instance fragmentu {@link PriceListAddFragment}
     */
    public static PriceListAddFragment newInstance() {
        return new PriceListAddFragment();
    }


    /**
     * Inicializuje preferenční úložiště {@link ShPAddPriceList}, nastaví posluchače tlačítek pro uložení a návrat,
     * načte uložené preference uživatele a registruje posluchače výběru pro přepínání regulovaných cen.
     *
     * @param view               pohled fragmentu
     * @param savedInstanceState uložený stav fragmentu
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        shPAddPriceList = new ShPAddPriceList(requireActivity());
        btnSave.setOnClickListener(v -> {
            setPreference();
            savePriceList();
        });
        if (savedInstanceState == null) {
            getPreference();
            setRegulPrice();
        }
        spRate.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                setRegulPrice();
            }


            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        spDistArea.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
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


    /**
     * Obnoví distribuční spinnery a přepočítá regulované ceny při návratu do fragmentu
     * nebo po zavření dialogu s podrobnostmi.
     */
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
        if (spDistArea.getSelectedItem().toString().equals(arrayDistArea[0]) || spRate.getSelectedItem().toString().equals(arrayRate[0])) {
            //pokud není vybráno distribuční uzemí nebo sazba k uložení nedojde
            OwnAlertDialog.showDialog(requireActivity(), getString(R.string.alert_title), getString(R.string.alert_message_select_area));
            return;
        }
        //kontrola platnosti datumů a zobrazení dialogového dotazu na rozdělení ceníku

        if (checkDateConditions())
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
     * Uloží dočasné hodnoty formuláře (platnost od/do, dodavatel, produkt, řada, distribuční území a sazba)
     * do preferencí {@link ShPAddPriceList}.
     */
    private void setPreference() {
        shPAddPriceList.set(ShPAddPriceList.PLATNOST_OD, btnFrom.getText().toString());
        shPAddPriceList.set(ShPAddPriceList.PLATNOST_DO, btnUntil.getText().toString());
        shPAddPriceList.set(ShPAddPriceList.DODAVATEL, ivCompany.getText());
        shPAddPriceList.set(ShPAddPriceList.PRODUKT, ivProduct.getText());
        shPAddPriceList.set(ShPAddPriceList.RADA, ivSeries.getText());
        shPAddPriceList.set(ShPAddPriceList.DIST_UZEMI, spDistArea.getSelectedItemPosition());
        shPAddPriceList.set(ShPAddPriceList.SAZBA, spRate.getSelectedItemPosition());
    }


    /**
     * Načte uložené dočasné hodnoty formuláře z preferencí {@link ShPAddPriceList} a uplatní je na ovládací prvky.
     */
    private void getPreference() {
        btnFrom.setText(shPAddPriceList.get(ShPAddPriceList.PLATNOST_OD, "01.01." + getYearBtnStart()));
        btnUntil.setText(shPAddPriceList.get(ShPAddPriceList.PLATNOST_DO, "31.12." + getYearBtnStart()));
        updateValidityRangeButtonText();
        ivCompany.setDefaultText(shPAddPriceList.get(ShPAddPriceList.DODAVATEL, ""));
        ivProduct.setDefaultText(shPAddPriceList.get(ShPAddPriceList.PRODUKT, ""));
        ivSeries.setDefaultText(shPAddPriceList.get(ShPAddPriceList.RADA, ""));
    }

}