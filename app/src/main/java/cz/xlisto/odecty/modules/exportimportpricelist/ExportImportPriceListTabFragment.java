package cz.xlisto.odecty.modules.exportimportpricelist;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.material.tabs.TabLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;
import cz.xlisto.odecty.R;

/**
 * Xlisto 08.12.2023 17:37
 */
public class ExportImportPriceListTabFragment extends Fragment {
    private static final String TAG = "ExportImportPriceListTabFragment";


    public ExportImportPriceListTabFragment() {
    }

    public static ExportImportPriceListTabFragment newInstance() {
        return new ExportImportPriceListTabFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_export_import_tab, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        TabLayout tabLayout = view.findViewById(R.id.tabLayout);
        ViewPager2 viewPager = view.findViewById(R.id.viewPager);

        MyExportImportPagerAdapter myExportImportPagerAdapter = new MyExportImportPagerAdapter(requireActivity());
        viewPager.setAdapter(myExportImportPagerAdapter);
    }
}
