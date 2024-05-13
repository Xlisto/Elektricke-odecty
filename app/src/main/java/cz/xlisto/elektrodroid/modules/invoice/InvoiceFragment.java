package cz.xlisto.elektrodroid.modules.invoice;


import static cz.xlisto.elektrodroid.models.PriceListModel.NEW_POZE_YEAR;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;

import cz.xlisto.elektrodroid.R;
import cz.xlisto.elektrodroid.databaze.DataInvoiceSource;
import cz.xlisto.elektrodroid.databaze.DataMonthlyReadingSource;
import cz.xlisto.elektrodroid.databaze.DataPriceListSource;
import cz.xlisto.elektrodroid.databaze.DataSubscriptionPointSource;
import cz.xlisto.elektrodroid.dialogs.SettingsInvoiceDialogFragment;
import cz.xlisto.elektrodroid.dialogs.SettingsViewDialogFragment;
import cz.xlisto.elektrodroid.dialogs.YesNoDialogFragment;
import cz.xlisto.elektrodroid.models.InvoiceModel;
import cz.xlisto.elektrodroid.models.MonthlyReadingModel;
import cz.xlisto.elektrodroid.models.PaymentModel;
import cz.xlisto.elektrodroid.models.PozeModel;
import cz.xlisto.elektrodroid.models.PriceListModel;
import cz.xlisto.elektrodroid.models.PriceListRegulBuilder;
import cz.xlisto.elektrodroid.models.SubscriptionPointModel;
import cz.xlisto.elektrodroid.ownview.ViewHelper;
import cz.xlisto.elektrodroid.shp.ShPInvoice;
import cz.xlisto.elektrodroid.utils.Calculation;
import cz.xlisto.elektrodroid.utils.DifferenceDate;
import cz.xlisto.elektrodroid.utils.FragmentChange;
import cz.xlisto.elektrodroid.utils.SubscriptionPoint;
import cz.xlisto.elektrodroid.utils.UIHelper;


/**
 * Fragment zobrazení jednotlivých záznamů ve faktuře
 * Xlisto 04.02.2023 10:52
 */
public class InvoiceFragment extends Fragment {

    private static final String TAG = "InvoiceFragment";
    private static final String ID_FAK = "idFak";
    private static final String TABLE_FAK = "tableFak";
    private static final String TABLE_NOW = "tableNow";
    private static final String TABLE_PAY = "tablePay";
    private static final String TABLE_READ = "tableRead";
    private static final String POSITION = "position";
    private String tableRead, tableFAK, tableNOW, tablePAY, table;
    private long idFak;
    private RecyclerView rv;
    private TextView tvTotal, tvDiscount;
    public int showTypeTotalPrice = 0;
    private int position;
    private double[] totalPrice;
    private double discountWithTax;
    private ShPInvoice shPInvoice;
    private SubscriptionPointModel subscriptionPoint;
    private ArrayList<InvoiceModel> invoices;
    private PozeModel poze;
    private InvoiceAdapter invoiceAdapter;
    boolean showCheckBoxSelect = false;
    private Button btnCreateInvoice, btnAddItemInvoice;
    private FloatingActionButton fab;


    public static InvoiceFragment newInstance(String tableFak, String tableNow, String tablePay, String tableRead, long idFak, int position) {
        InvoiceFragment invoiceFragment = new InvoiceFragment();
        Bundle bundle = new Bundle();
        bundle.putLong(ID_FAK, idFak);
        bundle.putString(TABLE_FAK, tableFak);
        bundle.putString(TABLE_NOW, tableNow);
        bundle.putString(TABLE_PAY, tablePay);
        bundle.putString(TABLE_READ, tableRead);
        bundle.putInt(POSITION, position);
        invoiceFragment.setArguments(bundle);
        return invoiceFragment;
    }


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        if (getArguments() != null) {
            tableFAK = getArguments().getString(TABLE_FAK);
            tableNOW = getArguments().getString(TABLE_NOW);
            tablePAY = getArguments().getString(TABLE_PAY);
            tableRead = getArguments().getString(TABLE_READ);
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
        btnAddItemInvoice = view.findViewById(R.id.btnAddItemInvoice);
        fab = view.findViewById(R.id.fab);
        btnCreateInvoice = view.findViewById(R.id.btnCreateInvoice);
        tvTotal = view.findViewById(R.id.tvTotal);
        tvDiscount = view.findViewById(R.id.tvDiscountInvoice);
        btnAddItemInvoice.setOnClickListener(v -> showAddInvoice());
        fab.setOnClickListener(v -> showAddInvoice());
        btnCreateInvoice.setOnClickListener(v -> {
            //vytvoření nové faktury z vybraných záznamů
            InvoiceCreateDialogFragment invoiceCreateDialogFragment = InvoiceCreateDialogFragment.newInstance();
            invoiceCreateDialogFragment.show(requireActivity().getSupportFragmentManager(), InvoiceCreateDialogFragment.TAG);
        });
        tvTotal.setOnClickListener(v -> {
            addOneShowTypeTotalPrice();
            setTotalTextView();
        });
        //změna textu tlačítka podle toho, zda se jedná o zobrazení stávající faktury nebo období bez faktury
        if (idFak == -1L) {
            btnAddItemInvoice.setText(getResources().getString(R.string.select_invoice));
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
        //posluchač na vytvoření nové faktury zw záznamů v období bez faktury
        requireActivity().getSupportFragmentManager().setFragmentResultListener(
                InvoiceCreateDialogFragment.RESULT_CREATE_DIALOG_FRAGMENT,
                this,
                (requestKey, result) -> {
                    if (result.getBoolean(InvoiceCreateDialogFragment.RESULT)) {
                        createInvoice(result.getString(InvoiceCreateDialogFragment.NUMBER, "0"));
                    }
                });
        //posluchač na změnu nastavení automatického generování faktury
        requireActivity().getSupportFragmentManager().setFragmentResultListener(
                SettingsInvoiceDialogFragment.FLAG_RESULT_DIALOG_FRAGMENT,
                this,
                (requestKey, result) -> {
                    if (result.getBoolean(SettingsInvoiceDialogFragment.RESULT) && invoiceAdapter != null) {
                        invoiceAdapter.resetButtons();
                    }
                });
        //posluchač zavření dialogová okna nastavení
        requireActivity().getSupportFragmentManager().setFragmentResultListener(SettingsViewDialogFragment.FLAG_UPDATE_SETTINGS, this,
                (requestKey, bundle) -> UIHelper.showButtons(btnAddItemInvoice, fab, requireActivity(), true)
        );
    }


    @Override
    public void onResume() {
        super.onResume();
        rv.setAdapter(null);
        shPInvoice = new ShPInvoice(requireContext());
        showTypeTotalPrice = shPInvoice.get(ShPInvoice.SHOW_TYPE_TOTAL_PRICE, 0);
        loadInvoice();
        setRecyclerView();
        setTotalTextView();
        UIHelper.showButtons(btnAddItemInvoice, fab, requireActivity(), true);
    }


    @Override
    public void onPause() {
        super.onPause();
        rv.setAdapter(null);
        //skrytí checkboxů, skrytí tlačítka pro vytvoření faktury
        showCheckBoxSelect = false;
        btnCreateInvoice.setVisibility(View.GONE);
        invoiceAdapter.setShowCheckBoxSelect(false);
    }


    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putLong(ID_FAK, idFak);
        outState.putInt(POSITION, position);
    }


    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        if (idFak == -1L)
            inflater.inflate(R.menu.menu_invoice, menu);
    }


    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.menu_invoice_item_settings) {
            SettingsInvoiceDialogFragment.newInstance().show(requireActivity().getSupportFragmentManager(), SettingsInvoiceDialogFragment.TAG);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }


    /**
     * Načte záznamy ve faktuře
     */
    private void loadInvoice() {
        DataInvoiceSource dataInvoiceSource = new DataInvoiceSource(getActivity());
        dataInvoiceSource.open();
        subscriptionPoint = SubscriptionPoint.load(getActivity());
        //zkontroluje zda existuje záznam v tabulce NOW, pokud ne, vytvoří prázdný záznam
        checkInvoiceItem(dataInvoiceSource);
        invoices = dataInvoiceSource.loadInvoices(idFak, table);
        discountWithTax = dataInvoiceSource.sumDiscountWithTax(idFak, tablePAY);
        dataInvoiceSource.close();
        poze = Calculation.getPoze(invoices, subscriptionPoint.getCountPhaze(), subscriptionPoint.getPhaze(), getActivity());
        totalPrice = calculationTotalInvoice(invoices, subscriptionPoint, poze);
        PaymentModel.getDiscountDPHText(discountWithTax, tvDiscount);
    }


    /**
     * Vytvoří novou fakturu z vybraných záznamů v období bez faktury
     */
    private void createInvoice(String number) {
        Log.w(TAG, "createInvoice: vytvoření nové faktury ");
        //vytvoří nový záznam v seznamu faktur
        DataInvoiceSource dataInvoiceSource = new DataInvoiceSource(requireContext());
        DataSubscriptionPointSource dataSubscriptionPointSource = new DataSubscriptionPointSource(getActivity());
        dataInvoiceSource.open();
        dataSubscriptionPointSource.open();
        long idFak = dataInvoiceSource.insertInvoiceList(number, subscriptionPoint.getId());
        for (int i = 0; i < invoices.size(); i++) {
            InvoiceModel invoice = invoices.get(i);
            if (invoice.isSelected()) {
                invoice.setIdInvoice(idFak);
                dataInvoiceSource.insertInvoice(tableFAK, invoice);
                dataInvoiceSource.deleteInvoice(tableNOW, invoice.getId());
                dataSubscriptionPointSource.changeInvoicePayment(idFak, tablePAY, invoice);
            }
        }
        checkInvoiceItem(dataInvoiceSource);
        dataInvoiceSource.close();
        dataSubscriptionPointSource.close();
        DataMonthlyReadingSource dataMonthlyReadingSource = new DataMonthlyReadingSource(requireContext());
        dataMonthlyReadingSource.open();
        MonthlyReadingModel monthlyReading = dataMonthlyReadingSource.loadLastMonthlyReadingByDate(tableRead);
        dataMonthlyReadingSource.close();
        WithOutInvoiceService.editFirstItemInInvoice(requireContext());
        WithOutInvoiceService.editLastItemInInvoice(requireContext(), tableNOW, monthlyReading);
        onResume();
    }


    /**
     * Zkontroluje zda existuje záznam v tabulce se záznamy pro období bez faktur, pokud ne, vytvoří prázdný záznam.
     * Spouštět se musí s otevřenou databází.
     *
     * @param datasource DataInvoiceSource
     */
    private void checkInvoiceItem(DataInvoiceSource datasource) {
        boolean exists = datasource.checkInvoiceExists(tableNOW);
        if (!exists) {
            datasource.insertFirstRecordWithoutInvoice(tableNOW);
        }
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
                    Animation animation = AnimationUtils.loadAnimation(requireContext(), R.anim.item_animation_fall_down);
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
        double payment = dataInvoiceSource.sumPayment(idFak, tablePAY); //platby
        //dataInvoiceSource.close();
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
            //nastavení regulovaného ceníku
            PriceListRegulBuilder priceListRegulBuilder = new PriceListRegulBuilder(priceList, invoice);
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
                if (priceList.getRokPlatnost() < NEW_POZE_YEAR) {
                    price[3] = ntVt * regulPriceList.getOze();//poze dle spotřeby starší ceník
                } else {
                    price[3] = ntVt * regulPriceList.getPoze2();//poze dle spotřeby novější ceník
                }
            } else {
                price[3] = subscriptionPoint.getCountPhaze() * subscriptionPoint.getPhaze() * differentDate * regulPriceList.getPoze1();//poze dle jističe
            }
            double otherServices = (invoice.getOtherServices() * differentDate);
            totalOtherServices += otherServices;
            for (int j = 0; j < priceTotal.length; j++) {
                priceTotal[j] += price[j];
                total += price[j];
                totalDPH += price[j] + (price[j] * priceList.getDph() / 100);
            }
            double discountWithoutTax = dataInvoiceSource.sumDiscountWithoutTax(idFak, tablePAY, invoice.getDateFrom(), invoice.getDateTo(), invoice.getIdInvoice());//sleva bez DPH
            total += otherServices - discountWithoutTax;
            totalDPH += otherServices - discountWithoutTax + ((otherServices - discountWithoutTax) * priceList.getDph() / 100);
        }
        dataInvoiceSource.close();
        totalPriceVt = priceTotal[0];
        totalPriceNt = priceTotal[1];
        totalPayment = priceTotal[2];
        totalPoze = priceTotal[3];
        return new double[]{totalVt, totalNT, (totalNT + totalVt), totalPriceVt, totalPriceNt,
                totalPayment, totalPoze, totalOtherServices, total, totalDPH - discountWithTax, payment, payment - totalDPH + discountWithTax};
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


    private void showAddInvoice() {
        if (idFak == -1L) {
            //zobrazit checkboxy pro výběr záznamů pro novou fakturu
            showCheckBoxSelect = !showCheckBoxSelect;
            invoiceAdapter.setShowCheckBoxSelect(showCheckBoxSelect);
            if (showCheckBoxSelect)
                btnCreateInvoice.setVisibility(View.VISIBLE);
            else
                btnCreateInvoice.setVisibility(View.GONE);
            //invoiceAdapter.notifyItemRangeChanged(0, invoiceAdapter.getItemCount());
        } else {
            btnCreateInvoice.setVisibility(View.GONE);
            showCheckBoxSelect = false;
            //přidání záznamu do faktury
            InvoiceAddFragment invoiceAddFragment = InvoiceAddFragment.newInstance(table, idFak);
            FragmentChange.replace(requireActivity(), invoiceAddFragment, FragmentChange.Transaction.MOVE, true);
        }
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
