package cz.xlisto.elektrodroid.modules.pricelist;

import static cz.xlisto.elektrodroid.utils.FragmentChange.Transaction.MOVE;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

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
 * Hlavní kontejnerový fragment porovnávání ceníků obsahující 4 záložky (Přehled, Grafy, Výhodnost sazeb, Tabulka).
 */
public class PriceListCompareBoxFragment extends Fragment {

    private static final String TAG = "PriceListCompareBoxFragment";
    private PriceListsViewModel priceListsViewModel;

    private Button btnSelectPriceLists;

    private final String[] tabTitles = new String[]{"Přehled", "Grafy", "Výhodnost sazeb", "Tabulka"};

    public static PriceListCompareBoxFragment newInstance() {
        return new PriceListCompareBoxFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        priceListsViewModel = new ViewModelProvider(this).get(PriceListsViewModel.class);
        loadCompareParameters();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_price_list_compare_box, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        MenuHost menuHost = requireActivity();
        menuHost.addMenuProvider(new MenuProvider() {
            @Override
            public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
                menuInflater.inflate(R.menu.menu_price_list_compare, menu);
            }

            @Override
            public boolean onMenuItemSelected(@NonNull MenuItem item) {
                if (item.getItemId() == R.id.menu_values) {
                    ConsuptionContainer container = createContainerFromScenario(priceListsViewModel.getScenario().getValue());
                    PriceListComparisonParametersDialogFragment.newInstance(container)
                            .show(requireActivity().getSupportFragmentManager(), TAG);
                    return true;
                }
                return false;
            }
        }, getViewLifecycleOwner(), Lifecycle.State.RESUMED);

        btnSelectPriceLists = view.findViewById(R.id.btnSelectPriceLists);
        TabLayout tabLayoutCompare = view.findViewById(R.id.tabLayoutCompare);
        ViewPager2 viewPagerCompare = view.findViewById(R.id.viewPagerCompare);

        ComparePagerAdapter adapter = new ComparePagerAdapter(this);
        viewPagerCompare.setAdapter(adapter);

        new TabLayoutMediator(tabLayoutCompare, viewPagerCompare, (tab, position) -> tab.setText(tabTitles[position])).attach();

        btnSelectPriceLists.setOnClickListener(v -> {
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
        });

        // Listener pro výběr ceníků z PriceListFragment
        getParentFragmentManager().setFragmentResultListener(PriceListFragment.FLAG_PRICE_LIST_FRAGMENT, this, (requestKey, result) -> {
            long[] selectedIds = result.getLongArray(PriceListFragment.FLAG_SELECTED_IDS);
            if (selectedIds != null && selectedIds.length > 0) {
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

        priceListsViewModel.getSelectedPriceLists().observe(getViewLifecycleOwner(), priceLists -> {
            int count = priceLists != null ? priceLists.size() : 0;
            if (count == 0) {
                btnSelectPriceLists.setText(String.format(java.util.Locale.getDefault(), "Vybrat ceníky ke srovnání (%d vybráno)", 0));
            } else {
                btnSelectPriceLists.setText(String.format(java.util.Locale.getDefault(), "Vybrané ceníky: %d (klepnutím upravte)", count));
            }
        });
    }


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

    private static class ComparePagerAdapter extends FragmentStateAdapter {

        public ComparePagerAdapter(@NonNull Fragment fragment) {
            super(fragment);
        }

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
