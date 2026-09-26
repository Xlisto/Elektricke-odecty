package cz.xlisto.elektrodroid.dialogs;


import android.app.Dialog;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

import cz.xlisto.elektrodroid.R;
import cz.xlisto.elektrodroid.databaze.DataSettingsSource;
import cz.xlisto.elektrodroid.format.SimpleDateFormatHelper;


/**
 * Dialog pro nastavení časového posunu (offsetu) HDO hodin.
 */
public class TimeShiftDialogFragment extends DialogFragment {

    private static final String ARG_SUBSCRIPTION_POINT_ID = "subscriptionPointId";
    private static final long minute = 60000;
    private long subscriptionPointId;
    private long timeShift;
    private TextView tvDeviceTimeDialog, tvMeterTimeDialog, tvTimeDifferenceDialog;
    private Timer timer;
    private final Runnable timerTick = this::updateTimeDifferenceText;


    public static TimeShiftDialogFragment newInstance(long subscriptionPointId, long currentTimeShift) {
        TimeShiftDialogFragment fragment = new TimeShiftDialogFragment();
        Bundle args = new Bundle();
        args.putLong(ARG_SUBSCRIPTION_POINT_ID, subscriptionPointId);
        args.putLong("timeShift", currentTimeShift);
        fragment.setArguments(args);
        return fragment;
    }


    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        if (getArguments() != null) {
            subscriptionPointId = getArguments().getLong(ARG_SUBSCRIPTION_POINT_ID, -1L);
            timeShift = getArguments().getLong("timeShift", 0L);
        }

        View view = getLayoutInflater().inflate(R.layout.dialog_time_shift, null);
        tvDeviceTimeDialog = view.findViewById(R.id.tvDeviceTimeDialog);
        tvMeterTimeDialog = view.findViewById(R.id.tvMeterTimeDialog);
        tvTimeDifferenceDialog = view.findViewById(R.id.tvTimeDifferenceDialog);
        Button btnAddHour = view.findViewById(R.id.btnAddHourDialog);
        Button btnRemoveHour = view.findViewById(R.id.btnRemoveHourDialog);
        Button btnAddMinute = view.findViewById(R.id.btnAddMinuteDialog);
        Button btnRemoveMinute = view.findViewById(R.id.btnRemoveMinuteDialog);

        updateTimeDifferenceText();

        btnAddHour.setOnClickListener(v -> {
            timeShift += minute * 60;
            saveTimeShift();
            updateTimeDifferenceText();
        });
        btnRemoveHour.setOnClickListener(v -> {
            timeShift -= minute * 60;
            saveTimeShift();
            updateTimeDifferenceText();
        });
        btnAddMinute.setOnClickListener(v -> {
            timeShift += minute;
            saveTimeShift();
            updateTimeDifferenceText();
        });
        btnRemoveMinute.setOnClickListener(v -> {
            timeShift -= minute;
            saveTimeShift();
            updateTimeDifferenceText();
        });

        return new AlertDialog.Builder(requireContext(), R.style.DialogTheme)
                .setTitle(R.string.different_time)
                .setView(view)
                .setPositiveButton(R.string.ok, (dialog, which) -> getParentFragmentManager().setFragmentResult("TIME_SHIFT_CHANGED", new Bundle()))
                .create();
    }


    @Override
    public void onStart() {
        super.onStart();
        DialogButtonColorHelper.apply(this);
        startTimer();
    }


    @Override
    public void onStop() {
        super.onStop();
        endTimer();
    }


    private void startTimer() {
        timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (isAdded() && getActivity() != null) {
                    getActivity().runOnUiThread(timerTick);
                }
            }
        }, 0, 1000);
    }


    private void endTimer() {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }


    private void saveTimeShift() {
        if (subscriptionPointId == -1L) return;
        DataSettingsSource dataSettingsSource = new DataSettingsSource(requireContext());
        dataSettingsSource.open();
        dataSettingsSource.changeTimeShift(subscriptionPointId, timeShift);
        dataSettingsSource.close();
    }


    private void updateTimeDifferenceText() {
        if (tvDeviceTimeDialog == null || tvMeterTimeDialog == null || tvTimeDifferenceDialog == null)
            return;
        long nowMillis = System.currentTimeMillis();
        long meterMillis = nowMillis + timeShift;

        String deviceTimeStr = SimpleDateFormatHelper.onlyTime.format(nowMillis).toUpperCase();
        String meterTimeStr = SimpleDateFormatHelper.onlyTime.format(meterMillis).toUpperCase();

        tvDeviceTimeDialog.setText(getString(R.string.device_time_label, deviceTimeStr));
        tvMeterTimeDialog.setText(getString(R.string.meter_time_label, meterTimeStr));

        String hoursStr = "hodin";
        long h = timeShift / 3600000;
        long absH = Math.abs(h);
        if (absH == 1) hoursStr = "hodina";
        else if (absH >= 2 && absH <= 4) hoursStr = "hodiny";

        String minutesStr = "minut";
        long m = (Math.abs(timeShift) % 3600000) / 60000;
        if (m == 1) minutesStr = "minuta";
        else if (m >= 2 && m <= 4) minutesStr = "minuty";

        tvTimeDifferenceDialog.setText(String.format(Locale.GERMANY, "Rozdíl %01d %s a %02d %s ", h, hoursStr, m, minutesStr));
    }

}
