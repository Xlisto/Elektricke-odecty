package cz.xlisto.odecty.modules.hdo;


import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.switchmaterial.SwitchMaterial;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import cz.xlisto.odecty.R;
import cz.xlisto.odecty.databaze.DataHdoSource;
import cz.xlisto.odecty.databaze.DataSettingsSource;
import cz.xlisto.odecty.dialogs.SettingsViewDialogFragment;
import cz.xlisto.odecty.dialogs.SubscriptionPointDialogFragment;
import cz.xlisto.odecty.dialogs.YesNoDialogFragment;
import cz.xlisto.odecty.format.SimpleDateFormatHelper;
import cz.xlisto.odecty.models.HdoModel;
import cz.xlisto.odecty.models.SubscriptionPointModel;
import cz.xlisto.odecty.services.HdoData;
import cz.xlisto.odecty.services.HdoService;
import cz.xlisto.odecty.shp.ShPHdo;
import cz.xlisto.odecty.utils.DetectNightMode;
import cz.xlisto.odecty.utils.FragmentChange;
import cz.xlisto.odecty.utils.SubscriptionPoint;
import cz.xlisto.odecty.utils.UIHelper;


/**
 * Fragment zobrazující časy HDO
 * Xlisto 26.05.2023 10:35
 */
public class HdoFragment extends Fragment {

    private static final String TAG = "HdoFragment";
    private static final long minute = 60000;
    private Timer timer;
    private SubscriptionPointModel subscriptionPoint;
    private TextView tvTimeHdo, tvTimeDifference, tvAlertHdo, tvDateHdo;
    private ImageView imageViewIconNT;
    private SwitchMaterial swHdoService;
    private Spinner spReleSettings;
    private RecyclerView rvHdo;
    private long idSubscriptionPoint, timeDifferent;
    private ArrayList<HdoModel> hdoModels = new ArrayList<>();
    private ArrayList<String> reles = new ArrayList<>();
    private HdoAdapter hdoAdapter;
    private Intent hdoServiceIntent;
    private Button btnAddMinute, btnRemoveMinute, btnRemoveHour, btnAddHour, btnAddHdo;
    private FloatingActionButton fab;
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
        requireActivity().invalidateOptionsMenu();
        hdoServiceIntent = new Intent(requireActivity(), HdoService.class);
        tvTimeHdo = view.findViewById(cz.xlisto.odecty.R.id.tvTimeHdo);
        tvTimeDifference = view.findViewById(cz.xlisto.odecty.R.id.tvTimeDifference);
        tvAlertHdo = view.findViewById(cz.xlisto.odecty.R.id.tvAlertHdo);
        tvDateHdo = view.findViewById(R.id.tvHdoDate);
        rvHdo = view.findViewById(cz.xlisto.odecty.R.id.rvHdo);
        swHdoService = view.findViewById(R.id.swHdoService);
        spReleSettings = view.findViewById(R.id.spReleSettings);
        imageViewIconNT = view.findViewById(R.id.imageViewIconNT);
        fab = view.findViewById(cz.xlisto.odecty.R.id.fabHdo);
        btnAddHdo = view.findViewById(cz.xlisto.odecty.R.id.btnAddHdo);
        btnAddHour = view.findViewById(cz.xlisto.odecty.R.id.btnAddHour);
        btnRemoveHour = view.findViewById(cz.xlisto.odecty.R.id.btnRemoveHour);
        btnAddMinute = view.findViewById(cz.xlisto.odecty.R.id.btnAddMinute);
        btnRemoveMinute = view.findViewById(cz.xlisto.odecty.R.id.btnRemoveMinute);
        Button btnHdoLoad = view.findViewById(cz.xlisto.odecty.R.id.btnHdoLoad);
        btnAddHdo = view.findViewById(cz.xlisto.odecty.R.id.btnAddHdo);
        btnAddMinute.setOnClickListener(v -> changeTimeShift(timeDifferent += minute));
        btnRemoveMinute.setOnClickListener(v -> changeTimeShift(timeDifferent -= minute));
        btnAddHour.setOnClickListener(v -> changeTimeShift(timeDifferent += minute * 60));
        btnRemoveHour.setOnClickListener(v -> changeTimeShift(timeDifferent -= minute * 60));
        fab.setOnClickListener(v -> showAddDialog());
        btnAddHdo.setOnClickListener(v -> showAddDialog());
        btnHdoLoad.setOnClickListener(v -> {
            HdoSiteFragment hdoSite = new HdoSiteFragment();
            FragmentChange.replace(requireActivity(), hdoSite, FragmentChange.Transaction.MOVE, true);
        });
        swHdoService.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                HdoData.loadHdoData(requireActivity());
                startService();
            } else {
                stopService();
            }
            if (reles.size() > 1)
                spReleSettings.setVisibility(View.VISIBLE);
            else
                spReleSettings.setVisibility(View.GONE);
            ShPHdo shPHdo = new ShPHdo(requireContext());
            shPHdo.set(ShPHdo.ARG_RUNNING_SERVICE, isChecked);
        });
        spReleSettings.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                loadData(spReleSettings.getAdapter().getItem(position).toString());
            }


            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        //posluchač na smazání HDO
        requireActivity().getSupportFragmentManager().setFragmentResultListener(HdoAdapter.FLAG_HDO_ADAPTER_DELETE, this, (requestKey, result) -> {
            if (result.getBoolean(YesNoDialogFragment.RESULT)) {
                deleteHdo();
            }
        });
        //posluchač pro změnu odběrného místa
        requireActivity().getSupportFragmentManager().setFragmentResultListener
                (SubscriptionPointDialogFragment.FLAG_UPDATE_SUBSCRIPTION_POINT,
                        this,
                        (requestKey, result) -> onResume()
                );
        //posluchač při zavření dialogového okna nastavení
        requireActivity().getSupportFragmentManager().setFragmentResultListener(SettingsViewDialogFragment.FLAG_UPDATE_SETTINGS, this,
                ((requestKey, result) -> UIHelper.showButtons(btnAddHdo, fab, requireActivity())));
    }


    @Override
    public void onResume() {
        super.onResume();
        subscriptionPoint = SubscriptionPoint.load(requireActivity());
        if (subscriptionPoint != null)
            idSubscriptionPoint = subscriptionPoint.getId();
        else {
            idSubscriptionPoint = -1L;
            btnAddMinute.setEnabled(false);
            btnRemoveMinute.setEnabled(false);
            btnAddHour.setEnabled(false);
            btnRemoveHour.setEnabled(false);
            imageViewIconNT.setVisibility(View.GONE);
            return;
        }
        DataSettingsSource dataSettingsSource = new DataSettingsSource(requireContext());
        dataSettingsSource.open();
        timeDifferent = dataSettingsSource.loadTimeShift(idSubscriptionPoint);
        dataSettingsSource.close();
        setTimeDifferent();
        startTimer();
        showAlert();
        loadData();
        loadReles();
        swHdoService.setChecked(HdoService.isRunningService());
        HdoService.setHdoModels(hdoModels);
        HdoService.setDifferentTime(timeDifferent);
        UIHelper.showButtons(btnAddHdo, fab, requireActivity());
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
                requireActivity().runOnUiThread(timerTick);//zavolání překreslení GUI
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
        String hours = "hodin";
        if (timeDifferent / 3600000 == 1) hours = "hodina";
        if ((timeDifferent / 3600000 >= 2) && (timeDifferent / 3600000) <= 4) hours = "hodiny";
        String minutes = "minut";
        if (((timeDifferent % 3600000) / 60000) == 1) minutes = "minuta";
        if ((((timeDifferent % 3600000) / 60000) >= 2) && (((timeDifferent % 3600000) / 60000) <= 4))
            minutes = "minuty";
        tvTimeDifference.setText(String.format(Locale.GERMANY, "Rozdíl %01d %s a %02d %s ", timeDifferent / 3600000, hours, (timeDifferent % 3600000) / 60000, minutes));
        setTime();
    }


    /**
     * Nastaví čas, který se zobrazuje na displeji hodin
     */
    private void setTime() {
        Calendar calendar = Calendar.getInstance();//získání aktuálního času
        calendar.setTimeInMillis(calendar.getTimeInMillis() + timeDifferent);//nastavení kalendáře na aktuální čas + časový posun
        long miliseconds = calendar.getTimeInMillis();//aktuální čas v milisekundách s časovým posunem
        tvTimeHdo.setText(SimpleDateFormatHelper.onlyTime.format(miliseconds).toUpperCase());
        if (hdoModels.isEmpty()) return;
        setTextHdoColor(HdoTime.checkHdo(hdoModels, calendar));
    }


    /**
     * Nastaví barvu textu podle toho, zda je čas v rozmezí HDO a zobrazí ikonu HDO
     */
    private void setTextHdoColor(boolean show) {
        if (isAdded()) {
            if (show) {
                tvTimeHdo.setTextColor(Color.parseColor("#187e34"));
                imageViewIconNT.setImageDrawable(ContextCompat.getDrawable(requireActivity(), R.drawable.nt_on));
            } else {
                if (DetectNightMode.isNightMode(requireActivity()))
                    tvTimeHdo.setTextColor(getResources().getColor(android.R.color.secondary_text_dark));
                else
                    tvTimeHdo.setTextColor(getResources().getColor(android.R.color.secondary_text_light));
                imageViewIconNT.setImageDrawable(ContextCompat.getDrawable(requireActivity(), R.drawable.nt_off));
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
        loadData(null);
    }


    /**
     * Načte hdo data z databáze a nastaví adapter
     *
     * @param rele - rele, které se má načíst
     */
    private void loadData(String rele) {
        DataHdoSource dataHdoSource = new DataHdoSource(requireActivity());
        hdoModels.clear();
        dataHdoSource.open();
        if (rele != null)
            hdoModels = dataHdoSource.loadHdo(subscriptionPoint.getTableHDO(), null, null, rele);
        else
            hdoModels = dataHdoSource.loadHdo(subscriptionPoint.getTableHDO());
        dataHdoSource.close();
        String date = "";
        String distributionArea = "";
        for (int i = 0; i < hdoModels.size(); i++) {
            distributionArea = hdoModels.get(i).getDistributionArea();
            if (i == 0)
                date = hdoModels.get(i).getDateFrom();
            if (i == hdoModels.size() - 1) {
                if (hdoModels.get(i).getDistributionArea().equals(DistributionArea.PRE.toString()))
                    date = date + " - " + hdoModels.get(i).getDateFrom();
                else
                    date = date + " - " + hdoModels.get(i).getDateUntil();
            }
        }
        setAdapter();
        if (distributionArea.isEmpty()) {
            tvDateHdo.setVisibility(View.GONE);
        } else {
            tvDateHdo.setVisibility(View.VISIBLE);
            tvDateHdo.setText(date);
        }
    }


    /**
     * Načte seznam relé z databáze a nastaví spinner
     * Pokud je pouze jedno rele, spinner se nezobrazí
     */
    private void loadReles() {
        DataHdoSource dataHdoSource = new DataHdoSource(requireActivity());
        dataHdoSource.open();
        reles = dataHdoSource.getReles(subscriptionPoint.getTableHDO());
        dataHdoSource.close();
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireActivity(), android.R.layout.simple_spinner_dropdown_item, reles);
        spReleSettings.setAdapter(adapter);
        if (reles.size() > 1)
            spReleSettings.setVisibility(View.VISIBLE);
        else
            spReleSettings.setVisibility(View.GONE);
    }


    /**
     * Nastaví adapter pro recycler view
     */
    private void setAdapter() {
        hdoAdapter = new HdoAdapter(hdoModels, rvHdo, true);
        rvHdo.setAdapter(hdoAdapter);
        rvHdo.setLayoutManager(new LinearLayoutManager(requireActivity()));
    }


    /**
     * Zavolá metodu v adapteru pro odstranění HDO záznamu
     */
    private void deleteHdo() {
        hdoAdapter.deleteItem();
    }


    /**
     * Spustí službu pro kontrolu HDO
     */
    private void startService() {
        HdoService.setHdoModels(hdoModels);
        if (subscriptionPoint == null) return;
        HdoService.setTitle(getResources().getString(R.string.app_name) + " - " + subscriptionPoint.getName());
        //myIntent.putExtra(HdoService.NOTIFICATION_HDO_SERVICE, isLowHdo);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            requireActivity().startForegroundService(hdoServiceIntent);
        } else {
            requireActivity().startService(hdoServiceIntent);
        }
        setDifferentTimeService();
    }


    /**
     * Zastaví službu pro kontrolu HDO
     */
    private void stopService() {
        //requireActivity().startService(myIntent);//Kvůli chybě: Context.startForegroundService() did not then call Service.startForeground()
        requireActivity().stopService(hdoServiceIntent);
    }


    /**
     * Uloží do statické proměnné časový posun z databáze
     */
    private void setDifferentTimeService() {
        HdoService.setDifferentTime(timeDifferent);
    }


    /**
     * Uloží do databáze časový posun, nastaví textview s časovým posunem a aktualizuje časový údaj ve službě
     */
    private void changeTimeShift(long timeShift) {
        if (idSubscriptionPoint == -1L) return;
        DataSettingsSource dataSettingsSource = new DataSettingsSource(requireContext());
        dataSettingsSource.open();
        dataSettingsSource.changeTimeShift(idSubscriptionPoint, timeShift);
        dataSettingsSource.close();
        setTimeDifferent();
        setDifferentTimeService();
    }


    /**
     * Zobrazí dialog pro přidání HDO
     */
    private void showAddDialog() {
        HdoAddFragment hdoAddFragment = HdoAddFragment.newInstance();
        FragmentChange.replace(requireActivity(), hdoAddFragment, FragmentChange.Transaction.MOVE, true);
    }

}
