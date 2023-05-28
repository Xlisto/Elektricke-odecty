package cz.xlisto.odecty.modules.hdo;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Timer;
import java.util.TimerTask;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import cz.xlisto.odecty.databaze.DataHdoSource;
import cz.xlisto.odecty.databaze.DataSettingsSource;
import cz.xlisto.odecty.dialogs.YesNoDialogFragment;
import cz.xlisto.odecty.format.SimpleDateFormatHelper;
import cz.xlisto.odecty.models.HdoModel;
import cz.xlisto.odecty.models.SubscriptionPointModel;
import cz.xlisto.odecty.utils.DetectNightMode;
import cz.xlisto.odecty.utils.FragmentChange;
import cz.xlisto.odecty.utils.SubscriptionPoint;

/**
 * Xlisto 26.05.2023 10:35
 */
public class HdoFragment extends Fragment {
    private static final String TAG = "HdoFragment";
    private static final long minute = 60000;
    private Timer timer;
    private SubscriptionPointModel subscriptionPoint;
    private TextView tvTimeHdo, tvTimeDifference, tvAlertHdo;
    private RecyclerView rvHdo;
    private long idSubscriptionPoint, timeDifferent;
    private ArrayList<HdoModel> hdoModels = new ArrayList<>();
    private HdoAdapter hdoAdapter;

    //překreslení gui
    private final Runnable timerTick = this::setTime;

    public static HdoFragment newInstance() {
        return new HdoFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(cz.xlisto.odecty.R.layout.fragment_hdo, container, false);
    }


    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        tvTimeHdo = view.findViewById(cz.xlisto.odecty.R.id.tvTimeHdo);
        tvTimeDifference = view.findViewById(cz.xlisto.odecty.R.id.tvTimeDifference);
        tvAlertHdo = view.findViewById(cz.xlisto.odecty.R.id.tvAlertHdo);
        rvHdo = view.findViewById(cz.xlisto.odecty.R.id.rvHdo);
        FloatingActionButton fabAddHdo = view.findViewById(cz.xlisto.odecty.R.id.fabHdo);

        Button btnAddHour = view.findViewById(cz.xlisto.odecty.R.id.btnAddHour);
        Button btnRemoveHour = view.findViewById(cz.xlisto.odecty.R.id.btnRemoveHour);
        Button btnAddMinute = view.findViewById(cz.xlisto.odecty.R.id.btnAddMinute);
        Button btnRemoveMinute = view.findViewById(cz.xlisto.odecty.R.id.btnRemoveMinute);

        btnAddMinute.setOnClickListener(v -> {
            DataSettingsSource dataSettingsSource = new DataSettingsSource(requireContext());
            dataSettingsSource.open();
            dataSettingsSource.changeTimeShift(idSubscriptionPoint, timeDifferent += minute);
            dataSettingsSource.close();
            setTimeDifferent();
        });
        btnRemoveMinute.setOnClickListener(v -> {
            DataSettingsSource dataSettingsSource = new DataSettingsSource(requireContext());
            dataSettingsSource.open();
            dataSettingsSource.changeTimeShift(idSubscriptionPoint, timeDifferent -= minute);
            dataSettingsSource.close();
            setTimeDifferent();
        });
        btnAddHour.setOnClickListener(v -> {
            DataSettingsSource dataSettingsSource = new DataSettingsSource(requireContext());
            dataSettingsSource.open();
            dataSettingsSource.changeTimeShift(idSubscriptionPoint, timeDifferent += minute * 60);
            dataSettingsSource.close();
            setTimeDifferent();
        });
        btnRemoveHour.setOnClickListener(v -> {
            DataSettingsSource dataSettingsSource = new DataSettingsSource(requireContext());
            dataSettingsSource.open();
            dataSettingsSource.changeTimeShift(idSubscriptionPoint, timeDifferent -= minute * 60);
            dataSettingsSource.close();
            setTimeDifferent();
        });

        fabAddHdo.setOnClickListener(v -> {
            HdoAddFragment hdoAddFragment = HdoAddFragment.newInstance();
            FragmentChange.replace(requireActivity(), hdoAddFragment, FragmentChange.Transaction.MOVE, true);
        });

        requireActivity().getSupportFragmentManager().setFragmentResultListener(HdoAdapter.FLAG_HDO_ADAPTER_DELETE, this, (requestKey, result) -> {
            if (result.getBoolean(YesNoDialogFragment.RESULT)) {
                deleteHdo();
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        subscriptionPoint = SubscriptionPoint.load(requireActivity());
        if (subscriptionPoint != null)
            idSubscriptionPoint = subscriptionPoint.getId();
        else
            idSubscriptionPoint = -1L;
        DataSettingsSource dataSettingsSource = new DataSettingsSource(requireContext());
        dataSettingsSource.open();
        timeDifferent = dataSettingsSource.loadTimeShift(idSubscriptionPoint);
        dataSettingsSource.close();
        setTimeDifferent();
        startTimer();
        showAlert();
        loadData();
    }

    @Override
    public void onPause() {
        super.onPause();
        endTimer();
    }


    /**
     * Spustí časovač
     */
    private void startTimer() {
        timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                requireActivity().runOnUiThread(timerTick);//zavolání překreslení GUI});
            }
        }, 0, 1000);
    }


    /**
     * Zastaví časovač
     */
    private void endTimer() {
        if (timer != null) timer.cancel();
    }


    /**
     * Nastaví časový posun
     */
    private void setTimeDifferent() {
        //TODO: doplnit správné časování slov

        tvTimeDifference.setText(String.format("Rozdíl %01d hodin a %02d minut", timeDifferent / 3600000, (timeDifferent % 3600000) / 60000));
        setTime();
    }


    /**
     * Nastaví čas, který se zobrazuje na displeji hodin
     */
    private void setTime() {
        Calendar calendar = Calendar.getInstance();//získání aktuálního času
        calendar.setTimeInMillis(calendar.getTimeInMillis() + timeDifferent);//nastavení kalendáře na aktuální čas + časový posun
        long miliseconds = calendar.getTimeInMillis();//aktuální čas v milisekundách s časovým posunem
        tvTimeHdo.setText(SimpleDateFormatHelper.timeFormatOnlyTime.format(miliseconds).toUpperCase());


        //kontrola, zda je čas v rozmezí HDO
        for (int i = 0; i < hdoModels.size(); i++) {
            int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
            if (dayOfWeek == 1 && hdoModels.get(i).getSun() == 0) return;
            if (dayOfWeek == 2 && hdoModels.get(i).getMon() == 0) return;
            if (dayOfWeek == 3 && hdoModels.get(i).getTue() == 0) return;
            if (dayOfWeek == 4 && hdoModels.get(i).getWed() == 0) return;
            if (dayOfWeek == 5 && hdoModels.get(i).getThu() == 0) return;
            if (dayOfWeek == 6 && hdoModels.get(i).getFri() == 0) return;
            if (dayOfWeek == 7 && hdoModels.get(i).getSat() == 0) return;

            Calendar calendarFrom = Calendar.getInstance();
            Calendar calendarUntil = Calendar.getInstance();
            int hourFrom = Integer.parseInt(hdoModels.get(i).getTimeFrom().split(":")[0]);
            int minuteFrom = Integer.parseInt(hdoModels.get(i).getTimeFrom().split(":")[1]);
            int hourUntil = Integer.parseInt(hdoModels.get(i).getTimeUntil().split(":")[0]);
            int minuteUntil = Integer.parseInt(hdoModels.get(i).getTimeUntil().split(":")[1]);
            calendarFrom.set(Calendar.HOUR_OF_DAY, hourFrom);
            calendarUntil.set(Calendar.HOUR_OF_DAY, hourUntil);
            calendarFrom.set(Calendar.MINUTE, minuteFrom);
            calendarUntil.set(Calendar.MINUTE, minuteUntil);
            calendarFrom.set(Calendar.SECOND, 0);
            calendarUntil.set(Calendar.SECOND, 0);
            if (calendarUntil.getTimeInMillis()<calendarFrom.getTimeInMillis()) calendarUntil.add(Calendar.DAY_OF_MONTH, 1);
            if (calendar.getTimeInMillis()<calendarFrom.getTimeInMillis()) calendar.add(Calendar.DAY_OF_MONTH, 1);


            if (calendar.after(calendarFrom) && calendar.before(calendarUntil)) {
                tvTimeHdo.setTextColor(Color.parseColor("#187e34"));
                break;
            } else {
                if (DetectNightMode.isNightMode(requireActivity()))
                    tvTimeHdo.setTextColor(getResources().getColor(android.R.color.secondary_text_dark));
                else
                    tvTimeHdo.setTextColor(getResources().getColor(android.R.color.secondary_text_light));
            }
        }
    }


    /**
     * Zobrazí/skryje upozornění na nevybrané odběrné místo
     */
    private void showAlert() {
        if (idSubscriptionPoint == -1L) {
            tvAlertHdo.setVisibility(View.VISIBLE);
        } else {
            tvAlertHdo.setVisibility(View.GONE);
        }
    }


    /**
     * Načte hdo data z databáze a nastaví adapter
     */
    private void loadData() {
        DataHdoSource dataHdoSource = new DataHdoSource(requireActivity());
        dataHdoSource.open();
        hdoModels.clear();
        hdoModels = dataHdoSource.loadHdo(subscriptionPoint.getTableHDO());
        dataHdoSource.close();

        setAdapter();
    }


    /**
     * Nastaví adapter pro recycler view
     */
    private void setAdapter() {
        hdoAdapter = new HdoAdapter(hdoModels, rvHdo);
        rvHdo.setAdapter(hdoAdapter);
        rvHdo.setLayoutManager(new LinearLayoutManager(requireActivity()));
    }


    /**
     * Zavolá metodu v adapteru pro odstranění HDO záznamu
     */
    private void deleteHdo() {
        hdoAdapter.deleteItem();
    }
}
