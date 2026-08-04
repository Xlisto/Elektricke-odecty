package cz.xlisto.elektrodroid.modules.subscriptionpoint;


import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static cz.xlisto.elektrodroid.utils.FragmentChange.Transaction.MOVE;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;

import android.app.TimePickerDialog;
import android.text.Editable;
import android.text.InputFilter;
import android.text.Spanned;
import android.text.TextWatcher;
import android.text.format.DateFormat;

import java.util.Locale;

import java.util.ArrayList;

import cz.xlisto.elektrodroid.R;
import cz.xlisto.elektrodroid.databaze.DataSettingsSource;
import cz.xlisto.elektrodroid.databaze.DataSubscriptionPointSource;
import cz.xlisto.elektrodroid.modules.settings.SettingsFragment;
import cz.xlisto.elektrodroid.dialogs.SubscriptionPointDialogFragment;
import cz.xlisto.elektrodroid.dialogs.YesNoDialogFragment;
import cz.xlisto.elektrodroid.models.SubscriptionPointModel;
import cz.xlisto.elektrodroid.shp.ShPDashBoard;
import cz.xlisto.elektrodroid.shp.ShPSubscriptionPoint;
import cz.xlisto.elektrodroid.services.MonthlyReadingReminderScheduler;
import cz.xlisto.elektrodroid.utils.FragmentChange;
import cz.xlisto.elektrodroid.utils.MainActivityHelper;
import cz.xlisto.elektrodroid.utils.SubscriptionPoint;
import cz.xlisto.elektrodroid.utils.UIHelper;


/**
 * Fragment pro správu a zobrazení odběrných míst.
 *
 * <p>Tento fragment zobrazuje seznam dostupných odběrných míst ve spinneru a podrobné
 * informace o aktuálně vybraném místě (popis, počet fází, číslo elektroměru atd.).</p>
 *
 * <p>Klíčové funkce:</p>
 * <ul>
 *   <li><strong>Výběr místa</strong> - Spinner pro přepnutí mezi dostupnými místy</li>
 *   <li><strong>Persistence výběru</strong> - Automaticky ukládá vybrané místo do SharedPreferences a databáze</li>
 *   <li><strong>Správa míst</strong> - Tlačítka pro úpravu nebo smazání současného místa</li>
 *   <li><strong>Vytvoření nového místa</strong> - FloatingActionButton a tlačítko pro přidání nového místa</li>
 *   <li><strong>Zobrazení stavu</strong> - Pokud nejsou místa dostupná, zobrazí výzvu k vytvoření</li>
 * </ul>
 * </p>
 *
 * <p>Integrace s feature:</p>
 * <ul>
 *   <li>Při změně výběru ve spinneru se volá </li>
 *   <li>Příjímá aktualizace z {@link SubscriptionPointDialogFragment} skrz fragment result listener</li>
 * </ul>
 * </p>
 *
 * @see SubscriptionPointDialogFragment
 * @see cz.xlisto.elektrodroid.utils.SubscriptionPoint
 */
public class SubscriptionPointFragment extends Fragment {

    private static final String TAG = "SubscriptionPointFragment";
    private static final String FLAG_DELETE_SUBSCRIPTION_POINT = "flagDeleteSubscriptionPoint";
    private static final int DAY_OF_MONTH_MIN = 1;
    private static final int DAY_OF_MONTH_MAX = 31;
    private Spinner spSubscriptionPoint, spSubscriptionPointNotification;
    private TextView tvDescription, tvPhaze, tvNumberElectricMeter, tvNumberSubscriptionPoint, tvNewSubscriptionPoint;
    private LinearLayout lnSpinner, lnDescription, lnPhaze, lnNumberElectricMeter, lnNumberSubscriptionPoint;
    private LinearLayout layoutSubscriptionPointManagement;
    private LinearLayout lnReadingNotification, layoutMonthlyDay, layoutWeeklyDay, layoutNotificationTime;
    private CheckBox chReadingNotificationEnabled;
    private Spinner spinnerReadingNotificationFrequency, spinnerDayOfWeek;
    private EditText etDayOfMonth;
    private TextView tvNotificationTime;
    private Button btnEdit, btnDelete, btnAddSubscriptionPoint;
    private FloatingActionButton fab;
    private ScrollView sc;
    private TabLayout tabLayout;
    private long itemId, milins;
    private boolean suppressReadingNotificationCallbacks;
    private boolean suppressSubscriptionPointSpinnerCallbacks;
    //TODO: Doplnit další detaily o počtu údaju odběrného místa a ukládat id odběrného místa do sharedprefences


    public SubscriptionPointFragment() {
    }


    /**
     * Fragment zobrazení odběrných míst
     *
     * @return Nová instance fragmentu SubscriptionPointFragment.
     */
    public static SubscriptionPointFragment newInstance() {
        return new SubscriptionPointFragment();
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
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
        btnEdit = view.findViewById(R.id.btnEdit);
        btnDelete = view.findViewById(R.id.btnDelete);
        btnAddSubscriptionPoint = view.findViewById(R.id.btnAddSubscriptionPoint);
        layoutSubscriptionPointManagement = view.findViewById(R.id.layoutSubscriptionPointManagement);
        lnSpinner = view.findViewById(R.id.lnSpinner);
        lnDescription = view.findViewById(R.id.lnDescription);
        lnPhaze = view.findViewById(R.id.lnPhaze);
        lnNumberElectricMeter = view.findViewById(R.id.lnNumberElectrometer);
        lnNumberSubscriptionPoint = view.findViewById(R.id.lnNumberSubscriptionPoint);
        sc = view.findViewById(R.id.scrollView);
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
        fab.setOnClickListener(v -> addSubcsriptionPoint());
        btnAddSubscriptionPoint.setOnClickListener(v -> addSubcsriptionPoint());
        btnEdit.setOnClickListener(v -> edit());
        btnDelete.setOnClickListener(v -> showDeleteDialog());

        setupTabs();

        if (etDayOfMonth != null) {
            etDayOfMonth.setFilters(new InputFilter[]{new DayOfMonthInputFilter(DAY_OF_MONTH_MIN, DAY_OF_MONTH_MAX)});
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

        //posluchač na odstranění odběrného místa
        requireActivity().getSupportFragmentManager().setFragmentResultListener(FLAG_DELETE_SUBSCRIPTION_POINT, this,
                (requestKey, result) -> {
                    if (result.getBoolean(YesNoDialogFragment.RESULT)) {
                        deleteItemSubscriptionPoint();
                    }
                });

        //posluchač na změnu odběrného místa
        requireActivity().getSupportFragmentManager().setFragmentResultListener(SubscriptionPointDialogFragment.FLAG_UPDATE_SUBSCRIPTION_POINT, this,
                (requestKey, result) -> onResume()
        );

        //posluchač na zavření dialogového okna s nastavením
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
        return spSubscriptionPoint != null
                && spSubscriptionPointNotification != null
                && spSubscriptionPoint != spSubscriptionPointNotification;
    }


    private void applySubscriptionPointSelection(ArrayList<SubscriptionPointModel> subscriptionPoints, int position) {
        SubscriptionPointModel selectedSubscriptionPoint = subscriptionPoints.get(position);
        setText(selectedSubscriptionPoint);
        SubscriptionPoint.setCurrentSelection(requireContext(), selectedSubscriptionPoint.getId());
        MainActivityHelper.updateToolbarAndLoadData(requireActivity());
    }


    /**
     * Z objektu odběrného místa nastaví popisky do textview
     *
     * @param subscriptionPoint Objekt odběrného místa
     */
    private void setText(SubscriptionPointModel subscriptionPoint) {
        itemId = subscriptionPoint.getId();
        milins = subscriptionPoint.getMilins();
        tvDescription.setText(subscriptionPoint.getDescription());
        tvPhaze.setText(getResources().getString(R.string.power, subscriptionPoint.getCountPhaze(), subscriptionPoint.getPhaze()));
        tvNumberElectricMeter.setText(subscriptionPoint.getNumberElectricMeter());
        tvNumberSubscriptionPoint.setText(subscriptionPoint.getNumberSubscriptionPoint());
        bindReadingNotificationSettings(subscriptionPoint);
    }


    /**
     * Zobrazí fragment na editaci odběrného místa
     */
    private void edit() {
        FragmentChange.replace(requireActivity(), SubscriptionPointEditFragment.newInstance(itemId), MOVE, true);
    }


    /**
     * Zobrazí dialogové okno s dotazem na smazání
     */
    private void showDeleteDialog() {
        YesNoDialogFragment yesNoDialogFragment = YesNoDialogFragment.newInstance(getResources().getString(R.string.smazat_odberne_misto2), FLAG_DELETE_SUBSCRIPTION_POINT);
        yesNoDialogFragment.show(requireActivity().getSupportFragmentManager(), TAG);
    }


    /**
     * Smaže odběrné místo
     */
    private void deleteItemSubscriptionPoint() {
        DataSubscriptionPointSource dataSubscriptionPointSource = new DataSubscriptionPointSource(getActivity());
        dataSubscriptionPointSource.open();
        dataSubscriptionPointSource.deleteSubscriptionPoint(itemId, milins);
        dataSubscriptionPointSource.close();
        MonthlyReadingReminderScheduler.rescheduleCurrentAsync(requireContext());
        //nastavení prvního odběrného místa v Přehledu
        ShPDashBoard shp = new ShPDashBoard(requireContext());
        shp.set(ShPDashBoard.SHOW_INVOICE_SUM, 0);
        onResume();
    }


    /**
     * Zobrazí fragment pro přidání odběrného místa
     */
    private void addSubcsriptionPoint() {
        SubscriptionPointAddFragment subscriptionPointAddFragment = new SubscriptionPointAddFragment();
        FragmentChange.replace(requireActivity(), subscriptionPointAddFragment, MOVE, true);
    }


    /**
     * Při žádném odběrném místě zobrazí výzvu k založení nového místa
     *
     * @param show true - zobrazí výzvu, false - skryje výzvu
     */
    private void hideAlert(boolean show) {
        if (show) {
            if (tabLayout != null) {
                tabLayout.setVisibility(GONE);
            }
            if (layoutSubscriptionPointManagement != null) {
                layoutSubscriptionPointManagement.setVisibility(GONE);
            }
            lnSpinner.setVisibility(GONE);
            lnDescription.setVisibility(GONE);
            lnPhaze.setVisibility(GONE);
            lnNumberElectricMeter.setVisibility(GONE);
            lnNumberSubscriptionPoint.setVisibility(GONE);
            if (lnReadingNotification != null) {
                lnReadingNotification.setVisibility(GONE);
            }
            btnEdit.setVisibility(GONE);
            btnDelete.setVisibility(GONE);
            tvNewSubscriptionPoint.setVisibility(VISIBLE);
        } else {
            if (tabLayout != null) {
                tabLayout.setVisibility(VISIBLE);
            }
            tvNewSubscriptionPoint.setVisibility(View.GONE);
            applySectionVisibility();
        }
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
        tabLayout.addTab(tabLayout.newTab().setText(R.string.subscription_point_tab_management), true);
        tabLayout.addTab(tabLayout.newTab().setText(R.string.subscription_point_tab_notifications));
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
        if (layoutSubscriptionPointManagement != null) {
            layoutSubscriptionPointManagement.setVisibility(VISIBLE);
        }
        if (lnReadingNotification != null) {
            lnReadingNotification.setVisibility(GONE);
        }
        updateAddControlsVisibility();
    }


    private void showNotificationSection() {
        if (layoutSubscriptionPointManagement != null) {
            layoutSubscriptionPointManagement.setVisibility(GONE);
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

        if (!isTabbedMode()) {
            layoutSubscriptionPointManagement.setVisibility(VISIBLE);
            lnReadingNotification.setVisibility(VISIBLE);
            if (btnEdit != null) {
                btnEdit.setVisibility(VISIBLE);
            }
            if (btnDelete != null) {
                btnDelete.setVisibility(VISIBLE);
            }
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

            String dayOfMonth = clampDayOfMonth(loadReadingNotificationDayOfMonth(subscriptionPointId));
            etDayOfMonth.setText(dayOfMonth);

            int dayOfWeek = loadReadingNotificationDayOfWeek(subscriptionPointId);
            spinnerDayOfWeek.setSelection(dayOfWeek);

            tvNotificationTime.setText(loadReadingNotificationTime(subscriptionPointId));
            updateReadingNotificationVisibility(enabled);
            if (enabled) {
                updateReadingNotificationFields(frequency);
            }
        } finally {
            suppressReadingNotificationCallbacks = false;
        }
    }


    private void updateReadingNotificationVisibility(boolean enabled) {
        if (lnReadingNotification == null) {
            return;
        }
        spinnerReadingNotificationFrequency.setVisibility(enabled ? VISIBLE : GONE);
        layoutNotificationTime.setVisibility(enabled ? VISIBLE : GONE);
        if (!enabled) {
            layoutMonthlyDay.setVisibility(GONE);
            layoutWeeklyDay.setVisibility(GONE);
        } else {
            updateReadingNotificationFields(spinnerReadingNotificationFrequency.getSelectedItemPosition());
        }
    }


    private void updateReadingNotificationFields(int frequencyIndex) {
        if (layoutMonthlyDay == null || layoutWeeklyDay == null) {
            return;
        }
        if (frequencyIndex == 0) {
            layoutMonthlyDay.setVisibility(VISIBLE);
            layoutWeeklyDay.setVisibility(GONE);
        } else if (frequencyIndex == 1) {
            layoutMonthlyDay.setVisibility(GONE);
            layoutWeeklyDay.setVisibility(VISIBLE);
        }
    }


    private void showNotificationTimePicker() {
        int[] time = parseTime(tvNotificationTime.getText().toString());
        TimePickerDialog dialog = new TimePickerDialog(
                requireContext(),
                (TimePicker view, int hourOfDay, int minute) -> {
                    String formattedTime = String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minute);
                    tvNotificationTime.setText(formattedTime);
                    setReadingNotificationTime(itemId, formattedTime);
                    MonthlyReadingReminderScheduler.rescheduleCurrentAsync(requireContext());
                },
                time[0],
                time[1],
                DateFormat.is24HourFormat(requireContext())
        );
        dialog.show();
    }


    private int[] parseTime(String timeText) {
        int hour = 8;
        int minute = 0;
        if (timeText != null) {
            String[] parts = timeText.trim().split(":");
            if (parts.length == 2) {
                try {
                    hour = Integer.parseInt(parts[0]);
                    minute = Integer.parseInt(parts[1]);
                } catch (NumberFormatException ignored) {
                    hour = 8;
                }
            }
        }
        return new int[]{hour, minute};
    }


    private String clampDayOfMonth(String dayText) {
        if (dayText == null || dayText.trim().isEmpty()) {
            return String.valueOf(DAY_OF_MONTH_MIN);
        }
        try {
            int value = Integer.parseInt(dayText.trim());
            if (value < DAY_OF_MONTH_MIN) {
                return String.valueOf(DAY_OF_MONTH_MIN);
            }
            if (value > DAY_OF_MONTH_MAX) {
                return String.valueOf(DAY_OF_MONTH_MAX);
            }
            return String.valueOf(value);
        } catch (NumberFormatException e) {
            return String.valueOf(DAY_OF_MONTH_MIN);
        }
    }


    private boolean loadReadingNotificationEnabled(long subscriptionPointId) {
        DataSettingsSource settingsSource = new DataSettingsSource(requireContext());
        settingsSource.open();
        try {
            return settingsSource.loadReadingNotificationEnabled(subscriptionPointId);
        } finally {
            settingsSource.close();
        }
    }


    private int loadReadingNotificationFrequency(long subscriptionPointId) {
        DataSettingsSource settingsSource = new DataSettingsSource(requireContext());
        settingsSource.open();
        try {
            return settingsSource.loadReadingNotificationFrequency(subscriptionPointId);
        } finally {
            settingsSource.close();
        }
    }


    private String loadReadingNotificationDayOfMonth(long subscriptionPointId) {
        DataSettingsSource settingsSource = new DataSettingsSource(requireContext());
        settingsSource.open();
        try {
            return settingsSource.loadReadingNotificationDayOfMonth(subscriptionPointId);
        } finally {
            settingsSource.close();
        }
    }


    private int loadReadingNotificationDayOfWeek(long subscriptionPointId) {
        DataSettingsSource settingsSource = new DataSettingsSource(requireContext());
        settingsSource.open();
        try {
            return settingsSource.loadReadingNotificationDayOfWeek(subscriptionPointId);
        } finally {
            settingsSource.close();
        }
    }


    private String loadReadingNotificationTime(long subscriptionPointId) {
        DataSettingsSource settingsSource = new DataSettingsSource(requireContext());
        settingsSource.open();
        try {
            return settingsSource.loadReadingNotificationTime(subscriptionPointId);
        } finally {
            settingsSource.close();
        }
    }


    private void setReadingNotificationEnabled(long subscriptionPointId, boolean enabled) {
        DataSettingsSource settingsSource = new DataSettingsSource(requireContext());
        settingsSource.open();
        try {
            settingsSource.setReadingNotificationEnabled(subscriptionPointId, enabled);
        } finally {
            settingsSource.close();
        }
    }


    private void setReadingNotificationFrequency(long subscriptionPointId, int frequency) {
        DataSettingsSource settingsSource = new DataSettingsSource(requireContext());
        settingsSource.open();
        try {
            settingsSource.setReadingNotificationFrequency(subscriptionPointId, frequency);
        } finally {
            settingsSource.close();
        }
    }


    private void setReadingNotificationDayOfMonth(long subscriptionPointId, String dayOfMonth) {
        DataSettingsSource settingsSource = new DataSettingsSource(requireContext());
        settingsSource.open();
        try {
            settingsSource.setReadingNotificationDayOfMonth(subscriptionPointId, dayOfMonth);
        } finally {
            settingsSource.close();
        }
    }


    private void setReadingNotificationDayOfWeek(long subscriptionPointId, int dayOfWeek) {
        DataSettingsSource settingsSource = new DataSettingsSource(requireContext());
        settingsSource.open();
        try {
            settingsSource.setReadingNotificationDayOfWeek(subscriptionPointId, dayOfWeek);
        } finally {
            settingsSource.close();
        }
    }


    private void setReadingNotificationTime(long subscriptionPointId, String time) {
        DataSettingsSource settingsSource = new DataSettingsSource(requireContext());
        settingsSource.open();
        try {
            settingsSource.setReadingNotificationTime(subscriptionPointId, time);
        } finally {
            settingsSource.close();
        }
    }


    private record DayOfMonthInputFilter(int min, int max) implements InputFilter {

        @Override
        public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
            StringBuilder builder = new StringBuilder(dest);
            builder.replace(dstart, dend, source.subSequence(start, end).toString());
            String newValue = builder.toString();

            if (newValue.isEmpty()) {
                return null;
            }

            try {
                int input = Integer.parseInt(newValue);
                if (input >= min && input <= max) {
                    return null;
                }
            } catch (NumberFormatException ignored) {
            }
            return "";
        }

    }

}