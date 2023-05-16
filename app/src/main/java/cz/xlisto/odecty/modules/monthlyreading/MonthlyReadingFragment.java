package cz.xlisto.odecty.modules.monthlyreading;

import android.os.Bundle;
import android.os.Parcelable;
import android.transition.TransitionManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.switchmaterial.SwitchMaterial;

import java.util.ArrayList;
import java.util.Objects;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import cz.xlisto.odecty.R;
import cz.xlisto.odecty.databaze.DataSubscriptionPointSource;
import cz.xlisto.odecty.dialogs.YesNoDialogFragment;
import cz.xlisto.odecty.models.MonthlyReadingModel;
import cz.xlisto.odecty.models.SubscriptionPointModel;
import cz.xlisto.odecty.shp.ShPMonthlyReading;
import cz.xlisto.odecty.utils.FragmentChange;
import cz.xlisto.odecty.utils.SubscriptionPoint;

import static cz.xlisto.odecty.shp.ShPMonthlyReading.REGUL_PRICE;
import static cz.xlisto.odecty.shp.ShPMonthlyReading.SHORT_LIST;
import static cz.xlisto.odecty.utils.FragmentChange.Transaction.MOVE;

/**
 * Fragment pro zobrazení měsíčních odečtů.
 */
public class MonthlyReadingFragment extends Fragment {
    private final String TAG = "MonthlyReadingFragment";
    private SubscriptionPointModel subscriptionPoint;
    private FloatingActionButton fab;
    private TextView subscriptionPointName;
    private TextView tvAlert;
    private RecyclerView rv;
    private SwitchMaterial swSimplyView, swRegulPrice;
    private ShPMonthlyReading shPMonthlyReading;
    private MonthlyReadingAdapter monthlyReadingAdapter;


    public MonthlyReadingFragment() {
        // Required empty public constructor
    }


    public static MonthlyReadingFragment newInstance() {
        return new MonthlyReadingFragment();
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_monthly_reading, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        shPMonthlyReading = new ShPMonthlyReading(requireActivity());

        fab = view.findViewById(R.id.fab);
        subscriptionPointName = view.findViewById(R.id.tvSubscriptionPointName);
        swSimplyView = view.findViewById(R.id.swSimplyView);
        swRegulPrice = view.findViewById(R.id.swRegulPrice);
        rv = view.findViewById(R.id.rvMonthlyReading);
        tvAlert = view.findViewById(R.id.tvAlertMonthlyReading);

        subscriptionPointName.setText("");

        swSimplyView.setChecked(shPMonthlyReading.get(SHORT_LIST, false));
        swRegulPrice.setChecked(shPMonthlyReading.get(REGUL_PRICE, false));
        swSimplyView.setOnCheckedChangeListener((buttonView, isChecked) -> {
            TransitionManager.beginDelayedTransition(rv);
            Parcelable out = Objects.requireNonNull(rv.getLayoutManager()).onSaveInstanceState();
            shPMonthlyReading.set(SHORT_LIST, swSimplyView.isChecked());
            monthlyReadingAdapter.showSimpleView(isChecked);

            rv.getLayoutManager().onRestoreInstanceState(out);
        });
        swRegulPrice.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (rv != null) {
                Parcelable out = Objects.requireNonNull(rv.getLayoutManager()).onSaveInstanceState();
                shPMonthlyReading.set(REGUL_PRICE, swRegulPrice.isChecked());

                monthlyReadingAdapter.setShowRegulPrice(isChecked);
                rv.getLayoutManager().onRestoreInstanceState(out);
            }
        });

        fab.setOnClickListener(v -> {
            MonthlyReadingAddFragment monthlyReadingAddFragment = MonthlyReadingAddFragment
                    .newInstance(subscriptionPoint.getTableO(), subscriptionPoint.getTablePLATBY());
            FragmentChange.replace(requireActivity(), monthlyReadingAddFragment, MOVE, true);
        });

        requireActivity().getSupportFragmentManager().setFragmentResultListener(MonthlyReadingAdapter.FLAG_DELETE_MONTHLY_READING, this,
                (requestKey, result) -> {
                    if (result.getBoolean(YesNoDialogFragment.RESULT))
                        monthlyReadingAdapter.deleteMonthlyReading();
                });
    }


    @Override
    public void onResume() {
        super.onResume();
        loadDataFromDatabase();
        showFab();
    }


    @Override
    public void onViewStateRestored(@Nullable Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);

    }

    private void loadDataFromDatabase() {
        subscriptionPoint = SubscriptionPoint.load(requireActivity());
        tvAlert.setVisibility(View.VISIBLE);

        ArrayList<MonthlyReadingModel> monthlyReadings;
        if (subscriptionPoint != null) {
            DataSubscriptionPointSource dataSubscriptionPointSource = new DataSubscriptionPointSource(getActivity());
            dataSubscriptionPointSource.open();
            monthlyReadings = dataSubscriptionPointSource.loadMonthlyReadings(subscriptionPoint.getTableO());
            if (!dataSubscriptionPointSource.checkInvoiceExists(subscriptionPoint.getTableTED()))
                dataSubscriptionPointSource.insertFirstRecordWithoutInvoice(subscriptionPoint.getTableTED());
            dataSubscriptionPointSource.close();

            monthlyReadingAdapter = new MonthlyReadingAdapter(getActivity(), monthlyReadings,
                    subscriptionPoint,
                    swSimplyView.isChecked(), swRegulPrice.isChecked(), rv);
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


    /**
     * Zobrazí/skryje tlačítko pro přidání měsíčního odečtu. Podle, toho zda je zvoleno odběrné místo.
     */
    private void showFab() {
        if (subscriptionPoint != null) {
            fab.setVisibility(View.VISIBLE);
            subscriptionPointName.setText(subscriptionPoint.getName());
        } else {
            fab.setVisibility(View.GONE);
        }
    }

}