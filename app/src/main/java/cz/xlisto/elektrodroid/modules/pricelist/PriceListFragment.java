package cz.xlisto.elektrodroid.modules.pricelist;


import static cz.xlisto.elektrodroid.modules.pricelist.PriceListDetailFragment.PRICE_LIST_DETAIL_FRAGMENT;
import static cz.xlisto.elektrodroid.utils.FragmentChange.Transaction.MOVE;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.MenuHost;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;

import cz.xlisto.elektrodroid.R;
import cz.xlisto.elektrodroid.databaze.DataPriceListSource;
import cz.xlisto.elektrodroid.dialogs.SettingsViewDialogFragment;
import cz.xlisto.elektrodroid.dialogs.YesNoDialogFragment;
import cz.xlisto.elektrodroid.models.PriceListModel;
import cz.xlisto.elektrodroid.models.SubscriptionPointModel;
import cz.xlisto.elektrodroid.ownview.ViewHelper;
import cz.xlisto.elektrodroid.shp.ShPAddEditInvoice;
import cz.xlisto.elektrodroid.shp.ShPFilter;
import cz.xlisto.elektrodroid.utils.DetectScreenMode;
import cz.xlisto.elektrodroid.utils.FragmentChange;
import cz.xlisto.elektrodroid.utils.SubscriptionPoint;
import cz.xlisto.elektrodroid.utils.UIHelper;


/**
 * A simple {@link Fragment} subclass.
 * Use the {@link PriceListFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class PriceListFragment extends Fragment {

    private static final String TAG = "PriceListFragment";
    public static final String FLAG_RESULT_PRICE_LIST_FRAGMENT = "flagResultPriceListFragment";
    public static final String FLAG_PRICE_LIST_FRAGMENT = "flagPriceListFragment";
    public static final String FLAG_SIDE = "flagSide";
    private static final String BTN_BACK_TEXT = "btnBackText";
    private static PriceListModel selectedPrice;
    private ShPFilter shpFilter;
    private static final String SHOW_SELECT_ITEM = "param1";
    private static final String ID_SELECTED_PRICE_LIST = "param2";
    private static final String ARG_ID_FRAGMENT = "idFragment";
    private boolean showSelectItem; //true = zobrazí radiobutton pro výběr ceníku
    private static long idSelectedPriceList;
    private RecyclerView rv;
    private TextView tvCountPriceItem;
    private Button btnBack, btnAddPriceList;
    private FloatingActionButton fab;
    private SubscriptionPointModel subscriptionPoint;
    private PriceListAdapter priceListAdapter;
    private ArrayList<PriceListModel> priceListModels;
    private int idFragment = 0; //id fragmentu pro zobrazení detailu ceníku v land režimu
    private Side side;


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
        return newInstance(selectedItem, idSelectedPriceList, null);
    }


    /**
     * Fragment pro zobrazení ceníků s možností výběru ceníku
     *
     * @param selectedItem        Volba zobrazení výběrového radiobuttonu
     * @param idSelectedPriceList ID vybraného ceníku, -1 nic nevybráno.
     * @param side                Strana, pro kterou se vybírá ceník pro porovnání ceníků
     * @return Nová instance fragmentu PriceListFragment
     */
    public static PriceListFragment newInstance(boolean selectedItem, long idSelectedPriceList, Side side) {
        PriceListFragment fragment = new PriceListFragment();
        Bundle args = new Bundle();
        args.putBoolean(SHOW_SELECT_ITEM, selectedItem);
        args.putLong(ID_SELECTED_PRICE_LIST, idSelectedPriceList);
        args.putSerializable(FLAG_SIDE, side);
        fragment.setArguments(args);
        return fragment;
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        shpFilter = new ShPFilter(getActivity());
        if (getArguments() != null) {
            showSelectItem = getArguments().getBoolean(SHOW_SELECT_ITEM);
            idSelectedPriceList = getArguments().getLong(ID_SELECTED_PRICE_LIST);
            side = (Side) getArguments().getSerializable(FLAG_SIDE);
        }
        if (savedInstanceState != null) {
            showSelectItem = savedInstanceState.getBoolean(SHOW_SELECT_ITEM);
            idSelectedPriceList = savedInstanceState.getLong(ID_SELECTED_PRICE_LIST);
            idFragment = savedInstanceState.getInt(ARG_ID_FRAGMENT);
            side = (Side) savedInstanceState.getSerializable(FLAG_SIDE);
        }
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        MenuHost menuHost = requireActivity();
        menuHost.addMenuProvider(new MenuProvider() {
            @Override
            public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
                menuInflater.inflate(R.menu.menu_price_list, menu);
            }


            @Override
            public boolean onMenuItemSelected(@NonNull MenuItem menuItem) {
                if (menuItem.getItemId() == R.id.menu_filter_pricelist) {
                    PriceListFilterDialogFragment.newInstance().show(requireActivity().getSupportFragmentManager(), TAG);
                    return true;
                }
                if (menuItem.getItemId() == R.id.menu_delete_unused_pricelist) {
                    YesNoDialogFragment yesNoDialogFragment = YesNoDialogFragment.newInstance(getResources().getString(R.string.delete_unused_pricelist),
                            YesNoDialogFragment.FLAG_RESULT_DIALOG_FRAGMENT, getResources().getString(R.string.delete_unused_pricelist_content));
                    yesNoDialogFragment.show(requireActivity().getSupportFragmentManager(), YesNoDialogFragment.TAG);
                    return true;
                }
                return false;
            }
        }, getViewLifecycleOwner(), Lifecycle.State.RESUMED);

        return inflater.inflate(R.layout.fragment_price_list, container, false);
    }


    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        requireActivity().invalidateOptionsMenu();
        fab = view.findViewById(R.id.fab);
        rv = view.findViewById(R.id.rv_price_list);
        tvCountPriceItem = view.findViewById(R.id.tvPocetMist);
        btnBack = view.findViewById(R.id.btnBack);
        btnAddPriceList = view.findViewById(R.id.btnAddPriceList);
        fab.setOnClickListener(v -> addNewPriceList());
        btnAddPriceList.setOnClickListener(v -> addNewPriceList());
        btnBack.setOnClickListener(v -> {
            ShPAddEditInvoice shPAddEditInvoice = new ShPAddEditInvoice(getContext());
            shPAddEditInvoice.set(ShPAddEditInvoice.SELECTED_ID_PRICE, idSelectedPriceList);
            Bundle bundle = new Bundle();
            bundle.putSerializable(PriceListFragment.FLAG_RESULT_PRICE_LIST_FRAGMENT, selectedPrice);
            bundle.putSerializable(PriceListFragment.FLAG_SIDE, side);
            getParentFragmentManager().setFragmentResult(FLAG_PRICE_LIST_FRAGMENT, bundle);
            getParentFragmentManager().popBackStack();
        });

        //listener při potvrzení filtru ceníku
        requireActivity().getSupportFragmentManager().setFragmentResultListener(PriceListFilterDialogFragment.FLAG_RESULT_FILTER_DIALOG_FRAGMENT, this,
                ((requestKey, result) -> {
                    if (result.getBoolean(YesNoDialogFragment.RESULT)) {
                        onLoadData();
                        setAdapter();
                        priceListAdapter.setHideButtons();
                        showDetailPriceFragment(false);
                    }
                }));

        //listener při potvrzení smazání ceníku
        requireActivity().getSupportFragmentManager().setFragmentResultListener(PriceListAdapter.FLAG_DIALOG_FRAGMENT_DELETE_PRICE_LIST, this,
                ((requestKey, result) -> {
                    if (result.getBoolean(YesNoDialogFragment.RESULT)) {
                        priceListAdapter.deleteItemPrice();
                        onLoadData();
                        priceListAdapter.setHideButtons();
                        idSelectedPriceList = -1; //nastavení na skrytí fragmentu detailu ceníku
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
                        idSelectedPriceList = -1; //nastavení na skrytí fragmentu detailu ceníku
                        showDetailPriceFragment(false);
                    }
                }));

        //listener při zavření dialogového okna nastavení
        requireActivity().getSupportFragmentManager().setFragmentResultListener(SettingsViewDialogFragment.FLAG_UPDATE_SETTINGS_FOR_FRAGMENT, this,
                ((requestKey, result) -> {
                    UIHelper.showButtons(btnAddPriceList, fab, requireActivity(), rv, false);
                    btnShowBack();
                })
        );

        onLoadData();
        setAdapter();
        showDetailPriceFragment(true);
        if (savedInstanceState != null) {
            btnBack.setText(savedInstanceState.getString(BTN_BACK_TEXT));
        }
    }


    @Override
    public void onResume() {
        super.onResume();
        UIHelper.showButtons(btnAddPriceList, fab, requireActivity(), rv, false);
        btnShowBack();
    }


    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putLong(ID_SELECTED_PRICE_LIST, idSelectedPriceList);
        outState.putBoolean(SHOW_SELECT_ITEM, showSelectItem);
        outState.putInt(ARG_ID_FRAGMENT, idFragment);
        outState.putSerializable(FLAG_SIDE, side);
        if (btnBack != null)
            outState.putString(BTN_BACK_TEXT, btnBack.getText().toString());
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
        String company = shpFilter.get(ShPFilter.COMPANY, ShPFilter.DEFAULT);
        String area = shpFilter.get(ShPFilter.AREA, ShPFilter.DEFAULT);
        String dateStart = "%";
        String dateEnd = "%";
        if (!shpFilter.get(ShPFilter.DATE_START, ShPFilter.DEFAULT).equals("%") && !shpFilter.get(ShPFilter.DATE_START, ShPFilter.DEFAULT).isEmpty())
            dateStart = Long.toString(ViewHelper.parseCalendarFromString(shpFilter.get(ShPFilter.DATE_START, ShPFilter.DEFAULT)).getTimeInMillis());
        if (!shpFilter.get(ShPFilter.DATE_END, ShPFilter.DEFAULT).equals("%") && !shpFilter.get(ShPFilter.DATE_END, ShPFilter.DEFAULT).isEmpty())
            dateEnd = Long.toString(ViewHelper.parseCalendarFromString(shpFilter.get(ShPFilter.DATE_END, ShPFilter.DEFAULT)).getTimeInMillis());
        DataPriceListSource dataPriceListSource = new DataPriceListSource(requireContext());
        dataPriceListSource.open();
        priceListModels = dataPriceListSource.readPriceList(rada, produkt, sazba, company, area, dateStart, dateEnd);
        int totalCountPriceList = dataPriceListSource.countPriceItems();
        dataPriceListSource.close();
        tvCountPriceItem.setText(getResources().getString(R.string.pocet_ceniku, totalCountPriceList, priceListModels.size()));
    }


    /**
     * Nastaví adapter pro zobrazení ceníků
     */
    private void setAdapter() {
        priceListAdapter = new PriceListAdapter(priceListModels, subscriptionPoint, showSelectItem, idSelectedPriceList,
                priceList -> {
                    selectedPrice = priceList;
                    idSelectedPriceList = -1L;
                    if (priceList != null) {
                        btnBack.setText(getResources().getString(R.string.vybrat));
                        idSelectedPriceList = priceList.getId();
                        Bundle bundle = new Bundle();
                        bundle.putLong(PriceListDetailFragment.PRICE_LIST_ID, idSelectedPriceList);
                        getParentFragmentManager().setFragmentResult(PRICE_LIST_DETAIL_FRAGMENT, bundle);
                        showDetailPriceFragment(false);
                    }
                }, rv);
        rv.setAdapter(priceListAdapter);
        rv.setLayoutManager(new LinearLayoutManager(getContext()));
    }


    /**
     * Zobrazí fragment detailu ceníku
     *
     * @param showAfterRotation true = zobrazí fragment po rotaci
     */
    private void showDetailPriceFragment(boolean showAfterRotation) {
        PriceListDetailFragment fragment;
        if (showAfterRotation && idFragment != 0) {
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
                    if (fragment != null) {
                        fragment.loadPrice(idSelectedPriceList);
                    }
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
     * Zobrazí tlačítka pro přidání nového ceníku podle zvoleného režimu zobrazení
     */
    private void btnShowBack() {
        if (showSelectItem) {
            //zobrazení tlačítek ve výběru ceníku
            btnBack.setVisibility(View.VISIBLE);
            fab.setVisibility(View.GONE);
        } else {
            //zobrazení tlačítek v seznamu ceníků
            btnBack.setVisibility(View.GONE);
        }
    }


    /**
     * Zobrazí fragment pro přidání nového ceníku
     */
    private void addNewPriceList() {
        Fragment priceListAddFragment = PriceListAddFragment.newInstance();
        FragmentChange.replace(requireActivity(), priceListAddFragment, MOVE, true);
    }


    /**
     * Strana, pro kterou se vybírá ceník pro porovnání ceníků
     */
    enum Side {
        LEFT, RIGHT
    }

}