package cz.xlisto.odecty.dialogs;

import android.app.Dialog;
import android.os.Bundle;
import android.view.View;

import java.util.ArrayList;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.documentfile.provider.DocumentFile;
import androidx.recyclerview.widget.RecyclerView;
import cz.xlisto.odecty.R;
import cz.xlisto.odecty.models.PriceListModel;
import cz.xlisto.odecty.utils.JSONPriceList;

/**
 * Xlisto 09.12.2023 9:12
 */
public class YesNoDialogRecyclerViewFragment extends YesNoDialogFragment {
    private static final String TAG = "YesNoDialogRecyclerViewFragment";
    public static final String SELECTED_ARRAYLIST = "selectedArrayList";
    public static final String PRICE_ARRAYLIST = "priceArrayList";
    private static DocumentFile documentFile;
    private ArrayList<PriceListModel> priceLists;
    YesNoDialogRecyclerViewAdapter yesNoDialogRecyclerViewAdapter;


    public static YesNoDialogRecyclerViewFragment newInstance(String title, String flagResultDialogFragment, DocumentFile documentFile) {
        YesNoDialogRecyclerViewFragment yesNoDialogRecyclerViewFragment = new YesNoDialogRecyclerViewFragment();
        yesNoDialogRecyclerViewFragment.title = title;
        yesNoDialogRecyclerViewFragment.flagResultDialogFragment = flagResultDialogFragment;
        YesNoDialogRecyclerViewFragment.documentFile = documentFile;
        return yesNoDialogRecyclerViewFragment;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        super.onCreateDialog(savedInstanceState);
        JSONPriceList jsonPriceList = new JSONPriceList();
        //rotace obrazovky
        if (savedInstanceState != null) {
            priceLists = (ArrayList<PriceListModel>) savedInstanceState.getSerializable(PRICE_ARRAYLIST);
        } else {
            priceLists = jsonPriceList.getPriceList(requireContext(), documentFile);
        }
        View view = View.inflate(requireContext(), R.layout.fragment_yes_no_dialog_recycler_view, null);
        RecyclerView recyclerView = view.findViewById(R.id.recyclerView);
        yesNoDialogRecyclerViewAdapter = new YesNoDialogRecyclerViewAdapter(priceLists);
        recyclerView.setAdapter(yesNoDialogRecyclerViewAdapter);
        recyclerView.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(requireActivity()));
        if (priceLists.size() > 0)
            builder.setMessage("Zatrhněte všechny sazby, které chcete nahrát.\nCeník: " + priceLists.get(0).getRada());
        builder.setView(view);
        builder.setPositiveButton(getResources().getString(R.string.ano), (dialog, which) -> {
            ArrayList<PriceListModel> priceListsChecked = new ArrayList<>();
            for (PriceListModel priceList : priceLists) {
                if (priceList.isChecked()) {
                    priceListsChecked.add(priceList);
                }
            }

            Bundle bundle = new Bundle();
            bundle.putBoolean(RESULT, true);
            bundle.putSerializable(SELECTED_ARRAYLIST, priceListsChecked);
            getParentFragmentManager().setFragmentResult(flagResultDialogFragment, bundle);
        });

        return builder.create();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable(PRICE_ARRAYLIST, priceLists);
    }
}
