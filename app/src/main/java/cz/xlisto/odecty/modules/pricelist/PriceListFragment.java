package cz.xlisto.odecty.modules.pricelist;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import cz.xlisto.odecty.R;
import cz.xlisto.odecty.databaze.DataPriceListSource;
import cz.xlisto.odecty.dialogs.YesNoDialogFragment;
import cz.xlisto.odecty.models.PriceListModel;
import cz.xlisto.odecty.models.SubscriptionPointModel;
import cz.xlisto.odecty.ownview.ViewHelper;
import cz.xlisto.odecty.shp.ShPAddEditInvoice;
import cz.xlisto.odecty.shp.ShPFilter;
import cz.xlisto.odecty.utils.FragmentChange;
import cz.xlisto.odecty.utils.SubscriptionPoint;

import static cz.xlisto.odecty.utils.FragmentChange.Transaction.MOVE;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link PriceListFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class PriceListFragment extends Fragment {
    private static final String TAG = "PriceListFragment";
    private PriceListModel selectedPrice;
    private ShPFilter shpFilter;
    private static final String SHOW_SELECT_ITEM = "param1";
    private static final String ID_SELECTED_PRICE_LIST = "param2";
    private boolean showSelectItem; //true = zobrazí radiobutton pro výběr ceníku
    private long idSelectedPriceList;
    private RecyclerView rv;
    private Fragment priceListAddFragment;
    private TextView tvCountPriceItem;
    private Button btnBack;
    private OnSelectedPriceList onSelectedPriceListListener;
    private SubscriptionPointModel subscriptionPoint;
    private PriceListAdapter priceListAdapter;
    private ArrayList<PriceListModel> priceListModels;


    public PriceListFragment() {
        // Required empty public constructor
    }


    /**
     * Fragment pro zobrazení ceníků
     *
     * @param selectedItem Volba zobrazení výběrového radiobuttonu.
     *                     true = zobrazí radiobutton pro výběr ceníku
     *                     false = skryje radiobutton pro výběr ceníku
     * @return Nová instance fragmentu PriceListFragment.
     */
    public static PriceListFragment newInstance(boolean selectedItem) {
        return newInstance(selectedItem, -1L);
    }


    /**
     * Fragment pro zobrazení ceníků s možností výběru ceníku
     *
     * @param selectedItem        Volba zobrazení výběrového radiobuttonu
     * @param idSelectedPriceList ID vybraného ceníku, -1 nic nevybráno.
     * @return Nová instance fragmentu PriceListFragment
     */
    public static PriceListFragment newInstance(boolean selectedItem, long idSelectedPriceList) {
        PriceListFragment fragment = new PriceListFragment();
        Bundle args = new Bundle();
        args.putBoolean(SHOW_SELECT_ITEM, selectedItem);
        args.putLong(ID_SELECTED_PRICE_LIST, idSelectedPriceList);
        fragment.setArguments(args);
        return fragment;
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        shpFilter = new ShPFilter(getActivity());

        if (getArguments() != null) {
            showSelectItem = getArguments().getBoolean(SHOW_SELECT_ITEM);
            idSelectedPriceList = getArguments().getLong(ID_SELECTED_PRICE_LIST);
        }
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_price_list, container, false);
    }


    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        FloatingActionButton fab = view.findViewById(R.id.fab);
        rv = view.findViewById(R.id.rv_price_list);
        tvCountPriceItem = view.findViewById(R.id.tvPocetMist);
        btnBack = view.findViewById(R.id.btnBack);

        fab.setOnClickListener(v -> {
            priceListAddFragment = PriceListAddFragment.newInstance();
            FragmentChange.replace(requireActivity(), priceListAddFragment, MOVE, true);
        });

        btnBack.setOnClickListener(v -> {
            ShPAddEditInvoice shPAddEditInvoice = new ShPAddEditInvoice(getContext());
            shPAddEditInvoice.set(ShPAddEditInvoice.SELECTED_ID_PRICE, idSelectedPriceList);
            if (onSelectedPriceListListener != null) {
                onSelectedPriceListListener.getOnSelectedItemPriceList(selectedPrice);
            }
            getParentFragmentManager().popBackStack();
        });

        if (showSelectItem) {
            fab.setVisibility(View.GONE);
            btnBack.setVisibility(View.VISIBLE);
        } else {
            fab.setVisibility(View.VISIBLE);
            btnBack.setVisibility(View.GONE);
        }

        requireActivity().getSupportFragmentManager().setFragmentResultListener(PriceListAdapter.FLAG_DIALOG_FRAGMENT, this,
                ((requestKey, result) -> {
                    if (result.getBoolean(YesNoDialogFragment.RESULT)) {
                        priceListAdapter.deleteItemPrice();
                        onLoadData();
                    }
                }));

        onLoadData();
        setAdapter();
    }


    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        requireActivity().getMenuInflater().inflate(R.menu.menu_price_list, menu);
    }


    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        final int id = item.getItemId();
        if (id == R.id.menu_bottom) {
            PriceListFilterDialogFragment.newInstance(() -> {
                onLoadData();
                setAdapter();
            }).show(requireActivity().getSupportFragmentManager(), TAG);
        }
        return super.onOptionsItemSelected(item);
    }


    /**
     * Načte seznam ceníků podle vybraných parametrů
     */
    public void onLoadData() {
        btnBack.setText(getResources().getString(R.string.zpet));
        subscriptionPoint = SubscriptionPoint.load(getActivity());
        String rada = shpFilter.get(ShPFilter.RADA, ShPFilter.DEFAULT);
        String produkt = shpFilter.get(ShPFilter.PRODUKT, ShPFilter.DEFAULT);
        String sazba = shpFilter.get(ShPFilter.SAZBA, ShPFilter.DEFAULT);
        String dodavatel = shpFilter.get(ShPFilter.DODAVATEL, ShPFilter.DEFAULT);
        String uzemi = shpFilter.get(ShPFilter.UZEMI, ShPFilter.DEFAULT);
        String datum = "%";
        if (!shpFilter.get(ShPFilter.DATUM, ShPFilter.DEFAULT).equals("%") && !shpFilter.get(ShPFilter.DATUM, ShPFilter.DEFAULT).equals(""))
            datum = Long.toString(ViewHelper.parseCalendarFromString(shpFilter.get(ShPFilter.DATUM, ShPFilter.DEFAULT)).getTimeInMillis());

        DataPriceListSource dataPriceListSource = new DataPriceListSource(getActivity());
        dataPriceListSource.open();
        priceListModels = dataPriceListSource.readPriceList(rada, produkt, sazba, dodavatel, uzemi, datum);
        int totalCountPriceList = dataPriceListSource.countPriceItems();
        dataPriceListSource.close();

        tvCountPriceItem.setText(getResources().getString(R.string.pocet_ceniku) + " " + totalCountPriceList + "/" + priceListModels.size());
    }


    /**
     * Nastaví adapter pro zobrazení ceníků
     */
    private void setAdapter() {
        priceListAdapter = new PriceListAdapter(getActivity(), priceListModels, subscriptionPoint, showSelectItem, idSelectedPriceList,
                priceList -> {
                    if (onSelectedPriceListListener != null) {
                        selectedPrice = priceList;
                        //onSelectedPriceListListener.getOnSelectedItemPriceList(priceList);
                        btnBack.setText(getResources().getString(R.string.vybrat));
                        idSelectedPriceList = priceList.getId();
                    }
                }, rv);
        rv.setAdapter(priceListAdapter);
        rv.setLayoutManager(new LinearLayoutManager(getContext()));
    }


    public void setOnSelectedPriceListListener(OnSelectedPriceList onSelectedPriceListListener) {
        this.onSelectedPriceListListener = onSelectedPriceListListener;
    }


    public interface OnSelectedPriceList {
        void getOnSelectedItemPriceList(PriceListModel priceList);
    }
}