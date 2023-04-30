package cz.xlisto.cenik.modules.monthlyreading;

import android.os.Bundle;
import android.os.Parcelable;
import android.transition.TransitionManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.TextView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.switchmaterial.SwitchMaterial;

import java.util.ArrayList;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import cz.xlisto.cenik.R;
import cz.xlisto.cenik.databaze.DataSubscriptionPointSource;
import cz.xlisto.cenik.models.MonthlyReadingModel;
import cz.xlisto.cenik.models.SubscriptionPointModel;
import cz.xlisto.cenik.shp.ShPMonthlyReading;
import cz.xlisto.cenik.utils.FragmentChange;
import cz.xlisto.cenik.utils.SubscriptionPoint;

import static cz.xlisto.cenik.shp.ShPMonthlyReading.REGUL_PRICE;
import static cz.xlisto.cenik.shp.ShPMonthlyReading.SHORT_LIST;
import static cz.xlisto.cenik.utils.FragmentChange.Transaction.MOVE;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link MonthlyReadingFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class MonthlyReadingFragment extends Fragment {
    private final String TAG = getClass().getName() + " ";
    private FloatingActionButton fab;
    private SubscriptionPointModel subscriptionPoint;
    private TextView subscriptionPointName, tvAlert;
    private RecyclerView rv;
    private SwitchMaterial swSimplyView, swRegulPrice;
    private ShPMonthlyReading shPMonthlyReading;
    private static final String BUNDLE_RECYCLER_LAYOUT = "classname.recycler.layout";
    private Bundle saved;
    private MonthlyReadingAdapter monthlyReadingAdapter;


    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    public MonthlyReadingFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment MonthlyReadingFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static MonthlyReadingFragment newInstance(String param1, String param2) {
        MonthlyReadingFragment fragment = new MonthlyReadingFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        subscriptionPoint = SubscriptionPoint.load(getActivity());

        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_monthly_reading, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        shPMonthlyReading = new ShPMonthlyReading(getActivity());
        saved = savedInstanceState;

        fab = view.findViewById(R.id.fab);
        subscriptionPointName = view.findViewById(R.id.tvSubscriptionPointName);
        swSimplyView = view.findViewById(R.id.swSimplyView);
        swRegulPrice = view.findViewById(R.id.swRegulPrice);
        rv = view.findViewById(R.id.rvMonthlyReading);
        tvAlert = view.findViewById(R.id.tvAlertMonthlyReading);

        subscriptionPointName.setText("");

        swSimplyView.setChecked(shPMonthlyReading.get(SHORT_LIST, false));
        swRegulPrice.setChecked(shPMonthlyReading.get(REGUL_PRICE, false));
        swSimplyView.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                TransitionManager.beginDelayedTransition(rv);
                Parcelable out = rv.getLayoutManager().onSaveInstanceState();
                shPMonthlyReading.set(SHORT_LIST, swSimplyView.isChecked());
                monthlyReadingAdapter.showSimpleView(isChecked);
                //onResume();
                rv.getLayoutManager().onRestoreInstanceState(out);
            }
        });
        swRegulPrice.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (rv != null) {
                    Parcelable out = rv.getLayoutManager().onSaveInstanceState();
                    shPMonthlyReading.set(REGUL_PRICE, swRegulPrice.isChecked());
                    //onResume();
                    monthlyReadingAdapter.setShowRegulPrice(isChecked);
                    rv.getLayoutManager().onRestoreInstanceState(out);
                }
            }
        });

        if (subscriptionPoint != null) {
            fab.setVisibility(View.VISIBLE);
            subscriptionPointName.setText(subscriptionPoint.getName());
        } else {
            fab.setVisibility(View.GONE);
        }

        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MonthlyReadingAddFragment monthlyReadingAddFragment = MonthlyReadingAddFragment
                        .newInstance(subscriptionPoint.getTableO(), subscriptionPoint.getTablePLATBY());
                FragmentChange.replace(getActivity(), monthlyReadingAddFragment, MOVE, true);
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        ArrayList<MonthlyReadingModel> monthlyReadings = null;
        tvAlert.setVisibility(View.VISIBLE);

        if (subscriptionPoint != null) {
            DataSubscriptionPointSource dataSubscriptionPointSource = new DataSubscriptionPointSource(getActivity());
            dataSubscriptionPointSource.open();
            monthlyReadings = dataSubscriptionPointSource.loadMonthlyReadings(subscriptionPoint.getTableO());
            dataSubscriptionPointSource.close();

            monthlyReadingAdapter = new MonthlyReadingAdapter(getActivity(), monthlyReadings,
                    subscriptionPoint,
                    swSimplyView.isChecked(), swRegulPrice.isChecked(), rv, null, null);
            monthlyReadingAdapter.setStateRestorationPolicy(RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY);
            rv.setAdapter(monthlyReadingAdapter);
            rv.setLayoutManager(new LinearLayoutManager(getContext()));


            if (monthlyReadings.size() > 0) {

                tvAlert.setVisibility(View.GONE);
            } else {

                tvAlert.setText(getResources().getString(R.string.pridejte_mesicni_odecty));
            }
        } else {
            tvAlert.setText(getResources().getString(R.string.vytvorte_odberne_misto));
        }
    }

    private void restor() {
        if (saved != null) {
            Parcelable savedRecyclerLayoutState = saved.getParcelable(BUNDLE_RECYCLER_LAYOUT);
            rv.getLayoutManager().onRestoreInstanceState(savedRecyclerLayoutState);
        }
    }


    @Override
    public void onViewStateRestored(@Nullable Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);

    }

}