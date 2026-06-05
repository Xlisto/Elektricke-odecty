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
import androidx.core.view.MenuHost;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.ViewModelProvider;

import cz.xlisto.elektrodroid.R;
import cz.xlisto.elektrodroid.models.PriceListModel;
import cz.xlisto.elektrodroid.utils.FragmentChange;


/**
 * Kontejnerový fragment pro porovnání ceníků.
 * Zajišťuje výběr levého/pravého ceníku, parametry výpočtu a předání dat do podřízeného fragmentu.
 * Xlisto 01.03.2024 11:46
 */
public class PriceListCompareBoxFragment extends Fragment {
    private static final String TAG = "PriceListCompareBoxFragment";
    private static final String ARG_CONSUPTION_CONTAINER = "consuptionContainer";
    private static long idPriceListLeft = -1L, idPriceListRight = -1L;
    private PriceListModel priceListLeftNERegul, priceListRightNERegul;
    private PriceListModel priceListLeft, priceListRight;
    private SelectedPriceListsInterface selectedPriceListsInterface;
    private PriceListsViewModel priceListsViewModel;
    private Button btnLeft;
    private Button btnRight;
    private ConsuptionContainer consuptionContainer = new ConsuptionContainer();


    /**
     * @return nová instance fragmentu porovnání ceníků
     */
    public static PriceListCompareBoxFragment newInstance() {
        return new PriceListCompareBoxFragment();
    }


    /**
     * Inicializuje ViewModel a výchozí kontejner vstupních parametrů.
     *
     * @param savedInstanceState uložený stav instance (může být null)
     */
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        priceListsViewModel = new ViewModelProvider(this).get(PriceListsViewModel.class);
        consuptionContainer = new ConsuptionContainer();
    }


    /**
     * Vytvoří kořenové zobrazení fragmentu.
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_price_list_compare_box, container, false);
    }


    /**
     * Nastaví menu, posluchače výběru ceníků a posluchače změny vstupních parametrů.
     *
     * @param view               kořenové zobrazení fragmentu
     * @param savedInstanceState uložený stav instance (může být null)
     */
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
                    PriceListAddParametersDialogFragment.newInstance(consuptionContainer)
                            .show(requireActivity().getSupportFragmentManager(), TAG);
                    return true;
                }
                return false;
            }
        }, getViewLifecycleOwner(), Lifecycle.State.RESUMED);

        btnLeft = view.findViewById(R.id.btnLeft);
        btnRight = view.findViewById(R.id.btnRight);

        btnLeft.setOnClickListener(v -> {
            PriceListFragment priceListFragment = PriceListFragment.newInstance(true, idPriceListLeft, PriceListFragment.Side.LEFT);
            FragmentChange.replace(requireActivity(), priceListFragment, MOVE, true);
        });

        btnRight.setOnClickListener(v -> {
            PriceListFragment priceListFragment = PriceListFragment.newInstance(true, idPriceListRight, PriceListFragment.Side.RIGHT);
            FragmentChange.replace(requireActivity(), priceListFragment, MOVE, true);
        });

        PriceListCompareCompactFragment priceListCompareCompactFragment = PriceListCompareCompactFragment.newInstance();

        //getChildFragmentManager().beginTransaction().replace(R.id.price_list_compare_container, priceListCompareDetailFragment).commit();
        getChildFragmentManager().beginTransaction().replace(R.id.price_list_compare_container, priceListCompareCompactFragment).commit();
        try {
            //selectedPriceListsInterface = priceListCompareDetailFragment;
            selectedPriceListsInterface = priceListCompareCompactFragment;
        } catch (ClassCastException e) {
            throw new ClassCastException(requireContext() + " must implement SelectedPriceListsInterface");
        }


        //listener pro výběr ceníku
        getParentFragmentManager().setFragmentResultListener(PriceListFragment.FLAG_PRICE_LIST_FRAGMENT, this, (requestKey, result) -> {
            PriceListModel priceList = (PriceListModel) result.getSerializable(PriceListFragment.FLAG_RESULT_PRICE_LIST_FRAGMENT);
            PriceListFragment.Side side = (PriceListFragment.Side) result.getSerializable(PriceListFragment.FLAG_SIDE);
            if (priceList != null && side != null) {
                if (side.equals(PriceListFragment.Side.LEFT)) {
                    priceListLeftNERegul = priceList;
                    idPriceListLeft = priceList.getId();
                    priceListsViewModel.setPriceListLeft(priceList);
                } else {
                    priceListRightNERegul = priceList;
                    idPriceListRight = priceList.getId();
                    priceListsViewModel.setPriceListRight(priceList);
                }
            }
            selectedPriceListsInterface.onPriceListsSelected(priceListLeftNERegul, priceListRightNERegul,consuptionContainer);
            if (priceListLeftNERegul != null)
                btnLeft.setText(priceListLeftNERegul.getName());
            if (priceListRightNERegul != null)
                btnRight.setText(priceListRightNERegul.getName());
        });

        //listener pro výběr parametrů
        getParentFragmentManager().setFragmentResultListener(PriceListAddParametersDialogFragment.TAG, this, (requestKey, result) -> {
            consuptionContainer = (ConsuptionContainer) result.getSerializable(PriceListAddParametersDialogFragment.CONSUPTION_CONTAINER);
            selectedPriceListsInterface.onPriceListsSelected(priceListLeftNERegul, priceListRightNERegul, consuptionContainer);
            priceListsViewModel.setConsuptionContainer(consuptionContainer);
        });

    }


    /**
     * Obnoví navázání dat z ViewModelu a předá aktuální stav do cílového porovnávacího fragmentu.
     */
    @Override
    public void onResume() {
        super.onResume();
        //posluchač pro aktualizaci dat levého ceníku
        priceListsViewModel.getPriceListLeft().observe(getViewLifecycleOwner(), priceList -> {
            priceListLeft = priceList;
            if (priceListLeft != null)
                btnLeft.setText(priceListLeft.getName());
           //selectedPriceListsInterface.onPriceListsSelected(priceListLeftNERegul, priceListRightNERegul);
        });

        //posluchač pro aktualizaci dat pravého ceníku
        priceListsViewModel.getPriceListRight().observe(getViewLifecycleOwner(), priceList -> {
            priceListRight = priceList;
            if (priceListRight != null)
                btnRight.setText(priceListRight.getName());
            //selectedPriceListsInterface.onPriceListsSelected(priceListLeftNERegul, priceListRightNERegul);
        });

        //posluchač pro aktualizaci dat parametrů
        priceListsViewModel.getConsuptionContainer().observe(getViewLifecycleOwner(), consuptionContainer -> {
            this.consuptionContainer = consuptionContainer;
            //selectedPriceListsInterface.onPriceListsSelected(priceListLeftNERegul, priceListRightNERegul, consuptionContainer);
        });

        selectedPriceListsInterface.onPriceListsSelected(priceListLeft, priceListRight, consuptionContainer);
    }


    /**
     * Uloží aktuální kontejner vstupních parametrů.
     *
     * @param outState cílový bundle pro uložení stavu
     */
    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable(ARG_CONSUPTION_CONTAINER, consuptionContainer);
    }



}
