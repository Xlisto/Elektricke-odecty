package cz.xlisto.elektrodroid.utils;


import android.graphics.Rect;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;


/**
 * ItemDecoration pro nastavení rovnoměrných mezer mezi položkami v RecyclerView s LinearLayoutManagerem.
 * Xlisto 30.01.2026
 */
public class LinearSpacingItemDecoration extends RecyclerView.ItemDecoration {

    private final int spacing;


    /**
     * Vytvoří dekoraci pro mezery v lineárním seznamu
     *
     * @param spacingPx velikost mezery v pixelech mezi sousedními položkami
     */
    public LinearSpacingItemDecoration(int spacingPx) {
        this.spacing = spacingPx;
    }


    @Override
    public void getItemOffsets(@NonNull Rect outRect, @NonNull View view, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
        int position = parent.getChildAdapterPosition(view);
        if (position < 0) return;

        // Mezera mezi položkami (pro všechny položky kromě první)
        if (position > 0) {
            outRect.top = spacing;
        }
    }

}
