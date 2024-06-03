package cz.xlisto.elektrodroid;


import static cz.xlisto.elektrodroid.utils.FragmentChange.Transaction.ALPHA;

import android.content.Intent;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;

import com.google.android.material.navigation.NavigationView;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import cz.xlisto.elektrodroid.dialogs.SettingsViewDialogFragment;
import cz.xlisto.elektrodroid.dialogs.SubscriptionPointDialogFragment;
import cz.xlisto.elektrodroid.modules.backup.BackupFragment;
import cz.xlisto.elektrodroid.modules.dashboard.DashBoardFragment;
import cz.xlisto.elektrodroid.modules.exportimportpricelist.ExportPriceListFragment;
import cz.xlisto.elektrodroid.modules.exportimportpricelist.ImportPriceListFragment;
import cz.xlisto.elektrodroid.modules.graphcolor.GraphColorFragment;
import cz.xlisto.elektrodroid.modules.graphmonth.GraphMonthFragment;
import cz.xlisto.elektrodroid.modules.hdo.HdoFragment;
import cz.xlisto.elektrodroid.modules.invoice.InvoiceListFragment;
import cz.xlisto.elektrodroid.modules.monthlyreading.MonthlyReadingDetailFragment;
import cz.xlisto.elektrodroid.modules.monthlyreading.MonthlyReadingFragment;
import cz.xlisto.elektrodroid.modules.pricelist.PriceListCompareBoxFragment;
import cz.xlisto.elektrodroid.modules.pricelist.PriceListFragment;
import cz.xlisto.elektrodroid.modules.subscriptionpoint.SubscriptionPointFragment;
import cz.xlisto.elektrodroid.ownview.MyBottomNavigationView;
import cz.xlisto.elektrodroid.services.HdoData;
import cz.xlisto.elektrodroid.services.HdoNotice;
import cz.xlisto.elektrodroid.services.HdoService;
import cz.xlisto.elektrodroid.shp.ShPHdo;
import cz.xlisto.elektrodroid.shp.ShPMainActivity;
import cz.xlisto.elektrodroid.utils.DetectScreenMode;
import cz.xlisto.elektrodroid.utils.FragmentChange;
import cz.xlisto.elektrodroid.utils.SubscriptionPoint;


public class MainActivity extends AppCompatActivity implements MonthlyReadingFragment.OnShowRegulPriceListener {

    private final static String TAG = "MainActivity";
    private static final String ACTUAL_FRAGMENT = "actualFragment";
    private static final String ACTUAL_SELECTED_ITEM_INDEX = "actualSelectedItemIndex";
    private Fragment actualFragment;
    private MyBottomNavigationView myBottomNavigationView;
    private NavigationView myNavigationView;
    private TextView toolbarTitle, toolbarSubtitle;
    private ShPMainActivity shPMainActivity;
    private int orientation, selectedItemIndex = -1;
    private boolean secondClick = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setConfiguration(getResources().getConfiguration());
        myBottomNavigationView = findViewById(R.id.myBottomNavigation);
        myNavigationView = findViewById(R.id.navigationView);
        NavigationView navigationView = findViewById(R.id.nav_view);
        shPMainActivity = new ShPMainActivity(getApplicationContext());
        //Horní toolbar + zobrazení tlačítka
        Toolbar toolbar = findViewById(R.id.toolbar);
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        toolbarTitle = toolbar.findViewById(R.id.toolbar_title);
        toolbarSubtitle = toolbar.findViewById(R.id.toolbar_subtitle);
        setSupportActionBar(toolbar);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this,
                drawer, toolbar,
                R.string.ano,
                R.string.ano);
        drawer.addDrawerListener(toggle);
        //drawer.setDrawerListener(toggle);
        toggle.syncState();

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
                actualFragment = DashBoardFragment.newInstance();
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

        //levý drawer
        myNavigationView.setNavigationItemSelectedListener(item -> {
            uncheckAllMenuItems(myNavigationView.getMenu());
            item.setChecked(true);
            long itemId = item.getItemId();
            if (itemId == R.id.meni_dashboard) {
                navigationView.setCheckedItem(R.id.menu_dashboard);
                myBottomNavigationView.setSelectedItemId(R.id.meni_dashboard);
                shPMainActivity.set(ACTUAL_FRAGMENT, R.id.meni_dashboard);
                actualFragment = DashBoardFragment.newInstance();
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
        //levý drawer
        navigationView.setNavigationItemSelectedListener(item -> {
            boolean b = false;
            long itemId = item.getItemId();
            if (itemId == R.id.menu_dashboard) {
                myBottomNavigationView.setSelectedItemId(R.id.meni_dashboard);
                actualFragment = DashBoardFragment.newInstance();
                setToolbarTitle(getResources().getString(R.string.dashboard));
                b = true;
            }
            if (itemId == R.id.menu_price_list) {
                myBottomNavigationView.setSelectedItemId(R.id.meni_prices);
                actualFragment = PriceListFragment.newInstance(false, -1L);
                setToolbarTitle(getResources().getString(R.string.price_lists));
                b = true;
            }
            if (itemId == R.id.menu_compare_price_list) {
                uncheckedBottomNavigation();
                //actualFragment = PriceListCompareDetailFragment.newInstance();
                actualFragment = PriceListCompareBoxFragment.newInstance();
                setToolbarTitle(getResources().getString(R.string.compare_price_list));
                b = true;
            }
            if (itemId == R.id.menu_monthly_reads) {
                myBottomNavigationView.setSelectedItemId(R.id.meni_monthly_readings);
                actualFragment = MonthlyReadingFragment.newInstance();
                setToolbarTitle(getResources().getString(R.string.month_reads));
                b = true;
            }
            if (itemId == R.id.menu_subscription_points) {
                myBottomNavigationView.setSelectedItemId(R.id.meni_subscription_points);
                actualFragment = SubscriptionPointFragment.newInstance();
                b = true;
            }
            if (itemId == R.id.menu_invoices) {
                myBottomNavigationView.setSelectedItemId(R.id.meni_invoice);
                actualFragment = InvoiceListFragment.newInstance();
                setToolbarTitle(getResources().getString(R.string.invoices));
                b = true;
            }
            if (itemId == R.id.menu_hdo) {
                uncheckedBottomNavigation();
                actualFragment = HdoFragment.newInstance();
                setToolbarTitle(getResources().getString(R.string.hdo_times));
                b = true;
            }
            if (itemId == R.id.menu_backup) {
                uncheckedBottomNavigation();
                actualFragment = BackupFragment.newInstance();
                setToolbarTitle(getResources().getString(R.string.backup1));
                b = true;
            }
            if (itemId == R.id.menu_graph_month) {
                uncheckedBottomNavigation();
                actualFragment = GraphMonthFragment.newInstance();
                setToolbarTitle(getResources().getString(R.string.graph_month));
                b = true;
            }
            if (itemId == R.id.menu_graph_color) {
                uncheckedBottomNavigation();
                actualFragment = GraphColorFragment.newInstance();
                setToolbarTitle(getResources().getString(R.string.graph_color));
                b = true;
            }
            if (itemId == R.id.menu_import_price_list) {
                uncheckedBottomNavigation();
                actualFragment = ImportPriceListFragment.newInstance();
                setToolbarTitle(getResources().getString(R.string.import_price_list));
                b = true;
            }
            if (itemId == R.id.menu_export_price_list) {
                uncheckedBottomNavigation();
                actualFragment = ExportPriceListFragment.newInstance();
                setToolbarTitle(getResources().getString(R.string.export_price_list));
                b = true;
            }
            if (actualFragment != null)
                FragmentChange.replace(MainActivity.this, actualFragment, ALPHA);
            drawer.closeDrawer(GravityCompat.START, true);
            return b;
        });
        Intent intent = getIntent();
        if (intent.getStringExtra(HdoNotice.ARGS_FRAGMENT) != null) {
            //nastavení fragmentu při kliknutí z notifikace
            if ((Objects.requireNonNull(intent.getStringExtra(HdoNotice.ARGS_FRAGMENT))).equals(HdoNotice.NOTIFICATION_HDO_SERVICE)) {
                getIntent().putExtra(HdoNotice.ARGS_FRAGMENT, "");
                actualFragment = HdoFragment.newInstance();
                FragmentChange.replace(this, actualFragment, ALPHA);
                return;
            }
        }
        if (savedInstanceState != null) {
            actualFragment = getSupportFragmentManager().getFragment(savedInstanceState, ACTUAL_FRAGMENT);
            selectedItemIndex = savedInstanceState.getInt(ACTUAL_SELECTED_ITEM_INDEX);
            if (selectedItemIndex >= 0)
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
        setVisibilityBottomNavigation();

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
                        new Handler().postDelayed(() -> secondClick = false, 2000);
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
    }


    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(ACTUAL_SELECTED_ITEM_INDEX, selectedItemIndex);

        //Save the fragment's instance
        if (actualFragment != null && actualFragment.isAdded() && !actualFragment.isDetached())
            getSupportFragmentManager().putFragment(outState, ACTUAL_FRAGMENT, actualFragment);

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        int countSubscriptionPoints = SubscriptionPoint.count(getApplicationContext());
        if (countSubscriptionPoints > 1)
            menu.add(0, 0, Menu.NONE, getResources().getString(R.string.subscription_point));
        menu.add(0, 1, Menu.NONE, getResources().getString(R.string.settings_view));
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == 0) {
            SubscriptionPointDialogFragment.newInstance()
                    .show(getSupportFragmentManager(), SubscriptionPointDialogFragment.class.getSimpleName());
            return true;
        }
        if (item.getItemId() == 1) {
            SettingsViewDialogFragment.newInstance()
                    .show(getSupportFragmentManager(), SettingsViewDialogFragment.class.getSimpleName());
            return true;
        }
        return super.onOptionsItemSelected(item);
    }


    //pro skrytí položky menu
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        //Seznam fragmentů
        List<String> visibleFragmentsTags = Arrays.asList("BackupFragment", "ExportPriceListFragment",
                "DashBoardFragment", "ExportPriceListFragment", "ImportPriceListFragment", "MonthlyReadingFragment",
                "GraphColorFragment", "GraphMonthFragment", "HdoFragment", "InvoiceListFragment",
                "SubscriptionPointFragment", "GraphColorFragment");
        //kontrola, zda-li je některý fragment zobrazen
        boolean isFragmentVisible = false;
        for (String tag : visibleFragmentsTags) {
            Fragment fragment = getSupportFragmentManager().findFragmentByTag(tag);
            if (fragment != null && fragment.isVisible()) {
                isFragmentVisible = true;
                break;
            }
        }
        //zobrazí položku menu, pokud je některý fragment zobrazen
        int countSubscriptionPoints = SubscriptionPoint.count(getApplicationContext());
        if (countSubscriptionPoints > 1) {
            MenuItem item = menu.findItem(0);
            item.setVisible(isFragmentVisible);
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
        setVisibilityBottomNavigation();
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
     * Nastaví viditelnost bottomNavigationView
     */
    private void setVisibilityBottomNavigation() {
        myBottomNavigationView.setVisibility(orientation == 1 ? View.VISIBLE : View.GONE);
        myNavigationView.setVisibility(orientation == 1 ? View.GONE : View.VISIBLE);
    }


    /**
     * Odznačí všechny položky bottomNavigationView
     * Nastaví se aktivní neviditelná položka :)
     */
    private void uncheckedBottomNavigation() {
        myBottomNavigationView.setSelectedItemId(R.id.meni_nothing);
    }


    /**
     * Spustí službu pro HDO, pokud je uživatelem povolená
     */
    private void startHdoService() {
        ShPHdo shPHdo = new ShPHdo(getApplicationContext());
        if (shPHdo.get(ShPHdo.ARG_RUNNING_SERVICE, false)) {
            HdoData.loadHdoData(getApplicationContext());
            Intent intent = new Intent(this, HdoService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                startForegroundService(intent);
            else
                startService(intent);
        }
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
    private void setToolbarTitle(String title) {
        shPMainActivity.set(ShPMainActivity.PRIMARY_TITLE, title);
        toolbarTitle.setText(title);
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

}