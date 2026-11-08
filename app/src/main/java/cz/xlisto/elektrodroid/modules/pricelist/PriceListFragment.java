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
import androidx.core.os.BundleCompat;
import androidx.core.view.MenuHost;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.Calendar;

import cz.xlisto.elektrodroid.R;
import cz.xlisto.elektrodroid.databaze.DataPriceListSource;
import cz.xlisto.elektrodroid.dialogs.PriceListFilterDialogFragment;
import cz.xlisto.elektrodroid.modules.settings.SettingsFragment;
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
 * Fragment pro zobrazení, filtrování a výběr ceníků elektřiny.
 *
 * <p>Umožňuje procházet uložené ceníky, aplikovat filtry, mazat nevyužité ceníky
 * a vybírat konkrétní ceník pro faktury nebo srovnání v dvoupanelovém rozvržení.</p>
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
    private boolean showSelectItem;
    private static long idSelectedPriceList;
    private RecyclerView rv;
    private TextView tvCountPriceItem;
    private Button btnBack, btnAddPriceList;
    private FloatingActionButton fab;
    private SubscriptionPointModel subscriptionPoint;
    private PriceListAdapter priceListAdapter;
    private ArrayList<PriceListModel> priceListModels;
    private int idFragment = 0;
    private Side side;


    /**
     * Vytvoří novou instanci {@link PriceListFragment}.
     * Vyžadováno architekturou {@link Fragment}.
     */
    public PriceListFragment() {
        // Vyžadovaný prázdný veřejný konstruktor
    }

    /**
     * Vytvoří novou instanci fragmentu pro zobrazení nebo výběr ceníků.
     *
     * @param selectedItem        {@code true} pro zobrazení režimu výběru ceníku (s výběrovým tlačítkem)
     * @param idSelectedPriceList ID vybraného ceníku, nebo {@code -1L}, pokud není vybrán žádný
     * @return nová instance {@link PriceListFragment}
     */
    public static PriceListFragment newInstance(boolean selectedItem, long idSelectedPriceList) {
        return newInstance(selectedItem, idSelectedPriceList, null);
    }

    /**
     * Vytvoří novou instanci fragmentu pro výběr ceníku s určením strany při srovnání ceníků.
     *
     * @param selectedItem        {@code true} pro zobrazení režimu výběru ceníku
     * @param idSelectedPriceList ID vybraného ceníku, nebo {@code -1L}, pokud není vybrán žádný
     * @param side                strana ({@link Side}), pro kterou se ceník vybírá v režimu srovnání
     * @return nová instance {@link PriceListFragment}
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
            side = BundleCompat.getSerializable(getArguments(), FLAG_SIDE, Side.class);
        }
        if (savedInstanceState != null) {
            showSelectItem = savedInstanceState.getBoolean(SHOW_SELECT_ITEM);
            idSelectedPriceList = savedInstanceState.getLong(ID_SELECTED_PRICE_LIST);
            idFragment = savedInstanceState.getInt(ARG_ID_FRAGMENT);
            side = BundleCompat.getSerializable(savedInstanceState, FLAG_SIDE, Side.class);
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

        // Posluchač při potvrzení filtru ceníku
        requireActivity().getSupportFragmentManager().setFragmentResultListener(PriceListFilterDialogFragment.FLAG_RESULT_FILTER_DIALOG_FRAGMENT, this,
                ((requestKey, result) -> {
                    if (result.getBoolean(YesNoDialogFragment.RESULT)) {
                        onLoadData();
                        setAdapter();
                        priceListAdapter.setHideButtons();
                        showDetailPriceFragment(false);
                    }
                }));

        // Posluchač při potvrzení smazání ceníku
        requireActivity().getSupportFragmentManager().setFragmentResultListener(PriceListAdapter.FLAG_DIALOG_FRAGMENT_DELETE_PRICE_LIST, this,
                ((requestKey, result) -> {
                    if (result.getBoolean(YesNoDialogFragment.RESULT)) {
                        priceListAdapter.deleteItemPrice();
                        onLoadData();
                        priceListAdapter.setHideButtons();
                        idSelectedPriceList = -1; // Nastavení na skrytí fragmentu detailu ceníku
                        showDetailPriceFragment(false);
                    }
                }));

        // Posluchač při potvrzení smazání nevyužitých ceníků
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
                        idSelectedPriceList = -1; // Nastavení na skrytí fragmentu detailu ceníku
                        showDetailPriceFragment(false);
                    }
                }));

        // Posluchač při zavření dialogového okna nastavení
        requireActivity().getSupportFragmentManager().setFragmentResultListener(SettingsFragment.FLAG_UPDATE_SETTINGS_FOR_FRAGMENT, this,
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
     * Načte seznam ceníků z databáze podle nastavených filtrů a aktualizuje počet zobrazených položek.
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
        if (!shpFilter.get(ShPFilter.DATE_START, ShPFilter.DEFAULT).equals("%") && !shpFilter.get(ShPFilter.DATE_START, ShPFilter.DEFAULT).isEmpty()) {
            Calendar calStart = ViewHelper.parseCalendarFromString(shpFilter.get(ShPFilter.DATE_START, ShPFilter.DEFAULT));
            calStart.set(Calendar.HOUR_OF_DAY, 0);
            calStart.set(Calendar.MINUTE, 0);
            calStart.set(Calendar.SECOND, 0);
            calStart.set(Calendar.MILLISECOND, 0);
            dateStart = Long.toString(calStart.getTimeInMillis());
        }
        if (!shpFilter.get(ShPFilter.DATE_END, ShPFilter.DEFAULT).equals("%") && !shpFilter.get(ShPFilter.DATE_END, ShPFilter.DEFAULT).isEmpty()) {
            Calendar calEnd = ViewHelper.parseCalendarFromString(shpFilter.get(ShPFilter.DATE_END, ShPFilter.DEFAULT));
            calEnd.set(Calendar.HOUR_OF_DAY, 23);
            calEnd.set(Calendar.MINUTE, 59);
            calEnd.set(Calendar.SECOND, 59);
            calEnd.set(Calendar.MILLISECOND, 999);
            dateEnd = Long.toString(calEnd.getTimeInMillis());
        }
        DataPriceListSource dataPriceListSource = new DataPriceListSource(requireContext());
        dataPriceListSource.open();
        priceListModels = dataPriceListSource.readPriceList(rada, produkt, sazba, company, area, dateStart, dateEnd);
        int totalCountPriceList = dataPriceListSource.countPriceItems();
        dataPriceListSource.close();
        tvCountPriceItem.setText(getResources().getString(R.string.pocet_ceniku, totalCountPriceList, priceListModels.size()));
    }

    /**
     * Inicializuje a nastaví {@link PriceListAdapter} pro zobrazení seznamu ceníků v {@link RecyclerView}.
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
     * Zobrazí nebo aktualizuje fragment s detailem ceníku v režimu na šířku (landscape).
     *
     * @param showAfterRotation {@code true}, pokud se jedná o obnovení detailu po rotaci obrazovky
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
                    getChildFragmentManager().beginTransaction()
                            .replace(R.id.fl_price_list_right, fragment)
                            .commit();
                    idFragment = fragment.getId();
                } else {
                    fragment = (PriceListDetailFragment) getChildFragmentManager().findFragmentById(idFragment);
                    if (fragment != null) {
                        fragment.loadPrice(idSelectedPriceList);
                    }
                }
            } else {
                if (idFragment != 0) {
                    fragment = (PriceListDetailFragment) requireActivity().getSupportFragmentManager().findFragmentById(idFragment);
                    if (fragment != null) {
                        getChildFragmentManager().beginTransaction().remove(fragment).commit();
                        idFragment = 0;
                    }
                }
            }
        }
    }

    /**
     * Řídí viditelnost tlačítka Zpět a plovoucího tlačítka FAB podle režimu výběru ceníku.
     */
    private void btnShowBack() {
        if (showSelectItem) {
            // Zobrazení tlačítek ve výběru ceníku
            btnBack.setVisibility(View.VISIBLE);
            fab.setVisibility(View.GONE);
        } else {
            // Zobrazení tlačítek v seznamu ceníků
            btnBack.setVisibility(View.GONE);
        }
    }

    /**
     * Otevře fragment pro přidání nového ceníku.
     */
    private void addNewPriceList() {
        Fragment priceListAddFragment = PriceListAddFragment.newInstance();
        FragmentChange.replace(requireActivity(), priceListAddFragment, MOVE, true);
    }

    /**
     * Určení strany v režimu porovnávání ceníků (levá nebo pravá strana).
     */
    public enum Side {
        LEFT, RIGHT
    }

}