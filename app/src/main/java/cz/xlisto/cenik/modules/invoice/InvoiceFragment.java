package cz.xlisto.cenik.modules.invoice;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import cz.xlisto.cenik.R;
import cz.xlisto.cenik.databaze.DataPriceListSource;
import cz.xlisto.cenik.databaze.DataSubscriptionPointSource;
import cz.xlisto.cenik.format.DecimalFormatHelper;
import cz.xlisto.cenik.models.InvoiceListModel;
import cz.xlisto.cenik.models.InvoiceModel;
import cz.xlisto.cenik.models.PaymentModel;
import cz.xlisto.cenik.models.PozeModel;
import cz.xlisto.cenik.models.PriceListModel;
import cz.xlisto.cenik.models.PriceListRegulBuilder;
import cz.xlisto.cenik.models.SubscriptionPointModel;
import cz.xlisto.cenik.models.SummaryInvoicesListModel;
import cz.xlisto.cenik.ownview.ViewHelper;
import cz.xlisto.cenik.shp.ShPInvoice;
import cz.xlisto.cenik.utils.Calculation;
import cz.xlisto.cenik.utils.DifferenceDate;
import cz.xlisto.cenik.utils.FragmentChange;
import cz.xlisto.cenik.utils.SubscriptionPoint;


/**
 * Fragment zobrazení jednotlivých záznamů ve faktuře
 * Xlisto 04.02.2023 10:52
 */
public class InvoiceFragment extends Fragment {
    private static final String TAG = "InvoiceFragment";
    private static final String ID_FAK = "id_fak";
    private static final String TABLE_FAK = "table_fak";
    private static final String TABLE_NOW = "table_now";
    private static final String TABLE_PAY = "table_pay";
    private static final String POSITION = "position";
    private String tableFAK,tableNOW,tablePAY, table;
    private long idFak;
    private Button btnAdd;
    private RecyclerView rv;
    private TextView tvTotal, tvDiscount;
    private Spinner spinner;
    public int showTypeTotalPrice = 0;
    private int position;
    private double[] totalPrice;
    private double discount;
    private ShPInvoice shPInvoice;
    private SubscriptionPointModel subscriptionPoint;
    private ArrayList<InvoiceModel> invoices;
    private ArrayList<InvoiceListModel> invoicesList;
    private ArrayList<SummaryInvoicesListModel> summaryInvocesList = new ArrayList<>();

    private boolean showDetail = true;

    private PozeModel poze;


    public static InvoiceFragment newInstance(String tableFak,String tableNow, String tablePay, long idFak, int position) {
        InvoiceFragment invoiceFragment = new InvoiceFragment();
        Bundle bundle = new Bundle();
        bundle.putLong(ID_FAK, idFak);
        bundle.putString(TABLE_FAK, tableFak);
        bundle.putString(TABLE_NOW, tableNow);
        bundle.putString(TABLE_PAY, tablePay);
        bundle.putInt(POSITION, position);
        invoiceFragment.setArguments(bundle);
        return invoiceFragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            tableFAK = getArguments().getString(TABLE_FAK);
            tableNOW = getArguments().getString(TABLE_NOW);
            tablePAY = getArguments().getString(TABLE_PAY);
            idFak = getArguments().getLong(ID_FAK);
            position = getArguments().getInt(POSITION);
        }
        if (savedInstanceState != null) {
            idFak = savedInstanceState.getLong(ID_FAK);
            position = savedInstanceState.getInt(POSITION);
        }
        table = tableFAK;
        if(idFak == -1L) {
            table = tableNOW;
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_invoice, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        rv = view.findViewById(R.id.recycleViewInvoice);
        btnAdd = view.findViewById(R.id.btnAddPayment);
        tvTotal = view.findViewById(R.id.tvTotal);
        tvDiscount = view.findViewById(R.id.tvDiscountInvoice);
        spinner = view.findViewById(R.id.spInvoice);

        btnAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                InvoiceAddFragment invoiceAddFragment = InvoiceAddFragment.newInstance(table, idFak);
                FragmentChange.replace(getActivity(), invoiceAddFragment, FragmentChange.Transaction.MOVE, true);
            }
        });
        tvTotal.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addOneShowTypeTotalPrice();
                setTotalTextView();
            }
        });
        //skrytí tlačítka pro přidání nového záznamu faktury v režimu bezfaktury
        if(idFak == -1L) {
            btnAdd.setVisibility(View.GONE);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        rv.setAdapter(null);
        shPInvoice = new ShPInvoice(getActivity());
        showTypeTotalPrice = shPInvoice.get(ShPInvoice.SHOW_TYPE_TOTAL_PRICE, 0);

        /*MySpinnerInvoiceListAdapter invoiceListAdapter = new MySpinnerInvoiceListAdapter(getActivity(), R.layout.item_own_simple_list, invoicesList);
        spinner.setAdapter(invoiceListAdapter);
        spinner.setSelection(position);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                InvoiceFragment.this.position = position;
                idFak = invoicesList.get(position).getIdFak();
                loadInvoice();
                setTotalTextView();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });*/

        loadInvoice();
        setRecyclerView();
        setTotalTextView();

    }

    @Override
    public void onPause() {
        super.onPause();
        rv.setAdapter(null);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putLong(ID_FAK, idFak);
        outState.putInt(POSITION, position);
    }

    private void loadInvoice() {
        DataSubscriptionPointSource dataSubscriptionPointSource = new DataSubscriptionPointSource(getActivity());
        dataSubscriptionPointSource.open();
        subscriptionPoint = SubscriptionPoint.load(getActivity());
        invoices = dataSubscriptionPointSource.loadInvoices(idFak, table);
        invoicesList = dataSubscriptionPointSource.loadInvoiceLists(subscriptionPoint);
        discount = dataSubscriptionPointSource.sumDiscount(idFak, tablePAY);
        dataSubscriptionPointSource.close();

        poze = Calculation.getPoze(invoices, subscriptionPoint.getCountPhaze(), subscriptionPoint.getPhaze(), getActivity());
        totalPrice = calculationTotalInvoice(invoices, subscriptionPoint, poze);
        PaymentModel.getDiscountDPHText(discount, tvDiscount);

    }

    private void setRecyclerView() {
        Log.w(TAG, "Invoices "+invoices.size());
        InvoiceAdapter invoiceAdapter = new InvoiceAdapter(getActivity(), invoices, table, idFak, subscriptionPoint, poze.getTypePoze(), rv);
        invoiceAdapter.setUpdateListener(() -> onResume());


        rv.setAdapter(invoiceAdapter);
        rv.setLayoutManager(new LinearLayoutManager(getActivity()));
        rv.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                rv.getViewTreeObserver().removeOnPreDrawListener(this);

                final WindowManager wm = (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);
                int height = wm.getDefaultDisplay().getHeight();
                rv.getMeasuredHeight();

                for (int i = 0; i < rv.getChildCount(); i++) {

                    View v = rv.getChildAt(i);
                    Animation animation = AnimationUtils.loadAnimation(getContext(), R.anim.item_animation_fall_down);
                    v.startAnimation(animation);
                    /*v.setAlpha(0.0f);
                    v.setTranslationY(((-1*v.getHeight()*20/100)*(i+1)));
                    v.setScaleY(1.05f);
                    v.setScaleX(1.05f);
                    v.setPivotX(0.5f);
                    v.setPivotY(0.5f);
                    v.animate().alpha(1.0f).translationY(0)
                            .scaleX(1).scaleY(1)
                            .setDuration(500)
                            .setStartDelay(i * 15*500/100)
                            .start();*/
                }
                return true;
            }
        });

    }




    /**
     * Provede výpočet cen všech položek ve faktuře
     *
     * @param invoices
     * @param subscriptionPoint
     */
    private double[] calculationTotalInvoice(ArrayList<InvoiceModel> invoices, SubscriptionPointModel subscriptionPoint, PozeModel poze) {
        DataSubscriptionPointSource dataSubscriptionPointSource = new DataSubscriptionPointSource(getActivity());
        dataSubscriptionPointSource.open();
        double payment = dataSubscriptionPointSource.sumPayment(idFak, tablePAY);
        dataSubscriptionPointSource.close();

        double[] priceTotal = new double[4];
        double total = 0, totalDPH = 0;
        double totalVt = 0, totalNT = 0;
        double totalPriceVt = 0, totalPriceNt = 0, totalPayment = 0, totalPoze = 0, totalOtherServices = 0;
        for (int i = 0; i < invoices.size(); i++) {
            InvoiceModel invoice = invoices.get(i);
            PriceListModel priceList = getPriceList(invoice);
            //nastavení datumu odečtu
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(invoice.getDateFrom());
            int[] date = ViewHelper.parseIntsFromCalendar(calendar);

            //nastavení regulovaného ceníku
            PriceListRegulBuilder priceListRegulBuilder = new PriceListRegulBuilder(priceList, date[2], date[1], date[0]);
            priceList = priceListRegulBuilder.getRegulPriceList();
            String dateOf = ViewHelper.convertLongToTime(invoice.getDateFrom());
            String dateTo = ViewHelper.convertLongToTime(invoice.getDateTo());
            double differentDate = Calculation.differentMonth(dateOf, dateTo, DifferenceDate.TypeDate.INVOICE);
            double[] price = Calculation.calculatePriceWithoutPozeMwH(priceList, subscriptionPoint);
            double vt = (invoice.getVtEnd() - invoice.getVtStart()) / 1000;
            double nt = (invoice.getNtEnd() - invoice.getNtStart()) / 1000;
            double ntVt = nt + vt;
            totalVt += vt;
            totalNT += nt;
            price[0] *= vt;
            price[1] *= nt;
            price[2] *= differentDate;
            price[3] *= ntVt;
            totalOtherServices += (invoice.getOtherServices() * differentDate);


            for (int j = 0; j < priceTotal.length; j++) {
                priceTotal[j] += price[j];
                total += price[j];
                totalDPH += price[j] + (price[j] * priceList.getDph() / 100);
            }
            total += totalOtherServices;
            totalDPH += totalOtherServices + (totalOtherServices * priceList.getDph() / 100);
        }

        totalPriceVt = priceTotal[0];
        totalPriceNt = priceTotal[1];
        totalPayment = priceTotal[2];
        //totalPoze = priceTotal[3];
        totalPoze = poze.getPoze();

        return new double[]{totalVt, totalNT, (totalNT + totalVt), totalPriceVt, totalPriceNt, totalPayment, totalPoze, totalOtherServices, total, totalDPH - discount, payment, payment - totalDPH + discount};
    }


    /**
     * Načte ceník podle id uložený ve faktuře
     *
     * @param invoice
     * @return
     */
    private PriceListModel getPriceList(InvoiceModel invoice) {
        DataPriceListSource dataPriceListSource = new DataPriceListSource(getActivity());
        dataPriceListSource.open();
        PriceListModel priceList = dataPriceListSource.readPrice(invoice.getIdPriceList());
        dataPriceListSource.close();
        return priceList;
    }

    private void setTotalTextView() {

        String s = "", sEnd = "";
        DecimalFormat df;
        if (totalPrice[7] == 0 && showTypeTotalPrice == 7)
            showTypeTotalPrice++;//pokud jsou ostatní služby rovny 0, posune se zobrazení o další
        if (totalPrice[1] == 0 && showTypeTotalPrice == 1)
            showTypeTotalPrice = 3;//pokud spotřeba NT je 0 a zobrazení NT spotřeby NT, přeskočí se rovnou na cenu VT
        if (totalPrice[1] == 0 && showTypeTotalPrice == 4)
            showTypeTotalPrice++;//pokud spotřeba NT je 0 a zobrazení NT ceny NT, přeskočí se rovnou na cenu platů
        switch (showTypeTotalPrice) {
            case 0:
                s = "Celkem spotřeba VT: ";
                break;
            case 1:
                s = "Celkem spotřeba NT: ";
                break;
            case 2:
                s = "Celkem spotřeba VT+NT: ";
                break;
            case 3:
                s = "Cena VT (bez DPH): ";
                break;
            case 4:
                s = "Cena NT (bez DPH): ";
                break;
            case 5:
                s = "Cena st. platů (bez DPH): ";
                break;
            case 6:
                s = "Cena POZE (bez DPH): ";
                break;
            case 7:
                s = "Ostatní služby/slevy bez DPH: ";
                break;
            case 8:
                s = "Cena bez DPH: ";
                break;
            case 9:
                s = "Cena s DPH: ";
                break;
            case 10:
                s = "Zaplacené zálohy: ";
                break;
            case 11:
                s = "Bilance: ";
                break;

        }
        if (showTypeTotalPrice <= 2) {
            sEnd = " MWh";
            df = DecimalFormatHelper.df3;
        } else {
            sEnd = " kč";
            df = DecimalFormatHelper.df2;
        }
        tvTotal.setText(s + df.format(totalPrice[showTypeTotalPrice]) + sEnd);
    }

    /**
     * Připočítává typ zobrezní celkového součtu
     */
    private void addOneShowTypeTotalPrice() {
        shPInvoice.set(ShPInvoice.SHOW_TYPE_TOTAL_PRICE, showTypeTotalPrice);
        showTypeTotalPrice++;
        if (showTypeTotalPrice > totalPrice.length - 1) {
            showTypeTotalPrice = 0;
        }
    }

}
