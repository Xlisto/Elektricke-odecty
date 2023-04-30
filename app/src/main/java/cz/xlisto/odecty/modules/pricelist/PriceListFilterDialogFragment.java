package cz.xlisto.odecty.modules.pricelist;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;

import java.util.ArrayList;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import cz.xlisto.odecty.R;
import cz.xlisto.odecty.shp.ShPFilter;
import cz.xlisto.odecty.databaze.DBHelperPriceList;
import cz.xlisto.odecty.databaze.DataPriceListSource;

import static cz.xlisto.odecty.shp.ShPFilter.DATUM;
import static cz.xlisto.odecty.shp.ShPFilter.DODAVATEL;
import static cz.xlisto.odecty.shp.ShPFilter.PRODUKT;
import static cz.xlisto.odecty.shp.ShPFilter.RADA;
import static cz.xlisto.odecty.shp.ShPFilter.SAZBA;
import static cz.xlisto.odecty.shp.ShPFilter.UZEMI;
import static cz.xlisto.odecty.databaze.DataPriceListSource.VSE;

public class PriceListFilterDialogFragment extends DialogFragment {
    private static String TAG = PriceListFilterDialogFragment.class.getSimpleName();
    private Spinner spRada, spProdukt, spSazba, spDodavatel, spUzemi, spDatum;
    private Button btnResetFilter;
    String rada, produkt, sazba, dodavatel, uzemi, datum = "";
    private CloseDialogWithPositiveButtonListener closeDialogWithPositiveButtonListener;

    private ShPFilter shpFilter;

    public PriceListFilterDialogFragment() {
        // Empty constructor required for DialogFragment
    }

    public static PriceListFilterDialogFragment newInstance(CloseDialogWithPositiveButtonListener closeDialogWithPositiveButtonListener) {
        PriceListFilterDialogFragment frag = new PriceListFilterDialogFragment();
        frag.closeDialogWithPositiveButtonListener = closeDialogWithPositiveButtonListener;
        return frag;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        shpFilter = new ShPFilter(getContext());
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        View dialogView = getLayoutInflater().inflate(R.layout.fragment_price_list_filter, null);

        spRada = dialogView.findViewById(R.id.spRada);
        spProdukt = dialogView.findViewById(R.id.spProdukt);
        spSazba = dialogView.findViewById(R.id.spSazba);
        spDodavatel = dialogView.findViewById(R.id.spDodavatel);
        spUzemi = dialogView.findViewById(R.id.spUzemi);
        spDatum = dialogView.findViewById(R.id.spDatum);
        btnResetFilter = dialogView.findViewById(R.id.btnResetFilter);

        btnResetFilter.setOnClickListener(v -> setItemSpinnerAll());

        builder.setView(dialogView);
        builder.setTitle(R.string.filtr_ceniku);
        builder.setPositiveButton(R.string.filtr_nastav, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                savePreferences();
                closeDialogWithPositiveButtonListener.onCloseDialogWithPositiveButton();
            }
        });
        builder.setNegativeButton(R.string.zrusit, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

            }
        });
        loadPreferences();
        setDataFromDatabase();

        return builder.create();
    }

    /**
     * Načte seznam ceníků podle RADA, PRODUKT, SAZBA, FIRMA, DISTRIBUCE, PLATNOST_OD a nastaví data do příslušných spinnerů
     */
    private void setDataFromDatabase() {
        DataPriceListSource dataPriceListSource = new DataPriceListSource(getContext());
        dataPriceListSource.open();
        ArrayList<String> arrayListRada = dataPriceListSource.readPriceListCount(DBHelperPriceList.RADA);
        ArrayList<String> arrayListProdukt = dataPriceListSource.readPriceListCount(DBHelperPriceList.PRODUKT);
        ArrayList<String> arrayListSazba = dataPriceListSource.readPriceListCount(DBHelperPriceList.SAZBA);
        ArrayList<String> arrayListDodavatel = dataPriceListSource.readPriceListCount(DBHelperPriceList.FIRMA);
        ArrayList<String> arrayListUzemi = dataPriceListSource.readPriceListCount(DBHelperPriceList.DISTRIBUCE);
        ArrayList<String> arrayListDatum = dataPriceListSource.readPriceListCount(DBHelperPriceList.PLATNOST_OD);
        dataPriceListSource.close();


        spRada.setAdapter(setStringAdapter(arrayListRada));
        spProdukt.setAdapter(setStringAdapter(arrayListProdukt));
        spSazba.setAdapter(setStringAdapter(arrayListSazba));
        spDodavatel.setAdapter(setStringAdapter(arrayListDodavatel));
        spUzemi.setAdapter(setStringAdapter(arrayListUzemi));
        spDatum.setAdapter(setStringAdapter(arrayListDatum));

        setItemSpinner(arrayListRada, spRada, rada);
        setItemSpinner(arrayListProdukt, spProdukt, produkt);
        setItemSpinner(arrayListSazba, spSazba, sazba);
        setItemSpinner(arrayListDodavatel, spDodavatel, dodavatel);
        setItemSpinner(arrayListUzemi, spUzemi, uzemi);
        setItemSpinner(arrayListDatum, spDatum, datum);

        spRada.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }

    /**
     * Vytvoří pro spiner adapter z arraylist
     *
     * @param arrayList
     * @return
     */
    private ArrayAdapter<String> setStringAdapter(ArrayList<String> arrayList) {
        return new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_dropdown_item, arrayList);
    }

    /**
     * Uloží hodnoty spinnerů do sharedprefences
     */
    private void savePreferences() {
        rada = spRada.getSelectedItem().toString();
        produkt = spProdukt.getSelectedItem().toString();
        sazba = spSazba.getSelectedItem().toString();
        dodavatel = spDodavatel.getSelectedItem().toString();
        uzemi = spUzemi.getSelectedItem().toString();
        datum = spDatum.getSelectedItem().toString();
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
        if (datum.equals(VSE))
            datum = "%";

        shpFilter.set(RADA, rada);
        shpFilter.set(PRODUKT, produkt);
        shpFilter.set(SAZBA, sazba);
        shpFilter.set(DODAVATEL, dodavatel);
        shpFilter.set(UZEMI, uzemi);
        shpFilter.set(DATUM, datum);
    }

    /**
     * Načte hodnoty ze sharedpreference, které se mají nastavit na spinnerech
     */
    private void loadPreferences() {
        rada = shpFilter.get(RADA, VSE);
        produkt = shpFilter.get(PRODUKT, VSE);
        sazba = shpFilter.get(SAZBA, VSE);
        dodavatel = shpFilter.get(DODAVATEL, VSE);
        uzemi = shpFilter.get(UZEMI, VSE);
        datum = shpFilter.get(DATUM, VSE);
    }

    /**
     * Zjistí index a arraylistu vybrané položky ve spinru, pokud nenajde,
     * bude nastaven na první položku [VŠE ]
     * @param arrayList
     * @param sp
     * @param item
     */
    private void setItemSpinner(ArrayList<String> arrayList, Spinner sp, String item) {
        for (int i = 0; i < arrayList.size(); i++) {
            if (arrayList.get(i).equals(item)) {
                sp.setSelection(i);
            }
        }
    }

    private void setItemSpinnerAll(){
        spRada.setSelection(0);
        spDatum.setSelection(0);
        spSazba.setSelection(0);
        spProdukt.setSelection(0);
        spDodavatel.setSelection(0);
        spUzemi.setSelection(0);
    }

    public interface CloseDialogWithPositiveButtonListener {
        void onCloseDialogWithPositiveButton();
    }

}
