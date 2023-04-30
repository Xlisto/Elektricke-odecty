package cz.xlisto.odecty.modules.pricelist;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import cz.xlisto.odecty.R;
import cz.xlisto.odecty.models.SubscriptionPointModel;
import cz.xlisto.odecty.shp.ShPAddEditInvoice;
import cz.xlisto.odecty.shp.ShPFilter;
import cz.xlisto.odecty.databaze.DataPriceListSource;
import cz.xlisto.odecty.models.PriceListModel;
import cz.xlisto.odecty.ownview.ViewHelper;
import cz.xlisto.odecty.utils.FragmentChange;
import cz.xlisto.odecty.utils.SubscriptionPoint;

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

import static cz.xlisto.odecty.utils.FragmentChange.Transaction.MOVE;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link PriceListFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class PriceListFragment extends Fragment {
    private final String TAG = getClass().getName() + " ";
    private PriceListAdapter priceListAdapter;
    private PriceListModel selectedPrice;
    private ShPFilter shpFilter;

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String SHOW_SELECT_ITEM = "param1";
    private static final String ID_SELECTED_PRICE_LIST = "param2";

    // TODO: Rename and change types of parameters
    private boolean showSelectItem; //true = zobrazí radiobutton pro výběr ceníku
    private long idSelectedPriceList;
    private View view;
    private RecyclerView rv;
    private FloatingActionButton fab;
    private Fragment priceListAddFragment;
    private TextView tvCountPriceItem;
    private Button btnBack;

    private OnSelectedPriceList onSelectedPriceListListener;
    private SubscriptionPointModel subscriptionPoint;

    public PriceListFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param selectedItem Volba zobrazení výběrového radiobuttonu
     * @return A new instance of fragment PriceListFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static PriceListFragment newInstance(boolean selectedItem) {
        return newInstance(selectedItem, -1L);
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param selectedItem        Volba zobrazení výběrového radiobuttonu
     * @param idSelectedPriceList ID vybraného ceníku, -1 nic nevybráno.
     * @return A new instance of fragment PriceListFragment.
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

        // Inflate the layout for this fragment
        view = inflater.inflate(R.layout.fragment_price_list, container, false);

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        fab = view.findViewById(R.id.fab);
        rv = view.findViewById(R.id.rv_price_list);
        tvCountPriceItem = view.findViewById(R.id.tvPocetMist);
        btnBack = view.findViewById(R.id.btnBack);

        fab.setOnClickListener(v -> {
            priceListAddFragment = PriceListAddFragment.newInstance();
            FragmentChange.replace(getActivity(), priceListAddFragment, MOVE, true);
        });

        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ShPAddEditInvoice shPAddEditInvoice = new ShPAddEditInvoice(getContext());
                shPAddEditInvoice.set(ShPAddEditInvoice.SELECTED_ID_PRICE, idSelectedPriceList);
                if (onSelectedPriceListListener != null) {
                    onSelectedPriceListListener.getOnSelectedItemPriceList(selectedPrice);
                }
                getParentFragmentManager().popBackStack();
            }
        });

        if (showSelectItem) {
            fab.setVisibility(View.GONE);
            btnBack.setVisibility(View.VISIBLE);
        } else {
            fab.setVisibility(View.VISIBLE);
            btnBack.setVisibility(View.GONE);
        }
        onLoadData();
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        getActivity().getMenuInflater().inflate(R.menu.menu_price_list, menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        final int id = item.getItemId();
        if (id == R.id.menu_bottom) {
            PriceListFilterDialogFragment.newInstance(new PriceListFilterDialogFragment.CloseDialogWithPositiveButtonListener() {
                @Override
                public void onCloseDialogWithPositiveButton() {
                    onLoadData();
                }
            }).show(getActivity().getSupportFragmentManager(), TAG);
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
        ArrayList<PriceListModel> priceListModels = dataPriceListSource.readPriceList(rada, produkt, sazba, dodavatel, uzemi, datum);
        int totalCountPriceList = dataPriceListSource.countPriceItems();
        dataPriceListSource.close();

        rv.setAdapter(new PriceListAdapter(getActivity(), priceListModels, subscriptionPoint, showSelectItem, idSelectedPriceList,
                new PriceListAdapter.OnClickItemListener() {

                    @Override
                    public void setClickPriceListListener(PriceListModel priceList) {
                        if (onSelectedPriceListListener != null) {
                            selectedPrice = priceList;
                            //onSelectedPriceListListener.getOnSelectedItemPriceList(priceList);
                            btnBack.setText(getResources().getString(R.string.vybrat));
                            idSelectedPriceList = priceList.getId();
                        }
                    }
                },
                new PriceListAdapter.OnLongClickItemListener() {
                    @Override
                    public void setLongClickItemListener(long id) {
                    }
                },rv));
        rv.setLayoutManager(new LinearLayoutManager(getContext()));

        tvCountPriceItem.setText(getResources().getString(R.string.pocet_ceniku) + " " + totalCountPriceList + "/" + priceListModels.size());

    }



    public void setOnSelectedPriceListListener(OnSelectedPriceList onSelectedPriceListListener) {
        this.onSelectedPriceListListener = onSelectedPriceListListener;
    }

    public interface OnSelectedPriceList {
        void getOnSelectedItemPriceList(PriceListModel priceList);
    }
}