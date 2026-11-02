package cz.xlisto.elektrodroid.ownview;

import android.content.Context;
import android.util.AttributeSet;
import android.view.ContextMenu;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

/**
 * Rozšířený RecyclerView o podporu kontextového menu.
 * Pro správný údaj id vybrané položky je potřeba přepsat metodu getItemId v adaptéru.
 * <p>
 * Použitý zdroj: <a href="https://stackoverflow.com/questions/40989863/set-and-get-item-id-of-recycler-view">StackOverflow</a>
 */
public class ContextMenuRecyclerView extends RecyclerView {

    private RecyclerViewContextMenuInfo mContextMenuInfo;


    public ContextMenuRecyclerView(@NonNull Context context) {
        super(context);
    }


    public ContextMenuRecyclerView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }


    @Override
    protected ContextMenu.ContextMenuInfo getContextMenuInfo() {
        return mContextMenuInfo;
    }


    @Override
    public boolean showContextMenuForChild(View originalView) {
        final int longPressPosition = getChildAdapterPosition(originalView);
        if (longPressPosition >= 0 && getAdapter() != null) {
            final long longPressId = getAdapter().getItemId(longPressPosition);
            mContextMenuInfo = new RecyclerViewContextMenuInfo(longPressPosition, longPressId);
            return super.showContextMenuForChild(originalView);
        }
        return false;
    }


    /**
     * Kontejner uchovávající informace o pozici a ID položky pro kontextové menu.
     */
    public record RecyclerViewContextMenuInfo(int position,
                                              long id) implements ContextMenu.ContextMenuInfo {

    }

}
