package cz.xlisto.odecty.modules.monthlyreading;

import android.os.Bundle;
import android.os.Parcelable;
import android.transition.TransitionManager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
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
import cz.xlisto.odecty.databaze.DataInvoiceSource;
import cz.xlisto.odecty.databaze.DataSubscriptionPointSource;
import cz.xlisto.odecty.dialogs.SubscriptionPointDialogFragment;
import cz.xlisto.odecty.dialogs.YesNoDialogFragment;
import cz.xlisto.odecty.models.MonthlyReadingModel;
import cz.xlisto.odecty.models.SubscriptionPointModel;
import cz.xlisto.odecty.ownview.ViewHelper;
import cz.xlisto.odecty.shp.ShPMonthlyReading;
import cz.xlisto.odecty.utils.DetectScreenMode;
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
    private final String TO = "to";
    private final String FROM = "from";
    private final String ID_CURRENTLY_READING = "idCurrentlyReading";
    private final String ID_PREVIOUS_READING = "idPreviousReading";
    private SubscriptionPointModel subscriptionPoint;
    private FloatingActionButton fab;
    private TextView tvAlert, tvMonthlyReadingFilter;
    private RecyclerView rv;
    private SwitchMaterial swSimplyView, swRegulPrice;
    private ShPMonthlyReading shPMonthlyReading;
    private MonthlyReadingAdapter monthlyReadingAdapter;
    private long from = 0;
    private long to = Long.MAX_VALUE;
    private long idCurrentlyReading = -1, idPreviousReading = -1;
    private MonthlyReadingAdapter.OnClickItemListener onClickItemListener;


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

        //posluchač potvrzení smazání záznamu
        requireActivity().getSupportFragmentManager().setFragmentResultListener(MonthlyReadingAdapter.FLAG_DELETE_MONTHLY_READING, this,
                (requestKey, result) -> {
                    if (result.getBoolean(YesNoDialogFragment.RESULT)) {
                        monthlyReadingAdapter.deleteMonthlyReading(requireContext());
                    }
                });

        //posluchač změny filtru
        requireActivity().getSupportFragmentManager().setFragmentResultListener(MonthlyReadingFilterDialogFragment.MONTHLY_READING_FILTER, this,
                (requestKey, result) -> {
                    to = result.getLong(MonthlyReadingFilterDialogFragment.TO);
                    from = result.getLong(MonthlyReadingFilterDialogFragment.FROM);
                    from = from + ViewHelper.getOffsetTimeZones(from);
                    tvMonthlyReadingFilter.setVisibility(View.VISIBLE);
                    tvMonthlyReadingFilter.setText(getResources().getString(R.string.show_period, ViewHelper.convertLongToDate(from), ViewHelper.convertLongToDate(to)));
                    if (from == 0 && to == Long.MAX_VALUE) {
                        tvMonthlyReadingFilter.setVisibility(View.GONE);
                    } else {
                        tvMonthlyReadingFilter.setVisibility(View.VISIBLE);
                    }
                    loadDataFromDatabase();
                });

        //posluchač změny odběrného místa
        requireActivity().getSupportFragmentManager().setFragmentResultListener(SubscriptionPointDialogFragment.FLAG_UPDATE_SUBSCRIPTION_POINT, this,
                (requestKey, result) -> {
                    loadDataFromDatabase();
                    showFab();
                });

        //posluchač změny měsíčního odečtu - zobrazení detailu v land režimu
        onClickItemListener = (idCurrentlyReading, idPreviousReading) -> {
            this.idCurrentlyReading = idCurrentlyReading;
            this.idPreviousReading = idPreviousReading;
            showDetailFragment(idCurrentlyReading, idPreviousReading);
        };
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_monthly_reading, container, false);
    }


    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (savedInstanceState != null) {
            from = savedInstanceState.getLong(FROM);
            to = savedInstanceState.getLong(TO);
            idCurrentlyReading = savedInstanceState.getLong(ID_CURRENTLY_READING);
            idPreviousReading = savedInstanceState.getLong(ID_PREVIOUS_READING);
        }

        shPMonthlyReading = new ShPMonthlyReading(requireActivity());

        fab = view.findViewById(R.id.fab);
        swSimplyView = view.findViewById(R.id.swSimplyView);
        swRegulPrice = view.findViewById(R.id.swRegulPrice);
        rv = view.findViewById(R.id.rvMonthlyReading);
        tvAlert = view.findViewById(R.id.tvAlertMonthlyReading);
        tvMonthlyReadingFilter = view.findViewById(R.id.tvMonthlyReadingFilter);

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

        showDetailFragment(idCurrentlyReading, idPreviousReading);
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


    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putLong(FROM, from);
        outState.putLong(TO, to);
        outState.putLong(ID_CURRENTLY_READING, idCurrentlyReading);
        outState.putLong(ID_PREVIOUS_READING, idPreviousReading);
    }


    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        requireActivity().getMenuInflater().inflate(R.menu.menu_monthly_reading, menu);
    }


    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.menu_filter_monthly_reading) {
            MonthlyReadingFilterDialogFragment monthlyReadingFilterDialogFragment = MonthlyReadingFilterDialogFragment.newInstance(from, to);
            monthlyReadingFilterDialogFragment.show(requireActivity().getSupportFragmentManager(), "MonthlyReadingFilterDialogFragment");
        }
        return super.onOptionsItemSelected(item);
    }


    /**
     * Načte měsíční odečty z databáze a zobrazí je v RecyclerView.
     */
    private void loadDataFromDatabase() {
        subscriptionPoint = SubscriptionPoint.load(requireActivity());
        tvAlert.setVisibility(View.VISIBLE);

        ArrayList<MonthlyReadingModel> monthlyReadings;
        if (subscriptionPoint != null) {
            DataSubscriptionPointSource dataSubscriptionPointSource = new DataSubscriptionPointSource(requireActivity());
            dataSubscriptionPointSource.open();
            monthlyReadings = dataSubscriptionPointSource.loadMonthlyReadings(subscriptionPoint.getTableO(), from, to);
            dataSubscriptionPointSource.close();

            DataInvoiceSource dataInvoiceSource = new DataInvoiceSource(requireActivity());
            dataInvoiceSource.open();
            if (!dataInvoiceSource.checkInvoiceExists(subscriptionPoint.getTableTED()))
                dataInvoiceSource.insertFirstRecordWithoutInvoice(subscriptionPoint.getTableTED());
            dataInvoiceSource.close();

            monthlyReadingAdapter = new MonthlyReadingAdapter(monthlyReadings, subscriptionPoint,
                    swSimplyView.isChecked(), swRegulPrice.isChecked(), rv);
            monthlyReadingAdapter.setStateRestorationPolicy(RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY);
            monthlyReadingAdapter.setOnClickItemListener(onClickItemListener);
            rv.setAdapter(monthlyReadingAdapter);
            rv.setLayoutManager(new LinearLayoutManager(getContext()));

            if (!monthlyReadings.isEmpty())
                tvAlert.setVisibility(View.GONE);
            else
                tvAlert.setText(getResources().getString(R.string.pridejte_mesicni_odecty));
        } else {
            tvAlert.setText(getResources().getString(R.string.create_place));
        }
    }

    private void showDetailFragment(long idCurrentlyReading, long idPreviousReading) {
        if (DetectScreenMode.isLandscape(requireActivity())) {
            MonthlyReadingDetailFragment monthlyReadingDetailFragment = MonthlyReadingDetailFragment.newInstance(idCurrentlyReading, idPreviousReading, swRegulPrice.isChecked());
            if (idCurrentlyReading >= 0 && idPreviousReading >= 0)
                requireActivity().getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragmentContainerViewDetail, monthlyReadingDetailFragment)
                        .addToBackStack(null).commit();
            else {
                requireActivity().getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragmentContainerViewDetail, new Fragment())
                        .addToBackStack(null).commit();
            }
        }
    }


    /**
     * Zobrazí/skryje tlačítko pro přidání měsíčního odečtu. Podle, zda-li je vybráno odběrné místo.
     */
    private void showFab() {
        if (subscriptionPoint != null) {
            fab.setVisibility(View.VISIBLE);
        } else {
            fab.setVisibility(View.GONE);
        }
    }

}