package cz.xlisto.elektrodroid.modules.pricelist;


import static cz.xlisto.elektrodroid.databaze.DataPriceListSource.VSE;
import static cz.xlisto.elektrodroid.shp.ShPFilter.AREA;
import static cz.xlisto.elektrodroid.shp.ShPFilter.COMPANY;
import static cz.xlisto.elektrodroid.shp.ShPFilter.DATE_END;
import static cz.xlisto.elektrodroid.shp.ShPFilter.DATE_START;
import static cz.xlisto.elektrodroid.shp.ShPFilter.PRODUKT;
import static cz.xlisto.elektrodroid.shp.ShPFilter.RADA;
import static cz.xlisto.elektrodroid.shp.ShPFilter.SAZBA;

import android.app.Dialog;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import java.util.ArrayList;

import cz.xlisto.elektrodroid.R;
import cz.xlisto.elektrodroid.databaze.DBHelperPriceList;
import cz.xlisto.elektrodroid.databaze.DataPriceListSource;
import cz.xlisto.elektrodroid.shp.ShPFilter;


public class PriceListFilterDialogFragment extends DialogFragment {
    private final static String TAG = PriceListFilterDialogFragment.class.getSimpleName();
    public static final String FLAG_RESULT_FILTER_DIALOG_FRAGMENT = "flagPriceListFilterDialogFragment";
    public static final String RESULT = "result";
    private Spinner spRada, spProdukt, spSazba, spCompany, spArea, spDateStart, spDateEnd;
    private String rada, produkt, sazba, dodavatel, uzemi, dateStart = "", dateEnd = "";

    private ShPFilter shpFilter;


    public PriceListFilterDialogFragment() {
        // Empty constructor required for DialogFragment
    }


    public static PriceListFilterDialogFragment newInstance() {
        return new PriceListFilterDialogFragment();
    }


    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        shpFilter = new ShPFilter(getContext());
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        View dialogView = getLayoutInflater().inflate(R.layout.fragment_price_list_filter, null);

        spRada = dialogView.findViewById(R.id.spRada);
        spProdukt = dialogView.findViewById(R.id.spProdukt);
        spSazba = dialogView.findViewById(R.id.spSazba);
        spCompany = dialogView.findViewById(R.id.spCompany);
        spArea = dialogView.findViewById(R.id.spArea);
        spDateStart = dialogView.findViewById(R.id.spDatumStart);
        spDateEnd = dialogView.findViewById(R.id.spDatumEnd);
        Button btnResetFilter = dialogView.findViewById(R.id.btnResetFilter);

        btnResetFilter.setOnClickListener(v -> setItemSpinnerAll());

        builder.setView(dialogView);
        builder.setTitle(R.string.filtr_ceniku);
        builder.setPositiveButton(R.string.filtr_nastav, (dialog, which) -> {
            savePreferences();
            Bundle bundle = new Bundle();
            bundle.putBoolean(RESULT, true);
            getParentFragmentManager().setFragmentResult(FLAG_RESULT_FILTER_DIALOG_FRAGMENT, bundle);
        });
        builder.setNegativeButton(R.string.zrusit, (dialog, which) -> {
        });
        loadPreferences();
        setDataFromDatabase();

        return builder.create();
    }


    /**
     * Načte seznam ceníků podle RADA, PRODUKT, SAZBA, FIRMA, DISTRIBUCE, PLATNOST_OD a nastaví data do příslušných spinnerů
     */
    private void setDataFromDatabase() {
        DataPriceListSource dataPriceListSource = new DataPriceListSource(requireContext());
        dataPriceListSource.open();
        ArrayList<String> arrayListRada = dataPriceListSource.readPriceListCount(DBHelperPriceList.RADA);
        ArrayList<String> arrayListProdukt = dataPriceListSource.readPriceListCount(DBHelperPriceList.PRODUKT);
        ArrayList<String> arrayListSazba = dataPriceListSource.readPriceListCount(DBHelperPriceList.SAZBA);
        ArrayList<String> arrayListDodavatel = dataPriceListSource.readPriceListCount(DBHelperPriceList.FIRMA);
        ArrayList<String> arrayListUzemi = dataPriceListSource.readPriceListCount(DBHelperPriceList.DISTRIBUCE);
        ArrayList<String> arrayListDateStart = dataPriceListSource.readPriceListCount(DBHelperPriceList.PLATNOST_OD);
        ArrayList<String> arrayListDateEnd = dataPriceListSource.readPriceListCount(DBHelperPriceList.PLATNOST_DO);
        dataPriceListSource.close();


        spRada.setAdapter(setStringAdapter(arrayListRada));
        spProdukt.setAdapter(setStringAdapter(arrayListProdukt));
        spSazba.setAdapter(setStringAdapter(arrayListSazba));
        spCompany.setAdapter(setStringAdapter(arrayListDodavatel));
        spArea.setAdapter(setStringAdapter(arrayListUzemi));
        spDateStart.setAdapter(setStringAdapter(arrayListDateStart));
        spDateEnd.setAdapter(setStringAdapter(arrayListDateEnd));

        setItemSpinner(arrayListRada, spRada, rada);
        setItemSpinner(arrayListProdukt, spProdukt, produkt);
        setItemSpinner(arrayListSazba, spSazba, sazba);
        setItemSpinner(arrayListDodavatel, spCompany, dodavatel);
        setItemSpinner(arrayListUzemi, spArea, uzemi);
        setItemSpinner(arrayListDateStart, spDateStart, dateStart);
        setItemSpinner(arrayListDateEnd, spDateEnd, dateEnd);

        spRada.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

            }


            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        spDateStart.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if(position==0) {
                    spDateEnd.setSelection(0);
                    spDateEnd.setEnabled(false);
                } else {
                    spDateEnd.setEnabled(true);
                }
            }


            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }


    /**
     * Vytvoří pro spinner adapter z arraylist
     *
     * @param arrayList - arraylist
     * @return ArrayAdapter
     */
    private ArrayAdapter<String> setStringAdapter(ArrayList<String> arrayList) {
        return new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, arrayList);
    }


    /**
     * Uloží hodnoty spinnerů do sharedprefences
     */
    private void savePreferences() {
        rada = spRada.getSelectedItem().toString();
        produkt = spProdukt.getSelectedItem().toString();
        sazba = spSazba.getSelectedItem().toString();
        dodavatel = spCompany.getSelectedItem().toString();
        uzemi = spArea.getSelectedItem().toString();
        dateStart = spDateStart.getSelectedItem().toString();
        dateEnd = spDateEnd.getSelectedItem().toString();
        if (rada.equals(VSE))
            rada = "%";
        if (produkt.equals(VSE))
            produkt = "%";
        if (sazba.equals(VSE))
            sazba = "%";
        if (dodavatel.equals(VSE))
            dodavatel = "%";
        if (uzemi.equals(VSE))
            uzemi = "%";
        if (dateStart.equals(VSE))
            dateStart = "%";
        if (dateEnd.equals(VSE))
            dateEnd = "%";

        shpFilter.set(RADA, rada);
        shpFilter.set(PRODUKT, produkt);
        shpFilter.set(SAZBA, sazba);
        shpFilter.set(COMPANY, dodavatel);
        shpFilter.set(AREA, uzemi);
        shpFilter.set(DATE_START, dateStart);
        shpFilter.set(DATE_END, dateEnd);
    }


    /**
     * Načte hodnoty ze sharedpreferences, které se mají nastavit na spinnerech
     */
    private void loadPreferences() {
        rada = shpFilter.get(RADA, VSE);
        produkt = shpFilter.get(PRODUKT, VSE);
        sazba = shpFilter.get(SAZBA, VSE);
        dodavatel = shpFilter.get(COMPANY, VSE);
        uzemi = shpFilter.get(AREA, VSE);
        dateStart = shpFilter.get(DATE_START, VSE);
        dateEnd = shpFilter.get(DATE_END, VSE);
    }


    /**
     * Zjistí index a arraylistu vybrané položky ve spinneru, pokud nenalezne,
     * bude nastaven na první položku [VŠE]
     *
     * @param arrayList - arraylist
     * @param sp        - spinner
     * @param item      - vybraná položka
     */
    private void setItemSpinner(ArrayList<String> arrayList, Spinner sp, String item) {
        for (int i = 0; i < arrayList.size(); i++) {
            if (arrayList.get(i).equals(item)) {
                sp.setSelection(i);
            }
        }
    }


    private void setItemSpinnerAll() {
        spRada.setSelection(0);
        spDateStart.setSelection(0);
        spDateEnd.setSelection(0);
        spSazba.setSelection(0);
        spProdukt.setSelection(0);
        spCompany.setSelection(0);
        spArea.setSelection(0);
    }
}
