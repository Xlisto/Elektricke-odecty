package cz.xlisto.odecty.modules.hdo;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import cz.xlisto.odecty.databaze.DataHdoSource;
import cz.xlisto.odecty.models.HdoModel;
import cz.xlisto.odecty.models.SubscriptionPointModel;
import cz.xlisto.odecty.utils.SubscriptionPoint;

/**
 * Xlisto 28.05.2023 17:55
 */
public class HdoEditFragment extends HdoAddEditFragmentAbstract {
    private static final String TAG = "HdoEditFragment";
    private static final String ARG_ID = "id";
    private static final String ARG_MON = "mon";
    private static final String ARG_TUE = "tue";
    private static final String ARG_WED = "wed";
    private static final String ARG_THU = "thu";
    private static final String ARG_FRI = "fri";
    private static final String ARG_SAT = "sat";
    private static final String ARG_SUN = "sun";
    private static final String ARG_TIME_FROM = "timeFrom";
    private static final String ARG_TIME_UNTIL = "timeUntil";
    private static final String ARG_IS_FIRST_LOAD = "isFirstLoad";
    private long id;
    private int mon, tue, wed, thu, fri, sat, sun;
    private String timeFrom, timeUntil;
    private boolean isFirstLoad = true;

    public static HdoEditFragment newInstance(HdoModel hdoModel) {
        HdoEditFragment fragment = new HdoEditFragment();
        Bundle args = new Bundle();
        args.putLong(ARG_ID, hdoModel.getId());
        args.putInt(ARG_MON, hdoModel.getMon());
        args.putInt(ARG_TUE, hdoModel.getTue());
        args.putInt(ARG_WED, hdoModel.getWed());
        args.putInt(ARG_THU, hdoModel.getThu());
        args.putInt(ARG_FRI, hdoModel.getFri());
        args.putInt(ARG_SAT, hdoModel.getSat());
        args.putInt(ARG_SUN, hdoModel.getSun());
        args.putString(ARG_TIME_FROM, hdoModel.getTimeFrom());
        args.putString(ARG_TIME_UNTIL, hdoModel.getTimeUntil());


        fragment.setArguments(args);
        return fragment;
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            id = getArguments().getLong(ARG_ID);
            mon = getArguments().getInt(ARG_MON);
            tue = getArguments().getInt(ARG_TUE);
            wed = getArguments().getInt(ARG_WED);
            thu = getArguments().getInt(ARG_THU);
            fri = getArguments().getInt(ARG_FRI);
            sat = getArguments().getInt(ARG_SAT);
            sun = getArguments().getInt(ARG_SUN);
            timeFrom = getArguments().getString(ARG_TIME_FROM);
            timeUntil = getArguments().getString(ARG_TIME_UNTIL);
            isFirstLoad = getArguments().getBoolean(ARG_IS_FIRST_LOAD,true);
        }
    }


    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        cbMonday.setChecked(mon == 1);
        cbTuesday.setChecked(tue == 1);
        cbWednesday.setChecked(wed == 1);
        cbThursday.setChecked(thu == 1);
        cbFriday.setChecked(fri == 1);
        cbSaturday.setChecked(sat == 1);
        cbSunday.setChecked(sun == 1);

        if(savedInstanceState != null){
            isFirstLoad = savedInstanceState.getBoolean(ARG_IS_FIRST_LOAD,true);
        }

        if (isFirstLoad) {
            isFirstLoad = false;
            int hourFrom = Integer.parseInt(timeFrom.split(":")[0]);
            int minuteFrom = Integer.parseInt(timeFrom.split(":")[1]);
            int hourUntil = Integer.parseInt(timeUntil.split(":")[0]);
            int minuteUntil = Integer.parseInt(timeUntil.split(":")[1]);
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                timePickerFrom.setHour(hourFrom);
                timePickerFrom.setMinute(minuteFrom);
                timePickerUntil.setHour(hourUntil);
                timePickerUntil.setMinute(minuteUntil);
            } else {
                timePickerFrom.setCurrentHour(hourFrom);
                timePickerFrom.setCurrentMinute(minuteFrom);
                timePickerUntil.setCurrentHour(hourUntil);
                timePickerUntil.setCurrentMinute(minuteUntil);
            }
        }

        btnSave.setText(getText(cz.xlisto.odecty.R.string.edit));
        btnSave.setOnClickListener(v -> onSaveClicked());
    }


    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(ARG_IS_FIRST_LOAD, isFirstLoad);
    }

    private void onSaveClicked(){
        if(checkSelectedDays()){
            return;
        }
        SubscriptionPointModel subscriptionPoint = SubscriptionPoint.load(requireContext());
        DataHdoSource dataHdoSource = new DataHdoSource(requireContext());
        dataHdoSource.open();
        if (subscriptionPoint != null) {
            dataHdoSource.updateHdo(createHdo(id),subscriptionPoint.getTableHDO());
        }
        dataHdoSource.close();
        requireActivity().getSupportFragmentManager().popBackStack();
    }
}
