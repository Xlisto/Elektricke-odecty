package cz.xlisto.elektrodroid.modules.pricelist;


import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static cz.xlisto.elektrodroid.format.DecimalFormatHelper.df2;
import static cz.xlisto.elektrodroid.ownview.OwnPriceListCompare.Type.MONTH;
import static cz.xlisto.elektrodroid.ownview.OwnPriceListCompare.Type.NT;
import static cz.xlisto.elektrodroid.ownview.OwnPriceListCompare.Type.VT;
import static cz.xlisto.elektrodroid.ownview.OwnPriceListCompare.Type.VT_NT;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.Fragment;

import cz.xlisto.elektrodroid.R;
import cz.xlisto.elektrodroid.databaze.DataPriceListSource;
import cz.xlisto.elektrodroid.databaze.DataSubscriptionPointSource;
import cz.xlisto.elektrodroid.models.PriceListModel;
import cz.xlisto.elektrodroid.models.PriceListRegulBuilder;
import cz.xlisto.elektrodroid.models.SubscriptionPointModel;
import cz.xlisto.elektrodroid.ownview.OwnPriceListCompare;
import cz.xlisto.elektrodroid.ownview.OwnPriceListCompare.Type;
import cz.xlisto.elektrodroid.shp.ShPSubscriptionPoint;
import cz.xlisto.elektrodroid.utils.Calculation;


/**
 * Fragment pro porovnání ceníků detailní pohled
 */

public class PriceListCompareDetailFragment extends Fragment implements SelectedPriceListsInterface {
    private static final String TAG = "PriceListCompareDetailFragment";
    private static final String KC_MWH = " Kč/MWh";
    private static final String KC = " Kč";
    private static final String KC_MONTH = " Kč/měsíc";
    private static final String ID_PRICE_LIST_LEFT = "id_price_list_left";
    private static final String ID_PRICE_LIST_RIGHT = "id_price_list_right";
    private static final String AMOUNT_VT = "vt";
    private static final String AMOUNT_NT = "nt";
    private static final String COUNT_MONTH = "month";
    private static final String PHAZE = "phaze";
    private static final String POWER = "power";
    private static final String SERVICES_L = "services_r";
    private static final String SERVICES_R = "services_l";

    private double vt = 1, nt = 1, month = 1, phaze = 3, power = 25, servicesL = 0, servicesR = 0;

    private OwnPriceListCompare ownprcRada, ownprcProdukt, ownprcSazba, ownprcDodavatel, ownprcUzemi;

    private OwnPriceListCompare ownprcVT, ownprcNT, ownprcPayment;

    private OwnPriceListCompare ownprcVTRegul, ownprcNTRegul, ownprcJ0, ownprcJ1, ownprcJ2, ownprcJ3, ownprcJ4, ownprcJ5, ownprcJ6, ownprcJ7, ownprcJ8, ownprcJ9, ownprcJ10,
            ownprcJ11, ownprcJ12, ownprcJ13, ownprcJ14;

    private OwnPriceListCompare ownprcSystem, ownprcOTE, ownprcPOZE1, ownprcPOZE2;
    private OwnPriceListCompare ownprcTaxation, ownprcDPH;

    private OwnPriceListCompare ownprcTotalVT, ownprcTotalNT, ownprcTotalPayment, ownprcTotalPoze;
    private OwnPriceListCompare ownprcTotalVTDPH, ownprcTotalNTDPH, ownprcTotalPaymentDPH, ownprcTotalPozeDPH;
    private OwnPriceListCompare ownprcTotalDPH;
    private SwitchCompat swLeft, swRight;
    private TextView tvShowRegulPriceLabel;
    private PriceListModel priceListLeft, priceListRight, priceListLeftRegul, priceListRightRegul;
    private PriceListModel priceListLeftNERegul, priceListRightNERegul;
    private PriceListRegulBuilder priceListLeftRegulBuilder, priceListRightRegulBuilder;
    private static long idPriceListLeft = -1L, idPriceListRight = -1L;
    private SubscriptionPointModel subscriptionPoint;
    private double[] totalPriceLeft, totalPriceRight;
    private double totalPozeLeft, totalPozeRight, totalRight, totalLeft;


    public static PriceListCompareDetailFragment newInstance() {
        return new PriceListCompareDetailFragment();
    }


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        ShPSubscriptionPoint shPSubscriptionPoint = new ShPSubscriptionPoint(getActivity());
        long idSubscriptionPoint = shPSubscriptionPoint.get(ShPSubscriptionPoint.ID_SUBSCRIPTION_POINT_LONG, -1L);
        if (idSubscriptionPoint != -1L) {
            DataSubscriptionPointSource dataSubscriptionPointSource = new DataSubscriptionPointSource(getActivity());
            dataSubscriptionPointSource.open();
            subscriptionPoint = dataSubscriptionPointSource.loadSubscriptionPoint(idSubscriptionPoint);
            dataSubscriptionPointSource.close();
        }
    }


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_price_list_compare_detail, container, false);

    }


    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ownprcRada = view.findViewById(R.id.ownprcTitle);
        ownprcProdukt = view.findViewById(R.id.ownprcProdukt);
        ownprcSazba = view.findViewById(R.id.ownprcSazba);
        ownprcDodavatel = view.findViewById(R.id.ownprcDodavatel);
        ownprcUzemi = view.findViewById(R.id.ownprcUzemi);

        ownprcVT = view.findViewById(R.id.ownprcVT);
        ownprcNT = view.findViewById(R.id.ownprcNT);
        ownprcPayment = view.findViewById(R.id.ownprcPayment);

        ownprcVTRegul = view.findViewById(R.id.ownprcVTRegul);
        ownprcNTRegul = view.findViewById(R.id.ownprcNTRegul);
        ownprcJ0 = view.findViewById(R.id.ownprcJ0);
        ownprcJ1 = view.findViewById(R.id.ownprcJ1);
        ownprcJ2 = view.findViewById(R.id.ownprcJ2);
        ownprcJ3 = view.findViewById(R.id.ownprcJ3);
        ownprcJ4 = view.findViewById(R.id.ownprcJ4);
        ownprcJ5 = view.findViewById(R.id.ownprcJ5);
        ownprcJ6 = view.findViewById(R.id.ownprcJ6);
        ownprcJ7 = view.findViewById(R.id.ownprcJ7);
        ownprcJ8 = view.findViewById(R.id.ownprcJ8);
        ownprcJ9 = view.findViewById(R.id.ownprcJ9);
        ownprcJ10 = view.findViewById(R.id.ownprcJ10);
        ownprcJ11 = view.findViewById(R.id.ownprcJ11);
        ownprcJ12 = view.findViewById(R.id.ownprcJ12);
        ownprcJ13 = view.findViewById(R.id.ownprcJ13);
        ownprcJ14 = view.findViewById(R.id.ownprcJ14);

        ownprcSystem = view.findViewById(R.id.ownprcSystem);
        ownprcOTE = view.findViewById(R.id.ownprcOTE);
        ownprcPOZE1 = view.findViewById(R.id.ownprcPOZE1);
        ownprcPOZE2 = view.findViewById(R.id.ownprcPOZE2);

        ownprcTaxation = view.findViewById(R.id.ownprcTaxation);
        ownprcDPH = view.findViewById(R.id.ownprcDPH);

        ownprcTotalVT = view.findViewById(R.id.ownprcTotalVT);
        ownprcTotalNT = view.findViewById(R.id.ownprcTotalNT);
        ownprcTotalPayment = view.findViewById(R.id.ownprcTotalPayment);
        ownprcTotalPoze = view.findViewById(R.id.ownprcTotalPoze);

        ownprcTotalVTDPH = view.findViewById(R.id.ownprcTotalVTDPH);
        ownprcTotalNTDPH = view.findViewById(R.id.ownprcTotalNTDPH);
        ownprcTotalPaymentDPH = view.findViewById(R.id.ownprcTotalPaymentDPH);
        ownprcTotalPozeDPH = view.findViewById(R.id.ownprcTotalPozeDPH);
        ownprcTotalDPH = view.findViewById(R.id.ownprcTotalDPH);

        swLeft = view.findViewById(R.id.swLeft);
        swRight = view.findViewById(R.id.swRight);
        tvShowRegulPriceLabel = view.findViewById(R.id.tvShowRegulPriceTitle);

        swLeft.setOnCheckedChangeListener((buttonView, isChecked) -> onResume());

        swRight.setOnCheckedChangeListener((buttonView, isChecked) -> onResume());

        tvShowRegulPriceLabel.setVisibility(GONE);
        showSwLeft(false);
        showSwRight(false);

        //listener pro výběr ceníku
        getParentFragmentManager().setFragmentResultListener(PriceListFragment.FLAG_PRICE_LIST_FRAGMENT, this, (requestKey, result) -> {
            PriceListModel priceList = (PriceListModel) result.getSerializable(PriceListFragment.FLAG_RESULT_PRICE_LIST_FRAGMENT);
            PriceListFragment.Side side = (PriceListFragment.Side) result.getSerializable(PriceListFragment.FLAG_SIDE);
            if (priceList != null && side != null) {
                if (side.equals(PriceListFragment.Side.LEFT)) {
                    priceListLeftNERegul = priceList;
                    idPriceListLeft = priceList.getId();
                    createNeregulPriceListLeft();
                } else {
                    priceListRightNERegul = priceList;
                    idPriceListRight = priceList.getId();
                    createNeregulPriceListRight();
                }
            }
        });


        if (savedInstanceState != null) {
            idPriceListLeft = savedInstanceState.getLong(ID_PRICE_LIST_LEFT, -1L);
            idPriceListRight = savedInstanceState.getLong(ID_PRICE_LIST_RIGHT, -1L);

            DataPriceListSource dataPriceListSource = new DataPriceListSource(requireContext());
            dataPriceListSource.open();
            if (idPriceListLeft > 0) {
                priceListLeftNERegul = dataPriceListSource.readPrice(idPriceListLeft);
                createNeregulPriceListLeft();
            }
            if (idPriceListRight > 0) {
                priceListRightNERegul = dataPriceListSource.readPrice(idPriceListRight);
                createNeregulPriceListRight();
            }
            dataPriceListSource.close();

            vt = savedInstanceState.getDouble(AMOUNT_VT);
            nt = savedInstanceState.getDouble(AMOUNT_NT);
            month = savedInstanceState.getDouble(COUNT_MONTH);
            phaze = savedInstanceState.getDouble(PHAZE);
            power = savedInstanceState.getDouble(POWER);
            servicesL = savedInstanceState.getDouble(SERVICES_L);
            servicesR = savedInstanceState.getDouble(SERVICES_R);
        }
    }


    @Override
    public void onResume() {
        super.onResume();
        priceListLeft = setPriceList(swLeft.isChecked(), priceListLeftRegul, priceListLeftNERegul);
        priceListRight = setPriceList(swRight.isChecked(), priceListRightRegul, priceListRightNERegul);

        ownprcUzemi.visibleSeparator(false);

        Runnable r = this::comparePriceList;
        r.run();

    }


    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putLong(ID_PRICE_LIST_LEFT, idPriceListLeft);
        outState.putLong(ID_PRICE_LIST_RIGHT, idPriceListRight);
        outState.putDouble(AMOUNT_VT, vt);
        outState.putDouble(AMOUNT_NT, nt);
        outState.putDouble(COUNT_MONTH, month);
        outState.putDouble(PHAZE, phaze);
        outState.putDouble(POWER, power);
        outState.putDouble(SERVICES_L, servicesL);
        outState.putDouble(SERVICES_R, servicesR);
    }


    /**
     * Skryje nepoužíté OwnPriceListCompare
     *
     * @param ownPriceListCompare vlastní ceník
     * @param left                levá strana
     * @param right               pravá strana
     */
    private void setHideNotUsedItem(OwnPriceListCompare ownPriceListCompare, double left, double right) {
        if (left == 0 && right == 0) {
            ownPriceListCompare.setVisibility(GONE);
        } else {
            ownPriceListCompare.setVisibility(VISIBLE);
        }
    }


    /**
     * Nastaví typ dílčí ceny pro zobrazení adekvátních jednotkových cen v ceníku a nastaví dvě desetinná místa
     *
     * @param ownPriceListCompare vlastní ceník
     * @param differentPrice      rozdílná cena
     * @param type                typ dílčí ceny
     */
    private void setDifferentPrice(OwnPriceListCompare ownPriceListCompare, double differentPrice, Type type) {
        double quantity = 1;
        if (type.equals(VT))
            quantity = vt;
        else if (type.equals(NT))
            quantity = nt;
        else if (type.equals(MONTH))
            quantity = month;
        else if (type.equals(VT_NT))
            quantity = vt + nt;
        ownPriceListCompare.setDifferent(differentPrice, df2, quantity, type);
    }


    /**
     * Naformátuje rozdílnou cenu a přiřadí zobrazení na dvě desetinná místa
     *
     * @param ownPriceListCompare vlastní ceník
     * @param d1                  rozdílná cena
     */
    private void setDifferentPrice(OwnPriceListCompare ownPriceListCompare, double d1) {
        ownPriceListCompare.setDifferent(d1, df2);
    }


    /**
     * Připočítá k dané ceně DPH podle platného ceníku
     *
     * @param d   cena
     * @param dph DPH
     * @return cena s DPH
     */
    private double addDPH(double d, double dph) {
        return d + (d * dph / 100);
    }


    /**
     * Vytvoří ceník s regulovanou cenou na pravé straně
     */
    private void createNeregulPriceListRight() {
        priceListRightRegulBuilder = new PriceListRegulBuilder(priceListRightNERegul);
        priceListRightRegul = priceListRightRegulBuilder.getRegulPriceList();
    }


    /**
     * Vytvoří ceník s regulovanou cenou na levé straně
     */
    private void createNeregulPriceListLeft() {
        priceListLeftRegulBuilder = new PriceListRegulBuilder(priceListLeftNERegul);
        priceListLeftRegul = priceListLeftRegulBuilder.getRegulPriceList();
    }


    /**
     * Nastaví regulovaný nebo neregulovaný ceník
     *
     * @param b         true = regulovaný ceník, false = neregulovaný ceník
     * @param prRegul   regulovaný ceník
     * @param prNERegul neregulovaný ceník
     * @return regulovaný nebo neregulovaný ceník
     */
    private PriceListModel setPriceList(boolean b, PriceListModel prRegul, PriceListModel prNERegul) {
        if (b)
            return prRegul;
        else
            return prNERegul;
    }


    /**
     * Zobrazí/skryje switch na levé straně
     *
     * @param b true = zobrazí, false = skryje
     */
    private void showSwLeft(boolean b) {
        if (b) {
            swLeft.setVisibility(VISIBLE);
            tvShowRegulPriceLabel.setVisibility(VISIBLE);
            ownprcUzemi.visibleSeparator(true);
        } else {
            swLeft.setVisibility(GONE);
        }
    }


    /**
     * Zobrazí/skryje switch na pravé straně
     *
     * @param b true = zobrazí, false = skryje
     */
    private void showSwRight(boolean b) {
        if (b) {
            swRight.setVisibility(VISIBLE);
            tvShowRegulPriceLabel.setVisibility(VISIBLE);
            ownprcUzemi.visibleSeparator(true);
        } else {
            swRight.setVisibility(GONE);
        }
    }


    private void comparePriceList() {
        //if (priceListRight != null) {
        if (priceListRight != null) {
            showSwRight(priceListRightRegulBuilder.isRegulPrice());

            ownprcRada.setRight(priceListRight.getRada());
            ownprcProdukt.setRight(priceListRight.getProdukt());
            ownprcSazba.setRight(priceListRight.getSazba());
            ownprcDodavatel.setRight(priceListRight.getFirma());
            ownprcUzemi.setRight(priceListRight.getDistribuce());

            ownprcVT.setRight(df2.format(priceListRight.getCenaVT()) + KC_MWH);
            ownprcNT.setRight(df2.format(priceListRight.getCenaNT()) + KC_MWH);
            ownprcPayment.setRight(df2.format(priceListRight.getMesicniPlat()) + KC_MONTH);

            ownprcVTRegul.setRight(df2.format(priceListRight.getDistVT()) + KC_MWH);
            ownprcNTRegul.setRight(df2.format(priceListRight.getDistNT()) + KC_MWH);

            ownprcJ0.setRight(df2.format(priceListRight.getJ0()) + KC_MONTH);
            ownprcJ1.setRight(df2.format(priceListRight.getJ1()) + KC_MONTH);
            ownprcJ2.setRight(df2.format(priceListRight.getJ2()) + KC_MONTH);
            ownprcJ3.setRight(df2.format(priceListRight.getJ3()) + KC_MONTH);
            ownprcJ4.setRight(df2.format(priceListRight.getJ4()) + KC_MONTH);
            ownprcJ5.setRight(df2.format(priceListRight.getJ5()) + KC_MONTH);
            ownprcJ6.setRight(df2.format(priceListRight.getJ6()) + KC_MONTH);
            ownprcJ7.setRight(df2.format(priceListRight.getJ7()) + KC_MONTH);
            ownprcJ8.setRight(df2.format(priceListRight.getJ8()) + KC_MONTH);
            ownprcJ9.setRight(df2.format(priceListRight.getJ9()) + KC_MONTH);
            ownprcJ10.setRight(df2.format(priceListRight.getJ10()) + KC_MONTH);
            ownprcJ11.setRight(df2.format(priceListRight.getJ11()) + KC_MONTH);
            ownprcJ12.setRight(df2.format(priceListRight.getJ12()) + KC_MONTH);
            ownprcJ13.setRight(df2.format(priceListRight.getJ13()) + KC_MONTH);
            ownprcJ14.setRight(df2.format(priceListRight.getJ14()) + KC_MONTH);

            ownprcSystem.setRight(df2.format(priceListRight.getSystemSluzby()) + KC_MWH);
            if (priceListRight.getCinnost() == 0)
                ownprcOTE.setRight(df2.format(priceListRight.getOte()) + " Kč za OM/měsíc");
            else
                ownprcOTE.setRight(df2.format(priceListRight.getCinnost()) + " Kč za OM/měsíc");
            ownprcPOZE1.setRight(df2.format(priceListRight.getPoze1()) + " Kč za 1A/měsíc");
            if (priceListRight.getOze() == 0)
                ownprcPOZE2.setRight(df2.format(priceListRight.getPoze2()) + KC_MWH);
            else
                ownprcPOZE2.setRight(df2.format(priceListRight.getOze()) + KC_MWH);

            ownprcTaxation.setRight(df2.format(priceListRight.getDan()) + KC_MWH);
            ownprcDPH.setRight(df2.format(priceListRight.getDph()) + " %");

            //výpočty
            if (subscriptionPoint != null) {
                //nastavení celkového součtu pravá strana
                totalPriceRight = Calculation.calculatePriceWithoutPozeMwH(priceListRight, subscriptionPoint);
                totalPozeRight = Calculation.getPoze(priceListRight, phaze, power, vt + nt, month).getMinPoze();


                ownprcTotalVT.setRight(df2.format(totalPriceRight[0] * vt) + KC);
                ownprcTotalNT.setRight(df2.format(totalPriceRight[1] * nt) + KC);
                ownprcTotalPayment.setRight(df2.format((totalPriceRight[2] + servicesR) * month) + KC);

                //nastavení celkového součtu s DPH pravá strana
                ownprcTotalVTDPH.setRight(df2.format(addDPH(totalPriceRight[0] * vt, priceListRight.getDph())) + KC);
                ownprcTotalNTDPH.setRight(df2.format(addDPH(totalPriceRight[1] * nt, priceListRight.getDph())) + KC);
                ownprcTotalPaymentDPH.setRight(df2.format(addDPH((totalPriceRight[2] + servicesR) * month, priceListRight.getDph())) + KC);

                ownprcTotalPoze.setRight(df2.format(totalPozeRight) + KC);
                ownprcTotalPozeDPH.setRight(df2.format(addDPH(totalPozeRight, priceListRight.getDph())) + KC);
                if (Calculation.getPoze(priceListRight, phaze, power, vt + nt, month).isMAXPoze()) {
                    ownprcTotalPoze.setRight(df2.format(totalPozeRight) + KC);
                    ownprcTotalPozeDPH.setRight(df2.format(addDPH(totalPozeRight, priceListRight.getDph())) + KC);
                }
                totalRight = (totalPriceRight[0] * vt) + (totalPriceRight[1] * nt) + ((totalPriceRight[2] + servicesR) * month) + totalPozeRight;
                ownprcTotalDPH.setRight(df2.format(addDPH(totalRight, priceListRight.getDph())) + KC);
            }
        }

        if (priceListLeft != null) {
            showSwLeft(priceListLeftRegulBuilder.isRegulPrice());

            ownprcRada.setLeft(priceListLeft.getRada());
            ownprcProdukt.setLeft(priceListLeft.getProdukt());
            ownprcSazba.setLeft(priceListLeft.getSazba());
            ownprcDodavatel.setLeft(priceListLeft.getFirma());
            ownprcUzemi.setLeft(priceListLeft.getDistribuce());

            ownprcVT.setLeft(df2.format(priceListLeft.getCenaVT()) + KC_MWH);
            ownprcNT.setLeft(df2.format(priceListLeft.getCenaNT()) + KC_MWH);
            ownprcPayment.setLeft(df2.format(priceListLeft.getMesicniPlat()) + KC_MONTH);

            ownprcVTRegul.setLeft(df2.format(priceListLeft.getDistVT()) + KC_MWH);
            ownprcNTRegul.setLeft(df2.format(priceListLeft.getDistNT()) + KC_MWH);

            ownprcJ0.setLeft(df2.format(priceListLeft.getJ0()) + KC_MONTH);
            ownprcJ1.setLeft(df2.format(priceListLeft.getJ1()) + KC_MONTH);
            ownprcJ2.setLeft(df2.format(priceListLeft.getJ2()) + KC_MONTH);
            ownprcJ3.setLeft(df2.format(priceListLeft.getJ3()) + KC_MONTH);
            ownprcJ4.setLeft(df2.format(priceListLeft.getJ4()) + KC_MONTH);
            ownprcJ5.setLeft(df2.format(priceListLeft.getJ5()) + KC_MONTH);
            ownprcJ6.setLeft(df2.format(priceListLeft.getJ6()) + KC_MONTH);
            ownprcJ7.setLeft(df2.format(priceListLeft.getJ7()) + KC_MONTH);
            ownprcJ8.setLeft(df2.format(priceListLeft.getJ8()) + KC_MONTH);
            ownprcJ9.setLeft(df2.format(priceListLeft.getJ9()) + KC_MONTH);
            ownprcJ10.setLeft(df2.format(priceListLeft.getJ10()) + KC_MONTH);
            ownprcJ11.setLeft(df2.format(priceListLeft.getJ11()) + KC_MONTH);
            ownprcJ12.setLeft(df2.format(priceListLeft.getJ12()) + KC_MONTH);
            ownprcJ13.setLeft(df2.format(priceListLeft.getJ13()) + KC_MONTH);
            ownprcJ14.setLeft(df2.format(priceListLeft.getJ14()) + KC_MONTH);

            ownprcSystem.setLeft(df2.format(priceListLeft.getSystemSluzby()) + KC_MWH);
            if (priceListLeft.getCinnost() == 0)
                ownprcOTE.setLeft(df2.format(priceListLeft.getOte()) + " Kč za OM/měsíc");
            else
                ownprcOTE.setLeft(df2.format(priceListLeft.getCinnost()) + " Kč za OM/měsíc");
            ownprcPOZE1.setLeft(df2.format(priceListLeft.getPoze1()) + " Kč za 1A/měsíc");
            if (priceListLeft.getOze() == 0) {
                ownprcPOZE2.setLeft(df2.format(priceListLeft.getPoze2()) + KC_MWH);
            } else
                ownprcPOZE2.setLeft(df2.format(priceListLeft.getOze()) + KC_MWH);
            ownprcTaxation.setLeft(df2.format(priceListLeft.getDan()) + KC_MWH);
            ownprcDPH.setLeft(df2.format(priceListLeft.getDph()) + " %");

            //výpočty
            if (subscriptionPoint != null) {
                //nastavení celkového součtu levá strana
                totalPriceLeft = Calculation.calculatePriceWithoutPozeMwH(priceListLeft, subscriptionPoint);
                totalPozeLeft = Calculation.getPoze(priceListLeft, phaze, power, vt + nt, month).getMinPoze();

                ownprcTotalVT.setLeft(df2.format(totalPriceLeft[0] * vt) + KC);
                ownprcTotalNT.setLeft(df2.format(totalPriceLeft[1] * nt) + KC);
                ownprcTotalPayment.setLeft(df2.format((totalPriceLeft[2] + servicesL) * month) + KC);

                //nastavení celkového součtu s DPH levá strana
                ownprcTotalVTDPH.setLeft(df2.format(addDPH(totalPriceLeft[0] * vt, priceListLeft.getDph())) + KC);
                ownprcTotalNTDPH.setLeft(df2.format(addDPH(totalPriceLeft[1] * nt, priceListLeft.getDph())) + KC);
                ownprcTotalPaymentDPH.setLeft(df2.format(addDPH((totalPriceLeft[2] + servicesL) * month, priceListLeft.getDph())) + KC);

                ownprcTotalPoze.setLeft(df2.format(totalPozeLeft) + KC);
                ownprcTotalPozeDPH.setLeft(df2.format(addDPH(totalPozeLeft, priceListLeft.getDph())) + KC);
                if (Calculation.getPoze(priceListLeft, phaze, power, vt + nt, month).isMAXPoze()) {
                    ownprcTotalPoze.setLeft(df2.format(totalPozeLeft) + KC);
                    ownprcTotalPozeDPH.setLeft(df2.format(addDPH(totalPozeLeft, priceListLeft.getDph())) + KC);
                }

                totalLeft = (totalPriceLeft[0] * vt) + (totalPriceLeft[1] * nt) + ((totalPriceLeft[2] + servicesL) * month) + totalPozeLeft;
                ownprcTotalDPH.setLeft(df2.format(addDPH(totalLeft, priceListLeft.getDph())) + KC);

            }
        }

        if (priceListLeft != null && priceListRight != null) {
            if (priceListRightRegulBuilder.isRegulPrice() && priceListLeftRegulBuilder.isRegulPrice())
                tvShowRegulPriceLabel.setVisibility(VISIBLE);
            double difVT = priceListLeft.getCenaVT() - priceListRight.getCenaVT();
            setDifferentPrice(ownprcVT, difVT, VT);

            double difNT = priceListLeft.getCenaNT() - priceListRight.getCenaNT();
            setDifferentPrice(ownprcNT, difNT, NT);

            double difPayment = priceListLeft.getMesicniPlat() - priceListRight.getMesicniPlat();
            setDifferentPrice(ownprcPayment, difPayment, MONTH);

            double difRegulVT = priceListLeft.getDistVT() - priceListRight.getDistVT();
            setDifferentPrice(ownprcVTRegul, difRegulVT, VT);

            double difRegulNT = priceListLeft.getDistNT() - priceListRight.getDistNT();
            setDifferentPrice(ownprcNTRegul, difRegulNT, NT);

            double difJ0 = priceListLeft.getJ0() - priceListRight.getJ0();
            setDifferentPrice(ownprcJ0, difJ0, MONTH);

            double difJ1 = priceListLeft.getJ1() - priceListRight.getJ1();
            setDifferentPrice(ownprcJ1, difJ1, MONTH);

            double difJ2 = priceListLeft.getJ2() - priceListRight.getJ2();
            setDifferentPrice(ownprcJ2, difJ2, MONTH);

            double difJ3 = priceListLeft.getJ3() - priceListRight.getJ3();
            setDifferentPrice(ownprcJ3, difJ3, MONTH);

            double difJ4 = priceListLeft.getJ4() - priceListRight.getJ4();
            setDifferentPrice(ownprcJ4, difJ4, MONTH);

            double difJ5 = priceListLeft.getJ5() - priceListRight.getJ5();
            setDifferentPrice(ownprcJ5, difJ5, MONTH);

            double difJ6 = priceListLeft.getJ6() - priceListRight.getJ6();
            setDifferentPrice(ownprcJ6, difJ6, MONTH);

            double difJ7 = priceListLeft.getJ7() - priceListRight.getJ7();
            setDifferentPrice(ownprcJ7, difJ7, MONTH);

            double difJ8 = priceListLeft.getJ8() - priceListRight.getJ8();
            setDifferentPrice(ownprcJ8, difJ8, MONTH);

            double difJ9 = priceListLeft.getJ9() - priceListRight.getJ9();
            setDifferentPrice(ownprcJ9, difJ9, MONTH);

            double difJ10 = priceListLeft.getJ10() - priceListRight.getJ10();
            setDifferentPrice(ownprcJ10, difJ10, MONTH);

            double difJ11 = priceListLeft.getJ11() - priceListRight.getJ11();
            setDifferentPrice(ownprcJ11, difJ11, MONTH);

            double difJ12 = priceListLeft.getJ12() - priceListRight.getJ12();
            setDifferentPrice(ownprcJ12, difJ12, MONTH);

            double difJ13 = priceListLeft.getJ13() - priceListRight.getJ13();
            setDifferentPrice(ownprcJ13, difJ13, MONTH);

            double difJ14 = priceListLeft.getJ14() - priceListRight.getJ14();
            setDifferentPrice(ownprcJ14, difJ14, MONTH);

            double difSystem = priceListLeft.getSystemSluzby() - priceListRight.getSystemSluzby();
            setDifferentPrice(ownprcSystem, difSystem, VT_NT);

            //načítám ote, pokud je činnost větší než nula, použiji tuto hodnotu
            //ote a cinnost jsou stejné údaje, liší se typem ceníku
            double oteLeft = priceListLeft.getOte();
            double oteRight = priceListRight.getOte();
            if (priceListLeft.getCinnost() > 0)
                oteLeft = priceListLeft.getCinnost();
            if (priceListRight.getCinnost() > 0)
                oteRight = priceListRight.getCinnost();
            double difOTE = oteLeft - oteRight;
            setDifferentPrice(ownprcOTE, difOTE, MONTH);

            double difPOZE1 = priceListLeft.getPoze1() - priceListRight.getPoze1();
            setDifferentPrice(ownprcPOZE1, difPOZE1, MONTH);

            //načítám oze, pokud je poze větší než nula, použiji tuto hodnotu
            //oze a poze2 jsou stejné údaje, liší se typem ceníku
            double poze2Left = priceListLeft.getOze();
            double poze2Right = priceListRight.getOze();
            if (priceListLeft.getPoze2() > 0)
                poze2Left = priceListLeft.getPoze2();
            if (priceListRight.getPoze2() > 0)
                poze2Right = priceListRight.getPoze2();
            double difPOZE2 = poze2Left - poze2Right;
            setDifferentPrice(ownprcPOZE2, difPOZE2, VT_NT);

            //rozdíl v celkovém součtu
            if (subscriptionPoint != null) {
                double difTotalVT = totalPriceLeft[0] - totalPriceRight[0];
                double difTotalNT = totalPriceLeft[1] - totalPriceRight[1];
                double difTotalPayment = totalPriceLeft[2] - totalPriceRight[2] - servicesL - servicesR;
                double difTotalPoze = totalPozeLeft - totalPozeRight;
                setDifferentPrice(ownprcTotalVT, difTotalVT, VT);
                setDifferentPrice(ownprcTotalNT, difTotalNT, NT);
                setDifferentPrice(ownprcTotalPayment, difTotalPayment, MONTH);
                setDifferentPrice(ownprcTotalPoze, difTotalPoze);

                //rozdíl v celkovém součtu s DPH levá strana
                double difTotalVTDPH = addDPH(totalPriceLeft[0], priceListLeft.getDph()) - addDPH(totalPriceRight[0], priceListRight.getDph());
                double difTotalNTDPH = addDPH(totalPriceLeft[1], priceListLeft.getDph()) - addDPH(totalPriceRight[1], priceListRight.getDph());
                double difTotalPaymentDPH = addDPH(totalPriceLeft[2] + servicesL, priceListLeft.getDph()) - addDPH(totalPriceRight[2] + servicesR, priceListRight.getDph());
                double difTotalPozeDPH = addDPH(totalPozeLeft, priceListLeft.getDph()) - addDPH(totalPozeRight, priceListRight.getDph());
                setDifferentPrice(ownprcTotalVTDPH, difTotalVTDPH, VT);
                setDifferentPrice(ownprcTotalNTDPH, difTotalNTDPH, NT);
                setDifferentPrice(ownprcTotalPaymentDPH, difTotalPaymentDPH, MONTH);

                setDifferentPrice(ownprcTotalPozeDPH, difTotalPozeDPH);
                setDifferentPrice(ownprcTotalDPH, addDPH(totalLeft, priceListLeft.getDph()) - addDPH(totalRight, priceListRight.getDph()));

                setHideNotUsedItem(ownprcNT, priceListLeft.getCenaNT(), priceListRight.getCenaNT());
                setHideNotUsedItem(ownprcNTRegul, priceListLeft.getDistNT(), priceListRight.getDistNT());
                setHideNotUsedItem(ownprcJ7, priceListLeft.getJ7(), priceListRight.getJ7());
                setHideNotUsedItem(ownprcJ8, priceListLeft.getJ8(), priceListRight.getJ8());
                setHideNotUsedItem(ownprcJ9, priceListLeft.getJ9(), priceListRight.getJ9());
                setHideNotUsedItem(ownprcJ10, priceListLeft.getJ10(), priceListRight.getJ10());
                setHideNotUsedItem(ownprcJ11, priceListLeft.getJ11(), priceListRight.getJ11());
                setHideNotUsedItem(ownprcJ12, priceListLeft.getJ12(), priceListRight.getJ12());
                setHideNotUsedItem(ownprcJ13, priceListLeft.getJ13(), priceListRight.getJ13());
                setHideNotUsedItem(ownprcJ14, priceListLeft.getJ14(), priceListRight.getJ14());
            }
        }
    }


    @Override
    public void onPriceListsSelected(PriceListModel priceListLeft, PriceListModel priceListRight, PriceListCompareBoxFragment.ConsuptionContainer container) {

        if (priceListLeft != null) {
            this.priceListLeftNERegul = priceListLeft;
            this.priceListLeftRegulBuilder = new PriceListRegulBuilder(priceListLeftNERegul);

        }
        if (priceListRight != null) {
            this.priceListRightNERegul = priceListRight;
            this.priceListRightRegulBuilder = new PriceListRegulBuilder(priceListRightNERegul);

        }
        this.vt = container.vt;
        this.nt = container.nt;
        this.month = container.month;
        this.phaze = container.phaze;
        this.power = container.power;
        this.servicesL = container.servicesL;
        this.servicesR = container.servicesR;
        onResume();
    }
}
