package cz.xlisto.elektrodroid.utils;


import android.graphics.Rect;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;


/**
 * ItemDecoration pro nastavení rovnoměrných mezer mezi položkami v RecyclerView s GridLayoutManagerem.
 * Xlisto 30.01.2026
 */
public class GridSpacingItemDecoration extends RecyclerView.ItemDecoration {

    private final int spanCount;
    private final int spacing;
    private final boolean includeEdge;


    /**
     * Vytvoří dekoraci pro mezery v mřížce
     *
     * @param spanCount   počet sloupců mřížky
     * @param spacingPx   velikost mezery v pixelech
     * @param includeEdge true pokud mají být mezery i na vnějších okrajích
     */
    public GridSpacingItemDecoration(int spanCount, int spacingPx, boolean includeEdge) {
        this.spanCount = spanCount;
        this.spacing = spacingPx;
        this.includeEdge = includeEdge;
    }


    @Override
    public void getItemOffsets(@NonNull Rect outRect, @NonNull View view, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
        int position = parent.getChildAdapterPosition(view);
        if (position < 0) return;

        int column = position % spanCount;

        if (includeEdge) {
            outRect.left = spacing - column * spacing / spanCount;
            outRect.right = (column + 1) * spacing / spanCount;

            if (position < spanCount) {
                outRect.top = spacing;
            }
            outRect.bottom = spacing;
        } else {
            outRect.left = column * spacing / spanCount;
            outRect.right = spacing - (column + 1) * spacing / spanCount;
            if (position >= spanCount) {
                outRect.top = spacing;
            }
        }
    }

}
