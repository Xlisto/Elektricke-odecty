package cz.xlisto.elektrodroid.modules.invoice;


import static cz.xlisto.elektrodroid.models.PriceListModel.NEW_POZE_YEAR;
import static cz.xlisto.elektrodroid.modules.invoice.InvoiceAbstract.D01;
import static cz.xlisto.elektrodroid.modules.invoice.InvoiceAbstract.D02;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Objects;

import cz.xlisto.elektrodroid.R;
import cz.xlisto.elektrodroid.databaze.DataInvoiceSource;
import cz.xlisto.elektrodroid.databaze.DataPriceListSource;
import cz.xlisto.elektrodroid.format.DecimalFormatHelper;
import cz.xlisto.elektrodroid.models.InvoiceModel;
import cz.xlisto.elektrodroid.models.PozeModel;
import cz.xlisto.elektrodroid.models.PriceListModel;
import cz.xlisto.elektrodroid.models.PriceListRegulBuilder;
import cz.xlisto.elektrodroid.models.SubscriptionPointModel;
import cz.xlisto.elektrodroid.models.SummaryInvoiceModel;
import cz.xlisto.elektrodroid.ownview.ViewHelper;
import cz.xlisto.elektrodroid.shp.ShPInvoiceDetail;
import cz.xlisto.elektrodroid.utils.Calculation;
import cz.xlisto.elektrodroid.utils.DifferenceDate;
import cz.xlisto.elektrodroid.utils.SubscriptionPoint;


/**
 * Fragment pro zobrazení detailu (jednotlivých složek ceny) faktury.
 */
public class InvoiceDetailFragment extends Fragment {

    private static final String TAG = "InvoiceDetailFragment";
    private static final String ID_FAK = "idFak";
    private static final String TABLE_FAK = "tableFak";
    private static final String TABLE_NOW = "tableNow";
    private static final String TABLE_PAY = "tablePay";
    private static final String POSITION = "position";
    private static final String SHOW_TYPE_DETAIL_INDEX = "showTypeDetailIndex";
    private InvoiceDetailAdapter invoiceDetailAdapter;
    private String tableFAK, tableNOW, table;
    private long idFak;
    private int position;
    private RecyclerView rv;
    private Spinner spinner;
    private int showTypeDetailIndex;
    private TextView tvTotal;
    private ShPInvoiceDetail shPInvoiceDetail;
    private ArrayList<InvoiceModel> invoices;
    private final ArrayList<SummaryInvoiceModel> summaryInvoices = new ArrayList<>();
    private final ArrayList<SummaryInvoiceModel> mergedSummaryInvoices = new ArrayList<>();
    private MySpinnerInvoiceDetailAdapter invoiceListAdapter;
    private MySpinnerInvoiceDetailAdapter.DatesInvoiceContainer datesInvoice;


    public InvoiceDetailFragment() {
        // Required empty public constructor
    }


    /**
     * Vytvoří instanci InvoiceDetailFragment.
     *
     * @param tableFak tabulka faktur
     * @param tableNow tabulka bez faktury (dříve TED)
     * @param tablePay tabulka plateb
     * @param idFak    long id faktury
     * @param position int pozice faktury
     * @return InvoiceDetailFragment - Instance InvoiceDetailFragment
     */
    public static InvoiceDetailFragment newInstance(String tableFak, String tableNow, String tablePay, long idFak, int position) {
        InvoiceDetailFragment invoiceDetailFragment = new InvoiceDetailFragment();
        Bundle bundle = new Bundle();
        bundle.putLong(ID_FAK, idFak);
        bundle.putString(TABLE_FAK, tableFak);
        bundle.putString(TABLE_NOW, tableNow);
        bundle.putString(TABLE_PAY, tablePay);
        bundle.putInt(POSITION, position);
        invoiceDetailFragment.setArguments(bundle);
        return invoiceDetailFragment;
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            tableFAK = getArguments().getString(TABLE_FAK);
            tableNOW = getArguments().getString(TABLE_NOW);
            idFak = getArguments().getLong(ID_FAK);
            position = getArguments().getInt(POSITION);
        }
        if (savedInstanceState != null) {
            idFak = savedInstanceState.getLong(ID_FAK);
            position = savedInstanceState.getInt(POSITION);
            showTypeDetailIndex = savedInstanceState.getInt(SHOW_TYPE_DETAIL_INDEX);
        }
        table = tableFAK;
        if (idFak == -1L) {
            table = tableNOW;
        }
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_invoice_detail, container, false);
    }


    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Button btn = view.findViewById(R.id.btn);
        rv = view.findViewById(R.id.recycleViewInvoice);
        tvTotal = view.findViewById(R.id.tvTotal);
        spinner = view.findViewById(R.id.spInvoice);

        btn.setOnClickListener(v -> {
            summaryInvoices.remove(0);
            int count = invoiceDetailAdapter.getItemCount();
            invoiceDetailAdapter.notifyItemRemoved(0);
            invoiceDetailAdapter.notifyItemRangeRemoved(0, count - 1);
        });
    }


    @Override
    public void onResume() {
        super.onResume();
        rv.setAdapter(null);
        shPInvoiceDetail = new ShPInvoiceDetail(getActivity());
        showTypeDetailIndex = shPInvoiceDetail.get(ShPInvoiceDetail.SHOW_TYPE_DETAIL_INDEX, 0);
        loadInvoice();
        setSpinner();
        calculationDetailsInvoice(invoices);
        setRecyclerView();
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
        outState.putInt(SHOW_TYPE_DETAIL_INDEX, showTypeDetailIndex);
    }


    private void setSpinner() {
        invoiceListAdapter = new MySpinnerInvoiceDetailAdapter(requireActivity(), R.layout.item_own_simple_list, SummaryInvoiceModel.Title.values(), datesInvoice);
        spinner.setAdapter(invoiceListAdapter);
        spinner.setSelection(shPInvoiceDetail.get(SHOW_TYPE_DETAIL_INDEX, 0));
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                shPInvoiceDetail.set(SHOW_TYPE_DETAIL_INDEX, spinner.getSelectedItemPosition());
                calculationDetailsInvoice(invoices);
                setRecyclerView();
            }


            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }


    private void loadInvoice() {
        DataInvoiceSource dataInvoiceSource = new DataInvoiceSource(requireActivity());
        dataInvoiceSource.open();
        invoices = dataInvoiceSource.loadInvoices(idFak, table);
        long minDateInvoice = dataInvoiceSource.minDateInvoice(idFak, table);
        long maxDateInvoice = dataInvoiceSource.maxDateInvoice(idFak, table);
        datesInvoice = new MySpinnerInvoiceDetailAdapter.DatesInvoiceContainer(minDateInvoice, maxDateInvoice);
        dataInvoiceSource.close();
    }


    private void setRecyclerView() {
        invoiceDetailAdapter = new InvoiceDetailAdapter(requireActivity(), summaryInvoices);
        invoiceDetailAdapter.setListener(totalPrice -> tvTotal.setText(requireContext().getResources().getString(R.string.total, DecimalFormatHelper.df2.format(totalPrice))));
        rv.setAdapter(invoiceDetailAdapter);

        rv.setLayoutManager(new LinearLayoutManager(getActivity()));
        rv.getViewTreeObserver().removeOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                rv.getViewTreeObserver().removeOnPreDrawListener(this);
                rv.getMeasuredHeight();

                for (int i = 0; i < rv.getChildCount(); i++) {
                    View v = rv.getChildAt(i);
                    v.setAlpha(1.0f);
                    v.animate().alpha(0.0f).translationY(0)
                            .setDuration(500)
                            .setStartDelay((long) i * 15 * 500 / 100)
                            .start();
                }
                return true;
            }
        });
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
     * Vypočítá detaily faktury a naplní seznamy summaryInvoices a mergedSummaryInvoices.
     *
     * @param invoices seznam faktur, které mají být zpracovány
     */
    private void calculationDetailsInvoice(ArrayList<InvoiceModel> invoices) {
        boolean hiddenNT = false;
        summaryInvoices.clear();
        SubscriptionPointModel subscriptionPoint = SubscriptionPoint.load(requireActivity());
        int phaze = Objects.requireNonNull(subscriptionPoint).getPhaze();
        int countPhaze = subscriptionPoint.getCountPhaze();
        PozeModel poze = Calculation.getPoze(invoices, countPhaze, phaze, requireActivity());

        for (int i = 0; i < invoices.size(); i++) {
            InvoiceModel invoice = invoices.get(i);
            PriceListRegulBuilder priceListRegulBuilder = new PriceListRegulBuilder(getPriceList(invoice), invoice);
            PriceListModel priceList = priceListRegulBuilder.getRegulPriceList();
            double priceBreaker = Calculation.calculatePriceBreaker(priceList, countPhaze, phaze);
            if (!hiddenNT) {
                if (priceList.getSazba().equals(D01) || priceList.getSazba().equals(D02)) {
                    hiddenNT = true;
                    if (spinner.getSelectedItemPosition() == 1)
                        spinner.setSelection(0);
                    if (spinner.getSelectedItemPosition() == 5)
                        spinner.setSelection(4);
                }
            }

            String s = spinner.getSelectedItem().toString();
            if (s.equals(SummaryInvoiceModel.Title.VT.toString())) {
                summaryInvoices.add(new SummaryInvoiceModel(invoice.getDateFrom(), invoice.getDateTo(),
                        invoice.getVt() / 1000, priceList.getCenaVT(),
                        SummaryInvoiceModel.Unit.MWH, SummaryInvoiceModel.Title.VT));
            } else if (s.equals(SummaryInvoiceModel.Title.NT.toString())) {
                summaryInvoices.add(new SummaryInvoiceModel(invoice.getDateFrom(), invoice.getDateTo(),
                        invoice.getNt() / 1000, priceList.getCenaNT(),
                        SummaryInvoiceModel.Unit.MWH, SummaryInvoiceModel.Title.NT));
            } else if (s.equals(SummaryInvoiceModel.Title.PAY.toString())) {
                summaryInvoices.add(new SummaryInvoiceModel(invoice.getDateFrom(), invoice.getDateTo(),
                        invoice.getDifferentDate(DifferenceDate.TypeDate.INVOICE), priceList.getMesicniPlat(),
                        SummaryInvoiceModel.Unit.MONTH, SummaryInvoiceModel.Title.PAY));
            } else if (s.equals(SummaryInvoiceModel.Title.TAX.toString())) {
                summaryInvoices.add(new SummaryInvoiceModel(invoice.getDateFrom(), invoice.getDateTo(),
                        invoice.getVtNt() / 1000, priceList.getDan(),
                        SummaryInvoiceModel.Unit.MWH, SummaryInvoiceModel.Title.TAX));
            } else if (s.equals(SummaryInvoiceModel.Title.VT_DIST.toString())) {
                summaryInvoices.add(new SummaryInvoiceModel(invoice.getDateFrom(), invoice.getDateTo(),
                        invoice.getVt() / 1000, priceList.getDistVT(),
                        SummaryInvoiceModel.Unit.MWH, SummaryInvoiceModel.Title.VT_DIST));
            } else if (s.equals(SummaryInvoiceModel.Title.NT_DIST.toString())) {
                summaryInvoices.add(new SummaryInvoiceModel(invoice.getDateFrom(), invoice.getDateTo(),
                        invoice.getNt() / 1000, priceList.getDistNT(),
                        SummaryInvoiceModel.Unit.MWH, SummaryInvoiceModel.Title.NT_DIST));
            } else if (s.equals(SummaryInvoiceModel.Title.CIRCUIT_BREAKER.toString())) {//jističe
                summaryInvoices.add(new SummaryInvoiceModel(invoice.getDateFrom(), invoice.getDateTo(),
                        invoice.getDifferentDate(DifferenceDate.TypeDate.INVOICE), priceBreaker,
                        SummaryInvoiceModel.Unit.MONTH, SummaryInvoiceModel.Title.TAX));
            } else if (s.equals(SummaryInvoiceModel.Title.SYS_SERVICES.toString())) {
                summaryInvoices.add(new SummaryInvoiceModel(invoice.getDateFrom(), invoice.getDateTo(),
                        invoice.getVtNt() / 1000, priceList.getSystemSluzby(),
                        SummaryInvoiceModel.Unit.MWH, SummaryInvoiceModel.Title.SYS_SERVICES));
            } else if (s.equals(SummaryInvoiceModel.Title.OTE.toString())) {
                if (invoice.getDateFrom() < (ViewHelper.parseCalendarFromString("01.07.2024").getTimeInMillis()))
                    summaryInvoices.add(new SummaryInvoiceModel(invoice.getDateFrom(), invoice.getDateTo(),
                            invoice.getDifferentDate(DifferenceDate.TypeDate.INVOICE), priceList.getCinnost(),
                            SummaryInvoiceModel.Unit.MONTH, SummaryInvoiceModel.Title.OTE));
            } else if (s.equals(SummaryInvoiceModel.Title.INF.toString())) {
                if (invoice.getDateFrom() >= (ViewHelper.parseCalendarFromString("01.07.2024").getTimeInMillis()))
                    summaryInvoices.add(new SummaryInvoiceModel(invoice.getDateFrom(), invoice.getDateTo(),
                            invoice.getDifferentDate(DifferenceDate.TypeDate.INVOICE), priceList.getCinnost(),
                            SummaryInvoiceModel.Unit.MONTH, SummaryInvoiceModel.Title.OTE));
            } else if (s.equals(SummaryInvoiceModel.Title.POZE.toString())) {
                if (poze.getTypePoze() == PozeModel.TypePoze.POZE2) {//podle spotřeby
                    if (priceList.getRokPlatnost() < NEW_POZE_YEAR) {
                        summaryInvoices.add(new SummaryInvoiceModel(invoice.getDateFrom(), invoice.getDateTo(),
                                invoice.getVtNt() / 1000, priceList.getOze(),
                                SummaryInvoiceModel.Unit.MWH, SummaryInvoiceModel.Title.POZE));
                    } else {
                        summaryInvoices.add(new SummaryInvoiceModel(invoice.getDateFrom(), invoice.getDateTo(),
                                invoice.getVtNt() / 1000, priceList.getPoze2(),
                                SummaryInvoiceModel.Unit.MWH, SummaryInvoiceModel.Title.POZE));
                    }
                } else {//podle jističe
                    summaryInvoices.add(new SummaryInvoiceModel(invoice.getDateFrom(), invoice.getDateTo(),
                            invoice.getDifferentDate(DifferenceDate.TypeDate.INVOICE), priceList.getPoze1() * countPhaze * phaze,
                            SummaryInvoiceModel.Unit.MONTH, SummaryInvoiceModel.Title.POZE));
                }
            }
        }

        invoiceListAdapter.setHideNt(hiddenNT);
        invoiceListAdapter.notifyDataSetChanged();

        for (SummaryInvoiceModel summaryInvoice : summaryInvoices) {
            boolean isMerged = false;
            for (SummaryInvoiceModel mergedInvoice : mergedSummaryInvoices) {
                if (summaryInvoice.getTitle() == mergedInvoice.getTitle() && summaryInvoice.getUnit() == mergedInvoice.getUnit() && summaryInvoice.getUnitPrice() == mergedInvoice.getUnitPrice()) {
                    mergedInvoice.addAmount(summaryInvoice.getAmount());
                    isMerged = true;
                    //break;
                }
            }
            if (!isMerged) {
                mergedSummaryInvoices.add(new SummaryInvoiceModel(summaryInvoice.getDateOf(), summaryInvoice.getDateTo(),
                        summaryInvoice.getAmount(), summaryInvoice.getUnitPrice(), summaryInvoice.getUnit(), summaryInvoice.getTitle()));
            }
        }

        InvoiceDetailAdapter invoiceDetailAdapter = new InvoiceDetailAdapter(requireActivity(), summaryInvoices);
        rv.setAdapter(invoiceDetailAdapter);
        rv.setLayoutManager(new LinearLayoutManager(getActivity()));
    }


    /**
     * Načte ceník podle id uložený ve faktuře
     *
     * @param invoice InvoiceModel - faktura
     * @return Objekt ceníku
     */
    private PriceListModel getPriceList(InvoiceModel invoice) {
        DataPriceListSource dataPriceListSource = new DataPriceListSource(getActivity());
        dataPriceListSource.open();
        PriceListModel priceList = dataPriceListSource.readPrice(invoice.getIdPriceList());
        dataPriceListSource.close();
        return priceList;
    }

}