package cz.xlisto.elektrodroid.modules.subscriptionpoint;


import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import cz.xlisto.elektrodroid.databaze.DataSubscriptionPointSource;
import cz.xlisto.elektrodroid.models.SubscriptionPointModel;
import cz.xlisto.elektrodroid.shp.ShPSubscriptionPoint;
import cz.xlisto.elektrodroid.utils.Keyboard;

/**
 * Fragment vytvoření nového odběrného místa
 */
public class SubscriptionPointAddFragment extends SubscriptionPointAddEditAbstract {
    private final String TAG = getClass().getName() + " ";

    public SubscriptionPointAddFragment() {
        // Required empty public constructor
    }


    public static SubscriptionPointAddFragment newInstance() {
        return new SubscriptionPointAddFragment();
    }


    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        btnSave.setOnClickListener(v -> {
            long id = save(createSubscriptionPoint());

            //uložení id do shared preferences pro nastavení právě zvoleného odběrného místa
            ShPSubscriptionPoint shpSubscriptionPoint = new ShPSubscriptionPoint(requireActivity());
            shpSubscriptionPoint.set(ShPSubscriptionPoint.ID_SUBSCRIPTION_POINT_LONG, id);

            Keyboard.hide(requireActivity());
            getParentFragmentManager().popBackStack();
        });
    }


    /**
     * Vytvoří objekt SubscriptionPointModel z dat z formuláře a uloží ho do databáze
     * @param subscriptionPoint objekt SubscriptionPointModel
     * @return long id nově vytvořeného záznamu
     */
    private long save(SubscriptionPointModel subscriptionPoint) {
        DataSubscriptionPointSource dataSubscriptionPointSource = new DataSubscriptionPointSource(getActivity());
        dataSubscriptionPointSource.open();
        long id = dataSubscriptionPointSource.insertSubscriptionPoint(subscriptionPoint);
        dataSubscriptionPointSource.close();
        return id;
    }
}