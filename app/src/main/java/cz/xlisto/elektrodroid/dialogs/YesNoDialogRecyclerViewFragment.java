package cz.xlisto.elektrodroid.dialogs;


import android.app.Dialog;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.documentfile.provider.DocumentFile;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

import cz.xlisto.elektrodroid.R;
import cz.xlisto.elektrodroid.models.PriceListModel;
import cz.xlisto.elektrodroid.utils.JSONPriceList;


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
        Button btnNo = view.findViewById(R.id.btnCancel);
        Button btnYes = view.findViewById(R.id.btnOk);
        yesNoDialogRecyclerViewAdapter = new YesNoDialogRecyclerViewAdapter(priceLists);
        recyclerView.setAdapter(yesNoDialogRecyclerViewAdapter);
        recyclerView.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(requireActivity()));

        if (!priceLists.isEmpty())
            builder.setMessage("Zatrhněte všechny sazby, které chcete nahrát.\nCeník: " + priceLists.get(0).getRada());
        builder.setView(view);

        btnYes.setOnClickListener(v -> {
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
            dismiss();
        });

        btnNo.setOnClickListener(v -> {
            Bundle bundle = new Bundle();
            bundle.putBoolean(RESULT, false);
            getParentFragmentManager().setFragmentResult(flagResultDialogFragment, bundle);
            dismiss();
        });

        return builder.create();
    }


    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable(PRICE_ARRAYLIST, priceLists);
    }
}
