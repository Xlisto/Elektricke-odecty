package cz.xlisto.elektrodroid.modules.monthlyreading;


import static cz.xlisto.elektrodroid.shp.ShPMonthlyReading.REGUL_PRICE;
import static cz.xlisto.elektrodroid.shp.ShPMonthlyReading.SHORT_LIST;
import static cz.xlisto.elektrodroid.utils.FragmentChange.Transaction.MOVE;

import android.content.Context;
import android.os.Bundle;
import android.os.Parcelable;
import android.transition.TransitionManager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.MenuHost;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.switchmaterial.SwitchMaterial;

import java.util.ArrayList;
import java.util.Objects;

import cz.xlisto.elektrodroid.R;
import cz.xlisto.elektrodroid.databaze.DataInvoiceSource;
import cz.xlisto.elektrodroid.databaze.DataSubscriptionPointSource;
import cz.xlisto.elektrodroid.dialogs.SettingsViewDialogFragment;
import cz.xlisto.elektrodroid.dialogs.SubscriptionPointDialogFragment;
import cz.xlisto.elektrodroid.dialogs.YesNoDialogFragment;
import cz.xlisto.elektrodroid.models.MonthlyReadingModel;
import cz.xlisto.elektrodroid.models.SubscriptionPointModel;
import cz.xlisto.elektrodroid.ownview.ViewHelper;
import cz.xlisto.elektrodroid.shp.ShPMonthlyReading;
import cz.xlisto.elektrodroid.utils.DetectScreenMode;
import cz.xlisto.elektrodroid.utils.FragmentChange;
import cz.xlisto.elektrodroid.utils.SubscriptionPoint;
import cz.xlisto.elektrodroid.utils.UIHelper;


/**
 * Fragment pro zobrazení měsíčních odečtů.
 */
public class MonthlyReadingFragment extends Fragment {

    public final String TAG = "MonthlyReadingFragment";
    private final String TO = "to";
    private final String FROM = "from";
    private final String ID_CURRENTLY_READING = "idCurrentlyReading";
    private final String ID_PREVIOUS_READING = "idPreviousReading";
    private SubscriptionPointModel subscriptionPoint;
    private FloatingActionButton fab;
    private Button btnAddMonthlyReading;
    private TextView tvAlert, tvMonthlyReadingFilter;
    private RecyclerView rv;
    private SwitchMaterial swSimplyView, swRegulPrice;
    private ShPMonthlyReading shPMonthlyReading;
    private MonthlyReadingAdapter monthlyReadingAdapter;
    private long from = 0;
    private long to = Long.MAX_VALUE;
    private long idCurrentlyReading = -1, idPreviousReading = -1;
    private MonthlyReadingAdapter.OnClickItemListener onClickItemListener;
    private OnShowRegulPriceListener onShowRegulPriceListener;


    public MonthlyReadingFragment() {
        // Required empty public constructor
    }


    public static MonthlyReadingFragment newInstance() {
        return new MonthlyReadingFragment();
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //posluchač potvrzení smazání záznamu
        requireActivity().getSupportFragmentManager().setFragmentResultListener(MonthlyReadingAdapter.FLAG_DELETE_MONTHLY_READING, this,
                (requestKey, result) -> {
                    if (result.getBoolean(YesNoDialogFragment.RESULT)) {
                        monthlyReadingAdapter.deleteMonthlyReading(requireContext());
                    }
                    setShowTvAlert();
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
                    UIHelper.showButtons(btnAddMonthlyReading, fab, requireActivity(), true);
                });

        //posluchač změny měsíčního odečtu - zobrazení detailu v land režimu
        onClickItemListener = (idCurrentlyReading, idPreviousReading) -> {
            this.idCurrentlyReading = idCurrentlyReading;
            this.idPreviousReading = idPreviousReading;
            showDetailFragment(idCurrentlyReading, idPreviousReading);
        };

        //posluchač na zavření dialogového okna s nastavením
        requireActivity().getSupportFragmentManager().setFragmentResultListener(SettingsViewDialogFragment.FLAG_UPDATE_SETTINGS_FOR_FRAGMENT, this,
                (requestKey, result) -> {
                    UIHelper.showButtons(btnAddMonthlyReading, fab, requireActivity(), true);
                    loadDataFromDatabase();
                });
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        MenuHost menuHost = requireActivity();
        menuHost.addMenuProvider(new MenuProvider() {
            @Override
            public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
                menuInflater.inflate(R.menu.menu_monthly_reading, menu);
            }


            @Override
            public boolean onMenuItemSelected(@NonNull MenuItem menuItem) {
                if (menuItem.getItemId() == R.id.menu_filter_monthly_reading) {
                    MonthlyReadingFilterDialogFragment monthlyReadingFilterDialogFragment = MonthlyReadingFilterDialogFragment.newInstance(from, to);
                    monthlyReadingFilterDialogFragment.show(requireActivity().getSupportFragmentManager(), "MonthlyReadingFilterDialogFragment");
                    return true;
                }

                return false;
            }
        }, getViewLifecycleOwner(), Lifecycle.State.RESUMED);

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
        btnAddMonthlyReading = view.findViewById(R.id.btnAddMonthlyReading);
        swSimplyView = view.findViewById(R.id.swSimplyView);
        swRegulPrice = view.findViewById(R.id.swRegulPrice);
        rv = view.findViewById(R.id.rvMonthlyReading);
        tvAlert = view.findViewById(R.id.tvAlertMonthlyReading);
        tvMonthlyReadingFilter = view.findViewById(R.id.tvMonthlyReadingFilter);
        swSimplyView.setChecked(shPMonthlyReading.get(SHORT_LIST, false));
        swRegulPrice.setChecked(shPMonthlyReading.get(REGUL_PRICE, false));
        swSimplyView.setOnCheckedChangeListener((buttonView, isChecked) -> {
            TransitionManager.beginDelayedTransition(rv);
            if (rv.getLayoutManager() != null) {
                Parcelable out = Objects.requireNonNull(rv.getLayoutManager()).onSaveInstanceState();
                shPMonthlyReading.set(SHORT_LIST, swSimplyView.isChecked());
                monthlyReadingAdapter.showSimpleView(isChecked);
                rv.getLayoutManager().onRestoreInstanceState(out);
            }
        });
        swRegulPrice.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (rv != null) {
                TransitionManager.beginDelayedTransition(rv);
                if (rv.getLayoutManager() != null) {
                    Parcelable out = Objects.requireNonNull(rv.getLayoutManager()).onSaveInstanceState();
                    shPMonthlyReading.set(REGUL_PRICE, swRegulPrice.isChecked());
                    monthlyReadingAdapter.setShowRegulPrice(isChecked);
                    rv.getLayoutManager().onRestoreInstanceState(out);
                    setOnShowRegulPriceListener(isChecked);
                }
            }
        });
        fab.setOnClickListener(v -> addMonthlyReading());
        btnAddMonthlyReading.setOnClickListener(v -> addMonthlyReading());
        showDetailFragment(idCurrentlyReading, idPreviousReading);
    }


    @Override
    public void onResume() {
        super.onResume();
        loadDataFromDatabase();
        UIHelper.showButtons(btnAddMonthlyReading, fab, requireActivity(), true);
        if (rv.getLayoutManager() == null) {
            swRegulPrice.setEnabled(false);
            swSimplyView.setEnabled(false);
        } else {
            swRegulPrice.setEnabled(true);
            swSimplyView.setEnabled(true);
        }
    }


    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putLong(FROM, from);
        outState.putLong(TO, to);
        outState.putLong(ID_CURRENTLY_READING, idCurrentlyReading);
        outState.putLong(ID_PREVIOUS_READING, idPreviousReading);
    }


    /**
     * Načte měsíční odečty z databáze a zobrazí je v RecyclerView.
     */
    private void loadDataFromDatabase() {
        subscriptionPoint = SubscriptionPoint.load(requireActivity());
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
            rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        }
        setShowTvAlert();
    }


    private void showDetailFragment(long idCurrentlyReading, long idPreviousReading) {
        if (DetectScreenMode.isLandscape(requireActivity())) {
            MonthlyReadingDetailFragment monthlyReadingDetailFragment = MonthlyReadingDetailFragment.newInstance(idCurrentlyReading, idPreviousReading, swRegulPrice.isChecked());
            if (idCurrentlyReading >= 0 && idPreviousReading >= 0)
                getChildFragmentManager().beginTransaction()
                        .replace(R.id.fragmentContainerViewDetail, monthlyReadingDetailFragment, MonthlyReadingDetailFragment.TAG)
                        .commit();
            else {
                getChildFragmentManager().beginTransaction()
                        .replace(R.id.fragmentContainerViewDetail, new Fragment())
                        .commit();
            }
        }
    }


    /**
     * Zobrazí fragment pro přidání měsíčního odečtu.
     */
    private void addMonthlyReading() {
        MonthlyReadingAddFragment monthlyReadingAddFragment = MonthlyReadingAddFragment
                .newInstance(subscriptionPoint.getTableO(), subscriptionPoint.getTablePLATBY());
        FragmentChange.replace(requireActivity(), monthlyReadingAddFragment, MOVE, true);
    }


    /**
     * Zobrazí nebo skryje textové upozornění v závislosti na počtu položek v adapteru.
     * Pokud adapter neobsahuje žádné položky, zobrazí textové upozornění.
     * Pokud adapter obsahuje alespoň jednu položku, skryje textové upozornění.
     */
    private void setShowTvAlert() {
        if (subscriptionPoint == null || monthlyReadingAdapter.getItemCount() == 0) {
            tvAlert.setVisibility(View.VISIBLE);
            if (subscriptionPoint == null) {
                tvAlert.setText(getResources().getString(R.string.create_place));
            } else {
                tvAlert.setText(getResources().getString(R.string.pridejte_mesicni_odecty));
            }
        } else {
            tvAlert.setVisibility(View.GONE);
        }
    }


    public interface OnShowRegulPriceListener {

        void onShowRegulPrice(boolean showRegulPrice);

    }


    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        try {
            onShowRegulPriceListener = (OnShowRegulPriceListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context + " must implement OnShowRegulPriceListener");
        }
    }


    public void setOnShowRegulPriceListener(boolean isChecked) {
        this.onShowRegulPriceListener.onShowRegulPrice(isChecked);
    }

}