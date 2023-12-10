package cz.xlisto.odecty.modules.exportimportpricelist;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

/**
 * Xlisto 08.12.2023 20:07
 */
public class MyExportImportPagerAdapter extends FragmentStateAdapter {
    private static final String TAG = "MyExportImportPagerAdapter";
    private final ExportImportPriceListFragment exportImportPriceListFragment;


    public MyExportImportPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
        this.exportImportPriceListFragment = ExportImportPriceListFragment.newInstance();
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 0:
                return exportImportPriceListFragment;
            default:
                return exportImportPriceListFragment;
        }
    }

    @Override
    public int getItemCount() {
        return 2;
    }

    enum TypeTabs {
        EXPORT,
        IMPORT
    }
}
