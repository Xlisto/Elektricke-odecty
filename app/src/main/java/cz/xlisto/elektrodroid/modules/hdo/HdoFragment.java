package cz.xlisto.elektrodroid.modules.hdo;


import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.transition.AutoTransition;
import android.transition.TransitionManager;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.view.MenuHost;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

import cz.xlisto.elektrodroid.R;
import cz.xlisto.elektrodroid.databaze.DataHdoSource;
import cz.xlisto.elektrodroid.databaze.DataSettingsSource;
import cz.xlisto.elektrodroid.dialogs.TimeShiftDialogFragment;
import cz.xlisto.elektrodroid.services.HdoAlarmScheduler;
import cz.xlisto.elektrodroid.modules.settings.SettingsFragment;
import cz.xlisto.elektrodroid.dialogs.SubscriptionPointDialogFragment;
import cz.xlisto.elektrodroid.dialogs.YesNoDialogFragment;
import cz.xlisto.elektrodroid.format.SimpleDateFormatHelper;
import cz.xlisto.elektrodroid.models.HdoModel;
import cz.xlisto.elektrodroid.models.SubscriptionPointModel;
import cz.xlisto.elektrodroid.shp.ShPHdo;
import cz.xlisto.elektrodroid.utils.FragmentChange;
import cz.xlisto.elektrodroid.utils.NotificationHelper;
import cz.xlisto.elektrodroid.utils.SubscriptionPoint;
import cz.xlisto.elektrodroid.utils.UIHelper;


/**
 * Fragment pro zobrazení a správu HDO (Doba Sníženého Tarifu) časů.
 *
 * <p></>Tento fragment umožňuje:
 * - Zobrazit aktuální čas a HDO období
 * - Spravovat časový posun
 * - Nastavit upozornění na začátek/konec HDO
 * - Vybírat a filtrovat HDO podle relé
 * - Zobrazit historii HDO záznamů
 *
 * <p></>Využívá {@link DataHdoSource} pro přístup k datům HDO.
 *
 * @author Xlisto
 * @version 2.0 (modernizováno s ActivityResultContracts)
 * @since 26.05.2023
 */
public class HdoFragment extends Fragment {

    private Timer timer;
    private SubscriptionPointModel subscriptionPoint;
    private TextView tvTimeHdo, tvTimeDifference, tvAlertHdo, tvDateHdo;
    private LinearLayout llRelaysStatusContainer;
    private ImageView imageViewIconNT;
    private Spinner spReleSettings;
    private RecyclerView rvHdo;
    private long idSubscriptionPoint, timeDifferent;
    private ArrayList<HdoModel> hdoModels = new ArrayList<>();
    private HdoAdapter hdoAdapter;
    private Button btnAddHdo;
    private FloatingActionButton fab;

    //překreslení gui
    private final Runnable timerTick = this::setTime;


    /**
     * Vytvoří novou instanci fragmentu HDO.
     *
     * @return Nová instance HdoFragment
     */
    public static HdoFragment newInstance() {
        return new HdoFragment();
    }


    /**
     * Vytvoří root View fragmentu.
     *
     * @param inflater           LayoutInflater pro vytvoření layoutu
     * @param container          Rodičovský kontejner
     * @param savedInstanceState Uložený stav instance (může být null)
     * @return Kořenový View fragmentu
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        MenuHost menuHost = requireActivity();
        menuHost.addMenuProvider(new MenuProvider() {
            @Override
            public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
                menuInflater.inflate(R.menu.menu_hdo, menu);
            }


            @Override
            public boolean onMenuItemSelected(@NonNull MenuItem menuItem) {
                if (menuItem.getItemId() == R.id.menu_hdo_load) {
                    HdoSiteFragment hdoSite = HdoSiteFragment.newInstance();
                    FragmentChange.replace(requireActivity(), hdoSite, FragmentChange.Transaction.MOVE, true);
                    return true;
                } else if (menuItem.getItemId() == R.id.menu_hdo_time_shift) {
                    if (idSubscriptionPoint != -1L) {
                        TimeShiftDialogFragment dialog = TimeShiftDialogFragment.newInstance(idSubscriptionPoint, timeDifferent);
                        dialog.show(requireActivity().getSupportFragmentManager(), "TimeShiftDialogFragment");
                    }
                    return true;
                }
                return false;
            }
        }, getViewLifecycleOwner(), Lifecycle.State.RESUMED);

        return inflater.inflate(R.layout.fragment_hdo, container, false);
    }


    /**
     * Inicializuje UI prvky, nastavuje posluchače a registruje ActivityResultLauncher.
     *
     * @param view               Kořenový view fragmentu
     * @param savedInstanceState Uložený stav instance (může být null)
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        requireActivity().invalidateOptionsMenu();

        tvTimeHdo = view.findViewById(R.id.tvTimeHdo);
        tvTimeDifference = view.findViewById(R.id.tvTimeDifference);
        tvAlertHdo = view.findViewById(R.id.tvAlertHdo);
        tvDateHdo = view.findViewById(R.id.tvHdoDate);
        llRelaysStatusContainer = view.findViewById(R.id.llRelaysStatusContainer);
        rvHdo = view.findViewById(R.id.rvHdo);
        rvHdo.setItemAnimator(null);
        new ShPHdo(requireContext()).set(ShPHdo.ARG_RUNNING_SERVICE, false);
        spReleSettings = view.findViewById(R.id.spReleSettings);
        imageViewIconNT = view.findViewById(R.id.imageViewIconNT);
        fab = view.findViewById(R.id.fabHdo);
        btnAddHdo = view.findViewById(R.id.btnAddHdo);

        ImageView btnMinimizeHdo = view.findViewById(R.id.btnMinimizeHdo);
        LinearLayout llHdoClockDetails = view.findViewById(R.id.llHdoClockDetails);

        ShPHdo shPHdo = new ShPHdo(requireContext());
        boolean isMinimized = shPHdo.get(ShPHdo.ARG_HDO_CLOCK_MINIMIZED, false);
        applyClockMinimizedState(isMinimized);

        btnMinimizeHdo.setOnClickListener(v -> {
            boolean currentlyMinimized = llHdoClockDetails.getVisibility() == View.GONE;
            boolean newMinimized = !currentlyMinimized;
            shPHdo.set(ShPHdo.ARG_HDO_CLOCK_MINIMIZED, newMinimized);
            applyClockMinimizedState(newMinimized);
        });

        fab.setOnClickListener(v -> showAddDialog());
        btnAddHdo.setOnClickListener(v -> showAddDialog());

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
        requireActivity().getSupportFragmentManager().setFragmentResultListener(SettingsFragment.FLAG_UPDATE_SETTINGS_FOR_FRAGMENT, this,
                ((requestKey, result) -> UIHelper.showButtons(btnAddHdo, fab, requireActivity(), true)));

        //posluchač pro změnu časového posunu v dialogu
        requireActivity().getSupportFragmentManager().setFragmentResultListener("TIME_SHIFT_CHANGED", this, (requestKey, result) -> {
            if (idSubscriptionPoint != -1L) {
                DataSettingsSource dataSettingsSource = new DataSettingsSource(requireContext());
                dataSettingsSource.open();
                timeDifferent = dataSettingsSource.loadTimeShift(idSubscriptionPoint);
                dataSettingsSource.close();
                setTimeDifferent();
                if (subscriptionPoint != null && subscriptionPoint.getTableHDO() != null) {
                    HdoAlarmScheduler.rescheduleForTable(requireContext(), subscriptionPoint.getTableHDO());
                }
            }
        });
    }


    /**
     * Načte data při návratu do popředí a obnoví zobrazení.
     * Inicializuje timer pro aktualizaci času a vyžádá notifikační oprávnění.
     */
    @Override
    public void onResume() {
        super.onResume();
        UIHelper.showButtons(btnAddHdo, fab, requireActivity(), true);
        subscriptionPoint = SubscriptionPoint.load(requireActivity());
        if (subscriptionPoint != null)
            idSubscriptionPoint = subscriptionPoint.getId();
        else {
            idSubscriptionPoint = -1L;
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
        new ShPHdo(requireContext()).set(ShPHdo.ARG_RUNNING_SERVICE, false);
        ShPHdo shPHdo = new ShPHdo(requireContext());
        applyClockMinimizedState(shPHdo.get(ShPHdo.ARG_HDO_CLOCK_MINIMIZED, false));
        checkNotificationPermissionWarning();
    }


    /**
     * Zkontroluje, zda jsou nastavená HDO upozornění a jsou-li vypnuté notifikace, zobrazí varování.
     */
    private void checkNotificationPermissionWarning() {
        boolean hasNotification = false;
        for (HdoModel model : hdoModels) {
            if (model.getNotifyStart() == 1 || model.getNotifyEnd() == 1) {
                hasNotification = true;
                break;
            }
        }
        if (hasNotification && !NotificationHelper.isNotificationPermissionGranted(requireContext())) {
            NotificationHelper.showNotificationWarningSnackbar(getView(), getString(R.string.notification_disabled_warning));
        }
    }


    /**
     * Zastaví timer při přechodu fragmentu do pozadí.
     */
    @Override
    public void onPause() {
        super.onPause();
        endTimer();
    }


    /**
     * Spustí časovač pro aktualizaci zobrazení času.
     * Aktualizuje UI každou sekundu s aktuálním časem a HDO stavem.
     */
    private void startTimer() {
        endTimer();
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


    /**
     * Zastaví a zruší časovač, aby se zabránilo zbytečnému spotřebování paměti.
     */
    private void endTimer() {
        if (timer != null) {
            timer.cancel();
            timer.purge();
            timer = null;
        }
    }


    /**
     * Nastaví a zobrazí časový posun s korektní češtinou.
     * Pokud je časový posun 0, zobrazovaný prvek skryje.
     */
    private void setTimeDifferent() {
        if (tvTimeDifference == null) return;
        if (timeDifferent == 0L) {
            tvTimeDifference.setVisibility(View.GONE);
        } else {
            tvTimeDifference.setVisibility(View.VISIBLE);
            String hours = "hodin";
            long h = timeDifferent / 3600000;
            long absH = Math.abs(h);
            if (absH == 1) hours = "hodina";
            else if (absH >= 2 && absH <= 4) hours = "hodiny";

            String minutes = "minut";
            long m = (Math.abs(timeDifferent) % 3600000) / 60000;
            if (m == 1) minutes = "minuta";
            else if (m >= 2 && m <= 4) minutes = "minuty";

            tvTimeDifference.setText(String.format(Locale.GERMANY, "Rozdíl %01d %s a %02d %s ", h, hours, m, minutes));
        }
        setTime();
    }


    /**
     * Nastaví a aktualizuje čas, který se zobrazuje na displeji hodin.
     * Aplikuje časový posun a kontroluje, zda se jedná o HDO čas.
     */
    private void setTime() {
        if (!isAdded() || getView() == null) return;
        Calendar calendar = Calendar.getInstance();//získání aktuálního času
        calendar.setTimeInMillis(calendar.getTimeInMillis() + timeDifferent);//nastavení kalendáře na aktuální čas + časový posun
        long miliseconds = calendar.getTimeInMillis();//aktuální čas v milisekundách s časovým posunem
        if (tvTimeHdo != null) {
            tvTimeHdo.setText(SimpleDateFormatHelper.onlyTime.format(miliseconds).toUpperCase());
        }

        if (subscriptionPoint != null && subscriptionPoint.getTableHDO() != null && llRelaysStatusContainer != null && getContext() != null) {
            DataHdoSource dataHdoSource = new DataHdoSource(requireContext());
            dataHdoSource.open();
            ArrayList<String> allReles = dataHdoSource.getReles(subscriptionPoint.getTableHDO());

            ShPHdo shPHdo = new ShPHdo(requireContext());
            String selectedMainRelay = shPHdo.get(ShPHdo.ARG_MAIN_HDO_RELAY, "");

            if ((selectedMainRelay.isEmpty() || !allReles.contains(selectedMainRelay)) && !allReles.isEmpty()) {
                selectedMainRelay = allReles.get(0);
                shPHdo.set(ShPHdo.ARG_MAIN_HDO_RELAY, selectedMainRelay);
            }

            // Nastavení stavu hlavních hodin a žárovky podle zvoleného relé
            if (!selectedMainRelay.isEmpty()) {
                ArrayList<HdoModel> mainRelayModels = dataHdoSource.loadHdo(subscriptionPoint.getTableHDO(), null, selectedMainRelay);
                boolean isMainRelayActive = HdoTime.checkHdo(mainRelayModels, calendar);
                setTextHdoColor(isMainRelayActive);
            } else if (!hdoModels.isEmpty()) {
                boolean isHdo = HdoTime.checkHdo(hdoModels, calendar);
                setTextHdoColor(isHdo);
            }

            llRelaysStatusContainer.removeAllViews();

            if (!allReles.isEmpty()) {
                llRelaysStatusContainer.setVisibility(View.VISIBLE);
                for (String rele : allReles) {
                    ArrayList<HdoModel> releModels = dataHdoSource.loadHdo(subscriptionPoint.getTableHDO(), null, rele);
                    boolean isReleActive = HdoTime.checkHdo(releModels, calendar);
                    long nowMillis = System.currentTimeMillis();
                    long targetMillis;
                    String statusFormat;
                    boolean hasReleName = rele != null && !rele.trim().isEmpty();

                    if (isReleActive) {
                        targetMillis = HdoAlarmScheduler.findNextTriggerForModels(releModels, HdoAlarmScheduler.TYPE_END, nowMillis, timeDifferent);
                        String durationStr = formatDuration(targetMillis - nowMillis);
                        statusFormat = hasReleName
                                ? getString(R.string.hdo_relay_active_status, rele, durationStr)
                                : getString(R.string.hdo_active_status_no_relay, durationStr);
                    } else {
                        targetMillis = HdoAlarmScheduler.findNextTriggerForModels(releModels, HdoAlarmScheduler.TYPE_START, nowMillis, timeDifferent);
                        String durationStr = formatDuration(targetMillis - nowMillis);
                        statusFormat = hasReleName
                                ? getString(R.string.hdo_relay_inactive_status, rele, durationStr)
                                : getString(R.string.hdo_inactive_status_no_relay, durationStr);
                    }

                    View rowView = LayoutInflater.from(requireContext()).inflate(R.layout.item_relay_status, llRelaysStatusContainer, false);
                    RadioButton rbSelectMainRelay = rowView.findViewById(R.id.rbSelectMainRelay);
                    TextView tvStatus = rowView.findViewById(R.id.tvRelayStatusText);
                    View viewLed = rowView.findViewById(R.id.viewRelayLed);

                    tvStatus.setText(statusFormat);
                    assert rele != null;
                    rbSelectMainRelay.setChecked(rele.equals(selectedMainRelay));

                    View.OnClickListener onSelectRelayListener = v -> {
                        shPHdo.set(ShPHdo.ARG_MAIN_HDO_RELAY, rele);
                        setTime();
                    };

                    rbSelectMainRelay.setOnClickListener(onSelectRelayListener);
                    rowView.setOnClickListener(onSelectRelayListener);

                    GradientDrawable drawable = (GradientDrawable) viewLed.getBackground();
                    if (drawable != null) {
                        drawable.setColor(isReleActive ? ContextCompat.getColor(requireContext(), R.color.color_yes) : ContextCompat.getColor(requireContext(), android.R.color.darker_gray));
                    }
                    llRelaysStatusContainer.addView(rowView);
                }
            } else {
                llRelaysStatusContainer.setVisibility(View.GONE);
            }
            dataHdoSource.close();
        }
    }


    /**
     * Formátuje milisekundy na tvar HH:mm:ss.
     */
    private String formatDuration(long millis) {
        if (millis <= 0) return "00:00:00";
        long seconds = (millis / 1000) % 60;
        long minutes = (millis / (1000 * 60)) % 60;
        long hours = millis / (1000 * 60 * 60);
        return String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds);
    }


    /**
     * Nastaví barvu textu a ikonu podle toho, zda je aktuální čas v rozmezí HDO.
     * - Když je HDO: zelená barva a ikona "NT ON"
     * - Mimo HDO: sekundární barva podle noční módu a ikona "NT OFF"
     *
     * @param show {@code true} pokud je aktuální čas v HDO, {@code false} jinak
     */
    private void setTextHdoColor(boolean show) {
        if (isAdded()) {
            if (show) {
                tvTimeHdo.setTextColor(ContextCompat.getColor(requireContext(), R.color.color_yes));
                imageViewIconNT.setImageDrawable(ContextCompat.getDrawable(requireActivity(), R.drawable.nt_on));
            } else {
                tvTimeHdo.setTextColor(getTextColorSecondary());
                imageViewIconNT.setImageDrawable(ContextCompat.getDrawable(requireActivity(), R.drawable.nt_off));
            }
        }
    }


    /**
     * Vrátí sekundární barvu textu podle aktuálního tématu.
     */
    private int getTextColorSecondary() {
        TypedValue typedValue = new TypedValue();
        if (requireContext().getTheme().resolveAttribute(android.R.attr.textColorSecondary, typedValue, true)) {
            if (typedValue.resourceId != 0) {
                return ContextCompat.getColor(requireContext(), typedValue.resourceId);
            } else {
                return typedValue.data;
            }
        }
        return Color.GRAY;
    }


    /**
     * Zobrazí nebo skryje upozornění na nevybrané odběrné místo.
     * Pokud není vybrán žádný punkt, zobrazí varování uživateli.
     */
    private void showAlert() {
        if (idSubscriptionPoint == -1L) {
            tvAlertHdo.setVisibility(View.VISIBLE);
        } else {
            tvAlertHdo.setVisibility(View.GONE);
        }
    }


    /**
     * Načte všechna HDO data z databáze bez filtrování podle relé.
     */
    private void loadData() {
        loadData(null);
    }


    /**
     * Načte HDO data z databáze a nastaví adapter pro RecyclerView.
     * Volitelně filtruje data podle vybraného relé.
     * Aktualizuje i TextView s intervalem dat.
     *
     * @param rele Název relé pro filtrování, nebo {@code null} pro zobrazení všech
     */
    private void loadData(String rele) {
        DataHdoSource dataHdoSource = new DataHdoSource(requireActivity());
        hdoModels.clear();
        dataHdoSource.open();
        if (rele != null)
            hdoModels = dataHdoSource.loadHdo(subscriptionPoint.getTableHDO(), null, rele);
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
     * Načte seznam dostupných relé z databáze a nastaví spinner.
     * Spinner se zobrazí pouze pokud existuje více než jedno relé.
     * Umožňuje uživateli filtrovat HDO časy podle vybraného relé.
     */
    private void loadReles() {
        DataHdoSource dataHdoSource = new DataHdoSource(requireActivity());
        dataHdoSource.open();
        ArrayList<String> reles = dataHdoSource.getReles(subscriptionPoint.getTableHDO());
        dataHdoSource.close();
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireActivity(), android.R.layout.simple_spinner_dropdown_item, reles);
        spReleSettings.setAdapter(adapter);
        if (reles.size() > 1)
            spReleSettings.setVisibility(View.VISIBLE);
        else
            spReleSettings.setVisibility(View.GONE);
    }


    /**
     * Vytvoří a nastaví adapter pro RecyclerView s HDO daty.
     * Také nastavuje LinearLayoutManager pro vertikální seznam.
     * <p>
     * V tomto fragmentu se používá interaktivní režim adapteru ({@code clickables=true}),
     * takže jsou viditelné checkboxy pro nastavení notifikací při změně NT i akce editace/mazání.
     */
    private void setAdapter() {
        hdoAdapter = new HdoAdapter(hdoModels, rvHdo, true);
        rvHdo.setAdapter(hdoAdapter);
        rvHdo.setLayoutManager(new LinearLayoutManager(requireActivity()));
        rvHdo.scheduleLayoutAnimation();
    }


    /**
     * Zavolá metodu v adapteru pro odstranění vybraného HDO záznamu z databáze.
     */
    private void deleteHdo() {
        hdoAdapter.deleteItem();
    }


    /**
     * Zobrazí dialog pro přidání nového HDO záznamu.
     * Nahradí aktuální fragment fragmentem {@link HdoAddFragment}.
     */
    private void showAddDialog() {
        HdoAddFragment hdoAddFragment = HdoAddFragment.newInstance();
        FragmentChange.replace(requireActivity(), hdoAddFragment, FragmentChange.Transaction.MOVE, true);
    }


    /**
     * Aplikuje stav minimalizace/maximalizace HDO hodinové karty s plynulou animací.
     */
    private void applyClockMinimizedState(boolean minimized) {
        if (getView() instanceof ViewGroup) {
            TransitionManager.beginDelayedTransition((ViewGroup) getView(), new AutoTransition());
        }

        assert getView() != null;
        ImageView btnMinimizeHdo = getView().findViewById(R.id.btnMinimizeHdo);
        LinearLayout llHdoClockDetails = getView().findViewById(R.id.llHdoClockDetails);
        if (btnMinimizeHdo == null || llHdoClockDetails == null) return;

        if (minimized) {
            llHdoClockDetails.setVisibility(View.GONE);
            btnMinimizeHdo.setImageResource(R.drawable.ic_expand_less_24);
            btnMinimizeHdo.setContentDescription(getString(R.string.maximize));
        } else {
            llHdoClockDetails.setVisibility(View.VISIBLE);
            btnMinimizeHdo.setImageResource(R.drawable.ic_expand_more_24);
            btnMinimizeHdo.setContentDescription(getString(R.string.minimize));
        }
    }



}
