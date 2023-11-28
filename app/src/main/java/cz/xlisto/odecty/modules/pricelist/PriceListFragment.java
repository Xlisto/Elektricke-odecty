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
import cz.xlisto.odecty.shp.ShPPriceList;
import cz.xlisto.odecty.utils.DetectScreenMode;
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
    private static final String ARG_ID_FRAGMENT = "idFragment";
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
    private int idFragment = 0; //id fragmentu pro zobrazení detailu ceníku v land režimu
    private ShPPriceList shPPriceList;


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

        if (savedInstanceState != null) {
            showSelectItem = savedInstanceState.getBoolean(SHOW_SELECT_ITEM);
            idSelectedPriceList = savedInstanceState.getLong(ID_SELECTED_PRICE_LIST);
            idFragment = savedInstanceState.getInt(ARG_ID_FRAGMENT);
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
        shPPriceList = new ShPPriceList(getContext());

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
                if (selectedPrice != null)
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

        //listener při potvrzení smazání ceníku
        requireActivity().getSupportFragmentManager().setFragmentResultListener(PriceListAdapter.FLAG_DIALOG_FRAGMENT_DELETE_PRICELIST, this,
                ((requestKey, result) -> {
                    if (result.getBoolean(YesNoDialogFragment.RESULT)) {
                        priceListAdapter.deleteItemPrice();
                        onLoadData();
                        priceListAdapter.setHideButtons();
                        idSelectedPriceList = -1; //nastavení n a skrytí fragmentu detailu ceníku
                        showDetailPriceFragment(false);
                    }
                }));

        //listener při potvrzení smazání nevyužitých ceníků
        requireActivity().getSupportFragmentManager().setFragmentResultListener(YesNoDialogFragment.FLAG_RESULT_DIALOG_FRAGMENT, this,
                ((requestKey, result) -> {
                    if (result.getBoolean(YesNoDialogFragment.RESULT)) {
                        DataPriceListSource dataPriceListSource = new DataPriceListSource(getActivity());
                        dataPriceListSource.open();
                        dataPriceListSource.deleteUnusedPriceList();
                        dataPriceListSource.close();
                        onLoadData();
                        setAdapter();
                        priceListAdapter.setHideButtons();
                        idSelectedPriceList = -1; //nastavení n a skrytí fragmentu detailu ceníku
                        showDetailPriceFragment(false);
                    }
                }));


        onLoadData();
        setAdapter();
        setIdSelectedPriceListFromShp();
        showDetailPriceFragment(true);
    }


    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putLong(ID_SELECTED_PRICE_LIST, idSelectedPriceList);
        outState.putBoolean(SHOW_SELECT_ITEM, showSelectItem);
        outState.putInt(ARG_ID_FRAGMENT, idFragment);
    }


    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        requireActivity().getMenuInflater().inflate(R.menu.menu_price_list, menu);
    }


    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        final int id = item.getItemId();
        if (id == R.id.menu_filter_pricelist) {
            PriceListFilterDialogFragment.newInstance(() -> {
                onLoadData();
                setAdapter();
                priceListAdapter.setHideButtons();
                idSelectedPriceList = -1; //nastavení n a skrytí fragmentu detailu ceníku
                showDetailPriceFragment(false);
            }).show(requireActivity().getSupportFragmentManager(), TAG);
        }

        if (id == R.id.menu_delete_unused_pricelist) {
            YesNoDialogFragment yesNoDialogFragment = YesNoDialogFragment.newInstance(getResources().getString(R.string.delete_unused_pricelist),
                    YesNoDialogFragment.FLAG_RESULT_DIALOG_FRAGMENT, getResources().getString(R.string.delete_unused_pricelist_content));
            yesNoDialogFragment.show(requireActivity().getSupportFragmentManager(), YesNoDialogFragment.TAG);
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
                    } else {
                        idSelectedPriceList = -1;
                    }

                    if (priceList != null) {
                        idSelectedPriceList = priceList.getId();

                    } else {
                        idSelectedPriceList = -1;
                    }
                    showDetailPriceFragment(false);
                }, rv);
        rv.setAdapter(priceListAdapter);
        rv.setLayoutManager(new LinearLayoutManager(getContext()));
    }


    private void showDetailPriceFragment(boolean showAfterRotation) {
        PriceListDetailFragment fragment;
        if (showAfterRotation) {
            fragment = (PriceListDetailFragment) requireActivity().getSupportFragmentManager().findFragmentById(idFragment);
            if (fragment != null) {
                requireActivity().getSupportFragmentManager().beginTransaction().remove(fragment).commit();
            }
            idFragment = 0;
        }
        if (DetectScreenMode.isLandscape(requireActivity())) {
            if (idSelectedPriceList != -1) {
                if (idFragment == 0) {
                    fragment = PriceListDetailFragment.newInstance(idSelectedPriceList, true);
                    requireActivity().getSupportFragmentManager().beginTransaction()
                            .replace(R.id.fl_price_list_right, fragment)
                            .commit();
                    idFragment = fragment.getId();
                } else {
                    fragment = (PriceListDetailFragment) requireActivity().getSupportFragmentManager().findFragmentById(idFragment);
                    assert fragment != null;
                    fragment.loadPrice(idSelectedPriceList);
                }


            } else {
                if (idFragment != 0) {
                    fragment = (PriceListDetailFragment) requireActivity().getSupportFragmentManager().findFragmentById(idFragment);
                    if (fragment != null) {
                        requireActivity().getSupportFragmentManager().beginTransaction().remove(fragment).commit();
                        idFragment = 0;
                    }
                }
            }
        }
    }


    /**
     * Načte ID vybraného ceníku z Shared Preferences
     */
    private void setIdSelectedPriceListFromShp() {
        idSelectedPriceList = shPPriceList.get(ShPPriceList.SHOW_ID_ITEM_PRICE_LIST, -1L);
    }


    public void setOnSelectedPriceListListener(OnSelectedPriceList onSelectedPriceListListener) {
        this.onSelectedPriceListListener = onSelectedPriceListListener;
    }


    public interface OnSelectedPriceList {
        void getOnSelectedItemPriceList(PriceListModel priceList);
    }
}