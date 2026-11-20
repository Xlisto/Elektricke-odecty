package cz.xlisto.elektrodroid.modules.pricelist;

import static cz.xlisto.elektrodroid.utils.FragmentChange.Transaction.MOVE;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.os.BundleCompat;
import androidx.core.view.MenuHost;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.ViewModelProvider;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import cz.xlisto.elektrodroid.MainActivity;
import cz.xlisto.elektrodroid.R;
import cz.xlisto.elektrodroid.databaze.DataPriceListSource;
import cz.xlisto.elektrodroid.databaze.DataSettingsSource;
import cz.xlisto.elektrodroid.dialogs.PriceListComparisonParametersDialogFragment;
import cz.xlisto.elektrodroid.models.PriceListModel;
import cz.xlisto.elektrodroid.models.SubscriptionPointModel;
import cz.xlisto.elektrodroid.modules.pricelist.models.PriceListComparisonScenario;
import cz.xlisto.elektrodroid.utils.FragmentChange;
import cz.xlisto.elektrodroid.utils.SubscriptionPoint;

/**
 * Hlavní kontejnerový fragment pro porovnávání ceníků elektřiny.
 * Obsahuje záložkové navigace (ViewPager2 + TabLayout) se 4 záložkami: Přehled, Grafy, Výhodnost sazeb a Tabulka,
 * spravuje sdílený {@link PriceListsViewModel} a načítání/ukládání srovnávacích parametrů spotřeby.
 */
public class PriceListCompareBoxFragment extends Fragment {

    private static final String TAG = "PriceListCompareBoxFragment";
    private PriceListsViewModel priceListsViewModel;

    private MenuItem menuSelectPriceLists;

    private final String[] tabTitles = new String[]{"Přehled", "Grafy", "Výhodnost sazeb", "Tabulka"};


    /**
     * Vytvoří novou výchozí instanci {@link PriceListCompareBoxFragment}.
     *
     * @return nová instance {@link PriceListCompareBoxFragment}
     */
    public static PriceListCompareBoxFragment newInstance() {
        return new PriceListCompareBoxFragment();
    }


    /**
     * Vytvoří novou instanci {@link PriceListCompareBoxFragment} s předanými ID vybraných ceníků pro porovnání.
     *
     * @param selectedIds pole ID ceníků k porovnání
     * @return nová instance {@link PriceListCompareBoxFragment}
     */
    public static PriceListCompareBoxFragment newInstance(long[] selectedIds) {
        PriceListCompareBoxFragment fragment = new PriceListCompareBoxFragment();
        if (selectedIds != null && selectedIds.length > 0) {
            Bundle args = new Bundle();
            args.putLongArray(PriceListFragment.FLAG_SELECTED_IDS, selectedIds);
            fragment.setArguments(args);
        }
        return fragment;
    }


    /**
     * Inicializuje ViewModel {@link PriceListsViewModel}, načte uložené srovnávací parametry spotřeby pro aktuální odběrné místo
     * a v případě předaných ID ceníků je načte z databáze a vloží do ViewModelu.
     *
     * @param savedInstanceState uložený stav fragmentu
     */
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        priceListsViewModel = new ViewModelProvider(this).get(PriceListsViewModel.class);
        loadCompareParameters();

        if (getArguments() != null) {
            long[] initialSelectedIds = getArguments().getLongArray(PriceListFragment.FLAG_SELECTED_IDS);
            if (initialSelectedIds != null && initialSelectedIds.length > 0) {
                DataPriceListSource source = new DataPriceListSource(requireContext());
                source.open();
                List<PriceListModel> selectedList = new ArrayList<>();
                for (long id : initialSelectedIds) {
                    PriceListModel pl = source.readPrice(id);
                    if (pl != null && pl.getId() > 0) {
                        selectedList.add(pl);
                    }
                }
                source.close();
                priceListsViewModel.setSelectedPriceLists(selectedList);
            }
        }
    }


    /**
     * Vytvoří a vrátí rozvržení pohledu kontejneru pro porovnávání ceníků.
     *
     * @param inflater           objekt pro nafouknutí rozvržení
     * @param container          kontejner rodičovského pohledu
     * @param savedInstanceState uložený stav fragmentu
     * @return nafouknutý pohled {@link View}
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_price_list_compare_box, container, false);
    }


    /**
     * Inicializuje záložky {@link ViewPager2} a {@link TabLayout}, zaregistruje {@link MenuProvider} pro akční nabídku,
     * nastaví posluchače výstupů fragmentů pro volbu parametrů i výběr ceníků a sleduje stav vybraných ceníků.
     *
     * @param view               pohled fragmentu
     * @param savedInstanceState uložený stav fragmentu
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (requireActivity() instanceof MainActivity) {
            MainActivity mainActivity = (MainActivity) requireActivity();
            int topPadding = mainActivity.getTopBarTotalHeight();
            int bottomPadding = mainActivity.getEffectiveNavBarInsetBottom();
            view.setPadding(
                    mainActivity.getEffectiveNavBarInsetLeft(),
                    topPadding,
                    mainActivity.getEffectiveNavBarInsetRight(),
                    bottomPadding
            );
        }

        MenuHost menuHost = requireActivity();
        menuHost.addMenuProvider(new MenuProvider() {
            @Override
            public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
                menuInflater.inflate(R.menu.menu_price_list_compare, menu);
                menuSelectPriceLists = menu.findItem(R.id.menu_select_price_lists);
                updateSelectPriceListsMenuItem();
            }

            @Override
            public boolean onMenuItemSelected(@NonNull MenuItem item) {
                if (item.getItemId() == R.id.menu_values) {
                    ConsuptionContainer container = createContainerFromScenario(priceListsViewModel.getScenario().getValue());
                    PriceListComparisonParametersDialogFragment.newInstance(container)
                            .show(requireActivity().getSupportFragmentManager(), TAG);
                    return true;
                } else if (item.getItemId() == R.id.menu_select_price_lists) {
                    openSelectPriceLists();
                    return true;
                }
                return false;
            }
        }, getViewLifecycleOwner(), Lifecycle.State.RESUMED);

        TabLayout tabLayoutCompare = view.findViewById(R.id.tabLayoutCompare);
        ViewPager2 viewPagerCompare = view.findViewById(R.id.viewPagerCompare);

        ComparePagerAdapter adapter = new ComparePagerAdapter(this);
        viewPagerCompare.setAdapter(adapter);

        new TabLayoutMediator(tabLayoutCompare, viewPagerCompare, (tab, position) -> tab.setText(tabTitles[position])).attach();

        // Listener pro výběr ceníků z PriceListFragment
        getParentFragmentManager().setFragmentResultListener(PriceListFragment.FLAG_PRICE_LIST_FRAGMENT, this, (requestKey, result) -> {
            long[] selectedIds = result.getLongArray(PriceListFragment.FLAG_SELECTED_IDS);
            if (selectedIds != null) {
                DataPriceListSource source = new DataPriceListSource(requireContext());
                source.open();
                List<PriceListModel> selectedList = new ArrayList<>();
                for (long id : selectedIds) {
                    PriceListModel pl = source.readPrice(id);
                    if (pl != null && pl.getId() > 0) {
                        selectedList.add(pl);
                    }
                }
                source.close();
                priceListsViewModel.setSelectedPriceLists(selectedList);
            } else {
                PriceListModel singlePl = BundleCompat.getSerializable(result, PriceListFragment.FLAG_RESULT_PRICE_LIST_FRAGMENT, PriceListModel.class);
                if (singlePl != null) {
                    List<PriceListModel> list = priceListsViewModel.getSelectedPriceLists().getValue();
                    if (list == null) list = new ArrayList<>();
                    List<PriceListModel> newList = new ArrayList<>(list);
                    if (!newList.contains(singlePl)) {
                        newList.add(singlePl);
                    }
                    priceListsViewModel.setSelectedPriceLists(newList);
                }
            }
        });

        // Listener pro parametr dialog
        getParentFragmentManager().setFragmentResultListener(PriceListComparisonParametersDialogFragment.TAG, this, (requestKey, result) -> {
            ConsuptionContainer container = BundleCompat.getSerializable(result, PriceListComparisonParametersDialogFragment.CONSUPTION_CONTAINER, ConsuptionContainer.class);
            if (container != null) {
                priceListsViewModel.setConsuptionContainer(container);
            }
        });

        priceListsViewModel.getSelectedPriceLists().observe(getViewLifecycleOwner(), priceLists -> updateSelectPriceListsMenuItem());
    }


    /**
     * Otevře fragment {@link PriceListFragment} v režimu výběru ceníků pro úpravu porovnávané sady ceníků.
     */
    private void openSelectPriceLists() {
        List<PriceListModel> currentSelected = priceListsViewModel.getSelectedPriceLists().getValue();
        long[] selectedIdsArray = null;
        if (currentSelected != null && !currentSelected.isEmpty()) {
            selectedIdsArray = new long[currentSelected.size()];
            for (int i = 0; i < currentSelected.size(); i++) {
                selectedIdsArray[i] = currentSelected.get(i).getId();
            }
        }
        PriceListFragment priceListFragment = PriceListFragment.newInstance(true, -1L, selectedIdsArray, null, true);
        FragmentChange.replace(requireActivity(), priceListFragment, MOVE, true);
    }


    /**
     * Aktualizuje název položky nabídky pro výběr ceníků podle aktuálního počtu vybraných ceníků ve ViewModelu.
     */
    private void updateSelectPriceListsMenuItem() {
        if (menuSelectPriceLists != null) {
            List<PriceListModel> priceLists = priceListsViewModel.getSelectedPriceLists().getValue();
            int count = priceLists != null ? priceLists.size() : 0;
            if (count == 0) {
                menuSelectPriceLists.setTitle(getString(R.string.select_pricelists_count, 0));
            } else {
                menuSelectPriceLists.setTitle(getString(R.string.selected_pricelists_count, count));
            }
        }
    }


    /**
     * Vytvoří objekt {@link ConsuptionContainer} na základě parametrů z předaného {@link PriceListComparisonScenario}.
     *
     * @param scenario srovnávací scénář spotřeby
     * @return nový objekt {@link ConsuptionContainer} se spotřebou a parametry jističe
     */
    private ConsuptionContainer createContainerFromScenario(PriceListComparisonScenario scenario) {
        PriceListComparisonScenario activeScenario = scenario != null ? scenario : new PriceListComparisonScenario();
        return new ConsuptionContainer(
                activeScenario.getVtKwh() / 1000.0,
                activeScenario.getNtKwh() / 1000.0,
                activeScenario.getMonths(),
                activeScenario.getCountPhaze(),
                activeScenario.getPowerBreaker(),
                activeScenario.getServicesLeft(),
                activeScenario.getServicesRight()
        );
    }


    /**
     * Vytvoří objekt {@link ConsuptionContainer} z načteného objektu {@link JSONObject} a modelu odběrného místa.
     *
     * @param jsonObject             načtená konfigurace z nastavení
     * @param subscriptionPointModel aktuální odběrné místo
     * @return nový objekt {@link ConsuptionContainer}
     */
    private ConsuptionContainer createContainerFromJson(JSONObject jsonObject, SubscriptionPointModel subscriptionPointModel) {
        return new ConsuptionContainer(
                jsonObject.optDouble("vt", 1.0),
                jsonObject.optDouble("nt", 2.0),
                jsonObject.optDouble("month", 12.0),
                jsonObject.optDouble("phaze", subscriptionPointModel.getCountPhaze()),
                jsonObject.optDouble("power", subscriptionPointModel.getPhaze()),
                jsonObject.optDouble("ser_l", 0.0),
                jsonObject.optDouble("ser_r", 0.0)
        );
    }


    /**
     * Načte uložené srovnávací parametry pro aktuální odběrné místo z databáze nastavení a uplatní je ve ViewModelu.
     */
    private void loadCompareParameters() {
        SubscriptionPointModel subscriptionPointModel = SubscriptionPoint.load(requireContext());
        if (subscriptionPointModel == null) {
            return;
        }

        DataSettingsSource dataSettingsSource = new DataSettingsSource(requireContext());
        dataSettingsSource.open();
        String json = dataSettingsSource.loadPriceListCompareParameters(subscriptionPointModel.getId());
        dataSettingsSource.close();

        if (json != null && !json.trim().isEmpty()) {
            try {
                JSONObject jsonObject = new JSONObject(json);
                ConsuptionContainer container = createContainerFromJson(jsonObject, subscriptionPointModel);
                priceListsViewModel.setConsuptionContainer(container);
            } catch (JSONException ignored) {
            }
        }
    }


    /**
     * Pager adaptér pro správu záložek vnořených fragmentů porovnání.
     */
    private static class ComparePagerAdapter extends FragmentStateAdapter {

        /**
         * Vytvoří novou instanci adaptéru {@link ComparePagerAdapter}.
         *
         * @param fragment rodičovský fragment
         */
        public ComparePagerAdapter(@NonNull Fragment fragment) {
            super(fragment);
        }


        /**
         * Vytvoří příslušný fragment záložky podle pozice.
         *
         * @param position pozice záložky (0 = Přehled, 1 = Grafy, 2 = Výhodnost sazeb, 3 = Tabulka)
         * @return nový fragment pro danou záložku
         */
        @NonNull
        @Override
        public Fragment createFragment(int position) {
            return switch (position) {
                case 1 -> PriceListCompareChartsFragment.newInstance();
                case 2 -> PriceListCompareProfitabilityFragment.newInstance();
                case 3 -> PriceListCompareTableFragment.newInstance();
                default -> PriceListCompareOverviewFragment.newInstance();
            };
        }

        @Override
        public int getItemCount() {
            return 4;
        }
    }
}
