package cz.xlisto.odecty;

import android.content.Intent;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationView;

import java.util.Objects;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import cz.xlisto.odecty.modules.backup.BackupFragment;
import cz.xlisto.odecty.modules.graphmonth.GraphMonthFragment;
import cz.xlisto.odecty.modules.hdo.HdoFragment;
import cz.xlisto.odecty.modules.invoice.InvoiceListFragment;
import cz.xlisto.odecty.modules.monthlyreading.MonthlyReadingFragment;
import cz.xlisto.odecty.modules.pricelist.PriceListCompareFragment;
import cz.xlisto.odecty.modules.pricelist.PriceListFragment;
import cz.xlisto.odecty.modules.subscriptionpoint.SubscriptionPointFragment;
import cz.xlisto.odecty.services.HdoData;
import cz.xlisto.odecty.services.HdoNotice;
import cz.xlisto.odecty.services.HdoService;
import cz.xlisto.odecty.shp.ShPHdo;
import cz.xlisto.odecty.shp.ShPMainActivity;
import cz.xlisto.odecty.utils.FragmentChange;

import static cz.xlisto.odecty.utils.FragmentChange.Transaction.ALPHA;

public class MainActivity extends AppCompatActivity {
    private final static String TAG = "MainActivity";
    private static final String ACTUAL_FRAGMENT = "actualFragment";
    private Fragment actualFragment;
    private BottomNavigationView bottomNavigationView;
    private ShPMainActivity shPMainActivity;
    private int orientation;
    private boolean secondClick = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setConfiguration(getResources().getConfiguration());

        bottomNavigationView = findViewById(R.id.bottomNavigation);
        NavigationView navigationView = findViewById(R.id.nav_view);

        shPMainActivity = new ShPMainActivity(getApplicationContext());

        //Horní toolbar + zobrazení tlačítka
        Toolbar toolbar = findViewById(R.id.toolbar);
        DrawerLayout drawer = findViewById(R.id.drawer_layout);

        setSupportActionBar(toolbar);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this,
                drawer, toolbar,
                R.string.ano,
                R.string.ano);
        drawer.addDrawerListener(toggle);
        //drawer.setDrawerListener(toggle);
        toggle.syncState();


        bottomNavigationView.setOnItemSelectedListener(item -> {
            long itemId = item.getItemId();

            if (itemId == R.id.meni_prices) {
                navigationView.setCheckedItem(R.id.menu_price_list);
                shPMainActivity.set(ACTUAL_FRAGMENT, R.id.meni_prices);
                actualFragment = PriceListFragment.newInstance(false, -1L);
                FragmentChange.replace(MainActivity.this, actualFragment, ALPHA);
                return true;
            }

            if (itemId == R.id.meni_monthly_readings) {
                navigationView.setCheckedItem(R.id.menu_monthly_reads);
                shPMainActivity.set(ACTUAL_FRAGMENT, R.id.meni_monthly_readings);
                actualFragment = MonthlyReadingFragment.newInstance();
                FragmentChange.replace(MainActivity.this, actualFragment, ALPHA);
                return true;
            }

            if (itemId == R.id.meni_subscription_points) {
                navigationView.setCheckedItem(R.id.menu_subscription_points);
                shPMainActivity.set(ACTUAL_FRAGMENT, R.id.meni_subscription_points);
                actualFragment = SubscriptionPointFragment.newInstance();
                FragmentChange.replace(MainActivity.this, actualFragment, ALPHA);
                return true;
            }

            if (itemId == R.id.meni_invoice) {
                navigationView.setCheckedItem(R.id.menu_invoices);
                shPMainActivity.set(ACTUAL_FRAGMENT, R.id.meni_invoice);
                actualFragment = InvoiceListFragment.newInstance("x", "x");
                FragmentChange.replace(MainActivity.this, actualFragment, ALPHA);
                return true;
            }

            return itemId == R.id.meni_nothing;
        });

        navigationView.setNavigationItemSelectedListener(item -> {
            boolean b = false;
            long itemId = item.getItemId();
            if (itemId == R.id.menu_price_list) {
                bottomNavigationView.setSelectedItemId(R.id.meni_prices);
                actualFragment = PriceListFragment.newInstance(false, -1L);
                b = true;
            }
            if (itemId == R.id.menu_compare_price_list) {
                uncheckedBottomNavigation();
                actualFragment = PriceListCompareFragment.newInstance();
                b = true;
            }

            if (itemId == R.id.menu_monthly_reads) {
                bottomNavigationView.setSelectedItemId(R.id.meni_monthly_readings);
                actualFragment = MonthlyReadingFragment.newInstance();
                b = true;
            }

            if (itemId == R.id.menu_subscription_points) {
                bottomNavigationView.setSelectedItemId(R.id.meni_subscription_points);
                actualFragment = SubscriptionPointFragment.newInstance();
                b = true;
            }

            if (itemId == R.id.menu_invoices) {
                bottomNavigationView.setSelectedItemId(R.id.meni_invoice);
                actualFragment = InvoiceListFragment.newInstance("x", "x");
                b = true;
            }

            if (itemId == R.id.menu_hdo) {
                uncheckedBottomNavigation();
                actualFragment = HdoFragment.newInstance();
                b = true;
            }

            if (itemId == R.id.menu_backup) {
                //uncheckedBottomNavigation();
                actualFragment = new BackupFragment();
                b = true;
            }

            if (itemId == R.id.menu_graph_month) {
                uncheckedBottomNavigation();
                actualFragment = GraphMonthFragment.newInstance();
                b = true;
            }

            if (itemId == R.id.menu_test) {
                uncheckedBottomNavigation();
                actualFragment = TestFragment.newInstance();
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
        } else {
            actualFragment = MonthlyReadingFragment.newInstance();
            bottomNavigationView.setSelectedItemId(shPMainActivity.get(ACTUAL_FRAGMENT, R.id.meni_monthly_readings));
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
                        Toast.makeText(getApplication(), "Následující kliknutí aplikaci ukončí", Toast.LENGTH_SHORT).show();
                        new Handler().postDelayed(() -> secondClick = false, 2000);
                    } else {
                        finish();
                    }
                }
            }
        });
    }


    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        //Save the fragment's instance
        if (actualFragment != null)
            getSupportFragmentManager().putFragment(outState, ACTUAL_FRAGMENT, actualFragment);

    }


    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        return super.onOptionsItemSelected(item);
    }


    /**
     * Akce při změně orientace obrazovky
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
    private void setVisibilityBottomNavigation(){
        bottomNavigationView.setVisibility(orientation == 1 ? View.VISIBLE : View.GONE);
    }


    /**
     * Odznačí všechny položky bottomNavigationView
     * Nastaví se aktivní neviditelná položka :)
     */
    private void uncheckedBottomNavigation(){
        bottomNavigationView.setSelectedItemId(R.id.meni_nothing);
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
}