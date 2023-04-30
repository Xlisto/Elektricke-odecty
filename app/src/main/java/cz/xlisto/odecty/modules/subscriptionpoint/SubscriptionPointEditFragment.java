package cz.xlisto.odecty.modules.subscriptionpoint;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import cz.xlisto.odecty.databaze.DataSubscriptionPointSource;
import cz.xlisto.odecty.models.SubscriptionPointModel;

public class SubscriptionPointEditFragment extends SubscriptionPointAddEditAbstract {
    private final String TAG = getClass().getName() + " ";
    private static final String ARG_ID = "id";
    private long itemId;
    private SubscriptionPointModel subscriptionPoint;
    private static String IS_FIRST_LOAD = "isFirstLoad";
    private boolean isFirstLoad = true;


    public SubscriptionPointEditFragment() {
    }

    public static SubscriptionPointEditFragment newInstance(long param1) {
        SubscriptionPointEditFragment fragment = new SubscriptionPointEditFragment();
        Bundle args = new Bundle();
        args.putLong(ARG_ID, param1);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            itemId = getArguments().getLong(ARG_ID);
        }


    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (savedInstanceState != null)
            isFirstLoad = savedInstanceState.getBoolean(IS_FIRST_LOAD);
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        //Log.w(TAG,"sub "+subscriptionPoint.getName());

        btnSave.setOnClickListener(v -> {
            update(itemId);
            getParentFragmentManager().popBackStack();
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        if (isFirstLoad) {
            isFirstLoad = false;
            loadSubscriptionPoint(itemId);
            setItems();
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(IS_FIRST_LOAD, isFirstLoad);
    }

    private void loadSubscriptionPoint(long itemId) {
        DataSubscriptionPointSource dataSubscriptionPointSource = new DataSubscriptionPointSource(getActivity());
        dataSubscriptionPointSource.open();
        subscriptionPoint = dataSubscriptionPointSource.loadSubscriptionPoint(itemId);
        dataSubscriptionPointSource.close();
    }

    private void update(long itemId) {
        DataSubscriptionPointSource dataSubscriptionPointSource = new DataSubscriptionPointSource(getActivity());
        dataSubscriptionPointSource.open();
        dataSubscriptionPointSource.updateSubscriptionPoint(createSubscriptionPoint(subscriptionPoint.getMilins()), itemId);
        dataSubscriptionPointSource.close();
    }

    private void setItems(){
        letName.setDefaultText(subscriptionPoint.getName());
        letDescription.setDefaultText(subscriptionPoint.getDescription());
        letNumberEletrometer.setDefaultText(subscriptionPoint.getNumberElectricMeter());
        letNumberSubscriptionPoint.setDefaultText(subscriptionPoint.getNumberSubscriptionPoint());
        etPhaze.setText(String.valueOf(subscriptionPoint.getPhaze()));
        etCountPhaze.setText(String.valueOf(subscriptionPoint.getCountPhaze()));
    }
}
