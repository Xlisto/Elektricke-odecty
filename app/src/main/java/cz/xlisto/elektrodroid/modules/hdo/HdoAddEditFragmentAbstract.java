package cz.xlisto.elektrodroid.modules.hdo;


import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.util.Locale;

import cz.xlisto.elektrodroid.R;
import cz.xlisto.elektrodroid.models.HdoModel;


/**
 * Abstraktní třída pro přidání/editaci dat s časy HDO
 * Xlisto 26.05.2023 21:13
 */
public class HdoAddEditFragmentAbstract extends Fragment {
    static final String ARG_HOUR_FROM = "hourFrom";
    static final String ARG_MINUTE_FROM = "minuteFrom";
    static final String ARG_HOUR_UNTIL = "hourUntil";
    static final String ARG_MINUTE_UNTIL = "minuteUntil";

    TimePicker timePickerFrom, timePickerUntil;
    CheckBox cbMonday, cbTuesday, cbWednesday, cbThursday, cbFriday, cbSaturday, cbSunday;
    Button btnSave, btnBack;
    int mon = 0;
    int tue = 0;
    int wed = 0;
    int thu = 0;
    int fri = 0;
    int sat = 0;
    int sun = 0;
    int notifyStart = 0;
    int notifyEnd = 0;
    String rele, dateFrom, dateUntil;


    /**
     * Vytvoří layout fragmentu pro přidání nebo editaci HDO.
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_hdo_add_edit, container, false);
    }


    /**
     * Inicializuje UI prvky a obnoví čas při znovuvytvoření fragmentu.
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        timePickerFrom = view.findViewById(R.id.tpFrom);
        timePickerUntil = view.findViewById(R.id.tpUntil);
        timePickerFrom.setIs24HourView(true);
        timePickerUntil.setIs24HourView(true);

        cbMonday = view.findViewById(R.id.cbMonday);
        cbTuesday = view.findViewById(R.id.cbTuesday);
        cbWednesday = view.findViewById(R.id.cbWednesday);
        cbThursday = view.findViewById(R.id.cbThursday);
        cbFriday = view.findViewById(R.id.cbFriday);
        cbSaturday = view.findViewById(R.id.cbSaturday);
        cbSunday = view.findViewById(R.id.cbSunday);

        btnSave = view.findViewById(R.id.btnAddEditHdoSave);
        btnBack = view.findViewById(R.id.btnAddEditHdoBack);


        btnBack.setOnClickListener(v -> requireActivity().getSupportFragmentManager().popBackStack());

        if (savedInstanceState != null) {
            timePickerFrom.setHour(savedInstanceState.getInt(ARG_HOUR_FROM));
            timePickerFrom.setMinute(savedInstanceState.getInt(ARG_MINUTE_FROM));
            timePickerUntil.setHour(savedInstanceState.getInt(ARG_HOUR_UNTIL));
            timePickerUntil.setMinute(savedInstanceState.getInt(ARG_MINUTE_UNTIL));
            initDays();
        }
    }


    /**
     * Uloží stav časových pickerů při změně konfigurace.
     */
    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(ARG_HOUR_FROM, timePickerFrom.getHour());
        outState.putInt(ARG_MINUTE_FROM, timePickerFrom.getMinute());
        outState.putInt(ARG_HOUR_UNTIL, timePickerUntil.getHour());
        outState.putInt(ARG_MINUTE_UNTIL, timePickerUntil.getMinute());
    }


    /**
     * Vytvoří objekt HdoModel z aktuálních hodnot v UI
     *
     * @param id ID záznamu
     * @return HdoModel
     */
    HdoModel createHdo(long id) {
        HdoModel hdo = createHdo();
        hdo.setId(id);
        return hdo;
    }


    /**
     * Vytvoří objekt HdoModel z aktuálních hodnot v UI
     *
     * @return HdoModel
     */
    HdoModel createHdo() {
        int hourFrom = timePickerFrom.getHour();
        int minuteFrom = timePickerFrom.getMinute();
        int hourUntil = timePickerUntil.getHour();
        int minuteUntil = timePickerUntil.getMinute();

        String from = String.format(Locale.getDefault(), "%02d:%02d", hourFrom, minuteFrom);
        String until = String.format(Locale.getDefault(), "%02d:%02d", hourUntil, minuteUntil);
        initDays();

        return new HdoModel(-1, rele, dateFrom, dateUntil, from, until, mon, tue, wed, thu, fri, sat, sun, "", notifyStart, notifyEnd);
    }


    /**
     * Načte výběr dnů z checkboxů do interních polí modelu.
     */
    void initDays() {
        mon = cbMonday.isChecked() ? 1 : 0;
        tue = cbTuesday.isChecked() ? 1 : 0;
        wed = cbWednesday.isChecked() ? 1 : 0;
        thu = cbThursday.isChecked() ? 1 : 0;
        fri = cbFriday.isChecked() ? 1 : 0;
        sat = cbSaturday.isChecked() ? 1 : 0;
        sun = cbSunday.isChecked() ? 1 : 0;
    }


    /**
     * Ověří, že je vybraný alespoň jeden den v týdnu.
     *
     * @return {@code true}, pokud validace selže; jinak {@code false}
     */
    boolean checkSelectedDays() {
        initDays();
        if (mon + tue + wed + thu + fri + sat + sun == 0) {
            Toast.makeText(getContext(), getResources().getString(R.string.have_to_coise_day), Toast.LENGTH_SHORT).show();
            return true;
        }
        return false;
    }
}
