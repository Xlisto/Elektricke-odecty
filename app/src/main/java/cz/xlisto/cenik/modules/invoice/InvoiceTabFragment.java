package cz.xlisto.cenik.modules.invoice;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.material.tabs.TabLayout;

import androidx.viewpager2.widget.ViewPager2;
import cz.xlisto.cenik.R;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link InvoiceTabFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class InvoiceTabFragment extends Fragment {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String TABLE_FAK = "table_fak";
    private static final String TABLE_NOW = "table_now";
    private static final String TABLE_PAY = "table_pay";
    private static final String ID_FAK = "id_fak";
    private static final String POSITION_ITEM = "position_item";
    private static final String TYPE_TAB = "type_tab";

    // TODO: Rename and change types of parameters
    private String tableFak;
    private String tableNow;
    private String tablePay;
    private long idFak;
    private int positionItem;
    private MyViewPagerAdapter.TypeTabs typeTabs;
    private TabLayout tabLayout;
    private ViewPager2 viewPager2;
    private MyViewPagerAdapter myViewPagerAdapter;

    public InvoiceTabFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param tableFak Parameter 1.
     * @param tablePay Parameter 2.
     * @return A new instance of fragment InvoiceTabFragment.
     */
    // TODO: Rename and change types and number of parameters
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
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_invoice_tab, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        tabLayout = view.findViewById(R.id.tabLayout);
        viewPager2 = view.findViewById(R.id.viewPager);
        myViewPagerAdapter = new MyViewPagerAdapter(this, tableFak, tableNow,tablePay, idFak, positionItem, typeTabs);
        viewPager2.setAdapter(myViewPagerAdapter);

        /*if(typeTabs== MyViewPagerAdapter.TypeTabs.PAYMENT) {
            TabLayout.Tab tab = tabLayout.getTabAt(1);
            tab.select();
            new Handler().postDelayed(new Runnable() {
                                          @Override
                                          public void run() {
                                              viewPager2.setCurrentItem(1, false);
                                          }
                                      }

                    , 0);
        }*/

        if(typeTabs== MyViewPagerAdapter.TypeTabs.PAYMENT) {
        viewPager2.setCurrentItem(2,false);
        tabLayout.getTabAt(2).select();}

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
                tabLayout.getTabAt(position).select();
            }
        });
    }
}