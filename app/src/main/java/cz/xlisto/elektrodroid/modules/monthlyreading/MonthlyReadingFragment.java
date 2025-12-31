package cz.xlisto.elektrodroid.modules.monthlyreading;


import static cz.xlisto.elektrodroid.shp.ShPMonthlyReading.REGUL_PRICE;
import static cz.xlisto.elektrodroid.shp.ShPMonthlyReading.SHORT_LIST;
import static cz.xlisto.elektrodroid.utils.FragmentChange.Transaction.MOVE;

import android.content.Context;
import android.os.Bundle;
import android.os.Parcelable;
import android.transition.TransitionManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.MenuHost;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.switchmaterial.SwitchMaterial;

import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;

import cz.xlisto.elektrodroid.R;
import cz.xlisto.elektrodroid.databaze.DataInvoiceSource;
import cz.xlisto.elektrodroid.databaze.DataPriceListSource;
import cz.xlisto.elektrodroid.databaze.DataSubscriptionPointSource;
import cz.xlisto.elektrodroid.dialogs.SettingsViewDialogFragment;
import cz.xlisto.elektrodroid.dialogs.SubscriptionPointDialogFragment;
import cz.xlisto.elektrodroid.dialogs.YesNoDialogFragment;
import cz.xlisto.elektrodroid.models.MonthlyReadingModel;
import cz.xlisto.elektrodroid.models.PriceListModel;
import cz.xlisto.elektrodroid.models.SubscriptionPointModel;
import cz.xlisto.elektrodroid.modules.backup.SaveDataToBackupFile;
import cz.xlisto.elektrodroid.ownview.ViewHelper;
import cz.xlisto.elektrodroid.shp.ShPMonthlyReading;
import cz.xlisto.elektrodroid.utils.DetectScreenMode;
import cz.xlisto.elektrodroid.utils.FragmentChange;
import cz.xlisto.elektrodroid.utils.SubscriptionPoint;
import cz.xlisto.elektrodroid.utils.UIHelper;


/**
 * Fragment pro zobrazení měsíčních odečtů.
 */
public class MonthlyReadingFragment extends Fragment {

    public final String TAG = "MonthlyReadingFragment";
    private final String TO = "to";
    private final String FROM = "from";
    private final String ID_CURRENTLY_READING = "idCurrentlyReading";
    private final String ID_PREVIOUS_READING = "idPreviousReading";
    private final String FLAG_RESULT_DIALOG_FRAGMENT_2024 = "2024";
    private final String FLAG_RESULT_DIALOG_FRAGMENT_2025 = "2025";
    private SubscriptionPointModel subscriptionPoint;
    private FloatingActionButton fab;
    private Button btnAddMonthlyReading;
    private TextView tvAlert, tvMonthlyReadingFilter;
    private RecyclerView rv;
    private SwitchMaterial swSimplyView, swRegulPrice;
    private ShPMonthlyReading shPMonthlyReading;
    private MonthlyReadingAdapter monthlyReadingAdapter;
    private long from = 0;
    private long to = Long.MAX_VALUE;
    private long idCurrentlyReading = -1, idPreviousReading = -1;
    private MonthlyReadingAdapter.OnClickItemListener onClickItemListener;
    private OnShowRegulPriceListener onShowRegulPriceListener;


    public MonthlyReadingFragment() {
        // Required empty public constructor
    }


    public static MonthlyReadingFragment newInstance() {
        return new MonthlyReadingFragment();
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //posluchač potvrzení smazání záznamu
        requireActivity().getSupportFragmentManager().setFragmentResultListener(MonthlyReadingAdapter.FLAG_DELETE_MONTHLY_READING, this,
                (requestKey, result) -> {
                    if (result.getBoolean(YesNoDialogFragment.RESULT)) {
                        monthlyReadingAdapter.deleteMonthlyReading(requireContext());
                    }
                    setShowTvAlert();
                });

        //posluchač změny filtru
        requireActivity().getSupportFragmentManager().setFragmentResultListener(MonthlyReadingFilterDialogFragment.MONTHLY_READING_FILTER, this,
                (requestKey, result) -> {
                    to = result.getLong(MonthlyReadingFilterDialogFragment.TO);
                    from = result.getLong(MonthlyReadingFilterDialogFragment.FROM);
                    from = from + ViewHelper.getOffsetTimeZones(from);
                    tvMonthlyReadingFilter.setVisibility(View.VISIBLE);
                    tvMonthlyReadingFilter.setText(getResources().getString(R.string.show_period, ViewHelper.convertLongToDate(from), ViewHelper.convertLongToDate(to)));
                    if (from == 0 && to == Long.MAX_VALUE) {
                        tvMonthlyReadingFilter.setVisibility(View.GONE);
                    } else {
                        tvMonthlyReadingFilter.setVisibility(View.VISIBLE);
                    }
                    loadDataFromDatabase();
                });

        //posluchač změny odběrného místa
        requireActivity().getSupportFragmentManager().setFragmentResultListener(SubscriptionPointDialogFragment.FLAG_UPDATE_SUBSCRIPTION_POINT, this,
                (requestKey, result) -> {
                    loadDataFromDatabase();
                    UIHelper.showButtons(btnAddMonthlyReading, fab, requireActivity(), true);
                });

        //posluchač změny měsíčního odečtu - zobrazení detailu v land režimu
        onClickItemListener = (idCurrentlyReading, idPreviousReading) -> {
            this.idCurrentlyReading = idCurrentlyReading;
            this.idPreviousReading = idPreviousReading;
            showDetailFragment(idCurrentlyReading, idPreviousReading);
        };

        //posluchač na zavření dialogového okna s nastavením
        requireActivity().getSupportFragmentManager().setFragmentResultListener(SettingsViewDialogFragment.FLAG_UPDATE_SETTINGS_FOR_FRAGMENT, this,
                (requestKey, result) -> {
                    UIHelper.showButtons(btnAddMonthlyReading, fab, requireActivity(), true);
                    loadDataFromDatabase();
                });

        //posluchač na zavření dialogového okna s upozorněním na změnu ceníku, které jsou přes 1.7.2024 platnosti
        getParentFragmentManager().setFragmentResultListener(FLAG_RESULT_DIALOG_FRAGMENT_2024, this,
                (requestKey, result) -> {
                    if (result.getBoolean(YesNoDialogFragment.RESULT)) {
                        //záloha
                        SaveDataToBackupFile.saveToZip(requireActivity(), null);
                        //aktualizace měsíčních odečtů
                        MonthlyReadingUpdater.updateMonthlyReadings(requireContext(), updatePriceList(PriceListSplitRange.YEAR_2024), PriceListSplitRange.YEAR_2024.getNewStart());
                        loadDataFromDatabase();
                    }
                });

        //posluchač na zavření dialogového okna s upozorněním na změnu ceníku, které jsou přes 1.9.2025 platnosti
        getParentFragmentManager().setFragmentResultListener(FLAG_RESULT_DIALOG_FRAGMENT_2025, this,
                (requestKey, result) -> {
                    if (result.getBoolean(YesNoDialogFragment.RESULT)) {
                        //záloha
                        SaveDataToBackupFile.saveToZip(requireActivity(), null);
                        //aktualizace měsíčních odečtů
                        MonthlyReadingUpdater.updateMonthlyReadings(requireContext(), updatePriceList(PriceListSplitRange.YEAR_2025), PriceListSplitRange.YEAR_2025.getNewStart());
                        loadDataFromDatabase();
                    }
                });
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        MenuHost menuHost = requireActivity();
        menuHost.addMenuProvider(new MenuProvider() {
            @Override
            public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
                menuInflater.inflate(R.menu.menu_monthly_reading, menu);
            }


            @Override
            public boolean onMenuItemSelected(@NonNull MenuItem menuItem) {
                if (menuItem.getItemId() == R.id.menu_filter_monthly_reading) {
                    MonthlyReadingFilterDialogFragment monthlyReadingFilterDialogFragment = MonthlyReadingFilterDialogFragment.newInstance(from, to);
                    monthlyReadingFilterDialogFragment.show(requireActivity().getSupportFragmentManager(), "MonthlyReadingFilterDialogFragment");
                    return true;
                }

                return false;
            }
        }, getViewLifecycleOwner(), Lifecycle.State.RESUMED);

        return inflater.inflate(R.layout.fragment_monthly_reading, container, false);
    }


    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (savedInstanceState != null) {
            from = savedInstanceState.getLong(FROM);
            to = savedInstanceState.getLong(TO);
            idCurrentlyReading = savedInstanceState.getLong(ID_CURRENTLY_READING);
            idPreviousReading = savedInstanceState.getLong(ID_PREVIOUS_READING);
        }
        shPMonthlyReading = new ShPMonthlyReading(requireActivity());
        fab = view.findViewById(R.id.fab);
        btnAddMonthlyReading = view.findViewById(R.id.btnAddMonthlyReading);
        swSimplyView = view.findViewById(R.id.swSimplyView);
        swRegulPrice = view.findViewById(R.id.swRegulPrice);
        rv = view.findViewById(R.id.rvMonthlyReading);
        tvAlert = view.findViewById(R.id.tvAlertMonthlyReading);
        tvMonthlyReadingFilter = view.findViewById(R.id.tvMonthlyReadingFilter);
        swSimplyView.setChecked(shPMonthlyReading.get(SHORT_LIST, false));
        swRegulPrice.setChecked(shPMonthlyReading.get(REGUL_PRICE, false));
        swSimplyView.setOnCheckedChangeListener((buttonView, isChecked) -> {
            TransitionManager.beginDelayedTransition(rv);
            if (rv.getLayoutManager() != null) {
                Parcelable out = Objects.requireNonNull(rv.getLayoutManager()).onSaveInstanceState();
                shPMonthlyReading.set(SHORT_LIST, swSimplyView.isChecked());
                monthlyReadingAdapter.showSimpleView(isChecked);
                rv.getLayoutManager().onRestoreInstanceState(out);
            }
        });
        swRegulPrice.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (rv != null) {
                TransitionManager.beginDelayedTransition(rv);
                if (rv.getLayoutManager() != null) {
                    Parcelable out = Objects.requireNonNull(rv.getLayoutManager()).onSaveInstanceState();
                    shPMonthlyReading.set(REGUL_PRICE, swRegulPrice.isChecked());
                    monthlyReadingAdapter.setShowRegulPrice(isChecked);
                    rv.getLayoutManager().onRestoreInstanceState(out);
                    setOnShowRegulPriceListener(isChecked);
                }
            }
        });
        fab.setOnClickListener(v -> addMonthlyReading());
        btnAddMonthlyReading.setOnClickListener(v -> addMonthlyReading());
        showDetailFragment(idCurrentlyReading, idPreviousReading);
    }


    @Override
    public void onResume() {
        super.onResume();
        promptPriceListUpdateIfNeeded();

        loadDataFromDatabase();
        UIHelper.showButtons(btnAddMonthlyReading, fab, requireActivity(), true);
        if (rv.getLayoutManager() == null) {
            swRegulPrice.setEnabled(false);
            swSimplyView.setEnabled(false);
        } else {
            swRegulPrice.setEnabled(true);
            swSimplyView.setEnabled(true);
        }

    }


    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putLong(FROM, from);
        outState.putLong(TO, to);
        outState.putLong(ID_CURRENTLY_READING, idCurrentlyReading);
        outState.putLong(ID_PREVIOUS_READING, idPreviousReading);
    }


    /**
     * Načte měsíční odečty z databáze a zobrazí je v RecyclerView.
     */
    private void loadDataFromDatabase() {
        subscriptionPoint = SubscriptionPoint.load(requireActivity());
        ArrayList<MonthlyReadingModel> monthlyReadings;
        if (subscriptionPoint != null) {
            DataSubscriptionPointSource dataSubscriptionPointSource = new DataSubscriptionPointSource(requireActivity());
            dataSubscriptionPointSource.open();
            monthlyReadings = dataSubscriptionPointSource.loadMonthlyReadings(subscriptionPoint.getTableO(), from, to);
            dataSubscriptionPointSource.close();
            DataInvoiceSource dataInvoiceSource = new DataInvoiceSource(requireActivity());
            dataInvoiceSource.open();
            if (!dataInvoiceSource.checkInvoiceExists(subscriptionPoint.getTableTED()))
                dataInvoiceSource.insertFirstRecordWithoutInvoice(subscriptionPoint.getTableTED());
            dataInvoiceSource.close();
            monthlyReadingAdapter = new MonthlyReadingAdapter(monthlyReadings, subscriptionPoint,
                    swSimplyView.isChecked(), swRegulPrice.isChecked(), rv);
            monthlyReadingAdapter.setStateRestorationPolicy(RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY);
            monthlyReadingAdapter.setOnClickItemListener(onClickItemListener);
            rv.setAdapter(monthlyReadingAdapter);
            rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        }
        setShowTvAlert();
    }


    /**
     * Zobrazí detailní fragment měsíčního odečtu v závislosti na aktuálním a předchozím odečtu.
     * Pokud je zařízení v režimu na šířku (landscape), nahradí obsah fragmentu detailním fragmentem.
     * Pokud jsou oba identifikátory odečtů platné (>= 0), zobrazí detailní fragment.
     * Pokud nejsou platné, nahradí obsah prázdným fragmentem.
     *
     * @param idCurrentlyReading ID aktuálního měsíčního odečtu
     * @param idPreviousReading  ID předchozího měsíčního odečtu
     */
    private void showDetailFragment(long idCurrentlyReading, long idPreviousReading) {
        if (DetectScreenMode.isLandscape(requireActivity())) {
            MonthlyReadingDetailFragment monthlyReadingDetailFragment = MonthlyReadingDetailFragment.newInstance(idCurrentlyReading, idPreviousReading, swRegulPrice.isChecked());
            if (idCurrentlyReading >= 0 && idPreviousReading >= 0)
                getChildFragmentManager().beginTransaction()
                        .replace(R.id.fragmentContainerViewDetail, monthlyReadingDetailFragment, MonthlyReadingDetailFragment.TAG)
                        .commit();
            else {
                getChildFragmentManager().beginTransaction()
                        .replace(R.id.fragmentContainerViewDetail, new Fragment())
                        .commit();
            }
        }
    }


    /**
     * Zobrazí fragment pro přidání měsíčního odečtu.
     */
    private void addMonthlyReading() {
        MonthlyReadingAddFragment monthlyReadingAddFragment = MonthlyReadingAddFragment
                .newInstance(subscriptionPoint.getTableO(), subscriptionPoint.getTablePLATBY());
        FragmentChange.replace(requireActivity(), monthlyReadingAddFragment, MOVE, true);
    }


    /**
     * Zobrazí nebo skryje textové upozornění v závislosti na počtu položek v adapteru.
     * Pokud adapter neobsahuje žádné položky, zobrazí textové upozornění.
     * Pokud adapter obsahuje alespoň jednu položku, skryje textové upozornění.
     */
    private void setShowTvAlert() {
        if (subscriptionPoint == null || monthlyReadingAdapter.getItemCount() == 0) {
            tvAlert.setVisibility(View.VISIBLE);
            if (subscriptionPoint == null) {
                tvAlert.setText(getResources().getString(R.string.create_place));
            } else {
                tvAlert.setText(getResources().getString(R.string.pridejte_mesicni_odecty));
            }
        } else {
            tvAlert.setVisibility(View.GONE);
        }
    }


    /**
     * Rozhraní pro posluchače, který reaguje na změnu zobrazení regulované ceny.
     */
    public interface OnShowRegulPriceListener {

        /**
         * Metoda volaná při změně zobrazení regulované ceny.
         *
         * @param showRegulPrice boolean hodnota určující, zda zobrazit regulovanou cenu
         */
        void onShowRegulPrice(boolean showRegulPrice);

    }


    /**
     * Připojí fragment k aktivitě a nastaví posluchače pro zobrazení regulované ceny.
     *
     * @param context Kontext aktivity, ke které je fragment připojen
     * @throws ClassCastException pokud aktivita neimplementuje rozhraní OnShowRegulPriceListener
     */
    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        try {
            onShowRegulPriceListener = (OnShowRegulPriceListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context + " must implement OnShowRegulPriceListener");
        }
    }


    /**
     * Asynchronně zkontroluje v databázi přítomnost ceníků spadajících do
     * předdefinovaných rozsahů (aktuálně YEAR_2024 a YEAR_2025) a při nálezu
     * nabídne uživateli dialog pro jejich aktualizaci.
     * <p>
     * Implementační poznámky:
     * - Používá `requireContext()` a běží v novém pozadí vlákně; po ukončení
     * přepne zpět na UI vlákno pomocí `requireActivity().runOnUiThread(...)`.
     * - Otevírá a vždy uzavírá `DataPriceListSource` ve `finally` bloku.
     * - Kontroluje nejprve rozsah YEAR_2024 a v případě nalezení zobrazí dialog
     * s flagem `FLAG_RESULT_DIALOG_FRAGMENT_2024`; jinak kontroluje YEAR_2025.
     * - Výjimky při práci s DB jsou zalogovány; metoda může vyhodit
     * `IllegalStateException`, pokud fragment není připojen (volání `requireContext()`/`requireActivity()`).
     */
    private void promptPriceListUpdateIfNeeded() {
        final Context context = requireContext();

        new Thread(() -> {
            DataPriceListSource dataPriceListSource = new DataPriceListSource(context);
            boolean has2024 = false, has2025 = false;
            try {
                dataPriceListSource.open();

                has2024 = !dataPriceListSource.readPriceListInDateRange(
                        PriceListSplitRange.YEAR_2024.getOldStart(),
                        PriceListSplitRange.YEAR_2024.getOldEnd(),
                        PriceListSplitRange.YEAR_2024.getNewStart(),
                        PriceListSplitRange.YEAR_2024.getNewEnd()
                ).isEmpty();
                has2025 = !dataPriceListSource.readPriceListInDateRange(
                        PriceListSplitRange.YEAR_2025.getOldStart(),
                        PriceListSplitRange.YEAR_2025.getOldEnd(),
                        PriceListSplitRange.YEAR_2025.getNewStart(),
                        PriceListSplitRange.YEAR_2025.getNewEnd()
                ).isEmpty();
            } catch (Exception e) {
                Log.e(TAG, "promptPriceListUpdateIfNeeded: error while checking price lists", e);
            } finally {
                try {
                    dataPriceListSource.close();
                } catch (Exception ignored) {
                }
            }
            boolean finalHas2024 = has2024;
            boolean finalHas2025 = has2025;

            requireActivity().runOnUiThread(() -> {
                if (finalHas2024) {
                    YesNoDialogFragment yesNoDialogFragment = YesNoDialogFragment.newInstance(
                            getString(R.string.alert_title),
                            FLAG_RESULT_DIALOG_FRAGMENT_2024,
                            getString(R.string.alert_message_provoz_nesitove_infrastruktury2));
                    yesNoDialogFragment.show(requireActivity().getSupportFragmentManager(), YesNoDialogFragment.TAG);
                } else if (finalHas2025) {
                    YesNoDialogFragment yesNoDialogFragment = YesNoDialogFragment.newInstance(
                            getString(R.string.alert_title),
                            FLAG_RESULT_DIALOG_FRAGMENT_2025,
                            getString(R.string.alert_message_provoz_nesitove_infrastruktury_new_price2));
                    yesNoDialogFragment.show(requireActivity().getSupportFragmentManager(), YesNoDialogFragment.TAG);
                }
            });
        }).start();
    }


    /**
     * Aktualizuje ceník rozdělením podle zadaného rozsahu.
     * <p>
     * Metoda otevře lokální zdroj {@code DataPriceListSource}, provede rozdělení
     * voláním {@code range.splitUsing(source)} a zdroj vždy uzavře ve {@code finally}.
     * V případě jakékoli výjimky je chyba zalogována a metoda vrací {@code null}.
     *
     * @param range rozsah rozdělení ({@link PriceListSplitRange})
     * @return mapu původního a nového {@link PriceListModel} po rozdělení,
     * nebo {@code null}, pokud došlo k chybě
     * @throws IllegalStateException pokud fragment není připojen a volání {@code requireContext()} selže
     * @see DataPriceListSource
     * @see PriceListSplitRange#splitUsing(DataPriceListSource)
     */
    private Map<PriceListModel, PriceListModel> updatePriceList(PriceListSplitRange range) {
        DataPriceListSource dataPriceListSource = new DataPriceListSource(requireContext());
        try {
            dataPriceListSource.open();
            return range.splitUsing(dataPriceListSource);
        } catch (Exception e) {
            Log.e(TAG, "updatePriceList: error while updating price list", e);
            return null;
        } finally {
            try {
                dataPriceListSource.close();
            } catch (Exception ignored) {
            }
        }
    }


    /**
     * Nastaví posluchače pro zobrazení regulované ceny.
     *
     * @param isChecked boolean hodnota určující, zda zobrazit regulovanou cenu
     */
    public void setOnShowRegulPriceListener(boolean isChecked) {
        this.onShowRegulPriceListener.onShowRegulPrice(isChecked);
    }


    /**
     * Rozsah použitelný pro rozdělení ceníku na staré a nové období.
     * <p>
     * Enum definuje přednastavené rozsahy (např. pro 2024 a 2025) a uchovává
     * hranice starého a nového období ve formě řetězců. Slouží jako jedna sada
     * parametrů, které se předávají do metody pro rozdělení ceníku.
     * <p>
     * Důležité:
     * - `oldStart`/`oldEnd` popisují původní období, které bude rozdělěno.
     * - `newStart`/`newEnd` popisují začátek a konec nového období po rozdělení.
     * - {@link #splitUsing(DataPriceListSource)} poskytuje pohodlné volání
     * {@link DataPriceListSource#splitPriceList(String, String, String, String)}.
     *
     * @see DataPriceListSource#splitPriceList(String, String, String, String)
     * @see #splitUsing(DataPriceListSource)
     */
    public enum PriceListSplitRange {
        YEAR_2024("1.1.2024", "30.06.2024", "1.7.2024", "31.12.2024"),
        YEAR_2025("1.1.2025", "31.08.2025", "1.9.2025", "31.12.2025");

        private final String oldStart;
        private final String oldEnd;
        private final String newStart;
        private final String newEnd;


        PriceListSplitRange(String oldStart, String oldEnd, String newStart, String newEnd) {
            this.oldStart = oldStart;
            this.oldEnd = oldEnd;
            this.newStart = newStart;
            this.newEnd = newEnd;
        }


        public String getOldStart() {
            return oldStart;
        }


        public String getOldEnd() {
            return oldEnd;
        }


        public String getNewStart() {
            return newStart;
        }


        public String getNewEnd() {
            return newEnd;
        }


        /**
         * Provede rozdělení ceníku podle tohoto rozsahu voláním
         * {@link DataPriceListSource#splitPriceList(String, String, String, String)}.
         * <p>
         * Deleguje logiku na předaný {@code source} a vrací výslednou mapu,
         * kde klíčem je původní {@link PriceListModel} a hodnotou odpovídající nový {@link PriceListModel}.
         * <p>
         * Poznámky:
         * - {@code source} by měl být otevřený (voláno {@code open()}) před tímto voláním, pokud je to potřeba.
         * - V závislosti na implementaci zdroje metoda může vrátit prázdnou mapu, {@code null}
         * nebo vyvolat výjimku při chybě přístupu k databázi.
         *
         * @param source zdroj dat pro ceníky, použitý k provedení rozdělení
         * @return mapa původního -> nového {@link PriceListModel}, nebo prázdná/mapa dle chování zdroje; může být i {@code null} při chybě
         */
        public Map<PriceListModel, PriceListModel> splitUsing(DataPriceListSource source) {
            return source.splitPriceList(oldStart, oldEnd, newStart, newEnd);
        }
    }

}