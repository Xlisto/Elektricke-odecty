package cz.xlisto.odecty.modules.invoice;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.material.tabs.TabLayout;

import java.util.Objects;

import androidx.viewpager2.widget.ViewPager2;
import cz.xlisto.odecty.R;

/**
 * Fragment zahrnující záložky a fragmenty záznamů faktury, detaily faktury a plateb faktury.
 */
public class InvoiceTabFragment extends Fragment {
    private static final String TABLE_FAK = "tableFak";
    private static final String TABLE_NOW = "tableNow";
    private static final String TABLE_PAY = "tablePay";
    private static final String ID_FAK = "idFak";
    private static final String POSITION_ITEM = "positionItem";
    private static final String TYPE_TAB = "typeTab";
    private String tableFak;
    private String tableNow;
    private String tablePay;
    private long idFak;
    private int positionItem;
    private MyViewPagerAdapter.TypeTabs typeTabs;
    private TabLayout tabLayout;
    private ViewPager2 viewPager2;

    public InvoiceTabFragment() {
        // Required empty public constructor
    }


    public static InvoiceTabFragment newInstance(String tableFak, String tableNow,String tablePay, long idFak, int positionItem, MyViewPagerAdapter.TypeTabs typeTabs) {
        InvoiceTabFragment fragment = new InvoiceTabFragment();
        Bundle args = new Bundle();
        args.putString(TABLE_FAK, tableFak);
        args.putString(TABLE_NOW, tableNow);
        args.putString(TABLE_PAY, tablePay);
        args.putLong(ID_FAK, idFak);
        args.putInt(POSITION_ITEM, positionItem);
        args.putSerializable(TYPE_TAB, typeTabs);
        fragment.setArguments(args);
        return fragment;
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            tableFak = getArguments().getString(TABLE_FAK);
            tableNow = getArguments().getString(TABLE_NOW);
            tablePay = getArguments().getString(TABLE_PAY);
            idFak = getArguments().getLong(ID_FAK);
            positionItem = getArguments().getInt(POSITION_ITEM);
            typeTabs = (MyViewPagerAdapter.TypeTabs) getArguments().get(TYPE_TAB);
        }
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_invoice_tab, container, false);
    }


    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        tabLayout = view.findViewById(R.id.tabLayout);
        viewPager2 = view.findViewById(R.id.viewPager);
        MyViewPagerAdapter myViewPagerAdapter = new MyViewPagerAdapter(this, tableFak, tableNow, tablePay, idFak, positionItem);
        viewPager2.setAdapter(myViewPagerAdapter);


        if(typeTabs== MyViewPagerAdapter.TypeTabs.PAYMENT) {
        viewPager2.setCurrentItem(2,false);
        Objects.requireNonNull(tabLayout.getTabAt(2)).select();}

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                viewPager2.setCurrentItem(tab.getPosition(),true);
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });

        viewPager2.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                Objects.requireNonNull(tabLayout.getTabAt(position)).select();
            }
        });
    }
}