package cz.xlisto.odecty.modules.subscriptionpoint;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import cz.xlisto.odecty.databaze.DataSubscriptionPointSource;
import cz.xlisto.odecty.models.SubscriptionPointModel;

import android.view.View;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link SubscriptionPointAddFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class SubscriptionPointAddFragment extends SubscriptionPointAddEditAbstract {
    private final String TAG = getClass().getName() + " ";

    public SubscriptionPointAddFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment SubscriptionPointAddFragment.
     */

    public static SubscriptionPointAddFragment newInstance() {
        SubscriptionPointAddFragment fragment = new SubscriptionPointAddFragment();
        return fragment;
    }



    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        btnSave.setOnClickListener(v -> {
            save(createSubscriptionPoint());
            getParentFragmentManager().popBackStack();
        });

    }

    private void save(SubscriptionPointModel subscriptionPoint) {
        DataSubscriptionPointSource dataSubscriptionPointSource = new DataSubscriptionPointSource(getActivity());
        dataSubscriptionPointSource.open();
        dataSubscriptionPointSource.insertSubscriptionPoint(subscriptionPoint);
        dataSubscriptionPointSource.close();
    }
}