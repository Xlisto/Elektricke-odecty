package cz.xlisto.odecty.ownview;

import android.content.Context;
import android.util.AttributeSet;
import android.view.ContextMenu;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

/**
 * Rozšířený RecyclerView o kontextové menu
 * pro správný údaj id po vybrané položky je potřeba přepsat metodu getItemId v adaptéru
 * Použitý zdroj: https://stackoverflow.com/questions/40989863/set-and-get-item-id-of-recycler-view
 */
public class ContextMenuRecyclerView extends RecyclerView {
    private RecyclerViewContextMenuInfo mContextMenuInfo;

    public ContextMenuRecyclerView(@NonNull Context context) {
        super(context);
    }

    public ContextMenuRecyclerView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    protected ContextMenu.ContextMenuInfo getContextMenuInfo(){
        return mContextMenuInfo;
    }

    @Override
    public boolean showContextMenuForChild(View originalView) {
        final int longPressPosition = getChildAdapterPosition(originalView);
        if (longPressPosition >= 0) {
            final long longPressId = getAdapter().getItemId(longPressPosition);
            mContextMenuInfo = new RecyclerViewContextMenuInfo(longPressPosition, longPressId);
            return super.showContextMenuForChild(originalView);
        }
        return false;
    }

    public static class RecyclerViewContextMenuInfo implements ContextMenu.ContextMenuInfo {

        public RecyclerViewContextMenuInfo(int position, long id) {
            this.position = position;
            this.id = id;
        }

        final public int position;
        final public long id;
    }
}
