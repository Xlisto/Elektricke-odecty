package cz.xlisto.elektrodroid.modules.pricelist;


import static cz.xlisto.elektrodroid.format.DecimalFormatHelper.df2;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import java.util.Calendar;

import cz.xlisto.elektrodroid.R;
import cz.xlisto.elektrodroid.databaze.DataPriceListSource;
import cz.xlisto.elektrodroid.models.PriceListModel;
import cz.xlisto.elektrodroid.models.PriceListRegulBuilder;
import cz.xlisto.elektrodroid.ownview.LabelPriceDetail;
import cz.xlisto.elektrodroid.ownview.ViewHelper;
import cz.xlisto.elektrodroid.utils.DetectScreenMode;


/**
 * Fragment pro zobrazení detailů ceníku.
 */
public class PriceListDetailFragment extends Fragment {

    private final static String TAG = "PriceListDetailFragment";
    public final static String PRICE_LIST_DETAIL_FRAGMENT = "price_list_detail_fragment";
    public final static String PRICE_LIST_ID = "price_list_id";
    private LabelPriceDetail ldnDatum;
    private LabelPriceDetail ldnRada, ldnProdukt, ldnSazba, ldnDodavatel, ldnUzemi;
    private LabelPriceDetail ldnVTDodavka, ldnNTDodavka, ldnMesPlat;
    private LabelPriceDetail ldnVTDistribuce, ldnNTDistribuce;
    private LabelPriceDetail ldnJ0, ldnJ1, ldnJ2, ldnJ3, ldnJ4, ldnJ5, ldnJ6, ldnJ7, ldnJ8, ldnJ9, ldnJ10, ldnJ11, ldnJ12, ldnJ13, ldnJ14;
    private LabelPriceDetail ldnSystemSluzby, ldnOTE, ldnPOZE1, ldnPOZE2;
    private LabelPriceDetail ldnDan, ldnDPH;
    private TextView tvPoznamka, tvPoznamkaVT, tvPoznamkaNT, tvPoznamkaPlat, tvPoznamkaPOZE1, tvPoznamkaPOZE2, tvPoznamkaOTE;
    private static final String PRICE_ID = "price_list_id";
    private static final String SHOW_IN_FRAGMENT = "show_in_fragment";
    private long price_id;
    private boolean showInFragment;


    /**
     * Konstruktor třídy PriceListDetailFragment.
     * Tento konstruktor je vyžadován a musí být veřejný a prázdný.
     */
    public PriceListDetailFragment() {
        // Required empty public constructor
    }


    /**
     * Tato metoda vytvoří novou instanci tohoto fragmentu s definovaným ID ceníku.
     *
     * @param priceListId ID ceníku, který má být načten.
     * @return Nová instance fragmentu PriceListDetailFragment.
     */
    public static PriceListDetailFragment newInstance(long priceListId) {
        return newInstance(priceListId, false);
    }


    /**
     * Vytvoří novou instanci fragmentu PriceListDetailFragment s definovanými parametry.
     *
     * @param priceListId    ID ceníku, který má být načten.
     * @param showInFragment Určuje, zda se má fragment zobrazit v rámci jiného fragmentu.
     * @return Nová instance fragmentu PriceListDetailFragment.
     */
    public static PriceListDetailFragment newInstance(long priceListId, boolean showInFragment) {
        PriceListDetailFragment fragment = new PriceListDetailFragment();
        Bundle args = new Bundle();
        args.putLong(PRICE_ID, priceListId);
        args.putBoolean(SHOW_IN_FRAGMENT, showInFragment);
        fragment.setArguments(args);
        return fragment;
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            price_id = getArguments().getLong(PRICE_ID);
            showInFragment = getArguments().getBoolean(SHOW_IN_FRAGMENT);
        }
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_price_list_detail, container, false);
        if (showInFragment && DetectScreenMode.isLandscape(requireActivity())) {
            v = inflater.inflate(R.layout.fragment_price_list_detail_infragment, container, false);
        }

        ldnDatum = v.findViewById(R.id.ldnPlatnost);

        ldnRada = v.findViewById(R.id.ldnRada);
        ldnProdukt = v.findViewById(R.id.ldnProdukt);
        ldnSazba = v.findViewById(R.id.ldnSazba);
        ldnDodavatel = v.findViewById(R.id.ldnDodavatel);
        ldnUzemi = v.findViewById(R.id.ldnUzemi);
        tvPoznamka = v.findViewById(R.id.tvPoznamkaDetail);

        ldnVTDodavka = v.findViewById(R.id.ldn_VT_dodavka);
        ldnNTDodavka = v.findViewById(R.id.ldn_NT_dodavka);
        ldnMesPlat = v.findViewById(R.id.ldn_staly_plat);
        tvPoznamkaVT = v.findViewById(R.id.tvPoznamkaVT);
        tvPoznamkaNT = v.findViewById(R.id.tvPoznamkaNT);
        tvPoznamkaPlat = v.findViewById(R.id.tvPoznamkaPlat);
        tvPoznamkaPOZE1 = v.findViewById(R.id.tvPoznamkaPOZE1);
        tvPoznamkaPOZE2 = v.findViewById(R.id.tvPoznamkaPOZE2);
        tvPoznamkaOTE = v.findViewById(R.id.tvPoznamkaOTE);

        ldnVTDistribuce = v.findViewById(R.id.ldn_VT_distribuce);
        ldnNTDistribuce = v.findViewById(R.id.ldn_NT_distribuce);

        ldnJ0 = v.findViewById(R.id.ldn_J0);
        ldnJ1 = v.findViewById(R.id.ldn_J1);
        ldnJ2 = v.findViewById(R.id.ldn_J2);
        ldnJ3 = v.findViewById(R.id.ldn_J3);
        ldnJ4 = v.findViewById(R.id.ldn_J4);
        ldnJ5 = v.findViewById(R.id.ldn_J5);
        ldnJ6 = v.findViewById(R.id.ldn_J6);
        ldnJ7 = v.findViewById(R.id.ldn_J7);
        ldnJ8 = v.findViewById(R.id.ldn_J8);
        ldnJ9 = v.findViewById(R.id.ldn_J9);
        ldnJ10 = v.findViewById(R.id.ldn_J10);
        ldnJ11 = v.findViewById(R.id.ldn_J11);
        ldnJ12 = v.findViewById(R.id.ldn_J12);
        ldnJ13 = v.findViewById(R.id.ldn_J13);
        ldnJ14 = v.findViewById(R.id.ldn_J14);

        ldnSystemSluzby = v.findViewById(R.id.ldn_systemove_sluzby);
        ldnOTE = v.findViewById(R.id.ldn_OTE);
        ldnPOZE1 = v.findViewById(R.id.ldn_POZE1);
        ldnPOZE2 = v.findViewById(R.id.ldn_POZE2);

        ldnDan = v.findViewById(R.id.ldn_dan);
        ldnDPH = v.findViewById(R.id.ldn_DPH);

        loadPrice(price_id);

        getParentFragmentManager().setFragmentResultListener(PRICE_LIST_DETAIL_FRAGMENT, this, (requestKey, result) -> {
            long priceId = result.getLong(PRICE_LIST_ID);
            loadPrice(priceId);
        });

        return v;
    }


    /**
     * Načte a zobrazí detaily ceníku na základě zadaného ID ceníku.
     *
     * @param priceId ID ceníku, který má být načten.
     */
    public void loadPrice(long priceId) {
        DataPriceListSource dataPriceListSource = new DataPriceListSource(requireActivity());
        dataPriceListSource.open();
        PriceListModel priceList = dataPriceListSource.readPrice(priceId);
        dataPriceListSource.close();

        long dateStart = priceList.getPlatnostOD();
        long dateEnd = priceList.getPlatnostDO();

        ldnDatum.setPrice(ViewHelper.convertLongToDate(dateStart) + " - " + ViewHelper.convertLongToDate(dateEnd));

        ldnRada.setPrice(priceList.getRada());
        ldnProdukt.setPrice(priceList.getProdukt());
        ldnSazba.setPrice(priceList.getSazba());
        ldnDodavatel.setPrice(priceList.getFirma());
        ldnUzemi.setPrice(priceList.getDistribuce());

        ldnVTDodavka.setPrice(df2.format(priceList.getCenaVT()));
        ldnNTDodavka.setPrice(df2.format(priceList.getCenaNT()));
        ldnMesPlat.setPrice(df2.format(priceList.getMesicniPlat()));

        ldnVTDistribuce.setPrice(df2.format(priceList.getDistVT()));
        ldnNTDistribuce.setPrice(df2.format(priceList.getDistNT()));

        ldnJ0.setPrice(df2.format(priceList.getJ0()));
        ldnJ1.setPrice(df2.format(priceList.getJ1()));
        ldnJ2.setPrice(df2.format(priceList.getJ2()));
        ldnJ3.setPrice(df2.format(priceList.getJ3()));
        ldnJ4.setPrice(df2.format(priceList.getJ4()));
        ldnJ5.setPrice(df2.format(priceList.getJ5()));
        ldnJ6.setPrice(df2.format(priceList.getJ6()));
        ldnJ7.setPrice(df2.format(priceList.getJ7()));
        ldnJ8.setPrice(df2.format(priceList.getJ8()));
        ldnJ9.setPrice(df2.format(priceList.getJ9()));
        ldnJ10.setPrice(df2.format(priceList.getJ10()));
        ldnJ11.setPrice(df2.format(priceList.getJ11()));
        ldnJ12.setPrice(df2.format(priceList.getJ12()));
        ldnJ13.setPrice(df2.format(priceList.getJ13()));
        ldnJ14.setPrice(df2.format(priceList.getJ14()));

        ldnSystemSluzby.setPrice(df2.format(priceList.getSystemSluzby()));
        ldnOTE.setPrice(df2.format(priceList.getCinnost()));
        ldnPOZE1.setPrice(df2.format(priceList.getPoze1()));
        ldnPOZE2.setPrice(df2.format(priceList.getPoze2()));

        ldnDan.setPrice(df2.format(priceList.getDan()));
        ldnDPH.setPrice(df2.format(priceList.getDph()));

        //skrytí u NT u jednotarifního ceníku
        if (priceList.getDistNT() == 0) {
            ldnNTDistribuce.setVisibility(View.GONE);
            ldnNTDodavka.setVisibility(View.GONE);
        } else {
            ldnNTDistribuce.setVisibility(View.VISIBLE);
            ldnNTDodavka.setVisibility(View.VISIBLE);
        }

        if (priceList.getJ10() == 0) {
            //méně hodnot jističů
            ldnJ10.setVisibility(View.GONE);
            ldnJ11.setVisibility(View.GONE);
            ldnJ12.setVisibility(View.GONE);
            ldnJ13.setVisibility(View.GONE);
            ldnJ14.setVisibility(View.GONE);
            ldnJ8.setVisibility(View.VISIBLE);
        } else {
            //vice hodnot jističů
            ldnJ10.setVisibility(View.VISIBLE);
            ldnJ11.setVisibility(View.VISIBLE);
            ldnJ12.setVisibility(View.VISIBLE);
            ldnJ13.setVisibility(View.VISIBLE);
            ldnJ14.setVisibility(View.VISIBLE);
            ldnJ8.setVisibility(View.GONE);
        }

        Calendar calendar = ViewHelper.parseCalendarFromString(ldnDatum.getPrice());
        int year = calendar.get(Calendar.YEAR);
        if (year < 2016) {
            ldnOTE.setPrice(df2.format(priceList.getOte()));
            ldnOTE.setLabel(getResources().getString(R.string.cinnost_ote));
            ldnOTE.setItem(getResources().getString(R.string.kc_MWh));

            ldnPOZE2.setPrice(df2.format(priceList.getOze()));
            ldnPOZE2.setLabel(getResources().getString(R.string.podpora_vykupu_el_z_oze_kvet_a_dz));

            ldnPOZE1.setVisibility(View.GONE);
        }

        //Přenastavení ceníku pro rok 2024, od 1.7. se činnost operátora trhu (OTE) mění na provoz nesíťové infrastruktury
        //1719784800000 == 1.7.2024
        if (dateStart >= 1719784800000L && dateEnd <= 1735599600000L) {//1.7.2024 - 31.12.2024
            ldnOTE.setLabel(getResources().getString(R.string.provoz_nesitove_infrastruktury));
            ldnOTE.setItem(getResources().getString(R.string.kc_om));
        }

        //zobrazení varování, pokud platnost ceníku překrývá s přechodem na provoz nesíťové infrastruktury (1.7.2024)
        if (year == 2024) {
            //1719784800000 == 1.7.2024
            if (dateStart < 1719784800000L && dateEnd >= 1719784800000L) {//1.7.2024 - 1.7.2024
                //zobraz varování
                tvPoznamkaOTE.setText(getResources().getText(R.string.provoz_nesitove_infrastruktury_poznamka));
                tvPoznamkaOTE.setVisibility(View.VISIBLE);
            } else {
                tvPoznamkaOTE.setVisibility(View.GONE);
            }
        }

        //poznámky jsou uloženy v třídě Notes a hledají se podle příslušných datumů
        tvPoznamka.setVisibility(View.GONE);
        PriceListRegulBuilder priceListRegulBuilder = new PriceListRegulBuilder(priceList, year);
        String poznamka = priceListRegulBuilder.getNotes(requireActivity());
        String maxVT = priceListRegulBuilder.getMaxVT();
        String maxNT = priceListRegulBuilder.getMaxNT();
        String maxPlat = priceListRegulBuilder.getMaxPlat();
        String maxPOZE1 = priceListRegulBuilder.getMaxPOZE();
        String maxPOZE2 = priceListRegulBuilder.getMaxPOZE();

        if (priceListRegulBuilder.isRegulPrice()) {
            tvPoznamka.setText(poznamka);
            tvPoznamka.setVisibility(View.VISIBLE);
            setPoznamka(tvPoznamkaVT, maxVT);
            setPoznamka(tvPoznamkaPOZE1, maxPOZE1);
            setPoznamka(tvPoznamkaPOZE2, maxPOZE2);

            tvPoznamkaVT.setVisibility(View.VISIBLE);
            setPoznamka(tvPoznamkaNT, maxNT);

            //vynechání poznámky NT u jednotarifního ceníku
            if (priceList.getDistNT() == 0.0) {

                tvPoznamkaNT.setVisibility(View.GONE);
            }
            tvPoznamkaPlat.setVisibility(View.VISIBLE);
            setPoznamka(tvPoznamkaPlat, maxPlat);
        } else {
            tvPoznamka.setVisibility(View.GONE);

            tvPoznamkaVT.setVisibility(View.GONE);
            tvPoznamkaNT.setVisibility(View.GONE);
            tvPoznamkaPlat.setVisibility(View.GONE);
            tvPoznamkaPOZE1.setVisibility(View.GONE);
            tvPoznamkaPOZE2.setVisibility(View.GONE);
        }
    }


    /**
     * Nastaví poznámku do zadaného TextView.
     *
     * @param tv TextView, do kterého se má poznámka nastavit.
     * @param s  Poznámka, která se má nastavit do TextView. Pokud je null, TextView se skryje.
     */
    private void setPoznamka(TextView tv, String s) {
        if (s != null) {
            tv.setVisibility(View.VISIBLE);
            tv.setText(s);
        } else {
            tv.setVisibility(View.GONE);
        }
    }

}