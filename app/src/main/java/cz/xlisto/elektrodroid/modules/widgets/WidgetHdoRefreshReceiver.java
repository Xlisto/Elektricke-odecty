package cz.xlisto.elektrodroid.modules.widgets;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import java.util.EnumSet;


/**
 * Příjemce šetrného plánovaného obnovení widgetů závislých na čase HDO
 * ({@link WidgetType#CURRENT_PRICE} a {@link WidgetType#HDO_CLOCK}).
 */
public class WidgetHdoRefreshReceiver extends BroadcastReceiver {

    public static final String ACTION_REFRESH_HDO_WIDGETS = "cz.xlisto.elektrodroid.action.REFRESH_HDO_WIDGETS";


    @Override
    public void onReceive(Context context, Intent intent) {
        if (context == null || intent == null) {
            return;
        }
        if (!ACTION_REFRESH_HDO_WIDGETS.equals(intent.getAction())) {
            return;
        }
        PendingResult pendingResult = goAsync();
        WidgetUpdateManager.updateWidgetTypesAsync(
                context,
                EnumSet.of(WidgetType.CURRENT_PRICE, WidgetType.HDO_CLOCK),
                pendingResult::finish
        );
    }

}
