package cz.xlisto.elektrodroid.modules.pricelist;


import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import java.util.ArrayList;
import java.util.List;

import cz.xlisto.elektrodroid.R;
import cz.xlisto.elektrodroid.models.PriceListModel;


/**
 * Záložka „Tabulka“ pro detailní srovnávací tabulku mezi dvěma libovolnými ceníky z vybrané sady.
 */
public class PriceListCompareTableFragment extends Fragment {

    private PriceListsViewModel viewModel;
    private Spinner spTableLeftPriceList;
    private Spinner spTableRightPriceList;
    private PriceListCompareCompactFragment compactFragment;


    public static PriceListCompareTableFragment newInstance() {
        return new PriceListCompareTableFragment();
    }


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_price_list_compare_table, container, false);
    }


    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(requireParentFragment()).get(PriceListsViewModel.class);

        spTableLeftPriceList = view.findViewById(R.id.spTableLeftPriceList);
        spTableRightPriceList = view.findViewById(R.id.spTableRightPriceList);

        compactFragment = PriceListCompareCompactFragment.newInstance();
        getChildFragmentManager().beginTransaction()
                .replace(R.id.tableFragmentContainer, compactFragment)
                .commit();

        viewModel.getSelectedPriceLists().observe(getViewLifecycleOwner(), this::updateSpinners);
    }


    private void updateSpinners(List<PriceListModel> priceLists) {
        if (priceLists == null || priceLists.isEmpty()) {
            spTableLeftPriceList.setAdapter(null);
            spTableRightPriceList.setAdapter(null);
            return;
        }

        List<String> titles = new ArrayList<>();
        for (PriceListModel pl : priceLists) {
            titles.add(pl.getNameWithPeriod() + " - " + pl.getDistribuce());
        }

        ArrayAdapter<String> adapterLeft = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, titles);
        adapterLeft.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spTableLeftPriceList.setAdapter(adapterLeft);

        ArrayAdapter<String> adapterRight = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, titles);
        adapterRight.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spTableRightPriceList.setAdapter(adapterRight);

        spTableLeftPriceList.setSelection(0);
        if (priceLists.size() > 1) {
            spTableRightPriceList.setSelection(1);
        }

        AdapterView.OnItemSelectedListener listener = new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                notifySelectedPriceLists();
            }


            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        };

        spTableLeftPriceList.setOnItemSelectedListener(listener);
        spTableRightPriceList.setOnItemSelectedListener(listener);

        notifySelectedPriceLists();
    }


    private void notifySelectedPriceLists() {
        List<PriceListModel> priceLists = viewModel.getSelectedPriceLists().getValue();
        if (priceLists == null || priceLists.isEmpty() || compactFragment == null) {
            return;
        }

        int leftIdx = spTableLeftPriceList.getSelectedItemPosition();
        int rightIdx = spTableRightPriceList.getSelectedItemPosition();

        if (leftIdx < 0 || leftIdx >= priceLists.size()) leftIdx = 0;
        if (rightIdx < 0 || rightIdx >= priceLists.size()) rightIdx = priceLists.size() > 1 ? 1 : 0;

        PriceListModel leftPl = priceLists.get(leftIdx);
        PriceListModel rightPl = priceLists.get(rightIdx);

        ConsuptionContainer container = viewModel.getConsuptionContainer().getValue();
        if (container == null) {
            container = new ConsuptionContainer(1, 2, 12, 3, 25, 0, 0);
        }

        compactFragment.onPriceListsSelected(leftPl, rightPl, container);
    }

}
