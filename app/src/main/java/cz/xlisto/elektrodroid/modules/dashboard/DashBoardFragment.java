package cz.xlisto.elektrodroid.modules.dashboard;


import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatSpinner;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Collections;

import cz.xlisto.elektrodroid.R;
import cz.xlisto.elektrodroid.databaze.DataInvoiceSource;
import cz.xlisto.elektrodroid.databaze.DataSettingsSource;
import cz.xlisto.elektrodroid.models.HdoModel;
import cz.xlisto.elektrodroid.models.InvoiceListSumModel;
import cz.xlisto.elektrodroid.models.SubscriptionPointModel;
import cz.xlisto.elektrodroid.ownview.NumbersMeter;
import cz.xlisto.elektrodroid.shp.ShPDashBoard;
import cz.xlisto.elektrodroid.utils.SubscriptionPoint;


/**
 * Dashboard - přehled důležitých informací
 * Xlisto 26.12.2023 16:55
 */
public class DashBoardFragment extends Fragment {
    private static final String TAG = "DashBoardFragment";
    private RecyclerView rv;
    private ImageButton btnLeftShowInvoiceSum, btnRightShowInvoiceSum;
    private AppCompatSpinner spSort;
    private TextView tvName, tvNoInvoices;
    private int showInvoiceSum = 1;
    private boolean isShowTotal = false;
    private int[] colorVTNT;
    private InvoiceListSumModel invoiceListSumModel;
    private GraphTotalConsuptionView graphTotalConsuptionView;
    private GraphTotalHdoView graphTotalHdoView;
    private NumbersMeter numbersMeterVT, numbersMeterNT;
    private double previousPayment = 0;
    private double previousConsuption = 0;


    public static DashBoardFragment newInstance() {
        return new DashBoardFragment();
    }


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_dashboard, container, false);
    }


    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        requireActivity().invalidateOptionsMenu();
        rv = view.findViewById(R.id.rvInvoiceSum);
        btnLeftShowInvoiceSum = view.findViewById(R.id.imgBtnLeftDashBoard);
        btnRightShowInvoiceSum = view.findViewById(R.id.imgBtnRightDashBoard);
        RelativeLayout rlGraphTotalConsuptionButtons = view.findViewById(R.id.rlGraphTotalConsuptionButtons);
        tvName = view.findViewById(R.id.tvNameDashBoard);
        SwitchCompat swShowTotalConsuption = view.findViewById(R.id.swShowTotalConsuption);
        tvNoInvoices = view.findViewById(R.id.tvNoInvoices);
        spSort = view.findViewById(R.id.spSortInvoiceSum);
        graphTotalConsuptionView = view.findViewById(R.id.graphTotalConsuptionView);
        graphTotalHdoView = view.findViewById(R.id.graphTotalHdoView);
        TextView tvAlertNoSubscriptionPoint = view.findViewById(R.id.tvAlertDashboard);
        numbersMeterVT = view.findViewById(R.id.numbersMeterVT);
        numbersMeterNT = view.findViewById(R.id.numbersMeterNT);

        SubscriptionPointModel subscriptionPointModel = SubscriptionPoint.load(requireContext());
        tvAlertNoSubscriptionPoint.setVisibility(View.GONE);
        if (subscriptionPointModel == null) {
            tvName.setText(getResources().getString(R.string.no_subsriptionpoint));
            tvAlertNoSubscriptionPoint.setVisibility(View.VISIBLE);
            return;
        }

        //při jednom odběrném místě se skryje tlačítka pro změnu odběrného místa
        int count = SubscriptionPoint.count(requireContext());
        if (count <= 1)
            rlGraphTotalConsuptionButtons.setVisibility(View.GONE);
        else
            rlGraphTotalConsuptionButtons.setVisibility(View.VISIBLE);


        ShPDashBoard shPDashBoard = new ShPDashBoard(requireActivity());
        shPDashBoard.get(ShPDashBoard.IS_SHOW_TOTAL, false);
        showInvoiceSum =shPDashBoard.get(ShPDashBoard.SHOW_INVOICE_SUM, 0);
        //pro případ, kdyby ve sharedprefences byl uložený větší index odběrného místa než je počet
        if(showInvoiceSum > count-1)
            showInvoiceSum = 0;

        loadData();

        swShowTotalConsuption.setOnCheckedChangeListener((buttonView, isChecked) -> {
            isShowTotal = isChecked;
            shPDashBoard.set(ShPDashBoard.IS_SHOW_TOTAL, isChecked);
            setAdapter();
        });

        btnLeftShowInvoiceSum.setOnClickListener(v -> {
            if (showInvoiceSum > 0) {
                showInvoiceSum--;
                shPDashBoard.set(ShPDashBoard.SHOW_INVOICE_SUM, showInvoiceSum);
                setAdapter();
                setGraphTotalConsuptionView();
                setTimeHDO();
                setNumbersMeter();
            }
        });

        btnRightShowInvoiceSum.setOnClickListener(v -> {
            if (showInvoiceSum < invoiceListSumModel.size() - 1) {
                showInvoiceSum++;
                shPDashBoard.set(ShPDashBoard.SHOW_INVOICE_SUM, showInvoiceSum);
                setAdapter();
                setGraphTotalConsuptionView();
                setTimeHDO();
                setNumbersMeter();
            }
        });

        TypeCompare[] types = TypeCompare.values();
        ArrayAdapter<TypeCompare> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, types);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spSort.setAdapter(adapter);
        spSort.setSelection(1);

        spSort.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                TypeCompare typeCompare = (TypeCompare) spSort.getSelectedItem();
                DashBoardFragment.this.sortInvoiceSumModels(typeCompare);
                DashBoardFragment.this.setAdapter();
            }


            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });


    }


    /**
     * Načte data z databáze
     */
    private void loadData() {

        DataSettingsSource dataSettingsSource = new DataSettingsSource(requireActivity());
        dataSettingsSource.open();
        colorVTNT = dataSettingsSource.loadColorVTNT();
        dataSettingsSource.close();

        DataInvoiceSource dataInvoiceSource = new DataInvoiceSource(requireActivity());
        dataInvoiceSource.open();
        invoiceListSumModel = dataInvoiceSource.getListSumData();
        dataInvoiceSource.close();

        setAdapter();
        setGraphTotalConsuptionView();
        setTimeHDO();
        setNumbersMeter();
    }


    /**
     * Nastaví adaptér pro zobrazení seznamu faktur
     */
    private void setAdapter() {
        double max = invoiceListSumModel.getMaxValue(showInvoiceSum);
        if (isShowTotal)
            max = invoiceListSumModel.getMaxValueTotal(showInvoiceSum);

        InvoiceSumAdapter invoiceSumAdapter = new InvoiceSumAdapter(requireContext(), invoiceListSumModel.getInvoiceSumModels(showInvoiceSum), colorVTNT,
                max, isShowTotal);
        rv.setAdapter(invoiceSumAdapter);
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        rv.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                rv.getViewTreeObserver().removeOnPreDrawListener(this);

                for (int i = 0; i < rv.getChildCount(); i++) {
                    View v = rv.getChildAt(i);
                    Animation animation = AnimationUtils.loadAnimation(getContext(), R.anim.item_animation_fall_down);
                    v.startAnimation(animation);
                }
                return true;
            }
        });

        tvName.setText(invoiceListSumModel.getName(showInvoiceSum));

        if (invoiceListSumModel.getInvoiceSumModels(showInvoiceSum).size() == 0)
            tvNoInvoices.setVisibility(View.VISIBLE);
        else
            tvNoInvoices.setVisibility(View.GONE);

        setImgButton();
    }


    /**
     * Nastaví ikony přepínačů pro změnu odběrných míst (šedá neaktivní)
     */
    private void setImgButton() {
        if (showInvoiceSum == 0)
            btnLeftShowInvoiceSum.setImageResource(R.mipmap.ic_graph_month_left_gray);
        else
            btnLeftShowInvoiceSum.setImageResource(R.mipmap.ic_graph_month_left);

        if (showInvoiceSum == invoiceListSumModel.size() - 1)
            btnRightShowInvoiceSum.setImageResource(R.mipmap.ic_graph_month_right_gray);
        else
            btnRightShowInvoiceSum.setImageResource(R.mipmap.ic_graph_month_right);
    }


    /**
     * Seřadí seznam faktur
     *
     * @param typeCompare typ řazení
     * @noinspection ComparatorCombinators
     */
    private void sortInvoiceSumModels(TypeCompare typeCompare) {
        switch (typeCompare) {
            case DATE_FROM:
                Collections.sort(invoiceListSumModel.getInvoiceSumModels(showInvoiceSum),
                        (o1, o2) -> Long.compare(o1.getDateStart(), o2.getDateStart()));
                break;
            case DATE_TO:
                Collections.sort(invoiceListSumModel.getInvoiceSumModels(showInvoiceSum),
                        (o1, o2) -> Long.compare(o2.getDateStart(), o1.getDateStart()));
                break;
            case NUMBER:
                Collections.sort(invoiceListSumModel.getInvoiceSumModels(showInvoiceSum),
                        (o1, o2) -> Long.compare(o1.getNumber(), o2.getNumber()));
                break;
            case VT_FROM:
                Collections.sort(invoiceListSumModel.getInvoiceSumModels(showInvoiceSum),
                        (o1, o2) -> Double.compare(o1.getTotalVT(), o2.getTotalVT()));
                break;
            case VT_TO:
                Collections.sort(invoiceListSumModel.getInvoiceSumModels(showInvoiceSum),
                        (o1, o2) -> Double.compare(o2.getTotalVT(), o1.getTotalVT()));
                break;
            case NT_FROM:
                Collections.sort(invoiceListSumModel.getInvoiceSumModels(showInvoiceSum),
                        (o1, o2) -> Double.compare(o1.getTotalNT(), o2.getTotalNT()));
                break;
            case NT_TO:
                Collections.sort(invoiceListSumModel.getInvoiceSumModels(showInvoiceSum),
                        (o1, o2) -> Double.compare(o2.getTotalNT(), o1.getTotalNT()));
                break;
            case TOTAL_FROM:
                Collections.sort(invoiceListSumModel.getInvoiceSumModels(showInvoiceSum),
                        (o1, o2) -> Double.compare(o1.getTotal(), o2.getTotal()));
                break;
            case TOTAL_TO:
                Collections.sort(invoiceListSumModel.getInvoiceSumModels(showInvoiceSum),
                        (o1, o2) -> Double.compare(o2.getTotal(), o1.getTotal()));
                break;
            default:
                break;
        }

    }


    /**
     * Nastaví spotřebu a platby do GraphTotalConsuptionView
     */
    private void setGraphTotalConsuptionView() {
        double payment = invoiceListSumModel.getInvoiceSumPayments(showInvoiceSum);
        double consuption = invoiceListSumModel.getInvoiceSumTotalPrices(showInvoiceSum);
        graphTotalConsuptionView.setPaymentAndConsuption(payment, previousPayment, consuption, previousConsuption);
        previousConsuption = consuption;
        previousPayment = payment;
    }


    /**
     * Nastaví časový posun pro ručičku času HDO
     */
    private void setTimeHDO() {
        ArrayList<HdoModel> hdoModels = invoiceListSumModel.getHdoModels(showInvoiceSum);
        long timeShift = invoiceListSumModel.getHdoTimeShift(showInvoiceSum);
        graphTotalHdoView.setHdoModels(hdoModels, timeShift);
    }


    private void setNumbersMeter() {
        int VT = (int) (invoiceListSumModel.getMeterValuesVT(showInvoiceSum) * 10);
        int NT = (int) (invoiceListSumModel.getMeterValuesNT(showInvoiceSum) * 10);
        numbersMeterVT.setCurrentNumber(VT);
        numbersMeterNT.setCurrentNumber(NT);
    }


    /**
     * Typy řazení seznamu faktur
     */
    private enum TypeCompare {
        DATE_FROM("Nejstarší"),
        DATE_TO("Nejnovější"),
        NUMBER("Číslo fak."),
        VT_FROM("VT nejmenší"),
        VT_TO("VT největší"),
        NT_FROM("NT nejmenší"),
        NT_TO("NT největší"),
        TOTAL_FROM("Celkem nejmenší"),
        TOTAL_TO("Celkem největší");

        private final String displayName;


        TypeCompare(String displayName) {
            this.displayName = displayName;
        }


        @NonNull
        @Override
        public String toString() {
            return displayName;
        }
    }
}
