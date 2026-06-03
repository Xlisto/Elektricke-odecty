package cz.xlisto.elektrodroid.ownview;


import android.content.Context;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomnavigation.BottomNavigationView;


/**
 * Vlastní BottomNavigationView rozšiřující Material Design komponent.
 * <p>
 * Upravuje maximální počet položek v navigaci na 6 namísto výchozích 5,
 * aby se vešlo více navigačních možností.
 * <p>
 * Xlisto 26.12.2023 17:54
 */
public class MyBottomNavigationView extends BottomNavigationView {

    private static final int MAX_ITEM_COUNT = 6;


    public MyBottomNavigationView(@NonNull Context context) {
        super(context);
    }


    public MyBottomNavigationView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }


    public MyBottomNavigationView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }


    @Override
    public int getMaxItemCount() {
        return MAX_ITEM_COUNT;
    }

}
