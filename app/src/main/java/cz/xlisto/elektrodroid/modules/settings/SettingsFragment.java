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
import android.widget.CheckBox;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.SwitchCompat;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import cz.xlisto.elektrodroid.R;
import cz.xlisto.elektrodroid.shp.ShPSettings;


/**
 * Fragment pro změnu preferencí zobrazení aplikace.
 * <p>
 * Uživatel může zapnout/vypnout FAB, spodní navigaci a levou navigaci.
 * Po změně přepínače se hodnoty ihned uloží do {@link ShPSettings} a odešlou se výsledky
 * pro aktualizaci fragmentů i aktivity.
 */
public class SettingsFragment extends Fragment {
    private CheckBox chNotificationPermission;
    private CheckBox chExactAlarmPermission;

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
        SwitchCompat switchShowFab = view.findViewById(R.id.switchShowFab);
        SwitchCompat switchShowBottomNavigation = view.findViewById(R.id.switchShowBottomNavigation);
        SwitchCompat switchShowLeftNavigation = view.findViewById(R.id.switchShowLeftNavigation);
        SwitchCompat switchAllowMobileData = view.findViewById(R.id.switchAllowMobileData);
        ShPSettings shPSettings = new ShPSettings(requireContext());

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
