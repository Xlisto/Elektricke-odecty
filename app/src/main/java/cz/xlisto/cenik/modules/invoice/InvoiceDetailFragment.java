package cz.xlisto.cenik.modules.invoice;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Calendar;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import cz.xlisto.cenik.R;
import cz.xlisto.cenik.databaze.DataPriceListSource;
import cz.xlisto.cenik.databaze.DataSubscriptionPointSource;
import cz.xlisto.cenik.format.DecimalFormatHelper;
import cz.xlisto.cenik.models.InvoiceListModel;
import cz.xlisto.cenik.models.InvoiceModel;
import cz.xlisto.cenik.models.PozeModel;
import cz.xlisto.cenik.models.PriceListModel;
import cz.xlisto.cenik.models.PriceListRegulBuilder;
import cz.xlisto.cenik.models.SubscriptionPointModel;
import cz.xlisto.cenik.models.SummaryInvoiceModel;
import cz.xlisto.cenik.shp.ShPInvoiceDetail;
import cz.xlisto.cenik.utils.Calculation;
import cz.xlisto.cenik.utils.DifferenceDate;
import cz.xlisto.cenik.utils.SubscriptionPoint;

import static cz.xlisto.cenik.modules.invoice.InvoiceAbstract.D01;
import static cz.xlisto.cenik.modules.invoice.InvoiceAbstract.D02;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link InvoiceDetailFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class InvoiceDetailFragment extends Fragment {
    private static final String TAG = "InvoiceDetailFragment";

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ID_FAK = "id_fak";
    private static final String TABLE_FAK = "table_fak";
    private static final String TABLE_NOW = "table_now";
    private static final String TABLE_PAY = "table_pay";
    private static final String POSITION = "position";

    private static final String SHOW_TYPE_DETAIL_INDEX = "showTypeDetailIndex";
    private InvoiceDetailAdapter invoiceDetailAdapter;

    private String tableFAK,tablePAY, tableNOW,table;

    private long idFak;
    private int position;
    private RecyclerView rv;
    private Spinner spinner;
    private int showTypeDetailIndex;
    private TextView tvTotal;
    private Button btn;
    private ShPInvoiceDetail shPInvoiceDetail;
    private SubscriptionPointModel subscriptionPoint;
    private ArrayList<InvoiceModel> invoices;
    private ArrayList<InvoiceListModel> invoicesList;
    private ArrayList<SummaryInvoiceModel> summaryInvoices = new ArrayList<>();
    private ArrayList<SummaryInvoiceModel> mergedSummaryInvoices = new ArrayList<>();
    private MySpinnerInvoiceDetailAdapter invoiceListAdapter;

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    public InvoiceDetailFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param tableFak tabulak faktur
     * @param tablePay tabulka plateb
     * @return A new instance of fragment InvoiceDetailFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static InvoiceDetailFragment newInstance(String tableFak, String tableNow,String tablePay, long idFak, int position) {
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
            tablePAY = getArguments().getString(TABLE_PAY);
            idFak = getArguments().getLong(ID_FAK);
            position = getArguments().getInt(POSITION);
        }
        if (savedInstanceState != null) {
            idFak = savedInstanceState.getLong(ID_FAK);
            position = savedInstanceState.getInt(POSITION);
            showTypeDetailIndex = savedInstanceState.getInt(SHOW_TYPE_DETAIL_INDEX);
        }
        table = tableFAK;
        if(idFak == -1L) {
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
        btn = view.findViewById(R.id.btn);
        rv = view.findViewById(R.id.recycleViewInvoice);
        tvTotal = view.findViewById(R.id.tvTotal);
        spinner = view.findViewById(R.id.spInvoice);

        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                /*new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {

                    }
                }, 3000);*/
                summaryInvoices.remove(0);
                int count = invoiceDetailAdapter.getItemCount();
                invoiceDetailAdapter.notifyItemRemoved(0);
                invoiceDetailAdapter.notifyItemRangeRemoved(0, count - 1);
            }

        });
    }

    @Override
    public void onResume() {
        super.onResume();
        rv.setAdapter(null);
        shPInvoiceDetail = new ShPInvoiceDetail(getActivity());
        showTypeDetailIndex = shPInvoiceDetail.get(ShPInvoiceDetail.SHOW_TYPE_DETAIL_INDEX, 0);
        loadInvoice();
        setSpinner(false);
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

    private void setSpinner(boolean hideNt) {
        invoiceListAdapter = new MySpinnerInvoiceDetailAdapter(getActivity(), R.layout.item_own_simple_list, SummaryInvoiceModel.Title.values(), hideNt);
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
        DataSubscriptionPointSource dataSubscriptionPointSource = new DataSubscriptionPointSource(getActivity());
        dataSubscriptionPointSource.open();
        subscriptionPoint = SubscriptionPoint.load(getActivity());
        invoices = dataSubscriptionPointSource.loadInvoices(idFak, table);
        invoicesList = dataSubscriptionPointSource.loadInvoiceLists(subscriptionPoint);
        dataSubscriptionPointSource.close();

    }

    private void setRecyclerView() {
        invoiceDetailAdapter = new InvoiceDetailAdapter(summaryInvoices);
        invoiceDetailAdapter.setListener(new InvoiceDetailAdapter.Listener() {
            @Override
            public void getTotalPrice(double totalPrice) {
                tvTotal.setText("Celkem " + DecimalFormatHelper.df2.format(totalPrice) + " kč bez DPH");
            }
        });
        rv.setAdapter(invoiceDetailAdapter);


        rv.setLayoutManager(new LinearLayoutManager(getActivity()));
        rv.getViewTreeObserver().removeOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                rv.getViewTreeObserver().removeOnPreDrawListener(this);

                final WindowManager wm = (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);
                int height = wm.getDefaultDisplay().getHeight();
                rv.getMeasuredHeight();

                for (int i = 0; i < rv.getChildCount(); i++) {

                    View v = rv.getChildAt(i);
                    //Animation animation = AnimationUtils.loadAnimation(getContext(), R.anim.item_animation_fall_down);
                    //v.startAnimation(animation);
                    v.setAlpha(1.0f);
                    //v.setTranslationY(((-1*v.getHeight()*20/100)*(i+1)));
                    //v.setScaleY(1.05f);
                    //v.setScaleX(1.05f);
                    //v.setPivotX(0.5f);
                    //v.setPivotY(0.5f);
                    v.animate().alpha(0.0f).translationY(0)
                            //.scaleX(1).scaleY(1)
                            .setDuration(500)
                            .setStartDelay(i * 15 * 500 / 100)
                            .start();
                }
                return true;
            }
        });
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

    private void calculationDetailsInvoice(ArrayList<InvoiceModel> invoices) {
        boolean hiddenNT = false;
        summaryInvoices.clear();
        SubscriptionPointModel subscriptionPoint = SubscriptionPoint.load(getActivity());
        int phaze = subscriptionPoint.getPhaze();
        int countPhaze = subscriptionPoint.getCountPhaze();
        PozeModel poze = Calculation.getPoze(invoices, countPhaze, phaze, getActivity());


        for (int i = 0; i < invoices.size(); i++) {
            InvoiceModel invoice = invoices.get(i);
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(invoice.getDateFrom());
            PriceListRegulBuilder priceListRegulBuilder = new PriceListRegulBuilder(getPriceList(invoice),
                    calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
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

            String s = "" + spinner.getSelectedItem().toString();
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
            } else if (s.equals(SummaryInvoiceModel.Title.CIRCUT_BREAKER.toString())) {//jističe
                summaryInvoices.add(new SummaryInvoiceModel(invoice.getDateFrom(), invoice.getDateTo(),
                        invoice.getDifferentDate(DifferenceDate.TypeDate.INVOICE), priceBreaker,
                        SummaryInvoiceModel.Unit.MONTH, SummaryInvoiceModel.Title.TAX));
            } else if (s.equals(SummaryInvoiceModel.Title.SYS_SERVICES.toString())) {
                summaryInvoices.add(new SummaryInvoiceModel(invoice.getDateFrom(), invoice.getDateTo(),
                        invoice.getVtNt() / 1000, priceList.getSystemSluzby(),
                        SummaryInvoiceModel.Unit.MWH, SummaryInvoiceModel.Title.SYS_SERVICES));
            } else if (s.equals(SummaryInvoiceModel.Title.OTE.toString())) {
                summaryInvoices.add(new SummaryInvoiceModel(invoice.getDateFrom(), invoice.getDateTo(),
                        invoice.getDifferentDate(DifferenceDate.TypeDate.INVOICE), priceList.getCinnost(),
                        SummaryInvoiceModel.Unit.MONTH, SummaryInvoiceModel.Title.OTE));
            } else if (s.equals(SummaryInvoiceModel.Title.POZE.toString())) {
                if (poze.getTypePoze() == PozeModel.TypePoze.POZE2) {//podle spotřeby
                    summaryInvoices.add(new SummaryInvoiceModel(invoice.getDateFrom(), invoice.getDateTo(),
                            invoice.getVtNt() / 1000, priceList.getPoze2(),
                            SummaryInvoiceModel.Unit.MWH, SummaryInvoiceModel.Title.POZE));
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


        /*Collections.sort(summaryInvoices,(a, b) -> {
            return a.getTitle().compareTo(b.getTitle());
        });*/


        for (int i = 0; i < summaryInvoices.size(); i++) {
            SummaryInvoiceModel sm = summaryInvoices.get(i);
            Log.w(TAG, "Fak " + i + " " + sm.getTitle() + " " + sm.getAmount() + " " + sm.getUnit() + " " + sm.getUnitPrice() + " " + sm.getTotalPrice());
        }
        Log.w(TAG, "Fak ");

        for (int i = 0; i < mergedSummaryInvoices.size(); i++) {
            SummaryInvoiceModel sm = mergedSummaryInvoices.get(i);
            Log.w(TAG, "Fak merged: " + i + " " + sm.getTitle() + " " + sm.getAmount() + " " + sm.getUnit() + " " + sm.getUnitPrice() + " " + sm.getTotalPrice());
        }

        InvoiceDetailAdapter invoiceDetailAdapter = new InvoiceDetailAdapter(summaryInvoices);
        rv.setAdapter(invoiceDetailAdapter);
        rv.setLayoutManager(new LinearLayoutManager(getActivity()));


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
}