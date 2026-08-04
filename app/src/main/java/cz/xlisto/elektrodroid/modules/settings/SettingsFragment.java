package cz.xlisto.elektrodroid.modules.settings;


import android.Manifest;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.app.AlarmManager;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.format.DateFormat;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.AdapterView;
import android.widget.TimePicker;
import android.text.TextWatcher;
import android.text.Spanned;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.SwitchCompat;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import cz.xlisto.elektrodroid.R;
import cz.xlisto.elektrodroid.services.HdoAlarmScheduler;
import cz.xlisto.elektrodroid.services.MonthlyReadingReminderScheduler;
import cz.xlisto.elektrodroid.shp.ShPSettings;

import java.util.Locale;


/**
 * Fragment pro změnu preferencí zobrazení aplikace.
 * <p>
 * Uživatel může zapnout/vypnout FAB, spodní navigaci a levou navigaci.
 * Po změně přepínače se hodnoty ihned uloží do {@link ShPSettings} a odešlou se výsledky
 * pro aktualizaci fragmentů i aktivity.
 */
public class SettingsFragment extends Fragment {

    private static final int DAY_OF_MONTH_MIN = 1;
    private static final int DAY_OF_MONTH_MAX = 31;

    private CheckBox chNotificationPermission;
    private CheckBox chExactAlarmPermission;
    private Boolean wasExactAlarmEnabled;

    private Spinner spinnerReadingNotificationFrequency;
    private LinearLayout layoutMonthlyDay;
    private LinearLayout layoutWeeklyDay;
    private LinearLayout layoutNotificationTime;
    private EditText etDayOfMonth;
    private TextView tvNotificationTime;

    public static final String TAG = "SettingsViewDialogFragment";
    public static final String FLAG_UPDATE_SETTINGS_FOR_FRAGMENT = "SettingsViewDialogFragment1";
    public static final String FLAG_UPDATE_SETTINGS_FOR_ACTIVITY = "SettingsViewDialogFragment2";

    /**
     * Vytvoří novou instanci dialogu nastavení zobrazení.
     *
     * @return instance fragmentu
     */
    public static SettingsFragment newInstance() {
        return new SettingsFragment();
    }


    /**
     * Vytvoří view fragmentu a naváže přepínače na uložené preference.
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings_view, container, false);
        chNotificationPermission = view.findViewById(R.id.chNotificationPermission);
        chExactAlarmPermission = view.findViewById(R.id.chExactAlarmPermission);
        CheckBox chUseSetAlarm = view.findViewById(R.id.chUseSetAlarm);
        SwitchCompat switchShowFab = view.findViewById(R.id.switchShowFab);
        SwitchCompat switchShowBottomNavigation = view.findViewById(R.id.switchShowBottomNavigation);
        SwitchCompat switchShowLeftNavigation = view.findViewById(R.id.switchShowLeftNavigation);
        SwitchCompat switchAllowMobileData = view.findViewById(R.id.switchAllowMobileData);
        final ShPSettings shPSettings = new ShPSettings(requireContext());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            chNotificationPermission.setVisibility(View.VISIBLE);
            syncNotificationPermissionState();
            chNotificationPermission.setOnClickListener(v -> {
                // Checkbox pouze zobrazuje stav oprávnění; změna probíhá v nastavení systému.
                syncNotificationPermissionState();
                openNotificationSettings();
            });
        } else {
            chNotificationPermission.setVisibility(View.GONE);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            chExactAlarmPermission.setVisibility(View.VISIBLE);
            syncExactAlarmPermissionState();
            chExactAlarmPermission.setOnClickListener(v -> {
                syncExactAlarmPermissionState();
                openExactAlarmSettings();
            });
        } else {
            chExactAlarmPermission.setVisibility(View.GONE);
        }

        switchShowFab.setChecked(shPSettings.get(ShPSettings.SHOW_FAB, true));
        switchShowBottomNavigation.setChecked(shPSettings.get(ShPSettings.SHOW_BOTTOM_NAVIGATION, true));
        switchShowLeftNavigation.setChecked(shPSettings.get(ShPSettings.SHOW_LEFT_NAVIGATION, true));
        switchAllowMobileData.setChecked(shPSettings.get(ShPSettings.ALLOW_MOBILE_DATA, true));
        chUseSetAlarm.setChecked(shPSettings.get(ShPSettings.USE_HDO_SET_ALARM, false));

        chUseSetAlarm.setOnCheckedChangeListener((buttonView, isChecked) -> {
            shPSettings.set(ShPSettings.USE_HDO_SET_ALARM, isChecked);
            rescheduleAllHdoAlarmsAsync();
        });

        switchShowFab.setOnCheckedChangeListener((buttonView, isChecked) -> {
            shPSettings.set(ShPSettings.SHOW_FAB, switchShowFab.isChecked());
            notifySettingsChanged();
        });

        switchShowBottomNavigation.setOnCheckedChangeListener((buttonView, isChecked) -> {
            shPSettings.set(ShPSettings.SHOW_BOTTOM_NAVIGATION, switchShowBottomNavigation.isChecked());
            notifySettingsChanged();
        });

        switchShowLeftNavigation.setOnCheckedChangeListener((buttonView, isChecked) -> {
            shPSettings.set(ShPSettings.SHOW_LEFT_NAVIGATION, switchShowLeftNavigation.isChecked());
            notifySettingsChanged();
        });

        switchAllowMobileData.setOnCheckedChangeListener((buttonView, isChecked) -> {
            shPSettings.set(ShPSettings.ALLOW_MOBILE_DATA, switchAllowMobileData.isChecked());
            notifySettingsChanged();
        });

        // ===== Inicializace notifikací pro zápis odečtu =====
        // Nové prvky pro notifikace zápisu odečtu
        CheckBox chReadingNotificationEnabled = view.findViewById(R.id.chReadingNotificationEnabled);
        spinnerReadingNotificationFrequency = view.findViewById(R.id.spinnerReadingNotificationFrequency);
        layoutMonthlyDay = view.findViewById(R.id.layoutMonthlyDay);
        layoutWeeklyDay = view.findViewById(R.id.layoutWeeklyDay);
        layoutNotificationTime = view.findViewById(R.id.layoutNotificationTime);
        etDayOfMonth = view.findViewById(R.id.etDayOfMonth);
        Spinner spinnerDayOfWeek = view.findViewById(R.id.spinnerDayOfWeek);
        tvNotificationTime = view.findViewById(R.id.tvNotificationTime);

        // Načtení uložených preferencí pro notifikace
        boolean readingNotificationEnabled = shPSettings.get(ShPSettings.READING_NOTIFICATION_ENABLED, false);
        chReadingNotificationEnabled.setChecked(readingNotificationEnabled);
        updateReadingNotificationVisibility(readingNotificationEnabled);

        int frequencyIndex = shPSettings.get(ShPSettings.READING_NOTIFICATION_FREQUENCY, 0);
        spinnerReadingNotificationFrequency.setSelection(frequencyIndex);

        String dayOfMonth = shPSettings.get(ShPSettings.READING_NOTIFICATION_DAY_OF_MONTH, "1");
        String normalizedDayOfMonth = clampDayOfMonth(dayOfMonth);
        etDayOfMonth.setFilters(new InputFilter[]{new DayOfMonthInputFilter(DAY_OF_MONTH_MIN, DAY_OF_MONTH_MAX)});
        etDayOfMonth.setText(normalizedDayOfMonth);
        if (!normalizedDayOfMonth.equals(dayOfMonth)) {
            shPSettings.set(ShPSettings.READING_NOTIFICATION_DAY_OF_MONTH, normalizedDayOfMonth);
        }

        int dayOfWeekIndex = shPSettings.get(ShPSettings.READING_NOTIFICATION_DAY_OF_WEEK, 0);
        spinnerDayOfWeek.setSelection(dayOfWeekIndex);

        String notificationTime = shPSettings.get(ShPSettings.READING_NOTIFICATION_TIME, "08:00");
        tvNotificationTime.setText(notificationTime);
        MonthlyReadingReminderScheduler.rescheduleCurrentAsync(requireContext());

        // Listeners pro notifikace
        chReadingNotificationEnabled.setOnCheckedChangeListener((buttonView, isChecked) -> {
            shPSettings.set(ShPSettings.READING_NOTIFICATION_ENABLED, isChecked);
            updateReadingNotificationVisibility(isChecked);
            MonthlyReadingReminderScheduler.rescheduleCurrentAsync(requireContext());
            notifySettingsChanged();
        });

        spinnerReadingNotificationFrequency.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                shPSettings.set(ShPSettings.READING_NOTIFICATION_FREQUENCY, position);
                updateReadingNotificationFields(position);
                MonthlyReadingReminderScheduler.rescheduleCurrentAsync(requireContext());
                notifySettingsChanged();
            }


            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        spinnerDayOfWeek.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                shPSettings.set(ShPSettings.READING_NOTIFICATION_DAY_OF_WEEK, position);
                MonthlyReadingReminderScheduler.rescheduleCurrentAsync(requireContext());
                notifySettingsChanged();
            }


            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

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
                if (selfChange) {
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
                    shPSettings.set(ShPSettings.READING_NOTIFICATION_DAY_OF_MONTH, normalizedText);
                    MonthlyReadingReminderScheduler.rescheduleCurrentAsync(requireContext());
                    notifySettingsChanged();
                }
            }
        });

        etDayOfMonth.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                String dayText = clampDayOfMonth(etDayOfMonth.getText().toString());
                etDayOfMonth.setText(dayText);
                shPSettings.set(ShPSettings.READING_NOTIFICATION_DAY_OF_MONTH, dayText);
                MonthlyReadingReminderScheduler.rescheduleCurrentAsync(requireContext());
                notifySettingsChanged();
            }
        });

        tvNotificationTime.setOnClickListener(v -> showNotificationTimePicker(shPSettings));

        return view;
    }


    /**
     * Po návratu do popředí aktualizuje stavy systémových oprávnění.
     */
    @Override
    public void onResume() {
        super.onResume();
        syncNotificationPermissionState();
        syncExactAlarmPermissionState();
    }


    /**
     * Odešle notifikaci, že se změnilo nastavení zobrazení.
     */
    private void notifySettingsChanged() {
        getParentFragmentManager().setFragmentResult(FLAG_UPDATE_SETTINGS_FOR_FRAGMENT, new Bundle());
        getParentFragmentManager().setFragmentResult(FLAG_UPDATE_SETTINGS_FOR_ACTIVITY, new Bundle());
    }


    /**
     * Synchronizuje stav checkboxu podle skutečného stavu oprávnění notifikací.
     */
    private void syncNotificationPermissionState() {
        if (chNotificationPermission == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return;
        }
        boolean granted = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED;
        chNotificationPermission.setChecked(granted);
    }


    /**
     * Synchronizuje stav checkboxu podle dostupnosti přesných alarmů v systému.
     */
    private void syncExactAlarmPermissionState() {
        if (chExactAlarmPermission == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            return;
        }
        AlarmManager alarmManager = (AlarmManager) requireContext().getSystemService(android.content.Context.ALARM_SERVICE);
        boolean canSchedule = alarmManager != null && alarmManager.canScheduleExactAlarms();
        chExactAlarmPermission.setChecked(canSchedule);

        // Po povolení přesných alarmů znovu naplánujeme vše, aby další alarmy byly skutečně exact.
        if (Boolean.FALSE.equals(wasExactAlarmEnabled) && canSchedule) {
            rescheduleAllHdoAlarmsAsync();
        }
        wasExactAlarmEnabled = canSchedule;
    }


    /**
     * Spustí přeplánování HDO alarmů mimo UI vlákno.
     */
    private void rescheduleAllHdoAlarmsAsync() {
        android.content.Context appContext = requireContext().getApplicationContext();
        new Thread(() -> HdoAlarmScheduler.rescheduleAll(appContext), "hdo-alarm-reschedule").start();
    }


    /**
     * Otevře systémové nastavení notifikací pro aplikaci.
     */
    private void openNotificationSettings() {
        Intent intent;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            intent = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                    .putExtra(Settings.EXTRA_APP_PACKAGE, requireContext().getPackageName());
        } else {
            intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    .setData(android.net.Uri.fromParts("package", requireContext().getPackageName(), null));
        }
        startActivity(intent);
    }


    /**
     * Otevře systémovou obrazovku pro správu oprávnění přesných alarmů.
     */
    private void openExactAlarmSettings() {
        Intent intent;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                    .setData(android.net.Uri.fromParts("package", requireContext().getPackageName(), null));
        } else {
            intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    .setData(android.net.Uri.fromParts("package", requireContext().getPackageName(), null));
        }
        startActivity(intent);
    }


    /**
     * Aktualizuje viditelnost prvků pro notifikace zápisu odečtu.
     */
    private void updateReadingNotificationVisibility(boolean enabled) {
        if (enabled) {
            spinnerReadingNotificationFrequency.setVisibility(View.VISIBLE);
            layoutNotificationTime.setVisibility(View.VISIBLE);
            int frequencyIndex = spinnerReadingNotificationFrequency.getSelectedItemPosition();
            updateReadingNotificationFields(frequencyIndex);
        } else {
            spinnerReadingNotificationFrequency.setVisibility(View.GONE);
            layoutMonthlyDay.setVisibility(View.GONE);
            layoutWeeklyDay.setVisibility(View.GONE);
            layoutNotificationTime.setVisibility(View.GONE);
        }
    }


    /**
     * Aktualizuje viditelnost specifických polí podle zvolené frekvence.
     * 0 = Měsíčně, 1 = Týdně
     */
    private void updateReadingNotificationFields(int frequencyIndex) {
        if (frequencyIndex == 0) {
            // Měsíčně
            layoutMonthlyDay.setVisibility(View.VISIBLE);
            layoutWeeklyDay.setVisibility(View.GONE);
        } else if (frequencyIndex == 1) {
            // Týdně
            layoutMonthlyDay.setVisibility(View.GONE);
            layoutWeeklyDay.setVisibility(View.VISIBLE);
        }
    }


    /**
     * Otevře dialog pro výběr času notifikace a uloží výsledek do preferencí.
     */
    private void showNotificationTimePicker(ShPSettings shPSettings) {
        int[] time = parseTime(tvNotificationTime.getText().toString());
        TimePickerDialog dialog = new TimePickerDialog(
                requireContext(),
                (TimePicker view, int hourOfDay, int minute) -> {
                    String formattedTime = String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minute);
                    tvNotificationTime.setText(formattedTime);
                    shPSettings.set(ShPSettings.READING_NOTIFICATION_TIME, formattedTime);
                    MonthlyReadingReminderScheduler.rescheduleCurrentAsync(requireContext());
                    notifySettingsChanged();
                },
                time[0],
                time[1],
                DateFormat.is24HourFormat(requireContext())
        );
        dialog.show();
    }


    /**
     * Převod času ve formátu HH:mm na hodiny a minuty.
     */
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


    /**
     * Zajistí, že den v měsíci je v rozsahu 1–31.
     */
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


    /**
     * InputFilter, který blokuje zadání hodnot mimo rozsah 1–31.
     */
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

