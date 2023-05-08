package cz.xlisto.odecty.modules.pricelist;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import cz.xlisto.odecty.utils.Keyboard;
import cz.xlisto.odecty.shp.ShPAddPriceList;
import cz.xlisto.odecty.databaze.DataPriceListSource;

import android.os.Handler;
import android.view.View;
import android.widget.AdapterView;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link PriceListAddFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class PriceListAddFragment extends PriceListAddEditAbstract {
    private final static String TAG = PriceListAddFragment.class.getSimpleName();
    private ShPAddPriceList shPAddPriceList;

    public PriceListAddFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment PriceListAddFragment.
     */
    public static PriceListAddFragment newInstance() {
        return new PriceListAddFragment();
    }


    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        shPAddPriceList = new ShPAddPriceList(getActivity());

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
     * Uloží nový ceník do databáze
     */
    private void savePriceList() {
        if (spDistribucniUzemi.getSelectedItem().toString().equals(arrayDistUzemi[0]) || spSazba.getSelectedItem().toString().equals(arraySazba[0])) {
            //pokud není vybráno distrbuční uzemí nebo sazba k uložení nedojde
            return;
        }

        DataPriceListSource dataPriceListSource = new DataPriceListSource(getActivity());
        dataPriceListSource.open();
        long id = dataPriceListSource.insertPriceList(createPriceList());
        dataPriceListSource.close();
        //-1 - chyba
        //kladné číslo - položka vytvořena
        if (id > 0)
            getParentFragmentManager().popBackStack();
    }

    /**
     * Uloží hodnoty tlačítek platností, textu dodavatele, rady a position spinneru distrubučního území
     */
    private void setPreference() {
        shPAddPriceList.set(ShPAddPriceList.PLATNOST_OD, btnFrom.getText().toString());
        shPAddPriceList.set(ShPAddPriceList.PLATNOST_DO, btnUntil.getText().toString());
        shPAddPriceList.set(ShPAddPriceList.DODAVATEL, ivDodavatel.getText());
        shPAddPriceList.set(ShPAddPriceList.RADA, ivRada.getText());
        shPAddPriceList.set(ShPAddPriceList.DIST_UZEMI, spDistribucniUzemi.getSelectedItemPosition());
        shPAddPriceList.set(ShPAddPriceList.SAZBA, spSazba.getSelectedItemPosition());
    }

    /**
     * Načte hodnoty tlačítek platností, textu dodavatele, rady a position spinneru distrubučního území
     */
    private void getPreference() {
        btnFrom.setText(shPAddPriceList.get(ShPAddPriceList.PLATNOST_OD, "01.01." + getYearBtnStart()));
        btnUntil.setText(shPAddPriceList.get(ShPAddPriceList.PLATNOST_DO, "31.12." + getYearBtnStart()));
        ivDodavatel.setDefaultText(shPAddPriceList.get(ShPAddPriceList.DODAVATEL, ""));
        ivRada.setDefaultText(shPAddPriceList.get(ShPAddPriceList.RADA, ""));
        Handler handler = new Handler();
        final Runnable r = () -> {
            //spDistribucniUzemi.setSelection(shPAddPriceList.get(ShPAddPriceList.DIST_UZEMI, 0));
            //spDistribucniUzemi.setSelection(shPAddPriceList.get(ShPAddPriceList.SAZBA, 0));
        };
        handler.postDelayed(r,1200);
    }
}