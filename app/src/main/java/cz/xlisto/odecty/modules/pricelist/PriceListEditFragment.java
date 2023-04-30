package cz.xlisto.odecty.modules.pricelist;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import cz.xlisto.odecty.R;
import cz.xlisto.odecty.databaze.DataPriceListSource;
import cz.xlisto.odecty.models.PriceListModel;
import cz.xlisto.odecty.ownview.ViewHelper;

import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Spinner;

import static cz.xlisto.odecty.format.DecimalFormatHelper.df2;


/**
 * A simple {@link Fragment} subclass.
 * Use the {@link PriceListEditFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class PriceListEditFragment extends PriceListAddEditAbstract {
    private static String TAG = PriceListAddFragment.class.getSimpleName();
    private static String IS_FIRST_LOAD = "isFirstLoad";
    private PriceListModel priceListModel;
    private boolean isFirstLoad = true;

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_ID = "id";
    //private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private long itemId;
    //private String mParam2;

    public PriceListEditFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @return A new instance of fragment PriceListEditFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static PriceListEditFragment newInstance(long param1) {
        PriceListEditFragment fragment = new PriceListEditFragment();
        Bundle args = new Bundle();
        args.putLong(ARG_ID, param1);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            itemId = getArguments().getLong(ARG_ID);
            //mParam2 = getArguments().getString(ARG_PARAM2);
        }

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        if (savedInstanceState != null)
            isFirstLoad = savedInstanceState.getBoolean(IS_FIRST_LOAD);
        return inflater.inflate(R.layout.fragment_price_list_add_edit, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        loadItemPrice(itemId);
        if (savedInstanceState == null) {
            setItemPrice();
        }
        btnSave.setOnClickListener(v -> updatePriceList(itemId));
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(IS_FIRST_LOAD, isFirstLoad);
    }

    /**
     * Načte stávající úaje ceníku a přiřadí do příslušných widgetů
     *
     * @param id
     */
    private void loadItemPrice(long id) {
        DataPriceListSource dataPriceListSource = new DataPriceListSource(getActivity());
        dataPriceListSource.open();
        priceListModel = dataPriceListSource.readPrice(id);
        dataPriceListSource.close();
    }

    private void setItemPrice() {
        btnFrom.setText(ViewHelper.convertLongToTime(priceListModel.getPlatnostOD()));
        btnUntil.setText(ViewHelper.convertLongToTime(priceListModel.getPlatnostDO()));
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
        setSpinner(priceListModel);
    }

    @Override
    public void onResume() {
        super.onResume();

        if (isFirstLoad) {
            isFirstLoad = false;
            getYear();
            setDistribucniUzemiAdapter();
            setSpinner(priceListModel);
            setSpinner();
        }
    }

    /**
     * Nastaví spinnery distribuční uzemí a sazba podle nalezeného udaje v ceníku
     *
     * @param priceListModel
     */
    private void setSpinner(PriceListModel priceListModel) {
        Handler handler = new Handler();
        final Runnable r = new Runnable() {
            @Override
            public void run() {
                String[] distribucniUzemi = getResources().getStringArray(R.array.distribucni_uzemi);
                String[] sazby = getResources().getStringArray(R.array.sazby);

                compare(distribucniUzemi, spDistribucniUzemi, priceListModel.getDistribuce());
                compare(sazby, spSazba, priceListModel.getSazba());
            }
        };
        handler.postDelayed(r,1300);
    }

    /**
     * Změni spiner podle datumu na E.ON nebo EG.D. Položek ČEZ a PRE se netýká
     */
    private void setSpinner() {
        if (spDistribucniUzemi.getSelectedItem().toString().equals("PRE")
                || spDistribucniUzemi.getSelectedItem().toString().equals("ČEZ")
        ) return;

        if (year >= 2021) {
            if (!spDistribucniUzemi.getSelectedItem().toString().equals("EG.D")) {
                setDistribucniUzemiAdapter();
                spDistribucniUzemi.setSelection(0);

            }
        } else {
            if (!spDistribucniUzemi.getSelectedItem().toString().equals("E.ON")) {
                setDistribucniUzemiAdapter();
                spDistribucniUzemi.setSelection(0);
            }
        }
    }

    /**
     * Porovná pole stringu načtený ze spineru s hledaným stringem. Při shodě nastaví položku na spinru
     *
     * @param strings
     * @param sp
     * @param searchString
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
     * Upravý ceník vybraný podle itemId
     *
     * @param itemId
     */
    private void updatePriceList(long itemId) {
        if (spDistribucniUzemi.getSelectedItem().toString().equals(arrayDistUzemi[0]) || spSazba.getSelectedItem().toString().equals(arraySazba[0])) {
            //pokud není vybráno distrbuční uzemí nebo sazba k uložení nedojde
            return;
        }
        DataPriceListSource dataPriceListSource = new DataPriceListSource(getActivity());
        dataPriceListSource.open();
        long id = dataPriceListSource.updatePriceList(createPriceList(), itemId);
        dataPriceListSource.close();
        if (id > 0)
            getParentFragmentManager().popBackStack();
    }
}
