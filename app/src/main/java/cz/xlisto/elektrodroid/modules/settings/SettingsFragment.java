package cz.xlisto.elektrodroid.modules.settings;


import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.app.AlarmManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.Spinner;

import androidx.annotation.Nullable;

import com.google.android.material.color.DynamicColors;
import com.google.android.material.materialswitch.MaterialSwitch;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;

import cz.xlisto.elektrodroid.MainActivity;
import cz.xlisto.elektrodroid.R;
import cz.xlisto.elektrodroid.modules.widgets.WidgetUpdateManager;
import cz.xlisto.elektrodroid.services.HdoAlarmScheduler;
import cz.xlisto.elektrodroid.shp.ShPHdo;
import cz.xlisto.elektrodroid.shp.ShPSettings;
import cz.xlisto.elektrodroid.ui.OnPaddingChangeListener;


/**
 * Fragment pro změnu preferencí zobrazení aplikace.
 * <p>
 * Uživatel může zapnout/vypnout FAB, spodní navigaci a levou navigaci.
 * Po změně přepínače se hodnoty ihned uloží do {@link ShPSettings} a odešlou se výsledky
 * pro aktualizaci fragmentů i aktivity.
 */
public class SettingsFragment extends Fragment implements OnPaddingChangeListener {

    private CheckBox chNotificationPermission;
    private CheckBox chExactAlarmPermission;
    private NestedScrollView scrollViewSettings;
    private View llSettingsContent;
    private Boolean wasExactAlarmEnabled;

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
        scrollViewSettings = view.findViewById(R.id.scrollViewSettings);
        llSettingsContent = view.findViewById(R.id.llSettingsContent);
        chNotificationPermission = view.findViewById(R.id.chNotificationPermission);
        chExactAlarmPermission = view.findViewById(R.id.chExactAlarmPermission);
        CheckBox chUseSetAlarm = view.findViewById(R.id.chUseSetAlarm);
        Spinner spAddButtonType = view.findViewById(R.id.spAddButtonType);
        MaterialSwitch switchShowBottomNavigation = view.findViewById(R.id.switchShowBottomNavigation);
        MaterialSwitch switchShowLeftNavigation = view.findViewById(R.id.switchShowLeftNavigation);
        MaterialSwitch switchAllowMobileData = view.findViewById(R.id.switchAllowMobileData);
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

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                requireContext(),
                R.array.add_button_type_options,
                android.R.layout.simple_spinner_item
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spAddButtonType.setAdapter(adapter);
        spAddButtonType.setSelection(shPSettings.getAddButtonType());
        spAddButtonType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View v, int position, long id) {
                if (shPSettings.getAddButtonType() != position) {
                    shPSettings.setAddButtonType(position);
                    notifySettingsChanged();
                }
            }


            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        switchShowBottomNavigation.setChecked(shPSettings.get(ShPSettings.SHOW_BOTTOM_NAVIGATION, true));
        switchShowLeftNavigation.setChecked(shPSettings.get(ShPSettings.SHOW_LEFT_NAVIGATION, true));
        switchAllowMobileData.setChecked(shPSettings.get(ShPSettings.ALLOW_MOBILE_DATA, true));
        chUseSetAlarm.setChecked(shPSettings.get(ShPSettings.USE_HDO_SET_ALARM, false));

        MaterialSwitch switchAutoSetCurrentDateNewReading = view.findViewById(R.id.switchAutoSetCurrentDateNewReading);
        if (switchAutoSetCurrentDateNewReading != null) {
            switchAutoSetCurrentDateNewReading.setChecked(shPSettings.isAutoSetCurrentDateNewReading());
            switchAutoSetCurrentDateNewReading.setOnCheckedChangeListener((buttonView, isChecked) ->
                    shPSettings.setAutoSetCurrentDateNewReading(isChecked));
        }

        CheckBox chHdoOldReminderEnabled = view.findViewById(R.id.chHdoOldReminderEnabled);
        if (chHdoOldReminderEnabled != null) {
            final ShPHdo shPHdo = new ShPHdo(requireContext());
            chHdoOldReminderEnabled.setChecked(!shPHdo.get(ShPHdo.HDO_OLD_REMINDER_DISABLED, false));
            chHdoOldReminderEnabled.setOnCheckedChangeListener((buttonView, isChecked) -> shPHdo.set(ShPHdo.HDO_OLD_REMINDER_DISABLED, !isChecked));
        }

        chUseSetAlarm.setOnCheckedChangeListener((buttonView, isChecked) -> {
            shPSettings.set(ShPSettings.USE_HDO_SET_ALARM, isChecked);
            rescheduleAllHdoAlarmsAsync();
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

        View layoutAppearanceSettings = view.findViewById(R.id.layoutAppearanceSettings);
        MaterialSwitch switchUseDynamicColors = view.findViewById(R.id.switchUseDynamicColors);
        if (layoutAppearanceSettings != null && switchUseDynamicColors != null) {
            if (DynamicColors.isDynamicColorAvailable()) {
                layoutAppearanceSettings.setVisibility(View.VISIBLE);
                switchUseDynamicColors.setChecked(shPSettings.isUseDynamicColors());
                switchUseDynamicColors.setOnCheckedChangeListener((buttonView, isChecked) -> {
                    if (shPSettings.isUseDynamicColors() != isChecked) {
                        shPSettings.setUseDynamicColors(isChecked);
                        WidgetUpdateManager.updateAllWidgetsAsync(requireContext(), null);
                        requireActivity().recreate();
                    }
                });
            } else {
                layoutAppearanceSettings.setVisibility(View.GONE);
            }
        }

        setupScrollListener();
        view.post(this::updateViewPadding);

        return view;
    }


    /**
     * Nastaví posluchač posunu nad {@link NestedScrollView}, který při posunu dolů přepíná
     * spodní navigaci do kompaktního režimu a při návratu nahoru ji opět rozbalí.
     */
    private void setupScrollListener() {
        if (scrollViewSettings == null) return;
        final int thresholdPx = (int) (12 * getResources().getDisplayMetrics().density);
        scrollViewSettings.setOnScrollChangeListener((NestedScrollView.OnScrollChangeListener) (v, scrollX, scrollY, oldScrollX, oldScrollY) -> {
            if (!(getActivity() instanceof MainActivity mainAct)) return;
            if (scrollY == 0) {
                mainAct.setBottomNavCompact(false);
                return;
            }
            int dy = scrollY - oldScrollY;
            if (Math.abs(dy) >= thresholdPx) {
                mainAct.setBottomNavCompact(dy > 0);
            }
        });
    }


    /**
     * Po návratu do popředí aktualizuje stavy systémových oprávnění a vypočte padding.
     */
    @Override
    public void onResume() {
        super.onResume();
        syncNotificationPermissionState();
        syncExactAlarmPermissionState();
        updateViewPadding();
    }


    public void updateViewPadding() {
        if (!isAdded()) return;
        if (getActivity() instanceof MainActivity mainAct) {
            float density = getResources().getDisplayMetrics().density;
            int basePaddingPx = (int) (16 * density);
            int topPaddingPx = mainAct.getTopBarTotalHeight();
            int bottomPaddingPx = mainAct.isNavRail()
                    ? mainAct.getEffectiveNavBarInsetBottom() + (int) (24 * density)
                    : Math.max(
                    mainAct.getBottomPanelMarginPx() + (int) (24 * density),
                    mainAct.getEffectiveNavBarInsetBottom() + (int) (120 * density)
            );
            int leftPaddingPx = mainAct.getLeftNavTotalWidth();
            int rightPaddingPx = mainAct.getEffectiveNavBarInsetRight();

            if (scrollViewSettings != null && llSettingsContent != null) {
                scrollViewSettings.setClipToPadding(false);
                scrollViewSettings.setPadding(0, 0, 0, 0);
                llSettingsContent.setPadding(
                        basePaddingPx + leftPaddingPx,
                        basePaddingPx + topPaddingPx,
                        basePaddingPx + rightPaddingPx,
                        bottomPaddingPx
                );
            } else if (getView() instanceof ViewGroup vg) {
                vg.setClipToPadding(false);
                vg.setPadding(leftPaddingPx, topPaddingPx, rightPaddingPx, bottomPaddingPx);
            }
        }
    }


    @Override
    public void onPaddingChanged() {
        updateViewPadding();
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



}

