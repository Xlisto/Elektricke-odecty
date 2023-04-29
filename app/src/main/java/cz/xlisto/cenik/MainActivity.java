package cz.xlisto.cenik;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import cz.xlisto.cenik.modules.backup.BackupFragment;
import cz.xlisto.cenik.modules.invoice.InvoiceFragment;
import cz.xlisto.cenik.modules.invoice.InvoiceListAddEditFragmentAbsctract;
import cz.xlisto.cenik.modules.invoice.InvoiceListFragment;
import cz.xlisto.cenik.modules.invoice.InvoiceTabFragment;
import cz.xlisto.cenik.modules.monthlyreading.MonthlyReadingFragment;
import cz.xlisto.cenik.modules.payment.PaymentFragment;
import cz.xlisto.cenik.modules.pricelist.PriceListCompareFragment;
import cz.xlisto.cenik.modules.pricelist.PriceListFragment;
import cz.xlisto.cenik.shp.ShPMainActivity;
import cz.xlisto.cenik.modules.subscriptionpoint.SubscriptionPointFragment;
import cz.xlisto.cenik.utils.FragmentChange;

import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Toast;


import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;
import com.google.android.material.navigation.NavigationView;

import static cz.xlisto.cenik.shp.ShPMainActivity.ACTUAL_FRAGMENT;
import static cz.xlisto.cenik.utils.FragmentChange.Transaction.ALPHA;

public class MainActivity extends AppCompatActivity {
    private static String TAG = "MainActivity";
    static final String ACTUAL_FRAGMENT = "actualFragment";
    Fragment actualFragment;
    BottomNavigationView bottomNavigationView;
    NavigationView navigationView;
    private ShPMainActivity shPMainActivity;
    private boolean secondClick = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bottomNavigationView = findViewById(R.id.bottomNavigation);
        navigationView = findViewById(R.id.nav_view);

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


        bottomNavigationView.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                switch (item.getItemId()) {

                    case R.id.meni_prices:
                        shPMainActivity.set(ACTUAL_FRAGMENT, R.id.meni_prices);
                        actualFragment = PriceListFragment.newInstance(false, -1l);
                        FragmentChange.replace(MainActivity.this, actualFragment, ALPHA);
                        return true;

                    case R.id.meni_monthly_readings:
                        shPMainActivity.set(ACTUAL_FRAGMENT, R.id.meni_monthly_readings);
                        actualFragment = MonthlyReadingFragment.newInstance("ar1", "ar2");
                        FragmentChange.replace(MainActivity.this, actualFragment, ALPHA);
                        return true;

                    case R.id.meni_subscription_points:
                        shPMainActivity.set(ACTUAL_FRAGMENT, R.id.meni_subscription_points);
                        actualFragment = SubscriptionPointFragment.newInstance("ar1", "ar2");
                        FragmentChange.replace(MainActivity.this, actualFragment, ALPHA);
                        return true;
                    case R.id.meni_invoice:
                        shPMainActivity.set(ACTUAL_FRAGMENT,R.id.meni_invoice);
                        actualFragment = InvoiceListFragment.newInstance("x","x");
                        FragmentChange.replace(MainActivity.this,actualFragment,ALPHA);
                        return true;
                }
                return false;
            }
        });

        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                boolean b = false;
                switch (item.getItemId()) {

                    case R.id.menu_compare_price_list:
                        //shPMainActivity.set(ACTUAL_FRAGMENT, R.id.menu_compare_price_list);
                        actualFragment = PriceListCompareFragment.newInstance();
                        b = true;
                        break;

                    case R.id.menu_backup:
                        actualFragment = new BackupFragment();
                        b = true;
                        break;

                    case R.id.menu_test:
                        actualFragment = TestFragment.newInstance();
                        b = true;
                        break;

                }
                if (actualFragment != null)
                    FragmentChange.replace(MainActivity.this, actualFragment, ALPHA);
                drawer.closeDrawer(GravityCompat.START, true);
                //bottomNavigationView.setSelectedItemId(false);
                return b;
            }
        });

        if (savedInstanceState != null) {
            //Restore the fragment's instance
            actualFragment = getSupportFragmentManager().getFragment(savedInstanceState, ACTUAL_FRAGMENT);
        } else {
            //actualFragment = PriceListFragment.newInstance(false, -1L);
            //actualFragment = SubscriptionPointFragment.newInstance("ar1","ar2");
            actualFragment = MonthlyReadingFragment.newInstance("ar1", "ar2");
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


    /*@Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_nav_view, menu);
        return true;
    }*/

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        Log.w(TAG, "Navigace 2 " + item.getItemId());
        //Log.w(TAG, "Navigace "+item.get);
        return super.onOptionsItemSelected(item);
    }

    /**
     * kliknutí na tlačítko zpět
     */
    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            int pf = getSupportFragmentManager().getBackStackEntryCount();
            if (pf > 0) {
                getSupportFragmentManager().popBackStack();//vrácení o fragment zpět, když je nějaký
            } else if (!secondClick) {
                secondClick = true;
                Toast.makeText(getApplication(), "Následující kliknutí aplikaci ukončí", Toast.LENGTH_SHORT).show();
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        secondClick = false;
                    }
                }, 2000);
            } else {
                super.onBackPressed();
            }
        }
    }


}