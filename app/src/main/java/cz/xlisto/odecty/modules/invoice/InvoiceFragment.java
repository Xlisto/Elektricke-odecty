package cz.xlisto.odecty.modules.invoice;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Calendar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import cz.xlisto.odecty.R;
import cz.xlisto.odecty.databaze.DataInvoiceSource;
import cz.xlisto.odecty.databaze.DataPriceListSource;
import cz.xlisto.odecty.dialogs.YesNoDialogFragment;
import cz.xlisto.odecty.models.InvoiceModel;
import cz.xlisto.odecty.models.PaymentModel;
import cz.xlisto.odecty.models.PozeModel;
import cz.xlisto.odecty.models.PriceListModel;
import cz.xlisto.odecty.models.PriceListRegulBuilder;
import cz.xlisto.odecty.models.SubscriptionPointModel;
import cz.xlisto.odecty.ownview.ViewHelper;
import cz.xlisto.odecty.shp.ShPInvoice;
import cz.xlisto.odecty.utils.Calculation;
import cz.xlisto.odecty.utils.DifferenceDate;
import cz.xlisto.odecty.utils.FragmentChange;
import cz.xlisto.odecty.utils.SubscriptionPoint;

import static cz.xlisto.odecty.models.PriceListModel.NEW_POZE_YEAR;


/**
 * Fragment zobrazení jednotlivých záznamů ve faktuře
 * Xlisto 04.02.2023 10:52
 */
public class InvoiceFragment extends Fragment {
    private static final String TAG = "InvoiceFragment";
    private static final String ID_FAK = "idFak";
    private static final String TABLE_FAK = "tableFak";
    private static final String TABLE_NOW = "tableMow";
    private static final String TABLE_PAY = "tablePay";
    private static final String POSITION = "position";
    private String tableFAK, tableNOW, tablePAY, table;
    private long idFak;
    private RecyclerView rv;
    private TextView tvTotal, tvDiscount;
    public int showTypeTotalPrice = 0;
    private int position;
    private double[] totalPrice;
    private double discount;
    private ShPInvoice shPInvoice;
    private SubscriptionPointModel subscriptionPoint;
    private ArrayList<InvoiceModel> invoices;
    private PozeModel poze;
    private InvoiceAdapter invoiceAdapter;


    public static InvoiceFragment newInstance(String tableFak, String tableNow, String tablePay, long idFak, int position) {
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
        if (idFak == -1L) {
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
        Button btnAdd = view.findViewById(R.id.btnAddPayment);
        tvTotal = view.findViewById(R.id.tvTotal);
        tvDiscount = view.findViewById(R.id.tvDiscountInvoice);

        btnAdd.setOnClickListener(v -> {
            InvoiceAddFragment invoiceAddFragment = InvoiceAddFragment.newInstance(table, idFak);
            FragmentChange.replace(requireActivity(), invoiceAddFragment, FragmentChange.Transaction.MOVE, true);
        });
        tvTotal.setOnClickListener(v -> {
            addOneShowTypeTotalPrice();
            setTotalTextView();
        });

        //skrytí tlačítka pro přidání nového záznamu faktury v režimu bezfaktury
        if (idFak == -1L) {
            btnAdd.setVisibility(View.GONE);
        }

        //posluchač na změnu počtu záznamů ve faktuře - spojení záznamů
        requireActivity().getSupportFragmentManager().setFragmentResultListener(
                InvoiceJoinDialogFragment.RESULT_JOIN_DIALOG_FRAGMENT,
                this,
                (requestKey, result) -> {
                    if (result.getBoolean(InvoiceJoinDialogFragment.RESULT)) {
                        loadInvoice();
                        invoiceAdapter.setUpdateJoin(invoices, position);
                    }
                });
        //posluchač na změnu počtu záznamů ve faktuře - rozdělení záznamů
        requireActivity().getSupportFragmentManager().setFragmentResultListener(
                InvoiceCutDialogFragment.RESULT_CUT_DIALOG_FRAGMENT,
                this,
                (requestKey, result) -> {
                    if (result.getBoolean(InvoiceCutDialogFragment.RESULT)) {
                        loadInvoice();
                        invoiceAdapter.setUpdateCut(invoices, position);
                    }
                });
        //posluchač na odstranění záznamu ve faktuře
        requireActivity().getSupportFragmentManager().setFragmentResultListener(
                InvoiceAdapter.INVOICE_ADAPTER_DELETE_INVOICE,
                this,
                (requestKey, result) -> {
                    if (result.getBoolean(YesNoDialogFragment.RESULT)) {
                        invoiceAdapter.deleteItem();
                    }
                });
    }


    @Override
    public void onResume() {
        super.onResume();
        rv.setAdapter(null);
        shPInvoice = new ShPInvoice(getActivity());
        showTypeTotalPrice = shPInvoice.get(ShPInvoice.SHOW_TYPE_TOTAL_PRICE, 0);

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
        DataInvoiceSource dataInvoiceSource = new DataInvoiceSource(getActivity());
        dataInvoiceSource.open();
        subscriptionPoint = SubscriptionPoint.load(getActivity());
        //zkontroluje zda existuje záznam v tabulce NOW, pokud ne, vytvoří prázdný záznam
        boolean exists = dataInvoiceSource.checkInvoiceExists(tableNOW);
        if (!exists) {
            dataInvoiceSource.insertFirstRecordWithoutInvoice(tableNOW);
        }
        invoices = dataInvoiceSource.loadInvoices(idFak, table);
        discount = dataInvoiceSource.sumDiscount(idFak, tablePAY);
        dataInvoiceSource.close();

        poze = Calculation.getPoze(invoices, subscriptionPoint.getCountPhaze(), subscriptionPoint.getPhaze(), getActivity());
        totalPrice = calculationTotalInvoice(invoices, subscriptionPoint, poze);
        PaymentModel.getDiscountDPHText(discount, tvDiscount);
    }


    /**
     * Nastaví na RecyclerView adaptér, zajistí animaci při vytváření
     */
    public void setRecyclerView() {
        invoiceAdapter = new InvoiceAdapter(getActivity(), invoices, table, subscriptionPoint, poze.getTypePoze(), rv);

        rv.setAdapter(invoiceAdapter);
        rv.setLayoutManager(new LinearLayoutManager(getActivity()));
        rv.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                rv.getViewTreeObserver().removeOnPreDrawListener(this);
                rv.getMeasuredHeight();

                for (int i = 0; i < rv.getChildCount(); i++) {

                    View v = rv.getChildAt(i);
                    Animation animation = AnimationUtils.loadAnimation(getContext(), R.anim.item_animation_fall_down);
                    v.startAnimation(animation);
                }
                return true;
            }
        });
    }


    /**
     * Provede výpočet cen všech položek ve faktuře
     *
     * @param invoices          seznam položek ve faktuře
     * @param subscriptionPoint nastavení odběrného místa
     */
    private double[] calculationTotalInvoice(ArrayList<InvoiceModel> invoices, SubscriptionPointModel subscriptionPoint, PozeModel poze) {
        DataInvoiceSource dataInvoiceSource = new DataInvoiceSource(requireActivity());
        dataInvoiceSource.open();
        double payment = dataInvoiceSource.sumPayment(idFak, tablePAY);
        dataInvoiceSource.close();

        double[] priceTotal = new double[4];
        double total = 0, totalDPH = 0;
        double totalVt = 0, totalNT = 0;
        double totalPriceVt, totalPriceNt, totalPayment, totalPoze, totalOtherServices = 0;
        for (int i = 0; i < invoices.size(); i++) {
            InvoiceModel invoice = invoices.get(i);
            PriceListModel priceList = getPriceList(invoice);
            if (priceList == null) {
                priceList = new PriceListModel();
            }
            //nastavení datumu odečtu
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(invoice.getDateFrom());
            int[] date = ViewHelper.parseIntsFromCalendar(calendar);

            //nastavení regulovaného ceníku
            PriceListRegulBuilder priceListRegulBuilder = new PriceListRegulBuilder(priceList, date[2], date[1], date[0]);
            PriceListModel regulPriceList = priceListRegulBuilder.getRegulPriceList();
            String dateOf = ViewHelper.convertLongToDate(invoice.getDateFrom());
            String dateTo = ViewHelper.convertLongToDate(invoice.getDateTo());
            double differentDate = Calculation.differentMonth(dateOf, dateTo, DifferenceDate.TypeDate.INVOICE);
            double[] price = Calculation.calculatePriceWithoutPozeMwH(regulPriceList, subscriptionPoint);
            double vt = (invoice.getVtEnd() - invoice.getVtStart()) / 1000;
            double nt = (invoice.getNtEnd() - invoice.getNtStart()) / 1000;
            double ntVt = nt + vt;
            totalVt += vt;
            totalNT += nt;
            price[0] *= vt;
            price[1] *= nt;
            price[2] *= differentDate;

            //TODO předělat objekt POZE, aby obsahoval tuto podmínku, stejná je v Calculation, možná komplet výpočet přesunout do Calculation
            //poze počítá podle typu, který se vybere podle celkové spotřeby na faktuře
            if (poze.getTypePoze() == PozeModel.TypePoze.POZE2) {
                if(priceList.getRokPlatnost() < NEW_POZE_YEAR){
                    price[3] = ntVt * regulPriceList.getOze();//poze dle spotřeby starší ceník
                }else{
                    price[3] = ntVt * regulPriceList.getPoze2();//poze dle spotřeby novější ceník
                }
            } else {
                price[3] = subscriptionPoint.getCountPhaze() * subscriptionPoint.getPhaze() * differentDate * regulPriceList.getPoze1();//poze dle jističe
            }

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
        totalPoze = priceTotal[3];

        return new double[]{totalVt, totalNT, (totalNT + totalVt), totalPriceVt, totalPriceNt,
                totalPayment, totalPoze, totalOtherServices, total, totalDPH - discount, payment, payment - totalDPH + discount};
    }


    /**
     * Načte ceník podle id uložený ve faktuře
     *
     * @param invoice Objekt faktury
     * @return Objekt ceníku
     */
    private PriceListModel getPriceList(InvoiceModel invoice) {
        DataPriceListSource dataPriceListSource = new DataPriceListSource(getActivity());
        dataPriceListSource.open();
        PriceListModel priceList = dataPriceListSource.readPrice(invoice.getIdPriceList());
        dataPriceListSource.close();
        return priceList;
    }


    /**
     * Nastaví textview s celkovou cenou
     */
    private void setTotalTextView() {

        String s;
        if (totalPrice[7] == 0 && showTypeTotalPrice == 7)
            showTypeTotalPrice++;//pokud jsou ostatní služby rovny 0, posune se zobrazení o další
        if (totalPrice[1] == 0 && showTypeTotalPrice == 1)
            showTypeTotalPrice = 3;//pokud spotřeba NT je 0 a zobrazení NT spotřeby NT, přeskočí se rovnou na cenu VT
        if (totalPrice[1] == 0 && showTypeTotalPrice == 4)
            showTypeTotalPrice++;//pokud spotřeba NT je 0 a zobrazení NT ceny NT, přeskočí se rovnou na cenu platů
        switch (showTypeTotalPrice) {
            case 0:
                s = getResources().getString(R.string.total_vt, totalPrice[showTypeTotalPrice]);
                break;
            case 1:
                s = getResources().getString(R.string.total_nt, totalPrice[showTypeTotalPrice]);
                break;
            case 2:
                s = getResources().getString(R.string.total_vt_nt, totalPrice[showTypeTotalPrice]);
                break;
            case 3:
                s = getResources().getString(R.string.price_vt, totalPrice[showTypeTotalPrice]);
                break;
            case 4:
                s = getResources().getString(R.string.price_nt, totalPrice[showTypeTotalPrice]);
                break;
            case 5:
                s = getResources().getString(R.string.price_fixed_salary, totalPrice[showTypeTotalPrice]);
                break;
            case 6:
                s = getResources().getString(R.string.price_poze, totalPrice[showTypeTotalPrice]);
                break;
            case 7:
                s = getResources().getString(R.string.price_other_services, totalPrice[showTypeTotalPrice]);
                break;
            case 8:
                s = getResources().getString(R.string.price_without_taxes, totalPrice[showTypeTotalPrice]);
                break;
            case 9:
                s = getResources().getString(R.string.price_with_taxes, totalPrice[showTypeTotalPrice]);
                break;
            case 10:
                s = getResources().getString(R.string.paymented_advances, totalPrice[showTypeTotalPrice]);
                break;
            case 11:
                s = getResources().getString(R.string.balance, totalPrice[showTypeTotalPrice]);
                break;
            default:
                s = "";

        }
        tvTotal.setText(s);
    }


    /**
     * Připočítává typ zobrazení celkového součtu
     */
    private void addOneShowTypeTotalPrice() {
        shPInvoice.set(ShPInvoice.SHOW_TYPE_TOTAL_PRICE, showTypeTotalPrice);
        showTypeTotalPrice++;
        if (showTypeTotalPrice > totalPrice.length - 1) {
            showTypeTotalPrice = 0;
        }
    }
}
