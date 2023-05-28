package cz.xlisto.odecty;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import cz.xlisto.odecty.modules.backup.BackupFragment;
import cz.xlisto.odecty.modules.hdo.HdoFragment;
import cz.xlisto.odecty.modules.invoice.InvoiceListFragment;
import cz.xlisto.odecty.modules.monthlyreading.MonthlyReadingFragment;
import cz.xlisto.odecty.modules.pricelist.PriceListCompareFragment;
import cz.xlisto.odecty.modules.pricelist.PriceListFragment;
import cz.xlisto.odecty.shp.ShPMainActivity;
import cz.xlisto.odecty.modules.subscriptionpoint.SubscriptionPointFragment;
import cz.xlisto.odecty.utils.FragmentChange;

import android.os.Bundle;
import android.os.Handler;
import android.view.MenuItem;
import android.widget.Toast;


import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationView;

import static cz.xlisto.odecty.utils.FragmentChange.Transaction.ALPHA;

public class MainActivity extends AppCompatActivity  {
    private final static String TAG = "MainActivity";
    private static final String ACTUAL_FRAGMENT = "actualFragment";
    private Fragment actualFragment;
    private ShPMainActivity shPMainActivity;
    private boolean secondClick = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        BottomNavigationView bottomNavigationView = findViewById(R.id.bottomNavigation);
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
                shPMainActivity.set(ACTUAL_FRAGMENT, R.id.meni_prices);
                actualFragment = PriceListFragment.newInstance(false, -1L);
                FragmentChange.replace(MainActivity.this, actualFragment, ALPHA);
                return true;
            }

            if (itemId == R.id.meni_monthly_readings) {
                shPMainActivity.set(ACTUAL_FRAGMENT, R.id.meni_monthly_readings);
                actualFragment = MonthlyReadingFragment.newInstance();
                FragmentChange.replace(MainActivity.this, actualFragment, ALPHA);
                return true;
            }

            if (itemId == R.id.meni_subscription_points) {
                shPMainActivity.set(ACTUAL_FRAGMENT, R.id.meni_subscription_points);
                actualFragment = SubscriptionPointFragment.newInstance();
                FragmentChange.replace(MainActivity.this, actualFragment, ALPHA);
                return true;
            }

            if (itemId == R.id.meni_invoice) {
                shPMainActivity.set(ACTUAL_FRAGMENT, R.id.meni_invoice);
                actualFragment = InvoiceListFragment.newInstance("x", "x");
                FragmentChange.replace(MainActivity.this, actualFragment, ALPHA);
                return true;
            }
            return false;
        });

        navigationView.setNavigationItemSelectedListener(item -> {
            boolean b = false;
            long itemId = item.getItemId();
            if (itemId == R.id.menu_compare_price_list) {
                actualFragment = PriceListCompareFragment.newInstance();
                b = true;
            }

            if(itemId == R.id.menu_hdo) {
                actualFragment = HdoFragment.newInstance();
                b = true;
            }

            if (itemId ==  R.id.menu_backup) {
                actualFragment = new BackupFragment();
                b = true;
            }

            if (itemId ==  R.id.menu_test) {
                actualFragment = TestFragment.newInstance();
                b = true;
            }

            if (actualFragment != null)
                FragmentChange.replace(MainActivity.this, actualFragment, ALPHA);
            drawer.closeDrawer(GravityCompat.START, true);
            return b;
        });

        if (savedInstanceState != null) {
            actualFragment = getSupportFragmentManager().getFragment(savedInstanceState, ACTUAL_FRAGMENT);
        } else {
            actualFragment = MonthlyReadingFragment.newInstance();
            bottomNavigationView.setSelectedItemId(shPMainActivity.get(ACTUAL_FRAGMENT, R.id.meni_monthly_readings));
            FragmentChange.replace(this, actualFragment, ALPHA);
        }
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
     * Akce kliknutí na tlačítko zpět
     * Pokud je otevřený drawer, zavře ho
     * Pokud je otevřený fragment, vrátí se o fragment zpět
     * Pokud je otevřený fragment a je poslední, ukončí aplikaci
     */
    @Override
    public void onBackPressed() {
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
                super.onBackPressed();
            }
        }
    }
}