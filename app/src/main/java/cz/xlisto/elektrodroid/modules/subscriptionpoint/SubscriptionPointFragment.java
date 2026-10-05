package cz.xlisto.elektrodroid.modules.subscriptionpoint;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.app.TimePickerDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.SpannableStringBuilder;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.MenuHost;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

import cz.xlisto.elektrodroid.R;
import cz.xlisto.elektrodroid.databaze.DataSubscriptionPointSource;
import cz.xlisto.elektrodroid.dialogs.YesNoDialogFragment;
import cz.xlisto.elektrodroid.models.SubscriptionPointModel;
import cz.xlisto.elektrodroid.modules.settings.SettingsFragment;
import cz.xlisto.elektrodroid.services.MonthlyReadingReminderScheduler;
import cz.xlisto.elektrodroid.shp.ShPSubscriptionPoint;
import cz.xlisto.elektrodroid.utils.FragmentChange;
import cz.xlisto.elektrodroid.utils.UIHelper;

/**
 * Fragment pro zobrazení a správu vybraného odběrného místa.
 * Zajišťuje načtení seznamu míst ze SQLite databáze, reakci na změny výběru v rozbalovacím seznamu (Spinner),
 * předání vybraných dat do ostatních modulů aplikace přes {@link ShPSubscriptionPoint}
 * a spuštění dialogu pro úpravu detailů.
 */
public class SubscriptionPointFragment extends Fragment {

    private static final String FLAG_DELETE_SUBSCRIPTION_POINT = "flagDeleteSubscriptionPoint";
    private static final String PREF_READING_NOTIFICATION_ENABLED = "reading_notification_enabled";
    private static final String PREF_READING_NOTIFICATION_FREQUENCY = "reading_notification_frequency";
    private static final String PREF_READING_NOTIFICATION_TIME = "reading_notification_time";
    private static final String PREF_READING_NOTIFICATION_DAY_OF_MONTH = "reading_notification_day_of_month";
    private static final String PREF_READING_NOTIFICATION_DAY_OF_WEEK = "reading_notification_day_of_week";
    private static final int FREQUENCY_MONTHLY = 0;
    private static final int FREQUENCY_WEEKLY = 1;
    private static final String TIME_DEFAULT = "09:00";
    private static final String DAY_OF_MONTH_DEFAULT = "1";
    private static final int DAY_OF_MONTH_MIN = 1;
    private static final int DAY_OF_MONTH_MAX = 31;

    private FloatingActionButton fab;
    private Button btnAddSubscriptionPoint;
    private Spinner spSubscriptionPoint;
    private Spinner spSubscriptionPointNotification;
    private TextView tvDescription, tvPhaze, tvNumberElectricMeter, tvNumberSubscriptionPoint, tvNewSubscriptionPoint, tvNotificationTime;
    private View lnSpinner, lnDescription, lnPhaze, lnNumberElectricMeter, lnNumberSubscriptionPoint;
    private LinearLayout layoutSubscriptionPointManagement;
    private GraphAnnualOverviewView graphAnnualOverview;
    private View btnFullscreenGraph;
    private View lnReadingNotification;
    private View layoutMonthlyDay;
    private View layoutWeeklyDay;
    private View layoutNotificationTime;
    private CheckBox chReadingNotificationEnabled;
    private Spinner spinnerReadingNotificationFrequency;
    private Spinner spinnerDayOfWeek;
    private EditText etDayOfMonth;
    private ScrollView sc;
    private TabLayout tabLayout;

    private SubscriptionPointModel selectedSubscriptionPoint;
    private long itemId = 0L;
    private boolean suppressReadingNotificationCallbacks = false;
    private boolean suppressSubscriptionPointSpinnerCallbacks = false;

    /**
     * Vytvoří novou instanci fragmentu {@link SubscriptionPointFragment}.
     *
     * @return Nová instance fragmentu SubscriptionPointFragment.
     */
    public static SubscriptionPointFragment newInstance() {
        return new SubscriptionPointFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        MenuHost menuHost = requireActivity();
        menuHost.addMenuProvider(new MenuProvider() {
            @Override
            public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
                menuInflater.inflate(R.menu.menu_subscription_point, menu);
            }

            @Override
            public boolean onMenuItemSelected(@NonNull MenuItem menuItem) {
                if (menuItem.getItemId() == R.id.menu_subscription_point_edit) {
                    if (itemId > 0) {
                        edit();
                    }
                    return true;
                } else if (menuItem.getItemId() == R.id.menu_subscription_point_delete) {
                    if (itemId > 0) {
                        showDeleteDialog();
                    }
                    return true;
                }
                return false;
            }
        }, getViewLifecycleOwner(), Lifecycle.State.RESUMED);

        return inflater.inflate(R.layout.fragment_subscription_point, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        requireActivity().invalidateOptionsMenu();
        fab = view.findViewById(R.id.fab);
        spSubscriptionPoint = view.findViewById(R.id.spSubscriptionPoints);
        spSubscriptionPointNotification = view.findViewById(R.id.spSubscriptionPointsNotification);
        tvDescription = view.findViewById(R.id.tvDescription);
        tvPhaze = view.findViewById(R.id.tvPhaze);
        tvNumberElectricMeter = view.findViewById(R.id.tvNumberElectrometer);
        tvNumberSubscriptionPoint = view.findViewById(R.id.tvNumberSubscriptionPoint);
        tvNewSubscriptionPoint = view.findViewById(R.id.tvCreateNewSubscriptionPoint);
        btnAddSubscriptionPoint = view.findViewById(R.id.btnAddSubscriptionPoint);
        layoutSubscriptionPointManagement = view.findViewById(R.id.layoutSubscriptionPointManagement);
        lnSpinner = view.findViewById(R.id.lnSpinner);
        lnDescription = view.findViewById(R.id.lnDescription);
        lnPhaze = view.findViewById(R.id.lnPhaze);
        lnNumberElectricMeter = view.findViewById(R.id.lnNumberElectrometer);
        lnNumberSubscriptionPoint = view.findViewById(R.id.lnNumberSubscriptionPoint);
        sc = view.findViewById(R.id.scrollView);
        graphAnnualOverview = view.findViewById(R.id.graphAnnualOverview);
        btnFullscreenGraph = view.findViewById(R.id.btnFullscreenGraph);
        lnReadingNotification = view.findViewById(R.id.lnReadingNotification);
        chReadingNotificationEnabled = view.findViewById(R.id.chReadingNotificationEnabled);
        spinnerReadingNotificationFrequency = view.findViewById(R.id.spinnerReadingNotificationFrequency);
        layoutMonthlyDay = view.findViewById(R.id.layoutMonthlyDay);
        layoutWeeklyDay = view.findViewById(R.id.layoutWeeklyDay);
        layoutNotificationTime = view.findViewById(R.id.layoutNotificationTime);
        etDayOfMonth = view.findViewById(R.id.etDayOfMonth);
        spinnerDayOfWeek = view.findViewById(R.id.spinnerDayOfWeek);
        tvNotificationTime = view.findViewById(R.id.tvNotificationTime);
        tabLayout = view.findViewById(R.id.tabLayout);

        if (btnFullscreenGraph != null) {
            btnFullscreenGraph.setOnClickListener(v -> {
                if (itemId > 0) {
                    FragmentChange.replace(requireActivity(), AnnualGraphFullscreenFragment.newInstance(itemId), FragmentChange.Transaction.MOVE, true);
                }
            });
        }
        fab.setOnClickListener(v -> addSubcsriptionPoint());
        btnAddSubscriptionPoint.setOnClickListener(v -> addSubcsriptionPoint());

        setupTabs();

        if (etDayOfMonth != null) {
            InputFilter minMaxFilter = (source, start, end, dest, dstart, dend) -> {
                try {
                    String newVal = dest.subSequence(0, dstart).toString() + source.subSequence(start, end) + dest.subSequence(dend, dest.length());
                    if (newVal.isEmpty()) return null;
                    int input = Integer.parseInt(newVal);
                    if (input >= DAY_OF_MONTH_MIN && input <= DAY_OF_MONTH_MAX) return null;
                } catch (NumberFormatException ignored) {
                }
                return "";
            };
            etDayOfMonth.setFilters(new InputFilter[]{minMaxFilter});
        }

        if (chReadingNotificationEnabled != null) {
            chReadingNotificationEnabled.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (suppressReadingNotificationCallbacks) {
                    return;
                }
                if (itemId > 0) {
                    setReadingNotificationEnabled(itemId, isChecked);
                    updateReadingNotificationVisibility(isChecked);
                    MonthlyReadingReminderScheduler.rescheduleCurrentAsync(requireContext());
                }
            });
        }

        if (spinnerReadingNotificationFrequency != null) {
            spinnerReadingNotificationFrequency.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    if (suppressReadingNotificationCallbacks) {
                        return;
                    }
                    if (itemId > 0) {
                        setReadingNotificationFrequency(itemId, position);
                        updateReadingNotificationFields(position);
                        MonthlyReadingReminderScheduler.rescheduleCurrentAsync(requireContext());
                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                }
            });
        }

        if (spinnerDayOfWeek != null) {
            spinnerDayOfWeek.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    if (suppressReadingNotificationCallbacks) {
                        return;
                    }
                    if (itemId > 0) {
                        setReadingNotificationDayOfWeek(itemId, position);
                        MonthlyReadingReminderScheduler.rescheduleCurrentAsync(requireContext());
                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                }
            });
        }

        if (etDayOfMonth != null) {
            etDayOfMonth.addTextChangedListener(new TextWatcher() {
                private boolean selfChange;

                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                }

                @Override
                public void afterTextChanged(Editable s) {
                    if (selfChange || suppressReadingNotificationCallbacks || itemId <= 0) {
                        return;
                    }
                    String currentText = s.toString();
                    String normalizedText = currentText.isEmpty() ? "" : clampDayOfMonth(currentText);
                    if (!normalizedText.equals(currentText)) {
                        selfChange = true;
                        etDayOfMonth.setText(normalizedText);
                        etDayOfMonth.setSelection(normalizedText.length());
                        selfChange = false;
                        return;
                    }
                    if (!normalizedText.isEmpty()) {
                        setReadingNotificationDayOfMonth(itemId, normalizedText);
                        MonthlyReadingReminderScheduler.rescheduleCurrentAsync(requireContext());
                    }
                }
            });

            etDayOfMonth.setOnFocusChangeListener((v, hasFocus) -> {
                if (hasFocus || suppressReadingNotificationCallbacks || itemId <= 0) {
                    return;
                }
                String dayText = clampDayOfMonth(etDayOfMonth.getText().toString());
                etDayOfMonth.setText(dayText);
                setReadingNotificationDayOfMonth(itemId, dayText);
                MonthlyReadingReminderScheduler.rescheduleCurrentAsync(requireContext());
            });
        }

        if (tvNotificationTime != null) {
            tvNotificationTime.setOnClickListener(v -> {
                if (itemId > 0) {
                    showNotificationTimePicker();
                }
            });
        }

        // posluchač na odstranění odběrného místa
        requireActivity().getSupportFragmentManager().setFragmentResultListener(FLAG_DELETE_SUBSCRIPTION_POINT, this,
                (requestKey, result) -> {
                    if (result.getBoolean(YesNoDialogFragment.RESULT)) {
                        deleteItemSubscriptionPoint();
                    }
                });

        // posluchač na změnu odběrného místa
        requireActivity().getSupportFragmentManager().setFragmentResultListener("invoiceDialogFragment", this,
                (requestKey, result) -> onResume()
        );

        // posluchač na zavření dialogového okna s nastavením
        requireActivity().getSupportFragmentManager().setFragmentResultListener(SettingsFragment.FLAG_UPDATE_SETTINGS_FOR_FRAGMENT, this,
                (requestKey, result) -> updateAddControlsVisibility()
        );
    }

    @Override
    public void onResume() {
        super.onResume();
        ShPSubscriptionPoint shp = new ShPSubscriptionPoint(getActivity());
        DataSubscriptionPointSource dataSubscriptionPointSource = new DataSubscriptionPointSource(getActivity());
        dataSubscriptionPointSource.open();
        ArrayList<SubscriptionPointModel> subscriptionPoints = dataSubscriptionPointSource.loadSubscriptionPoints();
        dataSubscriptionPointSource.close();
        MySpinnerSubscriptionPointAdapter adapter = new MySpinnerSubscriptionPointAdapter(requireActivity(), android.R.layout.simple_list_item_1, subscriptionPoints);
        spSubscriptionPoint.setAdapter(adapter);
        if (spSubscriptionPointNotification != null) {
            spSubscriptionPointNotification.setAdapter(adapter);
        }
        setupSubscriptionPointSpinnerListeners(subscriptionPoints);

        if (!subscriptionPoints.isEmpty()) {
            int selectedIndex = resolveSubscriptionPointIndex(subscriptionPoints, shp.get(ShPSubscriptionPoint.ID_SUBSCRIPTION_POINT_LONG, 0L));
            setSubscriptionPointSelection(selectedIndex);
            applySubscriptionPointSelection(subscriptionPoints, selectedIndex);
            hideAlert(false);
        } else {
            selectedSubscriptionPoint = null;
            itemId = 0L;
            hideAlert(true);
        }
        applySectionVisibility();
        updateAddControlsVisibility();
    }

    private void setupSubscriptionPointSpinnerListeners(ArrayList<SubscriptionPointModel> subscriptionPoints) {
        AdapterView.OnItemSelectedListener listener = new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (suppressSubscriptionPointSpinnerCallbacks) {
                    return;
                }
                if (position < 0 || position >= subscriptionPoints.size()) {
                    return;
                }
                if (hasMultipleSubscriptionPointSpinners()) {
                    syncSubscriptionPointSpinners(position);
                }
                applySubscriptionPointSelection(subscriptionPoints, position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        };
        spSubscriptionPoint.setOnItemSelectedListener(listener);
        if (spSubscriptionPointNotification != null) {
            spSubscriptionPointNotification.setOnItemSelectedListener(listener);
        }
    }

    private int resolveSubscriptionPointIndex(ArrayList<SubscriptionPointModel> subscriptionPoints, long selectedId) {
        for (int i = 0; i < subscriptionPoints.size(); i++) {
            if (subscriptionPoints.get(i).getId() == selectedId) {
                return i;
            }
        }
        return 0;
    }

    private void setSubscriptionPointSelection(int position) {
        suppressSubscriptionPointSpinnerCallbacks = true;
        try {
            spSubscriptionPoint.setSelection(position);
            if (hasMultipleSubscriptionPointSpinners()) {
                spSubscriptionPointNotification.setSelection(position);
            }
        } finally {
            suppressSubscriptionPointSpinnerCallbacks = false;
        }
    }

    private void syncSubscriptionPointSpinners(int position) {
        if (!hasMultipleSubscriptionPointSpinners()) {
            return;
        }
        suppressSubscriptionPointSpinnerCallbacks = true;
        try {
            if (spSubscriptionPoint.getSelectedItemPosition() != position) {
                spSubscriptionPoint.setSelection(position);
            }
            if (spSubscriptionPointNotification.getSelectedItemPosition() != position) {
                spSubscriptionPointNotification.setSelection(position);
            }
        } finally {
            suppressSubscriptionPointSpinnerCallbacks = false;
        }
    }

    private boolean hasMultipleSubscriptionPointSpinners() {
        return spSubscriptionPointNotification != null;
    }

    private void applySubscriptionPointSelection(ArrayList<SubscriptionPointModel> subscriptionPoints, int position) {
        SubscriptionPointModel subscriptionPointModel = subscriptionPoints.get(position);
        itemId = subscriptionPointModel.getId();

        ShPSubscriptionPoint shp = new ShPSubscriptionPoint(getActivity());
        shp.set(ShPSubscriptionPoint.ID_SUBSCRIPTION_POINT_LONG, subscriptionPointModel.getId());

        selectedSubscriptionPoint = subscriptionPointModel;

        tvDescription.setText(subscriptionPointModel.getDescription());
        SpannableStringBuilder builderPhaze = new SpannableStringBuilder();
        builderPhaze.append(String.valueOf(subscriptionPointModel.getCountPhaze()));
        builderPhaze.append(" x ");
        builderPhaze.append(String.valueOf(subscriptionPointModel.getPhaze()));
        builderPhaze.append(" A");
        tvPhaze.setText(builderPhaze);

        tvNumberElectricMeter.setText(subscriptionPointModel.getNumberElectricMeter());
        tvNumberSubscriptionPoint.setText(subscriptionPointModel.getNumberSubscriptionPoint());

        bindReadingNotificationSettings(subscriptionPointModel);

        if (graphAnnualOverview != null) {
            AnnualOverviewDataBuilder builder = new AnnualOverviewDataBuilder(requireContext());
            ArrayList<AnnualYearData> annualData = builder.buildAnnualData(subscriptionPointModel);
            graphAnnualOverview.setData(annualData);
        }
    }

    /**
     * Otevře okno pro přidání odběrného místa
     */
    private void addSubcsriptionPoint() {
        FragmentChange.replace(requireActivity(), SubscriptionPointAddFragment.newInstance(), FragmentChange.Transaction.MOVE, true);
    }

    /**
     * Otevře okno pro úpravu odběrného místa
     */
    private void edit() {
        FragmentChange.replace(requireActivity(), SubscriptionPointEditFragment.newInstance(itemId), FragmentChange.Transaction.MOVE, true);
    }

    /**
     * Zobrazí výzvu pro vytvoření odběrného místa v případě, že žádné neexistuje
     *
     * @param show true - zobrazí výzvu, false - skryje výzvu
     */
    private void hideAlert(boolean show) {
        if (show) {
            selectedSubscriptionPoint = null;
            itemId = 0L;
            if (tabLayout != null) {
                tabLayout.setVisibility(GONE);
            }
            if (layoutSubscriptionPointManagement != null) {
                layoutSubscriptionPointManagement.setVisibility(GONE);
            }
            if (graphAnnualOverview != null) {
                graphAnnualOverview.setVisibility(GONE);
            }
            if (btnFullscreenGraph != null) {
                btnFullscreenGraph.setVisibility(GONE);
            }
            if (lnSpinner != null) lnSpinner.setVisibility(GONE);
            if (spSubscriptionPoint != null) spSubscriptionPoint.setVisibility(GONE);
            if (spSubscriptionPointNotification != null)
                spSubscriptionPointNotification.setVisibility(GONE);
            if (lnDescription != null) lnDescription.setVisibility(GONE);
            if (lnPhaze != null) lnPhaze.setVisibility(GONE);
            if (lnNumberElectricMeter != null) lnNumberElectricMeter.setVisibility(GONE);
            if (lnNumberSubscriptionPoint != null) lnNumberSubscriptionPoint.setVisibility(GONE);
            if (lnReadingNotification != null) {
                lnReadingNotification.setVisibility(GONE);
            }
            tvNewSubscriptionPoint.setVisibility(VISIBLE);
        } else {
            if (tabLayout != null) {
                tabLayout.setVisibility(VISIBLE);
            }
            if (lnSpinner != null) lnSpinner.setVisibility(VISIBLE);
            if (spSubscriptionPoint != null) spSubscriptionPoint.setVisibility(VISIBLE);
            if (spSubscriptionPointNotification != null)
                spSubscriptionPointNotification.setVisibility(VISIBLE);
            if (lnDescription != null) lnDescription.setVisibility(VISIBLE);
            if (lnPhaze != null) lnPhaze.setVisibility(VISIBLE);
            if (lnNumberElectricMeter != null) lnNumberElectricMeter.setVisibility(VISIBLE);
            if (lnNumberSubscriptionPoint != null) lnNumberSubscriptionPoint.setVisibility(VISIBLE);
            if (btnFullscreenGraph != null) btnFullscreenGraph.setVisibility(VISIBLE);
            tvNewSubscriptionPoint.setVisibility(GONE);
            applySectionVisibility();
        }
    }

    private boolean isLandscape() {
        return getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;
    }

    private void setupTabs() {
        if (tabLayout == null) {
            return;
        }

        if (layoutSubscriptionPointManagement == null || lnReadingNotification == null) {
            tabLayout.setVisibility(GONE);
            return;
        }

        tabLayout.removeAllTabs();
        if (isLandscape()) {
            tabLayout.addTab(tabLayout.newTab().setText(R.string.subscription_point_tab_graph), true);
            tabLayout.addTab(tabLayout.newTab().setText(R.string.subscription_point_tab_notifications));
        } else {
            tabLayout.addTab(tabLayout.newTab().setText(R.string.subscription_point_tab_management), true);
            tabLayout.addTab(tabLayout.newTab().setText(R.string.subscription_point_tab_notifications));
        }

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if (tab == null) {
                    return;
                }
                if (tab.getPosition() == 0) {
                    showManagementSection();
                } else {
                    showNotificationSection();
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
            }
        });
        applySectionVisibility();
    }

    private void showManagementSection() {
        if (selectedSubscriptionPoint == null || itemId <= 0) {
            hideAlert(true);
            return;
        }

        if (layoutSubscriptionPointManagement != null) {
            layoutSubscriptionPointManagement.setVisibility(VISIBLE);
        }
        if (graphAnnualOverview != null) {
            graphAnnualOverview.setVisibility(VISIBLE);
        }
        if (btnFullscreenGraph != null) {
            btnFullscreenGraph.setVisibility(VISIBLE);
        }
        if (lnReadingNotification != null) {
            lnReadingNotification.setVisibility(GONE);
        }
        updateAddControlsVisibility();
    }

    private void showNotificationSection() {
        if (selectedSubscriptionPoint == null || itemId <= 0) {
            hideAlert(true);
            return;
        }

        if (isLandscape()) {
            if (layoutSubscriptionPointManagement != null) {
                layoutSubscriptionPointManagement.setVisibility(VISIBLE);
            }
            if (graphAnnualOverview != null) {
                graphAnnualOverview.setVisibility(GONE);
            }
            if (btnFullscreenGraph != null) {
                btnFullscreenGraph.setVisibility(GONE);
            }
        } else {
            if (layoutSubscriptionPointManagement != null) {
                layoutSubscriptionPointManagement.setVisibility(GONE);
            }
        }
        if (lnReadingNotification != null) {
            lnReadingNotification.setVisibility(VISIBLE);
        }
        if (btnAddSubscriptionPoint != null) {
            btnAddSubscriptionPoint.setVisibility(GONE);
        }
        if (fab != null) {
            fab.setVisibility(GONE);
        }
    }

    private void updateAddControlsVisibility() {
        if (tabLayout != null && tabLayout.getVisibility() == VISIBLE && tabLayout.getSelectedTabPosition() == 1) {
            if (btnAddSubscriptionPoint != null) {
                btnAddSubscriptionPoint.setVisibility(GONE);
            }
            if (fab != null) {
                fab.setVisibility(GONE);
            }
            return;
        }
        UIHelper.showButtons(btnAddSubscriptionPoint, fab, requireActivity(), sc, false);
    }

    private boolean isTabbedMode() {
        return tabLayout != null && tabLayout.getVisibility() == VISIBLE && tabLayout.getTabCount() >= 2;
    }

    private void applySectionVisibility() {
        if (layoutSubscriptionPointManagement == null || lnReadingNotification == null) {
            return;
        }

        if (selectedSubscriptionPoint == null || itemId <= 0) {
            hideAlert(true);
            return;
        }

        if (!isTabbedMode()) {
            layoutSubscriptionPointManagement.setVisibility(VISIBLE);
            lnReadingNotification.setVisibility(VISIBLE);
            if (graphAnnualOverview != null) graphAnnualOverview.setVisibility(VISIBLE);
            if (btnFullscreenGraph != null) btnFullscreenGraph.setVisibility(VISIBLE);
            updateAddControlsVisibility();
            return;
        }

        int selectedTab = tabLayout.getSelectedTabPosition();
        if (selectedTab == 1) {
            showNotificationSection();
        } else {
            showManagementSection();
        }
    }

    private void bindReadingNotificationSettings(SubscriptionPointModel subscriptionPoint) {
        if (lnReadingNotification == null || subscriptionPoint == null) {
            return;
        }

        long subscriptionPointId = subscriptionPoint.getId();

        suppressReadingNotificationCallbacks = true;
        try {
            boolean enabled = loadReadingNotificationEnabled(subscriptionPointId);
            chReadingNotificationEnabled.setChecked(enabled);

            int frequency = loadReadingNotificationFrequency(subscriptionPointId);
            spinnerReadingNotificationFrequency.setSelection(frequency);

            updateReadingNotificationVisibility(enabled);
            updateReadingNotificationFields(frequency);

            String notificationTime = loadReadingNotificationTime(subscriptionPointId);
            tvNotificationTime.setText(notificationTime);

            String dayOfMonth = loadReadingNotificationDayOfMonth(subscriptionPointId);
            etDayOfMonth.setText(dayOfMonth);

            int dayOfWeek = loadReadingNotificationDayOfWeek(subscriptionPointId);
            spinnerDayOfWeek.setSelection(dayOfWeek);
        } finally {
            suppressReadingNotificationCallbacks = false;
        }
    }

    private void updateReadingNotificationVisibility(boolean enabled) {
        if (chReadingNotificationEnabled == null) {
            return;
        }
        chReadingNotificationEnabled.setChecked(enabled);
        if (spinnerReadingNotificationFrequency != null) {
            spinnerReadingNotificationFrequency.setVisibility(enabled ? VISIBLE : GONE);
        }
        if (enabled) {
            int frequency = spinnerReadingNotificationFrequency != null ? spinnerReadingNotificationFrequency.getSelectedItemPosition() : FREQUENCY_MONTHLY;
            updateReadingNotificationFields(frequency);
        } else {
            if (layoutNotificationTime != null) {
                layoutNotificationTime.setVisibility(GONE);
            }
            if (layoutMonthlyDay != null) {
                layoutMonthlyDay.setVisibility(GONE);
            }
            if (layoutWeeklyDay != null) {
                layoutWeeklyDay.setVisibility(GONE);
            }
        }
    }

    private void updateReadingNotificationFields(int frequency) {
        if (chReadingNotificationEnabled == null || !chReadingNotificationEnabled.isChecked()) {
            return;
        }

        if (layoutNotificationTime != null) {
            layoutNotificationTime.setVisibility(VISIBLE);
        }

        if (frequency == FREQUENCY_WEEKLY) {
            if (layoutMonthlyDay != null) {
                layoutMonthlyDay.setVisibility(GONE);
            }
            if (layoutWeeklyDay != null) {
                layoutWeeklyDay.setVisibility(VISIBLE);
            }
        } else {
            if (layoutMonthlyDay != null) {
                layoutMonthlyDay.setVisibility(VISIBLE);
            }
            if (layoutWeeklyDay != null) {
                layoutWeeklyDay.setVisibility(GONE);
            }
        }
    }

    private void showNotificationTimePicker() {
        if (itemId <= 0 || tvNotificationTime == null) {
            return;
        }
        String currentTime = tvNotificationTime.getText().toString();
        int hour = 9;
        int minute = 0;
        String[] parts = currentTime.split(":");
        if (parts.length == 2) {
            try {
                hour = Integer.parseInt(parts[0]);
                minute = Integer.parseInt(parts[1]);
            } catch (NumberFormatException ignored) {
            }
        }

        TimePickerDialog dialog = new TimePickerDialog(
                requireContext(),
                (view, selectedHour, selectedMinute) -> {
                    String timeString = String.format(Locale.getDefault(), "%02d:%02d", selectedHour, selectedMinute);
                    tvNotificationTime.setText(timeString);
                    setReadingNotificationTime(itemId, timeString);
                    MonthlyReadingReminderScheduler.rescheduleCurrentAsync(requireContext());
                },
                hour,
                minute,
                true
        );
        dialog.show();
    }

    private SharedPreferences getNotificationPrefs() {
        return requireContext().getSharedPreferences("subscription_point_notifications", Context.MODE_PRIVATE);
    }

    private boolean loadReadingNotificationEnabled(long id) {
        return getNotificationPrefs().getBoolean(PREF_READING_NOTIFICATION_ENABLED + "_" + id, false);
    }

    private void setReadingNotificationEnabled(long id, boolean enabled) {
        getNotificationPrefs().edit().putBoolean(PREF_READING_NOTIFICATION_ENABLED + "_" + id, enabled).apply();
    }

    private int loadReadingNotificationFrequency(long id) {
        return getNotificationPrefs().getInt(PREF_READING_NOTIFICATION_FREQUENCY + "_" + id, FREQUENCY_MONTHLY);
    }

    private void setReadingNotificationFrequency(long id, int frequency) {
        getNotificationPrefs().edit().putInt(PREF_READING_NOTIFICATION_FREQUENCY + "_" + id, frequency).apply();
    }

    private String loadReadingNotificationTime(long id) {
        return getNotificationPrefs().getString(PREF_READING_NOTIFICATION_TIME + "_" + id, TIME_DEFAULT);
    }

    private void setReadingNotificationTime(long id, String time) {
        getNotificationPrefs().edit().putString(PREF_READING_NOTIFICATION_TIME + "_" + id, time).apply();
    }

    private String loadReadingNotificationDayOfMonth(long id) {
        return getNotificationPrefs().getString(PREF_READING_NOTIFICATION_DAY_OF_MONTH + "_" + id, DAY_OF_MONTH_DEFAULT);
    }

    private void setReadingNotificationDayOfMonth(long id, String dayOfMonth) {
        getNotificationPrefs().edit().putString(PREF_READING_NOTIFICATION_DAY_OF_MONTH + "_" + id, dayOfMonth).apply();
    }

    private int loadReadingNotificationDayOfWeek(long id) {
        return getNotificationPrefs().getInt(PREF_READING_NOTIFICATION_DAY_OF_WEEK + "_" + id, Calendar.MONDAY);
    }

    private void setReadingNotificationDayOfWeek(long id, int dayOfWeek) {
        getNotificationPrefs().edit().putInt(PREF_READING_NOTIFICATION_DAY_OF_WEEK + "_" + id, dayOfWeek).apply();
    }

    private String clampDayOfMonth(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return String.valueOf(DAY_OF_MONTH_MIN);
        }
        try {
            int val = Integer.parseInt(raw.trim());
            if (val < DAY_OF_MONTH_MIN) val = DAY_OF_MONTH_MIN;
            if (val > DAY_OF_MONTH_MAX) val = DAY_OF_MONTH_MAX;
            return String.valueOf(val);
        } catch (NumberFormatException e) {
            return String.valueOf(DAY_OF_MONTH_MIN);
        }
    }

    /**
     * Smaže vybrané odběrné místo
     */
    private void deleteItemSubscriptionPoint() {
        if (selectedSubscriptionPoint == null) {
            return;
        }
        DataSubscriptionPointSource dataSubscriptionPointSource = new DataSubscriptionPointSource(getActivity());
        dataSubscriptionPointSource.open();
        dataSubscriptionPointSource.deleteSubscriptionPoint(itemId, selectedSubscriptionPoint.getMilins());
        dataSubscriptionPointSource.close();
        onResume();
    }

    /**
     * Zobrazí dialogové okno pro smazání odběrného místa
     */
    private void showDeleteDialog() {
        YesNoDialogFragment.newInstance(getString(R.string.smazat_odberne_misto2), FLAG_DELETE_SUBSCRIPTION_POINT).show(requireActivity().getSupportFragmentManager(), "yesNoDialog");
    }

}
