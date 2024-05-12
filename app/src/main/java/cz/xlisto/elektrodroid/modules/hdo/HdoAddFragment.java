package cz.xlisto.elektrodroid.modules.hdo;

import android.os.Build;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import cz.xlisto.elektrodroid.databaze.DataHdoSource;
import cz.xlisto.elektrodroid.models.SubscriptionPointModel;
import cz.xlisto.elektrodroid.utils.SubscriptionPoint;

/**
 * Xlisto 26.05.2023 21:13
 */
public class HdoAddFragment extends HdoAddEditFragmentAbstract{
    private static final String TAG = "HdoAddFragment";

    public static HdoAddFragment newInstance() {
        return new HdoAddFragment();
    }


    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            timePickerFrom.setMinute(0);
            timePickerUntil.setMinute(0);
        } else {
            timePickerFrom.setCurrentMinute(0);
            timePickerUntil.setCurrentMinute(0);
        }

        btnSave.setOnClickListener(v -> onSaveClicked());
    }


    /**
     * Vytvoří HdoModel a uloží do databáze
     */
    private void onSaveClicked() {
        if(checkSelectedDays()){
            return;
        }
        SubscriptionPointModel subscriptionPoint = SubscriptionPoint.load(requireContext());
        DataHdoSource dataHdoSource = new DataHdoSource(requireContext());
        dataHdoSource.open();
        if (subscriptionPoint != null) {
            dateFrom = "0";
            dateUntil = "0";
            rele = "";
            dataHdoSource.saveHdo(createHdo(),subscriptionPoint.getTableHDO());
        }
        dataHdoSource.close();
        requireActivity().getSupportFragmentManager().popBackStack();
    }


}
