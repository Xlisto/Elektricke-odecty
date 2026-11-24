package cz.xlisto.elektrodroid.modules.widgets;


import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;


/**
 * Společná abstraktní základna pro všechny {@link AppWidgetProvider} třídy aplikace ElektroDroid.
 * <p>
 * Zajišťuje jednotnou a vláknově bezpečnou obsluhu životního cyklu každé instance widgetu:
 * aktualizaci po přidání či periodickém intervalu ({@link #onUpdate}), změnu velikosti
 * ({@link #onAppWidgetOptionsChanged}), odstranění konkrétní instance ({@link #onDeleted})
 * i systémové přemapování ID ({@link #onRestored}).
 */
public abstract class BaseElektroWidgetProvider extends AppWidgetProvider {

    /**
     * Vrátí konkrétní {@link WidgetType} obsluhovaný tímto providerem.
     *
     * @return typ widgetu
     */
    @NonNull
    protected abstract WidgetType getWidgetType();


    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        if (context == null || appWidgetIds == null || appWidgetIds.length == 0) {
            return;
        }
        PendingResult pendingResult = goAsync();
        WidgetUpdateManager.updateWidgetsAsync(context, getWidgetType(), appWidgetIds, pendingResult::finish);
    }


    @Override
    public void onAppWidgetOptionsChanged(Context context,
                                          AppWidgetManager appWidgetManager,
                                          int appWidgetId,
                                          Bundle newOptions) {
        if (context == null || appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            return;
        }
        PendingResult pendingResult = goAsync();
        WidgetUpdateManager.updateWidgetsAsync(context, getWidgetType(), new int[]{appWidgetId}, pendingResult::finish);
    }


    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        if (context == null || appWidgetIds == null || appWidgetIds.length == 0) {
            return;
        }
        WidgetUpdateManager.onWidgetsDeleted(context, appWidgetIds);
    }


    @Override
    public void onRestored(Context context, int[] oldWidgetIds, int[] newWidgetIds) {
        if (context == null) {
            return;
        }
        PendingResult pendingResult = goAsync();
        WidgetUpdateManager.onWidgetsRestored(context, getWidgetType(), oldWidgetIds, newWidgetIds, pendingResult::finish);
    }


    @Override
    public void onDisabled(Context context) {
        if (context == null) {
            return;
        }
        WidgetUpdateManager.rescheduleHdoWidgetsRefreshAsync(context);
    }

}
