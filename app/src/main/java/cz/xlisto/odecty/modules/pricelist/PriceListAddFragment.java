package cz.xlisto.odecty.modules.pricelist;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import cz.xlisto.odecty.utils.ReadRawJSON;
import cz.xlisto.odecty.shp.ShPAddPriceList;
import cz.xlisto.odecty.databaze.DataPriceListSource;
import cz.xlisto.odecty.models.PriceListModel;

import android.os.Handler;
import android.view.View;
import android.widget.AdapterView;

import static cz.xlisto.odecty.format.DecimalFormatHelper.df2;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link PriceListAddFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class PriceListAddFragment extends PriceListAddEditAbstract {
    private static String TAG = PriceListAddFragment.class.getSimpleName();
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
        PriceListAddFragment fragment = new PriceListAddFragment();
        return fragment;
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
            getParentFragmentManager().popBackStack();
        });//vrácení o fragment zpět

    }

    @Override
    public void onResume() {
        super.onResume();
        if (closedDialog) {
            setRegulPrice();
            closedDialog = false;
        }
    }

    /**
     * Načte regulované ceny
     */
    private void setRegulPrice() {
        ReadRawJSON readRawJSON = new ReadRawJSON(getActivity());
        Handler handler = new Handler();
        final Runnable r = new Runnable() {
            @Override
            public void run() {

                PriceListModel priceListModel = readRawJSON.read(year, spDistribucniUzemi.getSelectedItem().toString(),
                        spSazba.getSelectedItem().toString());

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

                ivSystemSluzby.setDefaultText(df2.format(priceListModel.getSystemSluzby()));
                ivCinnostOperatora.setDefaultText(df2.format(priceListModel.getCinnost()));
                ivPOZE1.setDefaultText(df2.format(priceListModel.getPoze1()));
                ivPOZE2.setDefaultText(df2.format(priceListModel.getPoze2()));
                ivDan.setDefaultText(df2.format(priceListModel.getDan()));
                ivDPH.setDefaultText(df2.format(priceListModel.getDph()));

                if ((priceListModel.getJ10() == 0) && (priceListModel.getJ11() == 0) &&
                        (priceListModel.getJ12() == 0) && (priceListModel.getJ12() == 0)) {
                    switchJistic.setChecked(false);
                } else {
                    switchJistic.setChecked(true);
                }
                hideItemView();
            }
        };
        handler.postDelayed(r,1000);
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
    }

    /**
     * Načte hodnoty tlačítek platností, textu dodavatele, rady a position spinneru distrubučního území
     */
    private void getPreference() {
        btnFrom.setText(shPAddPriceList.get(ShPAddPriceList.PLATNOST_OD, "01.01." + getYear()));
        btnUntil.setText(shPAddPriceList.get(ShPAddPriceList.PLATNOST_DO, "31.12." + getYear()));
        ivDodavatel.setDefaultText(shPAddPriceList.get(ShPAddPriceList.DODAVATEL, ""));
        ivRada.setDefaultText(shPAddPriceList.get(ShPAddPriceList.RADA, ""));
        Handler handler = new Handler();
        final Runnable r = new Runnable() {
            @Override
            public void run() {
                spDistribucniUzemi.setSelection(shPAddPriceList.get(ShPAddPriceList.DIST_UZEMI, 0));
            }
        };
        handler.postDelayed(r,1200);
    }
}