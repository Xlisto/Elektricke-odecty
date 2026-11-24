package cz.xlisto.elektrodroid.modules.widgets;


import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import androidx.annotation.NonNull;

import cz.xlisto.elektrodroid.MainActivity;


/**
 * Sestavuje jednoznačné {@link PendingIntent} akce pro navigaci z domovských widgetů do aplikace
 * ElektroDroid a pro opakované otevření konfigurační obrazovky konkrétní instance widgetu.
 * <p>
 * Každý {@link PendingIntent} používá jedinečné URI a unikátní {@code requestCode} odvozené
 * z {@code appWidgetId}, takže více instancí téhož widgetu si nikdy nepřepíše své klikací cíle.
 */
public final class WidgetNavigationHelper {

    public static final String ACTION_OPEN_FROM_WIDGET = "cz.xlisto.elektrodroid.action.OPEN_FROM_WIDGET";
    public static final String EXTRA_WIDGET_DESTINATION = "cz.xlisto.elektrodroid.widget.EXTRA_DESTINATION";
    public static final String EXTRA_SUBSCRIPTION_POINT_ID = "cz.xlisto.elektrodroid.widget.EXTRA_POINT_ID";
    public static final String EXTRA_SUBSCRIPTION_POINT_MILINS = "cz.xlisto.elektrodroid.widget.EXTRA_POINT_MILINS";
    public static final String EXTRA_APP_WIDGET_ID = "cz.xlisto.elektrodroid.widget.EXTRA_APP_WIDGET_ID";

    public static final String DEST_DASHBOARD = "dashboard";
    public static final String DEST_HDO = "hdo";
    public static final String DEST_MONTHLY_READINGS = "monthly_readings";
    public static final String DEST_BILLING = "billing";
    public static final String DEST_NEW_READING = "new_reading";


    private WidgetNavigationHelper() {
    }


    /**
     * Vytvoří {@link PendingIntent}, který otevře {@link MainActivity} na cílové obrazovce
     * pro odběrné místo konkrétní instance widgetu.
     *
     * @param context                 kontext aplikace
     * @param appWidgetId             systémové ID instance widgetu
     * @param destination             cílová obrazovka (jedna z konstant {@code DEST_*})
     * @param subscriptionPointId     databázové ID odběrného místa
     * @param subscriptionPointMilins stabilní {@code milins} odběrného místa pro ověření identity
     * @return připravený {@link PendingIntent}
     */
    @NonNull
    public static PendingIntent createNavigationPendingIntent(@NonNull Context context,
                                                              int appWidgetId,
                                                              @NonNull String destination,
                                                              long subscriptionPointId,
                                                              long subscriptionPointMilins) {
        Intent intent = new Intent(context.getApplicationContext(), MainActivity.class);
        intent.setAction(ACTION_OPEN_FROM_WIDGET);
        intent.setData(Uri.parse("elektrodroid://widget/nav/" + appWidgetId + "/" + destination));
        intent.putExtra(EXTRA_WIDGET_DESTINATION, destination);
        intent.putExtra(EXTRA_SUBSCRIPTION_POINT_ID, subscriptionPointId);
        intent.putExtra(EXTRA_SUBSCRIPTION_POINT_MILINS, subscriptionPointMilins);
        intent.putExtra(EXTRA_APP_WIDGET_ID, appWidgetId);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        int requestCode = buildRequestCode(appWidgetId, destination.hashCode() & 0x0F);
        return PendingIntent.getActivity(
                context.getApplicationContext(),
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
    }


    /**
     * Vytvoří {@link PendingIntent}, který otevře {@link WidgetConfigActivity} pro úpravu nastavení
     * již existující instance widgetu (bez vytvoření nové instance).
     *
     * @param context     kontext aplikace
     * @param appWidgetId systémové ID instance widgetu
     * @return připravený {@link PendingIntent}
     */
    @NonNull
    public static PendingIntent createConfigurePendingIntent(@NonNull Context context, int appWidgetId) {
        Intent intent = new Intent(context.getApplicationContext(), WidgetConfigActivity.class);
        intent.setAction(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE);
        intent.setData(Uri.parse("elektrodroid://widget/config/" + appWidgetId));
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        int requestCode = buildRequestCode(appWidgetId, 15);
        return PendingIntent.getActivity(
                context.getApplicationContext(),
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
    }


    private static int buildRequestCode(int appWidgetId, int slot) {
        return (appWidgetId * 31) + slot;
    }

}
