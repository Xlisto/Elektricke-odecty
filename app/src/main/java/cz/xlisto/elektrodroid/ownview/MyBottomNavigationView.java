package cz.xlisto.elektrodroid.ownview;


import android.content.Context;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomnavigation.BottomNavigationView;

/**
 * Xlisto 26.12.2023 17:54
 */
public class MyBottomNavigationView extends BottomNavigationView {
    private static final String TAG = "MyBottomNavigationView";
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

    public MyBottomNavigationView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    public int getMaxItemCount() {
        return MAX_ITEM_COUNT;
    }
}
