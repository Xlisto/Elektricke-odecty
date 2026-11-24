package cz.xlisto.elektrodroid;


import static cz.xlisto.elektrodroid.utils.FragmentChange.Transaction.ALPHA;
import static cz.xlisto.elektrodroid.utils.FragmentChange.Transaction.MOVE;

import android.content.Intent;
import android.content.res.Configuration;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.Bundle;
import android.view.Menu;
import android.graphics.drawable.GradientDrawable;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import eightbitlab.com.blurview.BlurView;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.navigation.NavigationView;

import java.util.Arrays;
import java.util.List;

import cz.xlisto.elektrodroid.modules.settings.SettingsFragment;
import cz.xlisto.elektrodroid.dialogs.SubscriptionPointDialogFragment;
import cz.xlisto.elektrodroid.modules.aboutme.AboutMeFragment;
import cz.xlisto.elektrodroid.modules.backup.BackupFragment;
import cz.xlisto.elektrodroid.modules.backup.GoogleDriveFragment;
import cz.xlisto.elektrodroid.modules.backup.PendingBackupUploadScheduler;
import cz.xlisto.elektrodroid.modules.dashboard.DashBoardFragment;
import cz.xlisto.elektrodroid.modules.dashboard.NewDashBoardFragment;
import cz.xlisto.elektrodroid.modules.exportimportpricelist.ExportPriceListFragment;
import cz.xlisto.elektrodroid.modules.exportimportpricelist.ImportPriceListFragment;
import cz.xlisto.elektrodroid.modules.help.HelpFragment;
import cz.xlisto.elektrodroid.modules.graphcolor.GraphColorFragment;
import cz.xlisto.elektrodroid.modules.graphmonth.GraphMonthFragment;
import cz.xlisto.elektrodroid.modules.hdo.HdoFragment;
import cz.xlisto.elektrodroid.modules.hdo.HdoUpdateHelper;
import cz.xlisto.elektrodroid.modules.hdo.PreCodesUpdateScheduler;
import cz.xlisto.elektrodroid.modules.invoice.InvoiceListFragment;
import cz.xlisto.elektrodroid.modules.monthlyreading.MonthlyReadingDetailFragment;
import cz.xlisto.elektrodroid.modules.monthlyreading.MonthlyReadingFragment;
import cz.xlisto.elektrodroid.models.SubscriptionPointModel;
import cz.xlisto.elektrodroid.modules.pricelist.PriceListCompareBoxFragment;
import cz.xlisto.elektrodroid.modules.pricelist.PriceListFragment;
import cz.xlisto.elektrodroid.modules.subscriptionpoint.SubscriptionPointFragment;
import cz.xlisto.elektrodroid.ownview.MyBottomNavigationView;
import cz.xlisto.elektrodroid.services.HdoNotice;
import cz.xlisto.elektrodroid.services.MonthlyReadingReminderNotice;
import cz.xlisto.elektrodroid.shp.ShPDashBoard;
import cz.xlisto.elektrodroid.shp.ShPHdo;
import cz.xlisto.elektrodroid.utils.AppReviewHelper;
import cz.xlisto.elektrodroid.utils.AppUpdateHelper;

import android.util.Log;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.IntentSenderRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import cz.xlisto.elektrodroid.shp.ShPMainActivity;
import cz.xlisto.elektrodroid.shp.ShPSettings;
import cz.xlisto.elektrodroid.utils.DetectScreenMode;
import cz.xlisto.elektrodroid.utils.FragmentChange;
import cz.xlisto.elektrodroid.utils.NetworkUtil;
import cz.xlisto.elektrodroid.utils.SubscriptionPoint;

import android.view.ViewGroup;

import androidx.core.graphics.ColorUtils;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.google.android.material.color.MaterialColors;

import cz.xlisto.elektrodroid.ui.FloatingNavManager;
import cz.xlisto.elektrodroid.ui.OnPaddingChangeListener;

/**
 * Hlavní aktivita aplikace Elektrodroid.
 * <p>
 * Spravuje:
 * - Navigaci mezi fragmenty pomocí spodního navigačního panelu a drawer menu
 * - Výběr a správu odběrných míst
 * - Sledování síťového připojení pro synchronizaci záloh
 * - Obnovení orientace obrazovky a ukládání stavu
 * - Zobrazení notifikací HDO
 * <p>
 * Implementuje {@link MonthlyReadingFragment.OnShowRegulPriceListener} pro
 * interakci s fragmentem měsíčního čtení.
 */
public class MainActivity extends AppCompatActivity implements MonthlyReadingFragment.OnShowRegulPriceListener {

    private static final String ACTUAL_FRAGMENT = "actualFragment";
    private static final String ACTUAL_SELECTED_ITEM_INDEX = "actualSelectedItemIndex";
    private Fragment actualFragment;
    private MyBottomNavigationView myBottomNavigationView;
    private NavigationView myNavigationView;
    private View floatingNavCard;
    private View floatingLeftNavCard;
    private FloatingNavManager floatingNavManager;
    private BlurView blurView;
    private int statusBarInsetTop = 0;
    private int navBarInsetBottom = 0;
    private int navBarInsetLeft = 0;
    private int navBarInsetRight = 0;
    private DrawerLayout drawerLayout;
    private ActionBarDrawerToggle drawerToggle;
    private Toolbar toolbar;
    private TextView toolbarTitle, toolbarSubtitle;
    private ShPMainActivity shPMainActivity;
    private ConnectivityManager connectivityManager;
    private ConnectivityManager.NetworkCallback pendingUploadNetworkCallback;
    private int orientation, selectedItemIndex = -1;
    private boolean secondClick = false;
    private boolean selectionNavigationActive = false;
    @Nullable
    private Runnable selectionNavigationAction;
    private AppUpdateHelper appUpdateHelper;


    /**
     * Inicializuje hlavní aktivitu, nastaví režim edge-to-edge, plovoucí navigaci, toolbar,
     * posluchače systémových insetů, sledování síťové konektivity a obnoví nebo zobrazí výchozí fragment.
     *
     * @param savedInstanceState Uložený stav aktivity při obnovení, nebo {@code null} při prvním spuštění
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setSystemBarColors(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT);
        setContentView(R.layout.activity_main);
        setupPendingUploadScheduler();
        schedulePendingBackupUploadIfNeeded();
        HdoUpdateHelper.checkHdoValidityAndShowDialogIfNeeded(this);
        PreCodesUpdateScheduler.schedulePreCodesUpdate(this);
        AppReviewHelper.checkAndPromptReview(this);

        ActivityResultLauncher<IntentSenderRequest> updateActivityResultLauncher = registerForActivityResult(
                new ActivityResultContracts.StartIntentSenderForResult(),
                result -> {
                    if (result.getResultCode() != RESULT_OK) {
                        Log.w("MainActivity", "Update flow failed code: " + result.getResultCode());
                    }
                }
        );
        appUpdateHelper = new AppUpdateHelper(this, updateActivityResultLauncher);
        appUpdateHelper.checkForUpdate(false);
        setConfiguration(getResources().getConfiguration());
        myBottomNavigationView = findViewById(R.id.myBottomNavigation);
        if (myBottomNavigationView != null) {
            ViewCompat.setOnApplyWindowInsetsListener(myBottomNavigationView, (v, insets) -> {
                v.setPadding(0, 0, 0, 0);
                if (v instanceof ViewGroup vg) {
                    for (int i = 0; i < vg.getChildCount(); i++) {
                        vg.getChildAt(i).setPadding(0, 0, 0, 0);
                    }
                }
                return insets;
            });
        }
        myNavigationView = findViewById(R.id.navigationView);
        floatingNavCard = findViewById(R.id.floatingNavCard);
        floatingLeftNavCard = findViewById(R.id.floatingLeftNavCard);
        floatingNavManager = new FloatingNavManager(this, myBottomNavigationView, floatingNavCard);
        setupBlurView();

        updateAppBarBackgroundAndSystemBars();

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.drawer_layout), (v, insets) -> {
            Insets sysBars = insets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
            statusBarInsetTop = sysBars.top;
            navBarInsetBottom = sysBars.bottom;
            navBarInsetLeft = sysBars.left;
            navBarInsetRight = sysBars.right;

            updateTopBarPadding();
            updateFloatingNavCardMargins();
            notifyPaddingChanged();
            return insets;
        });

        NavigationView navigationView = findViewById(R.id.nav_view);
        shPMainActivity = new ShPMainActivity(getApplicationContext());

        //Horní toolbar + zobrazení tlačítka
        toolbar = findViewById(R.id.toolbar);
        drawerLayout = findViewById(R.id.drawer_layout);
        toolbarTitle = toolbar.findViewById(R.id.toolbar_title);
        toolbarSubtitle = toolbar.findViewById(R.id.toolbar_subtitle);
        setSupportActionBar(toolbar);
        drawerToggle = new ActionBarDrawerToggle(this,
                drawerLayout, toolbar,
                R.string.ano,
                R.string.ano);
        drawerLayout.addDrawerListener(drawerToggle);
        drawerToggle.syncState();
        getSupportFragmentManager().addOnBackStackChangedListener(this::updateAppBarNavigationIcon);
        getSupportFragmentManager().registerFragmentLifecycleCallbacks(new FragmentManager.FragmentLifecycleCallbacks() {
            @Override
            public void onFragmentViewCreated(@NonNull FragmentManager fm, @NonNull Fragment f, @NonNull View v, @Nullable Bundle savedInstanceState) {
                if (f.getId() == R.id.fragmentContainerView) {
                    if (v.getBackground() == null) {
                        v.setBackgroundColor(MaterialColors.getColor(v, android.R.attr.colorBackground));
                    }
                    v.setClickable(true);
                }
            }
        }, false);

        //spodní lišta
        myBottomNavigationView.setOnItemSelectedListener(item -> {
            long itemId = item.getItemId();
            uncheckAllMenuItems(myNavigationView.getMenu());
            selectedItemIndex = -1;
            if (itemId == R.id.meni_dashboard) {
                navigationView.setCheckedItem(R.id.menu_dashboard);
                myNavigationView.setCheckedItem(myNavigationView.getMenu().getItem(0).setChecked(true));
                selectedItemIndex = 0;
                shPMainActivity.set(ACTUAL_FRAGMENT, R.id.meni_dashboard);
                actualFragment = createDashboardFragment();
                FragmentChange.replace(MainActivity.this, actualFragment, ALPHA);
                setToolbarTitle(getResources().getString(R.string.dashboard));
                return true;
            }
            if (itemId == R.id.meni_prices) {
                navigationView.setCheckedItem(R.id.menu_price_list);
                myNavigationView.setCheckedItem(myNavigationView.getMenu().getItem(1).setChecked(true));
                selectedItemIndex = 1;
                shPMainActivity.set(ACTUAL_FRAGMENT, R.id.meni_prices);
                actualFragment = PriceListFragment.newInstance(false, -1L);
                FragmentChange.replace(MainActivity.this, actualFragment, ALPHA);
                setToolbarTitle(getResources().getString(R.string.price_lists));
                return true;
            }
            if (itemId == R.id.meni_monthly_readings) {
                navigationView.setCheckedItem(R.id.menu_monthly_reads);
                myNavigationView.setCheckedItem(myNavigationView.getMenu().getItem(2).setChecked(true));
                selectedItemIndex = 2;
                shPMainActivity.set(ACTUAL_FRAGMENT, R.id.meni_monthly_readings);
                actualFragment = MonthlyReadingFragment.newInstance();
                FragmentChange.replace(MainActivity.this, actualFragment, ALPHA);
                setToolbarTitle(getResources().getString(R.string.month_reads));
                return true;
            }
            if (itemId == R.id.meni_subscription_points) {
                navigationView.setCheckedItem(R.id.menu_subscription_points);
                myNavigationView.setCheckedItem(myNavigationView.getMenu().getItem(3).setChecked(true));
                selectedItemIndex = 3;
                shPMainActivity.set(ACTUAL_FRAGMENT, R.id.meni_subscription_points);
                actualFragment = SubscriptionPointFragment.newInstance();
                FragmentChange.replace(MainActivity.this, actualFragment, ALPHA);
                setToolbarTitle(getResources().getString(R.string.subscription_points));
                return true;
            }
            if (itemId == R.id.meni_invoice) {
                navigationView.setCheckedItem(R.id.menu_invoices);
                myNavigationView.setCheckedItem(myNavigationView.getMenu().getItem(4).setChecked(true));
                selectedItemIndex = 4;
                shPMainActivity.set(ACTUAL_FRAGMENT, -1);
                actualFragment = InvoiceListFragment.newInstance();
                FragmentChange.replace(MainActivity.this, actualFragment, ALPHA);
                setToolbarTitle(getResources().getString(R.string.invoices));
                return true;
            }
            return itemId == R.id.meni_nothing;
        });

        //levá lišta
        myNavigationView.setNavigationItemSelectedListener(item -> {
            uncheckAllMenuItems(myNavigationView.getMenu());
            item.setChecked(true);
            long itemId = item.getItemId();
            if (itemId == R.id.meni_dashboard) {
                navigationView.setCheckedItem(R.id.menu_dashboard);
                myBottomNavigationView.setSelectedItemId(R.id.meni_dashboard);
                shPMainActivity.set(ACTUAL_FRAGMENT, R.id.meni_dashboard);
                actualFragment = createDashboardFragment();
                FragmentChange.replace(MainActivity.this, actualFragment, ALPHA);
                setToolbarTitle(getResources().getString(R.string.dashboard));
                return true;
            }
            if (itemId == R.id.meni_prices) {
                navigationView.setCheckedItem(R.id.menu_price_list);
                myBottomNavigationView.setSelectedItemId(R.id.meni_prices);
                shPMainActivity.set(ACTUAL_FRAGMENT, R.id.meni_prices);
                actualFragment = PriceListFragment.newInstance(false, -1L);
                FragmentChange.replace(MainActivity.this, actualFragment, ALPHA);
                setToolbarTitle(getResources().getString(R.string.price_lists));
                return true;
            }
            if (itemId == R.id.meni_monthly_readings) {
                navigationView.setCheckedItem(R.id.menu_monthly_reads);
                myBottomNavigationView.setSelectedItemId(R.id.meni_monthly_readings);
                shPMainActivity.set(ACTUAL_FRAGMENT, R.id.meni_monthly_readings);
                actualFragment = MonthlyReadingFragment.newInstance();
                FragmentChange.replace(MainActivity.this, actualFragment, ALPHA);
                setToolbarTitle(getResources().getString(R.string.month_reads));
                return true;
            }
            if (itemId == R.id.meni_subscription_points) {
                navigationView.setCheckedItem(R.id.menu_subscription_points);
                myBottomNavigationView.setSelectedItemId(R.id.meni_subscription_points);
                shPMainActivity.set(ACTUAL_FRAGMENT, R.id.meni_subscription_points);
                actualFragment = SubscriptionPointFragment.newInstance();
                FragmentChange.replace(MainActivity.this, actualFragment, ALPHA);
                setToolbarTitle(getResources().getString(R.string.subscription_points));
                return true;
            }
            if (itemId == R.id.meni_invoice) {
                navigationView.setCheckedItem(R.id.menu_invoices);
                myBottomNavigationView.setSelectedItemId(R.id.meni_invoice);
                shPMainActivity.set(ACTUAL_FRAGMENT, -1);
                actualFragment = InvoiceListFragment.newInstance();
                FragmentChange.replace(MainActivity.this, actualFragment, ALPHA);
                setToolbarTitle(getResources().getString(R.string.invoices));
                return true;
            }
            return itemId == R.id.meni_nothing;
        });

        //navigační menu
        navigationView.setNavigationItemSelectedListener(item -> {
            boolean b = false;
            long itemId = item.getItemId();
            if (itemId == R.id.menu_dashboard) {
                myBottomNavigationView.setSelectedItemId(R.id.meni_dashboard);
                actualFragment = createDashboardFragment();
                setToolbarTitle(getResources().getString(R.string.dashboard));
                selectedItemIndex = 0;
                b = true;
            }
            if (itemId == R.id.menu_price_list) {
                myBottomNavigationView.setSelectedItemId(R.id.meni_prices);
                actualFragment = PriceListFragment.newInstance(false, -1L);
                setToolbarTitle(getResources().getString(R.string.price_lists));
                selectedItemIndex = 1;
                b = true;
            }
            if (itemId == R.id.menu_compare_price_list) {
                uncheckedBottomNavigation();
                //actualFragment = PriceListCompareDetailFragment.newInstance();
                actualFragment = PriceListCompareBoxFragment.newInstance();
                setToolbarTitle(getResources().getString(R.string.compare_price_list));
                selectedItemIndex = 2;
                b = true;
            }
            if (itemId == R.id.menu_monthly_reads) {
                myBottomNavigationView.setSelectedItemId(R.id.meni_monthly_readings);
                actualFragment = MonthlyReadingFragment.newInstance();
                setToolbarTitle(getResources().getString(R.string.month_reads));
                selectedItemIndex = 3;
                b = true;
            }
            if (itemId == R.id.menu_subscription_points) {
                myBottomNavigationView.setSelectedItemId(R.id.meni_subscription_points);
                actualFragment = SubscriptionPointFragment.newInstance();
                selectedItemIndex = 4;
                b = true;
            }
            if (itemId == R.id.menu_invoices) {
                myBottomNavigationView.setSelectedItemId(R.id.meni_invoice);
                actualFragment = InvoiceListFragment.newInstance();
                setToolbarTitle(getResources().getString(R.string.invoices));
                selectedItemIndex = 5;
                b = true;
            }
            if (itemId == R.id.menu_hdo) {
                uncheckedBottomNavigation();
                actualFragment = HdoFragment.newInstance();
                setToolbarTitle(getResources().getString(R.string.hdo_times));
                selectedItemIndex = 6;
                b = true;
            }
            if (itemId == R.id.menu_backup) {
                uncheckedBottomNavigation();
                actualFragment = BackupFragment.newInstance();
                setToolbarTitle(getResources().getString(R.string.backup1));
                selectedItemIndex = 7;
                b = true;
            }
            if (itemId == R.id.menu_google_drive) {
                uncheckedBottomNavigation();
                actualFragment = GoogleDriveFragment.newInstance();
                setToolbarTitle(getResources().getString(R.string.google_drive));
                selectedItemIndex = 8;
                b = true;
            }
            if (itemId == R.id.menu_graph_month) {
                uncheckedBottomNavigation();
                actualFragment = GraphMonthFragment.newInstance();
                setToolbarTitle(getResources().getString(R.string.graph_month));
                selectedItemIndex = 9;
                b = true;
            }
            if (itemId == R.id.menu_graph_color) {
                uncheckedBottomNavigation();
                actualFragment = GraphColorFragment.newInstance();
                setToolbarTitle(getResources().getString(R.string.graph_color));
                selectedItemIndex = 10;
                b = true;
            }
            if (itemId == R.id.menu_import_price_list) {
                uncheckedBottomNavigation();
                actualFragment = ImportPriceListFragment.newInstance();
                setToolbarTitle(getResources().getString(R.string.import_price_list));
                selectedItemIndex = 11;
                b = true;
            }
            if (itemId == R.id.menu_export_price_list) {
                uncheckedBottomNavigation();
                actualFragment = ExportPriceListFragment.newInstance();
                setToolbarTitle(getResources().getString(R.string.export_price_list));
                selectedItemIndex = 12;
                b = true;
            }
            if (itemId == R.id.menu_help) {
                uncheckedBottomNavigation();
                actualFragment = HelpFragment.newInstance();
                setToolbarTitle(getResources().getString(R.string.help));
                selectedItemIndex = 13;
                b = true;
            }
            if (itemId == R.id.menu_about_me) {
                uncheckedBottomNavigation();
                actualFragment = AboutMeFragment.newInstance();
                setToolbarTitle(getResources().getString(R.string.about_me));
                selectedItemIndex = 14;
                b = true;
            }
            if (itemId == R.id.menu_rate_app) {
                AppReviewHelper.requestReview(MainActivity.this, true);
                b = true;
            }
            if (actualFragment != null)
                FragmentChange.replace(MainActivity.this, actualFragment, ALPHA);
            drawerLayout.closeDrawer(GravityCompat.START, true);
            return b;
        });
        if (handleHdoNotificationIntent(getIntent(), navigationView)) {
            return;
        }
        if (handleMonthlyReadingReminderIntent(getIntent(), navigationView)) {
            return;
        }
        if (savedInstanceState != null) {
            actualFragment = getSupportFragmentManager().getFragment(savedInstanceState, ACTUAL_FRAGMENT);
            selectedItemIndex = savedInstanceState.getInt(ACTUAL_SELECTED_ITEM_INDEX);
            if (selectedItemIndex >= 0 && selectedItemIndex < myNavigationView.getMenu().size())
                myNavigationView.setCheckedItem(myNavigationView.getMenu().getItem(selectedItemIndex).setChecked(true));
        } else {
            actualFragment = MonthlyReadingFragment.newInstance();
            //myBottomNavigationView.setSelectedItemId(shPMainActivity.get(ACTUAL_FRAGMENT, R.id.meni_monthly_readings));
            myBottomNavigationView.setSelectedItemId(R.id.meni_monthly_readings);
            myNavigationView.setCheckedItem(myNavigationView.getMenu().getItem(2).setChecked(true));
            shPMainActivity.set(ACTUAL_FRAGMENT, R.id.meni_monthly_readings);
            FragmentChange.replace(this, actualFragment, ALPHA);
        }
        startHdoService();

        /*
         * Akce kliknutí na tlačítko zpět
         * Pokud je otevřený drawer, zavře ho
         * Pokud je otevřený podřízený fragment, vrátí se o fragment zpět
         * Pokud je otevřený hlavní fragment a je poslední, ukončí aplikaci
         */
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                DrawerLayout drawer = findViewById(R.id.drawer_layout);
                if (drawer.isDrawerOpen(GravityCompat.START)) {
                    drawer.closeDrawer(GravityCompat.START);
                } else {
                    int pf = getSupportFragmentManager().getBackStackEntryCount();
                    if (pf > 0) {
                        getSupportFragmentManager().popBackStack();//vrácení o fragment zpět, když je nějaký
                    } else if (!secondClick) {
                        secondClick = true;
                        Toast.makeText(getApplication(), getResources().getString(R.string.alert_app_end), Toast.LENGTH_SHORT).show();
                        //new Handler(Looper.getMainLooper()).postDelayed(() -> secondClick = false, 2000);
                        drawerLayout.postDelayed(() -> secondClick = false, 2000);
                    } else {
                        finish();
                    }
                }
                invalidateOptionsMenu();
            }
        });
        setToolbarTitle(shPMainActivity.get(ShPMainActivity.PRIMARY_TITLE, getResources().getString(R.string.month_reads)));
        setToolbarSubtitle(shPMainActivity.get(ShPMainActivity.SECONDARY_TITLE, ""));

        //při rotaci se skryje detail měsíčního odečtu a zobrazí se seznam
        Fragment fragment = getSupportFragmentManager().findFragmentByTag("MonthlyReadingDetailFragment");
        if (fragment != null && DetectScreenMode.isLandscape(getApplicationContext())) {
            getSupportFragmentManager().popBackStack();
        }

        //posluchač na zavření dialogového okna s nastavením
        getSupportFragmentManager().setFragmentResultListener(SettingsFragment.FLAG_UPDATE_SETTINGS_FOR_ACTIVITY, this,
                (requestKey, result) -> setVisibilityNavigation());
        updateAppBarNavigationIcon();
    }


    /**
     * Zpracuje nový příchozí {@link Intent} u již běžící instance aktivity
     * (např. po kliknutí na notifikaci HDO nebo připomenutí měsíčního odečtu).
     *
     * @param intent Nový příchozí intent
     */
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        NavigationView navigationView = findViewById(R.id.nav_view);
        if (!handleHdoNotificationIntent(intent, navigationView)) {
            handleMonthlyReadingReminderIntent(intent, navigationView);
        }
    }


    /**
     * Při návratu aktivity do popředí aktualizuje viditelnost navigačních panelů,
     * naplánuje odeslání čekajících záloh a ověří stav probíhající aktualizace aplikace.
     */
    @Override
    protected void onResume() {
        super.onResume();
        //nastavení viditelnosti levé a spodní lišty podle orientace obrazovky
        setVisibilityNavigation();
        schedulePendingBackupUploadIfNeeded();
        if (appUpdateHelper != null) {
            appUpdateHelper.checkResumeUpdate();
        }
    }


    /**
     * Odregistruje síťový callback a posluchače aktualizací aplikace při rušení aktivity.
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (connectivityManager != null && pendingUploadNetworkCallback != null) {
            connectivityManager.unregisterNetworkCallback(pendingUploadNetworkCallback);
        }
        if (appUpdateHelper != null) {
            appUpdateHelper.unregisterListener();
        }
    }


    /**
     * Vrátí instanci pomocné třídy pro správu aktualizací aplikace přes Google Play.
     *
     * @return Instance {@link AppUpdateHelper}
     */
    public AppUpdateHelper getAppUpdateHelper() {
        return appUpdateHelper;
    }


    /**
     * Uloží index vybrané položky navigace a instanci aktuálně zobrazeného fragmentu.
     *
     * @param outState Bundle pro uložení stavu aktivity
     */
    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(ACTUAL_SELECTED_ITEM_INDEX, selectedItemIndex);

        //uložení instance aktuálního fragmentu
        if (actualFragment != null && actualFragment.isAdded() && !actualFragment.isDetached())
            getSupportFragmentManager().putFragment(outState, ACTUAL_FRAGMENT, actualFragment);
    }


    /**
     * Vytvoří instanci fragmentu přehledu podle volby uživatele (nový nebo původní přehled).
     *
     * @return Instance {@link NewDashBoardFragment} nebo {@link DashBoardFragment}
     */
    private Fragment createDashboardFragment() {
        ShPDashBoard shPDashBoard = new ShPDashBoard(getApplicationContext());
        boolean useNewDashboard = shPDashBoard.get(ShPDashBoard.USE_NEW_DASHBOARD, true);
        return useNewDashboard ? NewDashBoardFragment.newInstance() : DashBoardFragment.newInstance();
    }


    /**
     * Vytvoří položky v hlavním menu toolbaru (výběr odběrného místa, přepnutí přehledu a nastavení aplikace).
     *
     * @param menu Cílové menu toolbaru
     * @return {@code true} pro zobrazení menu
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, 0, Menu.NONE, getResources().getString(R.string.subscription_point));
        menu.add(0, 2, Menu.NONE, getResources().getString(R.string.switch_to_old_dashboard));
        menu.add(0, 1, Menu.NONE, getResources().getString(R.string.settings_app));
        return true;
    }


    /**
     * Zpracuje výběr položky v menu toolbaru nebo kliknutí na tlačítko zpět v AppBaru.
     *
     * @param item Vybraná položka menu
     * @return {@code true}, pokud byla událost obsloužena
     */
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home && getSupportFragmentManager().getBackStackEntryCount() > 0) {
            getSupportFragmentManager().popBackStack();
            return true;
        }
        if (item.getItemId() == 0) {
            SubscriptionPointDialogFragment.newInstance()
                    .show(getSupportFragmentManager(), SubscriptionPointDialogFragment.class.getSimpleName());
            return true;
        }
        if (item.getItemId() == 1) {
            uncheckedBottomNavigation();
            actualFragment = SettingsFragment.newInstance();
            FragmentChange.replace(MainActivity.this, actualFragment, MOVE, true);
            setToolbarTitle(getResources().getString(R.string.settings_app));
            return true;
        }
        if (item.getItemId() == 2) {
            ShPDashBoard shPDashBoard = new ShPDashBoard(getApplicationContext());
            boolean currentlyNew = shPDashBoard.get(ShPDashBoard.USE_NEW_DASHBOARD, true);
            shPDashBoard.set(ShPDashBoard.USE_NEW_DASHBOARD, !currentlyNew);
            actualFragment = createDashboardFragment();
            FragmentChange.replace(MainActivity.this, actualFragment, ALPHA);
            setToolbarTitle(getResources().getString(R.string.dashboard));
            invalidateOptionsMenu();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }


    /**
     * Dynamicky upraví viditelnost položky pro přepnutí odběrného místa a přepnutí verze přehledu v menu toolbaru.
     * Položka pro odběrné místo je viditelná pouze u podporovaných fragmentů a pokud existuje více než jedno odběrné místo.
     * Položka pro přepnutí přehledu je viditelná pouze tehdy, když je zobrazen přehled (starý nebo nový).
     *
     * @param menu Menu toolbaru před zobrazením
     * @return Výsledek volání nadřazené metody
     */
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        //Seznam fragmentů, pro které je volba odběrného místa v menu povolená
        List<String> visibleFragmentsTags = Arrays.asList(
                "BackupFragment",
                "GoogleDriveFragment",
                "ExportPriceListFragment",
                "ImportPriceListFragment",
                "DashBoardFragment",
                "NewDashBoardFragment",
                "MonthlyReadingFragment",
                "MonthlyReadingSettingsFragment",
                "PriceListFragment",
                "PriceListDetailFragment",
                "GraphColorFragment",
                "GraphMonthFragment",
                "HdoFragment",
                "HdoSiteFragment",
                "InvoiceFragment",
                "InvoiceListFragment",
                "SubscriptionPointFragment",
                "PaymentFragment",
                "HelpFragment",
                "AboutMeFragment",
                "SettingsViewDialogFragment",
                "SettingsFragment"
        );
        //kontrola, zda-li je některý fragment zobrazen
        boolean isFragmentVisible = false;
        for (String tag : visibleFragmentsTags) {
            Fragment fragment = getSupportFragmentManager().findFragmentByTag(tag);
            if (fragment != null && fragment.isVisible()) {
                isFragmentVisible = true;
                break;
            }
        }
        //zobrazí položku menu, pokud je některý fragment zobrazen a existuje více než 1 odběrné místo
        MenuItem item = menu.findItem(0);
        if (item != null) {
            int countSubscriptionPoints = SubscriptionPoint.count(getApplicationContext());
            item.setVisible(countSubscriptionPoints > 1 && isFragmentVisible);
        }

        MenuItem switchDashboardItem = menu.findItem(2);
        if (switchDashboardItem != null) {
            Fragment oldDashboard = getSupportFragmentManager().findFragmentByTag("DashBoardFragment");
            Fragment newDashboard = getSupportFragmentManager().findFragmentByTag("NewDashBoardFragment");
            boolean isOldVisible = oldDashboard != null && oldDashboard.isVisible();
            boolean isNewVisible = newDashboard != null && newDashboard.isVisible();
            switchDashboardItem.setVisible(isOldVisible || isNewVisible);
            if (isNewVisible) {
                switchDashboardItem.setTitle(R.string.switch_to_old_dashboard);
            } else if (isOldVisible) {
                switchDashboardItem.setTitle(R.string.switch_to_new_dashboard);
            }
        }
        return super.onPrepareOptionsMenu(menu);
    }


    /**
     * Akce při změně orientace obrazovky
     *
     * @param newConfig nová konfigurace
     */
    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        setConfiguration(newConfig);
        setVisibilityNavigation();
    }


    /**
     * Nastaví orientaci obrazovky
     *
     * @param newConfig nová konfigurace
     */
    private void setConfiguration(@NonNull Configuration newConfig) {
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE)
            orientation = Configuration.ORIENTATION_LANDSCAPE;
        else
            orientation = Configuration.ORIENTATION_PORTRAIT;
    }


    /**
     * Nastaví viditelnost spodního a levého plovoucího navigačního panelu podle aktuální orientace
     * obrazovky a uživatelského nastavení.
     */
    private void setVisibilityNavigation() {
        ShPSettings shPSettings = new ShPSettings(getApplicationContext());
        boolean showBottomNavigation = shPSettings.get(ShPSettings.SHOW_BOTTOM_NAVIGATION, true);
        boolean showLeftNavigation = shPSettings.get(ShPSettings.SHOW_LEFT_NAVIGATION, true);

        boolean isPortrait = orientation == Configuration.ORIENTATION_PORTRAIT;
        int bottomVisibility = isPortrait && showBottomNavigation ? View.VISIBLE : View.GONE;
        if (floatingNavCard != null) {
            floatingNavCard.setVisibility(bottomVisibility);
        }
        if (myBottomNavigationView != null) {
            myBottomNavigationView.setVisibility(bottomVisibility);
        }
        View bottomScrim = findViewById(R.id.bottomScrim);
        if (bottomScrim != null) {
            bottomScrim.setVisibility(bottomVisibility);
        }

        boolean showLeftReplacement = orientation == Configuration.ORIENTATION_LANDSCAPE
                && (showBottomNavigation || showLeftNavigation);
        if (floatingLeftNavCard != null) {
            floatingLeftNavCard.setVisibility(showLeftReplacement ? View.VISIBLE : View.GONE);
        }
        if (myNavigationView != null) {
            myNavigationView.setVisibility(showLeftReplacement ? View.VISIBLE : View.GONE);
        }
        notifyPaddingChanged();
    }


    /**
     * Určuje, zda je aktivní režim postranní navigace (orientace na šířku nebo skrytý spodní panel).
     *
     * @return {@code true}, pokud se místo spodního panelu používá postranní navigace
     */
    public boolean isNavRail() {
        return orientation == Configuration.ORIENTATION_LANDSCAPE
                || (myBottomNavigationView != null && myBottomNavigationView.getVisibility() != View.VISIBLE);
    }


    /**
     * Určuje, zda zařízení odpovídá rozměrům tabletu ({@code smallestScreenWidthDp >= 600}).
     *
     * @return {@code true}, pokud jde o tablet
     */
    public boolean isTablet() {
        return getResources().getConfiguration().smallestScreenWidthDp >= 600;
    }


    /**
     * Vrátí aktuální spodní inset systémové navigační lišty v pixelech.
     *
     * @return Spodní inset v pixelech
     */
    public int getEffectiveNavBarInsetBottom() {
        return navBarInsetBottom;
    }


    /**
     * Vrátí aktuální levý inset systémových lišt nebo výřezu displeje v pixelech.
     *
     * @return Levý inset v pixelech
     */
    public int getEffectiveNavBarInsetLeft() {
        return navBarInsetLeft;
    }


    /**
     * Vrátí aktuální pravý inset systémových lišt nebo výřezu displeje v pixelech.
     *
     * @return Pravý inset v pixelech
     */
    public int getEffectiveNavBarInsetRight() {
        return navBarInsetRight;
    }


    /**
     * Přepne spodní plovoucí navigační panel do kompaktního nebo plného zobrazení.
     *
     * @param compact {@code true} pro kompaktní režim (bez popisků), {@code false} pro plnou výšku
     */
    public void setBottomNavCompact(boolean compact) {
        if (floatingNavManager != null) {
            floatingNavManager.setCompact(compact);
        }
    }


    /**
     * Vrátí aktuální spodní odsazení (v pixelech) od spodní hrany obrazovky nad plovoucí navigační panel.
     *
     * @return Spodní odsazení v pixelech
     */
    public int getBottomPanelMarginPx() {
        return floatingNavManager != null
                ? floatingNavManager.getBottomPanelMarginPx()
                : (navBarInsetBottom + (int) (100 * getResources().getDisplayMetrics().density));
    }


    /**
     * Aktualizuje spodní margin plovoucích akčních tlačítek (FAB) ve všech viditelných fragmentech
     * podle aktuální výšky spodního navigačního panelu.
     *
     * @param marginPx Požadované základní spodní odsazení nad navigačním panelem v pixelech
     */
    public void updateBottomPanelMarginValue(int marginPx) {
        List<Fragment> fragments = getSupportFragmentManager().getFragments();
        float density = getResources().getDisplayMetrics().density;
        int targetFabMarginBottom = marginPx + (int) (4 * density);
        for (Fragment f : fragments) {
            if (f != null && f.isVisible() && f.getView() != null) {
                View fab = f.getView().findViewById(R.id.fab);
                if (fab == null) {
                    fab = f.getView().findViewById(R.id.fabHdo);
                }
                if (fab != null) {
                    ViewGroup.MarginLayoutParams fabLp = (ViewGroup.MarginLayoutParams) fab.getLayoutParams();
                    if (fabLp != null && fabLp.bottomMargin != targetFabMarginBottom) {
                        fabLp.bottomMargin = targetFabMarginBottom;
                        fab.setLayoutParams(fabLp);
                    }
                }
            }
        }
    }


    /**
     * Nastaví průhledný přechod (gradient) pozadí horní lišty ({@code appBarLayout}) a vypne {@link BlurView}.
     */
    private void setupBlurView() {
        blurView = findViewById(R.id.blurView);
        if (blurView != null) {
            blurView.setBackgroundColor(android.graphics.Color.TRANSPARENT);
            blurView.setBlurEnabled(false);
        }

        View appBarLayout = findViewById(R.id.appBarLayout);
        if (appBarLayout == null) return;

        int surfaceColor = MaterialColors.getColor(
                appBarLayout,
                com.google.android.material.R.attr.colorSurface,
                0
        );
        GradientDrawable appBarGradient = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{
                        surfaceColor,
                        surfaceColor,
                        surfaceColor,
                        surfaceColor,
                        surfaceColor,
                        surfaceColor,
                        surfaceColor,
                        surfaceColor,
                        ColorUtils.setAlphaComponent(surfaceColor, 244),
                        ColorUtils.setAlphaComponent(surfaceColor, 128),
                        ColorUtils.setAlphaComponent(surfaceColor, 0)
                }
        );
        appBarLayout.setBackground(appBarGradient);

        Toolbar appToolbar = toolbar != null ? toolbar : findViewById(R.id.toolbar);
        if (appToolbar != null) {
            appToolbar.setBackgroundColor(android.graphics.Color.TRANSPARENT);
        }
    }


    /**
     * Vrátí celkovou výšku horní lišty včetně stavového řádku (status baru) v pixelech.
     *
     * @return Celková výška horní lišty v pixelech
     */
    public int getTopBarTotalHeight() {
        float density = getResources().getDisplayMetrics().density;
        if (blurView != null && blurView.getHeight() > 0) {
            return blurView.getHeight();
        }
        Toolbar tb = toolbar != null ? toolbar : findViewById(R.id.toolbar);
        int toolbarHeight = (tb != null && tb.getHeight() > 0) ? tb.getHeight() : (int) (56 * density);
        return statusBarInsetTop + toolbarHeight;
    }


    /**
     * Vrátí celkovou šířku levého plovoucího navigačního panelu včetně systémového insetu a odsazení.
     * Pokud levý panel není zobrazen, vrací pouze levý systémový inset.
     *
     * @return Celkové levé odsazení v pixelech
     */
    public int getLeftNavTotalWidth() {
        if (!isNavRail() || floatingLeftNavCard == null || floatingLeftNavCard.getVisibility() != View.VISIBLE) {
            return getEffectiveNavBarInsetLeft();
        }
        float density = getResources().getDisplayMetrics().density;
        int cardWidth = floatingLeftNavCard.getWidth() > 0 ? floatingLeftNavCard.getWidth() : (int) (56 * density);
        ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) floatingLeftNavCard.getLayoutParams();
        int leftMargin = lp != null ? lp.leftMargin : (getEffectiveNavBarInsetLeft() + (int) (16 * density));
        return leftMargin + cardWidth + (int) (12 * density);
    }


    /**
     * Aktualizuje vzhled systémových lišt (světlé/tmavé ikony podle aktuálního motivu)
     * a nastaví pozadí horního AppBaru.
     */
    public void updateAppBarBackgroundAndSystemBars() {
        boolean isDarkTheme = (getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
        WindowInsetsControllerCompat windowInsetsController = WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        windowInsetsController.setAppearanceLightStatusBars(!isDarkTheme);
        windowInsetsController.setAppearanceLightNavigationBars(!isDarkTheme);

        View appBarLayout = findViewById(R.id.appBarLayout);
        if (appBarLayout != null) {
            appBarLayout.setBackgroundColor(android.graphics.Color.TRANSPARENT);
        }

        int surfaceColor = appBarLayout != null
                ? MaterialColors.getColor(appBarLayout, com.google.android.material.R.attr.colorSurface, 0)
                : android.graphics.Color.WHITE;
        setSystemBarColors(surfaceColor, null);

        setupBlurView();
        updateFloatingNavigationSurfaceColors(isDarkTheme);
    }


    /**
     * Odvodí poloprůhledné barvy plovoucích navigačních panelů a spodního přechodu
     * přímo z aktuálního motivu (včetně dynamických barev Material You).
     *
     * @param isDarkTheme {@code true}, pokud je aktivní tmavý motiv
     */
    private void updateFloatingNavigationSurfaceColors(boolean isDarkTheme) {
        int surfaceColor = MaterialColors.getColor(
                this,
                com.google.android.material.R.attr.colorSurface,
                android.graphics.Color.WHITE
        );
        int surfaceContainerColor = MaterialColors.getColor(
                this,
                com.google.android.material.R.attr.colorSurfaceContainer,
                surfaceColor
        );
        int onSurfaceColor = MaterialColors.getColor(
                this,
                com.google.android.material.R.attr.colorOnSurface,
                isDarkTheme ? android.graphics.Color.WHITE : android.graphics.Color.BLACK
        );

        int cardBackgroundColor = ColorUtils.setAlphaComponent(surfaceContainerColor, 0xF2);
        int cardStrokeColor = ColorUtils.setAlphaComponent(onSurfaceColor, isDarkTheme ? 0x26 : 0x1F);

        if (floatingNavCard instanceof MaterialCardView card) {
            card.setCardBackgroundColor(cardBackgroundColor);
            card.setStrokeColor(cardStrokeColor);
        }
        if (floatingLeftNavCard instanceof MaterialCardView card) {
            card.setCardBackgroundColor(cardBackgroundColor);
            card.setStrokeColor(cardStrokeColor);
        }

        View bottomScrim = findViewById(R.id.bottomScrim);
        if (bottomScrim != null) {
            int scrimEndColor = ColorUtils.setAlphaComponent(surfaceColor, isDarkTheme ? 0xA0 : 0x80);
            GradientDrawable scrimDrawable = new GradientDrawable(
                    GradientDrawable.Orientation.TOP_BOTTOM,
                    new int[]{android.graphics.Color.TRANSPARENT, scrimEndColor}
            );
            bottomScrim.setBackground(scrimDrawable);
        }
    }


    /**
     * Nastaví barvu stavové a navigační lišty pro zpětnou kompatibilitu se staršími verzemi Androidu (API &lt; 35).
     * Od Androidu 15 (API 35) jsou metody {@code setStatusBarColor} a {@code setNavigationBarColor} zastaralé,
     * protože systém vynucuje režim edge-to-edge a lišty jsou průhledné automaticky.
     *
     * @param statusBarColor     Barva stavové lišty
     * @param navigationBarColor Barva navigační lišty (nebo {@code null}, pokud se nemá měnit)
     */
    @SuppressWarnings("deprecation")
    private void setSystemBarColors(int statusBarColor, @Nullable Integer navigationBarColor) {
        getWindow().setStatusBarColor(statusBarColor);
        if (navigationBarColor != null) {
            getWindow().setNavigationBarColor(navigationBarColor);
        }
    }


    /**
     * Aktualizuje vnitřní odsazení horního {@code appBarLayout} a výšku {@link BlurView}
     * podle aktuálních systémových insetů.
     */
    public void updateTopBarPadding() {
        View appBarLayout = findViewById(R.id.appBarLayout);
        if (appBarLayout != null) {
            appBarLayout.setPadding(
                    getEffectiveNavBarInsetLeft(),
                    statusBarInsetTop,
                    getEffectiveNavBarInsetRight(),
                    0
            );
        }

        if (blurView != null) {
            float density = getResources().getDisplayMetrics().density;
            Toolbar tb = toolbar != null ? toolbar : findViewById(R.id.toolbar);
            int toolbarHeight = (tb != null && tb.getHeight() > 0) ? tb.getHeight() : (int) (56 * density);
            int totalHeight = statusBarInsetTop + toolbarHeight;

            ViewGroup.LayoutParams lp = blurView.getLayoutParams();
            if (lp != null && lp.height != totalHeight) {
                lp.height = totalHeight;
                blurView.setLayoutParams(lp);
            }
        }
    }


    /**
     * Aktualizuje vnější marginy spodní a levé plovoucí navigační karty podle systémových insetů.
     */
    public void updateFloatingNavCardMargins() {
        float density = getResources().getDisplayMetrics().density;
        int baseMargin = (int) (16 * density);

        if (floatingNavCard != null) {
            ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) floatingNavCard.getLayoutParams();
            if (lp != null) {
                lp.leftMargin = baseMargin + getEffectiveNavBarInsetLeft();
                lp.rightMargin = baseMargin + getEffectiveNavBarInsetRight();
                lp.bottomMargin = baseMargin + getEffectiveNavBarInsetBottom();
                floatingNavCard.setLayoutParams(lp);
            }
        }
        if (floatingLeftNavCard != null) {
            ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) floatingLeftNavCard.getLayoutParams();
            if (lp != null) {
                lp.leftMargin = baseMargin + getEffectiveNavBarInsetLeft();
                lp.topMargin = (int) (8 * density);
                lp.bottomMargin = baseMargin + getEffectiveNavBarInsetBottom();
                floatingLeftNavCard.setLayoutParams(lp);
            }
        }
    }


    /**
     * Upozorní všechny viditelné fragmenty implementující {@link OnPaddingChangeListener},
     * že došlo ke změně systémových insetů nebo rozměrů navigačních panelů.
     */
    public void notifyPaddingChanged() {
        List<Fragment> fragments = getSupportFragmentManager().getFragments();
        for (Fragment f : fragments) {
            if (f != null && f.isVisible()) {
                if (f instanceof OnPaddingChangeListener) {
                    ((OnPaddingChangeListener) f).onPaddingChanged();
                }
            }
        }
    }


    /**
     * Odznačí všechny položky bottomNavigationView
     * Nastaví se aktivní neviditelná položka :)
     */
    private void uncheckedBottomNavigation() {
        myBottomNavigationView.setSelectedItemId(R.id.meni_nothing);
    }


    /**
     * HDO tray služba je z aplikace odebraná kvůli Play Console požadavkům.
     * Pokud je v preferencích uložen starý stav, vynulujeme ho.
     */
    private void startHdoService() {
        ShPHdo shPHdo = new ShPHdo(getApplicationContext());
        if (shPHdo.get(ShPHdo.ARG_RUNNING_SERVICE, false))
            shPHdo.set(ShPHdo.ARG_RUNNING_SERVICE, false);
    }


    /**
     * Nastaví jako aktivní vybranou položku bottomNavigationView
     *
     * @param id id položky
     */
    public void onCheckedNavigationItem(int id) {
        myBottomNavigationView.setSelectedItemId(id);
    }


    /**
     * Nastaví titulek toolbaru
     *
     * @param title titulek
     */
    public void setToolbarTitle(String title) {
        shPMainActivity.set(ShPMainActivity.PRIMARY_TITLE, title);
        toolbarTitle.setText(title);
    }


    /**
     * Aktualizuje toolbar pro režim výběru více položek.
     *
     * @param selectedCount         Počet vybraných položek
     * @param isMultiSelect         {@code true}, pokud je aktivní režim výběru
     * @param cancelSelectionAction Akce provedená po kliknutí na tlačítko zavření výběru v toolbaru
     */
    public void updateToolbarForSelection(int selectedCount, boolean isMultiSelect, @Nullable Runnable cancelSelectionAction) {
        selectionNavigationActive = isMultiSelect;
        selectionNavigationAction = isMultiSelect ? cancelSelectionAction : null;

        if (isMultiSelect) {
            toolbarTitle.setText(String.valueOf(selectedCount));
        } else {
            toolbarTitle.setText(shPMainActivity.get(ShPMainActivity.PRIMARY_TITLE, getResources().getString(R.string.month_reads)));
        }

        updateAppBarNavigationIcon();
    }


    /**
     * Nastaví podtitulek toolbaru
     *
     * @param subtitle podtitulek
     */
    public void setToolbarSubtitle(String subtitle) {
        shPMainActivity.set(ShPMainActivity.SECONDARY_TITLE, subtitle);
        toolbarSubtitle.setText(subtitle);
    }


    /**
     * Nastaví zobrazení při změně ne/regulované ceny
     *
     * @param showRegulPrice zobrazení regulované ceny
     */
    @Override
    public void onShowRegulPrice(boolean showRegulPrice) {
        MonthlyReadingDetailFragment monthlyReadingDetailFragment = (MonthlyReadingDetailFragment) getSupportFragmentManager().findFragmentByTag(MonthlyReadingDetailFragment.TAG);
        if (monthlyReadingDetailFragment != null) {
            monthlyReadingDetailFragment.setShowRegulPrice(showRegulPrice);
        }
    }


    /**
     * Odznačí všechny položky menu
     *
     * @param menu menu pro odznačení položek
     */
    private void uncheckAllMenuItems(Menu menu) {
        for (int i = 0; i < menu.size(); i++) {
            MenuItem item = menu.getItem(i);
            if (item.hasSubMenu()) {
                Menu subMenu = item.getSubMenu();
                for (int j = 0; j < (subMenu != null ? subMenu.size() : 0); j++) {
                    subMenu.getItem(j).setChecked(false);
                }
            } else {
                item.setChecked(false);
            }
        }
    }


    /**
     * Přepne ikonu v AppBar podle stavu backstacku.
     */
    private void updateAppBarNavigationIcon() {
        if (selectionNavigationActive && selectionNavigationAction != null) {
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            }

            drawerToggle.setDrawerIndicatorEnabled(false);
            toolbar.setNavigationIcon(R.drawable.ic_close_24);
            drawerToggle.setToolbarNavigationClickListener(v -> {
                if (selectionNavigationAction != null) {
                    selectionNavigationAction.run();
                }
            });
            drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
            drawerToggle.syncState();
            return;
        }

        boolean hasBackStack = getSupportFragmentManager().getBackStackEntryCount() > 0;
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(hasBackStack);
        }

        if (hasBackStack) {
            drawerToggle.setDrawerIndicatorEnabled(false);
            toolbar.setNavigationIcon(R.drawable.ic_arrow_back_24);
            drawerToggle.setToolbarNavigationClickListener(v -> getSupportFragmentManager().popBackStack());
        } else {
            drawerToggle.setToolbarNavigationClickListener(null);
            drawerToggle.setDrawerIndicatorEnabled(true);
        }

        drawerLayout.setDrawerLockMode(hasBackStack
                ? DrawerLayout.LOCK_MODE_LOCKED_CLOSED
                : DrawerLayout.LOCK_MODE_UNLOCKED);
        drawerToggle.syncState();
    }


    /**
     * Připraví callback změn konektivity pro centrální plánování pending uploadu.
     */
    private void setupPendingUploadScheduler() {
        connectivityManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        if (connectivityManager == null)
            return;

        pendingUploadNetworkCallback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(@NonNull Network network) {
                schedulePendingBackupUploadIfNeeded();
            }

            @Override
            public void onCapabilitiesChanged(@NonNull Network network, @NonNull NetworkCapabilities networkCapabilities) {
                if (networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) {
                    schedulePendingBackupUploadIfNeeded();
                }
            }
        };

        NetworkRequest networkRequest = new NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build();
        connectivityManager.registerNetworkCallback(networkRequest, pendingUploadNetworkCallback);
    }


    /**
     * Naplánuje čekající odesílání záloh centrálně z MainActivity.
     */
    private void schedulePendingBackupUploadIfNeeded() {
        if (!NetworkUtil.isInternetAllowedBySettings(getApplicationContext()))
            return;

        PendingBackupUploadScheduler.scheduleIfNeeded(getApplicationContext(), false);
    }
    /**
     * Zpracuje kliknutí na HDO notifikaci: nastaví odběrné místo a otevře HDO fragment.
     *
     * @param intent         Příchozí intent aktivity
     * @param navigationView Postranní navigační menu (drawer) pro označení aktivní položky
     * @return {@code true}, pokud intent obsahoval akci HDO notifikace
     */
    private boolean handleHdoNotificationIntent(@Nullable Intent intent, @Nullable NavigationView navigationView) {
        if (intent == null) {
            return false;
        }

        String argsFragment = intent.getStringExtra(HdoNotice.ARGS_FRAGMENT);
        if (!HdoNotice.NOTIFICATION_HDO_SERVICE.equals(argsFragment)) {
            return false;
        }

        long subscriptionPointId = intent.getLongExtra(HdoNotice.EXTRA_SUBSCRIPTION_POINT_ID, -1L);
        if (subscriptionPointId > 0) {
            SubscriptionPoint.setCurrentSelection(getApplicationContext(), subscriptionPointId);
        }

        intent.removeExtra(HdoNotice.ARGS_FRAGMENT);
        intent.removeExtra(HdoNotice.EXTRA_SUBSCRIPTION_POINT_ID);

        if (navigationView != null) {
            navigationView.setCheckedItem(R.id.menu_hdo);
        }
        uncheckedBottomNavigation();
        actualFragment = HdoFragment.newInstance();
        FragmentChange.replace(this, actualFragment, ALPHA);
        setToolbarTitle(getResources().getString(R.string.hdo_times));
        SubscriptionPointModel subscriptionPoint = SubscriptionPoint.load(getApplicationContext());
        setToolbarSubtitle(subscriptionPoint != null ? subscriptionPoint.getName() : "");
        return true;
    }


    /**
     * Zpracuje kliknutí na notifikaci připomenutí měsíčního odečtu.
     *
     * @param intent         Příchozí intent aktivity
     * @param navigationView Postranní navigační menu (drawer) pro označení aktivní položky
     * @return {@code true}, pokud intent obsahoval akci připomenutí měsíčního odečtu
     */
    private boolean handleMonthlyReadingReminderIntent(@Nullable Intent intent, @Nullable NavigationView navigationView) {
        if (intent == null) {
            return false;
        }

        String argsFragment = intent.getStringExtra(HdoNotice.ARGS_FRAGMENT);
        if (!MonthlyReadingReminderNotice.NOTIFICATION_FRAGMENT.equals(argsFragment)) {
            return false;
        }

        long subscriptionPointId = intent.getLongExtra(HdoNotice.EXTRA_SUBSCRIPTION_POINT_ID, -1L);
        if (subscriptionPointId > 0) {
            SubscriptionPoint.setCurrentSelection(getApplicationContext(), subscriptionPointId);
        }

        intent.removeExtra(HdoNotice.ARGS_FRAGMENT);
        intent.removeExtra(HdoNotice.EXTRA_SUBSCRIPTION_POINT_ID);

        if (navigationView != null) {
            navigationView.setCheckedItem(R.id.menu_monthly_reads);
        }
        myNavigationView.setCheckedItem(myNavigationView.getMenu().getItem(2).setChecked(true));
        myBottomNavigationView.setSelectedItemId(R.id.meni_monthly_readings);
        selectedItemIndex = 2;
        shPMainActivity.set(ACTUAL_FRAGMENT, R.id.meni_monthly_readings);
        actualFragment = MonthlyReadingFragment.newInstance();
        FragmentChange.replace(this, actualFragment, ALPHA);
        setToolbarTitle(getResources().getString(R.string.month_reads));
        SubscriptionPointModel subscriptionPoint = SubscriptionPoint.load(getApplicationContext());
        setToolbarSubtitle(subscriptionPoint != null ? subscriptionPoint.getName() : "");
        return true;
    }

}