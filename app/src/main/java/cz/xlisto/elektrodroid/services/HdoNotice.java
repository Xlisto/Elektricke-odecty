package cz.xlisto.elektrodroid.services;


import static android.content.Context.NOTIFICATION_SERVICE;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import cz.xlisto.elektrodroid.MainActivity;
import cz.xlisto.elektrodroid.R;


/**
 * Pomocná třída pro správu notifikací v aplikaci.
 * Umožňuje zobrazit dočasnou notifikaci, notifikaci pro foreground službu
 * a rušit notifikace podle ID.
 * <p>
 * Xlisto 01.06.2023 22:58
 */
public class HdoNotice {

    public static final String NOTIFICATION_CHANNEL_ID = "10001";
    public static final String NOTIFICATION_HDO_SERVICE = "HdoService";
    public static final String ARGS_FRAGMENT = "ARGS_FRAGMENT";
    public static final String EXTRA_SUBSCRIPTION_POINT_ID = "extra_subscription_point_id";
    private static final String DEFAULT_NOTIFICATION_CHANNEL_ID = "default";
    private static final String HDO_NOTIFICATION_GROUP_KEY = "hdo_notification_group";
    private static final int SUMMARY_NOTIFICATION_ID = 10001;
    private static final int BASE_SUBSCRIPTION_NOTIFICATION_ID = 20000;


    /**
     * Zobrazí dočasnou notifikaci s daným titulkem a textem. Vhodná pro vyžádání pozornosti uživatele pro znovuspuštění služby
     *
     * @param context     Kontext aplikace
     * @param title       Titulek notifikace
     * @param noticeTitle Název kanálu notifikace
     * @param contentText Text notifikace
     */
    public static void setNotice(Context context, String title, String noticeTitle, String contentText) {
        setNotice(context, title, noticeTitle, contentText, -1L);
    }


    /**
     * Zobrazí HDO notifikaci. Pro jedno odběrné místo vždy přepíše předchozí notifikaci.
     *
     * @param context             Kontext aplikace
     * @param title               Titulek notifikace
     * @param noticeTitle         Název kanálu notifikace
     * @param contentText         Text notifikace
     * @param subscriptionPointId ID odběrného místa; pokud je <= 0, použije se fallback ID
     */
    public static void setNotice(Context context, String title, String noticeTitle, String contentText, long subscriptionPointId) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        intent.putExtra(ARGS_FRAGMENT, NOTIFICATION_HDO_SERVICE);
        if (subscriptionPointId > 0) {
            intent.putExtra(EXTRA_SUBSCRIPTION_POINT_ID, subscriptionPointId);
        }

        int requestCode = subscriptionPointId > 0 ? ("hdo_open_" + subscriptionPointId).hashCode() : SUMMARY_NOTIFICATION_ID;
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, DEFAULT_NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(contentText)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setGroup(HDO_NOTIFICATION_GROUP_KEY)
                .setAutoCancel(true);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(NOTIFICATION_CHANNEL_ID, noticeTitle, importance);
            channel.setShowBadge(false);
            channel.setSound(null, null);
            builder.setChannelId(NOTIFICATION_CHANNEL_ID);

            assert notificationManager != null;
            notificationManager.createNotificationChannel(channel);
        }

        assert notificationManager != null;
        notificationManager.notify(resolveNotificationId(subscriptionPointId), builder.build());

        NotificationCompat.Builder summaryBuilder = new NotificationCompat.Builder(context, DEFAULT_NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(noticeTitle)
                .setContentText(contentText)
                .setGroup(HDO_NOTIFICATION_GROUP_KEY)
                .setGroupSummary(true)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            summaryBuilder.setChannelId(NOTIFICATION_CHANNEL_ID);
        }
        notificationManager.notify(SUMMARY_NOTIFICATION_ID, summaryBuilder.build());
    }


    private static int resolveNotificationId(long subscriptionPointId) {
        if (subscriptionPointId <= 0) {
            return SUMMARY_NOTIFICATION_ID + 1;
        }
        return BASE_SUBSCRIPTION_NOTIFICATION_ID + Math.abs((int) (subscriptionPointId % 10000));
    }

}
