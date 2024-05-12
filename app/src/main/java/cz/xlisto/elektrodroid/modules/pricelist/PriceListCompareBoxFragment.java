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
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import java.io.Serializable;

import cz.xlisto.elektrodroid.R;
import cz.xlisto.elektrodroid.models.PriceListModel;
import cz.xlisto.elektrodroid.models.PriceListRegulBuilder;
import cz.xlisto.elektrodroid.utils.FragmentChange;


/**
 * Xlisto 01.03.2024 11:46
 */
public class PriceListCompareBoxFragment extends Fragment {
    private static final String TAG = "PriceListCompareBoxFragment";
    private static final String ARG_CONSUPTION_CONTAINER = "consuptionContainer";
    private static long idPriceListLeft = -1L, idPriceListRight = -1L;
    private PriceListModel priceListLeftNERegul, priceListRightNERegul;
    private PriceListModel priceListLeft, priceListRight, priceListLeftRegul, priceListRightRegul;
    private PriceListRegulBuilder priceListLeftRegulBuilder, priceListRightRegulBuilder;
    private SelectedPriceListsInterface selectedPriceListsInterface;
    private PriceListCompareDetailFragment priceListCompareDetailFragment;
    private PriceListCompareCompactFragment priceListCompareCompactFragment;
    private PriceListsViewModel priceListsViewModel;
    private Button btnLeft;
    private Button btnRight;
    private double vt = 1, nt = 1, month = 1, phaze = 3, power = 25, servicesL = 0, servicesR = 0;
    private ConsuptionContainer consuptionContainer = new ConsuptionContainer();


    public static PriceListCompareBoxFragment newInstance() {
        return new PriceListCompareBoxFragment();
    }


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        priceListsViewModel = new ViewModelProvider(this).get(PriceListsViewModel.class);
        consuptionContainer = new ConsuptionContainer();

        /*if (savedInstanceState != null) {
            consuptionContainer = (ConsuptionContainer) savedInstanceState.getSerializable(ARG_CONSUPTION_CONTAINER);
        }*/

        //consuptionContainer = priceListsViewModel.getConsuptionContainer();
    }


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_price_list_compare_box, container, false);
    }


    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

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

        priceListCompareDetailFragment = PriceListCompareDetailFragment.newInstance();
        priceListCompareCompactFragment = PriceListCompareCompactFragment.newInstance();

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


    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable(ARG_CONSUPTION_CONTAINER, consuptionContainer);
    }


    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        requireActivity().getMenuInflater().inflate(R.menu.menu_price_list_compare, menu);
    }


    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        final int id = item.getItemId();
        if (id == R.id.menu_values) {
            PriceListAddParametersDialogFragment.newInstance(consuptionContainer
            ).show(requireActivity().getSupportFragmentManager(), TAG);

            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    static class ConsuptionContainer implements Serializable {
        double vt, nt, month, phaze, power, servicesL, servicesR;

        public ConsuptionContainer(double vt, double nt, double month, double phaze, double power, double servicesL, double servicesR) {
            this.vt = vt;
            this.nt = nt;
            this.month = month;
            this.phaze = phaze;
            this.power = power;
            this.servicesL = servicesL;
            this.servicesR = servicesR;
        }

        public ConsuptionContainer() {
            this.vt = 1;
            this.nt = 1;
            this.month = 12;
            this.phaze = 3;
            this.power = 25;
            this.servicesL = 0;
            this.servicesR = 0;
        }
    }
}
